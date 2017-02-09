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
package com.espertech.esper.epl.expression.methodagg;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.*;

public class ExprMethodAggUtil {
    public static ExprEvaluator getDefaultEvaluator(ExprNode[] childNodes, boolean join, EventType[] typesPerStream)
            throws ExprValidationException {
        ExprEvaluator evaluator;
        if (childNodes.length > 1) {
            evaluator = getMultiNodeEvaluator(childNodes, join, typesPerStream);
        } else if (childNodes.length > 0) {
            if (childNodes[0] instanceof ExprWildcard) {
                evaluator = getWildcardEvaluator(typesPerStream, join);
            } else {
                // Use the evaluation node under the aggregation node to obtain the aggregation value
                evaluator = childNodes[0].getExprEvaluator();
            }
        } else {
            // For aggregation that doesn't evaluate any particular sub-expression, return null on evaluation
            evaluator = new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    return null;
                }

                public Class getType() {
                    return null;
                }
            };
        }
        return evaluator;
    }

    public static ExprEvaluator getMultiNodeEvaluator(ExprNode[] childNodes, boolean join, EventType[] typesPerStream) throws ExprValidationException {
        final ExprEvaluator[] evaluators = new ExprEvaluator[childNodes.length];

        // determine constant nodes
        int count = 0;
        for (ExprNode node : childNodes) {
            if (node instanceof ExprWildcard) {
                evaluators[count] = getWildcardEvaluator(typesPerStream, join);
            } else {
                evaluators[count] = node.getExprEvaluator();
            }
            count++;
        }

        return new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                Object[] values = new Object[evaluators.length];
                for (int i = 0; i < evaluators.length; i++) {
                    values[i] = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                }
                return values;
            }

            public Class getType() {
                return Object[].class;
            }
        };
    }

    private static ExprEvaluator getWildcardEvaluator(EventType[] typesPerStream, boolean isJoin) throws ExprValidationException {
        final Class returnType = typesPerStream != null && typesPerStream.length > 0 ? typesPerStream[0].getUnderlyingType() : null;
        if (isJoin || returnType == null) {
            throw new ExprValidationException("Invalid use of wildcard (*) for stream selection in a join or an empty from-clause, please use the stream-alias syntax to select a specific stream instead");
        }
        return new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                EventBean event = eventsPerStream[0];
                if (event == null) {
                    return null;
                }
                return event.getUnderlying();
            }

            public Class getType() {
                return returnType;
            }
        };
    }
}
