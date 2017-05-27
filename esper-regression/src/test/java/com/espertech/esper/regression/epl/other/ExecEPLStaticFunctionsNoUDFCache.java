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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationPlugInSingleRowFunction;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportTemperatureBean;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static junit.framework.TestCase.assertTrue;

public class ExecEPLStaticFunctionsNoUDFCache implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addImport(SupportStaticMethodLib.class.getName());
        configuration.addPlugInSingleRowFunction("sleepme", SupportStaticMethodLib.class.getName(), "sleep", ConfigurationPlugInSingleRowFunction.ValueCache.ENABLED);
        configuration.getEngineDefaults().getExpression().setUdfCache(false);
        configuration.addEventType("Temperature", SupportTemperatureBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String text = "select SupportStaticMethodLib.sleep(100) as val from Temperature as temp";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        long startTime = System.currentTimeMillis();
        epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta > 120);
        stmt.destroy();

        // test plug-in single-row function
        String textSingleRow = "select " +
                "sleepme(100) as val" +
                " from Temperature as temp";
        EPStatement stmtSingleRow = epService.getEPAdministrator().createEPL(textSingleRow);
        SupportUpdateListener listenerSingleRow = new SupportUpdateListener();
        stmtSingleRow.addListener(listenerSingleRow);

        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(new SupportTemperatureBean("a"));
        }
        delta = System.currentTimeMillis() - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1000);
        stmtSingleRow.destroy();
    }
}
