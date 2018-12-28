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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventBean;

import java.util.Collection;
import java.util.Iterator;

public class AggregationMethodSortedWrapperValueIterator implements Iterator<Collection<EventBean>> {
    private final Iterator<Object> iterator;

    public AggregationMethodSortedWrapperValueIterator(Iterator<Object> iterator) {
        this.iterator = iterator;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public Collection<EventBean> next() {
        return AggregatorAccessSortedImpl.checkedPayloadGetCollEvents(iterator.next());
    }
}
