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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecEPLLiteralConstants implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String statement = "select 0x23 as mybyte, " +
                "'\u0041' as myunicode," +
                "08 as zero8, " +
                "09 as zero9, " +
                "008 as zeroZero8 " +
                "from SupportBean";

        EPStatement stmt = epService.getEPAdministrator().createEPL(statement);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        epService.getEPRuntime().sendEvent(new SupportBean("e1", 100));

        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(),
                "mybyte,myunicode,zero8,zero9,zeroZero8".split(","),
                new Object[]{(byte) 35, "A", 8, 9, 8});
    }
}
