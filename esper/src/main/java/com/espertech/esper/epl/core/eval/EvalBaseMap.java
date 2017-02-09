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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class EvalBaseMap extends EvalBase implements SelectExprProcessor {

    protected EvalBaseMap(SelectExprContext selectExprContext, EventType resultEventType) {
        super(selectExprContext, resultEventType);
    }

    public abstract EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext);

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        ExprEvaluator[] expressionNodes = selectExprContext.getExpressionNodes();
        String[] columnNames = selectExprContext.getColumnNames();

        // Evaluate all expressions and build a map of name-value pairs
        Map<String, Object> props;
        if (expressionNodes.length == 0) {
            props = Collections.EMPTY_MAP;
        } else {
            props = new HashMap<String, Object>();
            for (int i = 0; i < expressionNodes.length; i++) {
                Object evalResult = expressionNodes[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                props.put(columnNames[i], evalResult);
            }
        }

        return processSpecific(props, eventsPerStream, isNewData, isSynthesize, exprEvaluatorContext);
    }
}