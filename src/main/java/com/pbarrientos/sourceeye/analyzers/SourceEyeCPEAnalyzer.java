package com.pbarrientos.sourceeye.analyzers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.CPEAnalyzer;
import org.owasp.dependencycheck.analyzer.CpeSuppressionAnalyzer;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.data.cpe.CpeMemoryIndex;
import org.owasp.dependencycheck.data.cpe.Fields;
import org.owasp.dependencycheck.data.cpe.IndexEntry;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Evidence;
import org.owasp.dependencycheck.dependency.EvidenceType;
import org.owasp.dependencycheck.dependency.Identifier;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.owasp.dependencycheck.exception.InitializationException;
import org.owasp.dependencycheck.utils.DependencyVersion;
import org.owasp.dependencycheck.utils.DependencyVersionUtil;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pbarrientos.sourceeye.engine.SourceEyeEngine;

/**
 * Using {@link CPEAnalyzer} as a base, extends it to adapt to Source Eye needs.
 *
 * @author Pablo Barrientos
 */
@ThreadSafe
public class SourceEyeCPEAnalyzer extends CPEAnalyzer {

    /**
     * The Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceEyeCPEAnalyzer.class);

    /**
     * The property used to enable the analyzer
     */
    public static final String ANALYZER_SE_CPE_ENABLED = "analyzer.se.cpe.enabled";

    /**
     * The maximum number of query results to return.
     */
    private static final int MAX_QUERY_RESULTS = 25;

    /**
     * The URL to perform a search of the NVD CVE data at NIST.
     */
    public static final String NVD_SEARCH_URL = "https://web.nvd.nist.gov/view/vuln/search-results?adv_search=true&cves=on&cpe_version=%s";

    /**
     * The CPE in memory index.
     */
    private CpeMemoryIndex cpe;

    /**
     * The CVE Database.
     */
    private CveDB cve;

    /**
     * A reference to the suppression analyzer; for timing reasons we need to test for suppressions immediately after
     * identifying the match because a higher confidence match on a FP can mask a lower confidence, yet valid match.
     */
    private CpeSuppressionAnalyzer suppression;

    /**
     * The list of ecosystems to skip during analysis. These are skipped because there is generally a more accurate
     * vulnerability analyzer in the pipeline.
     */
    private List<String> skipEcosystems;

    /**
     * @return the name of this analyzer.
     */
    @Override
    public String getName() {
        return "Source Eye CPE Analyzer";
    }

    /**
     * @see org.owasp.dependencycheck.analyzer.CPEAnalyzer#prepareAnalyzer(org.owasp.dependencycheck.Engine) Changes the
     *      {@link CveDB} and {@link CpeMemoryIndex} used on this analyzer.
     */
    @Override
    public void prepareAnalyzer(final Engine engine) throws InitializationException {
        SourceEyeEngine sEngine = (SourceEyeEngine) engine;

        this.cve = sEngine.getDatabase();
        this.cpe = sEngine.getCpeService().getIndex();

        final String[] tmp = engine.getSettings().getArray(Settings.KEYS.ECOSYSTEM_SKIP_CPEANALYZER);
        if (tmp == null) {
            this.skipEcosystems = new ArrayList<>();
        } else {
            SourceEyeCPEAnalyzer.LOGGER.info("Skipping CPE Analysis for {}", StringUtils.join(tmp, ","));
            this.skipEcosystems = Arrays.asList(tmp);
        }

        this.suppression = new CpeSuppressionAnalyzer();
        this.suppression.initialize(engine.getSettings());
        this.suppression.prepareAnalyzer(engine);
    }

    /**
     * Analyzes a dependency and attempts to determine if there are any CPE identifiers for this dependency.
     *
     * @param dependency The Dependency to analyze.
     * @param engine The analysis engine
     * @throws AnalysisException is thrown if there is an issue analyzing the dependency.
     */
    @Override
    protected void analyzeDependency(final Dependency dependency, final Engine engine) throws AnalysisException {
        if (this.skipEcosystems.contains(dependency.getEcosystem())) {
            return;
        }
        try {
            this.determineCPE(dependency);
        } catch (CorruptIndexException ex) {
            throw new AnalysisException("CPE Index is corrupt.", ex);
        } catch (IOException ex) {
            throw new AnalysisException("Failure opening the CPE Index.", ex);
        } catch (ParseException ex) {
            throw new AnalysisException("Unable to parse the generated Lucene query for this dependency.", ex);
        }
    }

