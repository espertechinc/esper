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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.PatternDeltaCompute;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

/**
 * Factory for {@link TimerWithinGuard} instances.
 */
public class TimerWithinGuardFactory implements GuardFactory {

    private PatternDeltaCompute deltaCompute;
    private int scheduleCallbackId = -1;

    public void setDeltaCompute(PatternDeltaCompute deltaCompute) {
        this.deltaCompute = deltaCompute;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public long computeTime(MatchedEventMap beginState, PatternAgentInstanceContext context) {
        return deltaCompute.computeDelta(beginState, context);
    }

    public Guard makeGuard(PatternAgentInstanceContext context, MatchedEventMap beginState, Quitable quitable, Object guardState) {
        return new TimerWithinGuard(computeTime(beginState, context), quitable);
    }
}
