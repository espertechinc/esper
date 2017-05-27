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
package com.espertech.esper.regression.epl.variable;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertTrue;

public class ExecVariablesPerf implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("create const variable String MYCONST = 'E331'");

        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i * -1));
        }

        // test join
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean_S0 s0 unidirectional, MyWindow sb where theString = MYCONST");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "E" + i));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sb.intPrimitive".split(","), new Object[]{"E331", -331});
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 500);
        stmt.destroy();

        // test subquery
        EPStatement stmtSubquery = epService.getEPAdministrator().createEPL("select * from SupportBean_S0 where exists (select * from MyWindow where theString = MYCONST)");
        stmtSubquery.addListener(listener);

        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "E" + i));
            assertTrue(listener.getAndClearIsInvoked());
        }
        delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 500);
    }
}
