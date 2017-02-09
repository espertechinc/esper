/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.util;

import java.util.Locale;

/**
 * Utility for string comparison based on the Levenshtein algo.
 */
public class LevenshteinDistance {
    /**
     * Make 3 characters an acceptable distance for reporting.
     */
    public final static int ACCEPTABLE_DISTANCE = 3;

    /**
     * Compute the distance between two strins using the Levenshtein algorithm,
     * including a case-insensitive string comparison.
     *
     * @param str1 first string
     * @param str2 second string
     * @return distance or zero if case-insensitive string comparison found equal strings
     * or Integer.MAX_VALUE for invalid comparison because of null values.
     */
    public static int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
        if ((str1 == null) || (str2 == null)) {
            return Integer.MAX_VALUE;
        }
        if (str1.toString().toLowerCase(Locale.ENGLISH).equals(str2.toString().toLowerCase(Locale.ENGLISH))) {
            return 0;
        }

        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1]
                                + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
                                : 1));
            }
        }

        return distance[str1.length()][str2.length()];
    }

    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
}
