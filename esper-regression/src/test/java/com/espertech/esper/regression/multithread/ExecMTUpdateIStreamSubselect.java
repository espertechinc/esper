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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExecMTUpdateIStreamSubselect implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("update istream SupportBean as sb " +
                "set longPrimitive = (select count(*) from SupportBean_S0#keepall as s0 where s0.p00 = sb.theString)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // insert 5 data events for each symbol
        int numGroups = 20;
        int numRepeats = 5;
        for (int i = 0; i < numGroups; i++) {
            for (int j = 0; j < numRepeats; j++) {
                epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "S0_" + i)); // S0_0 .. S0_19 each has 5 events
            }
        }

        List<Thread> threads = new LinkedList<Thread>();
        for (int i = 0; i < numGroups; i++) {
            final int group = i;
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    epService.getEPRuntime().sendEvent(new SupportBean("S0_" + group, 1));
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        // validate results, price must be 5 for each symbol
        assertEquals(numGroups, listener.getNewDataList().size());
        for (EventBean[] newData : listener.getNewDataList()) {
            SupportBean result = (SupportBean) (newData[0]).getUnderlying();
            assertEquals(numRepeats, result.getLongPrimitive());
        }
    }
}