    /**
     * <p>
     * Searches the Lucene CPE index to identify possible CPE entries associated with the supplied vendor, product, and
     * version.
     * </p>
     * <p>
     * If either the vendorWeightings or productWeightings lists have been populated this data is used to add weighting
     * factors to the search.
     * </p>
     *
     * @param vendor the text used to search the vendor field
     * @param product the text used to search the product field
     * @param vendorWeightings a list of strings to use to add weighting factors to the vendor field
     * @param productWeightings Adds a list of strings that will be used to add weighting factors to the product search
     * @return a list of possible CPE values
     */
    @Override
    protected List<IndexEntry> searchCPE(final String vendor, final String product, final Set<String> vendorWeightings,
            final Set<String> productWeightings) {

        final List<IndexEntry> ret = new ArrayList<>(SourceEyeCPEAnalyzer.MAX_QUERY_RESULTS);

        final String searchString = this.buildSearch(vendor, product, vendorWeightings, productWeightings);
        if (searchString == null) {
            return ret;
        }
        try {
            final TopDocs docs = this.cpe.search(searchString, SourceEyeCPEAnalyzer.MAX_QUERY_RESULTS);
            for (ScoreDoc d : docs.scoreDocs) {
                if (d.score >= 0.08) {
                    final Document doc = this.cpe.getDocument(d.doc);
                    final IndexEntry entry = new IndexEntry();
                    entry.setVendor(doc.get(Fields.VENDOR));
                    entry.setProduct(doc.get(Fields.PRODUCT));
                    entry.setSearchScore(d.score);
                    if (!ret.contains(entry)) {
                        ret.add(entry);
                    }
                }
            }
            return ret;
        } catch (ParseException ex) {
            SourceEyeCPEAnalyzer.LOGGER.warn("An error occurred querying the CPE data. See the log for more details.");
            SourceEyeCPEAnalyzer.LOGGER.info("Unable to parse: {}", searchString, ex);
        } catch (IOException ex) {
            SourceEyeCPEAnalyzer.LOGGER.warn("An error occurred reading CPE data. See the log for more details.");
            SourceEyeCPEAnalyzer.LOGGER.info("IO Error with search string: {}", searchString, ex);
        }
        return null;
    }

