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
package com.espertech.esper.epl.expression.prev;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.view.window.RandomAccessByIndex;
import com.espertech.esper.view.window.RandomAccessByIndexGetter;
import com.espertech.esper.view.window.RelativeAccessByEventNIndex;
import com.espertech.esper.view.window.RelativeAccessByEventNIndexGetter;

import java.lang.reflect.Array;
import java.util.*;

public class ExprPreviousEvalStrategyWindow implements ExprPreviousEvalStrategy {
    private final int streamNumber;
    private final ExprEvaluator evalNode;
    private final Class componentType;
    private final RandomAccessByIndexGetter randomAccessGetter;
    private final RelativeAccessByEventNIndexGetter relativeAccessGetter;

    public ExprPreviousEvalStrategyWindow(int streamNumber, ExprEvaluator evalNode, Class componentType, RandomAccessByIndexGetter randomAccessGetter, RelativeAccessByEventNIndexGetter relativeAccessGetter) {
        this.streamNumber = streamNumber;
        this.evalNode = evalNode;
        this.componentType = componentType;
        this.randomAccessGetter = randomAccessGetter;
        this.relativeAccessGetter = relativeAccessGetter;
    }

    public Object evaluate(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Iterator<EventBean> events;
        int size;
        if (randomAccessGetter != null) {
            RandomAccessByIndex randomAccess = randomAccessGetter.getAccessor();
            events = randomAccess.getWindowIterator();
            size = (int) randomAccess.getWindowCount();
        } else {
            EventBean evalEvent = eventsPerStream[streamNumber];
            RelativeAccessByEventNIndex relativeAccess = relativeAccessGetter.getAccessor(evalEvent);
            if (relativeAccess == null) {
                return null;
            }
            size = relativeAccess.getWindowToEventCount();
            events = relativeAccess.getWindowToEvent();
        }

        if (size <= 0) {
            return null;
        }

        EventBean originalEvent = eventsPerStream[streamNumber];
        Object[] result = (Object[]) Array.newInstance(componentType, size);

        for (int i = 0; i < size; i++) {
            eventsPerStream[streamNumber] = events.next();
            Object evalResult = evalNode.evaluate(eventsPerStream, true, exprEvaluatorContext);
            result[i] = evalResult;
        }

        eventsPerStream[streamNumber] = originalEvent;
        return result;
    }

    public Collection<EventBean> evaluateGetCollEvents(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        Collection<EventBean> events;
        if (randomAccessGetter != null) {
            RandomAccessByIndex randomAccess = randomAccessGetter.getAccessor();
            events = randomAccess.getWindowCollectionReadOnly();
        } else {
            EventBean evalEvent = eventsPerStream[streamNumber];
            RelativeAccessByEventNIndex relativeAccess = relativeAccessGetter.getAccessor(evalEvent);
            if (relativeAccess == null) {
                return null;
            }
            events = relativeAccess.getWindowToEventCollReadOnly();
        }
        return events;
    }

    public Collection evaluateGetCollScalar(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        Iterator<EventBean> events;
        int size;
        if (randomAccessGetter != null) {
            RandomAccessByIndex randomAccess = randomAccessGetter.getAccessor();
            events = randomAccess.getWindowIterator();
            size = (int) randomAccess.getWindowCount();
        } else {
            EventBean evalEvent = eventsPerStream[streamNumber];
            RelativeAccessByEventNIndex relativeAccess = relativeAccessGetter.getAccessor(evalEvent);
            if (relativeAccess == null) {
                return null;
            }
            size = relativeAccess.getWindowToEventCount();
            events = relativeAccess.getWindowToEvent();
        }

        if (size <= 0) {
            return Collections.emptyList();
        }

        EventBean originalEvent = eventsPerStream[streamNumber];
        Deque deque = new ArrayDeque(size);
        for (int i = 0; i < size; i++) {
            eventsPerStream[streamNumber] = events.next();
            Object evalResult = evalNode.evaluate(eventsPerStream, true, context);
            deque.add(evalResult);
        }
        eventsPerStream[streamNumber] = originalEvent;
        return deque;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        return null;
    }
}