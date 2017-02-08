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

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.HashMap;

public class ExpressionResultCacheForPropUnwrapImpl implements ExpressionResultCacheForPropUnwrap {

    private final HashMap<String, SoftReference<ExpressionResultCacheEntry<EventBean, Collection<EventBean>>>> collPropertyCache = new HashMap<String, SoftReference<ExpressionResultCacheEntry<EventBean, Collection<EventBean>>>>();

    public ExpressionResultCacheEntry<EventBean, Collection<EventBean>> getPropertyColl(String propertyNameFullyQualified, EventBean reference) {
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
}
