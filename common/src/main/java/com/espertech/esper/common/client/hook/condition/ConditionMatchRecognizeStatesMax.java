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
package com.espertech.esper.common.client.hook.condition;

import com.espertech.esper.common.internal.util.DeploymentIdNamePair;

import java.util.Map;

/**
 * Indicates that on the runtimelevel the match-recognize has reached the configured runtime-wide limit at runtime.
 */
public class ConditionMatchRecognizeStatesMax implements BaseCondition {
    private final long max;
    private final Map<DeploymentIdNamePair, Long> counts;

    /**
     * Ctor.
     *
     * @param max    limit reached
     * @param counts the number of state counts per statement
     */
    public ConditionMatchRecognizeStatesMax(long max, Map<DeploymentIdNamePair, Long> counts) {
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
    public Map<DeploymentIdNamePair, Long> getCounts() {
        return counts;
    }
}
