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
package com.espertech.esper.epl.datetime.interval;

public abstract class IntervalComputerConstantBase {
    protected final long start;
    protected final long end;

    public IntervalComputerConstantBase(IntervalStartEndParameterPairForge pair, boolean allowSwitch) {
        long startVal = pair.getStart().getOptionalConstant();
        long endVal = pair.getEnd().getOptionalConstant();

        if (startVal > endVal && allowSwitch) {
            start = endVal;
            end = startVal;
        } else {
            start = startVal;
            end = endVal;
        }
    }
}
