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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestNamedWindowViews extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listenerWindow;
    private SupportUpdateListener listenerStmtOne;
    private SupportUpdateListener listenerStmtTwo;
    private SupportUpdateListener listenerStmtThree;
    private SupportUpdateListener listenerStmtDelete;

    public void setUp()
    {
        Map<String, Object> types = new HashMap<String, Object>();
        types.put("key", String.class);
        types.put("value", long.class);

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyMap", types);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listenerWindow = new SupportUpdateListener();
        listenerStmtOne = new SupportUpdateListener();
        listenerStmtTwo = new SupportUpdateListener();
        listenerStmtThree = new SupportUpdateListener();
        listenerStmtDelete = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerWindow = null;
        listenerStmtOne = null;
        listenerStmtTwo = null;
        listenerStmtThree = null;
        listenerStmtDelete = null;
    }

    public void testBeanBacked()
    {
        runAssertionBeanBacked(EventRepresentationEnum.OBJECTARRAY);
        runAssertionBeanBacked(EventRepresentationEnum.MAP);
        runAssertionBeanBacked(EventRepresentationEnum.DEFAULT);
    }

    public void testIntersection() throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(
                "create window MyWindow.win:length(2).std:unique(intPrimitive) as SupportBean;\n" +
                        "insert into MyWindow select * from SupportBean;\n" +
                        "@name('out') select irstream * from MyWindow");

        String[] fields = "theString".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields, new Object[][] {{"E1"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields, new Object[][]{{"E2"}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.assertInvokedAndReset(), fields, new Object[][]{{"E3"}}, new Object[][]{{"E1"}, {"E2"}});
    }

    private void runAssertionBeanBacked(EventRepresentationEnum eventRepresentationEnum) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        // Test create from class
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window MyWindow.win:keepall() as SupportBean");
        stmt.addListener(listenerWindow);
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");

        EPStatementSPI stmtConsume = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from MyWindow");
        assertTrue(stmtConsume.getStatementContext().isStatelessSelect());
        stmtConsume.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEvent(listenerWindow.assertOneGetNewAndReset());
        assertEvent(listenerStmtOne.assertOneGetNewAndReset());

        EPStatement stmtUpdate = epService.getEPAdministrator().createEPL("on SupportBean_A update MyWindow set theString='s'");
        stmtUpdate.addListener(listenerStmtTwo);
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        assertEvent(listenerStmtTwo.getLastNewData()[0]);
        
        // test bean-property
        epService.getEPAdministrator().destroyAllStatements();
        listenerWindow.reset();

        EPStatement stmtW = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window MyWindowTwo.win:keepall() as (bean " + SupportBean_S0.class.getName() + ")");
        stmtW.addListener(listenerWindow);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmtW.getEventType().getUnderlyingType());
        epService.getEPAdministrator().createEPL("insert into MyWindowTwo select bean.* as bean from " + SupportBean_S0.class.getName() + " as bean");
        
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), "bean.p00".split(","), new Object[] {"E1"});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowOne", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowTwo", true);
        listenerStmtOne.reset();
    }

    public void testBeanSchemaBacked() {

        // Test create from schema
        epService.getEPAdministrator().createEPL("create schema ABC as " + SupportBean.class.getName());
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as ABC");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from " + SupportBean.class.getName());

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEvent(epService.getEPRuntime().executeQuery("select * from MyWindow").getArray()[0]);

        EPStatement stmtABC = epService.getEPAdministrator().createEPL("select * from ABC");
        stmtABC.addListener(listenerStmtOne);
        
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(listenerStmtOne.isInvoked());
    }

    private void assertEvent(EventBean theEvent) {
        assertTrue(theEvent.getEventType() instanceof BeanEventType);
        assertTrue(theEvent.getUnderlying() instanceof SupportBean);
        assertEquals(EventTypeMetadata.TypeClass.NAMED_WINDOW, ((EventTypeSPI) theEvent.getEventType()).getMetadata().getTypeClass());
        assertEquals("MyWindow", theEvent.getEventType().getName());
    }

    public void testDeepSupertypeInsert() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportOverrideOneA", SupportOverrideOneA.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportOverrideOne", SupportOverrideOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportOverrideBase", SupportOverrideBase.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as select * from SupportOverrideBase");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportOverrideOneA");
        epService.getEPRuntime().sendEvent(new SupportOverrideOneA("1a", "1", "base"));
        assertEquals("1a", stmt.iterator().next().get("val"));
    }

    // Assert the named window is updated at the time that a subsequent event queries the named window
    public void testOnInsertPremptiveTwoWindow()
    {
        epService.getEPAdministrator().createEPL("create schema TypeOne(col1 int)");
        epService.getEPAdministrator().createEPL("create schema TypeTwo(col2 int)");
        epService.getEPAdministrator().createEPL("create schema TypeTrigger(trigger int)");
        epService.getEPAdministrator().createEPL("create schema SupportBean as " + SupportBean.class.getName());

        epService.getEPAdministrator().createEPL("create window WinOne.win:keepall() as TypeOne");
        epService.getEPAdministrator().createEPL("create window WinTwo.win:keepall() as TypeTwo");

        epService.getEPAdministrator().createEPL("insert into WinOne(col1) select intPrimitive from SupportBean");

        epService.getEPAdministrator().createEPL("on TypeTrigger insert into OtherStream select col1 from WinOne");
        epService.getEPAdministrator().createEPL("on TypeTrigger insert into WinTwo(col2) select col1 from WinOne");
        EPStatement stmt = epService.getEPAdministrator().createEPL("on OtherStream select col2 from WinTwo");
        stmt.addListener(listenerStmtOne);

        // populate WinOne
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 9));

        // fire trigger
        if (EventRepresentationEnum.getEngineDefault(epService).isObjectArrayEvent()) {
            epService.getEPRuntime().getEventSender("TypeTrigger").sendEvent(new Object[0]);
        }
        else {
            epService.getEPRuntime().getEventSender("TypeTrigger").sendEvent(new HashMap());
        }

        assertEquals(9, listenerStmtOne.assertOneGetNewAndReset().get("col2"));
    }

    public void testWithDeleteUseAs()
    {
        tryCreateWindow("create window MyWindow.win:keepall() as MyMap",
                        "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key");
    }

    public void testWithDeleteFirstAs()
    {
        tryCreateWindow("create window MyWindow.win:keepall() as select key, value from MyMap",
                        "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow as s1 where symbol = s1.key");
    }

    public void testWithDeleteSecondAs()
    {
        tryCreateWindow("create window MyWindow.win:keepall() as MyMap",
                        "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow where s0.symbol = key");
    }

    public void testWithDeleteNoAs()
    {
        tryCreateWindow("create window MyWindow.win:keepall() as select key as key, value as value from MyMap",
                        "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where symbol = key");
    }

    private void tryCreateWindow(String createWindowStatement, String deleteStatement)
    {
        String[] fields = new String[] {"key", "value"};
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(createWindowStatement);
        stmtCreate.addListener(listenerWindow);

        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("key"));
        assertEquals(long.class, stmtCreate.getEventType().getPropertyType("value"));

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(stmtTextInsert);

        String stmtTextSelectOne = "select irstream key, value*2 as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        String stmtTextSelectTwo = "select irstream key, sum(value) as value from MyWindow group by key";
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectTwo);
        stmtSelectTwo.addListener(listenerStmtTwo);

        String stmtTextSelectThree = "select irstream key, value from MyWindow where value >= 10";
        EPStatement stmtSelectThree = epService.getEPAdministrator().createEPL(stmtTextSelectThree);
        stmtSelectThree.addListener(listenerStmtThree);

        // send events
        sendSupportBean("E1", 10L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E1", null});
        listenerStmtTwo.reset();
        EPAssertionUtil.assertProps(listenerStmtThree.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{"E1", 20L}});

        sendSupportBean("E2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E2", null});
        listenerStmtTwo.reset();
        EPAssertionUtil.assertProps(listenerStmtThree.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E2", 20L}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{"E1", 20L}, {"E2", 40L}});

        sendSupportBean("E3", 5L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 10L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E3", 5L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E3", null});
        listenerStmtTwo.reset();
        assertFalse(listenerStmtThree.isInvoked());
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 5L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E2", 20L}, {"E3", 5L}});

        // create delete stmt
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(deleteStatement);
        stmtDelete.addListener(listenerStmtDelete);

        // send delete event
        sendMarketBean("E1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E1", null});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E1", 10l});
        listenerStmtTwo.reset();
        EPAssertionUtil.assertProps(listenerStmtThree.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 20L}, {"E3", 5L}});

        // send delete event again, none deleted now
        sendMarketBean("E1");
        assertFalse(listenerStmtOne.isInvoked());
        assertFalse(listenerStmtTwo.isInvoked());
        assertFalse(listenerWindow.isInvoked());
        assertTrue(listenerStmtDelete.isInvoked());
        listenerStmtDelete.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 20L}, {"E3", 5L}});

        // send delete event
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fields, new Object[]{"E2", null});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastOldData()[0], fields, new Object[]{"E2", 20L});
        listenerStmtTwo.reset();
        EPAssertionUtil.assertProps(listenerStmtThree.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 5L}});

        // send delete event
        sendMarketBean("E3");
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

    public void testTimeWindow()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:time(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(1000);
        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendTimer(5000);
        sendSupportBean("E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        sendTimer(10000);
        sendSupportBean("E3", 3L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // Should push out the window
        sendTimer(10999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        sendTimer(11000);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2L}, {"E3", 3L}});

        sendSupportBean("E4", 4L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2L}, {"E3", 3L}, {"E4", 4L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3L}, {"E4", 4L}});

        // nothing pushed
        sendTimer(15000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        // push last event
        sendTimer(19999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        sendTimer(20000);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 4L}});

        // delete E4
        sendMarketBean("E4");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(100000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testTimeFirstWindow()
    {
        String[] fields = new String[] {"key", "value"};

        sendTimer(1000);

        // create window
        String stmtTextCreate = "create window MyWindow.win:firsttime(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendTimer(5000);
        sendSupportBean("E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        sendTimer(10000);
        sendSupportBean("E3", 3L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // Should not push out the window
        sendTimer(12000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        sendSupportBean("E4", 4L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        // nothing pushed
        sendTimer(100000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testExtTimeWindow()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:ext_timed(value, 10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1000L}});

        sendSupportBean("E2", 5000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 5000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 5000L});

        sendSupportBean("E3", 10000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 10000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 10000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1000L}, {"E2", 5000L}, {"E3", 10000L}});

        // Should push out the window
        sendSupportBean("E4", 11000L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E4", 11000L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E1", 1000L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E4", 11000L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1000L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 5000L}, {"E3", 10000L}, {"E4", 11000L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 5000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 5000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 10000L}, {"E4", 11000L}});

        // nothing pushed other then E5 (E2 is deleted)
        sendSupportBean("E5", 15000L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E5", 15000L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E5", 15000L});
        assertNull(listenerWindow.getLastOldData());
        assertNull(listenerStmtOne.getLastOldData());
    }

    public void testTimeOrderWindow()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.ext:time_order(value, 10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(5000);
        sendSupportBean("E1", 3000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 3000L});

        sendTimer(6000);
        sendSupportBean("E2", 2000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2000L});

        sendTimer(10000);
        sendSupportBean("E3", 1000L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 1000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 1000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 1000L}, {"E2", 2000L}, {"E1", 3000L}});

        // Should push out the window
        sendTimer(11000);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3", 1000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E3", 1000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2000L}, {"E1", 3000L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 3000L}});

        sendTimer(12999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(13000);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 3000L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 3000L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(100000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testLengthWindow()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:length(3) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        sendSupportBean("E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

        sendSupportBean("E3", 3L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        sendSupportBean("E4", 4L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}, {"E4", 4L}});

        sendSupportBean("E5", 5L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E5", 5L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E5", 5L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3L}, {"E4", 4L}, {"E5", 5L}});

        sendSupportBean("E6", 6L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E6", 6L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E3", 3L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E6", 6L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E3", 3L});
        listenerStmtOne.reset();

        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testLengthFirstWindow()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:firstlength(2) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        sendSupportBean("E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

        sendSupportBean("E3", 3L);
        assertFalse(listenerWindow.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean("E4", 4L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E4", 4L}});

        sendSupportBean("E5", 5L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E4", 4L}});
    }

    public void testTimeAccum()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:time_accum(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(1000);
        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        sendTimer(5000);
        sendSupportBean("E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

        sendTimer(10000);
        sendSupportBean("E3", 3L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3L});

        sendTimer(15000);
        sendSupportBean("E4", 4L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 4L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}, {"E4", 4L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}, {"E4", 4L}});

        // nothing pushed
        sendTimer(24999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(25000);
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
        sendMarketBean("E4");
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(30000);
        sendSupportBean("E5", 5L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E5", 5L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E5", 5L});

        sendTimer(31000);
        sendSupportBean("E6", 6L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E6", 6L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E6", 6L});

        sendTimer(38000);
        sendSupportBean("E7", 7L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E5", 5L}, {"E6", 6L}, {"E7", 7L}});

        // delete E7 - deleting the last should spit out the first 2 timely
        sendMarketBean("E7");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E7", 7L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E5", 5L}, {"E6", 6L}});

        sendTimer(40999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(41000);
        assertNull(listenerStmtOne.getLastNewData());
        oldData = listenerStmtOne.getLastOldData();
        assertEquals(2, oldData.length);
        EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E5", 5L});
        EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E6", 6L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(50000);
        sendSupportBean("E8", 8L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E8", 8L}});

        sendTimer(55000);
        sendMarketBean("E8");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E8", 8L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(100000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testTimeBatch()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:time_batch(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(1000);
        sendSupportBean("E1", 1L);

        sendTimer(5000);
        sendSupportBean("E2", 2L);

        sendTimer(10000);
        sendSupportBean("E3", 3L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        // nothing pushed
        sendTimer(10999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(11000);
        assertNull(listenerWindow.getLastOldData());
        EventBean[] newData = listenerWindow.getLastNewData();
        assertEquals(2, newData.length);
        EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(21000);
        assertNull(listenerWindow.getLastNewData());
        EventBean[] oldData = listenerWindow.getLastOldData();
        assertEquals(2, oldData.length);
        EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
        listenerWindow.reset();
        listenerStmtOne.reset();

        // send and delete E4, leaving an empty batch
        sendSupportBean("E4", 4L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 4L}});

        sendMarketBean("E4");
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(31000);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testTimeBatchLateConsumer()
    {
        sendTimer(0);

        // create window
        String stmtTextCreate = "create window MyWindow.win:time_batch(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        sendTimer(0);
        sendSupportBean("E1", 1L);

        sendTimer(5000);
        sendSupportBean("E2", 2L);

        // create consumer
        String stmtTextSelectOne = "select sum(value) as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendTimer(8000);
        sendSupportBean("E3", 3L);
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(10000);
        EventBean[] newData = listenerStmtOne.getLastNewData();
        assertEquals(1, newData.length);
        EPAssertionUtil.assertProps(newData[0], new String[]{"value"}, new Object[]{6L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), new String[]{"value"}, null);
    }

    public void testLengthBatch()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:length_batch(3) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1L);
        sendSupportBean("E2", 2L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean("E3", 3L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        sendSupportBean("E4", 4L);
        assertNull(listenerWindow.getLastOldData());
        EventBean[] newData = listenerWindow.getLastNewData();
        assertEquals(3, newData.length);
        EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(newData[2], fields, new Object[]{"E4", 4L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E5", 5L);
        sendSupportBean("E6", 6L);
        sendMarketBean("E5");
        sendMarketBean("E6");
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E7", 7L);
        sendSupportBean("E8", 8L);
        sendSupportBean("E9", 9L);
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

        sendSupportBean("E10", 10L);
        sendSupportBean("E10", 11L);
        sendMarketBean("E10");

        sendSupportBean("E21", 21L);
        sendSupportBean("E22", 22L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        sendSupportBean("E23", 23L);
        oldData = listenerWindow.getLastOldData();
        newData = listenerWindow.getLastNewData();
        assertEquals(3, newData.length);
        assertEquals(3, oldData.length);
    }

    public void testSortWindow()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.ext:sort(3, value asc) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 10L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});

        sendSupportBean("E2", 20L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});

        sendSupportBean("E3", 15L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 15L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E2", 20L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E3", 15L}});

        sendSupportBean("E4", 18L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 18L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E4", 18L}});

        sendSupportBean("E5", 17L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E5", 17L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E4", 18L});
        listenerWindow.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E5", 17L}});

        // delete E1
        sendMarketBean("E1");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 15L}, {"E5", 17L}});

        sendSupportBean("E6", 16L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E6", 16L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 15L}, {"E6", 16L}, {"E5", 17L}});

        sendSupportBean("E7", 16L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E7", 16L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E5", 17L});
        listenerWindow.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 15L}, {"E7", 16L}, {"E6", 16L}});

        // delete E7 has no effect
        sendMarketBean("E7");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E7", 16L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 15L}, {"E6", 16L}});

        sendSupportBean("E8", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E8", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E8", 1L}, {"E3", 15L}, {"E6", 16L}});

        sendSupportBean("E9", 1L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E9", 1L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E6", 16L});
        listenerWindow.reset();
    }

    public void testTimeLengthBatch()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:time_length_batch(10 sec, 3) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendTimer(1000);
        sendSupportBean("E1", 1L);
        sendSupportBean("E2", 2L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

        // delete E2
        sendMarketBean("E2");
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean("E3", 3L);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

        sendSupportBean("E4", 4L);
        assertNull(listenerWindow.getLastOldData());
        EventBean[] newData = listenerWindow.getLastNewData();
        assertEquals(3, newData.length);
        EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
        EPAssertionUtil.assertProps(newData[2], fields, new Object[]{"E4", 4L});
        listenerWindow.reset();
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendTimer(5000);
        sendSupportBean("E5", 5L);
        sendSupportBean("E6", 6L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E5", 5L}, {"E6", 6L}});

        sendMarketBean("E5");   // deleting E5
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E6", 6L}});

        sendTimer(10999);
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerStmtOne.isInvoked());

        sendTimer(11000);
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
    }

    public void testLengthWindowPerGroup()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.std:groupwin(value).win:length(2) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where symbol = key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

        sendSupportBean("E2", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});

        sendSupportBean("E3", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 2L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E2", 1L}, {"E3", 2L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}, {"E3", 2L}});

        sendSupportBean("E4", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E4", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E4", 1L});

        sendSupportBean("E5", 1L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E5", 1L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E5", 1L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerStmtOne.reset();

        sendSupportBean("E6", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E6", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E6", 2L});

        // delete E6
        sendMarketBean("E6");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E6", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E6", 2L});

        sendSupportBean("E7", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E7", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E7", 2L});

        sendSupportBean("E8", 2L);
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"E8", 2L});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E3", 2L});
        listenerWindow.reset();
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E8", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E3", 2L});
        listenerStmtOne.reset();
    }

    public void testTimeBatchPerGroup()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        sendTimer(0);
        String stmtTextCreate = "create window MyWindow.std:groupwin(value).win:time_batch(10 sec) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendTimer(1000);
        sendSupportBean("E1", 10L);
        sendSupportBean("E2", 20L);
        sendSupportBean("E3", 20L);
        sendSupportBean("E4", 10L);

        sendTimer(11000);
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
    }

    public void testDoubleInsertSameWindow()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed+1 as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        stmtTextInsert = "insert into MyWindow select theString as key, longBoxed+2 as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean("E1", 10L);
        assertEquals(2, listenerWindow.getNewDataList().size());    // listener to window gets 2 individual events
        assertEquals(2, listenerStmtOne.getNewDataList().size());   // listener to statement gets 1 individual event
        assertEquals(2, listenerWindow.getNewDataListFlattened().length);
        assertEquals(2, listenerStmtOne.getNewDataListFlattened().length);
        EPAssertionUtil.assertPropsPerRow(listenerStmtOne.getNewDataListFlattened(), fields, new Object[][]{{"E1", 11L}, {"E1", 12L}});
        listenerStmtOne.reset();
    }

    public void testLastEvent()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.std:lastevent() as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E1", 1L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean("E2", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E2", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E2", 2L});
        assertNull(listenerStmtOne.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E3", 3L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E3", 3L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3L}});

        // delete E3
        sendMarketBean("E3");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E3", 3L});
        assertNull(listenerStmtOne.getLastNewData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E4", 4L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E4", 4L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 4L}});

        // delete other event
        sendMarketBean("E1");
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testFirstEvent()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.std:firstevent() as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E1", 1L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        sendSupportBean("E2", 2L);
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1L}});

        // delete E2
        sendMarketBean("E1");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E1", 1L});
        assertNull(listenerStmtOne.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E3", 3L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E3", 3L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 3L}});

        // delete E3
        sendMarketBean("E2");   // no effect
        sendMarketBean("E3");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"E3", 3L});
        assertNull(listenerStmtOne.getLastNewData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E4", 4L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"E4", 4L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 4L}});

        // delete other event
        sendMarketBean("E1");
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testUnique()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.std:unique(key) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("G1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G1", 1L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean("G2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G2", 20L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}, {"G2", 20L}});

        // delete G2
        sendMarketBean("G2");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G2", 20L});
        assertNull(listenerStmtOne.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean("G1", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G1", 2L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G1", 1L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 2L}});

        sendSupportBean("G2", 21L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G2", 21L});
        assertNull(listenerStmtOne.getLastOldData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 2L}, {"G2", 21L}});

        sendSupportBean("G2", 22L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{"G2", 22L});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G2", 21L});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 2L}, {"G2", 22L}});

        sendMarketBean("G1");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G1", 2L});
        assertNull(listenerStmtOne.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G2", 22L}});
    }

    public void testFirstUnique()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.std:firstunique(key) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("G1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean("G2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G2", 20L});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}, {"G2", 20L}});

        // delete G2
        sendMarketBean("G2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"G2", 20L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean("G1", 2L);  // ignored
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}});

        sendSupportBean("G2", 21L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G2", 21L});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}, {"G2", 21L}});

        sendSupportBean("G2", 22L); // ignored
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 1L}, {"G2", 21L}});

        sendMarketBean("G1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"G1", 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"G2", 21L}});
    }

    public void testFilteringConsumer()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.std:unique(key) as select theString as key, intPrimitive as value from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, intPrimitive as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow(value > 0, value < 10)";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBeanInt("G1", 5);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G1", 5});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"G1", 5});

        sendSupportBeanInt("G1", 15);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{"G1", 5});
        assertNull(listenerStmtOne.getLastNewData());
        listenerStmtOne.reset();
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"G1", 5});
        EPAssertionUtil.assertProps(listenerWindow.getLastNewData()[0], fields, new Object[]{"G1", 15});
        listenerWindow.reset();

        // send G2
        sendSupportBeanInt("G2", 8);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"G2", 8});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"G2", 8});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 15}, {"G2", 8}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{"G2", 8}});

        // delete G2
        sendMarketBean("G2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"G2", 8});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"G2", 8});

        // send G3
        sendSupportBeanInt("G3", -1);
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"G3", -1});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"G1", 15}, {"G3", -1}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, null);

        // delete G2
        sendMarketBean("G3");
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"G3", -1});

        sendSupportBeanInt("G1", 6);
        sendSupportBeanInt("G2", 7);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectOne.iterator(), fields, new Object[][]{{"G1", 6}, {"G2", 7}});

        stmtSelectOne.destroy();
        stmtDelete.destroy();
    }

    public void testSelectGroupedViewLateStart()
    {
        // create window
        String stmtTextCreate = "create window MyWindow.std:groupwin(theString, intPrimitive).win:length(9) as select theString, intPrimitive from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString, intPrimitive from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // fill window
        String[] stringValues = new String[] {"c0", "c1", "c2"};
        for (int i = 0; i < stringValues.length; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                epService.getEPRuntime().sendEvent(new SupportBean(stringValues[i], j));
            }
        }
        epService.getEPRuntime().sendEvent(new SupportBean("c0", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("c1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("c3", 3));
        EventBean[] received = EPAssertionUtil.iteratorToArray(stmtCreate.iterator());
        assertEquals(12, received.length);

        // create select stmt
        String stmtTextSelect = "select theString, intPrimitive, count(*) from MyWindow group by theString, intPrimitive order by theString, intPrimitive";
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
    }

    public void testSelectGroupedViewLateStartVariableIterate()
    {
        // create window
        String stmtTextCreate = "create window MyWindow.std:groupwin(theString, intPrimitive).win:length(9) as select theString, intPrimitive, longPrimitive, boolPrimitive from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString, intPrimitive, longPrimitive, boolPrimitive from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create variable
        epService.getEPAdministrator().createEPL("create variable string var_1_1_1");
        epService.getEPAdministrator().createEPL("on " + SupportVariableSetEvent.class.getName() + "(variableName='var_1_1_1') set var_1_1_1 = value");

        // fill window
        String[] stringValues = new String[] {"c0", "c1", "c2"};
        for (int i = 0; i < stringValues.length; i++)
        {
            for (int j = 0; j < 3; j++)
            {
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
                                " from MyWindow group by theString, intPrimitive having theString = var_1_1_1 order by theString, intPrimitive";
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
    }

    public void testFilteringConsumerLateStart()
    {
        String[] fields = new String[] {"sumvalue"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as select theString as key, intPrimitive as value from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, intPrimitive as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        sendSupportBeanInt("G1", 5);
        sendSupportBeanInt("G2", 15);
        sendSupportBeanInt("G3", 2);

        // create consumer
        String stmtTextSelectOne = "select irstream sum(value) as sumvalue from MyWindow(value > 0, value < 10)";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{7}});

        sendSupportBeanInt("G4", 1);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{8});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{7});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{8}});

        sendSupportBeanInt("G5", 20);
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{8}});

        sendSupportBeanInt("G6", 9);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{17});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{8});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{17}});

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendMarketBean("G4");
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fields, new Object[]{16});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fields, new Object[]{17});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{16}});

        sendMarketBean("G5");
        assertFalse(listenerStmtOne.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fields, new Object[][]{{16}});

        stmtSelectOne.destroy();
        stmtDelete.destroy();
    }

    public void testInvalid()
    {
        assertEquals("Error starting statement: Named windows require one or more child views that are data window views [create window MyWindow.std:groupwin(value).stat:uni(value) as MyMap]",
                     tryInvalid("create window MyWindow.std:groupwin(value).stat:uni(value) as MyMap"));

        assertEquals("Named windows require one or more child views that are data window views [create window MyWindow as MyMap]",
                     tryInvalid("create window MyWindow as MyMap"));

        assertEquals("Named window or table 'dummy' has not been declared [on MyMap delete from dummy]",
                     tryInvalid("on MyMap delete from dummy"));

        epService.getEPAdministrator().createEPL("create window SomeWindow.win:keepall() as (a int)");
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
            epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as select innermap.abc from OuterMap");
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Failed to validate select-clause expression 'innermap.abc': Failed to resolve property 'innermap.abc' to a stream or nested property in a stream [create window MyWindow.win:keepall() as select innermap.abc from OuterMap]", ex.getMessage());
        }
    }

    public void testAlreadyExists()
    {
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as MyMap");
        try
        {
            epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as MyMap");
            fail();
        }
        catch (EPException ex)
        {
            assertEquals("Error starting statement: A named window by name 'MyWindow' has already been created [create window MyWindow.win:keepall() as MyMap]", ex.getMessage());
        }
    }

    public void testConsumerDataWindow()
    {
        epService.getEPAdministrator().createEPL("create window MyWindow.win:keepall() as MyMap");
        try
        {
            epService.getEPAdministrator().createEPL("select key, value as value from MyWindow.win:time(10 sec)");
            fail();
        }
        catch (EPException ex)
        {
            assertEquals("Error starting statement: Consuming statements to a named window cannot declare a data window view onto the named window [select key, value as value from MyWindow.win:time(10 sec)]", ex.getMessage());
        }
    }

    private String tryInvalid(String expression)
    {
        try
        {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        }
        catch (EPException ex)
        {
            return ex.getMessage();
        }
        return null;
    }

    public void testPriorStats()
    {
        String[] fieldsPrior = new String[] {"priorKeyOne", "priorKeyTwo"};
        String[] fieldsStat = new String[] {"average"};

        String stmtTextCreate = "create window MyWindow.win:keepall() as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("key"));
        assertEquals(long.class, stmtCreate.getEventType().getPropertyType("value"));

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(stmtTextInsert);

        String stmtTextSelectOne = "select prior(1, key) as priorKeyOne, prior(2, key) as priorKeyTwo from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        String stmtTextSelectThree = "select average from MyWindow.stat:uni(value)";
        EPStatement stmtSelectThree = epService.getEPAdministrator().createEPL(stmtTextSelectThree);
        stmtSelectThree.addListener(listenerStmtThree);

        // send events
        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsPrior, new Object[]{null, null});
        EPAssertionUtil.assertProps(listenerStmtThree.getLastNewData()[0], fieldsStat, new Object[]{1d});
        EPAssertionUtil.assertPropsPerRow(stmtSelectThree.iterator(), fieldsStat, new Object[][]{{1d}});

        sendSupportBean("E2", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"E1", null});
        EPAssertionUtil.assertProps(listenerStmtThree.getLastNewData()[0], fieldsStat, new Object[]{1.5d});
        EPAssertionUtil.assertPropsPerRow(stmtSelectThree.iterator(), fieldsStat, new Object[][]{{1.5d}});

        sendSupportBean("E3", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"E2", "E1"});
        EPAssertionUtil.assertProps(listenerStmtThree.getLastNewData()[0], fieldsStat, new Object[]{5 / 3d});
        EPAssertionUtil.assertPropsPerRow(stmtSelectThree.iterator(), fieldsStat, new Object[][]{{5 / 3d}});

        sendSupportBean("E4", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fieldsPrior, new Object[]{"E3", "E2"});
        EPAssertionUtil.assertProps(listenerStmtThree.getLastNewData()[0], fieldsStat, new Object[]{1.75});
        EPAssertionUtil.assertPropsPerRow(stmtSelectThree.iterator(), fieldsStat, new Object[][]{{1.75d}});
    }

    public void testLateConsumer()
    {
        String[] fieldsWin = new String[] {"key", "value"};
        String[] fieldsStat = new String[] {"average"};
        String[] fieldsCnt = new String[] {"cnt"};

        String stmtTextCreate = "create window MyWindow.win:keepall() as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("key"));
        assertEquals(long.class, stmtCreate.getEventType().getPropertyType("value"));

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL(stmtTextInsert);

        // send events
        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E1", 1L});

        sendSupportBean("E2", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E2", 2L});

        String stmtTextSelectOne = "select irstream average from MyWindow.stat:uni(value)";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{1.5d}});

        sendSupportBean("E3", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fieldsStat, new Object[]{5 / 3d});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fieldsStat, new Object[]{3 / 2d});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{5 / 3d}});

        sendSupportBean("E4", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fieldsStat, new Object[]{7 / 4d});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{7 / 4d}});

        String stmtTextSelectTwo = "select count(*) as cnt from MyWindow";
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectTwo);
        stmtSelectTwo.addListener(listenerStmtTwo);
        EPAssertionUtil.assertPropsPerRow(stmtSelectTwo.iterator(), fieldsCnt, new Object[][]{{4L}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{7 / 4d}});

        sendSupportBean("E5", 3L);
        EPAssertionUtil.assertProps(listenerStmtOne.getLastNewData()[0], fieldsStat, new Object[]{10 / 5d});
        EPAssertionUtil.assertProps(listenerStmtOne.getLastOldData()[0], fieldsStat, new Object[]{7 / 4d});
        listenerStmtOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtSelectOne.iterator(), fieldsStat, new Object[][]{{10 / 5d}});
        EPAssertionUtil.assertPropsPerRow(stmtSelectTwo.iterator(), fieldsCnt, new Object[][]{{5L}});
    }

    public void testLateConsumerJoin()
    {
        String[] fieldsWin = new String[] {"key", "value"};
        String[] fieldsJoin = new String[] {"key", "value", "symbol"};

        String stmtTextCreate = "create window MyWindow.win:keepall() as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("key"));
        assertEquals(long.class, stmtCreate.getEventType().getPropertyType("value"));

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // send events
        sendSupportBean("E1", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E1", 1L});

        sendSupportBean("E2", 1L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E2", 1L});

        // This replays into MyWindow
        String stmtTextSelectTwo = "select key, value, symbol from MyWindow as s0" +
                                   " left outer join " + SupportMarketDataBean.class.getName() + ".win:keepall() as s1" +
                                   " on s0.value = s1.volume";
        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(stmtTextSelectTwo);
        stmtSelectTwo.addListener(listenerStmtTwo);
        assertFalse(listenerStmtTwo.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectTwo.iterator(), fieldsJoin, new Object[][]{{"E1", 1L, null}, {"E2", 1L, null}});

        sendMarketBean("S1", 1);    // join on long
        assertEquals(2, listenerStmtTwo.getLastNewData().length);
        if (listenerStmtTwo.getLastNewData()[0].get("key").equals("E1"))
        {
            EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fieldsJoin, new Object[]{"E1", 1L, "S1"});
            EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[1], fieldsJoin, new Object[]{"E2", 1L, "S1"});
        }
        else
        {
            EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fieldsJoin, new Object[]{"E2", 1L, "S1"});
            EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[1], fieldsJoin, new Object[]{"E1", 1L, "S1"});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectTwo.iterator(), fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}});
        listenerStmtTwo.reset();

        sendMarketBean("S2", 2);    // join on long
        assertFalse(listenerStmtTwo.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectTwo.iterator(), fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}});

        sendSupportBean("E3", 2L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsWin, new Object[]{"E3", 2L});
        EPAssertionUtil.assertProps(listenerStmtTwo.getLastNewData()[0], fieldsJoin, new Object[]{"E3", 2L, "S2"});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtSelectTwo.iterator(), fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}, {"E3", 2L, "S2"}});
    }

    public void testPattern()
    {
        String[] fields = new String[] {"key", "value"};
        String stmtTextCreate = "create window MyWindow.win:keepall() as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        String stmtTextPattern = "select a.key as key, a.value as value from pattern [every a=MyWindow(key='S1') or a=MyWindow(key='S2')]";
        EPStatement stmtPattern = epService.getEPAdministrator().createEPL(stmtTextPattern);
        stmtPattern.addListener(listenerStmtOne);

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        sendSupportBean("E1", 1L);
        assertFalse(listenerStmtOne.isInvoked());

        sendSupportBean("S1", 2L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 2L});

        sendSupportBean("S1", 3L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 3L});

        sendSupportBean("S2", 4L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S2", 4L});

        sendSupportBean("S1", 1L);
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testExternallyTimedBatch()
    {
        String[] fields = new String[] {"key", "value"};

        // create window
        String stmtTextCreate = "create window MyWindow.win:ext_timed_batch(value, 10 sec, 0L) as MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumer
        String stmtTextSelectOne = "select irstream key, value as value from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1000L);
        sendSupportBean("E2", 8000L);
        sendSupportBean("E3", 9999L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1000L}, {"E2", 8000L}, {"E3", 9999L}});

        // delete E2
        sendMarketBean("E2");
        EPAssertionUtil.assertPropsPerRow(listenerWindow.assertInvokedAndReset(), fields, null, new Object[][] {{"E2", 8000L}});
        EPAssertionUtil.assertPropsPerRow(listenerStmtOne.assertInvokedAndReset(), fields, null, new Object[][] {{"E2", 8000L}});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1000L}, {"E3", 9999L}});

        sendSupportBean("E4", 10000L);
        EPAssertionUtil.assertPropsPerRow(listenerWindow.assertInvokedAndReset(), fields,
                new Object[][] {{"E1", 1000L}, {"E3", 9999L}}, null);
        EPAssertionUtil.assertPropsPerRow(listenerStmtOne.assertInvokedAndReset(), fields,
                new Object[][] {{"E1", 1000L}, {"E3", 9999L}}, null);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 10000L}});

        // delete E4
        sendMarketBean("E4");
        EPAssertionUtil.assertPropsPerRow(listenerWindow.assertInvokedAndReset(), fields, null, new Object[][] {{"E4", 10000L}});
        EPAssertionUtil.assertPropsPerRow(listenerStmtOne.assertInvokedAndReset(), fields, null, new Object[][] {{"E4", 10000L}});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        sendSupportBean("E5", 14000L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E5", 14000L}});

        sendSupportBean("E6", 21000L);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E6", 21000L}});
        EPAssertionUtil.assertPropsPerRow(listenerWindow.assertInvokedAndReset(), fields,
                new Object[][] {{"E5", 14000L}}, new Object[][] {{"E1", 1000L}, {"E3", 9999L}});
    }

    private SupportBean sendSupportBean(String theString, Long longBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBeanInt(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendMarketBean(String symbol)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0l, "");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(String symbol, long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
