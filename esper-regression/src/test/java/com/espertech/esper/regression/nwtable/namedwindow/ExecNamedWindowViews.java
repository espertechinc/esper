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
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.hook.ObjectValueTypeWidenerFactory;
import com.espertech.esper.client.hook.ObjectValueTypeWidenerFactoryContext;
import com.espertech.esper.client.hook.TypeRepresentationMapper;
import com.espertech.esper.client.hook.TypeRepresentationMapperContext;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.support.EventRepresentationChoice;
import com.espertech.esper.util.TypeWidener;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;

public class ExecNamedWindowViews implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        Map<String, Object> types = new HashMap<>();
        types.put("key", String.class);
        types.put("value", long.class);

        configuration.addEventType("MyMap", types);
        configuration.getEngineDefaults().getEventMeta().getAvroSettings().setObjectValueTypeWidenerFactoryClass(MyAvroTypeWidenerFactory.class.getName());
        configuration.getEngineDefaults().getEventMeta().getAvroSettings().setTypeRepresentationMapperClass(MyAvroTypeRepMapper.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionBeanBacked(epService);
        runAssertionBeanContained(epService);
        runAssertionIntersection(epService);
        runAssertionBeanSchemaBacked(epService);
        runAssertionDeepSupertypeInsert(epService);
        runAssertionOnInsertPremptiveTwoWindow(epService);
        runAssertionWithDeleteUseAs(epService);
        runAssertionWithDeleteFirstAs(epService);
        runAssertionWithDeleteSecondAs(epService);
        runAssertionWithDeleteNoAs(epService);
        runAssertionTimeWindow(epService);
        runAssertionTimeFirstWindow(epService);
        runAssertionExtTimeWindow(epService);
        runAssertionTimeOrderWindow(epService);
        runAssertionLengthWindow(epService);
        runAssertionLengthFirstWindow(epService);
        runAssertionTimeAccum(epService);
        runAssertionTimeBatch(epService);
        runAssertionTimeBatchLateConsumer(epService);
        runAssertionLengthBatch(epService);
        runAssertionSortWindow(epService);
        runAssertionTimeLengthBatch(epService);
        runAssertionLengthWindowPerGroup(epService);
        runAssertionTimeBatchPerGroup(epService);
        runAssertionDoubleInsertSameWindow(epService);
        runAssertionLastEvent(epService);
        runAssertionFirstEvent(epService);
        runAssertionUnique(epService);
        runAssertionFirstUnique(epService);
        runAssertionFilteringConsumer(epService);
        runAssertionSelectGroupedViewLateStart(epService);
        runAssertionSelectGroupedViewLateStartVariableIterate(epService);
        runAssertionFilteringConsumerLateStart(epService);
        runAssertionInvalid(epService);
        runAssertionAlreadyExists(epService);
        runAssertionConsumerDataWindow(epService);
        runAssertionPriorStats(epService);
        runAssertionLateConsumer(epService);
        runAssertionLateConsumerJoin(epService);
        runAssertionPattern(epService);
        runAssertionExternallyTimedBatch(epService);
        runAssertionSelectStreamDotStarInsert(epService);

    }

    private void runAssertionSelectStreamDotStarInsert(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL(EventRepresentationChoice.ARRAY.getAnnotationText() + " create window MyNWWindowObjectArray#keepall (p0 int)");
        epService.getEPAdministrator().createEPL("insert into MyNWWindowObjectArray select intPrimitive as p0, sb.* as c0 from SupportBean as sb");
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBeanBacked(EPServiceProvider epService) {
        tryAssertionBeanBacked(epService, EventRepresentationChoice.ARRAY);
        tryAssertionBeanBacked(epService, EventRepresentationChoice.MAP);
        tryAssertionBeanBacked(epService, EventRepresentationChoice.DEFAULT);

        try {
            tryAssertionBeanBacked(epService, EventRepresentationChoice.AVRO);
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Avro event type does not allow contained beans");
        }
    }

    private void runAssertionBeanContained(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            if (!rep.isAvroEvent()) {
                tryAssertionBeanContained(epService, rep);
            }
        }

        try {
            tryAssertionBeanContained(epService, EventRepresentationChoice.AVRO);
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Avro event type does not allow contained beans");
        }
    }

    private void tryAssertionBeanContained(EPServiceProvider epService, EventRepresentationChoice rep) {
        EPStatement stmtW = epService.getEPAdministrator().createEPL(rep.getAnnotationText() + " create window MyWindowBC#keepall as (bean " + SupportBean_S0.class.getName() + ")");
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtW.addListener(listenerWindow);
        assertTrue(rep.matchesClass(stmtW.getEventType().getUnderlyingType()));
        epService.getEPAdministrator().createEPL("insert into MyWindowBC select bean.* as bean from " + SupportBean_S0.class.getName() + " as bean");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), "bean.p00".split(","), new Object[]{"E1"});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowBC", true);
    }

    private void runAssertionIntersection(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        DeploymentResult deployed = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create window MyWindowINT#length(2)#unique(intPrimitive) as SupportBean;\n" +
                        "insert into MyWindowINT select * from SupportBean;\n" +
                        "@name('out') select irstream * from MyWindowINT");

        String[] fields = "theString".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields, new Object[][]{{"E1"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields, new Object[][]{{"E2"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.assertInvokedAndReset(), fields, new Object[][]{{"E3"}}, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deployed.getDeploymentId());
    }

    private void tryAssertionBeanBacked(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        // Test create from class
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window MyWindowBB#keepall as SupportBean");
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmt.addListener(listenerWindow);
        epService.getEPAdministrator().createEPL("insert into MyWindowBB select * from SupportBean");

        EPStatementSPI stmtConsume = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from MyWindowBB");
        assertTrue(stmtConsume.getStatementContext().isStatelessSelect());
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtConsume.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEvent(listenerWindow.assertOneGetNewAndReset(), "MyWindowBB");
        assertEvent(listenerStmtOne.assertOneGetNewAndReset(), "MyWindowBB");

        EPStatement stmtUpdate = epService.getEPAdministrator().createEPL("on SupportBean_A update MyWindowBB set theString='s'");
        SupportUpdateListener listenerStmtTwo = new SupportUpdateListener();
        stmtUpdate.addListener(listenerStmtTwo);
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        assertEvent(listenerStmtTwo.getLastNewData()[0], "MyWindowBB");

        // test bean-property
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowBB", true);
        listenerWindow.reset();
        listenerStmtOne.reset();
    }

    private void runAssertionBeanSchemaBacked(EPServiceProvider epService) {

        // Test create from schema
        epService.getEPAdministrator().createEPL("create schema ABC as " + SupportBean.class.getName());
        epService.getEPAdministrator().createEPL("create window MyWindowBSB#keepall as ABC");
        epService.getEPAdministrator().createEPL("insert into MyWindowBSB select * from " + SupportBean.class.getName());

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEvent(epService.getEPRuntime().executeQuery("select * from MyWindowBSB").getArray()[0], "MyWindowBSB");

        EPStatement stmtABC = epService.getEPAdministrator().createEPL("select * from ABC");
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtABC.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(listenerStmtOne.isInvoked());
    }

    private void assertEvent(EventBean theEvent, String name) {
        assertTrue(theEvent.getEventType() instanceof BeanEventType);
        assertTrue(theEvent.getUnderlying() instanceof SupportBean);
        assertEquals(EventTypeMetadata.TypeClass.NAMED_WINDOW, ((EventTypeSPI) theEvent.getEventType()).getMetadata().getTypeClass());
        assertEquals(name, theEvent.getEventType().getName());
    }

    private void runAssertionDeepSupertypeInsert(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportOverrideOneA", SupportOverrideOneA.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportOverrideOne", SupportOverrideOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportOverrideBase", SupportOverrideBase.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyWindowDSI#keepall as select * from SupportOverrideBase");
        epService.getEPAdministrator().createEPL("insert into MyWindowDSI select * from SupportOverrideOneA");
        epService.getEPRuntime().sendEvent(new SupportOverrideOneA("1a", "1", "base"));
        assertEquals("1a", stmt.iterator().next().get("val"));
    }

    // Assert the named window is updated at the time that a subsequent event queries the named window
    private void runAssertionOnInsertPremptiveTwoWindow(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema TypeOne(col1 int)");
        epService.getEPAdministrator().createEPL("create schema TypeTwo(col2 int)");
        epService.getEPAdministrator().createEPL("create schema TypeTrigger(trigger int)");
        epService.getEPAdministrator().createEPL("create schema SupportBean as " + SupportBean.class.getName());

        epService.getEPAdministrator().createEPL("create window WinOne#keepall as TypeOne");
        epService.getEPAdministrator().createEPL("create window WinTwo#keepall as TypeTwo");

        epService.getEPAdministrator().createEPL("insert into WinOne(col1) select intPrimitive from SupportBean");

        epService.getEPAdministrator().createEPL("on TypeTrigger insert into OtherStream select col1 from WinOne");
        epService.getEPAdministrator().createEPL("on TypeTrigger insert into WinTwo(col2) select col1 from WinOne");
        EPStatement stmt = epService.getEPAdministrator().createEPL("on OtherStream select col2 from WinTwo");
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmt.addListener(listenerStmtOne);

        // populate WinOne
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 9));

        // fire trigger
        if (EventRepresentationChoice.getEngineDefault(epService).isObjectArrayEvent()) {
            epService.getEPRuntime().getEventSender("TypeTrigger").sendEvent(new Object[0]);
        } else {
            epService.getEPRuntime().getEventSender("TypeTrigger").sendEvent(new HashMap());
        }

        assertEquals(9, listenerStmtOne.assertOneGetNewAndReset().get("col2"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWithDeleteUseAs(EPServiceProvider epService) {
        tryCreateWindow(epService, "create window MyWindow#keepall as MyMap",
                "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key");
    }

    private void runAssertionWithDeleteFirstAs(EPServiceProvider epService) {
        tryCreateWindow(epService, "create window MyWindow#keepall as select key, value from MyMap",
                "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow as s1 where symbol = s1.key");
    }

    private void runAssertionWithDeleteSecondAs(EPServiceProvider epService) {
        tryCreateWindow(epService, "create window MyWindow#keepall as MyMap",
                "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow where s0.symbol = key");
    }

    private void runAssertionWithDeleteNoAs(EPServiceProvider epService) {
        tryCreateWindow(epService, "create window MyWindow#keepall as select key as key, value as value from MyMap",
                "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where symbol = key");
    }

    private void tryCreateWindow(EPServiceProvider epService, String createWindowStatement, String deleteStatement) {
        String[] fields = new String[]{"key", "value"};
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(createWindowStatement);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("key"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("value"));

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(stmtTextInsert);

        String stmtTextSelectOne = "select irstream key, value*2 as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        String stmtTextSelectTwo = "select irstream key, sum(value) as value from MyWindow group by key";
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectTwo);
        SupportUpdateListener listenerStmtTwo = new SupportUpdateListener();
        stmtSelectTwo.addListener(listenerStmtTwo);

        String stmtTextSelectThree = "select irstream key, value from MyWindow where value >= 10";
        EPStatement stmtSelectThree = epService.getEPAdministrator().createEPL(stmtTextSelectThree);
        SupportUpdateListener listenerStmtThree = new SupportUpdateListener();
        stmtSelectThree.addListener(listenerStmtThree);

        // send events
        sendSupportBean(epService, "E1", 10L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E1", null});
        listenerStmtTwo.reset();
        EPAssertionUtil.assertProps(listenerStmtThree.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{"E1", 20L}});

        sendSupportBean(epService, "E2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E2", null});
        listenerStmtTwo.reset();
        EPAssertionUtil.assertProps(listenerStmtThree.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E2", 20L}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{"E1", 20L}, {"E2", 40L}});

        sendSupportBean(epService, "E3", 5L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 10L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E3", 5L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E3", null});
        listenerStmtTwo.reset();
        assertFalse(listenerStmtThree.isInvoked());
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 5L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E2", 20L}, {"E3", 5L}});

        // create delete stmt
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(deleteStatement);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        // send delete event
        sendMarketBean(epService, "E1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E1", null});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E1", 10L});
        listenerStmtTwo.reset();
        EPAssertionUtil.assertProps(listenerStmtThree.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 20L}, {"E3", 5L}});

        // send delete event again, none deleted now
        sendMarketBean(epService, "E1");
        assertFalse(listenerStmtOne.isInvoked());
        assertFalse(listenerStmtTwo.isInvoked());
        assertFalse(listenerWindow.isInvoked());
        assertTrue(listenerStmtDelete.isInvoked());
        listenerStmtDelete.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 20L}, {"E3", 5L}});

        // send delete event
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E2", null});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E2", 20L});
        listenerStmtTwo.reset();
        EPAssertionUtil.assertProps(listenerStmtThree.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 5L}});

        // send delete event
        sendMarketBean(epService, "E3");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E3", 10L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E3", null});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E3", 5L});
        listenerStmtTwo.reset();
        assertFalse(listenerStmtThree.isInvoked());
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3", 5L});
        assertTrue(listenerStmtDelete.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        stmtSelectOne.destroy();
        stmtSelectTwo.destroy();
        stmtSelectThree.destroy();
        stmtDelete.destroy();
        stmtInsert.destroy();
        stmtCreate.destroy();
    }

    private void runAssertionTimeWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowTW#time(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        // create insert into
        String stmtTextInsert = "insert into MyWindowTW select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowTW";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowTW as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(epService, 1000);
        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendTimer(epService, 5000);
        sendSupportBean(epService, "E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        sendTimer(epService, 10000);
        sendSupportBean(epService, "E3", 3L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // Should push out the window
        sendTimer(epService, 10999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        sendTimer(epService, 11000);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2L}, {"E3", 3L}});

        sendSupportBean(epService, "E4", 4L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2L}, {"E3", 3L}, {"E4", 4L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3L}, {"E4", 4L}});

        // nothing pushed
        sendTimer(epService, 15000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        // push last event
        sendTimer(epService, 19999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        sendTimer(epService, 20000);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 4L}});

        // delete E4
        sendMarketBean(epService, "E4");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(epService, 100000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTimeFirstWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        sendTimer(epService, 1000);

        // create window
        String stmtTextCreate = "create window MyWindowTFW#firsttime(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        // create insert into
        String stmtTextInsert = "insert into MyWindowTFW select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowTFW";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowTFW as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendTimer(epService, 5000);
        sendSupportBean(epService, "E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        sendTimer(epService, 10000);
        sendSupportBean(epService, "E3", 3L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // Should not push out the window
        sendTimer(epService, 12000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        sendSupportBean(epService, "E4", 4L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        // nothing pushed
        sendTimer(epService, 100000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionExtTimeWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowETW#ext_timed(value, 10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowETW select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowETW";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindowETW where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1000L}});

        sendSupportBean(epService, "E2", 5000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 5000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 5000L});

        sendSupportBean(epService, "E3", 10000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 10000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 10000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1000L}, {"E2", 5000L}, {"E3", 10000L}});

        // Should push out the window
        sendSupportBean(epService, "E4", 11000L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E4", 11000L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E1", 1000L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E4", 11000L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1000L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 5000L}, {"E3", 10000L}, {"E4", 11000L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 5000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 5000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 10000L}, {"E4", 11000L}});

        // nothing pushed other then E5 (E2 is deleted)
        sendSupportBean(epService, "E5", 15000L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E5", 15000L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E5", 15000L});
        assertNull(listenerWindow.getLastOldData());
        assertNull(listenerStmtOne.getLastOldData());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTimeOrderWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowTOW#time_order(value, 10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowTOW select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowTOW";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindowTOW where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(epService, 5000);
        sendSupportBean(epService, "E1", 3000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3000L});

        sendTimer(epService, 6000);
        sendSupportBean(epService, "E2", 2000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2000L});

        sendTimer(epService, 10000);
        sendSupportBean(epService, "E3", 1000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 1000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 1000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 1000L}, {"E2", 2000L}, {"E1", 3000L}});

        // Should push out the window
        sendTimer(epService, 11000);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3", 1000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E3", 1000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2000L}, {"E1", 3000L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 3000L}});

        sendTimer(epService, 12999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(epService, 13000);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 3000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 3000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(epService, 100000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLengthWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowLW#length(3) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowLW select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowLW";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindowLW where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        sendSupportBean(epService, "E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

        sendSupportBean(epService, "E3", 3L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        sendSupportBean(epService, "E4", 4L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}, {"E4", 4L}});

        sendSupportBean(epService, "E5", 5L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E5", 5L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E5", 5L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3L}, {"E4", 4L}, {"E5", 5L}});

        sendSupportBean(epService, "E6", 6L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E6", 6L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E3", 3L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E6", 6L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E3", 3L});
        listenerStmtOne.reset();

        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLengthFirstWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowLFW#firstlength(2) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowLFW select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowLFW";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindowLFW where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        sendSupportBean(epService, "E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

        sendSupportBean(epService, "E3", 3L);
        assertFalse(listenerWindow.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean(epService, "E4", 4L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E4", 4L}});

        sendSupportBean(epService, "E5", 5L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E4", 4L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTimeAccum(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowTA#time_accum(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowTA select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowTA";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowTA as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(epService, 1000);
        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        sendTimer(epService, 5000);
        sendSupportBean(epService, "E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

        sendTimer(epService, 10000);
        sendSupportBean(epService, "E3", 3L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});

        sendTimer(epService, 15000);
        sendSupportBean(epService, "E4", 4L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}, {"E4", 4L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}, {"E4", 4L}});

        // nothing pushed
        sendTimer(epService, 24999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(epService, 25000);
        assertNull(listenerWindow.getLastNewData());
        EventBean[] oldData = listenerWindow.getLastOldData();
        assertEquals(3, oldData.length);
        EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(oldData[2], fields, new Object[]{"E4", 4L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        // delete E4
        sendMarketBean(epService, "E4");
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(epService, 30000);
        sendSupportBean(epService, "E5", 5L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E5", 5L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E5", 5L});

        sendTimer(epService, 31000);
        sendSupportBean(epService, "E6", 6L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E6", 6L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E6", 6L});

        sendTimer(epService, 38000);
        sendSupportBean(epService, "E7", 7L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E5", 5L}, {"E6", 6L}, {"E7", 7L}});

        // delete E7 - deleting the last should spit out the first 2 timely
        sendMarketBean(epService, "E7");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E5", 5L}, {"E6", 6L}});

        sendTimer(epService, 40999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(epService, 41000);
        assertNull(listenerStmtOne.getLastNewData());
        oldData = listenerStmtOne.getLastOldData();
        assertEquals(2, oldData.length);
        EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E5", 5L});
        EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E6", 6L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(epService, 50000);
        sendSupportBean(epService, "E8", 8L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E8", 8L}});

        sendTimer(epService, 55000);
        sendMarketBean(epService, "E8");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(epService, 100000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTimeBatch(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowTB#time_batch(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowTB select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindowTB";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowTB as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(epService, 1000);
        sendSupportBean(epService, "E1", 1L);

        sendTimer(epService, 5000);
        sendSupportBean(epService, "E2", 2L);

        sendTimer(epService, 10000);
        sendSupportBean(epService, "E3", 3L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        // nothing pushed
        sendTimer(epService, 10999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(epService, 11000);
        assertNull(listenerWindow.getLastOldData());
        EventBean[] newData = listenerWindow.getLastNewData();
        assertEquals(2, newData.length);
        EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(epService, 21000);
        assertNull(listenerWindow.getLastNewData());
        EventBean[] oldData = listenerWindow.getLastOldData();
        assertEquals(2, oldData.length);
        EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
        listenerWindow.reset();
        listenerStmtOne.reset();

        // send and delete E4, leaving an empty batch
        sendSupportBean(epService, "E4", 4L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 4L}});

        sendMarketBean(epService, "E4");
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(epService, 31000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTimeBatchLateConsumer(EPServiceProvider epService) {
        sendTimer(epService, 0);

        // create window
        String stmtTextCreate = "create window MyWindowTBLC#time_batch(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowTBLC select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        sendTimer(epService, 0);
        sendSupportBean(epService, "E1", 1L);

        sendTimer(epService, 5000);
        sendSupportBean(epService, "E2", 2L);

        // create consumer
        String stmtTextSelectOne = "select sum(value) as value from MyWindowTBLC";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        sendTimer(epService, 8000);
        sendSupportBean(epService, "E3", 3L);
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(epService, 10000);
        EventBean[] newData = listenerStmtOne.getLastNewData();
        assertEquals(1, newData.length);
        EPAssertionUtil.assertProps(newData[0], new String[]{"value"}, new Object[]{6L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), new String[]{"value"}, null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLengthBatch(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowLB#length_batch(3) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowLB select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindowLB";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowLB as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1L);
        sendSupportBean(epService, "E2", 2L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean(epService, "E3", 3L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        sendSupportBean(epService, "E4", 4L);
        assertNull(listenerWindow.getLastOldData());
        EventBean[] newData = listenerWindow.getLastNewData();
        assertEquals(3, newData.length);
        EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(newData[2], fields, new Object[]{"E4", 4L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean(epService, "E5", 5L);
        sendSupportBean(epService, "E6", 6L);
        sendMarketBean(epService, "E5");
        sendMarketBean(epService, "E6");
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean(epService, "E7", 7L);
        sendSupportBean(epService, "E8", 8L);
        sendSupportBean(epService, "E9", 9L);
        EventBean[] oldData = listenerWindow.getLastOldData();
        newData = listenerWindow.getLastNewData();
        assertEquals(3, newData.length);
        assertEquals(3, oldData.length);
        EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertProps(newData[2], fields, new Object[]{"E9", 9L});
        EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(oldData[2], fields, new Object[]{"E4", 4L});
        listenerWindow.reset();
        listenerStmtOne.reset();

        sendSupportBean(epService, "E10", 10L);
        sendSupportBean(epService, "E10", 11L);
        sendMarketBean(epService, "E10");

        sendSupportBean(epService, "E21", 21L);
        sendSupportBean(epService, "E22", 22L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        sendSupportBean(epService, "E23", 23L);
        oldData = listenerWindow.getLastOldData();
        newData = listenerWindow.getLastNewData();
        assertEquals(3, newData.length);
        assertEquals(3, oldData.length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSortWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowSW#sort(3, value asc) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowSW select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindowSW";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowSW as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 10L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});

        sendSupportBean(epService, "E2", 20L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});

        sendSupportBean(epService, "E3", 15L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 15L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E2", 20L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E3", 15L}});

        sendSupportBean(epService, "E4", 18L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 18L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E4", 18L}});

        sendSupportBean(epService, "E5", 17L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E5", 17L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E4", 18L});
        listenerWindow.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E5", 17L}});

        // delete E1
        sendMarketBean(epService, "E1");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 15L}, {"E5", 17L}});

        sendSupportBean(epService, "E6", 16L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E6", 16L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 15L}, {"E6", 16L}, {"E5", 17L}});

        sendSupportBean(epService, "E7", 16L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E7", 16L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E5", 17L});
        listenerWindow.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 15L}, {"E7", 16L}, {"E6", 16L}});

        // delete E7 has no effect
        sendMarketBean(epService, "E7");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E7", 16L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 15L}, {"E6", 16L}});

        sendSupportBean(epService, "E8", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E8", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E8", 1L}, {"E3", 15L}, {"E6", 16L}});

        sendSupportBean(epService, "E9", 1L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E9", 1L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E6", 16L});
        listenerWindow.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTimeLengthBatch(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowTLB#time_length_batch(10 sec, 3) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowTLB select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindowTLB";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowTLB as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(epService, 1000);
        sendSupportBean(epService, "E1", 1L);
        sendSupportBean(epService, "E2", 2L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        // delete E2
        sendMarketBean(epService, "E2");
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean(epService, "E3", 3L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        sendSupportBean(epService, "E4", 4L);
        assertNull(listenerWindow.getLastOldData());
        EventBean[] newData = listenerWindow.getLastNewData();
        assertEquals(3, newData.length);
        EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(newData[2], fields, new Object[]{"E4", 4L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(epService, 5000);
        sendSupportBean(epService, "E5", 5L);
        sendSupportBean(epService, "E6", 6L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E5", 5L}, {"E6", 6L}});

        sendMarketBean(epService, "E5");   // deleting E5
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E6", 6L}});

        sendTimer(epService, 10999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(epService, 11000);
        newData = listenerWindow.getLastNewData();
        assertEquals(1, newData.length);
        EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E6", 6L});
        EventBean[] oldData = listenerWindow.getLastOldData();
        assertEquals(3, oldData.length);
        EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(oldData[2], fields, new Object[]{"E4", 4L});
        listenerWindow.reset();
        listenerStmtOne.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLengthWindowPerGroup(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowWPG#groupwin(value)#length(2) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowWPG select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowWPG";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindowWPG where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        sendSupportBean(epService, "E2", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});

        sendSupportBean(epService, "E3", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 1L}, {"E3", 2L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 2L}});

        sendSupportBean(epService, "E4", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 1L});

        sendSupportBean(epService, "E5", 1L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E5", 1L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E5", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerStmtOne.reset();

        sendSupportBean(epService, "E6", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E6", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E6", 2L});

        // delete E6
        sendMarketBean(epService, "E6");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E6", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E6", 2L});

        sendSupportBean(epService, "E7", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E7", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E7", 2L});

        sendSupportBean(epService, "E8", 2L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E8", 2L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E3", 2L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E8", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E3", 2L});
        listenerStmtOne.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTimeBatchPerGroup(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        sendTimer(epService, 0);
        String stmtTextCreate = "create window MyWindowTBPG#groupwin(value)#time_batch(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowTBPG select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindowTBPG";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        sendTimer(epService, 1000);
        sendSupportBean(epService, "E1", 10L);
        sendSupportBean(epService, "E2", 20L);
        sendSupportBean(epService, "E3", 20L);
        sendSupportBean(epService, "E4", 10L);

        sendTimer(epService, 11000);
        assertEquals(listenerWindow.getLastNewData().length, 4);
        assertEquals(listenerStmtOne.getLastNewData().length, 4);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[1], fields, new Object[]{"E4", 10L});
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[2], fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[3], fields, new Object[]{"E3", 20L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[1], fields, new Object[]{"E4", 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[2], fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[3], fields, new Object[]{"E3", 20L});
        listenerStmtOne.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionDoubleInsertSameWindow(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowDISM#keepall as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowDISM select theString as key, longBoxed+1 as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        stmtTextInsert = "insert into MyWindowDISM select theString as key, longBoxed+2 as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindowDISM";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean(epService, "E1", 10L);
        assertEquals(2, listenerWindow.getNewDataList().size());    // listener to window gets 2 individual events
        assertEquals(2, listenerStmtOne.getNewDataList().size());   // listener to statement gets 1 individual event
        assertEquals(2, listenerWindow.getNewDataListFlattened().length);
        assertEquals(2, listenerStmtOne.getNewDataListFlattened().length);
        EPAssertionUtil.assertPropsPerRow(listenerStmtOne.getNewDataListFlattened(), fields, new Object[][]{{"E1", 11L}, {"E1", 12L}});
        listenerStmtOne.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLastEvent(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowLE#lastevent as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowLE select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowLE";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowLE as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E1", 1L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean(epService, "E2", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E2", 2L});
        assertNull(listenerStmtOne.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean(epService, "E3", 3L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E3", 3L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3L}});

        // delete E3
        sendMarketBean(epService, "E3");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E3", 3L});
        assertNull(listenerStmtOne.getLastNewData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean(epService, "E4", 4L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E4", 4L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 4L}});

        // delete other event
        sendMarketBean(epService, "E1");
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFirstEvent(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowFE#firstevent as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowFE select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowFE";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowFE as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E1", 1L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean(epService, "E2", 2L);
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        // delete E2
        sendMarketBean(epService, "E1");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        assertNull(listenerStmtOne.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean(epService, "E3", 3L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E3", 3L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3L}});

        // delete E3
        sendMarketBean(epService, "E2");   // no effect
        sendMarketBean(epService, "E3");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E3", 3L});
        assertNull(listenerStmtOne.getLastNewData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean(epService, "E4", 4L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E4", 4L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 4L}});

        // delete other event
        sendMarketBean(epService, "E1");
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUnique(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowUN#unique(key) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowUN select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowUN";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowUN as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "G1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G1", 1L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean(epService, "G2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G2", 20L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}, {"G2", 20L}});

        // delete G2
        sendMarketBean(epService, "G2");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G2", 20L});
        assertNull(listenerStmtOne.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean(epService, "G1", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G1", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G1", 1L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 2L}});

        sendSupportBean(epService, "G2", 21L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G2", 21L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 2L}, {"G2", 21L}});

        sendSupportBean(epService, "G2", 22L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G2", 22L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G2", 21L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 2L}, {"G2", 22L}});

        sendMarketBean(epService, "G1");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G1", 2L});
        assertNull(listenerStmtOne.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G2", 22L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFirstUnique(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowFU#firstunique(key) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowFU select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowFU";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowFU as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "G1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean(epService, "G2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G2", 20L});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}, {"G2", 20L}});

        // delete G2
        sendMarketBean(epService, "G2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"G2", 20L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean(epService, "G1", 2L);  // ignored
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean(epService, "G2", 21L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G2", 21L});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}, {"G2", 21L}});

        sendSupportBean(epService, "G2", 22L); // ignored
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}, {"G2", 21L}});

        sendMarketBean(epService, "G1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"G1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G2", 21L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilteringConsumer(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowFC#unique(key) as select theString as key, intPrimitive as value from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowFC select theString as key, intPrimitive as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowFC(value > 0, value < 10)";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowFC as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBeanInt(epService, "G1", 5);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G1", 5});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"G1", 5});

        sendSupportBeanInt(epService, "G1", 15);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G1", 5});
        assertNull(listenerStmtOne.getLastNewData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"G1", 5});
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"G1", 15});
        listenerWindow.reset();

        // send G2
        sendSupportBeanInt(epService, "G2", 8);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G2", 8});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"G2", 8});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 15}, {"G2", 8}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{"G2", 8}});

        // delete G2
        sendMarketBean(epService, "G2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"G2", 8});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"G2", 8});

        // send G3
        sendSupportBeanInt(epService, "G3", -1);
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"G3", -1});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 15}, {"G3", -1}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, null);

        // delete G2
        sendMarketBean(epService, "G3");
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"G3", -1});

        sendSupportBeanInt(epService, "G1", 6);
        sendSupportBeanInt(epService, "G2", 7);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectOne.iterator(), fields, new Object[][]{{"G1", 6}, {"G2", 7}});

        stmtSelectOne.destroy();
        stmtDelete.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSelectGroupedViewLateStart(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowSGVS#groupwin(theString, intPrimitive)#length(9) as select theString, intPrimitive from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create insert into
        String stmtTextInsert = "insert into MyWindowSGVS select theString, intPrimitive from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // fill window
        String[] stringValues = new String[]{"c0", "c1", "c2"};
        for (int i = 0; i < stringValues.length; i++) {
            for (int j = 0; j < 3; j++) {
                epService.getEPRuntime().sendEvent(new SupportBean(stringValues[i], j));
            }
        }
        epService.getEPRuntime().sendEvent(new SupportBean("c0", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("c1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("c3", 3));
        EventBean[] received = EPAssertionUtil.iteratorToArray(stmtCreate.iterator());
        assertEquals(12, received.length);

        // create select stmt
        String stmtTextSelect = "select theString, intPrimitive, count(*) from MyWindowSGVS group by theString, intPrimitive order by theString, intPrimitive";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        received = EPAssertionUtil.iteratorToArray(stmtSelect.iterator());
        assertEquals(10, received.length);

        EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive,count(*)".split(","),
                new Object[][]{
                        {"c0", 0, 1L},
                        {"c0", 1, 2L},
                        {"c0", 2, 1L},
                        {"c1", 0, 1L},
                        {"c1", 1, 1L},
                        {"c1", 2, 2L},
                        {"c2", 0, 1L},
                        {"c2", 1, 1L},
                        {"c2", 2, 1L},
                        {"c3", 3, 1L},
                });

        stmtSelect.destroy();
        stmtCreate.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSelectGroupedViewLateStartVariableIterate(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowSGVLS#groupwin(theString, intPrimitive)#length(9) as select theString, intPrimitive, longPrimitive, boolPrimitive from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create insert into
        String stmtTextInsert = "insert into MyWindowSGVLS select theString, intPrimitive, longPrimitive, boolPrimitive from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create variable
        epService.getEPAdministrator().createEPL("create variable string var_1_1_1");
        epService.getEPAdministrator().createEPL("on " + SupportVariableSetEvent.class.getName() + "(variableName='var_1_1_1') set var_1_1_1 = value");

        // fill window
        String[] stringValues = new String[]{"c0", "c1", "c2"};
        for (int i = 0; i < stringValues.length; i++) {
            for (int j = 0; j < 3; j++) {
                SupportBean bean = new SupportBean(stringValues[i], j);
                bean.setLongPrimitive(j);
                bean.setBoolPrimitive(true);
                epService.getEPRuntime().sendEvent(bean);
            }
        }
        // extra record to create non-uniform data
        SupportBean bean = new SupportBean("c1", 1);
        bean.setLongPrimitive(10);
        bean.setBoolPrimitive(true);
        epService.getEPRuntime().sendEvent(bean);
        EventBean[] received = EPAssertionUtil.iteratorToArray(stmtCreate.iterator());
        assertEquals(10, received.length);

        // create select stmt
        String stmtTextSelect = "select theString, intPrimitive, avg(longPrimitive) as avgLong, count(boolPrimitive) as cntBool" +
                " from MyWindowSGVLS group by theString, intPrimitive having theString = var_1_1_1 order by theString, intPrimitive";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);

        // set variable to C0
        epService.getEPRuntime().sendEvent(new SupportVariableSetEvent("var_1_1_1", "c0"));

        // get iterator results
        received = EPAssertionUtil.iteratorToArray(stmtSelect.iterator());
        assertEquals(3, received.length);
        EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive,avgLong,cntBool".split(","),
                new Object[][]{
                        {"c0", 0, 0.0, 1L},
                        {"c0", 1, 1.0, 1L},
                        {"c0", 2, 2.0, 1L},
                });

        // set variable to C1
        epService.getEPRuntime().sendEvent(new SupportVariableSetEvent("var_1_1_1", "c1"));

        received = EPAssertionUtil.iteratorToArray(stmtSelect.iterator());
        assertEquals(3, received.length);
        EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive,avgLong,cntBool".split(","),
                new Object[][]{
                        {"c1", 0, 0.0, 1L},
                        {"c1", 1, 5.5, 2L},
                        {"c1", 2, 2.0, 1L},
                });

        stmtSelect.destroy();
        stmtCreate.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilteringConsumerLateStart(EPServiceProvider epService) {
        String[] fields = new String[]{"sumvalue"};

        // create window
        String stmtTextCreate = "create window MyWindowFCLS#keepall as select theString as key, intPrimitive as value from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowFCLS select theString as key, intPrimitive as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        sendSupportBeanInt(epService, "G1", 5);
        sendSupportBeanInt(epService, "G2", 15);
        sendSupportBeanInt(epService, "G3", 2);

        // create consumer
        String stmtTextSelectOne = "select irstream sum(value) as sumvalue from MyWindowFCLS(value > 0, value < 10)";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{7}});

        sendSupportBeanInt(epService, "G4", 1);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{8});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{7});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{8}});

        sendSupportBeanInt(epService, "G5", 20);
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{8}});

        sendSupportBeanInt(epService, "G6", 9);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{17});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{8});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{17}});

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowFCLS as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendMarketBean(epService, "G4");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{16});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{17});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{16}});

        sendMarketBean(epService, "G5");
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{16}});

        stmtSelectOne.destroy();
        stmtDelete.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        assertEquals("Error starting statement: Named windows require one or more child views that are data window views [create window MyWindowI1#groupwin(value)#uni(value) as MyMap]",
                tryInvalid(epService, "create window MyWindowI1#groupwin(value)#uni(value) as MyMap"));

        assertEquals("Named windows require one or more child views that are data window views [create window MyWindowI2 as MyMap]",
                tryInvalid(epService, "create window MyWindowI2 as MyMap"));

        assertEquals("Named window or table 'dummy' has not been declared [on MyMap delete from dummy]",
                tryInvalid(epService, "on MyMap delete from dummy"));

        epService.getEPAdministrator().createEPL("create window SomeWindow#keepall as (a int)");
        SupportMessageAssertUtil.tryInvalid(epService, "update SomeWindow set a = 'a' where a = 'b'",
                "Provided EPL expression is an on-demand query expression (not a continuous query), please use the runtime executeQuery API instead");
        SupportMessageAssertUtil.tryInvalidExecuteQuery(epService, "update istream SomeWindow set a = 'a' where a = 'b'",
                "Provided EPL expression is a continuous query expression (not an on-demand query), please use the administrator createEPL API instead");

        // test model-after with no field
        Map<String, Object> innerType = new HashMap<String, Object>();
        innerType.put("key", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("InnerMap", innerType);
        Map<String, Object> outerType = new HashMap<String, Object>();
        outerType.put("innermap", "InnerMap");
        epService.getEPAdministrator().getConfiguration().addEventType("OuterMap", outerType);
        try {
            epService.getEPAdministrator().createEPL("create window MyWindowI3#keepall as select innermap.abc from OuterMap");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Failed to validate select-clause expression 'innermap.abc': Failed to resolve property 'innermap.abc' to a stream or nested property in a stream [create window MyWindowI3#keepall as select innermap.abc from OuterMap]", ex.getMessage());
        }
    }

    private void runAssertionAlreadyExists(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindowAE#keepall as MyMap");
        try {
            epService.getEPAdministrator().createEPL("create window MyWindowAE#keepall as MyMap");
            fail();
        } catch (EPException ex) {
            assertEquals("Error starting statement: A named window by name 'MyWindowAE' has already been created [create window MyWindowAE#keepall as MyMap]", ex.getMessage());
        }
    }

    private void runAssertionConsumerDataWindow(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindowCDW#keepall as MyMap");
        try {
            epService.getEPAdministrator().createEPL("select key, value as value from MyWindowCDW#time(10 sec)");
            fail();
        } catch (EPException ex) {
            assertEquals("Error starting statement: Consuming statements to a named window cannot declare a data window view onto the named window [select key, value as value from MyWindowCDW#time(10 sec)]", ex.getMessage());
        }
    }

    private String tryInvalid(EPServiceProvider epService, String expression) {
        try {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        } catch (EPException ex) {
            return ex.getMessage();
        }
        return null;
    }

    private void runAssertionPriorStats(EPServiceProvider epService) {
        String[] fieldsPrior = new String[]{"priorKeyOne", "priorKeyTwo"};
        String[] fieldsStat = new String[]{"average"};

        String stmtTextCreate = "create window MyWindowPS#keepall as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("key"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("value"));

        String stmtTextInsert = "insert into MyWindowPS select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(stmtTextInsert);

        String stmtTextSelectOne = "select prior(1, key) as priorKeyOne, prior(2, key) as priorKeyTwo from MyWindowPS";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        String stmtTextSelectThree = "select average from MyWindowPS#uni(value)";
        EPStatement stmtSelectThree = epService.getEPAdministrator().createEPL(stmtTextSelectThree);
        SupportUpdateListener listenerStmtThree = new SupportUpdateListener();
        stmtSelectThree.addListener(listenerStmtThree);

        // send events
        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsPrior, new Object[]{null, null});
        EPAssertionUtil.assertProps(listenerStmtThree.getLastNewData()[0], fieldsStat, new Object[]{1d});
        EPAssertionUtil.assertPropsPerRow(stmtSelectThree.iterator(), fieldsStat, new Object[][]{{1d}});

        sendSupportBean(epService, "E2", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"E1", null});
        EPAssertionUtil.assertProps(listenerStmtThree.getLastNewData()[0], fieldsStat, new Object[]{1.5d});
        EPAssertionUtil.assertPropsPerRow(stmtSelectThree.iterator(), fieldsStat, new Object[][]{{1.5d}});

        sendSupportBean(epService, "E3", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"E2", "E1"});
        EPAssertionUtil.assertProps(listenerStmtThree.getLastNewData()[0], fieldsStat, new Object[]{5 / 3d});
        EPAssertionUtil.assertPropsPerRow(stmtSelectThree.iterator(), fieldsStat, new Object[][]{{5 / 3d}});

        sendSupportBean(epService, "E4", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"E3", "E2"});
        EPAssertionUtil.assertProps(listenerStmtThree.getLastNewData()[0], fieldsStat, new Object[]{1.75});
        EPAssertionUtil.assertPropsPerRow(stmtSelectThree.iterator(), fieldsStat, new Object[][]{{1.75d}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLateConsumer(EPServiceProvider epService) {
        String[] fieldsWin = new String[]{"key", "value"};
        String[] fieldsStat = new String[]{"average"};
        String[] fieldsCnt = new String[]{"cnt"};

        String stmtTextCreate = "create window MyWindowLCL#keepall as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("key"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("value"));

        String stmtTextInsert = "insert into MyWindowLCL select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // send events
        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E1", 1L});

        sendSupportBean(epService, "E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E2", 2L});

        String stmtTextSelectOne = "select irstream average from MyWindowLCL#uni(value)";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{1.5d}});

        sendSupportBean(epService, "E3", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fieldsStat, new Object[]{5 / 3d});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fieldsStat, new Object[]{3 / 2d});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{5 / 3d}});

        sendSupportBean(epService, "E4", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fieldsStat, new Object[]{7 / 4d});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{7 / 4d}});

        String stmtTextSelectTwo = "select count(*) as cnt from MyWindowLCL";
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectTwo);
        SupportUpdateListener listenerStmtTwo = new SupportUpdateListener();
        stmtSelectTwo.addListener(listenerStmtTwo);
        EPAssertionUtil.assertPropsPerRow(stmtSelectTwo.iterator(), fieldsCnt, new Object[][]{{4L}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{7 / 4d}});

        sendSupportBean(epService, "E5", 3L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fieldsStat, new Object[]{10 / 5d});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fieldsStat, new Object[]{7 / 4d});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{10 / 5d}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectTwo.iterator(), fieldsCnt, new Object[][]{{5L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLateConsumerJoin(EPServiceProvider epService) {
        String[] fieldsWin = new String[]{"key", "value"};
        String[] fieldsJoin = new String[]{"key", "value", "symbol"};

        String stmtTextCreate = "create window MyWindowLCJ#keepall as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("key"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("value"));

        String stmtTextInsert = "insert into MyWindowLCJ select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // send events
        sendSupportBean(epService, "E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E1", 1L});

        sendSupportBean(epService, "E2", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E2", 1L});

        // This replays into MyWindow
        String stmtTextSelectTwo = "select key, value, symbol from MyWindowLCJ as s0" +
                " left outer join " + SupportMarketDataBean.class.getName() + "#keepall as s1" +
                " on s0.value = s1.volume";
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectTwo);
        SupportUpdateListener listenerStmtTwo = new SupportUpdateListener();
        stmtSelectTwo.addListener(listenerStmtTwo);
        assertFalse(listenerStmtTwo.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectTwo.iterator(), fieldsJoin, new Object[][]{{"E1", 1L, null}, {"E2", 1L, null}});

        sendMarketBean(epService, "S1", 1);    // join on long
        assertEquals(2, listenerStmtTwo.getLastNewData().length);
        if (listenerStmtTwo.getLastNewData()[0].get("key").equals("E1")) {
            EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fieldsJoin, new Object[]{"E1", 1L, "S1"});
            EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[1], fieldsJoin, new Object[]{"E2", 1L, "S1"});
        } else {
            EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fieldsJoin, new Object[]{"E2", 1L, "S1"});
            EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[1], fieldsJoin, new Object[]{"E1", 1L, "S1"});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectTwo.iterator(), fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}});
        listenerStmtTwo.reset();

        sendMarketBean(epService, "S2", 2);    // join on long
        assertFalse(listenerStmtTwo.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectTwo.iterator(), fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}});

        sendSupportBean(epService, "E3", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E3", 2L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fieldsJoin, new Object[]{"E3", 2L, "S2"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectTwo.iterator(), fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}, {"E3", 2L, "S2"}});
    }

    private void runAssertionPattern(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};
        String stmtTextCreate = "create window MyWindowPAT#keepall as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        String stmtTextPattern = "select a.key as key, a.value as value from pattern [every a=MyWindowPAT(key='S1') or a=MyWindowPAT(key='S2')]";
        EPStatement stmtPattern = epService.getEPAdministrator().createEPL(stmtTextPattern);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtPattern.addListener(listenerStmtOne);

        String stmtTextInsert = "insert into MyWindowPAT select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        sendSupportBean(epService, "E1", 1L);
        assertFalse(listenerStmtOne.isInvoked());

        sendSupportBean(epService, "S1", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 2L});

        sendSupportBean(epService, "S1", 3L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 3L});

        sendSupportBean(epService, "S2", 4L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S2", 4L});

        sendSupportBean(epService, "S1", 1L);
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionExternallyTimedBatch(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindowETB#ext_timed_batch(value, 10 sec, 0L) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindowETB select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindowETB";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowETB as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1000L);
        sendSupportBean(epService, "E2", 8000L);
        sendSupportBean(epService, "E3", 9999L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1000L}, {"E2", 8000L}, {"E3", 9999L}});

        // delete E2
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertPropsPerRow(listenerWindow.assertInvokedAndReset(), fields, null, new Object[][]{{"E2", 8000L}});
        EPAssertionUtil.assertPropsPerRow(listenerStmtOne.assertInvokedAndReset(), fields, null, new Object[][]{{"E2", 8000L}});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1000L}, {"E3", 9999L}});

        sendSupportBean(epService, "E4", 10000L);
        EPAssertionUtil.assertPropsPerRow(listenerWindow.assertInvokedAndReset(), fields,
                new Object[][]{{"E1", 1000L}, {"E3", 9999L}}, null);
        EPAssertionUtil.assertPropsPerRow(listenerStmtOne.assertInvokedAndReset(), fields,
                new Object[][]{{"E1", 1000L}, {"E3", 9999L}}, null);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 10000L}});

        // delete E4
        sendMarketBean(epService, "E4");
        EPAssertionUtil.assertPropsPerRow(listenerWindow.assertInvokedAndReset(), fields, null, new Object[][]{{"E4", 10000L}});
        EPAssertionUtil.assertPropsPerRow(listenerStmtOne.assertInvokedAndReset(), fields, null, new Object[][]{{"E4", 10000L}});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean(epService, "E5", 14000L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E5", 14000L}});

        sendSupportBean(epService, "E6", 21000L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E6", 21000L}});
        EPAssertionUtil.assertPropsPerRow(listenerWindow.assertInvokedAndReset(), fields,
                new Object[][]{{"E5", 14000L}}, new Object[][]{{"E1", 1000L}, {"E3", 9999L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportBean sendSupportBean(EPServiceProvider epService, String theString, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendSupportBeanInt(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(EPServiceProvider epService, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(EPServiceProvider epService, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    public static class MyAvroTypeWidenerFactory implements ObjectValueTypeWidenerFactory {
        public TypeWidener make(ObjectValueTypeWidenerFactoryContext context) {
            if (context.getClazz() == SupportBean_S0.class) {
                return new TypeWidener() {
                    public Object widen(Object val) {
                        GenericData.Record row = new GenericData.Record(getSupportBeanS0Schema());
                        row.put("p00", ((SupportBean_S0) val).getP00());
                        return row;
                    }

                    public CodegenExpression widenCodegen(CodegenExpression expression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
                        throw new UnsupportedOperationException("not yet implemented");
                    }
                };
            }
            return null;
        }
    }

    public static class MyAvroTypeRepMapper implements TypeRepresentationMapper {
        public Object map(TypeRepresentationMapperContext context) {
            if (context.getClazz() == SupportBean_S0.class) {
                return getSupportBeanS0Schema();
            }
            return null;
        }
    }

    public static Schema getSupportBeanS0Schema() {
        return record("SupportBean_S0").fields().requiredString("p00").endRecord();
    }
}
