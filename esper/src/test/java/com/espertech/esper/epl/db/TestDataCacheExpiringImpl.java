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

import com.espertech.esper.client.ConfigurationCacheReferenceType;
import com.espertech.esper.core.support.SupportSchedulingServiceImpl;
import com.espertech.esper.epl.expression.time.TimeAbacusMilliseconds;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTableImpl;
import com.espertech.esper.schedule.SchedulingServiceImpl;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import junit.framework.TestCase;

public class TestDataCacheExpiringImpl extends TestCase {
    private SupportSchedulingServiceImpl scheduler;
    private DataCacheExpiringImpl cache;
    private EventTable[] lists = new EventTable[10];

    public void setUp() {
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new UnindexedEventTableImpl(0);
        }
    }

    public void testPurgeInterval() {
        SchedulingServiceImpl scheduler = new SchedulingServiceImpl(new TimeSourceServiceImpl());
        cache = new DataCacheExpiringImpl(10, 20, ConfigurationCacheReferenceType.HARD, scheduler, 1, null, TimeAbacusMilliseconds.INSTANCE);   // age 10 sec, purge 1000 seconds

        // test single entry in cache
        scheduler.setTime(5000);
        cache.put(make("a"), 1, new EventTable[]{lists[0]}); // a at 5 sec
        assertSame(lists[0], cache.getCached(make("a"), 1)[0]);

        scheduler.setTime(26000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduler);
        assertEquals(0, cache.getSize());

        // test 4 entries in cache
        scheduler.setTime(30000);
        cache.put(make("b"), 1, new EventTable[]{lists[1]});  // b at 30 sec

        scheduler.setTime(35000);
        cache.put(make("c"), 1, new EventTable[]{lists[2]});  // c at 35 sec

        scheduler.setTime(40000);
        cache.put(make("d"), 1, new EventTable[]{lists[3]});  // d at 40 sec

        scheduler.setTime(45000);
        cache.put(make("e"), 1, new EventTable[]{lists[4]});  // d at 40 sec

        scheduler.setTime(50000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduler);
        assertEquals(2, cache.getSize());   // only d and e

        assertSame(lists[3], cache.getCached(make("d"), 1)[0]);
        assertSame(lists[4], cache.getCached(make("e"), 1)[0]);
    }

    public void testGet() {
        scheduler = new SupportSchedulingServiceImpl();
        cache = new DataCacheExpiringImpl(10, 1000, ConfigurationCacheReferenceType.HARD, scheduler, 1, null, TimeAbacusMilliseconds.INSTANCE);   // age 10 sec, purge 1000 seconds

        assertNull(cache.getCached(make("a"), 1));

        scheduler.setTime(5000);
        cache.put(make("a"), 1, new EventTable[]{lists[0]}); // a at 5 sec
        assertSame(lists[0], cache.getCached(make("a"), 1)[0]);

        scheduler.setTime(10000);
        cache.put(make("b"), 1, new EventTable[]{lists[1]}); // b at 10 sec
        assertSame(lists[0], cache.getCached(make("a"), 1)[0]);
        assertSame(lists[1], cache.getCached(make("b"), 1)[0]);

        scheduler.setTime(11000);
        cache.put(make("c"), 1, new EventTable[]{lists[2]}); // c at 11 sec
        cache.put(make("d"), 1, new EventTable[]{lists[3]}); // d at 11 sec

        scheduler.setTime(14999);
        assertSame(lists[0], cache.getCached(make("a"), 1)[0]);

        scheduler.setTime(15000);
        assertSame(lists[0], cache.getCached(make("a"), 1)[0]);

        scheduler.setTime(15001);
        assertNull(cache.getCached(make("a"), 1));

        scheduler.setTime(15001);
        assertNull(cache.getCached(make("a"), 1));

        scheduler.setTime(15001);
        assertNull(cache.getCached(make("a"), 1));
        assertSame(lists[1], cache.getCached(make("b"), 1)[0]);
        assertSame(lists[2], cache.getCached(make("c"), 1)[0]);
        assertSame(lists[3], cache.getCached(make("d"), 1)[0]);

        scheduler.setTime(20000);
        assertSame(lists[1], cache.getCached(make("b"), 1)[0]);

        scheduler.setTime(20001);
        assertNull(cache.getCached(make("b"), 1));

        scheduler.setTime(21001);
        assertNull(cache.getCached(make("a"), 1));
        assertNull(cache.getCached(make("b"), 1));
        assertNull(cache.getCached(make("c"), 1));
        assertNull(cache.getCached(make("d"), 1));

        scheduler.setTime(22000);
        cache.put(make("b"), 1, new EventTable[]{lists[1]}); // b at 22 sec
        cache.put(make("d"), 1, new EventTable[]{lists[3]}); // d at 22 sec

        scheduler.setTime(32000);
        assertSame(lists[1], cache.getCached(make("b"), 1)[0]);
        assertSame(lists[3], cache.getCached(make("d"), 1)[0]);
    }

    private Object[] make(String key) {
        return new Object[]{key};
    }
}
