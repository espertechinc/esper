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
package com.espertech.esper.common.internal.epl.join.exec.composite;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.collection.HashableMultiKey;

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class CompositeIndexEnterRemoveKeyed implements CompositeIndexEnterRemove {

    private final EventPropertyValueGetter hashGetter;
    private CompositeIndexEnterRemove next;

    public CompositeIndexEnterRemoveKeyed(EventPropertyValueGetter hashGetter) {
        this.hashGetter = hashGetter;
    }

    public void setNext(CompositeIndexEnterRemove next) {
        this.next = next;
    }

    public void enter(EventBean theEvent, Map parent) {
        Object mk = hashGetter.get(theEvent);
        Map innerIndex = (Map) parent.get(mk);
        if (innerIndex == null) {
            innerIndex = new TreeMap<Object, Object>();
            parent.put(mk, innerIndex);
        }
        next.enter(theEvent, innerIndex);
    }

    public void remove(EventBean theEvent, Map parent) {
        Object mk = hashGetter.get(theEvent);
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
        Map<HashableMultiKey, Map> map = parent;
        for (Map.Entry<HashableMultiKey, Map> entry : map.entrySet()) {
            next.getAll(result, entry.getValue());
        }
    }
}
