package com.pbarrientos.sourceeye.engine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pbarrientos.sourceeye.data.model.Vulnerability;

/**
 * Class that will handle the logs about discovered vulnerabilities
 *
 * @author Pablo Barrientos
 */
public class SourceEyeEngineLogger {

    /**
     * The logger to use
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceEyeEngineLogger.class);

    /**
     * The format of the logs.
     */
    private static final String FORMAT = "%s sourceeye : { \"project\": \"%s\", \"cve\": \"%s\", \"cwe\": \"%s\", \"cvss\": \"%f\" } ";

    /**
     * The formatter for the timestamp
     */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Method that will log a {@link Vulnerability} using the format defined above.
     *
     * @param vuln the vulnerability to log
     * @since 0.1.0
     */
    public void logVulnerability(final Vulnerability vuln) {

        String log = String.format(SourceEyeEngineLogger.FORMAT,
            LocalDateTime.now().format(SourceEyeEngineLogger.formatter), vuln.getProject().getQualifiedName(),
            vuln.getCve(), vuln.getCwe(), vuln.getCvssScore());

        SourceEyeEngineLogger.LOGGER.info(log);
    }

}
