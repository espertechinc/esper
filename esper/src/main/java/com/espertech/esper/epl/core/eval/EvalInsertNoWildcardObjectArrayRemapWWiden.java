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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.TypeWidener;

public class EvalInsertNoWildcardObjectArrayRemapWWiden extends EvalInsertNoWildcardObjectArrayRemap {

    private final TypeWidener[] wideners;

    public EvalInsertNoWildcardObjectArrayRemapWWiden(SelectExprContext selectExprContext, EventType resultEventType, int[] remapped, TypeWidener[] wideners) {
        super(selectExprContext, resultEventType, remapped);
        this.wideners = wideners;
    }

    @Override
    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        ExprEvaluator[] expressionNodes = selectExprContext.getExpressionNodes();

        Object[] result = new Object[resultEventType.getPropertyNames().length];
        for (int i = 0; i < expressionNodes.length; i++) {
            Object value = expressionNodes[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (wideners[i] != null) {
                value = wideners[i].widen(value);
            }
            result[remapped[i]] = value;
        }

        return selectExprContext.getEventAdapterService().adapterForTypedObjectArray(result, resultEventType);
    }
}
