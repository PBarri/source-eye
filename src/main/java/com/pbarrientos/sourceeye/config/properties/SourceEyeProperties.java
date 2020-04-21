package com.pbarrientos.sourceeye.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Class that models the properties from the configuration yml files. These
 * properties will be validated against {@link SourceEyePropertiesValidator}.
 * The meaning of all the properties can be found on the project documentation.
 *
 * @author Pablo Barrientos
 */
@ConfigurationProperties(prefix = "sourceeye")
@Validated
public class SourceEyeProperties {

    // Properties -------------------------------------------------

    private final Analysis analysis = new Analysis();

    private final NVD nvd = new NVD();

    private final Database database = new Database();

    private final Github github = new Github();

    private final Gitlab gitlab = new Gitlab();

    private final LocalRepository localRepository = new LocalRepository();

    private final Api api = new Api();

    private final Log log = new Log();

    private final Proxy proxy = new Proxy();

    private final Maven maven = new Maven();

    // Getters -------------------------------------------------------

    public Analysis getAnalysis() {
        return this.analysis;
    }

    public NVD getNvd() {
        return this.nvd;
    }

    public Database getDatabase() {
        return this.database;
    }

    public Github getGithub() {
        return this.github;
    }

    public Gitlab getGitlab() {
        return this.gitlab;
    }

    public LocalRepository getLocalRepository() {
        return this.localRepository;
    }

    public Api getApi() {
        return this.api;
    }

    public Log getLog() {
        return this.log;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public Maven getMaven() {
        return this.maven;
    }

    // Static classes --------------------------------------------------

    public static class Analysis {

        private boolean enabled;

        private String periodicity;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        public String getPeriodicity() {
            return this.periodicity;
        }

        public void setPeriodicity(final String periodicity) {
            this.periodicity = periodicity;
        }

    }

    public static class NVD {

        private Integer validHours;

        private boolean autoUpdate;

        public Integer getValidHours() {
            return this.validHours;
        }

        public void setValidHours(final Integer validHours) {
            this.validHours = validHours;
        }

        public boolean isAutoUpdate() {
            return this.autoUpdate;
        }

        public void setAutoUpdate(final boolean autoUpdate) {
            this.autoUpdate = autoUpdate;
        }

    }

    public static class Database {

        private String connection;

        private String driverClassName;

        private String username;

        private String password;

        public String getConnection() {
            return this.connection;
        }

        public void setConnection(final String connection) {
            this.connection = connection;
        }

        public String getDriverClassName() {
            return this.driverClassName;
        }

        public void setDriverClassName(final String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getUsername() {
            return this.username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

    }

    public static class Github {

        private boolean scanEnabled;

        private String username;

        private String password;

        public boolean isScanEnabled() {
            return this.scanEnabled;
        }

        public void setScanEnabled(final boolean scanEnabled) {
            this.scanEnabled = scanEnabled;
        }

        public String getUsername() {
            return this.username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

    }

    public static class Gitlab {

        private boolean scanEnabled;

        private String url;

        private String username;

        private String password;

        private String apiToken;

        public boolean isScanEnabled() {
            return this.scanEnabled;
        }

        public void setScanEnabled(final boolean scanEnabled) {
            this.scanEnabled = scanEnabled;
        }

        public String getUsername() {
            return this.username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public String getApiToken() {
            return this.apiToken;
        }

        public void setApiToken(final String apiToken) {
            this.apiToken = apiToken;
        }

        public String getUrl() {
            return this.url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

    }

    public static class LocalRepository {

        private boolean scanEnabled;

        private String path;

        public boolean isScanEnabled() {
            return this.scanEnabled;
        }

        public void setScanEnabled(final boolean scanEnabled) {
            this.scanEnabled = scanEnabled;
        }

        public String getPath() {
            return this.path;
        }

        public void setPath(final String path) {
            this.path = path;
        }

    }

    public static class Api {

        private boolean expose;

        private String username;

        private String password;

        private String bindAddress;

        private Integer port;

        public boolean isExpose() {
            return this.expose;
        }

        public void setExpose(final boolean expose) {
            this.expose = expose;
        }

        public String getUsername() {
            return this.username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public String getBindAddress() {
            return this.bindAddress;
        }

        public void setBindAddress(final String bindAddress) {
            this.bindAddress = bindAddress;
        }

        public Integer getPort() {
            return this.port;
        }

        public void setPort(final Integer port) {
            this.port = port;
        }

    }

    public static class Log {

        private String file;

        private String vulnerability;

        private String level;

        private final Syslog syslog = new Syslog();

        public String getFile() {
            return this.file;
        }

        public void setFile(final String file) {
            this.file = file;
        }

        public String getVulnerability() {
            return this.vulnerability;
        }

        public void setVulnerability(final String vulnerability) {
            this.vulnerability = vulnerability;
        }

        public String getLevel() {
            return this.level;
        }

        public void setLevel(final String level) {
            this.level = level;
        }

        public Syslog getSyslog() {
            return this.syslog;
        }

        public static class Syslog {

            private boolean enabled;

            private String address;

            private Integer port;

            public boolean isEnabled() {
                return this.enabled;
            }

            public void setEnabled(final boolean enabled) {
                this.enabled = enabled;
            }

            public String getAddress() {
                return this.address;
            }

            public void setAddress(final String address) {
                this.address = address;
            }

            public Integer getPort() {
                return this.port;
            }

            public void setPort(final Integer port) {
                this.port = port;
            }

        }

    }

    public static class Proxy {

        private String host;

        private Integer port;

        private String username;

        private String password;

        public String getHost() {
            return this.host;
        }

        public void setHost(final String host) {
            this.host = host;
        }

        public Integer getPort() {
            return this.port;
        }

        public void setPort(final Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return this.username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

    }

    public static class Maven {

        private String home;

        public String getHome() {
            return this.home;
        }

        public void setHome(final String home) {
            this.home = home;
        }

    }

}
