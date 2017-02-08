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
package com.espertech.esper.pattern.pool;

public class PatternSubexpressionPoolStmtHandler {

    private int count;

    public int getCount() {
        return count;
    }

    public void decreaseCount() {
        count--;
        if (count < 0) {
            count = 0;
        }
    }

    public void increaseCount() {
        count++;
    }
}
