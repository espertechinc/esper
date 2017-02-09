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

/**
 * Indicates that the followed-by pattern operator, when parameterized with a max number of sub-expressions,
 * has reached that limit at runtime.
 */
public class ConditionPatternSubexpressionMax implements BaseCondition {
    private final int max;

    /**
     * Ctor.
     *
     * @param max limit reached
     */
    public ConditionPatternSubexpressionMax(int max) {
        this.max = max;
    }

    /**
     * Returns the limit reached.
     *
     * @return limit
     */
    public int getMax() {
        return max;
    }
}
