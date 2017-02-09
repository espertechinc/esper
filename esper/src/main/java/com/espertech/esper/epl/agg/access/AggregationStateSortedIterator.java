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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.collection.MixedEventBeanAndCollectionIteratorBase;

import java.util.SortedMap;
import java.util.TreeMap;

public class AggregationStateSortedIterator extends MixedEventBeanAndCollectionIteratorBase {
    private final SortedMap<Object, Object> window;

    /**
     * Ctor.
     *
     * @param window  - sorted map with events
     * @param reverse for reverse iterator
     */
    public AggregationStateSortedIterator(TreeMap<Object, Object> window, boolean reverse) {
        super(reverse ? window.descendingKeySet().iterator() : window.keySet().iterator());
        this.window = window;
        init();
    }

    protected Object getValue(Object iteratorKeyValue) {
        return window.get(iteratorKeyValue);
    }
}
