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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SupportSubscriberRowByRowFullBase extends SupportSubscriberBase {
    private final ArrayList<UniformPair<Integer>> indicateStart = new ArrayList<UniformPair<Integer>>();
    private final ArrayList<Object> indicateEnd = new ArrayList<Object>();
    private final ArrayList<Object[]> indicateIStream = new ArrayList<Object[]>();
    private final ArrayList<Object[]> indicateRStream = new ArrayList<Object[]>();

    public SupportSubscriberRowByRowFullBase(boolean requiresStatementDelivery) {
        super(requiresStatementDelivery);
    }

    protected void addUpdateStart(int lengthIStream, int lengthRStream) {
        indicateStart.add(new UniformPair<Integer>(lengthIStream, lengthRStream));
    }

    protected void addUpdate(Object[] values) {
        indicateIStream.add(values);
    }

    protected void addUpdateRStream(Object[] values) {
        indicateRStream.add(values);
    }

    protected void addUpdateEnd() {
        indicateEnd.add(this);
    }


    protected void addUpdateStart(EPStatement statement, int lengthIStream, int lengthRStream) {
        indicateStart.add(new UniformPair<Integer>(lengthIStream, lengthRStream));
        addStmtIndication(statement);
    }

    protected void addUpdate(EPStatement statement, Object[] values) {
        indicateIStream.add(values);
        addStmtIndication(statement);
    }

    protected void addUpdateRStream(EPStatement statement, Object[] values) {
        indicateRStream.add(values);
        addStmtIndication(statement);
    }

    protected void addUpdateEnd(EPStatement statement) {
        indicateEnd.add(this);
        addStmtIndication(statement);
    }

    public void reset() {
        indicateStart.clear();
        indicateIStream.clear();
        indicateRStream.clear();
        indicateEnd.clear();
        resetStmts();
    }

    public void assertNoneReceived() {
        assertTrue(indicateStart.isEmpty());
        assertTrue(indicateIStream.isEmpty());
        assertTrue(indicateRStream.isEmpty());
        assertTrue(indicateEnd.isEmpty());
    }

    public void assertOneReceivedAndReset(EPStatement stmt, int expectedLenIStream, int expectedLenRStream, Object[][] expectedIStream, Object[][] expectedRStream) {

        int stmtCount = 2 + expectedLenIStream + expectedLenRStream;
        assertStmtMultipleReceived(stmt, stmtCount);

        assertEquals(1, indicateStart.size());
        UniformPair<Integer> pairLength = indicateStart.get(0);
        assertEquals(expectedLenIStream, (int) pairLength.getFirst());
        assertEquals(expectedLenRStream, (int) pairLength.getSecond());

        EPAssertionUtil.assertEqualsExactOrder(expectedIStream, indicateIStream);
        EPAssertionUtil.assertEqualsExactOrder(expectedRStream, indicateRStream);

        assertEquals(1, indicateEnd.size());

        reset();
    }
}
