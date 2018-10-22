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
package com.espertech.esper.common.internal.epl.output.polled;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;
import com.espertech.esper.common.internal.schedule.ScheduleSpec;
import com.espertech.esper.common.internal.schedule.ScheduleSpecUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output condition handling crontab-at schedule output.
 */
public final class OutputConditionPolledCrontabFactory implements OutputConditionPolledFactory {
    private final ExprEvaluator[] expressions;

    public OutputConditionPolledCrontabFactory(ExprEvaluator[] expressions) {
        this.expressions = expressions;
    }

    public OutputConditionPolled makeNew(AgentInstanceContext agentInstanceContext) {
        ScheduleSpec scheduleSpec;
        try {
            Object[] scheduleSpecParameterList = evaluate(expressions, agentInstanceContext);
            scheduleSpec = ScheduleSpecUtil.computeValues(scheduleSpecParameterList);
        } catch (ScheduleParameterException e) {
            throw new IllegalArgumentException("Invalid schedule specification : " + e.getMessage(), e);
        }
        OutputConditionPolledCrontabState state = new OutputConditionPolledCrontabState(scheduleSpec, null, 0);
        return new OutputConditionPolledCrontab(agentInstanceContext, state);
    }

    public OutputConditionPolled makeFromState(AgentInstanceContext agentInstanceContext, OutputConditionPolledState state) {
        return new OutputConditionPolledCrontab(agentInstanceContext, (OutputConditionPolledCrontabState) state);
    }

    private static Object[] evaluate(ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] results = new Object[parameters.length];
        int count = 0;
        for (ExprEvaluator expr : parameters) {
            try {
                results[count] = expr.evaluate(null, true, exprEvaluatorContext);
                count++;
            } catch (RuntimeException ex) {
                String message = "Failed expression evaluation in crontab timer-at for parameter " + count + ": " + ex.getMessage();
                log.error(message, ex);
                throw new IllegalArgumentException(message);
            }
        }
        return results;
    }

    private static final Logger log = LoggerFactory.getLogger(OutputConditionPolledCrontabFactory.class);
}
