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
import com.espertech.esper.client.EventBeanFactory;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ContainedEventEvalExprNode implements ContainedEventEval {

    private final ExprEvaluator evaluator;
    private final EventBeanFactory eventBeanFactory;

    public ContainedEventEvalExprNode(ExprEvaluator evaluator, EventBeanFactory eventBeanFactory) {
        this.evaluator = evaluator;
        this.eventBeanFactory = eventBeanFactory;
    }

    public Object getFragment(EventBean eventBean, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Object result = evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);

        if (result == null) {
            return null;
        }

        if (result.getClass().isArray()) {
            EventBean[] events = new EventBean[Array.getLength(result)];
            for (int i = 0; i < events.length; i++) {
                Object arrayItem = Array.get(result, i);
                if (arrayItem != null) {
                    events[i] = eventBeanFactory.wrap(arrayItem);
                }
            }
            return events;
        }

        if (result instanceof Collection) {
            Collection coll = (Collection) result;
            EventBean[] events = new EventBean[coll.size()];
            Iterator it = coll.iterator();
            for (int i = 0; i < events.length; i++) {
                Object collItem = it.next();
                if (collItem != null) {
                    events[i] = eventBeanFactory.wrap(collItem);
                }
            }
            return events;
        }

        if (result instanceof Iterable) {
            Iterable iterable = (Iterable) result;
            List<EventBean> events = new ArrayList<EventBean>();
            for (Object item : iterable) {
                if (item != null) {
                    events.add(eventBeanFactory.wrap(item));
                }
            }
            return events.toArray(new EventBean[events.size()]);
        }

        return null;
    }
}
