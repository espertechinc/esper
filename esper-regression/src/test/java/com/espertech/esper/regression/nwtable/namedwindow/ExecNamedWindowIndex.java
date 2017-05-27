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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecNamedWindowIndex implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MyWindowOne#unique(theString) as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select * from SupportBean");
        epService.getEPAdministrator().createEPL("create unique index I1 on MyWindowOne(theString)");

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E0", 5));

        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtWindow.iterator(), "theString,intPrimitive".split(","), new Object[][]{{"E0", 5}, {"E1", 4}, {"E2", 3}});
    }
}
