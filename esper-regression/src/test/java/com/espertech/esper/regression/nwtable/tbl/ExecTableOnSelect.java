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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecTableOnSelect implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        epService.getEPAdministrator().createEPL("create table varagg as (" +
                "key string primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table varagg " +
                "select sum(intPrimitive) as total from SupportBean group by theString");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("on SupportBean_S0 select total as value from varagg where key = p00").addListener(listener);

        assertValues(epService, listener, "G1,G2", new Integer[]{null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 100));
        assertValues(epService, listener, "G1,G2", new Integer[]{100, null});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        assertValues(epService, listener, "G1,G2", new Integer[]{100, 200});

        epService.getEPAdministrator().createEPL("on SupportBean_S1 insert into MyStream select total from varagg where key = p10");
        epService.getEPAdministrator().createEPL("select * from MyStream").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 300));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "G2"));
        assertEquals(500, listener.assertOneGetNewAndReset().get("total"));
    }

    private static void assertValues(EPServiceProvider engine, SupportUpdateListener listener, String keys, Integer[] values) {
        String[] keyarr = keys.split(",");
        for (int i = 0; i < keyarr.length; i++) {
            engine.getEPRuntime().sendEvent(new SupportBean_S0(0, keyarr[i]));
            if (values[i] == null) {
                assertFalse(listener.isInvoked());
            } else {
                EventBean event = listener.assertOneGetNewAndReset();
                assertEquals("Failed for key '" + keyarr[i] + "'", values[i], event.get("value"));
            }
        }
    }
}
