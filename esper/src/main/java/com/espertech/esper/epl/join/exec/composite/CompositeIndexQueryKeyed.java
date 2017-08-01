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
package com.espertech.esper.epl.join.exec.composite;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryHashKeyed;
import com.espertech.esper.event.EventBeanUtility;

import java.util.*;

public class CompositeIndexQueryKeyed implements CompositeIndexQuery {

    private final ExprEvaluator[] evaluators;
    private final Class[] keyCoercionTypes;
    private final int lookupStream;
    private final EventBean[] events;
    private final boolean isNWOnTrigger;

    private CompositeIndexQuery next;

    public CompositeIndexQueryKeyed(boolean isNWOnTrigger, int lookupStream, int numStreams, List<QueryGraphValueEntryHashKeyed> hashKeys, Class[] keyCoercionTypes) {
        this.keyCoercionTypes = keyCoercionTypes;
        this.evaluators = new ExprEvaluator[hashKeys.size()];
        this.isNWOnTrigger = isNWOnTrigger;
        this.lookupStream = lookupStream;

        for (int i = 0; i < evaluators.length; i++) {
            evaluators[i] = hashKeys.get(i).getKeyExpr().getForge().getExprEvaluator();
        }
        if (lookupStream != -1) {
            events = new EventBean[lookupStream + 1];
        } else {
            events = new EventBean[numStreams + 1];
        }
    }

    public void setNext(CompositeIndexQuery next) {
        this.next = next;
    }

    public Set<EventBean> get(EventBean theEvent, Map parent, ExprEvaluatorContext context, CompositeIndexQueryResultPostProcessor postProcessor) {
        events[lookupStream] = theEvent;
        MultiKeyUntyped mk = EventBeanUtility.getMultiKey(events, evaluators, context, keyCoercionTypes);
        Map innerIndex = (Map) parent.get(mk);
        if (innerIndex == null) {
            return null;
        }
        return next.get(theEvent, innerIndex, context, postProcessor);
    }

    public Set<EventBean> getCollectKeys(EventBean theEvent, Map parent, ExprEvaluatorContext context, ArrayList<Object> keys, CompositeIndexQueryResultPostProcessor postProcessor) {
        events[lookupStream] = theEvent;
        MultiKeyUntyped mk = EventBeanUtility.getMultiKey(events, evaluators, context, keyCoercionTypes);
        Collections.addAll(keys, mk.getKeys());
        Map innerIndex = (Map) parent.get(mk);
        if (innerIndex == null) {
            return null;
        }
        return next.getCollectKeys(theEvent, innerIndex, context, keys, postProcessor);
    }

    public Collection<EventBean> get(EventBean[] eventsPerStream, Map parent, ExprEvaluatorContext context, CompositeIndexQueryResultPostProcessor postProcessor) {

        EventBean[] eventsToUse;
        if (isNWOnTrigger) {
            eventsToUse = eventsPerStream;
        } else {
            System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);
            eventsToUse = events;
        }

        MultiKeyUntyped mk = EventBeanUtility.getMultiKey(eventsToUse, evaluators, context, keyCoercionTypes);
        Map innerIndex = (Map) parent.get(mk);
        if (innerIndex == null) {
            return null;
        }
        return next.get(eventsPerStream, innerIndex, context, postProcessor);
    }

    public Collection<EventBean> getCollectKeys(EventBean[] eventsPerStream, Map parent, ExprEvaluatorContext context, ArrayList<Object> keys, CompositeIndexQueryResultPostProcessor postProcessor) {

        EventBean[] eventsToUse;
        if (isNWOnTrigger) {
            eventsToUse = eventsPerStream;
        } else {
            System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);
            eventsToUse = events;
        }

        MultiKeyUntyped mk = EventBeanUtility.getMultiKey(eventsToUse, evaluators, context, keyCoercionTypes);
        Collections.addAll(keys, mk.getKeys());
        Map innerIndex = (Map) parent.get(mk);
        if (innerIndex == null) {
            return null;
        }
        return next.getCollectKeys(eventsPerStream, innerIndex, context, keys, postProcessor);
    }

    public void add(EventBean theEvent, Map value, Set<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor) {
        throw new UnsupportedOperationException();
    }

    public void add(EventBean[] eventsPerStream, Map value, Collection<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor) {
        throw new UnsupportedOperationException();
    }
}
