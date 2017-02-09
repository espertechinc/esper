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
/*
 *  Credit: Apache Commons Collections
 */
package com.espertech.esper.collection.apachecommons;

import java.util.NoSuchElementException;

/**
 * Provides an implementation of an empty iterator.
 *
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.1
 */
abstract class AbstractEmptyIterator {

    /**
     * Constructor.
     */
    protected AbstractEmptyIterator() {
        super();
    }

    public boolean hasNext() {
        return false;
    }

    public Object next() {
        throw new NoSuchElementException("Iterator contains no elements");
    }

    public boolean hasPrevious() {
        return false;
    }

    public Object previous() {
        throw new NoSuchElementException("Iterator contains no elements");
    }

    public int nextIndex() {
        return 0;
    }

    public int previousIndex() {
        return -1;
    }

    public void add(Object obj) {
        throw new UnsupportedOperationException("add() not supported for empty Iterator");
    }

    public void set(Object obj) {
        throw new IllegalStateException("Iterator contains no elements");
    }

    public void remove() {
        throw new IllegalStateException("Iterator contains no elements");
    }

    public Object getKey() {
        throw new IllegalStateException("Iterator contains no elements");
    }

    public Object getValue() {
        throw new IllegalStateException("Iterator contains no elements");
    }

    public Object setValue(Object value) {
        throw new IllegalStateException("Iterator contains no elements");
    }

    public void reset() {
        // do nothing
    }

}