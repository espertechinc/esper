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

import com.espertech.esper.schedule.ScheduleSpec;

public class OutputConditionPolledCrontabState implements OutputConditionPolledState {
    private final ScheduleSpec scheduleSpec;
    private Long currentReferencePoint;
    private long nextScheduledTime;

    public OutputConditionPolledCrontabState(ScheduleSpec scheduleSpec, Long currentReferencePoint, long nextScheduledTime) {
        this.scheduleSpec = scheduleSpec;
        this.currentReferencePoint = currentReferencePoint;
        this.nextScheduledTime = nextScheduledTime;
    }

    public ScheduleSpec getScheduleSpec() {
        return scheduleSpec;
    }

    public Long getCurrentReferencePoint() {
        return currentReferencePoint;
    }

    public void setCurrentReferencePoint(Long currentReferencePoint) {
        this.currentReferencePoint = currentReferencePoint;
    }

    public long getNextScheduledTime() {
        return nextScheduledTime;
    }

    public void setNextScheduledTime(long nextScheduledTime) {
        this.nextScheduledTime = nextScheduledTime;
    }
}
