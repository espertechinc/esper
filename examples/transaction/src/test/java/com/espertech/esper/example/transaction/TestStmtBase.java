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
package com.espertech.esper.example.transaction;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import junit.framework.TestCase;

public abstract class TestStmtBase extends TestCase {
    protected EPServiceProvider epService;

    public void setUp() {
        Configuration configuration = new Configuration();
        configuration.addEventType("TxnEventA", TxnEventA.class.getName());
        configuration.addEventType("TxnEventB", TxnEventB.class.getName());
        configuration.addEventType("TxnEventC", TxnEventC.class.getName());
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);

        epService = EPServiceProviderManager.getProvider("TestStmtBase", configuration);
        epService.initialize();
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    protected void sendEvent(Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

}
