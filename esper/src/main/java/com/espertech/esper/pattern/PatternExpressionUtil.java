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
package com.espertech.esper.pattern;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for evaluating pattern expressions.
 */
public class PatternExpressionUtil {
    private final static Logger log = LoggerFactory.getLogger(PatternExpressionUtil.class);

    public static Object getKeys(MatchedEventMap matchEvent, MatchedEventConvertor convertor, ExprEvaluator[] expressions, AgentInstanceContext agentInstanceContext) {
        EventBean[] eventsPerStream = convertor.convert(matchEvent);
        if (expressions.length == 1) {
            return expressions[0].evaluate(eventsPerStream, true, agentInstanceContext);
        }

        Object[] keys = new Object[expressions.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = expressions[i].evaluate(eventsPerStream, true, agentInstanceContext);
        }
        return new MultiKeyUntyped(keys);
    }

    /**
     * Ctor.
     *
     * @param objectName           is the pattern object name
     * @param beginState           the pattern begin state
     * @param parameters           object parameters
     * @param convertor            for converting to a event-per-stream view for use to evaluate expressions
     * @param exprEvaluatorContext expression evaluation context
     * @return expression results
     * @throws EPException if the evaluate failed
     */
    public static List<Object> evaluate(String objectName, MatchedEventMap beginState, List<ExprNode> parameters, MatchedEventConvertor convertor, ExprEvaluatorContext exprEvaluatorContext)
            throws EPException {
        List<Object> results = new ArrayList<Object>();
        int count = 0;
        EventBean[] eventsPerStream = convertor.convert(beginState);
        for (ExprNode expr : parameters) {
            try {
                Object result = evaluate(objectName, expr, eventsPerStream, exprEvaluatorContext);
                results.add(result);
                count++;
            } catch (RuntimeException ex) {
                String message = objectName + " invalid parameter in expression " + count;
                if (ex.getMessage() != null) {
                    message += ": " + ex.getMessage();
                }
                log.error(message, ex);
                throw new EPException(message);
            }
        }
        return results;
    }

    /**
     * Evaluate the pattern expression.
     *
     * @param objectName           pattern object name
     * @param beginState           pattern state
     * @param convertor            to converting from pattern match to event-per-stream
     * @param exprEvaluatorContext expression evaluation context
     * @param timePeriod           time period
     * @return evaluation result
     * @throws EPException if the evaluation failed
     */
    public static Object evaluateTimePeriod(String objectName, MatchedEventMap beginState, ExprTimePeriod timePeriod, MatchedEventConvertor convertor, ExprEvaluatorContext exprEvaluatorContext)
            throws EPException {
        EventBean[] eventsPerStream = convertor.convert(beginState);
        try {
            return timePeriod.evaluateGetTimePeriod(eventsPerStream, true, exprEvaluatorContext);
        } catch (RuntimeException ex) {
            throw handleRuntimeEx(ex, objectName);
        }
    }

    public static Object evaluate(String objectName, MatchedEventMap beginState, ExprNode parameter, MatchedEventConvertor convertor, ExprEvaluatorContext exprEvaluatorContext)
            throws EPException {
        EventBean[] eventsPerStream = convertor.convert(beginState);
        return evaluate(objectName, parameter, eventsPerStream, exprEvaluatorContext);
    }

    private static Object evaluate(String objectName, ExprNode expression, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) throws EPException {
        try {
            return expression.getForge().getExprEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
        } catch (RuntimeException ex) {
            throw handleRuntimeEx(ex, objectName);
        }
    }

    private static EPException handleRuntimeEx(RuntimeException ex, String objectName) {
        String message = objectName + " failed to evaluate expression";
        if (ex.getMessage() != null) {
            message += ": " + ex.getMessage();
        }
        log.error(message, ex);
        throw new EPException(message);
    }

    public static void toPrecedenceFreeEPL(StringWriter writer, String delimiterText, List<EvalFactoryNode> childNodes, PatternExpressionPrecedenceEnum precedence) {
        String delimiter = "";
        for (EvalFactoryNode child : childNodes) {
            writer.append(delimiter);
            child.toEPL(writer, precedence);
            delimiter = " " + delimiterText + " ";
        }
    }
}
