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
package com.espertech.esper.regressionlib.support.subscriber;


import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public abstract class SupportSubscriberMultirowUnderlyingBase extends SupportSubscriberBase {
    private final ArrayList<UniformPair<Object[]>> indicate = new ArrayList<UniformPair<Object[]>>();

    public SupportSubscriberMultirowUnderlyingBase(boolean requiresStatementDelivery) {
        super(requiresStatementDelivery);
    }

    public void addIndication(Object[] newEvents, Object[] oldEvents) {
        indicate.add(new UniformPair<Object[]>(newEvents, oldEvents));
    }

    public void addIndication(EPStatement stmt, Object[] newEvents, Object[] oldEvents) {
        indicate.add(new UniformPair<Object[]>(newEvents, oldEvents));
        addStmtIndication(stmt);
    }

    public void assertOneReceivedAndReset(EPStatement stmt, Object[] firstExpected, Object[] secondExpected) {
        assertStmtOneReceived(stmt);

        assertEquals(1, indicate.size());
        UniformPair<Object[]> result = indicate.get(0);
        assertValues(firstExpected, result.getFirst());
        assertValues(secondExpected, result.getSecond());

        reset();
    }

    private void assertValues(Object[] expected, Object[] received) {
        if (expected == null) {
            assertNull(received);
            return;
        }
        EPAssertionUtil.assertEqualsExactOrder(expected, received);
    }

    private void reset() {
        resetStmts();
        indicate.clear();
    }
}
