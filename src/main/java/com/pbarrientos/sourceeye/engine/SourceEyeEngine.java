package com.pbarrientos.sourceeye.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.Analyzer;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.data.update.exception.UpdateException;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.exception.InitializationException;
import org.owasp.dependencycheck.exception.NoDataException;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;
import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.data.model.Vulnerability;
import com.pbarrientos.sourceeye.data.services.CPEMemoryService;
import com.pbarrientos.sourceeye.data.services.ProjectService;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;
import com.pbarrientos.sourceeye.git.GitService;
import com.pbarrientos.sourceeye.git.GithubService;
import com.pbarrientos.sourceeye.git.GitlabService;
import com.pbarrientos.sourceeye.git.LocalGitService;
import com.pbarrientos.sourceeye.scanners.GradleDependencyScanner;
import com.pbarrientos.sourceeye.scanners.MavenDependencyScanner;
import com.pbarrientos.sourceeye.utils.ProjectType;
import com.pbarrientos.sourceeye.utils.ProjectUtils;
import com.pbarrientos.sourceeye.utils.VulnerabilityUtils;

/**
 * Class based on {@link Engine} with the following extensions:
 * <ul>
 * <li>A provided {@link CveDB}</li>
 * </ul>
 *
 * @author Pablo Barrientos
 */
