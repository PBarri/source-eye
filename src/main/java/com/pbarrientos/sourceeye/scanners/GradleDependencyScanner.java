package com.pbarrientos.sourceeye.scanners;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

/**
 * Class responsible of executing Gradle to find the dependencies of a project.
 * It uses the gradle wrapper configured in the projects. The executed command
 * is:
 * <p>
 * <code>
 * ./gradlew dependencies --configuration compile
 * </code>
 * </p>
 *
 * @author Pablo Barrientos
 */
public class GradleDependencyScanner implements DependencyScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradleDependencyScanner.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * The command line
     */
    private Commandline cli;

    public GradleDependencyScanner() throws Exception {
        this.clearCli();
    }

    /**
     * In this case, this will only work in single module projects
     *
     * @see com.pbarrientos.sourceeye.scanners.DependencyScanner#getDependencyTree(java.io.File)
     */
    @Override
    public Path getDependencyTree(final File buildFile) throws Exception {
        String timestamp = LocalDateTime.now().format(this.formatter);
        Path logFile = Files.createTempFile("gradlelog_", timestamp);
        File workingDirectory = buildFile.getParentFile();

        // TODO Check inputs and so on

        // Set log file
        GradleOutputHandler outputHandler = new GradleOutputHandler(logFile);

        File exec = this.getGradleWrapper(workingDirectory);

        if ((exec != null) && exec.exists() && exec.canExecute()) {
            GradleDependencyScanner.LOGGER.debug("Found executable for gradle wrapper: {}", exec.getAbsolutePath());
            this.cli.setExecutable(exec.getAbsolutePath());
            this.cli.setWorkingDirectory(workingDirectory.getCanonicalPath());
            int result = Integer.MIN_VALUE;
            try {
                GradleDependencyScanner.LOGGER.debug("Executing gradlew dependencies --configuration compile:");
                final long scanStart = System.currentTimeMillis();
                // Execute gradle command line
                result = CommandLineUtils.executeCommandLine(this.cli, outputHandler, null, 0);
                final long scanDurationMilli = System.currentTimeMillis() - scanStart;
                final long scanDurationSeconds = TimeUnit.MILLISECONDS.toSeconds(scanDurationMilli);
                GradleDependencyScanner.LOGGER.debug("Scan Completed in ({} seconds)", scanDurationSeconds);
                if (result != 0) {
                    GradleDependencyScanner.LOGGER.error("Project {} could not be built. Results will be ignored",
                        workingDirectory.getName());
                    // TODO There is an error with the build
                }
            } catch (CommandLineException e) {
                // TODO catch and log error
                e.printStackTrace();
                throw e;
            }
        } else {
            throw new Exception("The gradle wrapper could not be found or permissions are insufficient for executing");
        }

        // Clear the command line for the next execution
        this.clearCli();

        return logFile;
    }

    /**
     * Method that will find the gradle wrapper in the working directory
     *
     * @param workingDirectory the working directory
     * @return a File pointing to the gradle wrapper
     * @throws Exception in case there is an error
     * @since 0.1.0
     */
    private File getGradleWrapper(final File workingDirectory) throws Exception {
        if (!workingDirectory.isDirectory()) {
            // TODO: Raise a custom exception
            throw new Exception("The file is not a directory");
        }

        // Define the filter depending on the operating system
        String filter = (SystemUtils.IS_OS_UNIX) ? "gradlew" : "gradlew.bat";

        Collection<File> gradleWrappers = FileUtils.listFiles(workingDirectory, new NameFileFilter(filter), null);
        if (CollectionUtils.isEmpty(gradleWrappers) || (gradleWrappers.size() > 1)) {
            throw new Exception("There are more than one gradle wrappers within the parent folder");
        }

        // return the first wrapper file
        return gradleWrappers.iterator().next();
    }

    /**
     * Clears the cli interface.
     *
     * @since 0.1.0
     */
    private void clearCli() {
        this.cli = new Commandline();
        this.cli.addSystemEnvironment();
        // Set goal
        this.cli.createArg().setLine("dependencies --configuration compile");
    }

}
