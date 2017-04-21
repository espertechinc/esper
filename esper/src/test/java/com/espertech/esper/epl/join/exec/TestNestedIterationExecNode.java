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
package com.espertech.esper.epl.join.exec;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.epl.join.exec.base.FullTableScanLookupStrategy;
import com.espertech.esper.epl.join.exec.base.NestedIterationExecNode;
import com.espertech.esper.epl.join.exec.base.TableLookupExecNode;
import com.espertech.esper.epl.join.table.UnindexedEventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTableImpl;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestNestedIterationExecNode extends TestCase {
    private NestedIterationExecNode exec;
    private EventBean[][] streamEvents;

    public void setUp() {
        UnindexedEventTable[] indexes = new UnindexedEventTable[4];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = new UnindexedEventTableImpl(0);
        }

        exec = new NestedIterationExecNode(new int[]{3, 0, 1});
        exec.addChildNode(new TableLookupExecNode(3, new FullTableScanLookupStrategy(indexes[3])));
        exec.addChildNode(new TableLookupExecNode(0, new FullTableScanLookupStrategy(indexes[0])));
        exec.addChildNode(new TableLookupExecNode(1, new FullTableScanLookupStrategy(indexes[1])));

        streamEvents = new EventBean[4][2];
        streamEvents[0] = SupportEventBeanFactory.makeEvents_A(new String[]{"a1", "a2"});
        streamEvents[1] = SupportEventBeanFactory.makeEvents_B(new String[]{"b1", "b2"});
        streamEvents[2] = SupportEventBeanFactory.makeEvents_C(new String[]{"c1", "c2"});
        streamEvents[3] = SupportEventBeanFactory.makeEvents_D(new String[]{"d1", "d2"});

        // Fill with data
        indexes[0].add(streamEvents[0], null);
        indexes[1].add(streamEvents[1], null);
        indexes[2].add(streamEvents[2], null);
        indexes[3].add(streamEvents[3], null);
    }

    public void testLookup() {
        List<EventBean[]> result = new LinkedList<EventBean[]>();
        EventBean[] prefill = new EventBean[4];
        prefill[2] = streamEvents[2][0];

        exec.process(streamEvents[2][0], prefill, result, null);

        assertEquals(8, result.size());

        EventBean[][] received = makeArray(result);
        EventBean[][] expected = makeExpected();
        EPAssertionUtil.assertEqualsAnyOrder(expected, received);
    }

    private EventBean[][] makeExpected() {
        EventBean[][] expected = new EventBean[8][4];
        int count = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    expected[count][0] = streamEvents[0][i];
                    expected[count][1] = streamEvents[1][j];
                    expected[count][2] = streamEvents[2][0];
                    expected[count][3] = streamEvents[3][k];
                    count++;
                }
            }
        }
        return expected;
    }

    private EventBean[][] makeArray(List<EventBean[]> eventArrList) {
        EventBean[][] result = new EventBean[eventArrList.size()][4];
        for (int i = 0; i < eventArrList.size(); i++) {
            result[i] = eventArrList.get(i);
        }
        return result;
    }
}

// Result
    /* 8 combinations
    d1
        a1
            b1
            b2
        a2
            b1
            b2
    d2
        a1
            b1
            b2
        a2
            b1
            b2
    */
