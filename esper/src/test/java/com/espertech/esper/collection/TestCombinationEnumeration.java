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

import com.espertech.esper.util.CollectionUtil;
import junit.framework.TestCase;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class TestCombinationEnumeration extends TestCase {

    public void testEnumerate() {
        tryEnumerate("1A, 1B, 2A, 2B", new Object[][]{{1, 2}, {"A", "B"}});
        tryEnumerate("1AX, 1AY, 1BX, 1BY", new Object[][]{{1}, {"A", "B"}, {"X", "Y"}});
        tryEnumerate("1A, 1B", new Object[][]{{1}, {"A", "B"}});
        tryEnumerate("1", new Object[][]{{1}});
        tryEnumerate("", new Object[0][]);
        tryEnumerate("1A, 2A, 3A", new Object[][]{{1, 2, 3}, {"A"}});
        tryEnumerate("1AX, 1AY, 2AX, 2AY, 3AX, 3AY", new Object[][]{{1, 2, 3}, {"A"}, {"X", "Y"}});

        try {
            new CombinationEnumeration(new Object[][]{{1}, {}});
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Expecting non-null element of minimum length 1", ex.getMessage());
        }
    }

    private void tryEnumerate(String expected, Object[][] objects) {
        CombinationEnumeration e = new CombinationEnumeration(objects);

        List<Object[]> results = new ArrayList<Object[]>();
        for (; e.hasMoreElements(); ) {
            Object[] copy = new Object[objects.length];
            Object[] result = e.nextElement();
            System.arraycopy(result, 0, copy, 0, result.length);
            results.add(copy);
        }

        try {
            e.nextElement();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }

        List<String> items = new ArrayList<String>();
        for (Object[] result : results) {
            StringWriter writer = new StringWriter();
            for (Object item : result) {
                writer.append(item.toString());
            }
            items.add(writer.toString());
        }

        String resultStr = CollectionUtil.toString(items);
        assertEquals(expected, resultStr);
    }
}
