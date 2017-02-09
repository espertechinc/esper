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
import com.espertech.esper.supportunit.epl.join.SupportJoinResultNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.*;

public class TestRepositoryImpl extends TestCase {
    private EventBean s0Event;
    private RepositoryImpl repository;

    public void setUp() {
        s0Event = SupportEventBeanFactory.createObject(new Object());
        repository = new RepositoryImpl(0, s0Event, 6);
    }

    public void testGetCursors() {
        // get cursor for root stream lookup
        Iterator<Cursor> it = repository.getCursors(0);
        assertTrue(it.hasNext());
        Cursor cursor = it.next();
        assertSame(s0Event, cursor.getTheEvent());
        assertSame(0, cursor.getStream());

        assertFalse(it.hasNext());
        tryIteratorEmpty(it);

        // try invalid get cursor for no results
        try {
            repository.getCursors(2);
            fail();
        } catch (NullPointerException ex) {
            // expected
        }
    }

    public void testAddResult() {
        Set<EventBean> results = SupportJoinResultNodeFactory.makeEventSet(2);
        repository.addResult(repository.getCursors(0).next(), results, 1);
        assertEquals(1, repository.getNodesPerStream()[1].size());

        try {
            repository.addResult(repository.getCursors(0).next(), new HashSet<EventBean>(), 1);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            repository.addResult(repository.getCursors(0).next(), null, 1);
            fail();
        } catch (NullPointerException ex) {
            // expected
        }
    }

    public void testFlow() {
        // Lookup from s0
        Cursor cursors[] = read(repository.getCursors(0));
        assertEquals(1, cursors.length);

        Set<EventBean> resultsS1 = SupportJoinResultNodeFactory.makeEventSet(2);
        repository.addResult(cursors[0], resultsS1, 1);

        // Lookup from s1
        cursors = read(repository.getCursors(1));
        assertEquals(2, cursors.length);

        Set<EventBean> resultsS2[] = SupportJoinResultNodeFactory.makeEventSets(new int[]{2, 3});
        repository.addResult(cursors[0], resultsS2[0], 2);
        repository.addResult(cursors[1], resultsS2[1], 2);

        // Lookup from s2
        cursors = read(repository.getCursors(2));
        assertEquals(5, cursors.length);        // 2 + 3 for s2

        Set<EventBean> resultsS3[] = SupportJoinResultNodeFactory.makeEventSets(new int[]{2, 1, 3, 5, 1});
        repository.addResult(cursors[0], resultsS3[0], 3);
        repository.addResult(cursors[1], resultsS3[1], 3);
        repository.addResult(cursors[2], resultsS3[2], 3);
        repository.addResult(cursors[3], resultsS3[3], 3);
        repository.addResult(cursors[4], resultsS3[4], 3);

        // Lookup from s3
        cursors = read(repository.getCursors(3));
        assertEquals(12, cursors.length);
    }

    private void tryIteratorEmpty(Iterator it) {
        try {
            it.next();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }
    }

    private Cursor[] read(Iterator<Cursor> iterator) {
        List<Cursor> cursors = new ArrayList<Cursor>();
        while (iterator.hasNext()) {
            Cursor cursor = iterator.next();
            cursors.add(cursor);
        }
        return cursors.toArray(new Cursor[0]);
    }
}
