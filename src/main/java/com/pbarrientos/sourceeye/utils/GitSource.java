package com.pbarrientos.sourceeye.utils;

/**
 * Enum referring to the possible sources.
 *
 * @author Pablo Barrientos
 */
public enum GitSource {

    LOCAL("local"),
    GITHUB("github"),
    GITLAB("gitlab");

    private String source;

    private GitSource(final String source) {
        this.source = source;
    }

    public String getSource() {
        return this.source;
    }

}
