package com.pbarrientos.sourceeye.scanners;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Class responsible of parsing the logs produced by Gradle
 *
 * @author Pablo Barrientos
 */
public class GradleOutputHandler extends DependencyTreeOutputHandler {

    /**
     * The dependencies pattern
     */
    private final Pattern pattern = Pattern.compile("(\\+---|\\\\---)(.*:.*)");

    public GradleOutputHandler(final Path logFile) throws Exception {
        super(logFile);
    }

    /**
     * @see com.pbarrientos.sourceeye.scanners.DependencyTreeOutputHandler#getPattern()
     */
    @Override
    protected Pattern getPattern() {
        return this.pattern;
    }

    /**
     * This will filter the line if contains repeated dependencies
     * 
     * @see com.pbarrientos.sourceeye.scanners.DependencyTreeOutputHandler#filterLine(java.lang.String)
     */
    @Override
    protected boolean filterLine(final String dependency) {
        // Filter if line contains -> or (*)
        return dependency.contains("->") || dependency.contains("(*)");
    }

}
