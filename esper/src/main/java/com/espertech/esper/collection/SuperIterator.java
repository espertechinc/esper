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

import java.util.Iterator;

public class SuperIterator<T> implements Iterator<T> {

    private final Iterator<T> first;
    private final Iterator<T> second;

    public SuperIterator(Iterator<T> first, Iterator<T> second) {
        this.first = first;
        this.second = second;
    }

    public boolean hasNext() {
        return first.hasNext() || second.hasNext();
    }

    public T next() {
        if (first.hasNext()) {
            return first.next();
        }
        return second.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
