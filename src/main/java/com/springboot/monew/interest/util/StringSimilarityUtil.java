package com.springboot.monew.interest.util;

import org.apache.commons.text.similarity.LevenshteinDistance;

public final class StringSimilarityUtil {

    private static final LevenshteinDistance LEVENSHTEIN_DISTANCE =
            LevenshteinDistance.getDefaultInstance();

    private StringSimilarityUtil() {
    }

    public static double similarity(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }

        int maxLength = Math.max(a.length(), b.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = LEVENSHTEIN_DISTANCE.apply(a, b);
        return (double) (maxLength - distance) / maxLength;
    }

    public static boolean isSimilarEnough(String a, String b, double threshold) {
        return similarity(a, b) >= threshold;
    }
}
