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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecViewTimeFirst implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendCurrentTime(epService, "2002-02-01T09:00:00.000");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#firsttime(1 month)");

        sendCurrentTime(epService, "2002-02-15T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        sendCurrentTimeWithMinus(epService, "2002-03-01T09:00:00.000", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        sendCurrentTime(epService, "2002-03-01T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));

        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), "theString".split(","), new Object[][]{{"E1"}, {"E2"}});
    }

    private void sendCurrentTime(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendCurrentTimeWithMinus(EPServiceProvider epService, String time, long minus) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time) - minus));
    }
}
