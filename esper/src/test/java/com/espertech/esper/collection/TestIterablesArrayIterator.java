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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportunit.event.EventFactoryHelper;
import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

public class TestIterablesArrayIterator extends TestCase {
    private Map<String, EventBean> events;

    public void setUp() {
        events = EventFactoryHelper.makeEventMap(new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "z"});
    }

    public void testIterator() {
        Iterable<EventBean>[][] iterables = new Iterable[1][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{"a", "b", "c"}));
        checkResults(iterables, EventFactoryHelper.makeArray(events, new String[]{"a", "b", "c"}));

        iterables = new Iterable[3][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{"a"}));
        iterables[1] = makeArray(EventFactoryHelper.makeList(events, new String[]{"b"}));
        iterables[2] = makeArray(EventFactoryHelper.makeList(events, new String[]{"c"}));
        checkResults(iterables, EventFactoryHelper.makeArray(events, new String[]{"a", "b", "c"}));

        iterables = new Iterable[2][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{"a", "b"}));
        iterables[1] = makeArray(EventFactoryHelper.makeList(events, new String[]{"c"}));
        checkResults(iterables, EventFactoryHelper.makeArray(events, new String[]{"a", "b", "c"}));

        iterables = new Iterable[5][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[1] = makeArray(EventFactoryHelper.makeList(events, new String[]{"a", "b"}));
        iterables[2] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[3] = makeArray(EventFactoryHelper.makeList(events, new String[]{"c"}));
        iterables[4] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        checkResults(iterables, EventFactoryHelper.makeArray(events, new String[]{"a", "b", "c"}));

        iterables = new Iterable[1][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        checkResults(iterables, null);

        iterables = new Iterable[3][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[1] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[2] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        checkResults(iterables, null);

        iterables = new Iterable[4][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[1] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[2] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[3] = makeArray(EventFactoryHelper.makeList(events, new String[]{"d"}));
        checkResults(iterables, EventFactoryHelper.makeArray(events, new String[]{"d"}));

        iterables = new Iterable[4][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{"d"}));
        iterables[1] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[2] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[3] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        checkResults(iterables, EventFactoryHelper.makeArray(events, new String[]{"d"}));

        iterables = new Iterable[8][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{"a", "b", "c"}));
        iterables[1] = makeArray(EventFactoryHelper.makeList(events, new String[]{"d"}));
        iterables[2] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[3] = makeArray(EventFactoryHelper.makeList(events, new String[]{"e", "f"}));
        iterables[4] = makeArray(EventFactoryHelper.makeList(events, new String[]{"g"}));
        iterables[5] = makeArray(EventFactoryHelper.makeList(events, new String[]{}));
        iterables[6] = makeArray(EventFactoryHelper.makeList(events, new String[]{"h", "i"}));
        iterables[7] = makeArray(EventFactoryHelper.makeList(events, new String[]{"z"}));
        checkResults(iterables, EventFactoryHelper.makeArray(events, new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "z"}));

        iterables = new Iterable[0][];
        checkResults(iterables, null);
    }

    public void testRemove() {
        Iterable<EventBean>[][] iterables = new Iterable[1][];
        iterables[0] = makeArray(EventFactoryHelper.makeList(events, new String[]{"a", "b", "c"}));
        IterablesArrayIterator iterator = new IterablesArrayIterator(iterables);

        try {
            iterator.remove();
            assertTrue(false);
        } catch (UnsupportedOperationException ex) {
            // Expected
        }
    }

    private void checkResults(Iterable<EventBean>[][] iterables, EventBean[] expectedValues) {
        IterablesArrayIterator iterator = new IterablesArrayIterator(iterables);
        EPAssertionUtil.assertEqualsExactOrder(expectedValues, iterator);
    }

    private Iterable<EventBean>[] makeArray(List<EventBean> eventBeans) {
        return (Iterable<EventBean>[]) new Iterable[]{eventBeans};
    }
}
