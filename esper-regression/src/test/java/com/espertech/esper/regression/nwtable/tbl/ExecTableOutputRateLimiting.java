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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;

public class ExecTableOutputRateLimiting implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        AtomicLong currentTime = new AtomicLong(0);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime.get()));

        epService.getEPAdministrator().createEPL("@name('create') create table MyTable as (\n" +
                "key string primary key, thesum sum(int))");
        epService.getEPAdministrator().createEPL("@name('select') into table MyTable " +
                "select sum(intPrimitive) as thesum from SupportBean group by theString");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 30));
        epService.getEPAdministrator().getStatement("create").destroy();

        EPStatement stmt = epService.getEPAdministrator().createEPL("select key, thesum from MyTable output snapshot every 1 seconds");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        currentTime.set(currentTime.get() + 1000L);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime.get()));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "key,thesum".split(","),
                new Object[][]{{"E1", 40}, {"E2", 20}});

        currentTime.set(currentTime.get() + 1000L);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime.get()));
        assertTrue(listener.isInvoked());

        stmt.destroy();
    }

}
