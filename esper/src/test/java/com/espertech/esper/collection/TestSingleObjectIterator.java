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

import junit.framework.TestCase;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class TestSingleObjectIterator extends TestCase {
    public void testNext() {
        Iterator<String> it = new SingleObjectIterator<String>("a");
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }
    }
}
