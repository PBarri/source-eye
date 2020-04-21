package com.pbarrientos.sourceeye.scanners;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;

/**
 * Class that parses Maven projects and extracts dependencies. To find the dependencies, it will execute the command:
 * <p>
 * <code>
 * mvn dependency:tree
 * </code>
 * </p>
 *
 * @author Pablo Barrientos
 */
public class MavenDependencyScanner implements DependencyScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDependencyScanner.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Invoker invoker;

    /**
     * Constructor that initializes the invoker
     *
     * @param properties the project properties
     * @throws Exception in case there is an error
     */
    public MavenDependencyScanner(final SourceEyeProperties properties) throws Exception {
        SourceEyeProperties.Maven mavenProperties = properties.getMaven();

        this.invoker = new DefaultInvoker();

        if (StringUtils.hasText(mavenProperties.getHome())) {
            this.invoker.setMavenHome(new File(mavenProperties.getHome()));
        }
    }

    /**
     * @see com.pbarrientos.sourceeye.scanners.DependencyScanner#getDependencyTree(java.io.File)
     */
    @Override
    public Path getDependencyTree(final File pomFile) throws Exception {
        String timestamp = LocalDateTime.now().format(this.formatter);
        Path logFile = Files.createTempFile("mavenlog_", timestamp);

        MavenOutputHandler outputHandler = new MavenOutputHandler(logFile);
        this.invoker.setOutputHandler(outputHandler);

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setPomFile(pomFile);
        request.setGoals(Arrays.asList("dependency:tree"));

        try {
            MavenDependencyScanner.LOGGER.debug("Scanning project for dependencies:");
            final long scanStart = System.currentTimeMillis();
            InvocationResult result = this.invoker.execute(request);
            final long scanDurationMilli = System.currentTimeMillis() - scanStart;
            final long scanDurationSeconds = TimeUnit.MILLISECONDS.toSeconds(scanDurationMilli);
            MavenDependencyScanner.LOGGER.debug("Scan Completed in ({} seconds)", scanDurationSeconds);
            if (result.getExitCode() != 0) {
                // There is an error with the build
                // TODO: Log this !!
            }
        } catch (MavenInvocationException e) {
            // TODO catch and log error
            e.printStackTrace();
        }

        return logFile;
    }

}
