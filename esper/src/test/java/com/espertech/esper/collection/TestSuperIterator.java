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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TestSuperIterator extends TestCase {

    public void testEmpty() {
        SuperIterator<String> it = new SuperIterator<String>(make(null), make(null));
        assertFalse(it.hasNext());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException ex) {
        }
    }

    public void testFlow() {
        SuperIterator<String> it = new SuperIterator<String>(make("a"), make(null));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a"}, it);

        it = new SuperIterator<String>(make("a,b"), make(null));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", "b"}, it);

        it = new SuperIterator<String>(make("a"), make("b"));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", "b"}, it);

        it = new SuperIterator<String>(make(null), make("a,b"));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", "b"}, it);
    }

    private Iterator<String> make(String csv) {
        if ((csv == null) || (csv.length() == 0)) {
            return new NullIterator<String>();
        }
        String[] fields = csv.split(",");
        return Arrays.asList(fields).iterator();
    }

}
