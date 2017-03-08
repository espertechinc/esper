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
package com.espertech.esper.epl.db;

import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTableImpl;
import junit.framework.TestCase;

public class TestDataCacheLRUImpl extends TestCase {
    private DataCacheLRUImpl cache;
    private EventTable[] lists = new EventTable[10];

    public void setUp() {
        cache = new DataCacheLRUImpl(3);
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new UnindexedEventTableImpl(0);
        }
    }

    public void testGet() {
        assertNull(cache.getCached(make("a"), 1));
        assertTrue(cache.isActive());

        cache.put(make("a"), 1, new EventTable[]{lists[0]});     // a
        assertSame(lists[0], cache.getCached(make("a"), 1)[0]);

        cache.put(make("b"), 1, new EventTable[]{lists[1]});     // b, a
        assertSame(lists[1], cache.getCached(make("b"), 1)[0]); // b, a

        assertSame(lists[0], cache.getCached(make("a"), 1)[0]); // a, b

        cache.put(make("c"), 1, new EventTable[]{lists[2]});     // c, a, b
        cache.put(make("d"), 1, new EventTable[]{lists[3]});     // d, c, a  (b gone)

        assertNull(cache.getCached(make("b"), 1));

        assertEquals(lists[2], cache.getCached(make("c"), 1)[0]); // c, d, a
        assertEquals(lists[0], cache.getCached(make("a"), 1)[0]); // a, c, d

        cache.put(make("e"), 1, new EventTable[]{lists[4]}); // e, a, c (d and b gone)

        assertNull(cache.getCached(make("d"), 1));
        assertNull(cache.getCached(make("b"), 1));
    }

    private Object[] make(String key) {
        return new Object[]{key};
    }
}
