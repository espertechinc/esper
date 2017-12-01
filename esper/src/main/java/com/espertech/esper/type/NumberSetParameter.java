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

import java.io.Serializable;
import java.util.Set;

/**
 * Interface to generate a set of integers from parameters that include ranges, lists and frequencies.
 */
public interface NumberSetParameter extends Serializable {
    /**
     * Returns true if all values between and including min and max are supplied by the parameter.
     *
     * @param min - lower end of range
     * @param max - upper end of range
     * @return true if parameter specifies all int values between min and max, false if not
     */
    public boolean isWildcard(int min, int max);

    /**
     * Return a set of int values representing the value of the parameter for the given range.
     *
     * @param min - lower end of range
     * @param max - upper end of range
     * @return set of integer
     */
    public Set<Integer> getValuesInRange(int min, int max);

    public boolean containsPoint(int point);

    public String formatted();
}
