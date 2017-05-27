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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;

import static org.junit.Assert.*;

public class ExecNamedWindowOnSelect implements RegressionExecution, IndexBackingTableInfo {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportQueryPlanIndexHook.reset();
        String[] fields = new String[]{"theString", "intPrimitive"};

        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select * from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select * from " + SupportBean.class.getName() + "(theString like 'E%')";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create on-select stmt
        String stmtTextSelect = "on " + SupportBean_A.class.getName() + " insert into MyStream select mywin.* from MyWindow as mywin order by theString asc";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        stmtSelect.addListener(listenerSelect);
        assertEquals(StatementType.ON_INSERT, ((EPStatementSPI) stmtSelect).getStatementMetadata().getStatementType());

        // create consuming statement
        String stmtTextConsumer = "select * from default.MyStream";
        EPStatement stmtConsumer = epService.getEPAdministrator().createEPL(stmtTextConsumer);
        SupportUpdateListener listenerConsumer = new SupportUpdateListener();
        stmtConsumer.addListener(listenerConsumer);

        // create second inserting statement
        String stmtTextInsertTwo = "insert into MyStream select * from " + SupportBean.class.getName() + "(theString like 'I%')";
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        // send event
        sendSupportBean(epService, "E1", 1);
        assertFalse(listenerSelect.isInvoked());
        assertFalse(listenerConsumer.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        // fire trigger
        sendSupportBean_A(epService, "A1");
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertProps(listenerConsumer.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        // insert via 2nd insert into
        sendSupportBean(epService, "I2", 2);
        assertFalse(listenerSelect.isInvoked());
        EPAssertionUtil.assertProps(listenerConsumer.assertOneGetNewAndReset(), fields, new Object[]{"I2", 2});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});

        // send event
        sendSupportBean(epService, "E3", 3);
        assertFalse(listenerSelect.isInvoked());
        assertFalse(listenerConsumer.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});

        // fire trigger
        sendSupportBean_A(epService, "A2");
        assertEquals(1, listenerSelect.getNewDataList().size());
        EPAssertionUtil.assertPropsPerRow(listenerSelect.getLastNewData(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
        listenerSelect.reset();
        assertEquals(2, listenerConsumer.getNewDataList().size());
        EPAssertionUtil.assertPropsPerRow(listenerConsumer.getNewDataListFlattened(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
        listenerConsumer.reset();

        // check type
        EventType consumerType = stmtConsumer.getEventType();
        assertEquals(String.class, consumerType.getPropertyType("theString"));
        assertTrue(consumerType.getPropertyNames().length > 10);
        assertEquals(SupportBean.class, consumerType.getUnderlyingType());

        // check type
        EventType onSelectType = stmtSelect.getEventType();
        assertEquals(String.class, onSelectType.getPropertyType("theString"));
        assertTrue(onSelectType.getPropertyNames().length > 10);
        assertEquals(SupportBean.class, onSelectType.getUnderlyingType());

        // delete all from named window
        String stmtTextDelete = "on " + SupportBean_B.class.getName() + " delete from MyWindow";
        epService.getEPAdministrator().createEPL(stmtTextDelete);
        sendSupportBean_B(epService, "B1");

        // fire trigger - nothing to insert
        sendSupportBean_A(epService, "A3");

        stmtConsumer.destroy();
        stmtSelect.destroy();
        stmtCreate.destroy();
    }

    private void sendSupportBean_A(EPServiceProvider epService, String id) {
        SupportBean_A bean = new SupportBean_A(id);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBean_B(EPServiceProvider epService, String id) {
        SupportBean_B bean = new SupportBean_B(id);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
