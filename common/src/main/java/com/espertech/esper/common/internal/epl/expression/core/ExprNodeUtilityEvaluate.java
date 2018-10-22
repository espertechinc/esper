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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

public class ExprNodeUtilityEvaluate {
    private static final Logger log = LoggerFactory.getLogger(ExprNodeUtilityEvaluate.class);

    public static Object evaluateValidationTimeNoStreams(ExprEvaluator evaluator, ExprEvaluatorContext context, String expressionName) throws ExprValidationException {
        try {
            return evaluator.evaluate(null, true, context);
        } catch (EPException ex) {
            throw new ExprValidationException("Invalid " + expressionName + " expression: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.warn("Invalid " + expressionName + " expression evaluation: {}", ex.getMessage(), ex);
            throw new ExprValidationException("Invalid " + expressionName + " expression");
        }
    }

    public static void applyFilterExpressionIterable(Iterator<EventBean> iterator, ExprEvaluator filterExpression, ExprEvaluatorContext exprEvaluatorContext, Collection<EventBean> eventsInWindow) {
        EventBean[] events = new EventBean[1];
        for (; iterator.hasNext(); ) {
            events[0] = iterator.next();
            Object result = filterExpression.evaluate(events, true, exprEvaluatorContext);
            if ((result == null) || (!((Boolean) result))) {
                continue;
            }
            eventsInWindow.add(events[0]);
        }
    }

    /**
     * Apply a filter expression.
     *
     * @param filter               expression
     * @param streamZeroEvent      the event that represents stream zero
     * @param streamOneEvents      all events thate are stream one events
     * @param exprEvaluatorContext context for expression evaluation
     * @return filtered stream one events
     */
    public static EventBean[] applyFilterExpression(ExprEvaluator filter, EventBean streamZeroEvent, EventBean[] streamOneEvents, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean[] eventsPerStream = new EventBean[2];
        eventsPerStream[0] = streamZeroEvent;

        EventBean[] filtered = new EventBean[streamOneEvents.length];
        int countPass = 0;

        for (EventBean eventBean : streamOneEvents) {
            eventsPerStream[1] = eventBean;

            Boolean result = (Boolean) filter.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if ((result != null) && result) {
                filtered[countPass] = eventBean;
                countPass++;
            }
        }

        if (countPass == streamOneEvents.length) {
            return streamOneEvents;
        }
        return EventBeanUtility.resizeArray(filtered, countPass);
    }

    /**
     * Apply a filter expression returning a pass indicator.
     *
     * @param filter               to apply
     * @param eventsPerStream      events per stream
     * @param exprEvaluatorContext context for expression evaluation
     * @return pass indicator
     */
    public static boolean applyFilterExpression(ExprEvaluator filter, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Boolean result = (Boolean) filter.evaluate(eventsPerStream, true, exprEvaluatorContext);
        return (result != null) && result;
    }

    public static Object[] evaluateExpressions(ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext) {
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
}
