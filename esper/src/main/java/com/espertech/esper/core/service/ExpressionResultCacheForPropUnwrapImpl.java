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

    private final HashMap<String, SoftReference<ExpressionResultCacheEntryBeanAndCollBean>> collPropertyCache = new HashMap<>();

    public ExpressionResultCacheEntryBeanAndCollBean getPropertyColl(String propertyNameFullyQualified, EventBean reference) {
        SoftReference<ExpressionResultCacheEntryBeanAndCollBean> cacheRef = collPropertyCache.get(propertyNameFullyQualified);
        if (cacheRef == null) {
            return null;
        }
        ExpressionResultCacheEntryBeanAndCollBean entry = cacheRef.get();
        if (entry == null) {
            return null;
        }
        if (entry.getReference() != reference) {
            return null;
        }
        return entry;
    }

    public void savePropertyColl(String propertyNameFullyQualified, EventBean reference, Collection<EventBean> events) {
        ExpressionResultCacheEntryBeanAndCollBean entry = new ExpressionResultCacheEntryBeanAndCollBean(reference, events);
        collPropertyCache.put(propertyNameFullyQualified, new SoftReference<>(entry));
    }
}
