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
package com.espertech.esper.epl.join.rep;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A utility class for an iterator that has one element.
 */
public class SingleCursorIterator implements Iterator<Cursor> {
    private Cursor cursor;

    /**
     * Ctor.
     *
     * @param cursor is the single element.
     */
    public SingleCursorIterator(Cursor cursor) {
        this.cursor = cursor;
    }

    public boolean hasNext() {
        return cursor != null;
    }

    public Cursor next() {
        if (cursor == null) {
            throw new NoSuchElementException();
        }
        Cursor c = cursor;
        this.cursor = null;
        return c;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

