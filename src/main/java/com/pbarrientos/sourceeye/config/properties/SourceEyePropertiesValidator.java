package com.pbarrientos.sourceeye.config.properties;

import org.quartz.CronExpression;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Class that validates the properties. Constraints on the properties can be
 * found on the project documentation
 *
 * @author Pablo Barrientos
 */
public class SourceEyePropertiesValidator implements Validator {

    @Override
    public boolean supports(final Class<?> type) {
        return type == SourceEyeProperties.class;
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        SourceEyeProperties properties = (SourceEyeProperties) target;

        this.validateAnalysisSettings(properties.getAnalysis(), errors);
        this.validateNvdSettings(properties.getNvd(), errors);
        this.validateDatabaseSettings(properties.getDatabase(), errors);
        this.validateGithubSettings(properties.getGithub(), errors);
        this.validateGitlabSettings(properties.getGitlab(), errors);
        this.validateLocalRepositorySettings(properties.getLocalRepository(), errors);
        this.validateApiSettings(properties.getApi(), errors);
        this.validateLogSettings(properties.getLog(), errors);
        this.validateMavenSettings(properties.getMaven(), errors);
        this.validateProxySettings(properties.getProxy(), errors);
    }

    private void validateAnalysisSettings(final SourceEyeProperties.Analysis properties, final Errors errors) {
        if (properties.isEnabled()) {
            if (!CronExpression.isValidExpression(properties.getPeriodicity())) {
                errors.rejectValue("analysis.periodicity", "periodicity must be a valid cron expression");
            }
        }
    }

    private void validateNvdSettings(final SourceEyeProperties.NVD properties, final Errors errors) {
        if (properties.getValidHours() == null) {
            errors.rejectValue("nvd.validHours", "validForHours property must not be null");
        }
    }

    private void validateDatabaseSettings(final SourceEyeProperties.Database properties, final Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "database.connection", "Database connection cannot be empty");
        ValidationUtils.rejectIfEmpty(errors, "database.driverClassName", "Database driver cannot be empty");
        ValidationUtils.rejectIfEmpty(errors, "database.username", "Database username cannot be empty");
        ValidationUtils.rejectIfEmpty(errors, "database.password", "Database password cannot be empty");
    }

    private void validateGithubSettings(final SourceEyeProperties.Github properties, final Errors errors) {
        if (properties.isScanEnabled()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "github.username", "The username must not be empty");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "github.password", "The password must not be empty");
        }
    }

    private void validateGitlabSettings(final SourceEyeProperties.Gitlab properties, final Errors errors) {
        if (properties.isScanEnabled()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "gitlab.username", "The username must not be empty");
            if (!StringUtils.hasText(properties.getPassword()) && !StringUtils.hasText(properties.getApiToken())) {
                errors.rejectValue("gitlab.password", "Password and API Token cannot be null");
                errors.rejectValue("gitlab.apiToken", "Password and API Token cannot be null");
            }
        }
    }

    private void validateLocalRepositorySettings(final SourceEyeProperties.LocalRepository properties,
        final Errors errors) {

        if (properties.isScanEnabled()) {
            ValidationUtils.rejectIfEmpty(errors, "localRepository.path", "Path cannot be null for Local Repository");
        }
    }

    private void validateApiSettings(final SourceEyeProperties.Api properties, final Errors errors) {
        // Nothing to validate
    }

    private void validateLogSettings(final SourceEyeProperties.Log properties, final Errors errors) {
        if (properties.getSyslog().isEnabled()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "log.syslog.address",
                "The syslog address cannot be empty when syslog is enabled");
            if (properties.getSyslog().getPort() == null) {
                errors.rejectValue("log.syslog.port", "Port must be configured when syslog is enabled");
            }
        }
    }

    private void validateProxySettings(final SourceEyeProperties.Proxy properties, final Errors errors) {
        // Nothing to validate
    }

    private void validateMavenSettings(final SourceEyeProperties.Maven properties, final Errors errors) {
        // Nothing to validate
    }

}
