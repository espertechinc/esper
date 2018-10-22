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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertor;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.PatternDeltaCompute;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionUtil;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

public class TimerWithinOrMaxCountGuardFactory implements GuardFactory {

    private PatternDeltaCompute deltaCompute;
    private ExprEvaluator countEval;
    private MatchedEventConvertor optionalConvertor;
    private int scheduleCallbackId = -1;

    public void setDeltaCompute(PatternDeltaCompute deltaCompute) {
        this.deltaCompute = deltaCompute;
    }

    public void setOptionalConvertor(MatchedEventConvertor optionalConvertor) {
        this.optionalConvertor = optionalConvertor;
    }

    public void setCountEval(ExprEvaluator countEval) {
        this.countEval = countEval;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public long computeTime(MatchedEventMap beginState, PatternAgentInstanceContext context) {
        return deltaCompute.computeDelta(beginState, context);
    }

    public int computeNumCountTo(MatchedEventMap beginState, PatternAgentInstanceContext context) {
        EventBean[] events = optionalConvertor == null ? null : optionalConvertor.convert(beginState);
        Object numCountToVal = PatternExpressionUtil.evaluateChecked("Timer-Within-Or-Max-Count guard", countEval, events, context.getAgentInstanceContext());
        if (null == numCountToVal) {
            throw new EPException("Timer-within-or-max second parameter evaluated to a null-value");
        }
        return (Integer) numCountToVal;
    }

    public Guard makeGuard(PatternAgentInstanceContext context, MatchedEventMap beginState, Quitable quitable, Object guardState) {
        return new TimerWithinOrMaxCountGuard(computeTime(beginState, context), computeNumCountTo(beginState, context), quitable);
    }
}
