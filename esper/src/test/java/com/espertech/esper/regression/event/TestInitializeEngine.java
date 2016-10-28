/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.event;

import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.support.bean.SupportBean;
import junit.framework.TestCase;

public class TestInitializeEngine extends TestCase
{
    public void testInitialize()
    {
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        String eplOne = "insert into A(a) select 1 from " + SupportBean.class.getName() + "#length(100)";
        String eplTwo = "insert into A(a, b) select 1,2 from " + SupportBean.class.getName() + "#length(100)";

        // Asserting that the engine allows to use the new event stream A with more properties then the old A
        epService.getEPAdministrator().createEPL(eplOne);
        epService.initialize();
        epService.getEPAdministrator().createEPL(eplTwo);
    }
}
