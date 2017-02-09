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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Iterator for use by {@link com.espertech.esper.epl.join.table.PropertySortedEventTable}.
 */
public final class PropertySortedEventTableIterator implements Iterator<EventBean> {
    private final Map<Object, Set<EventBean>> window;

    private final Iterator<Object> keyIterator;
    private Iterator<EventBean> currentListIterator;

    /**
     * Ctor.
     *
     * @param window - sorted map with events
     */
    public PropertySortedEventTableIterator(Map<Object, Set<EventBean>> window) {
        this.window = window;
        keyIterator = window.keySet().iterator();
        if (keyIterator.hasNext()) {
            Object initialKey = keyIterator.next();
            currentListIterator = window.get(initialKey).iterator();
        }
    }

    public final EventBean next() {
        if (currentListIterator == null) {
            throw new NoSuchElementException();
        }

        EventBean eventBean = currentListIterator.next();

        if (!currentListIterator.hasNext()) {
            currentListIterator = null;
            if (keyIterator.hasNext()) {
                Object nextKey = keyIterator.next();
                currentListIterator = window.get(nextKey).iterator();
            }
        }

        return eventBean;
    }

    public final boolean hasNext() {
        if (currentListIterator == null) {
            return false;
        }

        if (currentListIterator.hasNext()) {
            return true;
        }

        currentListIterator = null;

        if (!keyIterator.hasNext()) {
            return false;
        }

        return true;
    }

    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
