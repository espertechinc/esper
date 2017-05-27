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
package com.espertech.esper.supportregression.subscriber;

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.collection.UniformPair;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;

public abstract class SupportSubscriberMultirowMapBase extends SupportSubscriberBase {
    private final ArrayList<UniformPair<Map[]>> indicate = new ArrayList<UniformPair<Map[]>>();

    protected SupportSubscriberMultirowMapBase(boolean requiresStatementDelivery) {
        super(requiresStatementDelivery);
    }

    protected void addIndication(Map[] newEvents, Map[] oldEvents) {
        indicate.add(new UniformPair<Map[]>(newEvents, oldEvents));
    }

    protected void addIndication(EPStatement statement, Map[] newEvents, Map[] oldEvents) {
        indicate.add(new UniformPair<Map[]>(newEvents, oldEvents));
        addStmtIndication(statement);
    }

    public void assertNoneReceived() {
        assertTrue(indicate.isEmpty());
        assertStmtNoneReceived();
    }

    public void assertOneReceivedAndReset(EPStatement stmt, String[] fields, Object[][] firstExpected, Object[][] secondExpected) {
        assertStmtOneReceived(stmt);

        assertEquals(1, indicate.size());
        UniformPair<Map[]> result = indicate.get(0);
        assertValues(fields, firstExpected, result.getFirst());
        assertValues(fields, secondExpected, result.getSecond());

        reset();
    }

    private void assertValues(String[] fields, Object[][] expected, Map[] received) {
        if (expected == null) {
            assertNull(received);
            return;
        }
        EPAssertionUtil.assertPropsPerRow(received, fields, expected);
    }

    private void reset() {
        resetStmts();
        indicate.clear();
    }
}
