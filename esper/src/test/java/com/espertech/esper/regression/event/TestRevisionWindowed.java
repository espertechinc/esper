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

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

public class TestRevisionWindowed extends TestCase
{
    private static final Log log = LogFactory.getLog(TestRevisionWindowed.class);
    private EPServiceProvider epService;
    private EPStatement stmtCreateWin;
    private SupportUpdateListener listenerOne;
    private final String[] fields = "k0,p1,p5".split(",");

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();

        // first revision event type
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("FullEvent", SupportRevisionFull.class);
        config.addEventType("D1", SupportDeltaOne.class);
        config.addEventType("D5", SupportDeltaFive.class);

        ConfigurationRevisionEventType configRev = new ConfigurationRevisionEventType();
        configRev.setKeyPropertyNames(new String[] {"k0"});
        configRev.addNameBaseEventType("FullEvent");
        configRev.addNameDeltaEventType("D1");
        configRev.addNameDeltaEventType("D5");
        config.addRevisionEventType("RevisableQuote", configRev);

        // second revision event type
        config.addEventType("MyMap", makeMap(
                new Object[][] { {"p5", String.class}, {"p1", String.class}, {"k0", String.class}, {"m0", String.class} }));
        configRev = new ConfigurationRevisionEventType();
        configRev.setKeyPropertyNames(new String[] {"p5", "p1"});
        configRev.addNameBaseEventType("MyMap");
        configRev.addNameDeltaEventType("D1");
        configRev.addNameDeltaEventType("D5");
        config.addRevisionEventType("RevisableMap", configRev);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listenerOne = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerOne = null;
    }

    public void testSubclassInterface()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("ISupportRevisionFull", ISupportRevisionFull.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ISupportDeltaFive", ISupportDeltaFive.class);

        ConfigurationRevisionEventType config = new ConfigurationRevisionEventType();
        config.addNameBaseEventType("ISupportRevisionFull");
        config.setKeyPropertyNames(new String[] {"k0"});
        config.addNameDeltaEventType("ISupportDeltaFive");
        epService.getEPAdministrator().getConfiguration().addRevisionEventType("MyInterface", config);

        stmtCreateWin = epService.getEPAdministrator().createEPL("create window MyInterfaceWindow.win:keepall() as select * from MyInterface");
        epService.getEPAdministrator().createEPL("insert into MyInterfaceWindow select * from ISupportRevisionFull");
        epService.getEPAdministrator().createEPL("insert into MyInterfaceWindow select * from ISupportDeltaFive");

        EPStatement consumerOne = epService.getEPAdministrator().createEPL("@Audit select irstream k0,p0,p1 from MyInterfaceWindow");
        consumerOne.addListener(listenerOne);
        String fields[] = "k0,p0,p1".split(",");
        EPAssertionUtil.assertEqualsAnyOrder(consumerOne.getEventType().getPropertyNames(), fields);

        epService.getEPRuntime().sendEvent(new SupportRevisionFull(null, "00", "10", "20", "30", "40", "50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{null, "00", "10"});

        epService.getEPRuntime().sendEvent(new SupportDeltaFive(null, "999", null));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{null, "00", "999"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{null, "00", "10"});
        listenerOne.reset();

        stmtCreateWin.stop();
        stmtCreateWin.start();
        consumerOne.stop();
        consumerOne.start();

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("zz", "xx", "yy", "20", "30", "40", "50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"zz", "xx", "yy"});
    }

    public void testMultiPropertyMapMixin()
    {
        String[] fields = "k0,p1,p5,m0".split(",");
        stmtCreateWin = epService.getEPAdministrator().createEPL("create window RevMap.win:length(3) as select * from RevisableMap");
        epService.getEPAdministrator().createEPL("insert into RevMap select * from MyMap");
        epService.getEPAdministrator().createEPL("insert into RevMap select * from D1");
        epService.getEPAdministrator().createEPL("insert into RevMap select * from D5");

        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from RevMap order by k0");
        consumerOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(makeMap(new Object[][] { {"p5", "p5_1"}, {"p1", "p1_1"}, {"k0", "E1"}, {"m0", "M0"}}), "MyMap");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", "p1_1", "p5_1", "M0"});

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("E2", "p1_1", "p5_1"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"E2", "p1_1", "p5_1", "M0"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"E1", "p1_1", "p5_1", "M0"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"E2", "p1_1", "p5_1", "M0"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap(new Object[][] { {"p5", "p5_1"}, {"p1", "p1_2"}, {"k0", "E3"}, {"m0", "M1"}}), "MyMap");
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields, new Object[][]{{"E2", "p1_1", "p5_1", "M0"}, {"E3", "p1_2", "p5_1", "M1"}});

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("E4", "p1_1", "p5_1"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"E4", "p1_1", "p5_1", "M0"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"E2", "p1_1", "p5_1", "M0"});
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields, new Object[][]{{"E3", "p1_2", "p5_1", "M1"}, {"E4", "p1_1", "p5_1", "M0"}});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap(new Object[][] { {"p5", "p5_2"}, {"p1", "p1_1"}, {"k0", "E5"}, {"m0", "M2"}}), "MyMap");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"E5", "p1_1", "p5_2", "M2"});
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields,
                new Object[][]{{"E3", "p1_2", "p5_1", "M1"}, {"E4", "p1_1", "p5_1", "M0"}, {"E5", "p1_1", "p5_2", "M2"}});

        epService.getEPRuntime().sendEvent(new SupportDeltaOne("E6", "p1_1", "p5_2"));
        EPAssertionUtil.assertProps(listenerOne.assertPairGetIRAndReset(), fields,
                new Object[]{"E6", "p1_1", "p5_2", "M2"}, new Object[]{"E5", "p1_1", "p5_2", "M2"});
    }

    public void testTimeWindow()
    {
        sendTimer(0);
        stmtCreateWin = epService.getEPAdministrator().createEPL("create window RevQuote.win:time(10 sec) as select * from RevisableQuote");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from FullEvent");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D1");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D5");

        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from RevQuote");
        consumerOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("a", "a10", "a50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"a", "a10", "a50"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"a", "a10", "a50"});

        sendTimer(1000);

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("a", "a11", "a51"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"a", "a11", "a51"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"a", "a10", "a50"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"a", "a11", "a51"});

        sendTimer(2000);

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("b", "b10", "b50"));
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("c", "c10", "c50"));

        sendTimer(3000);
        epService.getEPRuntime().sendEvent(new SupportDeltaOne("c", "c11", "c51"));

        sendTimer(8000);
        epService.getEPRuntime().sendEvent(new SupportDeltaOne("c", "c12", "c52"));
        listenerOne.reset();

        sendTimer(10000);
        assertFalse(listenerOne.isInvoked());

        sendTimer(11000);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"a", "a11", "a51"});

        sendTimer(12000);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"b", "b10", "b50"});

        sendTimer(13000);
        assertFalse(listenerOne.isInvoked());

        sendTimer(18000);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"c", "c12", "c52"});
    }

    public void testUnique()
    {
        stmtCreateWin = epService.getEPAdministrator().createEPL("create window RevQuote.std:unique(p1) as select * from RevisableQuote");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from FullEvent");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D1");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D5");

        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from RevQuote");
        consumerOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("a", "a10", "a50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"a", "a10", "a50"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"a", "a10", "a50"});

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("a", "a11", "a51"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"a", "a11", "a51"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"a", "a10", "a50"});
        listenerOne.reset();
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"a", "a11", "a51"});

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("b", "b10", "b50"));
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("b", "b10", "b50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"b", "b10", "b50"});
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields, new Object[][]{{"a", "a11", "a51"}, {"b", "b10", "b50"}});

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("b", "a11", "b51"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"b", "a11", "b51"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"a", "a11", "a51"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"b", "a11", "b51"});
    }

    public void testGroupLength()
    {
        stmtCreateWin = epService.getEPAdministrator().createEPL("create window RevQuote.std:groupwin(p1).win:length(2) as select * from RevisableQuote");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from FullEvent");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D1");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D5");

        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from RevQuote order by k0 asc");
        consumerOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("a", "p1", "a50"));
        epService.getEPRuntime().sendEvent(new SupportDeltaFive("a", "p1", "a51"));
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("b", "p2", "b50"));
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("c", "p3", "c50"));
        epService.getEPRuntime().sendEvent(new SupportDeltaFive("d", "p3", "d50"));

        listenerOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields, new Object[][]{{"a", "p1", "a51"}, {"b", "p2", "b50"}, {"c", "p3", "c50"}});

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("b", "p1", "b51"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"b", "p1", "b51"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"b", "p2", "b50"});
        listenerOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields, new Object[][]{{"a", "p1", "a51"}, {"b", "p1", "b51"}, {"c", "p3", "c50"}});

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("c", "p1", "c51"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"c", "p1", "c51"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[1], fields, new Object[]{"c", "p3", "c50"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"a", "p1", "a51"});
        listenerOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields, new Object[][]{{"b", "p1", "b51"}, {"c", "p1", "c51"}});
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private Map<String, Object> makeMap(Object[][] entries)
    {
        Map result = new HashMap<String, Object>();
        for (int i = 0; i < entries.length; i++)
        {
            result.put(entries[i][0], entries[i][1]);
        }
        return result;
    }
}
