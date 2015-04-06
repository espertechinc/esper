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

import java.lang.ref.SoftReference;
import java.util.*;

public class ExpressionResultCacheServiceAgentInstance implements ExpressionResultCacheService{

    private HashMap<String, SoftReference<ExpressionResultCacheEntry<EventBean, Collection<EventBean>>>> collPropertyCache;
    private IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntry<EventBean[], Object>>> exprDeclCacheObject;
    private IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntry<EventBean[], Collection<EventBean>>>> exprDeclCacheCollection;
    private IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntry<Long[], Object>>> enumMethodCache;

    private Deque<ExpressionResultCacheStackEntry> callStack;
    private Deque<Long> lastValueCacheStack;

    public ExpressionResultCacheServiceAgentInstance() {
    }

    public void pushStack(ExpressionResultCacheStackEntry lambda) {
        if (callStack == null) {
            callStack = new ArrayDeque<ExpressionResultCacheStackEntry>();
            lastValueCacheStack = new ArrayDeque<Long>(10);
        }
        callStack.push(lambda);
    }

    public boolean popLambda() {
        callStack.remove();
        return callStack.isEmpty();
    }

    public Deque<ExpressionResultCacheStackEntry> getStack() {
        return callStack;
    }

    public void destroy() {
    }

    public ExpressionResultCacheEntry<EventBean, Collection<EventBean>> getPropertyColl(String propertyNameFullyQualified, EventBean reference) {
        initPropertyCollCache();
        SoftReference<ExpressionResultCacheEntry<EventBean, Collection<EventBean>>> cacheRef = collPropertyCache.get(propertyNameFullyQualified);
        if (cacheRef == null) {
            return null;
        }
        ExpressionResultCacheEntry<EventBean, Collection<EventBean>> entry = cacheRef.get();
        if (entry == null) {
            return null;
        }
        if (entry.getReference() != reference) {
            return null;
        }
        return entry;
    }

    public void savePropertyColl(String propertyNameFullyQualified, EventBean reference, Collection<EventBean> events) {
        ExpressionResultCacheEntry<EventBean, Collection<EventBean>> entry = new ExpressionResultCacheEntry<EventBean, Collection<EventBean>>(reference, events);
        collPropertyCache.put(propertyNameFullyQualified, new SoftReference<ExpressionResultCacheEntry<EventBean, Collection<EventBean>>>(entry));
    }

    public ExpressionResultCacheEntry<EventBean[], Object> getDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream) {
        initExprDeclaredCacheObject();
        SoftReference<ExpressionResultCacheEntry<EventBean[], Object>> cacheRef = this.exprDeclCacheObject.get(node);
        if (cacheRef == null) {
            return null;
        }
        ExpressionResultCacheEntry<EventBean[], Object> entry = cacheRef.get();
        if (entry == null) {
            return null;
        }
        EventBean[] cacheEvents = entry.getReference();
        if (cacheEvents.length != eventsPerStream.length) {
            return null;
        }
        for (int i = 0; i < cacheEvents.length; i++) {
            if (cacheEvents[i] != eventsPerStream[i]) {
                return null;
            }
        }
        return entry;
    }

    public void saveDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream, Object result) {
        EventBean[] copy = new EventBean[eventsPerStream.length];
        System.arraycopy(eventsPerStream, 0, copy, 0, copy.length);
        ExpressionResultCacheEntry<EventBean[], Object> entry = new ExpressionResultCacheEntry<EventBean[], Object>(copy, result);
        exprDeclCacheObject.put(node, new SoftReference<ExpressionResultCacheEntry<EventBean[], Object>>(entry));
    }

    public ExpressionResultCacheEntry<EventBean[], Collection<EventBean>> getDeclaredExpressionLastColl(Object node, EventBean[] eventsPerStream) {
        initExprDeclaredCacheCollection();
        SoftReference<ExpressionResultCacheEntry<EventBean[], Collection<EventBean>>> cacheRef = this.exprDeclCacheCollection.get(node);
        if (cacheRef == null) {
            return null;
        }
        ExpressionResultCacheEntry<EventBean[], Collection<EventBean>> entry = cacheRef.get();
        if (entry == null) {
            return null;
        }
        EventBean[] cacheEvents = entry.getReference();
        if (cacheEvents.length != eventsPerStream.length) {
            return null;
        }
        for (int i = 0; i < cacheEvents.length; i++) {
            if (cacheEvents[i] != eventsPerStream[i]) {
                return null;
            }
        }
        return entry;
    }

    public void saveDeclaredExpressionLastColl(Object node, EventBean[] eventsPerStream, Collection<EventBean> result) {
        EventBean[] copy = new EventBean[eventsPerStream.length];
        System.arraycopy(eventsPerStream, 0, copy, 0, copy.length);
        ExpressionResultCacheEntry<EventBean[], Collection<EventBean>> entry = new ExpressionResultCacheEntry<EventBean[], Collection<EventBean>>(copy, result);
        exprDeclCacheCollection.put(node, new SoftReference<ExpressionResultCacheEntry<EventBean[], Collection<EventBean>>>(entry));
    }

    public ExpressionResultCacheEntry<Long[], Object> getEnumerationMethodLastValue(Object node) {
        initEnumMethodCache();
        SoftReference<ExpressionResultCacheEntry<Long[], Object>> cacheRef = enumMethodCache.get(node);
        if (cacheRef == null) {
            return null;
        }
        ExpressionResultCacheEntry<Long[], Object> entry = cacheRef.get();
        if (entry == null) {
            return null;
        }
        Long[] required = entry.getReference();
        if (required.length != lastValueCacheStack.size()) {
            return null;
        }
        Iterator<Long> prov = lastValueCacheStack.iterator();
        for (int i = 0; i < lastValueCacheStack.size(); i++) {
            if (!required[i].equals(prov.next())) {
                return null;
            }
        }
        return entry;
    }

    public void saveEnumerationMethodLastValue(Object node, Object result) {
        Long[] snapshot = lastValueCacheStack.toArray(new Long[lastValueCacheStack.size()]);
        ExpressionResultCacheEntry<Long[], Object> entry = new ExpressionResultCacheEntry<Long[], Object>(snapshot, result);
        enumMethodCache.put(node, new SoftReference<ExpressionResultCacheEntry<Long[], Object>>(entry));
    }

    private void initEnumMethodCache() {
        if (enumMethodCache == null) {
            enumMethodCache = new IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntry<Long[], Object>>>();
        }
    }

    private void initPropertyCollCache() {
        if (collPropertyCache == null) {
            collPropertyCache = new HashMap<String, SoftReference<ExpressionResultCacheEntry<EventBean, Collection<EventBean>>>>();
        }
    }

    private void initExprDeclaredCacheObject() {
        if (exprDeclCacheObject == null) {
            exprDeclCacheObject = new IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntry<EventBean[], Object>>>();
        }
    }

    private void initExprDeclaredCacheCollection() {
        if (exprDeclCacheCollection == null) {
            exprDeclCacheCollection = new IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntry<EventBean[], Collection<EventBean>>>>();
        }
    }

    public void pushContext(long contextNumber) {
        if (callStack == null) {
            callStack = new ArrayDeque<ExpressionResultCacheStackEntry>();
            lastValueCacheStack = new ArrayDeque<Long>(10);
        }
        lastValueCacheStack.push(contextNumber);
    }

    public void popContext() {
        lastValueCacheStack.remove();
    }

}
