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

/**
 * Provides 3 caches on the statement-level:
 * <p>
 * (A) On the level of indexed event properties:
 *     Properties that are wrapped in EventBean instances, such as for Enumeration Methods, get wrapped only once for the same event.
 *     The cache is keyed by property-name and EventBean reference and maintains a Collection<EventBean>.
 * <p>
 * (B) On the level of enumeration method:
 *     If a enumeration method expression is invoked within another enumeration method expression (not counting expression declarations),
 *     for example "source.where(a => source.minBy(b => b.x))" the "source.minBy(b => b.x)" is not dependent on any other lambda so the result gets cached.
 *     The cache is keyed by the enumeration-method-node as an IdentityHashMap and verified by a context stack (Long[]) that is built in nested evaluation calls.
 * <p>
 * (C) On the level of expression declaration:
 *     a) for non-enum evaluation and for enum-evaluation a separate cache
 *     b) The cache is keyed by the prototype-node as an IdentityHashMap and verified by a events-per-stream (EventBean[]) that is maintained or rewritten.
 */
public interface ExpressionResultCacheService {

    public void pushStack(ExpressionResultCacheStackEntry lambda);
    public boolean popLambda();
    public Deque<ExpressionResultCacheStackEntry> getStack();
    public ExpressionResultCacheEntry<EventBean, Collection<EventBean>> getPropertyColl(String propertyNameFullyQualified, EventBean reference);
    public void savePropertyColl(String propertyNameFullyQualified, EventBean reference, Collection<EventBean> events);
    public ExpressionResultCacheEntry<EventBean[], Object> getDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream);
    public void saveDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream, Object result);
    public ExpressionResultCacheEntry<EventBean[], Collection<EventBean>> getDeclaredExpressionLastColl(Object node, EventBean[] eventsPerStream);
    public void saveDeclaredExpressionLastColl(Object node, EventBean[] eventsPerStream, Collection<EventBean> result);
    public ExpressionResultCacheEntry<Long[], Object> getEnumerationMethodLastValue(Object node);
    public void saveEnumerationMethodLastValue(Object node, Object result);
    public void pushContext(long contextNumber);
    public void popContext();
}
