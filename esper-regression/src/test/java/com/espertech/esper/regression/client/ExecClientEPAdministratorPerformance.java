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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientEPAdministratorPerformance implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getByteCodeGeneration().disableAll();
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertion1kValidStmtsPerformance(epService);
        runAssertion1kInvalidStmts(epService);
    }

    private void runAssertion1kValidStmtsPerformance(EPServiceProvider epService) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            String text = "select * from " + SupportBean.class.getName();
            EPStatement stmt = epService.getEPAdministrator().createEPL(text, "s1");
            assertEquals("s1", stmt.getName());
            stmt.stop();
            stmt.start();
            stmt.stop();
            stmt.destroy();
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue(".test10kValid delta=" + delta, delta < 10000);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion1kInvalidStmts(EPServiceProvider epService) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            try {
                String text = "select xxx from " + SupportBean.class.getName();
                epService.getEPAdministrator().createEPL(text, "s1");
            } catch (Exception ex) {
                // expected
            }
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue(".test1kInvalid delta=" + delta, delta < 2500);
        epService.getEPAdministrator().destroyAllStatements();
    }
}
