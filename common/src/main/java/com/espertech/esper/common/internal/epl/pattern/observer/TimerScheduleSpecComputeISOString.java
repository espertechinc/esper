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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertor;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionUtil;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;

import java.util.TimeZone;

import static com.espertech.esper.common.internal.epl.pattern.observer.TimerScheduleObserverForge.NAME_OBSERVER;

public class TimerScheduleSpecComputeISOString implements TimerScheduleSpecCompute {
    private final ExprEvaluator parameter;

    public TimerScheduleSpecComputeISOString(ExprEvaluator parameter) {
        this.parameter = parameter;
    }

    public TimerScheduleSpec compute(MatchedEventConvertor optionalConvertor, MatchedEventMap beginState, ExprEvaluatorContext exprEvaluatorContext, TimeZone timeZone, TimeAbacus timeAbacus) throws ScheduleParameterException {
        EventBean[] events = optionalConvertor == null ? null : optionalConvertor.convert(beginState);
        return compute(parameter, events, exprEvaluatorContext);
    }

    protected static TimerScheduleSpec compute(ExprEvaluator parameter, EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) throws ScheduleParameterException {
        Object param = PatternExpressionUtil.evaluateChecked(NAME_OBSERVER, parameter, events, exprEvaluatorContext);
        String iso = (String) param;
        if (iso == null) {
            throw new ScheduleParameterException("Received null parameter value");
        }
        return TimerScheduleISO8601Parser.parse(iso);
    }
}
