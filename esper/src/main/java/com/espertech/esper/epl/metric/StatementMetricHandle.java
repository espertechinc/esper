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
package com.espertech.esper.epl.metric;

/**
 * Handle for statements metric reporting by runtime.
 */
public class StatementMetricHandle {
    private final int groupNum;
    private final int index;
    private boolean isEnabled;

    /**
     * Ctor.
     *
     * @param groupNum group number, zero for default group
     * @param index    index slot
     */
    public StatementMetricHandle(int groupNum, int index) {
        this.groupNum = groupNum;
        this.index = index;
        this.isEnabled = true;
    }

    /**
     * Returns group number for statement.
     *
     * @return group number
     */
    public int getGroupNum() {
        return groupNum;
    }

    /**
     * Returns slot number of metric.
     *
     * @return metric index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns true if enabled for statement.
     *
     * @return enabled flag
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Set to true if statement metric reporting is enabled, false for disabled.
     *
     * @param enabled flag
     */
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
