/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.util.support.SupportEventTypeAssertionUtil;
import junit.framework.TestCase;

public class TestTableOnDelete extends TestCase {
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        for (Class clazz : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testOnDelete() throws Exception {
        runAssertionDeleteFlow();
        runAssertionDeleteSecondaryIndexUpd();
    }

    private void runAssertionDeleteSecondaryIndexUpd() {
        epService.getEPAdministrator().createEPL("create table MyTable as (pkey0 string primary key, " +
                "pkey1 int primary key, thesum sum(long))");
        epService.getEPAdministrator().createEPL("into table MyTable select sum(longPrimitive) as thesum from SupportBean group by theString, intPrimitive");

        makeSendSupportBean("E1", 10, 2L);
        makeSendSupportBean("E2", 20, 3L);
        makeSendSupportBean("E1", 11, 4L);
        makeSendSupportBean("E2", 21, 5L);

        epService.getEPAdministrator().createEPL("create index MyIdx on MyTable(pkey0)");
        epService.getEPAdministrator().createEPL("@name('select') on SupportBean_S0 select sum(thesum) as c0 from MyTable where pkey0=p00").addListener(listener);

        assertSum("E1,E2,E3", new Long[] {6L, 8L, null});

        makeSendSupportBean("E3", 30, 77L);
        makeSendSupportBean("E2", 21, 2L);

        assertSum("E1,E2,E3", new Long[] {6L, 10L, 77L});

        epService.getEPAdministrator().createEPL("@name('on-delete') on SupportBean_S1 delete from MyTable where pkey0=p10 and pkey1=id");

        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "E1"));   // deletes {"E1", 11, 4L}
        assertSum("E1,E2,E3", new Long[] {2L, 10L, 77L});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "E2"));   // deletes {"E2", 20, 3L}
        assertSum("E1,E2,E3", new Long[] {2L, 7L, 77L});
    }

    private void assertSum(String listOfP00, Long[] sums) {
        String[] p00s = listOfP00.split(",");
        assertEquals(p00s.length, sums.length);
        for (int i = 0; i < p00s.length; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00s[i]));
            assertEquals(sums[i], listener.assertOneGetNewAndReset().get("c0"));
        }
    }

    private void runAssertionDeleteFlow() {
        SupportUpdateListener listenerDeleteFiltered = new SupportUpdateListener();
        SupportUpdateListener listenerDeleteAll = new SupportUpdateListener();

        String[] fields = "key,thesum".split(",");
        epService.getEPAdministrator().createEPL("create table varagg as (key string primary key, thesum sum(int))");
        epService.getEPAdministrator().createEPL("into table varagg select sum(intPrimitive) as thesum from SupportBean group by theString");
        epService.getEPAdministrator().createEPL("select varagg[p00].thesum as value from SupportBean_S0").addListener(listener);
        EPStatement stmtDeleteFiltered = epService.getEPAdministrator().createEPL("on SupportBean_S1(id = 1) delete from varagg where key = p10");
        EPStatement stmtDeleteAll = epService.getEPAdministrator().createEPL("on SupportBean_S1(id = 2) delete from varagg");

        Object[][] expectedType = new Object[][]{{"key", String.class},{"thesum", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, stmtDeleteAll.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        stmtDeleteFiltered.addListener(listenerDeleteFiltered);
        stmtDeleteAll.addListener(listenerDeleteAll);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        assertValues(epService, listener, "G1,G2", new Integer[] {10, null});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        assertValues(epService, listener, "G1,G2", new Integer[] {10, 20});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "G1"));
        assertValues(epService, listener, "G1,G2", new Integer[]{null, 20});
        EPAssertionUtil.assertProps(listenerDeleteFiltered.assertOneGetNewAndReset(), fields, new Object[] {"G1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, null));
        assertValues(epService, listener, "G1,G2", new Integer[] {null, null});
        EPAssertionUtil.assertProps(listenerDeleteAll.assertOneGetNewAndReset(), fields, new Object[] {"G2", 20});

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

    private void makeSendSupportBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(b);
    }
}
