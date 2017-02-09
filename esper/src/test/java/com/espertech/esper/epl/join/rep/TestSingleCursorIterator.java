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

import com.espertech.esper.client.EventBean;
import junit.framework.TestCase;

import java.util.NoSuchElementException;

public class TestSingleCursorIterator extends TestCase {
    private SingleCursorIterator filledIterator;
    private SingleCursorIterator emptyIterator;
    private Cursor cursor;

    public void setUp() {
        cursor = makeAnonymousCursor();
        filledIterator = new SingleCursorIterator(cursor);
        emptyIterator = new SingleCursorIterator(null);
    }

    public void testNext() {
        assertSame(cursor, filledIterator.next());
        try {
            filledIterator.next();
            TestCase.fail();
        } catch (NoSuchElementException ex) {
            // Expected exception
        }

        try {
            emptyIterator.next();
            TestCase.fail();
        } catch (NoSuchElementException ex) {
            // Expected exception
        }
    }

    public void testHasNext() {
        assertTrue(filledIterator.hasNext());
        filledIterator.next();
        assertFalse(filledIterator.hasNext());

        assertFalse(emptyIterator.hasNext());
    }

    public void testRemove() {
        try {
            filledIterator.remove();
            assertTrue(false);
        } catch (UnsupportedOperationException ex) {
            // Expected exception
        }
    }

    private Cursor makeAnonymousCursor() {
        return new Cursor(null, 0, null) {

            public EventBean getLookupEvent() {
                return null;
            }

            public int getLookupStream() {
                return 0;
            }

            public int getIndexedStream() {
                return 0;
            }
        };
    }
}