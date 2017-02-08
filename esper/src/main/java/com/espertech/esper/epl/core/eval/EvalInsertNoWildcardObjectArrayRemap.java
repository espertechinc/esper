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
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class EvalInsertNoWildcardObjectArrayRemap implements SelectExprProcessor {

    protected final SelectExprContext selectExprContext;
    protected final EventType resultEventType;
    protected final int[] remapped;

    public EvalInsertNoWildcardObjectArrayRemap(SelectExprContext selectExprContext, EventType resultEventType, int[] remapped) {
        this.selectExprContext = selectExprContext;
        this.resultEventType = resultEventType;
        this.remapped = remapped;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        ExprEvaluator[] expressionNodes = selectExprContext.getExpressionNodes();

        Object[] result = new Object[resultEventType.getPropertyNames().length];
        for (int i = 0; i < expressionNodes.length; i++) {
            result[remapped[i]] = expressionNodes[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        return selectExprContext.getEventAdapterService().adapterForTypedObjectArray(result, resultEventType);
    }
}
