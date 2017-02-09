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
package com.espertech.esper.example.transaction.sim;

import com.espertech.esper.example.transaction.TxnEventBase;

import java.util.Iterator;

/**
 * An Iterable source of events.
 *
 * @author Hans Gilde
 */
public abstract class EventSource implements Iterable<TxnEventBase> {

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<TxnEventBase> iterator() {
        return new InternalIterator();
    }

    protected abstract boolean hasNext();

    protected abstract TxnEventBase next();

    private class InternalIterator implements Iterator<TxnEventBase> {

        public boolean hasNext() {
            return EventSource.this.hasNext();
        }

        public TxnEventBase next() {
            return EventSource.this.next();
        }

        public void remove() {
            throw new UnsupportedOperationException("This iterator does not suppoer removal.");
        }

    }

}