    /**
     * Retrieves a list of CPE values from the CveDB based on the vendor and product passed in. The list is then
     * validated to find only CPEs that are valid for the given dependency. It is possible that the CPE identified is a
     * best effort "guess" based on the vendor, product, and version information.
     *
     * @param dependency the Dependency being analyzed
     * @param vendor the vendor for the CPE being analyzed
     * @param product the product for the CPE being analyzed
     * @param currentConfidence the current confidence being used during analysis
     * @return <code>true</code> if an identifier was added to the dependency; otherwise <code>false</code>
     * @throws UnsupportedEncodingException is thrown if UTF-8 is not supported
     * @throws AnalysisException thrown if the suppression rules failed
     */
    @Override
    protected boolean determineIdentifiers(final Dependency dependency, final String vendor, final String product,
            final Confidence currentConfidence) throws UnsupportedEncodingException, AnalysisException {
        final Set<VulnerableSoftware> cpes = this.cve.getCPEs(vendor, product);
        if (cpes.isEmpty()) {
            return false;
        }
        DependencyVersion bestGuess = new DependencyVersion("-");
        Confidence bestGuessConf = null;
        boolean hasBroadMatch = false;
        final List<IdentifierMatch> collected = new ArrayList<>();

        // TODO the following algorithm incorrectly identifies things as a lower version
        // if there lower confidence evidence when the current (highest) version number
        // is newer then anything in the NVD.
        for (Confidence conf : Confidence.values()) {
            for (Evidence evidence : dependency.getIterator(EvidenceType.VERSION, conf)) {
                final DependencyVersion evVer = DependencyVersionUtil.parseVersion(evidence.getValue());
                if (evVer == null) {
                    continue;
                }
                for (VulnerableSoftware vs : cpes) {
                    final DependencyVersion dbVer;
                    if ((vs.getUpdate() != null) && !vs.getUpdate().isEmpty()) {
                        dbVer = DependencyVersionUtil.parseVersion(vs.getVersion() + '.' + vs.getUpdate());
                    } else {
                        dbVer = DependencyVersionUtil.parseVersion(vs.getVersion());
                    }
                    if (dbVer == null) { // special case, no version specified - everything is vulnerable
                        hasBroadMatch = true;
                        final String url = String.format(SourceEyeCPEAnalyzer.NVD_SEARCH_URL,
                                URLEncoder.encode(vs.getName(), StandardCharsets.UTF_8.name()));
                        final IdentifierMatch match = new IdentifierMatch("cpe", vs.getName(), url,
                                IdentifierConfidence.BROAD_MATCH, conf);
                        collected.add(match);
                    } else if (evVer.equals(dbVer)) { // yeah! exact match
                        final String url = String.format(SourceEyeCPEAnalyzer.NVD_SEARCH_URL,
                                URLEncoder.encode(vs.getName(), StandardCharsets.UTF_8.name()));
                        final IdentifierMatch match = new IdentifierMatch("cpe", vs.getName(), url,
                                IdentifierConfidence.EXACT_MATCH, conf);
                        collected.add(match);

                        // TODO the following isn't quite right is it? need to think about this guessing
                        // game a bit more.
                    } else if ((evVer.getVersionParts().size() <= dbVer.getVersionParts().size())
                            && evVer.matchesAtLeastThreeLevels(dbVer)) {
                        if ((bestGuessConf == null) || (bestGuessConf.compareTo(conf) > 0)) {
                            if (bestGuess.getVersionParts().size() < dbVer.getVersionParts().size()) {
                                bestGuess = dbVer;
                                bestGuessConf = conf;
                            }
                        }
                    }
                }
                if (((bestGuessConf == null) || (bestGuessConf.compareTo(conf) > 0))
                        && (bestGuess.getVersionParts().size() < evVer.getVersionParts().size())) {
                    bestGuess = evVer;
                    bestGuessConf = conf;
                }
            }
        }
        final String cpeName = String.format("cpe:/a:%s:%s:%s", vendor, product, bestGuess.toString());
        String url = null;
        if (hasBroadMatch) { // if we have a broad match we can add the URL to the best guess.
            final String cpeUrlName = String.format("cpe:/a:%s:%s", vendor, product);
            url = String.format(SourceEyeCPEAnalyzer.NVD_SEARCH_URL,
                    URLEncoder.encode(cpeUrlName, StandardCharsets.UTF_8.name()));
        }
        if (bestGuessConf == null) {
            bestGuessConf = Confidence.LOW;
        }
        final IdentifierMatch match = new IdentifierMatch("cpe", cpeName, url, IdentifierConfidence.BEST_GUESS,
                bestGuessConf);

        collected.add(match);

        Collections.sort(collected);
        final IdentifierConfidence bestIdentifierQuality = collected.get(0).getConfidence();
        final Confidence bestEvidenceQuality = collected.get(0).getEvidenceConfidence();
        boolean identifierAdded = false;
        for (IdentifierMatch m : collected) {
            if (bestIdentifierQuality.equals(m.getConfidence())
                    && bestEvidenceQuality.equals(m.getEvidenceConfidence())) {
                final Identifier i = m.getIdentifier();
                if (bestIdentifierQuality == IdentifierConfidence.BEST_GUESS) {
                    i.setConfidence(Confidence.LOW);
                } else {
                    i.setConfidence(bestEvidenceQuality);
                }
                // TODO - while this gets the job down it is slow; consider refactoring
                dependency.addIdentifier(i);
                this.suppression.analyze(dependency, null);
                if (dependency.getIdentifiers().contains(i)) {
                    identifierAdded = true;
                }
            }
        }
        return identifierAdded;
    }

    /**
     * @see org.owasp.dependencycheck.analyzer.CPEAnalyzer#closeAnalyzer() Override this method, since we do not want to
     *      close the {@link CpeMemoryIndex}
     */
    @Override
    public void closeAnalyzer() {
        // Do nothing
    }

    /**
     * <p>
     * Returns the setting key to determine if the analyzer is enabled.
     * </p>
     *
     * @return the key for the analyzer's enabled property
     */
    @Override
    protected String getAnalyzerEnabledSettingKey() {
        return SourceEyeCPEAnalyzer.ANALYZER_SE_CPE_ENABLED;
    }

    /**
     * @see org.owasp.dependencycheck.analyzer.AbstractAnalyzer#supportsParallelProcessing()
     */
    @Override
    public boolean supportsParallelProcessing() {
        return true;
    }

    /**
     * The confidence whether the identifier is an exact match, or a best guess.
     */
    private enum IdentifierConfidence {

