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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

import static org.junit.Assert.assertEquals;

public class ExecTableOnDelete implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionDeleteFlow(epService);
        runAssertionDeleteSecondaryIndexUpd(epService);
    }

    private void runAssertionDeleteSecondaryIndexUpd(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table MyTable as (pkey0 string primary key, " +
                "pkey1 int primary key, thesum sum(long))");
        epService.getEPAdministrator().createEPL("into table MyTable select sum(longPrimitive) as thesum from SupportBean group by theString, intPrimitive");

        makeSendSupportBean(epService, "E1", 10, 2L);
        makeSendSupportBean(epService, "E2", 20, 3L);
        makeSendSupportBean(epService, "E1", 11, 4L);
        makeSendSupportBean(epService, "E2", 21, 5L);

        epService.getEPAdministrator().createEPL("create index MyIdx on MyTable(pkey0)");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@name('select') on SupportBean_S0 select sum(thesum) as c0 from MyTable where pkey0=p00").addListener(listener);

        assertSum(epService, listener, "E1,E2,E3", new Long[]{6L, 8L, null});

        makeSendSupportBean(epService, "E3", 30, 77L);
        makeSendSupportBean(epService, "E2", 21, 2L);

        assertSum(epService, listener, "E1,E2,E3", new Long[]{6L, 10L, 77L});

        epService.getEPAdministrator().createEPL("@name('on-delete') on SupportBean_S1 delete from MyTable where pkey0=p10 and pkey1=id");

        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "E1"));   // deletes {"E1", 11, 4L}
        assertSum(epService, listener, "E1,E2,E3", new Long[]{2L, 10L, 77L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "E2"));   // deletes {"E2", 20, 3L}
        assertSum(epService, listener, "E1,E2,E3", new Long[]{2L, 7L, 77L});
    }

    private void assertSum(EPServiceProvider epService, SupportUpdateListener listener, String listOfP00, Long[] sums) {
        String[] p00s = listOfP00.split(",");
        assertEquals(p00s.length, sums.length);
        for (int i = 0; i < p00s.length; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00s[i]));
            assertEquals(sums[i], listener.assertOneGetNewAndReset().get("c0"));
        }
    }

    private void runAssertionDeleteFlow(EPServiceProvider epService) {
        SupportUpdateListener listenerDeleteFiltered = new SupportUpdateListener();
        SupportUpdateListener listenerDeleteAll = new SupportUpdateListener();

        String[] fields = "key,thesum".split(",");
        epService.getEPAdministrator().createEPL("create table varagg as (key string primary key, thesum sum(int))");
        epService.getEPAdministrator().createEPL("into table varagg select sum(intPrimitive) as thesum from SupportBean group by theString");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select varagg[p00].thesum as value from SupportBean_S0").addListener(listener);
        EPStatement stmtDeleteFiltered = epService.getEPAdministrator().createEPL("on SupportBean_S1(id = 1) delete from varagg where key = p10");
        EPStatement stmtDeleteAll = epService.getEPAdministrator().createEPL("on SupportBean_S1(id = 2) delete from varagg");

        Object[][] expectedType = new Object[][]{{"key", String.class}, {"thesum", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, stmtDeleteAll.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        stmtDeleteFiltered.addListener(listenerDeleteFiltered);
        stmtDeleteAll.addListener(listenerDeleteAll);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        assertValues(epService, listener, "G1,G2", new Integer[]{10, null});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        assertValues(epService, listener, "G1,G2", new Integer[]{10, 20});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "G1"));
        assertValues(epService, listener, "G1,G2", new Integer[]{null, 20});
        EPAssertionUtil.assertProps(listenerDeleteFiltered.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, null));
        assertValues(epService, listener, "G1,G2", new Integer[]{null, null});
        EPAssertionUtil.assertProps(listenerDeleteAll.assertOneGetNewAndReset(), fields, new Object[]{"G2", 20});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private static void assertValues(EPServiceProvider engine, SupportUpdateListener listener, String keys, Integer[] values) {
        String[] keyarr = keys.split(",");
        assertEquals(keyarr.length, values.length);
        for (int i = 0; i < keyarr.length; i++) {
            engine.getEPRuntime().sendEvent(new SupportBean_S0(0, keyarr[i]));
            EventBean event = listener.assertOneGetNewAndReset();
            assertEquals("Failed for key '" + keyarr[i] + "'", values[i], event.get("value"));
        }
    }

    private void makeSendSupportBean(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(b);
    }
}
