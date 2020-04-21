package com.pbarrientos.sourceeye.utils;

import org.apache.commons.lang3.Range;

/**
 * Possible ranges of CVSS scores.
 * 
 * @author Pablo Barrientos
 */
public enum ScoreRanges {

    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private Range<Float> range;

    private ScoreRanges(final String rangeStr) {
        switch (rangeStr) {
        case "LOW":
            this.range = Range.between(0f, 2.99f);
            break;
        case "MEDIUM":
            this.range = Range.between(3f, 6.99f);
            break;
        case "HIGH":
            this.range = Range.between(7f, 8.99f);
            break;
        case "CRITICAL":
            this.range = Range.between(9f, 10f);
            break;
        }
    }

    public Range<Float> getRange() {
        return this.range;
    }

}
