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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertTrue;

public class ExecSubselectOrderOfEvalNoPreeval implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExpression().setSelfSubselectPreeval(false);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        String epl = "select * from SupportBean(intPrimitive<10) where intPrimitive not in (select intPrimitive from SupportBean#unique(intPrimitive))";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(epl);
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        assertTrue(listener.getAndClearIsInvoked());

        stmtOne.destroy();

        String eplTwo = "select * from SupportBean where intPrimitive not in (select intPrimitive from SupportBean(intPrimitive<10)#unique(intPrimitive))";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(eplTwo);
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        assertTrue(listener.getAndClearIsInvoked());
    }
}