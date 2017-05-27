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
package com.espertech.esper.supportregression.patternassert;

public class StepDesc {
    private final int step;
    private final Object[][] newDataPerRow;
    private final Object[][] oldDataPerRow;

    public StepDesc(int step, Object[][] newDataPerRow, Object[][] oldDataPerRow) {
        this.step = step;
        this.newDataPerRow = newDataPerRow;
        this.oldDataPerRow = oldDataPerRow;
    }

    public int getStep() {
        return step;
    }

    public Object[][] getNewDataPerRow() {
        return newDataPerRow;
    }

    public Object[][] getOldDataPerRow() {
        return oldDataPerRow;
    }
}
