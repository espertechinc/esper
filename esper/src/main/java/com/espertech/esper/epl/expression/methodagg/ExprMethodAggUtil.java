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
    public static ExprForge[] getDefaultForges(ExprNode[] childNodes, boolean join, EventType[] typesPerStream) throws ExprValidationException {
        if (childNodes.length == 0) {
            return ExprNodeUtilityCore.EMPTY_FORGE_ARRAY;
        }
        ExprForge[] forges = new ExprForge[childNodes.length];
        for (int i = 0; i < childNodes.length; i++) {
            if (childNodes[i] instanceof ExprWildcard) {
                validateWildcard(typesPerStream, join);
                forges[i] = new ExprForgeWildcard(typesPerStream[0].getUnderlyingType());
            } else {
                forges[i] = childNodes[i].getForge();
            }
        }
        return forges;
    }

    public static ExprEvaluator getMultiNodeEvaluator(ExprNode[] childNodes, boolean join, EventType[] typesPerStream) throws ExprValidationException {
        final ExprEvaluator[] evaluators = new ExprEvaluator[childNodes.length];

        // determine constant nodes
        int count = 0;
        for (ExprNode node : childNodes) {
            if (node instanceof ExprWildcard) {
                evaluators[count] = getWildcardEvaluator(typesPerStream, join);
            } else {
                evaluators[count] = node.getForge().getExprEvaluator();
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
        };
    }

    private static ExprEvaluator getWildcardEvaluator(EventType[] typesPerStream, boolean isJoin) throws ExprValidationException {
        validateWildcard(typesPerStream, isJoin);
        return ExprEvaluatorWildcard.INSTANCE;
    }

    private static void validateWildcard(EventType[] typesPerStream, boolean isJoin) throws ExprValidationException {
        final Class returnType = typesPerStream != null && typesPerStream.length > 0 ? typesPerStream[0].getUnderlyingType() : null;
        if (isJoin || returnType == null) {
            throw new ExprValidationException("Invalid use of wildcard (*) for stream selection in a join or an empty from-clause, please use the stream-alias syntax to select a specific stream instead");
        }
    }
}