@Component
public class SourceEyeEngine extends Engine {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceEyeEngine.class);

    /**
     * The database
     */
    @Autowired
    private CveDB database;

    /**
     * The CPE service
     */
    @Autowired
    private CPEMemoryService cpeService;

    /**
     * The local repository service
     */
    @Autowired
    private LocalGitService localGitService;

    /**
     * The Gitlab service
     */
    @Autowired
    private GitlabService gitlabService;

    /**
     * The Github service
     */
    @Autowired
    private GithubService githubService;

    /**
     * The project properties
     */
    @Autowired
    private SourceEyeProperties properties;

    /**
     * The project service
     */
    @Autowired
    private ProjectService projectService;

    /**
     * Map containing the scanned projects and the dependencies associated to them
     */
    private final Map<Project, List<Dependency>> dependenciesByProject;

    /**
     * The logger
     */
    private final SourceEyeEngineLogger engineLogger = new SourceEyeEngineLogger();

    /**
     * The maven project scanner
     */
    private MavenDependencyScanner mavenScanner;

    /**
     * The gradle project scanner
     */
    private GradleDependencyScanner gradleScanner;

    /**
     * Constructor that receives the settings and initializes the engine
     *
     * @param settings the settings
     */
    @Autowired
    public SourceEyeEngine(final Settings settings) {
        super(Thread.currentThread().getContextClassLoader(), Mode.STANDALONE, settings);
        this.dependenciesByProject = new HashMap<>();
    }

    /**
     * Method executed after initialization that starts the instances of the
     * different scanners
     *
     * @throws Exception in case there is an error
     * @since 0.1.0
     */
    @PostConstruct
    protected void initializeScanners() throws Exception {
        this.mavenScanner = new MavenDependencyScanner(this.properties);
        this.gradleScanner = new GradleDependencyScanner();
    }

    /**
     * Main method of the engine, that will orchestrate the scanning and analysis of
     * the projects.
     *
     * @throws SourceEyeServiceException in case there is an error in the services
     * @throws ExceptionCollection       in case there is an error during the
     *                                   analysis
     * @since 0.1.0
     */
    public synchronized void run() throws SourceEyeServiceException, ExceptionCollection {
        this.scan();
        this.analyzeDependencies();
        this.writeLogs();
        this.clear();
    }

    /**
     * Write the discovered vulnerabilities to the application log
     *
     * @since 0.1.0
     */
    public void writeLogs() {
        Map<Project, List<Vulnerability>> vulnerableDependenciesByProject = this.getVulnerableDependenciesByProject();

        List<Vulnerability> vulns = new ArrayList<>();
        for (Entry<Project, List<Vulnerability>> entry : vulnerableDependenciesByProject.entrySet()) {
            entry.getValue().forEach(v -> v.setProject(entry.getKey()));
            vulns.addAll(entry.getValue());
        }

        vulns.forEach(this.engineLogger::logVulnerability);
    }

    /**
     * Starts the scanning of local, github and gitlab projects and extracts its
     * dependencies.
     * <p>
     * This delegates the scanning of the project to the method
     * {@link SourceEyeEngine#scan(GitService)}.
     * </p>
     *
     * @return a list of discovered dependencies
     * @since 0.1.0
     */
    public List<Dependency> scan() {
        List<Dependency> dependencies = new ArrayList<>();

        if (this.properties.getLocalRepository().isScanEnabled()) {
            try {
                List<Dependency> localDependencies = this.scan(this.localGitService);
                dependencies.addAll(localDependencies);
            } catch (SourceEyeServiceException e) {
                SourceEyeEngine.LOGGER.error("There was an error scanning projects in local repository", e);
            }
        }

        if (this.properties.getGithub().isScanEnabled()) {
            try {
                List<Dependency> githubDependencies = this.scan(this.githubService);
                dependencies.addAll(githubDependencies);
            } catch (SourceEyeServiceException e) {
                SourceEyeEngine.LOGGER.error("There was an error scanning projects in github repository", e);
            }
        }

        if (this.properties.getGitlab().isScanEnabled()) {
            try {
                List<Dependency> gitlabDependencies = this.scan(this.gitlabService);
                dependencies.addAll(gitlabDependencies);
            } catch (SourceEyeServiceException e) {
                SourceEyeEngine.LOGGER.error("There was an error scanning projects in gitlab repository", e);
            }
        }

        this.setDependencies(dependencies);
        return dependencies;
    }

    /**
     * This handles the scanning of the different projects. The process is the
     * following:
     * <ol>
     * <li>Find the projects available to the given {@link GitService}</li>
     * <li>Retrieves the root path and the type of the project</li>
     * <li>If the project type is known (Maven or Gradle), scan the project for
     * dependencies using
     * {@link SourceEyeEngine#scanProject(Path, ProjectType)}</li>
     * <li>Add the project to the dependencies references</li>
     * <li>Add the dependencies to the engine dependencies by project map</li>
     * <li>Return the discovered dependencies</li>
     * <li>If necessary, deletes the temporary files created</li>
     * </ol>
     *
     * @param service the git service
     * @return a list of dependencies
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    protected List<Dependency> scan(final GitService service) throws SourceEyeServiceException {
        List<Project> projects = service.getProjects();
        List<Dependency> dependencies = new ArrayList<>();

        for (Project p : projects) {
            Path projectRoot = null;
            try {
                projectRoot = service.getProjectRoot(p);
                ProjectType type = ProjectUtils.getProjectType(projectRoot);

                if (!ProjectType.UNKNOWN.equals(type)) {
                    final long scanStart = System.currentTimeMillis();
                    SourceEyeEngine.LOGGER.info("Scanning project {} for dependencies", p.getName());
                    List<Dependency> projectDependencies = this.scanProject(projectRoot, type);
                    final long scanDurationMillis = System.currentTimeMillis() - scanStart;
                    final long scanDurationSeconds = TimeUnit.MILLISECONDS.toSeconds(scanDurationMillis);
                    SourceEyeEngine.LOGGER.info("Scanning finalized in {} seconds. Found {} dependencies",
                        scanDurationSeconds, projectDependencies.size());
                    if (!CollectionUtils.isEmpty(projectDependencies)) {
                        projectDependencies.forEach(d -> d.addProjectReference(p.getName()));
                        dependencies.addAll(projectDependencies);
                        this.dependenciesByProject.put(p, dependencies);
                    } else {
                        this.dependenciesByProject.put(p, new ArrayList<>());
                    }
                }
            } catch (Exception e) {
                SourceEyeEngine.LOGGER.error("There was an error processing project {}. It will be ignored",
                    p.getName(), e);
            } finally {
                if (service.deleteFilesAfterScan() && (projectRoot != null) && projectRoot.toFile().exists()
                    && projectRoot.toFile().isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(projectRoot.toFile());
                    } catch (IOException e) {
                        SourceEyeEngine.LOGGER.error("There was an error while trying to delete the folder {}",
                            projectRoot.toAbsolutePath().toString(), e);
                    }
                }
            }
        }

        return dependencies;
    }

    /**
     * Scans a project for finding its dependencies. Depending on the project type,
     * it delegates the dependencies scanning to either
     * {@link MavenDependencyScanner} or {@link GradleDependencyScanner}.
     *
     * @param project the project to scan
     * @param type    the type of the project
     * @return a list of dependencies
     * @throws Exception in case there is an error
     * @since 0.1.0
     */
    public List<Dependency> scanProject(final Path project, final ProjectType type) throws Exception {
        List<Dependency> projectDependencies = new ArrayList<>();

        if (ProjectType.MAVEN.equals(type)) {
            try {
                File pomFile = ProjectUtils.getParentPomFile(project);
                projectDependencies = this.mavenScanner.getDependencies(pomFile);
            } catch (Exception e) {
                // TODO Custom exception, throw it
                e.printStackTrace();
                throw e;
            }
        } else if (ProjectType.GRADLE.equals(type)) {
            try {
                File buildFile = ProjectUtils.getParentBuildFile(project);
                projectDependencies = this.gradleScanner.getDependencies(buildFile);
            } catch (Exception e) {
                // TODO: Custom exception
                e.printStackTrace();
                throw e;
            }
        }

        return projectDependencies;
    }

    /**
     * Same as overridden method, but delete code that would have opened a new
     * instance of the database and created a new CPE Lucene index. Also, this will
     * save the discovered vulnerabilities at the end of the analysis.
     *
     * @see org.owasp.dependencycheck.Engine#analyzeDependencies()
     */
    @Override
    public void analyzeDependencies() throws ExceptionCollection {
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

        // need to ensure that data exists
        try {
            this.ensureDataExists();
        } catch (NoDataException ex) {
            this.throwFatalExceptionCollection("Unable to continue dependency-check analysis.", ex, exceptions);
        }

        SourceEyeEngine.LOGGER.debug(
            "\n----------------------------------------------------\nBEGIN ANALYSIS\n----------------------------------------------------");
        SourceEyeEngine.LOGGER.info("Analysis Started");
        final long analysisStart = System.currentTimeMillis();

        // analysis phases
        for (AnalysisPhase phase : this.getMode().getPhases()) {
            final List<Analyzer> analyzerList = this.getAnalyzers(phase);

            for (final Analyzer analyzer : analyzerList) {

                final long analyzerStart = System.currentTimeMillis();
                try {
                    this.initializeAnalyzer(analyzer);
                } catch (InitializationException ex) {
                    exceptions.add(ex);
                    if (ex.isFatal()) {
                        continue;
                    }
                }

                if (analyzer.isEnabled()) {
                    this.executeAnalysisTasks(analyzer, exceptions);

                    final long analyzerDurationMillis = System.currentTimeMillis() - analyzerStart;
                    final long analyzerDurationSeconds = TimeUnit.MILLISECONDS.toSeconds(analyzerDurationMillis);
                    SourceEyeEngine.LOGGER.info("Finished {} ({} seconds)", analyzer.getName(),
                        analyzerDurationSeconds);
                } else {
                    SourceEyeEngine.LOGGER.debug("Skipping {} (not enabled)", analyzer.getName());
                }
            }

            this.closeAnalyzers(analyzerList);
        }

        SourceEyeEngine.LOGGER.debug(
            "\n----------------------------------------------------\nEND ANALYSIS\n----------------------------------------------------");
        final long analysisDurationSeconds = TimeUnit.MILLISECONDS
            .toSeconds(System.currentTimeMillis() - analysisStart);
        SourceEyeEngine.LOGGER.info("Analysis Complete ({} seconds)", analysisDurationSeconds);
        if (!exceptions.isEmpty()) {
            throw new ExceptionCollection("One or more exceptions occurred during dependency-check analysis",
                exceptions);
        }

        // Save discovered vulnerabilities in the database
        this.saveVulnerabilities();
    }

    /**
     * Returns a list with all the dependencies that contains vulnerabilities
     *
     * @return the vulnerable dependencies
     * @since 0.1.0
     */
    public List<Dependency> getVulnerableDependencies() {
        List<Dependency> dependencies = Arrays.asList(this.getDependencies());

        return dependencies.stream().filter(dep -> !CollectionUtils.isEmpty(dep.getVulnerabilities()))
            .collect(Collectors.toList());
    }

    /**
     * Returns a Map with the vulnerabilities grouped by Project
     *
     * @return the vulnerabilities by project
     * @since 0.1.0
     */
    public Map<Project, List<Vulnerability>> getVulnerableDependenciesByProject() {
        Map<Project, List<Vulnerability>> res = new HashMap<>();
        List<Dependency> vulnerableDependencies = this.getVulnerableDependencies();

        for (Entry<Project, List<Dependency>> entry : this.dependenciesByProject.entrySet()) {
            List<Dependency> vulns = new ArrayList<>(entry.getValue());
            vulns.retainAll(vulnerableDependencies);

            if (!CollectionUtils.isEmpty(vulns)) {
                // Convert vulnerability to source eye model
                List<Vulnerability> v = vulns.stream().map(VulnerabilityUtils::mapDependencyVulnerabilities)
                    .flatMap(Collection::stream).collect(Collectors.toList());
                res.put(entry.getKey(), v);
            }
        }

        return res;
    }

    /**
     * Saves the vulnerabilities in the database
     *
     * @since 0.1.0
     */
    private void saveVulnerabilities() {
        Map<Project, List<Vulnerability>> vulnerableDependenciesByProject = this.getVulnerableDependenciesByProject();

        for (Entry<Project, List<Vulnerability>> entry : vulnerableDependenciesByProject.entrySet()) {
            for (Vulnerability vuln : entry.getValue()) {
                entry.getKey().addVulnerability(vuln);
            }
            try {
                this.projectService.save(entry.getKey());
            } catch (SourceEyeServiceException e) {
                // TODO Log y demas
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes all the opened analyzers
     *
     * @param analyzerList the list of analyzers
     * @since 0.1.0
     */
    private void closeAnalyzers(final List<Analyzer> analyzerList) {
        for (Analyzer a : analyzerList) {
            this.closeAnalyzer(a);
        }
    }

    /**
     * Checks the CPE Index to ensure documents exists. If none exist a
     * NoDataException is thrown.
     *
     * @throws NoDataException thrown if no data exists in the CPE Index
     * @since 0.1.0
     */
    private void ensureDataExists() throws NoDataException {
        if ((this.database == null) || !this.database.dataExists()) {
            throw new NoDataException("No documents exist");
        }
    }

    /**
     * Constructs and throws a fatal exception collection.
     *
     * @param message    the exception message
     * @param throwable  the cause
     * @param exceptions a collection of exception to include
     * @throws ExceptionCollection a collection of exceptions that occurred during
     *                             analysis
     */
    private void throwFatalExceptionCollection(final String message, final Throwable throwable,
        final List<Throwable> exceptions) throws ExceptionCollection {
        SourceEyeEngine.LOGGER.error("{}\n\n{}", throwable.getMessage(), message);
        SourceEyeEngine.LOGGER.debug("", throwable);
        exceptions.add(throwable);
        throw new ExceptionCollection(message, exceptions, true);
    }

    /**
     * @deprecated the database no longer needs to be opened in the engine
     * @see org.owasp.dependencycheck.Engine#openDatabase()
     */
    @Override
    @Deprecated
    public void openDatabase() throws DatabaseException {
        SourceEyeEngine.LOGGER.debug("This method does nothing");
    }

    /**
     * @deprecated the database no longer needs to be opened in the engine
     * @see org.owasp.dependencycheck.Engine#openDatabase(boolean, boolean)
     */
    @Override
    @Deprecated
    public void openDatabase(final boolean readOnly, final boolean lockRequired) throws DatabaseException {
        SourceEyeEngine.LOGGER.debug("This method does nothing");

    }

    /**
     * @deprecated Database updates will be managed in the updater service
     * @throws UpdateException   cannot be thrown
     * @throws DatabaseException cannot be thrown
     */
    @Override
    @Deprecated
    public void doUpdates() throws UpdateException, DatabaseException {
        SourceEyeEngine.LOGGER.debug(
            "This method is deprecated and will do nothing. Database updates will be managed in the updater service");
    }

    /**
     * @deprecated Database updates will be managed in the updater service
     * @param remainOpen not used
     * @throws UpdateException   cannot be thrown
     * @throws DatabaseException cannot be thrown
     */
    @Override
    @Deprecated
    public void doUpdates(final boolean remainOpen) throws UpdateException, DatabaseException {
        SourceEyeEngine.LOGGER.debug(
            "This method is deprecated and will do nothing. Database updates will be managed in the updater service");
    }

    /**
     * @see org.owasp.dependencycheck.Engine#getDatabase()
     */
    @Override
    public CveDB getDatabase() {
        return this.database;
    }

    /**
     * @return the cpeService
     */
    public CPEMemoryService getCpeService() {
        return this.cpeService;
    }

    /**
     * @return the properties
     */
    public SourceEyeProperties getProperties() {
        return this.properties;
    }

    /**
     * Clears the map of the vulnerabilities grouped by project
     *
     * @since 0.1.0
     */
    private void clear() {
        this.dependenciesByProject.clear();
    }

}
