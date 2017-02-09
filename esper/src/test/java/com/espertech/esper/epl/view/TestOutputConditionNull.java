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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;

public class TestOutputConditionNull extends TestCase {
    private OutputConditionNull condition;
    private SupportUpdateListener listener;
    private OutputCallback callback;

    public void setUp() {
        listener = new SupportUpdateListener();

        callback = new OutputCallback() {
            public void continueOutputProcessing(boolean doOutput, boolean forceUpdate) {
                listener.update(null, null);
            }
        };
        condition = new OutputConditionNull(callback);
    }

    public void testUpdateCondition() {
        // the callback should be made regardles of the update
        condition.updateOutputCondition(1, 1);
        assertTrue(listener.getAndClearIsInvoked());
        condition.updateOutputCondition(1, 0);
        assertTrue(listener.getAndClearIsInvoked());
        condition.updateOutputCondition(0, 1);
        assertTrue(listener.getAndClearIsInvoked());
        condition.updateOutputCondition(0, 0);
        assertTrue(listener.getAndClearIsInvoked());
    }
}
