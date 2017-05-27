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
package com.espertech.esper.regression.expr.filter;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportMarketDataIDBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertTrue;

public class ExecFilterWhereClauseNoDataWindowPerformance implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MD", SupportMarketDataIDBean.class);
    }

    // Compares the performance of
    //     select * from MD(symbol = 'xyz')
    //  against
    //     select * from MD where symbol = 'xyz'
    public void run(EPServiceProvider epService) throws Exception {
        for (int i = 0; i < 1000; i++) {
            String text = "select * from MD where symbol = '" + Integer.toString(i) + "'";
            epService.getEPAdministrator().createEPL(text);
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            SupportMarketDataIDBean bean = new SupportMarketDataIDBean("NOMATCH", "", 1);
            epService.getEPRuntime().sendEvent(bean);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("Delta=" + delta, delta < 500);
    }
}
