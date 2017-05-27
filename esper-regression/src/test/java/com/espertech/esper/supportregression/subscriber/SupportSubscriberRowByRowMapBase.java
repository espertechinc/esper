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

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class SupportSubscriberRowByRowMapBase extends SupportSubscriberBase {
    private final ArrayList<Map> indicateIStream = new ArrayList<Map>();
    private final ArrayList<Map> indicateRStream = new ArrayList<Map>();

    public SupportSubscriberRowByRowMapBase(boolean requiresStatementDelivery) {
        super(requiresStatementDelivery);
    }

    protected void addIndicationIStream(Map row) {
        indicateIStream.add(row);
    }

    protected void addIndicationRStream(Map row) {
        indicateRStream.add(row);
    }

    protected void addIndicationIStream(EPStatement stmt, Map row) {
        indicateIStream.add(row);
        addStmtIndication(stmt);
    }

    protected void addIndicationRStream(EPStatement stmt, Map row) {
        indicateRStream.add(row);
        addStmtIndication(stmt);
    }

    public void assertIRStreamAndReset(EPStatement stmt, String[] fields, Object[] expectedIStream, Object[] expectedRStream) {
        assertStmtMultipleReceived(stmt, 1 + (expectedRStream == null ? 0 : 1));

        assertEquals(1, indicateIStream.size());
        EPAssertionUtil.assertPropsMap(indicateIStream.get(0), fields, expectedIStream);

        if (expectedRStream == null) {
            assertTrue(indicateRStream.isEmpty());
        } else {
            assertEquals(1, indicateRStream.size());
            EPAssertionUtil.assertPropsMap(indicateRStream.get(0), fields, expectedRStream);
        }

        indicateIStream.clear();
        indicateRStream.clear();
        resetStmts();
    }
}
