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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.schedule.ScheduleSpec;

import java.util.List;

public class ContextDetailConditionCrontab implements ContextDetailCondition {
    private static final long serialVersionUID = -1671433952748059211L;
    private final List<ExprNode> crontab;
    private final boolean immediate;
    private ScheduleSpec schedule;
    private int scheduleCallbackId = -1;

    public ContextDetailConditionCrontab(List<ExprNode> crontab, boolean immediate) {
        this.crontab = crontab;
        this.immediate = immediate;
    }

    public List<ExprNode> getCrontab() {
        return crontab;
    }

    public ScheduleSpec getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleSpec schedule) {
        this.schedule = schedule;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }
}
