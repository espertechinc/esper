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
package com.espertech.esper.regressionlib.support.multithread;

import com.espertech.esper.common.internal.support.SupportBean;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class GeneratorIterator implements Iterator<Object> {
    public final static GeneratorIteratorCallback DEFAULT_SUPPORTEBEAN_CB = new GeneratorIteratorCallback() {
        public Object getObject(int numEvent) {
            return new SupportBean(Integer.toString(numEvent), numEvent);
        }
    };

    private final int maxNumEvents;
    private int numEvents;
    private final GeneratorIteratorCallback callback;

    public GeneratorIterator(int maxNumEvents, GeneratorIteratorCallback callback) {
        this.maxNumEvents = maxNumEvents;
        this.callback = callback;
    }

    public GeneratorIterator(int maxNumEvents) {
        this.maxNumEvents = maxNumEvents;
        this.callback = DEFAULT_SUPPORTEBEAN_CB;
    }

    public boolean hasNext() {
        if (numEvents < maxNumEvents) {
            return true;
        }
        return false;
    }

    public Object next() {
        if (numEvents >= maxNumEvents) {
            throw new NoSuchElementException();
        }
        Object event = callback.getObject(numEvents);
        numEvents++;
        return event;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
