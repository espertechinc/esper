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
package com.espertech.esper.regression.resultset.orderby;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecOrderByAggregationResult implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOrderByLast(epService);
    }

    private void runAssertionOrderByLast(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select last(intPrimitive) as c0, theString as c1  " +
                "from SupportBean#length_batch(5) group by theString order by last(intPrimitive) desc");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 12));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 13));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 14));

        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), "c0,c1".split(","), new Object[][] {{14, "E1"}, {13, "E2"}, {12, "E3"}});

        stmt.destroy();
    }
}
