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
import com.espertech.esper.event.EventBeanUtility;

import java.lang.ref.SoftReference;
import java.util.IdentityHashMap;

public class ExpressionResultCacheForDeclaredExprLastValueSingle implements ExpressionResultCacheForDeclaredExprLastValue {

    private final IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntryEventBeanArrayAndObj>> exprDeclCacheObject
            = new IdentityHashMap<Object, SoftReference<ExpressionResultCacheEntryEventBeanArrayAndObj>>();

    public boolean cacheEnabled() {
        return true;
    }

    public ExpressionResultCacheEntryEventBeanArrayAndObj getDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream) {
        SoftReference<ExpressionResultCacheEntryEventBeanArrayAndObj> cacheRef = this.exprDeclCacheObject.get(node);
        if (cacheRef == null) {
            return null;
        }
        ExpressionResultCacheEntryEventBeanArrayAndObj entry = cacheRef.get();
        if (entry == null) {
            return null;
        }
        return EventBeanUtility.compareEventReferences(entry.getReference(), eventsPerStream) ? entry : null;
    }

    public void saveDeclaredExpressionLastValue(Object node, EventBean[] eventsPerStream, Object result) {
        EventBean[] copy = EventBeanUtility.copyArray(eventsPerStream);
        ExpressionResultCacheEntryEventBeanArrayAndObj entry = new ExpressionResultCacheEntryEventBeanArrayAndObj(copy, result);
        exprDeclCacheObject.put(node, new SoftReference<ExpressionResultCacheEntryEventBeanArrayAndObj>(entry));
    }
}
