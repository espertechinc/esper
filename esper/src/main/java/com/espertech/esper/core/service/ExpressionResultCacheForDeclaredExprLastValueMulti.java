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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.RollingTwoValueBuffer;
import com.espertech.esper.event.EventBeanUtility;

import java.lang.ref.SoftReference;
import java.util.IdentityHashMap;

public class ExpressionResultCacheForDeclaredExprLastValueMulti implements ExpressionResultCacheForDeclaredExprLastValue {

    private final int cacheSize;
    private final ExpressionResultCacheEntryEventBeanArrayAndObj resultCacheEntry = new ExpressionResultCacheEntryEventBeanArrayAndObj(null, null);
    private final IdentityHashMap<Object, SoftReference<RollingTwoValueBuffer<EventBean[], Object>>> cache
            = new IdentityHashMap<Object, SoftReference<RollingTwoValueBuffer<EventBean[], Object>>>();

    public ExpressionResultCacheForDeclaredExprLastValueMulti(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public boolean cacheEnabled() {
        return true;
    }

    public ExpressionResultCacheEntryEventBeanArrayAndObj getDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream) {
        SoftReference<RollingTwoValueBuffer<EventBean[], Object>> cacheRef = cache.get(node);
        if (cacheRef == null) {
            return null;
        }
        RollingTwoValueBuffer<EventBean[], Object> entry = cacheRef.get();
        if (entry == null) {
            return null;
        }
        for (int i = 0; i < entry.getBufferA().length; i++) {
            EventBean[] key = entry.getBufferA()[i];
            if (key != null && EventBeanUtility.compareEventReferences(key, eventsPerStream)) {
                resultCacheEntry.setReference(key);
                resultCacheEntry.setResult(entry.getBufferB()[i]);
                return resultCacheEntry;
            }
        }
        return null;
    }

    public void saveDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream, Object result) {
        SoftReference<RollingTwoValueBuffer<EventBean[], Object>> cacheRef = cache.get(node);

        RollingTwoValueBuffer<EventBean[], Object> buf;
        if (cacheRef == null) {
            buf = new RollingTwoValueBuffer<EventBean[], Object>(new EventBean[cacheSize][], new Object[cacheSize]);
            cache.put(node, new SoftReference<RollingTwoValueBuffer<EventBean[], Object>>(buf));
        } else {
            buf = cacheRef.get();
            if (buf == null) {
                buf = new RollingTwoValueBuffer<EventBean[], Object>(new EventBean[cacheSize][], new Object[cacheSize]);
                cache.put(node, new SoftReference<RollingTwoValueBuffer<EventBean[], Object>>(buf));
            }
        }

        EventBean[] copy = new EventBean[eventsPerStream.length];
        System.arraycopy(eventsPerStream, 0, copy, 0, copy.length);
        buf.add(copy, result);
    }
}
