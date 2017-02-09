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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.*;

public class EnumEvalOrderByAscDescEvents extends EnumEvalBase implements EnumEval {

    private final boolean descending;

    public EnumEvalOrderByAscDescEvents(ExprEvaluator innerExpression, int streamCountIncoming, boolean descending) {
        super(innerExpression, streamCountIncoming);
        this.descending = descending;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        TreeMap<Comparable, Object> sort = new TreeMap<Comparable, Object>();
        boolean hasColl = false;

        Collection<EventBean> beans = (Collection<EventBean>) target;
        for (EventBean next : beans) {
            eventsLambda[streamNumLambda] = next;

            Comparable comparable = (Comparable) innerExpression.evaluate(eventsLambda, isNewData, context);
            Object entry = sort.get(comparable);

            if (entry == null) {
                sort.put(comparable, next);
                continue;
            }

            if (entry instanceof Collection) {
                ((Collection) entry).add(next);
                continue;
            }

            Deque<Object> coll = new ArrayDeque<Object>();
            coll.add(entry);
            coll.add(next);
            sort.put(comparable, coll);
            hasColl = true;
        }

        Map<Comparable, Object> sorted;
        if (descending) {
            sorted = sort.descendingMap();
        } else {
            sorted = sort;
        }

        if (!hasColl) {
            return sorted.values();
        }

        Deque<Object> coll = new ArrayDeque<Object>();
        for (Map.Entry<Comparable, Object> entry : sorted.entrySet()) {
            if (entry.getValue() instanceof Collection) {
                coll.addAll((Collection) entry.getValue());
            } else {
                coll.add(entry.getValue());
            }
        }
        return coll;
    }
}
