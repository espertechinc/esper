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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Set;

import static junit.framework.TestCase.*;

public class ExecNamedWindowStartStop implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAddRemoveType(epService);
        runAssertionStartStopCreator(epService);
    }

    private void runAssertionAddRemoveType(EPServiceProvider epService) {
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();

        // test remove type with statement used (no force)
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyWindowEventType#keepall (a int, b string)", "stmtOne");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyWindowEventType").toArray(), new String[]{"stmtOne"});

        try {
            configOps.removeEventType("MyWindowEventType", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyWindowEventType"));
        }

        // destroy statement and type
        stmt.destroy();
        assertTrue(configOps.getEventTypeNameUsedBy("MyWindowEventType").isEmpty());
        assertTrue(configOps.isEventTypeExists("MyWindowEventType"));
        assertTrue(configOps.removeEventType("MyWindowEventType", false));
        assertFalse(configOps.removeEventType("MyWindowEventType", false));    // try double-remove
        assertFalse(configOps.isEventTypeExists("MyWindowEventType"));
        try {
            epService.getEPAdministrator().createEPL("select a from MyWindowEventType");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // add back the type
        stmt = epService.getEPAdministrator().createEPL("create window MyWindowEventType#keepall (c int, d string)", "stmtOne");
        assertTrue(configOps.isEventTypeExists("MyWindowEventType"));
        assertFalse(configOps.getEventTypeNameUsedBy("MyWindowEventType").isEmpty());

        // compile
        epService.getEPAdministrator().createEPL("select d from MyWindowEventType", "stmtTwo");
        Object[] usedBy = configOps.getEventTypeNameUsedBy("MyWindowEventType").toArray();
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtOne", "stmtTwo"}, usedBy);
        try {
            epService.getEPAdministrator().createEPL("select a from MyWindowEventType");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // remove with force
        try {
            configOps.removeEventType("MyWindowEventType", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyWindowEventType"));
        }
        assertTrue(configOps.removeEventType("MyWindowEventType", true));
        assertFalse(configOps.isEventTypeExists("MyWindowEventType"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyWindowEventType").isEmpty());

        // add back the type
        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("create window MyWindowEventType#keepall (f int)", "stmtOne");
        assertTrue(configOps.isEventTypeExists("MyWindowEventType"));

        // compile
        epService.getEPAdministrator().createEPL("select f from MyWindowEventType");
        try {
            epService.getEPAdministrator().createEPL("select c from MyWindowEventType");
            fail();
        } catch (EPException ex) {
            // expected
        }
    }

    private void runAssertionStartStopCreator(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate, "stmtCreateFirst");
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create delete stmt
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyWindow";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete, "stmtDelete");

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(stmtTextInsertOne, "stmtInsert");

        // create consumer
        String[] fields = new String[]{"a", "b"};
        String stmtTextSelect = "select a, b from MyWindow as s1";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect, "stmtSelect");
        SupportUpdateListener listenerSelect = new SupportUpdateListener();
        stmtSelect.addListener(listenerSelect);

        // send 1 event
        sendSupportBean(epService, "E1", 1);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E1", 1}});

        // stop creator
        stmtCreate.stop();
        sendSupportBean(epService, "E2", 2);
        assertFalse(listenerSelect.isInvoked());
        assertFalse(listenerWindow.isInvoked());
        assertNull(stmtCreate.iterator());
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E1", 1}});

        // start creator
        stmtCreate.start();
        sendSupportBean(epService, "E3", 3);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
        assertFalse(listenerSelect.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3}});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E3", 3}});

        // stop and start consumer: should pick up last event
        stmtSelect.stop();
        stmtSelect.start();
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E3", 3}});

        sendSupportBean(epService, "E4", 4);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3}, {"E4", 4}});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E3", 3}, {"E4", 4}});

        // destroy creator
        stmtCreate.destroy();
        sendSupportBean(epService, "E5", 5);
        assertFalse(listenerSelect.isInvoked());
        assertFalse(listenerWindow.isInvoked());
        assertNull(stmtCreate.iterator());
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E3", 3}, {"E4", 4}});

        // create window anew
        stmtTextCreate = "create window MyWindow#keepall as select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate, "stmtCreate");
        stmtCreate.addListener(listenerWindow);

        sendSupportBean(epService, "E6", 6);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E6", 6});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E6", 6}});
        assertFalse(listenerSelect.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fields, new Object[][]{{"E3", 3}, {"E4", 4}});

        // create select stmt
        String stmtTextOnSelect = "on " + SupportBean_A.class.getName() + " insert into A select * from MyWindow";
        EPStatement stmtOnSelect = epService.getEPAdministrator().createEPL(stmtTextOnSelect, "stmtOnSelect");

        // assert statement-type reference
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;

        assertTrue(spi.getStatementEventTypeRef().isInUse("MyWindow"));
        Set<String> stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType("MyWindow");
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtCreate", "stmtSelect", "stmtInsert", "stmtDelete", "stmtOnSelect"}, stmtNames.toArray());

        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtCreate", "stmtInsert"}, stmtNames.toArray());

        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportBean_A.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean_A.class.getName());
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtDelete", "stmtOnSelect"}, stmtNames.toArray());

        stmtInsert.destroy();
        stmtDelete.destroy();

        assertTrue(spi.getStatementEventTypeRef().isInUse("MyWindow"));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType("MyWindow");
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtCreate", "stmtSelect", "stmtOnSelect"}, stmtNames.toArray());

        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtCreate"}, stmtNames.toArray());

        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportBean_A.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean_A.class.getName());
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtOnSelect"}, stmtNames.toArray());

        stmtCreate.destroy();

        assertTrue(spi.getStatementEventTypeRef().isInUse("MyWindow"));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType("MyWindow");
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtSelect", "stmtOnSelect"}, stmtNames.toArray());

        assertFalse(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));

        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportBean_A.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean_A.class.getName());
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmtOnSelect"}, stmtNames.toArray());

        stmtOnSelect.destroy();
        stmtSelect.destroy();

        assertFalse(spi.getStatementEventTypeRef().isInUse("MyWindow"));
        assertFalse(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));
        assertFalse(spi.getStatementEventTypeRef().isInUse(SupportBean_A.class.getName()));
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
