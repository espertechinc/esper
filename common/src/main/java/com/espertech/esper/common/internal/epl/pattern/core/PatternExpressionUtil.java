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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

/**
 * Utility for evaluating pattern expressions.
 */
public class PatternExpressionUtil {
    private final static Logger log = LoggerFactory.getLogger(PatternExpressionUtil.class);

    public static void toPrecedenceFreeEPL(StringWriter writer, String delimiterText, List<EvalForgeNode> childNodes, PatternExpressionPrecedenceEnum precedence) {
        String delimiter = "";
        for (EvalForgeNode child : childNodes) {
            writer.append(delimiter);
            child.toEPL(writer, precedence);
            delimiter = " " + delimiterText + " ";
        }
    }

    public static Object getKeys(MatchedEventMap matchEvent, MatchedEventConvertor convertor, ExprEvaluator expression, AgentInstanceContext agentInstanceContext) {
        EventBean[] eventsPerStream = convertor.convert(matchEvent);
        return expression.evaluate(eventsPerStream, true, agentInstanceContext);
    }

    public static Object evaluateChecked(String objectName, ExprEvaluator evaluator, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) throws EPException {
        try {
            return evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
        } catch (RuntimeException ex) {
            throw handleRuntimeEx(ex, objectName);
        }
    }

    public static EPException handleRuntimeEx(RuntimeException ex, String objectName) {
        String message = objectName + " failed to evaluate expression";
        if (ex.getMessage() != null) {
            message += ": " + ex.getMessage();
        }
        log.error(message, ex);
        throw new EPException(message);
    }
}
