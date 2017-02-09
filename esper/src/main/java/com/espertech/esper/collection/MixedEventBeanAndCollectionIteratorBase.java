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
package com.espertech.esper.collection;

import com.espertech.esper.client.EventBean;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class MixedEventBeanAndCollectionIteratorBase implements Iterator<EventBean> {

    private final Iterator keyIterator;
    private Iterator<EventBean> currentListIterator;
    private EventBean currentItem;

    protected abstract Object getValue(Object iteratorKeyValue);

    protected MixedEventBeanAndCollectionIteratorBase(Iterator keyIterator) {
        this.keyIterator = keyIterator;
    }

    protected void init() {
        if (keyIterator.hasNext()) {
            goToNext();
        }
    }

    public final EventBean next() {
        if (currentListIterator == null && currentItem == null) {
            throw new NoSuchElementException();
        }

        EventBean eventBean;
        if (currentListIterator != null) {
            eventBean = currentListIterator.next();

            if (!currentListIterator.hasNext()) {
                currentListIterator = null;
                currentItem = null;
                if (keyIterator.hasNext()) {
                    goToNext();
                }
            }
        } else {
            eventBean = currentItem;
            currentItem = null;
            if (keyIterator.hasNext()) {
                goToNext();
            }
        }

        return eventBean;
    }

    public final boolean hasNext() {
        if (currentListIterator == null && currentItem == null) {
            return false;
        }

        if (currentItem != null) {
            return true;
        }

        if (currentListIterator.hasNext()) {
            return true;
        }

        currentListIterator = null;
        currentItem = null;

        return keyIterator.hasNext();
    }

    public final void remove() {
        throw new UnsupportedOperationException();
    }

    private void goToNext() {
        Object nextKey = keyIterator.next();
        Object entry = getValue(nextKey);
        while (true) {
            if (entry instanceof Collection) {
                currentListIterator = ((Collection<EventBean>) entry).iterator();
                if (currentListIterator.hasNext()) {
                    break;
                } else {
                    currentListIterator = null;
                }
            } else if (entry instanceof EventBean) {
                currentItem = (EventBean) entry;
                break;
            }

            // next key
            if (keyIterator.hasNext()) {
                nextKey = keyIterator.next();
                entry = getValue(nextKey);
            } else {
                break;
            }
        }
    }
}
