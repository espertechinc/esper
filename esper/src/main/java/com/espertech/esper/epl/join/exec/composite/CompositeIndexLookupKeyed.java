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
import com.espertech.esper.collection.MultiKeyUntyped;

import java.util.Map;
import java.util.Set;

public class CompositeIndexLookupKeyed implements CompositeIndexLookup {

    private final Object[] keys;
    private CompositeIndexLookup next;

    public CompositeIndexLookupKeyed(Object[] keys) {
        this.keys = keys;
    }

    public void setNext(CompositeIndexLookup next) {
        this.next = next;
    }

    public void lookup(Map parent, Set<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor) {
        MultiKeyUntyped mk = new MultiKeyUntyped(keys);
        Map innerIndex = (Map) parent.get(mk);
        if (innerIndex == null) {
            return;
        }
        next.lookup(innerIndex, result, postProcessor);
    }
}
