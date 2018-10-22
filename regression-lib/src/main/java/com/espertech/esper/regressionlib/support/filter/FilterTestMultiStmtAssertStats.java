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
package com.espertech.esper.regressionlib.support.filter;

public class FilterTestMultiStmtAssertStats {
    private final String stats;
    private final int[] permutation;

    public FilterTestMultiStmtAssertStats(String stats, int... permutation) {
        this.stats = stats;
        this.permutation = permutation;
    }

    public String getStats() {
        return stats;
    }

    public int[] getPermutation() {
        return permutation;
    }

    public static FilterTestMultiStmtAssertStats[] makeSingleStat(String stats) {
        return new FilterTestMultiStmtAssertStats[]{
            new FilterTestMultiStmtAssertStats(stats, 0)
        };
    }

    public static FilterTestMultiStmtAssertStats[] makeTwoSameStat(String stats) {
        return new FilterTestMultiStmtAssertStats[]{
            new FilterTestMultiStmtAssertStats(stats, 0, 1),
            new FilterTestMultiStmtAssertStats(stats, 1, 0)
        };
    }
}
