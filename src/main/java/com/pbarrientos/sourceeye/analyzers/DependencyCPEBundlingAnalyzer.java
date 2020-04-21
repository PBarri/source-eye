package com.pbarrientos.sourceeye.analyzers;

import java.util.Set;

import org.owasp.dependencycheck.analyzer.DependencyBundlingAnalyzer;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on {@link DependencyBundlingAnalyzer}
 * <p>
 * This analyzer ensures dependencies that should be grouped together, to remove excess noise from the report, are
 * grouped. An example would be Spring, Spring Beans, Spring MVC, etc. If they are all for the same version and have the
 * same relative path then these should be grouped into a single dependency under the core/main library.
 * </p>
 * <p>
 * Note, this grouping only works on dependencies with identified CVE entries
 * </p>
 *
 * @author Pablo Barrientos
 */
public class DependencyCPEBundlingAnalyzer extends DependencyBundlingAnalyzer {

    /**
     * The Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyCPEBundlingAnalyzer.class);

    /**
     * The property used to enable the analyzer
     */
    public static final String ANALYZER_SE_CPE_ENABLED = "analyzer.se.dependency.bundling.enabled";

    /**
     * @return the name of this analyzer.
     */
    @Override
    public String getName() {
        return "Dependency CPE Bundling Analyzer";
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
        return DependencyCPEBundlingAnalyzer.ANALYZER_SE_CPE_ENABLED;
    }

    /**
     * Check if the CPEs or the names of the dependencies matches.
     *
     * @see org.owasp.dependencycheck.analyzer.DependencyBundlingAnalyzer#evaluateDependencies(org.owasp.dependencycheck.dependency.Dependency,
     *      org.owasp.dependencycheck.dependency.Dependency, java.util.Set)
     */
    @Override
    protected boolean evaluateDependencies(final Dependency dependency, final Dependency nextDependency,
            final Set<Dependency> dependenciesToRemove) {

        if (this.cpeMatch(dependency, nextDependency) && this.namesMatch(dependency, nextDependency)) {
            this.mergeVirtualDependencies(dependency, nextDependency, dependenciesToRemove);
        }

        return false;

    }

    /**
     * Method that merge two dependencies that have the same CPE and name into one single dependency to avoid
     * duplications. This also merges the related projects of the dependencies.
     *
     * @param dependency the dependency to compare
     * @param relatedDependency the related dependency to compare
     * @param dependenciesToRemove list of dependencies that will be removed
     */
    protected void mergeVirtualDependencies(final Dependency dependency, final Dependency relatedDependency,
            final Set<Dependency> dependenciesToRemove) {

        // Add project references
        dependency.addAllProjectReferences(relatedDependency.getProjectReferences());
        dependency.addRelatedDependency(relatedDependency);
        relatedDependency.getRelatedDependencies().forEach(d -> {
            dependency.addRelatedDependency(d);
            relatedDependency.removeRelatedDependencies(d);
        });

        if (dependenciesToRemove != null) {
            dependenciesToRemove.add(relatedDependency);
        }

    }

    /**
     * Returns true if the CPE identifiers in the two supplied dependencies are equal.
     *
     * @param dependency1 a dependency2 to compare
     * @param dependency2 a dependency2 to compare
     * @return true if the identifiers in the two supplied dependencies are equal
     */
    private boolean cpeMatch(final Dependency dependency1, final Dependency dependency2) {
        if ((dependency1 == null) || (dependency1.getIdentifiers() == null) || (dependency2 == null)
                || (dependency2.getIdentifiers() == null)) {
            return false;
        }
        boolean matches = false;
        int cpeCount1 = 0;
        int cpeCount2 = 0;
        for (Identifier i : dependency1.getIdentifiers()) {
            if ("cpe".equals(i.getType())) {
                cpeCount1 += 1;
            }
        }
        for (Identifier i : dependency2.getIdentifiers()) {
            if ("cpe".equals(i.getType())) {
                cpeCount2 += 1;
            }
        }
        if ((cpeCount1 > 0) && (cpeCount1 == cpeCount2)) {
            for (Identifier i : dependency1.getIdentifiers()) {
                if ("cpe".equals(i.getType())) {
                    matches |= dependency2.getIdentifiers().contains(i);
                    if (!matches) {
                        break;
                    }
                }
            }
        }
        DependencyCPEBundlingAnalyzer.LOGGER.debug("IdentifiersMatch={} ({}, {})", matches, dependency1.getName(),
                dependency2.getName());
        return matches;
    }

    /**
     * Returns true if the names of the two dependencies are sufficiently similar.
     *
     * @param dependency1 a dependency2 to compare
     * @param dependency2 a dependency2 to compare
     * @return true if the identifiers in the two supplied dependencies are equal
     */
    private boolean namesMatch(final Dependency dependency1, final Dependency dependency2) {
        if ((dependency1 == null) || (dependency1.getName() == null) || (dependency2 == null)
                || (dependency2.getName() == null)) {
            return false;
        }

        return dependency1.getName().equals(dependency2.getName());
    }

}
