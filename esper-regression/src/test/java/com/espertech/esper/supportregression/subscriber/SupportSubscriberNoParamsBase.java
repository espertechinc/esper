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

import static org.junit.Assert.assertTrue;

public abstract class SupportSubscriberNoParamsBase extends SupportSubscriberBase {
    private boolean called;

    protected SupportSubscriberNoParamsBase(boolean requiresStatementDelivery) {
        super(requiresStatementDelivery);
    }

    public void addCalled() {
        called = true;
    }

    public void addCalled(EPStatement stmt) {
        called = true;
        addStmtIndication(stmt);
    }

    public void assertCalledAndReset(EPStatement stmt) {
        assertStmtOneReceived(stmt);
        assertTrue(called);
        called = false;
        resetStmts();
    }
}
