/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;

import java.util.Collection;
import java.util.Deque;

public class ExpressionResultCacheServiceThreadlocal implements ExpressionResultCacheService {
    private ThreadLocal<ExpressionResultCacheServiceAgentInstance> threadCache = new ThreadLocal<ExpressionResultCacheServiceAgentInstance>()
    {
        protected synchronized ExpressionResultCacheServiceAgentInstance initialValue()
        {
            return new ExpressionResultCacheServiceAgentInstance();
        }
    };

    public ExpressionResultCacheServiceThreadlocal() {
        init();
    }

    public void destroy() {
        init();
    }

    public void init() {
        threadCache = new ThreadLocal<ExpressionResultCacheServiceAgentInstance>()
        {
            protected synchronized ExpressionResultCacheServiceAgentInstance initialValue()
            {
                return new ExpressionResultCacheServiceAgentInstance();
            }
        };
    }

    public void pushStack(ExpressionResultCacheStackEntry lambda) {
        threadCache.get().pushStack(lambda);
    }

    public boolean popLambda() {
        return threadCache.get().popLambda();
    }

    public Deque<ExpressionResultCacheStackEntry> getStack() {
        return threadCache.get().getStack();
    }

    public ExpressionResultCacheEntry<EventBean, Collection<EventBean>> getPropertyColl(String propertyNameFullyQualified, EventBean reference) {
        return threadCache.get().getPropertyColl(propertyNameFullyQualified, reference);
    }

    public void savePropertyColl(String propertyNameFullyQualified, EventBean reference, Collection<EventBean> events) {
        threadCache.get().savePropertyColl(propertyNameFullyQualified, reference, events);
    }

    public ExpressionResultCacheEntry<EventBean[], Object> getDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream) {
        return threadCache.get().getDeclaredExpressionLastValue(node, eventsPerStream);
    }

    public void saveDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream, Object result) {
        threadCache.get().saveDeclaredExpressionLastValue(node, eventsPerStream, result);
    }

    public ExpressionResultCacheEntry<EventBean[], Collection<EventBean>> getDeclaredExpressionLastColl(Object node, EventBean[] eventsPerStream) {
        return threadCache.get().getDeclaredExpressionLastColl(node, eventsPerStream);
    }

    public void saveDeclaredExpressionLastColl(Object node, EventBean[] eventsPerStream, Collection<EventBean> result) {
        threadCache.get().saveDeclaredExpressionLastColl(node, eventsPerStream, result);
    }

    public ExpressionResultCacheEntry<Long[], Object> getEnumerationMethodLastValue(Object node) {
        return threadCache.get().getEnumerationMethodLastValue(node);
    }

    public void saveEnumerationMethodLastValue(Object node, Object result) {
        threadCache.get().saveEnumerationMethodLastValue(node, result);
    }

    public void pushContext(long contextNumber) {
        threadCache.get().pushContext(contextNumber);
    }

    public void popContext() {
        threadCache.get().popContext();
    }
}
