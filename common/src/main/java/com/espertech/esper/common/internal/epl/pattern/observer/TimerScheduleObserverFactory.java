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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertor;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;

/**
 * Factory for ISO8601 repeating interval observers that indicate truth when a time point was reached.
 */
public class TimerScheduleObserverFactory implements ObserverFactory {
    private boolean isAllConstant;
    private TimerScheduleSpecCompute scheduleComputer;
    private MatchedEventConvertor optionalConvertor;
    protected int scheduleCallbackId = -1;
    private TimerScheduleSpec spec;

    public void setAllConstant(boolean allConstant) {
        isAllConstant = allConstant;
    }

    public void setScheduleComputer(TimerScheduleSpecCompute scheduleComputer) {
        this.scheduleComputer = scheduleComputer;
    }

    public void setOptionalConvertor(MatchedEventConvertor optionalConvertor) {
        this.optionalConvertor = optionalConvertor;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public EventObserver makeObserver(PatternAgentInstanceContext context, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator,
                                      Object observerState, boolean isFilterChildNonQuitting) {
        if (isAllConstant) {
            try {
                spec = scheduleComputer.compute(optionalConvertor, beginState, context.getAgentInstanceContext(), context.getAgentInstanceContext().getClasspathImportServiceRuntime().getTimeZone(), context.getAgentInstanceContext().getClasspathImportServiceRuntime().getTimeAbacus());
            } catch (ScheduleParameterException ex) {
                throw new EPException(ex.getMessage(), ex);
            }
        }

        return new TimerScheduleObserver(computeSpecDynamic(beginState, context), beginState, observerEventEvaluator, isFilterChildNonQuitting);
    }

    public boolean isNonRestarting() {
        return true;
    }

    public TimerScheduleSpec computeSpecDynamic(MatchedEventMap beginState, PatternAgentInstanceContext context) {
        if (spec != null) {
            return spec;
        }
        try {
            return scheduleComputer.compute(optionalConvertor, beginState, context.getAgentInstanceContext(), context.getStatementContext().getClasspathImportServiceRuntime().getTimeZone(), context.getStatementContext().getClasspathImportServiceRuntime().getTimeAbacus());
        } catch (ScheduleParameterException e) {
            throw new EPException("Error computing iso8601 schedule specification: " + e.getMessage(), e);
        }
    }
}
