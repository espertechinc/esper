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
package com.espertech.esper.core.service;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;

public class ExpressionResultCacheForEnumerationMethodImpl implements ExpressionResultCacheForEnumerationMethod {

    private final IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntryLongArrayAndObj>> enumMethodCache
            = new IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntryLongArrayAndObj>>();

    private Deque<ExpressionResultCacheStackEntry> callStack;
    private Deque<Long> lastValueCacheStack;

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

    public ExpressionResultCacheEntryLongArrayAndObj getEnumerationMethodLastValue(Object node) {
        SoftReference<ExpressionResultCacheEntryLongArrayAndObj> cacheRef = enumMethodCache.get(node);
        if (cacheRef == null) {
            return null;
        }
        ExpressionResultCacheEntryLongArrayAndObj entry = cacheRef.get();
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
        ExpressionResultCacheEntryLongArrayAndObj entry = new ExpressionResultCacheEntryLongArrayAndObj(snapshot, result);
        enumMethodCache.put(node, new SoftReference<ExpressionResultCacheEntryLongArrayAndObj>(entry));
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
