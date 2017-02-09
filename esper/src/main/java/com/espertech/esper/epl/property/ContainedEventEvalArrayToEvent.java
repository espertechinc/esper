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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanManufacturer;

import java.lang.reflect.Array;

public class ContainedEventEvalArrayToEvent implements ContainedEventEval {

    private final ExprEvaluator evaluator;
    private final EventBeanManufacturer manufacturer;

    public ContainedEventEvalArrayToEvent(ExprEvaluator evaluator, EventBeanManufacturer manufacturer) {
        this.evaluator = evaluator;
        this.manufacturer = manufacturer;
    }

    public Object getFragment(EventBean eventBean, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Object result = evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);

        if (result == null) {
            return null;
        }

        EventBean[] events = new EventBean[Array.getLength(result)];
        for (int i = 0; i < events.length; i++) {
            Object column = Array.get(result, i);
            if (column != null) {
                events[i] = manufacturer.make(new Object[]{column});
            }
        }
        return events;
    }
}