        /**
         * An exact match for the CPE.
         */
        EXACT_MATCH,
        /**
         * A best guess for the CPE.
         */
        BEST_GUESS,
        /**
         * The entire vendor/product group must be added (without a guess at version) because there is a CVE with a VS
         * that only specifies vendor/product.
         */
        BROAD_MATCH
    }

    /**
     * A simple object to hold an identifier and carry information about the confidence in the identifier.
     */
    private static class IdentifierMatch implements Comparable<IdentifierMatch> {

        /**
         * The confidence in the evidence used to identify this match.
         */
        private Confidence evidenceConfidence;

        /**
         * The confidence whether this is an exact match, or a best guess.
         */
        private IdentifierConfidence confidence;

        /**
         * The CPE identifier.
         */
        private Identifier identifier;

        /**
         * Constructs an IdentifierMatch.
         *
         * @param type the type of identifier (such as CPE)
         * @param value the value of the identifier
         * @param url the URL of the identifier
         * @param identifierConfidence the confidence in the identifier: best guess or exact match
         * @param evidenceConfidence the confidence of the evidence used to find the identifier
         */
        IdentifierMatch(final String type, final String value, final String url,
                final IdentifierConfidence identifierConfidence, final Confidence evidenceConfidence) {
            this.identifier = new Identifier(type, value, url);
            this.confidence = identifierConfidence;
            this.evidenceConfidence = evidenceConfidence;
        }

        /**
         * Get the value of evidenceConfidence
         *
         * @return the value of evidenceConfidence
         */
        public Confidence getEvidenceConfidence() {
            return this.evidenceConfidence;
        }

        /**
         * Set the value of evidenceConfidence
         *
         * @param evidenceConfidence new value of evidenceConfidence
         */
        public void setEvidenceConfidence(final Confidence evidenceConfidence) {
            this.evidenceConfidence = evidenceConfidence;
        }

        /**
         * Get the value of confidence.
         *
         * @return the value of confidence
         */
        public IdentifierConfidence getConfidence() {
            return this.confidence;
        }

        /**
         * Set the value of confidence.
         *
         * @param confidence new value of confidence
         */
        public void setConfidence(final IdentifierConfidence confidence) {
            this.confidence = confidence;
        }

        /**
         * Get the value of identifier.
         *
         * @return the value of identifier
         */
        public Identifier getIdentifier() {
            return this.identifier;
        }

        /**
         * Set the value of identifier.
         *
         * @param identifier new value of identifier
         */
        public void setIdentifier(final Identifier identifier) {
            this.identifier = identifier;
        }
        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="Standard implementations of
        // toString, hashCode, and equals">

        /**
         * Standard toString() implementation.
         *
         * @return the string representation of the object
         */
        @Override
        public String toString() {
            return "IdentifierMatch{" + "evidenceConfidence=" + this.evidenceConfidence + ", confidence="
                    + this.confidence + ", identifier=" + this.identifier + '}';
        }

        /**
         * Standard hashCode() implementation.
         *
         * @return the hashCode
         */
        @Override
        public int hashCode() {
            int hash = 5;
            hash = (97 * hash) + (this.evidenceConfidence != null ? this.evidenceConfidence.hashCode() : 0);
            hash = (97 * hash) + (this.confidence != null ? this.confidence.hashCode() : 0);
            hash = (97 * hash) + (this.identifier != null ? this.identifier.hashCode() : 0);
            return hash;
        }

        /**
         * Standard equals implementation.
         *
         * @param obj the object to compare
         * @return true if the objects are equal, otherwise false
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            final IdentifierMatch other = (IdentifierMatch) obj;
            if (this.evidenceConfidence != other.evidenceConfidence) {
                return false;
            }
            if (this.confidence != other.confidence) {
                return false;
            }
            return !((this.identifier != other.identifier)
                    && ((this.identifier == null) || !this.identifier.equals(other.identifier)));
        }
        // </editor-fold>

        /**
         * Standard implementation of compareTo that compares identifier confidence, evidence confidence, and then the
         * identifier.
         *
         * @param o the IdentifierMatch to compare to
         * @return the natural ordering of IdentifierMatch
         */
        @Override
        public int compareTo(final IdentifierMatch o) {
            return new CompareToBuilder().append(this.confidence, o.confidence)
                    .append(this.evidenceConfidence, o.evidenceConfidence).append(this.identifier, o.identifier)
                    .toComparison();
        }
    }

}
