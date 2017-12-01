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
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ExecTableOnUpdate implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, MyUpdateEvent.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        SupportUpdateListener listenerUpdate = new SupportUpdateListener();

        String[] fields = "keyOne,keyTwo,p0".split(",");
        epService.getEPAdministrator().createEPL("create table varagg as (" +
                "keyOne string primary key, keyTwo int primary key, p0 long)");
        epService.getEPAdministrator().createEPL("on SupportBean merge varagg where theString = keyOne and " +
                "intPrimitive = keyTwo when not matched then insert select theString as keyOne, intPrimitive as keyTwo, 1 as p0");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select varagg[p00, id].p0 as value from SupportBean_S0").addListener(listener);
        EPStatement stmtUpdate = epService.getEPAdministrator().createEPL("on MyUpdateEvent update varagg set p0 = newValue " +
                "where k1 = keyOne and k2 = keyTwo");
        stmtUpdate.addListener(listenerUpdate);

        Object[][] expectedType = new Object[][]{{"keyOne", String.class}, {"keyTwo", Integer.class}, {"p0", Long.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, stmtUpdate.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        assertValues(epService, listener, new Object[][]{{"G1", 10}}, new Long[]{1L});

        epService.getEPRuntime().sendEvent(new MyUpdateEvent("G1", 10, 2));
        assertValues(epService, listener, new Object[][]{{"G1", 10}}, new Long[]{2L});
        EPAssertionUtil.assertProps(listenerUpdate.getLastNewData()[0], fields, new Object[]{"G1", 10, 2L});
        EPAssertionUtil.assertProps(listenerUpdate.getAndResetLastOldData()[0], fields, new Object[]{"G1", 10, 1L});

        // try property method invocation
        epService.getEPAdministrator().createEPL("create table MyTableSuppBean as (sb SupportBean)");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 update MyTableSuppBean sb set sb.setLongPrimitive(10)");
    }

    private void assertValues(EPServiceProvider epService, SupportUpdateListener listener, Object[][] keys, Long[] values) {
        assertEquals(keys.length, values.length);
        for (int i = 0; i < keys.length; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0((Integer) keys[i][1], (String) keys[i][0]));
            EventBean event = listener.assertOneGetNewAndReset();
            assertEquals("Failed for key '" + Arrays.toString(keys[i]) + "'", values[i], event.get("value"));
        }
    }

    public static class MyUpdateEvent {
        private final String k1;
        private final int k2;
        private final int newValue;

        private MyUpdateEvent(String k1, int k2, int newValue) {
            this.k1 = k1;
            this.k2 = k2;
            this.newValue = newValue;
        }

        public String getK1() {
            return k1;
        }

        public int getK2() {
            return k2;
        }

        public int getNewValue() {
            return newValue;
        }
    }
}
