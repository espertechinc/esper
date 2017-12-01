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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanTwo;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class ExecNamedWindowOnDelete implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionFirstUnique(epService);
        runAssertionStaggeredNamedWindow(epService);
        runAssertionCoercionKeyMultiPropIndexes(epService);
        runAssertionCoercionRangeMultiPropIndexes(epService);
        runAssertionCoercionKeyAndRangeMultiPropIndexes(epService);
    }

    private void runAssertionFirstUnique(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String[] fields = new String[]{"theString", "intPrimitive"};
        String stmtTextCreateOne = "create window MyWindowFU#firstunique(theString) as select * from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyWindowFU select * from SupportBean");
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL("on SupportBean_A a delete from MyWindowFU where theString=a.id");
        SupportUpdateListener listenerDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerDelete);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));

        epService.getEPRuntime().sendEvent(new SupportBean_A("A"));
        EPAssertionUtil.assertProps(listenerDelete.assertOneGetNewAndReset(), fields, new Object[]{"A", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 3));
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"A", 3}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("A"));
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
    }

    private void runAssertionStaggeredNamedWindow(EPServiceProvider epService) throws Exception {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionStaggered(epService, rep);
        }
    }

    private void tryAssertionStaggered(EPServiceProvider epService, EventRepresentationChoice outputType) throws Exception {

        String[] fieldsOne = new String[]{"a1", "b1"};
        String[] fieldsTwo = new String[]{"a2", "b2"};

        // create window one
        String stmtTextCreateOne = outputType.getAnnotationText() + " create window MyWindowSTAG#keepall as select theString as a1, intPrimitive as b1 from " + SupportBean.class.getName();
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreateOne.addListener(listenerWindow);
        assertEquals(0, getCount(epService, "MyWindowSTAG"));
        assertTrue(outputType.matchesClass(stmtCreateOne.getEventType().getUnderlyingType()));

        // create window two
        String stmtTextCreateTwo = outputType.getAnnotationText() + " create window MyWindowSTAGTwo#keepall as select theString as a2, intPrimitive as b2 from " + SupportBean.class.getName();
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(stmtTextCreateTwo);
        SupportUpdateListener listenerWindowTwo = new SupportUpdateListener();
        stmtCreateTwo.addListener(listenerWindowTwo);
        assertEquals(0, getCount(epService, "MyWindowSTAGTwo"));
        assertTrue(outputType.matchesClass(stmtCreateTwo.getEventType().getUnderlyingType()));

        // create delete stmt
        String stmtTextDelete = "on MyWindowSTAG delete from MyWindowSTAGTwo where a1 = a2";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerDelete);
        assertEquals(StatementType.ON_DELETE, ((EPStatementSPI) stmtDelete).getStatementMetadata().getStatementType());

        // create insert into
        String stmtTextInsert = "insert into MyWindowSTAG select theString as a1, intPrimitive as b1 from " + SupportBean.class.getName() + "(intPrimitive > 0)";
        epService.getEPAdministrator().createEPL(stmtTextInsert);
        stmtTextInsert = "insert into MyWindowSTAGTwo select theString as a2, intPrimitive as b2 from " + SupportBean.class.getName() + "(intPrimitive < 0)";
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        sendSupportBean(epService, "E1", -10);
        EPAssertionUtil.assertProps(listenerWindowTwo.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"E1", -10});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsTwo, new Object[][]{{"E1", -10}});
        assertFalse(listenerWindow.isInvoked());
        assertEquals(1, getCount(epService, "MyWindowSTAGTwo"));

        sendSupportBean(epService, "E2", 5);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E2", 5});
        EPAssertionUtil.assertPropsPerRow(stmtCreateOne.iterator(), fieldsOne, new Object[][]{{"E2", 5}});
        assertFalse(listenerWindowTwo.isInvoked());
        assertEquals(1, getCount(epService, "MyWindowSTAG"));

        sendSupportBean(epService, "E3", -1);
        EPAssertionUtil.assertProps(listenerWindowTwo.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"E3", -1});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsTwo, new Object[][]{{"E1", -10}, {"E3", -1}});
        assertFalse(listenerWindow.isInvoked());
        assertEquals(2, getCount(epService, "MyWindowSTAGTwo"));

        sendSupportBean(epService, "E3", 1);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E3", 1});
        EPAssertionUtil.assertPropsPerRow(stmtCreateOne.iterator(), fieldsOne, new Object[][]{{"E2", 5}, {"E3", 1}});
        EPAssertionUtil.assertProps(listenerWindowTwo.assertOneGetOldAndReset(), fieldsTwo, new Object[]{"E3", -1});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsTwo, new Object[][]{{"E1", -10}});
        assertEquals(2, getCount(epService, "MyWindowSTAG"));
        assertEquals(1, getCount(epService, "MyWindowSTAGTwo"));

        stmtDelete.destroy();
        stmtCreateOne.destroy();
        stmtCreateTwo.destroy();
        listenerDelete.reset();
        listenerWindow.reset();
        listenerWindowTwo.reset();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowSTAG", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowSTAGTwo", true);
    }

    private void runAssertionCoercionKeyMultiPropIndexes(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowCK#keepall as select " +
                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        List<EPStatement> deleteStatements = new LinkedList<>();
        String stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='DB') as s0 delete from MyWindowCK as win where win.intPrimitive = s0.doubleBoxed";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(1, getNWMW(epService).getNamedWindowIndexes("MyWindowCK").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='DP') as s0 delete from MyWindowCK as win where win.intPrimitive = s0.doublePrimitive";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(1, getNWMW(epService).getNamedWindowIndexes("MyWindowCK").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='IB') as s0 delete from MyWindowCK where MyWindowCK.intPrimitive = s0.intBoxed";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCK").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='IPDP') as s0 delete from MyWindowCK as win where win.intPrimitive = s0.intPrimitive and win.doublePrimitive = s0.doublePrimitive";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCK").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='IPDP2') as s0 delete from MyWindowCK as win where win.doublePrimitive = s0.doublePrimitive and win.intPrimitive = s0.intPrimitive";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCK").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='IPDPIB') as s0 delete from MyWindowCK as win where win.doublePrimitive = s0.doublePrimitive and win.intPrimitive = s0.intPrimitive and win.intBoxed = s0.intBoxed";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCK").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='CAST') as s0 delete from MyWindowCK as win where win.intBoxed = s0.intPrimitive and win.doublePrimitive = s0.doubleBoxed and win.intPrimitive = s0.intBoxed";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCK").length);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowCK select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed "
                + "from " + SupportBean.class.getName() + "(theString like 'E%')";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        sendSupportBean(epService, "E1", 1, 10, 100d, 1000d);
        sendSupportBean(epService, "E2", 2, 20, 200d, 2000d);
        sendSupportBean(epService, "E3", 3, 30, 300d, 3000d);
        sendSupportBean(epService, "E4", 4, 40, 400d, 4000d);
        listenerWindow.reset();

        String[] fields = new String[]{"theString"};
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});

        sendSupportBean(epService, "DB", 0, 0, 0d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBean(epService, "DB", 0, 0, 0d, 3d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E4"}});

        sendSupportBean(epService, "DP", 0, 0, 5d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBean(epService, "DP", 0, 0, 4d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

        sendSupportBean(epService, "IB", 0, -1, 0d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBean(epService, "IB", 0, 1, 0d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}});

        sendSupportBean(epService, "E5", 5, 50, 500d, 5000d);
        sendSupportBean(epService, "E6", 6, 60, 600d, 6000d);
        sendSupportBean(epService, "E7", 7, 70, 700d, 7000d);
        listenerWindow.reset();

        sendSupportBean(epService, "IPDP", 5, 0, 500d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}, {"E6"}, {"E7"}});

        sendSupportBean(epService, "IPDP2", 6, 0, 600d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E6"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}, {"E7"}});

        sendSupportBean(epService, "IPDPIB", 7, 70, 0d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBean(epService, "IPDPIB", 7, 70, 700d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E7"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}});

        sendSupportBean(epService, "E8", 8, 80, 800d, 8000d);
        listenerWindow.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}, {"E8"}});

        sendSupportBean(epService, "CAST", 80, 8, 0, 800d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E8"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}});

        for (EPStatement stmt : deleteStatements) {
            stmt.destroy();
        }
        deleteStatements.clear();

        // late delete on a filled window
        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='LAST') as s0 delete from MyWindowCK as win where win.intPrimitive = s0.intPrimitive and win.doublePrimitive = s0.doublePrimitive";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        sendSupportBean(epService, "LAST", 2, 20, 200, 2000d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        for (EPStatement stmt : deleteStatements) {
            stmt.destroy();
        }

        // test single-two-field index reuse
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create window WinOne#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_ST0 select * from WinOne where theString = key0");
        assertEquals(1, getNWMW(epService).getNamedWindowIndexes("WinOne").length);

        epService.getEPAdministrator().createEPL("on SupportBean_ST0 select * from WinOne where theString = key0 and intPrimitive = p00");
        assertEquals(1, getNWMW(epService).getNamedWindowIndexes("WinOne").length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCoercionRangeMultiPropIndexes(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);

        // create window
        String stmtTextCreate = "create window MyWindowCR#keepall as select " +
                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);
        String stmtText = "insert into MyWindowCR select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
        epService.getEPAdministrator().createEPL(stmtText);
        String[] fields = new String[]{"theString"};

        sendSupportBean(epService, "E1", 1, 10, 100d, 1000d);
        sendSupportBean(epService, "E2", 2, 20, 200d, 2000d);
        sendSupportBean(epService, "E3", 3, 30, 3d, 30d);
        sendSupportBean(epService, "E4", 4, 40, 4d, 40d);
        sendSupportBean(epService, "E5", 5, 50, 500d, 5000d);
        sendSupportBean(epService, "E6", 6, 60, 600d, 6000d);
        listenerWindow.reset();

        List<EPStatement> deleteStatements = new LinkedList<>();
        String stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindowCR as win where win.intPrimitive between s2.doublePrimitiveTwo and s2.doubleBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(1, getNWMW(epService).getNamedWindowIndexes("MyWindowCR").length);

        sendSupportBeanTwo(epService, "T", 0, 0, 0d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBeanTwo(epService, "T", 0, 0, -1d, 1d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindowCR as win where win.intPrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCR").length);

        sendSupportBeanTwo(epService, "T", -2, 2, 0d, 0d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindowCR as win " +
                "where win.intPrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo and win.doublePrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCR").length);

        sendSupportBeanTwo(epService, "T", -3, 3, -3d, 3d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindowCR as win " +
                "where win.doublePrimitive between s2.intPrimitiveTwo and s2.intPrimitiveTwo and win.intPrimitive between s2.intPrimitiveTwo and s2.intPrimitiveTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCR").length);

        sendSupportBeanTwo(epService, "T", -4, 4, -4, 4d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E4"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindowCR as win where win.intPrimitive <= doublePrimitiveTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCR").length);

        sendSupportBeanTwo(epService, "T", 0, 0, 5, 1d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E5"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindowCR as win where win.intPrimitive not between s2.intPrimitiveTwo and s2.intBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCR").length);

        sendSupportBeanTwo(epService, "T", 100, 200, 0, 0d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E6"});

        // delete
        for (EPStatement stmt : deleteStatements) {
            stmt.destroy();
        }
        deleteStatements.clear();
        assertEquals(0, getNWMW(epService).getNamedWindowIndexes("MyWindowCR").length);
    }

    private void runAssertionCoercionKeyAndRangeMultiPropIndexes(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);

        // create window
        String stmtTextCreate = "create window MyWindowCKR#keepall as select " +
                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);
        String stmtText = "insert into MyWindowCKR select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
        epService.getEPAdministrator().createEPL(stmtText);
        String[] fields = new String[]{"theString"};

        sendSupportBean(epService, "E1", 1, 10, 100d, 1000d);
        sendSupportBean(epService, "E2", 2, 20, 200d, 2000d);
        sendSupportBean(epService, "E3", 3, 30, 300d, 3000d);
        sendSupportBean(epService, "E4", 4, 40, 400d, 4000d);
        listenerWindow.reset();

        List<EPStatement> deleteStatements = new LinkedList<>();
        String stmtTextDelete = "on SupportBeanTwo delete from MyWindowCKR where theString = stringTwo and intPrimitive between doublePrimitiveTwo and doubleBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(1, getNWMW(epService).getNamedWindowIndexes("MyWindowCKR").length);

        sendSupportBeanTwo(epService, "T", 0, 0, 1d, 200d);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBeanTwo(epService, "E1", 0, 0, 1d, 200d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1"});

        stmtTextDelete = "on SupportBeanTwo delete from MyWindowCKR where theString = stringTwo and intPrimitive = intPrimitiveTwo and intBoxed between doublePrimitiveTwo and doubleBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCKR").length);

        sendSupportBeanTwo(epService, "E2", 2, 0, 19d, 21d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2"});

        stmtTextDelete = "on SupportBeanTwo delete from MyWindowCKR where intBoxed between doubleBoxedTwo and doublePrimitiveTwo and intPrimitive = intPrimitiveTwo and theString = stringTwo ";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, getNWMW(epService).getNamedWindowIndexes("MyWindowCKR").length);

        sendSupportBeanTwo(epService, "E3", 3, 0, 29d, 34d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3"});

        stmtTextDelete = "on SupportBeanTwo delete from MyWindowCKR where intBoxed between intBoxedTwo and intBoxedTwo and intPrimitive = intPrimitiveTwo and theString = stringTwo ";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(3, getNWMW(epService).getNamedWindowIndexes("MyWindowCKR").length);

        sendSupportBeanTwo(epService, "E4", 4, 40, 0d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E4"});

        // delete
        for (EPStatement stmt : deleteStatements) {
            stmt.destroy();
        }
        deleteStatements.clear();
        assertEquals(0, getNWMW(epService).getNamedWindowIndexes("MyWindowCKR").length);
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive, Integer intBoxed,
                                 double doublePrimitive, Double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        bean.setDoublePrimitive(doublePrimitive);
        bean.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBeanTwo(EPServiceProvider epService, String theString, int intPrimitive, Integer intBoxed,
                                    double doublePrimitive, Double doubleBoxed) {
        SupportBeanTwo bean = new SupportBeanTwo();
        bean.setStringTwo(theString);
        bean.setIntPrimitiveTwo(intPrimitive);
        bean.setIntBoxedTwo(intBoxed);
        bean.setDoublePrimitiveTwo(doublePrimitive);
        bean.setDoubleBoxedTwo(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private long getCount(EPServiceProvider epService, String windowName) throws Exception {
        NamedWindowProcessor processor = getNWMW(epService).getProcessor(windowName);
        return processor.getProcessorInstance(null).getCountDataWindow();
    }

    private NamedWindowMgmtService getNWMW(EPServiceProvider epService) {
        return ((EPServiceProviderSPI) epService).getNamedWindowMgmtService();
    }
}
