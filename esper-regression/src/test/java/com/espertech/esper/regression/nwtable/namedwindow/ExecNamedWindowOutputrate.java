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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecNamedWindowOutputrate implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create schema SupportBean as " + SupportBean.class.getName());

        epService.getEPAdministrator().createEPL("create window MyWindowOne#keepall as (theString string, intv int)");
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select theString, intPrimitive as intv from SupportBean");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = new String[]{"theString", "c"};
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select irstream theString, count(*) as c from MyWindowOne group by theString output snapshot every 1 second");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("B", 4));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A", 2L}, {"B", 1L}});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 5));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A", 2L}, {"B", 2L}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A", 2L}, {"B", 2L}});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("C", 1));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(4000));

        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"A", 3L}, {"B", 2L}, {"C", 1L}});
    }
}
