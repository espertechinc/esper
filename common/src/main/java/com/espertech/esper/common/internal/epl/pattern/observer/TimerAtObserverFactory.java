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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertor;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;
import com.espertech.esper.common.internal.schedule.ScheduleSpec;
import com.espertech.esper.common.internal.schedule.ScheduleSpecUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for 'crontab' observers that indicate truth when a time point was reached.
 */
public class TimerAtObserverFactory implements ObserverFactory {

    private final static Logger log = LoggerFactory.getLogger(TimerAtObserverFactory.class);

    private ExprEvaluator[] parameters;
    private MatchedEventConvertor optionalConvertor;
    private ScheduleSpec spec;
    private int scheduleCallbackId = -1;

    public void setParameters(ExprEvaluator[] parameters) {
        this.parameters = parameters;
    }

    public void setOptionalConvertor(MatchedEventConvertor optionalConvertor) {
        this.optionalConvertor = optionalConvertor;
    }

    public void setSpec(ScheduleSpec spec) {
        this.spec = spec;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public ScheduleSpec computeSpec(MatchedEventMap beginState, PatternAgentInstanceContext context) {
        if (spec != null) {
            return spec;
        }
        Object[] observerParameters = evaluateRuntime(beginState, parameters, optionalConvertor, context.getAgentInstanceContext());
        try {
            return ScheduleSpecUtil.computeValues(observerParameters);
        } catch (ScheduleParameterException e) {
            throw new EPException("Error computing crontab schedule specification: " + e.getMessage(), e);
        }
    }

    public EventObserver makeObserver(PatternAgentInstanceContext context, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, Object observerState, boolean isFilterChildNonQuitting) {
        return new TimerAtObserver(computeSpec(beginState, context), beginState, observerEventEvaluator);
    }

    public boolean isNonRestarting() {
        return false;
    }

    private static Object[] evaluateRuntime(MatchedEventMap beginState, ExprEvaluator[] parameters, MatchedEventConvertor optionalConvertor, ExprEvaluatorContext exprEvaluatorContext)
            throws EPException {
        Object[] results = new Object[parameters.length];
        int count = 0;
        EventBean[] eventsPerStream = optionalConvertor == null ? null : optionalConvertor.convert(beginState);
        for (ExprEvaluator expr : parameters) {
            try {
                Object result = expr.evaluate(eventsPerStream, true, exprEvaluatorContext);
                results[count] = result;
                count++;
            } catch (RuntimeException ex) {
                String message = "Timer-at observer invalid parameter in expression " + count;
                if (ex.getMessage() != null) {
                    message += ": " + ex.getMessage();
                }
                log.error(message, ex);
                throw new EPException(message);
            }
        }
        return results;
    }
}
