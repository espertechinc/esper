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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.io.StringWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecJoin20Stream implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class.getName());

        StringWriter buf = new StringWriter();
        buf.append("select * from ");

        String delimiter = "";
        for (int i = 0; i < 20; i++) {
            buf.append(delimiter);
            buf.append("S0(id=" + i + ")#lastevent as s_" + i);
            delimiter = ", ";
        }
        EPStatement stmt = epService.getEPAdministrator().createEPL(buf.toString());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        for (int i = 0; i < 19; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i));
        }
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_S0(19));
        assertTrue(listener.isInvoked());
    }
}