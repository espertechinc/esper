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
package com.espertech.esper.example.matchmaker.eventbean;

public enum AgeRange {
    AGE_1(18, 25),
    AGE_2(26, 35),
    AGE_3(36, 45),
    AGE_4(46, 55),
    AGE_5(55, 65),
    AGE_6(65, Integer.MAX_VALUE);

    private int low;
    private int high;

    AgeRange(int low, int high) {
        this.low = low;
        this.high = high;
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

}
