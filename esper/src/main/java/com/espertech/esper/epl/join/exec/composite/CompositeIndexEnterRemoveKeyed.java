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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.event.EventBeanUtility;

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class CompositeIndexEnterRemoveKeyed implements CompositeIndexEnterRemove {

    private final EventPropertyGetter[] propertyGetters;
    private final Class[] keyCoercionTypes;
    private CompositeIndexEnterRemove next;

    public CompositeIndexEnterRemoveKeyed(EventType eventType, String[] keysProps, Class[] keyCoercionTypes) {
        this.keyCoercionTypes = keyCoercionTypes;
        propertyGetters = new EventPropertyGetter[keysProps.length];
        for (int i = 0; i < keysProps.length; i++) {
            propertyGetters[i] = EventBeanUtility.getAssertPropertyGetter(eventType, keysProps[i]);
        }
    }

    public void setNext(CompositeIndexEnterRemove next) {
        this.next = next;
    }

    public void enter(EventBean theEvent, Map parent) {
        MultiKeyUntyped mk = EventBeanUtility.getMultiKey(theEvent, propertyGetters, keyCoercionTypes);
        Map innerIndex = (Map) parent.get(mk);
        if (innerIndex == null) {
            innerIndex = new TreeMap<Object, Object>();
            parent.put(mk, innerIndex);
        }
        next.enter(theEvent, innerIndex);
    }

    public void remove(EventBean theEvent, Map parent) {
        MultiKeyUntyped mk = EventBeanUtility.getMultiKey(theEvent, propertyGetters, keyCoercionTypes);
        Map innerIndex = (Map) parent.get(mk);
        if (innerIndex == null) {
            return;
        }
        next.remove(theEvent, innerIndex);
        if (innerIndex.isEmpty()) {
            parent.remove(mk);
        }
    }

    public void getAll(HashSet<EventBean> result, Map parent) {
        Map<MultiKeyUntyped, Map> map = parent;
        for (Map.Entry<MultiKeyUntyped, Map> entry : map.entrySet()) {
            next.getAll(result, entry.getValue());
        }
    }
}
