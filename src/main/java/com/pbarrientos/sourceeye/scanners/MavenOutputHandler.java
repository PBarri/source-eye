package com.pbarrientos.sourceeye.scanners;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Class in charge of parsing the generated logs by Maven and parse the dependencies.
 * 
 * @author Pablo Barrientos
 */
public class MavenOutputHandler extends DependencyTreeOutputHandler {

    private final Pattern pattern = Pattern.compile("(\\+-|\\\\-)(.*:.*)(:)");

    public MavenOutputHandler(final Path logFile) throws Exception {
        super(logFile);
    }

    @Override
    protected Pattern getPattern() {
        return this.pattern;
    }

    @Override
    protected boolean filterLine(final String dependency) {
        // Never filter lines
        return false;
    }

}
