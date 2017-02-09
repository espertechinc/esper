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
import java.util.NoSuchElementException;

/**
 * A utility class for an iterator that has zero or one element and can be reset with a new value.
 */
public class SingleObjectIterator<T> implements Iterator<T> {
    private T object;

    /**
     * Constructor, takes the single object to iterate over as a parameter.
     * The single object can be null indicating that there are no more elements.
     *
     * @param object single object that the iterator returns, or null for an empty iterator
     */
    public SingleObjectIterator(T object) {
        this.object = object;
    }

    /**
     * Ctor for an iterator starting out empty.
     */
    public SingleObjectIterator() {
    }

    public boolean hasNext() {
        if (object == null) {
            return false;
        }
        return true;
    }

    public T next() {
        if (object == null) {
            throw new NoSuchElementException();
        }
        T result = object;
        object = null;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
