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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class AgentInstanceArrayIterator implements Iterator<EventBean> {

    protected final AgentInstance[] instances;
    private int currentIndex;
    private Iterator<EventBean> currentIterator;

    public AgentInstanceArrayIterator(AgentInstance[] instances) {
        this.instances = instances;
        if (instances.length > 0) {
            currentIterator = instances[0].getFinalView().iterator();
        }
    }

    public boolean hasNext() {
        if (currentIterator != null) {
            if (currentIterator.hasNext()) {
                return true;
            }
        }
        moveNext();
        return currentIterator != null && currentIterator.hasNext();
    }

    public EventBean next() {
        if (currentIterator != null) {
            if (currentIterator.hasNext()) {
                return currentIterator.next();
            }
        }
        moveNext();
        if (currentIterator != null) {
            if (currentIterator.hasNext()) {
                return currentIterator.next();
            }
        }
        throw new NoSuchElementException();
    }

    private void moveNext() {
        if (currentIndex >= instances.length - 1) {
            return;
        }
        do {
            currentIndex++;
            currentIterator = instances[currentIndex].getFinalView().iterator();
        }
        while (currentIndex < instances.length - 1 && !currentIterator.hasNext());
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
