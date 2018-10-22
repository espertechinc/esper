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

import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.PatternDeltaCompute;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

/**
 * Factory for making observer instances.
 */
public class TimerIntervalObserverFactory implements ObserverFactory {

    protected int scheduleCallbackId;
    protected PatternDeltaCompute deltaCompute;

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public void setDeltaCompute(PatternDeltaCompute deltaCompute) {
        this.deltaCompute = deltaCompute;
    }

    public PatternDeltaCompute getDeltaCompute() {
        return deltaCompute;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public EventObserver makeObserver(PatternAgentInstanceContext context, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, Object observerState, boolean isFilterChildNonQuitting) {
        return new TimerIntervalObserver(deltaCompute.computeDelta(beginState, context), beginState, observerEventEvaluator);
    }

    public boolean isNonRestarting() {
        return false;
    }
}
