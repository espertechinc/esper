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
package com.espertech.esper.type;

import java.util.HashSet;
import java.util.Set;

/**
 * Parameter supplying a single int value is a set of numbers.
 */
public class IntParameter implements NumberSetParameter {
    private int intValue;
    private static final long serialVersionUID = -895750000874644640L;

    public IntParameter() {
    }

    /**
     * Ctor.
     *
     * @param intValue - single in value
     */
    public IntParameter(int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns int value.
     *
     * @return int value
     */
    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public boolean isWildcard(int min, int max) {
        if ((intValue == min) && (intValue == max)) {
            return true;
        }
        return false;
    }

    public Set<Integer> getValuesInRange(int min, int max) {
        Set<Integer> values = new HashSet<Integer>();

        if ((intValue >= min) && (intValue <= max)) {
            values.add(intValue);
        }

        return values;
    }

    public boolean containsPoint(int point) {
        return intValue == point;
    }

    public String formatted() {
        return Integer.toString(intValue);
    }
}
