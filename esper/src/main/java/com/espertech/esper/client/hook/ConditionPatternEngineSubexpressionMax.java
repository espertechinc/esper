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
package com.espertech.esper.client.hook;

import java.util.Map;

/**
 * Indicates that on the engine level the followed-by pattern operator, regardless whether parameterized with a max number of sub-expressions or not,
 * has reached the configured engine-wide limit at runtime.
 */
public class ConditionPatternEngineSubexpressionMax implements BaseCondition {
    private final long max;
    private final Map<String, Long> counts;

    /**
     * Ctor.
     *
     * @param max    limit reached
     * @param counts the number of subexpression counts per statement
     */
    public ConditionPatternEngineSubexpressionMax(long max, Map<String, Long> counts) {
        this.max = max;
        this.counts = counts;
    }

    /**
     * Returns the limit reached.
     *
     * @return limit
     */
    public long getMax() {
        return max;
    }

    /**
     * Returns the per-statement count.
     *
     * @return count
     */
    public Map<String, Long> getCounts() {
        return counts;
    }
}
