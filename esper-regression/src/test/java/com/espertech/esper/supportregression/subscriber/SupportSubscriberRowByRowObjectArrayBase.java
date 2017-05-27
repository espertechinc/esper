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

import static org.junit.Assert.assertEquals;

public abstract class SupportSubscriberRowByRowObjectArrayBase extends SupportSubscriberBase {
    private final ArrayList<Object[]> indicate = new ArrayList<Object[]>();

    protected SupportSubscriberRowByRowObjectArrayBase(boolean requiresStatementDelivery) {
        super(requiresStatementDelivery);
    }

    protected void addIndication(Object[] row) {
        indicate.add(row);
    }

    protected void addIndication(EPStatement stmt, Object[] row) {
        indicate.add(row);
        addStmtIndication(stmt);
    }

    public void assertOneAndReset(EPStatement stmt, Object[] expected) {
        assertStmtOneReceived(stmt);

        assertEquals(1, indicate.size());
        EPAssertionUtil.assertEqualsAnyOrder(expected, indicate.get(0));

        indicate.clear();
        resetStmts();
    }
}
