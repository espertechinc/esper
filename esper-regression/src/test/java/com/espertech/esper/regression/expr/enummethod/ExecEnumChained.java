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
package com.espertech.esper.regression.expr.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.bean.sales.PersonSales;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecEnumChained implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("PersonSales", PersonSales.class);
    }

    public void run(EPServiceProvider epService) throws Exception {

        String eplFragment = "select sales.where(x => x.cost > 1000).min(y => y.buyer.age) as val from PersonSales";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val".split(","), new Class[]{Integer.class});

        PersonSales bean = PersonSales.make();
        epService.getEPRuntime().sendEvent(bean);
        assertEquals(50, listener.assertOneGetNewAndReset().get("val"));
    }
}
