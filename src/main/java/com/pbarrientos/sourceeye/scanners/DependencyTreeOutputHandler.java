package com.pbarrientos.sourceeye.scanners;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that serve as a base for
 *
 * @author Pablo Barrientos
 */
public abstract class DependencyTreeOutputHandler implements InvocationOutputHandler {

    /**
     * The logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTreeOutputHandler.class);

    /**
     * The temporary file path
     */
    protected final Path logFile;

    /**
     * Constructor that initializes the temporary file path, checking its existance.
     *
     * @param logFile the file
     * @throws Exception in case the file does not exist
     */
    public DependencyTreeOutputHandler(final Path logFile) throws Exception {
        if ((logFile == null) || !logFile.toFile().exists()) {
            throw new Exception("Log file was not created successfully");
        }

        this.logFile = logFile;
    }

    /**
     * Returns a defined pattern that will match the dependencies found by the scanner
     *
     * @return the pattern
     * @since 0.1.0
     */
    protected abstract Pattern getPattern();

    /**
     * Check if the found dependency must by filtered in the output
     *
     * @param dependency the dependency
     * @return if the dependency should be filtered
     * @since 0.1.0
     */
    protected abstract boolean filterLine(String dependency);

    /**
     * This method parses the log produced by the scanner, checks if the line belongs to a dependency using the pattern
     * given at {@link DependencyTreeOutputHandler#getPattern()}. In that case, checks if the dependency should be
     * filtered and, if not, writes to the temporary file the dependency.
     *
     * @see org.apache.maven.shared.utils.cli.StreamConsumer#consumeLine(java.lang.String)
     */
    @Override
    public void consumeLine(final String line) throws IOException {

        DependencyTreeOutputHandler.LOGGER.debug(line);

        Matcher matcher = this.getPattern().matcher(line);

        if (matcher.find()) {
            String dependency = matcher.group(2).trim();

            if (!this.filterLine(dependency)) {
                DependencyTreeOutputHandler.LOGGER.debug("----> {}", dependency);
                try (PrintWriter writer = new PrintWriter(new FileOutputStream(this.logFile.toFile(), true), true)) {
                    writer.println(dependency);
                } catch (FileNotFoundException e) {
                    // This will never happen
                }
            }
        }
    }

}
