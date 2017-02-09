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
import com.espertech.esper.supportunit.bean.SupportBean_S0;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

public class TestRollingEventBuffer extends TestCase {
    private RollingEventBuffer bufferOne;
    private RollingEventBuffer bufferTwo;
    private RollingEventBuffer bufferFive;
    private static int eventId;

    public void setUp() {
        bufferOne = new RollingEventBuffer(1);
        bufferTwo = new RollingEventBuffer(2);
        bufferFive = new RollingEventBuffer(5);
    }

    public void testFlowSizeOne() {
        bufferOne.add((EventBean[]) null);
        assertNull(bufferOne.get(0));

        EventBean[] set1 = make(2);
        bufferOne.add(set1);
        assertSame(set1[1], bufferOne.get(0));
        tryInvalid(bufferOne, 1);

        EventBean[] set2 = make(1);
        bufferOne.add(set2);
        assertSame(set2[0], bufferOne.get(0));
        tryInvalid(bufferOne, 1);
    }

    public void testFlowSizeTwo() {
        EventBean[] set1 = make(2);
        bufferTwo.add(set1);
        assertEvents(new EventBean[]{set1[1], set1[0]}, bufferTwo);

        EventBean[] set2 = make(1);
        bufferTwo.add(set2);
        assertEvents(new EventBean[]{set2[0], set1[1]}, bufferTwo);

        EventBean[] set3 = make(1);
        bufferTwo.add(set3);
        assertEvents(new EventBean[]{set3[0], set2[0]}, bufferTwo);

        EventBean[] set4 = make(3);
        bufferTwo.add(set4);
        assertEvents(new EventBean[]{set4[2], set4[1]}, bufferTwo);

        EventBean[] set5 = make(5);
        bufferTwo.add(set5);
        assertEvents(new EventBean[]{set5[4], set5[3]}, bufferTwo);

        EventBean[] set6 = make(1);
        bufferTwo.add(set6);
        assertEvents(new EventBean[]{set6[0], set5[4]}, bufferTwo);
        bufferTwo.add(make(0));
        assertEvents(new EventBean[]{set6[0], set5[4]}, bufferTwo);

        EventBean[] set7 = make(2);
        bufferTwo.add(set7);
        assertEvents(new EventBean[]{set7[1], set7[0]}, bufferTwo);
    }

    public void testFlowSizeTen() {
        EventBean[] set1 = make(3);
        bufferFive.add(set1);
        assertEvents(new EventBean[]{set1[2], set1[1], set1[0], null, null}, bufferFive);

        EventBean[] set2 = make(1);
        bufferFive.add(set2);
        assertEvents(new EventBean[]{set2[0], set1[2], set1[1], set1[0], null}, bufferFive);

        EventBean[] set3 = make(3);
        bufferFive.add(set3);
        assertEvents(new EventBean[]{set3[2], set3[1], set3[0], set2[0], set1[2]}, bufferFive);

        EventBean[] set4 = make(5);
        bufferFive.add(set4);
        assertEvents(new EventBean[]{set4[4], set4[3], set4[2], set4[1], set4[0]}, bufferFive);

        EventBean[] set5 = make(8);
        bufferFive.add(set5);
        assertEvents(new EventBean[]{set5[7], set5[6], set5[5], set5[4], set5[3]}, bufferFive);

        EventBean[] set6 = make(2);
        bufferFive.add(set6);
        assertEvents(new EventBean[]{set6[1], set6[0], set5[7], set5[6], set5[5]}, bufferFive);
    }

    private void assertEvents(EventBean[] expected, RollingEventBuffer buffer) {
        for (int i = 0; i < expected.length; i++) {
            assertSame(expected[i], buffer.get(i));
        }
        tryInvalid(buffer, expected.length);
    }

    private void tryInvalid(RollingEventBuffer buffer, int index) {
        try {
            buffer.get(index);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    private EventBean[] make(int size) {
        EventBean[] events = new EventBean[size];
        for (int i = 0; i < events.length; i++) {
            events[i] = SupportEventBeanFactory.createObject(new SupportBean_S0(eventId++));
        }
        return events;
    }
}
