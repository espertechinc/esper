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

import static org.junit.Assert.assertTrue;

public abstract class SupportSubscriberRowByRowSpecificBase extends SupportSubscriberBase {
    private final ArrayList<Object[]> indicate = new ArrayList<Object[]>();

    public SupportSubscriberRowByRowSpecificBase(boolean requiresStatementDelivery) {
        super(requiresStatementDelivery);
    }

    protected void addIndication(EPStatement statement, Object[] values) {
        indicate.add(values);
        addStmtIndication(statement);
    }

    protected void addIndication(Object[] values) {
        indicate.add(values);
    }

    public void reset() {
        indicate.clear();
        resetStmts();
    }

    public void assertOneReceivedAndReset(EPStatement stmt, Object[] objects) {
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{objects}, indicate);
        assertStmtOneReceived(stmt);
        reset();
    }

    public void assertMultipleReceivedAndReset(EPStatement stmt, Object[][] objects) {
        EPAssertionUtil.assertEqualsExactOrder(objects, indicate);
        assertStmtMultipleReceived(stmt, objects.length);
        reset();
    }

    public void assertNoneReceived() {
        assertTrue(indicate.isEmpty());
        assertStmtNoneReceived();
    }
}
