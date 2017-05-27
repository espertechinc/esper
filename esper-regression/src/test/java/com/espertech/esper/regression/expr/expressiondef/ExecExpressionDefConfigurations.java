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
package com.espertech.esper.regression.expr.expressiondef;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST1;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecExpressionDefConfigurations implements RegressionExecution {

    private final Integer configuredCacheSize;
    private final int expectedInvocationCount;

    public ExecExpressionDefConfigurations(Integer configuredCacheSize, int expectedInvocationCount) {
        this.configuredCacheSize = configuredCacheSize;
        this.expectedInvocationCount = expectedInvocationCount;
    }

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean_ST0", SupportBean_ST0.class);
        configuration.addEventType("SupportBean_ST1", SupportBean_ST1.class);

        // set cache size
        if (configuredCacheSize != null) {
            configuration.getEngineDefaults().getExecution().setDeclaredExprValueCacheSize(configuredCacheSize);
        }
    }

    public void run(EPServiceProvider epService) throws Exception {

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
