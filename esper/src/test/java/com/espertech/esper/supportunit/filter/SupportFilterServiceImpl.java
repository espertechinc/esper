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
package com.espertech.esper.supportunit.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.filter.FilterHandle;
import com.espertech.esper.filter.FilterService;
import com.espertech.esper.filter.FilterServiceEntry;
import com.espertech.esper.filterspec.FilterValueSet;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SupportFilterServiceImpl implements FilterService {
    private List<Pair<FilterValueSet, FilterHandle>> added = new LinkedList<Pair<FilterValueSet, FilterHandle>>();
    private List<FilterHandle> removed = new LinkedList<FilterHandle>();

    public long evaluate(EventBean theEvent, Collection<FilterHandle> matches) {
        throw new UnsupportedOperationException();
    }

    public long evaluate(EventBean theEvent, Collection<FilterHandle> matches, int statementId) {
        throw new UnsupportedOperationException();
    }

    public FilterServiceEntry add(FilterValueSet filterValueSet, FilterHandle callback) {
        added.add(new Pair<FilterValueSet, FilterHandle>(filterValueSet, callback));
        return null;
    }

    public void remove(FilterHandle callback, FilterServiceEntry filterServiceEntry) {
        removed.add(callback);
    }

    public long getNumEventsEvaluated() {
        throw new UnsupportedOperationException();
    }

    public void resetStats() {
        throw new UnsupportedOperationException();
    }

    public List<Pair<FilterValueSet, FilterHandle>> getAdded() {
        return added;
    }

    public List<FilterHandle> getRemoved() {
        return removed;
    }

    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public long getFiltersVersion() {
        return Long.MIN_VALUE;
    }

    public void removeType(EventType type) {

    }
}
