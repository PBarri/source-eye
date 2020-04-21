package com.pbarrientos.sourceeye.utils;

/**
 * The type of the projects
 *
 * @author Pablo Barrientos
 */
public enum ProjectType {

    MAVEN("maven"),
    GRADLE("gradle"),
    UNKNOWN("unknown");

    private ProjectType(final String type) {
        this.type = type;
    }

    private String type;

    public String getType() {
        return this.type;
    }

}
