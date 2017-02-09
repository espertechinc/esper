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
package com.espertech.esper.epl.view;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionPolledExpressionState implements OutputConditionPolledState {
    private int totalNewEventsCount;
    private int totalOldEventsCount;
    private int totalNewEventsSum;
    private int totalOldEventsSum;
    private Long lastOutputTimestamp;

    public OutputConditionPolledExpressionState(int totalNewEventsCount, int totalOldEventsCount, int totalNewEventsSum, int totalOldEventsSum, Long lastOutputTimestamp) {
        this.totalNewEventsCount = totalNewEventsCount;
        this.totalOldEventsCount = totalOldEventsCount;
        this.totalNewEventsSum = totalNewEventsSum;
        this.totalOldEventsSum = totalOldEventsSum;
        this.lastOutputTimestamp = lastOutputTimestamp;
    }

    public int getTotalNewEventsCount() {
        return totalNewEventsCount;
    }

    public void setTotalNewEventsCount(int totalNewEventsCount) {
        this.totalNewEventsCount = totalNewEventsCount;
    }

    public int getTotalOldEventsCount() {
        return totalOldEventsCount;
    }

    public void setTotalOldEventsCount(int totalOldEventsCount) {
        this.totalOldEventsCount = totalOldEventsCount;
    }

    public int getTotalNewEventsSum() {
        return totalNewEventsSum;
    }

    public void setTotalNewEventsSum(int totalNewEventsSum) {
        this.totalNewEventsSum = totalNewEventsSum;
    }

    public int getTotalOldEventsSum() {
        return totalOldEventsSum;
    }

    public void setTotalOldEventsSum(int totalOldEventsSum) {
        this.totalOldEventsSum = totalOldEventsSum;
    }

    public Long getLastOutputTimestamp() {
        return lastOutputTimestamp;
    }

    public void setLastOutputTimestamp(Long lastOutputTimestamp) {
        this.lastOutputTimestamp = lastOutputTimestamp;
    }
}
