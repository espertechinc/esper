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

package com.espertech.esper.regression.enummethod;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportStaticMethodLib;
import junit.framework.TestCase;

public class TestExpressionDefConfigurations extends TestCase {

    public void testExpressionCacheSize() {
        runAssertionExpressionCacheSize(null, 4);
        runAssertionExpressionCacheSize(0, 4);
        runAssertionExpressionCacheSize(1, 4);
        runAssertionExpressionCacheSize(2, 2);
    }

    private void runAssertionExpressionCacheSize(Integer configuredCacheSize, int expectedInvocationCount) {
        // get config
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean_ST0", SupportBean_ST0.class);
        config.addEventType("SupportBean_ST1", SupportBean_ST1.class);

        // set cache size
        if (configuredCacheSize != null) {
            config.getEngineDefaults().getExecution().setDeclaredExprValueCacheSize(configuredCacheSize);
        }

        // allocate
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("alwaysTrue", SupportStaticMethodLib.class.getName(), "alwaysTrue");

        // set up
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "expression myExpr {v => alwaysTrue(null) } select myExpr(st0) as c0, myExpr(st1) as c1, myExpr(st0) as c2, myExpr(st1) as c3 from SupportBean_ST0#lastevent as st0, SupportBean_ST1#lastevent as st1");
        stmt.addListener(new SupportUpdateListener());

        // send event and assert
        SupportStaticMethodLib.getInvocations().clear();
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("a", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("a", 0));
        assertEquals(expectedInvocationCount, SupportStaticMethodLib.getInvocations().size());
    }
}
