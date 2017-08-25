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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecExprAndOrNot implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        // more extensive testing elsewhere especially as part of filters
        // these tests are for independent select-clause expression tests

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "(intPrimitive=1) or (intPrimitive=2) as c0, " +
                "(intPrimitive>0) and (intPrimitive<3) as c1," +
                "not(intPrimitive=2) as c2" +
                " from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0,c1,c2".split(",");

        makeSendBean(epService, 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, true, true});

        makeSendBean(epService, 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, true, false});

        makeSendBean(epService, 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {false, false, true});

        stmt.destroy();
    }

    private void makeSendBean(EPServiceProvider epService, int intPrimitive) {
        SupportBean bean = new SupportBean("", intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
