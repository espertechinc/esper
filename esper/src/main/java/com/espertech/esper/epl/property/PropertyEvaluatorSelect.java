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
package com.espertech.esper.epl.property;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.filterspec.PropertyEvaluator;

import java.util.ArrayDeque;

/**
 * Property evaluator that considers a select-clauses and relies
 * on an accumulative property evaluator that presents events for all columns and rows.
 */
public class PropertyEvaluatorSelect implements PropertyEvaluator {
    private final EventType resultEventType;
    private final SelectExprProcessor selectExprProcessor;
    private final PropertyEvaluatorAccumulative accumulative;

    public PropertyEvaluatorSelect(EventType resultEventType, SelectExprProcessor selectExprProcessor, PropertyEvaluatorAccumulative accumulative) {
        this.resultEventType = resultEventType;
        this.selectExprProcessor = selectExprProcessor;
        this.accumulative = accumulative;
    }

    public EventBean[] getProperty(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        ArrayDeque<EventBean[]> rows = accumulative.getAccumulative(theEvent, exprEvaluatorContext);
        if ((rows == null) || (rows.isEmpty())) {
            return null;
        }
        ArrayDeque<EventBean> result = new ArrayDeque<EventBean>();
        for (EventBean[] row : rows) {
            EventBean bean = selectExprProcessor.process(row, true, false, exprEvaluatorContext);
            result.add(bean);
        }
        return result.toArray(new EventBean[result.size()]);
    }

    public EventType getFragmentEventType() {
        return resultEventType;
    }

    public boolean compareTo(PropertyEvaluator otherFilterPropertyEval) {
        return false;
    }
}