package com.pbarrientos.sourceeye.config;

import javax.sql.DataSource;

import org.owasp.dependencycheck.data.cpe.IndexException;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.utils.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.pbarrientos.sourceeye.analyzers.DependencyCPEBundlingAnalyzer;
import com.pbarrientos.sourceeye.analyzers.SourceEyeCPEAnalyzer;
import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;
import com.pbarrientos.sourceeye.data.services.CPEMemoryService;

/**
 * Configuration class that handles the creation of the different datasources
 * and the engine of Source Eye
 *
 * @author Pablo Barrientos
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("com.pbarrientos.sourceeye.data.repositories")
@EntityScan("com.pbarrientos.sourceeye.data.model")
public class DataConfiguration {

    /**
     * Database properties
     */
    private final SourceEyeProperties.Database dbProperties;

    /**
     * NVD properties
     */
    private final SourceEyeProperties.NVD nvdProperties;

    /**
     * Proxy properties
     */
    private final SourceEyeProperties.Proxy proxyProperties;

    /**
     * Constructor that initializes the different used properties
     *
     * @param properties the project configuration properties
     */
    @Autowired
    public DataConfiguration(final SourceEyeProperties properties) {
        this.dbProperties = properties.getDatabase();
        this.nvdProperties = properties.getNvd();
        this.proxyProperties = properties.getProxy();
    }

    /**
     * Bean that creates the principal datasource.
     *
     * @return the datasource
     * @since 0.1.0
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(this.dbProperties.getDriverClassName());
        dataSource.setUrl(this.dbProperties.getConnection());
        dataSource.setUsername(this.dbProperties.getUsername());
        dataSource.setPassword(this.dbProperties.getPassword());

        return dataSource;
    }

    /**
     * Creates the NVD Database bean
     *
     * @return the database
     */
    @Bean
    public CveDB cveDatabase() {
        return new CveDB(this.settings());
    }

    /**
     * Creates and initializes the {@link CPEMemoryService} that will store all the
     * CPEs in an in-memory Lucene index
     *
     * @return the CPE Memory Service
     * @throws IndexException in case there is an error initializing the index
     * @since 0.1.0
     */
    @Bean
    public CPEMemoryService cpeService() throws IndexException {
        CPEMemoryService cpeService = new CPEMemoryService(this.cveDatabase());
        return cpeService;
    }

    /**
     * Bean that will set and store the settings required by dependency-checker
     *
     * @return the settings
     * @since 0.1.0
     */
    @Bean
    public Settings settings() {
        Settings settings = new Settings();

        // Set database connection properties
        settings.setString(Settings.KEYS.DB_CONNECTION_STRING, this.dbProperties.getConnection());
        settings.setString(Settings.KEYS.DB_DRIVER_NAME, this.dbProperties.getDriverClassName());
        settings.setString(Settings.KEYS.DB_USER, this.dbProperties.getUsername());
        settings.setString(Settings.KEYS.DB_PASSWORD, this.dbProperties.getPassword());

        // Set NVD properties
        settings.setIntIfNotNull(Settings.KEYS.CVE_CHECK_VALID_FOR_HOURS, 0);

        // Set proxy properties
        settings.setStringIfNotEmpty(Settings.KEYS.PROXY_SERVER, this.proxyProperties.getHost());
        settings.setIntIfNotNull(Settings.KEYS.PROXY_PORT, this.proxyProperties.getPort());
        settings.setStringIfNotEmpty(Settings.KEYS.PROXY_USERNAME, this.proxyProperties.getUsername());
        settings.setStringIfNotEmpty(Settings.KEYS.PROXY_PASSWORD, this.proxyProperties.getPassword());

        // Disable not required analyzers
        settings.setBoolean(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_ARTIFACTORY_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_ASSEMBLY_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_AUTOCONF_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_BUNDLE_AUDIT_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_CENTRAL_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_CMAKE_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_COCOAPODS_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_COMPOSER_LOCK_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_FILE_NAME_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_CPE_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_MSBUILD_PROJECT_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_NEXUS_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_NODE_PACKAGE_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_NSP_PACKAGE_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_NUSPEC_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_OPENSSL_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_PYTHON_DISTRIBUTION_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_PYTHON_PACKAGE_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_RETIRED_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_RETIREJS_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_RUBY_GEMSPEC_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_SWIFT_PACKAGE_MANAGER_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_JAR_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_DEPENDENCY_MERGING_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_FALSE_POSITIVE_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_DEPENDENCY_BUNDLING_ENABLED, false);
        settings.setBoolean(Settings.KEYS.ANALYZER_NUGETCONF_ENABLED, false);

        // Enable used analyzers
        settings.setBoolean(SourceEyeCPEAnalyzer.ANALYZER_SE_CPE_ENABLED, true);
        settings.setBoolean(DependencyCPEBundlingAnalyzer.ANALYZER_SE_CPE_ENABLED, true);
        settings.setBoolean(Settings.KEYS.ANALYZER_NVD_CVE_ENABLED, true);

        return settings;
    }

}
