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

public class OutputConditionPolledCountState implements OutputConditionPolledState {
    private long eventRate;
    private int newEventsCount;
    private int oldEventsCount;
    private boolean isFirst = true;

    public OutputConditionPolledCountState(long eventRate, int newEventsCount, int oldEventsCount, boolean isFirst) {
        this.eventRate = eventRate;
        this.newEventsCount = newEventsCount;
        this.oldEventsCount = oldEventsCount;
        this.isFirst = isFirst;
    }

    public long getEventRate() {
        return eventRate;
    }

    public void setEventRate(long eventRate) {
        this.eventRate = eventRate;
    }

    public int getNewEventsCount() {
        return newEventsCount;
    }

    public void setNewEventsCount(int newEventsCount) {
        this.newEventsCount = newEventsCount;
    }

    public int getOldEventsCount() {
        return oldEventsCount;
    }

    public void setOldEventsCount(int oldEventsCount) {
        this.oldEventsCount = oldEventsCount;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setIsFirst(boolean isFirst) {
        this.isFirst = isFirst;
    }
}
