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
package com.espertech.esper.regression.event.revision;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;

public class ExecEventRevisionDeclared implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecEventRevisionDeclared.class);
    private final String[] fields = "k0,p0,p1,p2,p3,p4,p5".split(",");

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("FullEvent", SupportRevisionFull.class);
        configuration.addEventType("D1", SupportDeltaOne.class);
        configuration.addEventType("D2", SupportDeltaTwo.class);
        configuration.addEventType("D3", SupportDeltaThree.class);
        configuration.addEventType("D4", SupportDeltaFour.class);
        configuration.addEventType("D5", SupportDeltaFive.class);

        ConfigurationRevisionEventType configRev = new ConfigurationRevisionEventType();
        configRev.setKeyPropertyNames(new String[]{"k0"});
        configRev.addNameBaseEventType("FullEvent");
        configRev.addNameDeltaEventType("D1");
        configRev.addNameDeltaEventType("D2");
        configRev.addNameDeltaEventType("D3");
        configRev.addNameDeltaEventType("D4");
        configRev.addNameDeltaEventType("D5");
        configuration.addRevisionEventType("RevisableQuote", configRev);
    }

    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmtCreateWin = epService.getEPAdministrator().createEPL("create window RevQuote#keepall as select * from RevisableQuote");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from FullEvent");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D1");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D2");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D3");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D4");
        epService.getEPAdministrator().createEPL("insert into RevQuote select * from D5");

        runAssertionMetadata(epService);
        runAssertionRevision(epService, stmtCreateWin);
        runAssertionOnDelete(epService, stmtCreateWin);
        if (!InstrumentationHelper.ENABLED) {
            runAssertionRevisionGen(epService);
        }
        runAssertionInvalidConfig(epService);
        runAssertionInvalidInsertInto(epService);
    }

    private void runAssertionMetadata(EPServiceProvider epService) {
        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getValueAddEventService().getValueAddProcessor("RevQuote").getValueAddEventType();
        assertEquals(null, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("RevisableQuote", type.getMetadata().getPrimaryName());
        assertEquals("RevisableQuote", type.getMetadata().getPublicName());
        assertEquals("RevisableQuote", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.REVISION, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfiguredStatic());

        EventType[] valueAddTypes = ((EPServiceProviderSPI) epService).getValueAddEventService().getValueAddedTypes();
        assertEquals(1, valueAddTypes.length);
        assertSame(type, valueAddTypes[0]);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("k0", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("p0", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("p1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("p2", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("p3", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("p4", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("p5", String.class, null, false, false, false, false, false)
        }, type.getPropertyDescriptors());
    }

    private void runAssertionRevision(EPServiceProvider epService, EPStatement stmtCreateWin) {
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select * from RevQuote");
        consumerOne.addListener(listenerOne);
        EPStatement consumerTwo = epService.getEPAdministrator().createEPL("select k0, count(*) as count, sum(Long.parseLong(p0)) as sum from RevQuote group by k0");
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        consumerTwo.addListener(listenerTwo);
        EPStatement consumerThree = epService.getEPAdministrator().createEPL("select * from RevQuote output every 2 events");
        SupportUpdateListener listenerThree = new SupportUpdateListener();
        consumerThree.addListener(listenerThree);
        String[] agg = "k0,count,sum".split(",");

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("k00", "01", "p10", "20", "p30", "40", "50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"k00", "01", "p10", "20", "p30", "40", "50"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"k00", "01", "p10", "20", "p30", "40", "50"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), agg, new Object[]{"k00", 1L, 1L});
        assertFalse(listenerThree.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportDeltaThree("k00", "03", "41"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"k00", "03", "p10", "20", "p30", "41", "50"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"k00", "03", "p10", "20", "p30", "41", "50"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), agg, new Object[]{"k00", 1L, 3L});
        EPAssertionUtil.assertProps(listenerThree.getLastNewData()[0], fields, new Object[]{"k00", "01", "p10", "20", "p30", "40", "50"});
        EPAssertionUtil.assertProps(listenerThree.getLastNewData()[1], fields, new Object[]{"k00", "03", "p10", "20", "p30", "41", "50"});
        listenerThree.reset();

        epService.getEPRuntime().sendEvent(new SupportDeltaOne("k00", "p11", "51"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"k00", "03", "p11", "20", "p30", "41", "51"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"k00", "03", "p11", "20", "p30", "41", "51"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), agg, new Object[]{"k00", 1L, 3L});
        assertFalse(listenerThree.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportDeltaTwo("k00", "04", "21", "p31"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"k00", "04", "p11", "21", "p31", "41", "51"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"k00", "04", "p11", "21", "p31", "41", "51"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), agg, new Object[]{"k00", 1L, 4L});
        EPAssertionUtil.assertProps(listenerThree.getLastNewData()[0], fields, new Object[]{"k00", "03", "p11", "20", "p30", "41", "51"});
        EPAssertionUtil.assertProps(listenerThree.getLastNewData()[1], fields, new Object[]{"k00", "04", "p11", "21", "p31", "41", "51"});
        listenerThree.reset();

        epService.getEPRuntime().sendEvent(new SupportDeltaFour("k00", "05", "22", "52"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"k00", "05", "p11", "22", "p31", "41", "52"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"k00", "05", "p11", "22", "p31", "41", "52"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), agg, new Object[]{"k00", 1L, 5L});
        assertFalse(listenerThree.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportDeltaFive("k00", "p12", "53"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"k00", "05", "p12", "22", "p31", "41", "53"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"k00", "05", "p12", "22", "p31", "41", "53"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), agg, new Object[]{"k00", 1L, 5L});
        EPAssertionUtil.assertProps(listenerThree.getLastNewData()[0], fields, new Object[]{"k00", "05", "p11", "22", "p31", "41", "52"});
        EPAssertionUtil.assertProps(listenerThree.getLastNewData()[1], fields, new Object[]{"k00", "05", "p12", "22", "p31", "41", "53"});
        listenerThree.reset();

        epService.getEPRuntime().sendEvent(new SupportRevisionFull("k00", "06", "p13", "23", "p32", "42", "54"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"k00", "06", "p13", "23", "p32", "42", "54"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"k00", "06", "p13", "23", "p32", "42", "54"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), agg, new Object[]{"k00", 1L, 6L});
        assertFalse(listenerThree.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportDeltaOne("k00", "p14", "55"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"k00", "06", "p14", "23", "p32", "42", "55"});
        EPAssertionUtil.assertProps(stmtCreateWin.iterator().next(), fields, new Object[]{"k00", "06", "p14", "23", "p32", "42", "55"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), agg, new Object[]{"k00", 1L, 6L});
        EPAssertionUtil.assertProps(listenerThree.getLastNewData()[0], fields, new Object[]{"k00", "06", "p13", "23", "p32", "42", "54"});
        EPAssertionUtil.assertProps(listenerThree.getLastNewData()[1], fields, new Object[]{"k00", "06", "p14", "23", "p32", "42", "55"});
        listenerThree.reset();

        consumerOne.destroy();
        consumerTwo.destroy();
        consumerThree.destroy();
        epService.getEPRuntime().executeQuery("delete from RevQuote");
    }

    private void runAssertionOnDelete(EPServiceProvider epService, EPStatement stmtCreateWin) {
        List<EPStatement> statements = new ArrayList<>();
        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from RevQuote");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        consumerOne.addListener(listenerOne);
        statements.add(consumerOne);

        statements.add(epService.getEPAdministrator().createEPL("on SupportBean(intPrimitive=2) as sb delete from RevQuote where theString = p2"));

        log("a00");
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("a", "a00", "a10", "a20", "a30", "a40", "a50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"a", "a00", "a10", "a20", "a30", "a40", "a50"});

        epService.getEPRuntime().sendEvent(new SupportDeltaThree("x", "03", "41"));
        assertFalse(listenerOne.isInvoked());

        statements.add(epService.getEPAdministrator().createEPL("on SupportBean(intPrimitive=3) as sb delete from RevQuote where theString = p3"));

        log("b00");
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("b", "b00", "b10", "b20", "b30", "b40", "b50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"b", "b00", "b10", "b20", "b30", "b40", "b50"});

        log("a01");
        epService.getEPRuntime().sendEvent(new SupportDeltaThree("a", "a01", "a41"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"a", "a01", "a10", "a20", "a30", "a41", "a50"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"a", "a00", "a10", "a20", "a30", "a40", "a50"});
        listenerOne.reset();

        log("c00");
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("c", "c00", "c10", "c20", "c30", "c40", "c50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"c", "c00", "c10", "c20", "c30", "c40", "c50"});

        statements.add(epService.getEPAdministrator().createEPL("on SupportBean(intPrimitive=0) as sb delete from RevQuote where theString = p0"));

        log("c11");
        epService.getEPRuntime().sendEvent(new SupportDeltaFive("c", "c11", "c51"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"c", "c00", "c11", "c20", "c30", "c40", "c51"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"c", "c00", "c10", "c20", "c30", "c40", "c50"});
        listenerOne.reset();

        statements.add(epService.getEPAdministrator().createEPL("on SupportBean(intPrimitive=1) as sb delete from RevQuote where theString = p1"));

        log("d00");
        epService.getEPRuntime().sendEvent(new SupportRevisionFull("d", "d00", "d10", "d20", "d30", "d40", "d50"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"d", "d00", "d10", "d20", "d30", "d40", "d50"});

        log("d01");
        epService.getEPRuntime().sendEvent(new SupportDeltaFour("d", "d01", "d21", "d51"));
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"d", "d01", "d10", "d21", "d30", "d40", "d51"});
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"d", "d00", "d10", "d20", "d30", "d40", "d50"});
        listenerOne.reset();

        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields,
                new Object[][]{{"b", "b00", "b10", "b20", "b30", "b40", "b50"}, {"a", "a01", "a10", "a20", "a30", "a41", "a50"},
                    {"c", "c00", "c11", "c20", "c30", "c40", "c51"}, {"d", "d01", "d10", "d21", "d30", "d40", "d51"}});

        statements.add(epService.getEPAdministrator().createEPL("on SupportBean(intPrimitive=4) as sb delete from RevQuote where theString = p4"));

        epService.getEPRuntime().sendEvent(new SupportBean("abc", 1));
        assertFalse(listenerOne.isInvoked());

        log("delete b");
        epService.getEPRuntime().sendEvent(new SupportBean("b40", 4));  // delete b
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"b", "b00", "b10", "b20", "b30", "b40", "b50"});
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields,
                new Object[][]{{"a", "a01", "a10", "a20", "a30", "a41", "a50"}, {"c", "c00", "c11", "c20", "c30", "c40", "c51"}, {"d", "d01", "d10", "d21", "d30", "d40", "d51"}});

        log("delete d");
        epService.getEPRuntime().sendEvent(new SupportBean("d21", 2)); // delete d
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"d", "d01", "d10", "d21", "d30", "d40", "d51"});
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields,
                new Object[][]{{"a", "a01", "a10", "a20", "a30", "a41", "a50"}, {"c", "c00", "c11", "c20", "c30", "c40", "c51"}});

        log("delete a");
        epService.getEPRuntime().sendEvent(new SupportBean("a30", 3)); // delete a
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"a", "a01", "a10", "a20", "a30", "a41", "a50"});
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields, new Object[][]{{"c", "c00", "c11", "c20", "c30", "c40", "c51"}});

        log("delete c");
        epService.getEPRuntime().sendEvent(new SupportBean("c11", 1)); // delete c
        EPAssertionUtil.assertProps(listenerOne.assertOneGetOldAndReset(), fields, new Object[]{"c", "c00", "c11", "c20", "c30", "c40", "c51"});
        EPAssertionUtil.assertPropsPerRow(stmtCreateWin.iterator(), fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("c11", 1));
        assertFalse(listenerOne.isInvoked());

        for (EPStatement statement : statements) {
            statement.destroy();
        }
    }

    private void runAssertionRevisionGen(EPServiceProvider epService) {
        Random random = new Random();
        Map<String, Map<String, String>> last = new HashMap<String, Map<String, String>>();
        int count = 0;
        String[] groups = new String[]{"K0", "K1", "K2", "K4"};

        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select * from RevQuote");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        consumerOne.addListener(listenerOne);

        for (int i = 0; i < groups.length; i++) {
            String key = groups[i];
            Object theEvent = new SupportRevisionFull(key, "0-" + next(count), "1-" + next(count), "2-" + next(count),
                    "3-" + next(count), "4-" + next(count), "5-" + next(count));
            add(last, key, "0-" + next(count), "1-" + next(count), "2-" + next(count),
                    "3-" + next(count), "4-" + next(count), "5-" + next(count));
            epService.getEPRuntime().sendEvent(theEvent);
        }
        listenerOne.reset();

        for (int i = 0; i < 10000; i++) {
            if (i % 20000 == 0) {
                log.debug(".testRevisionGen Loop " + i);
            }
            int typeNum = random.nextInt(6);
            String key = groups[random.nextInt(groups.length)];
            count++;

            Object theEvent;
            if (typeNum == 0) {
                theEvent = new SupportRevisionFull(key, "0-" + next(count), "1-" + next(count), "2-" + next(count),
                        "3-" + next(count), "4-" + next(count), "5-" + next(count));
                add(last, key, "0-" + next(count), "1-" + next(count), "2-" + next(count),
                        "3-" + next(count), "4-" + next(count), "5-" + next(count));
            } else if (typeNum == 1) {
                theEvent = new SupportDeltaOne(key, "1-" + next(count), "5-" + next(count));
                add(last, key, null, "1-" + next(count), null, null, null, "5-" + next(count));
            } else if (typeNum == 2) {
                theEvent = new SupportDeltaTwo(key, "0-" + next(count), "2-" + next(count), "3-" + next(count));
                add(last, key, "0-" + next(count), null, "2-" + next(count), "3-" + next(count), null, null);
            } else if (typeNum == 3) {
                theEvent = new SupportDeltaThree(key, "0-" + next(count), "4-" + next(count));
                add(last, key, "0-" + next(count), null, null, null, "4-" + next(count), null);
            } else if (typeNum == 4) {
                theEvent = new SupportDeltaFour(key, "0-" + next(count), "2-" + next(count), "5-" + next(count));
                add(last, key, "0-" + next(count), null, "2-" + next(count), null, null, "5-" + next(count));
            } else if (typeNum == 5) {
                theEvent = new SupportDeltaFive(key, "1-" + next(count), "5-" + next(count));
                add(last, key, null, "1-" + next(count), null, null, null, "5-" + next(count));
            } else {
                throw new IllegalStateException();
            }

            epService.getEPRuntime().sendEvent(theEvent);
            assertEvent(last, listenerOne.assertOneGetNewAndReset(), count);
        }
        consumerOne.destroy();
    }

    private void runAssertionInvalidConfig(EPServiceProvider epService) {
        ConfigurationRevisionEventType config = new ConfigurationRevisionEventType();
        tryInvalidConfig(epService, "abc", config, "Required base event type name is not set in the configuration for revision event type 'abc'");

        epService.getEPAdministrator().getConfiguration().addEventType("MyEvent", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyComplex", SupportBeanComplexProps.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyTypeChange", SupportBeanTypeChange.class);

        config.addNameBaseEventType("XYZ");
        tryInvalidConfig(epService, "abc", config, "Could not locate event type for name 'XYZ' in the configuration for revision event type 'abc'");

        config.getNameBaseEventTypes().clear();
        config.addNameBaseEventType("MyEvent");
        tryInvalidConfig(epService, "abc", config, "Required key properties are not set in the configuration for revision event type 'abc'");

        config.addNameBaseEventType("AEvent");
        config.addNameBaseEventType("AEvent");
        tryInvalidConfig(epService, "abc", config, "Only one base event type name may be added to revision event type 'abc', multiple base types are not yet supported");

        config.getNameBaseEventTypes().clear();
        config.addNameBaseEventType("MyEvent");
        config.setKeyPropertyNames(new String[0]);
        tryInvalidConfig(epService, "abc", config, "Required key properties are not set in the configuration for revision event type 'abc'");

        config.setKeyPropertyNames(new String[]{"xyz"});
        tryInvalidConfig(epService, "abc", config, "Key property 'xyz' as defined in the configuration for revision event type 'abc' does not exists in event type 'MyEvent'");

        config.setKeyPropertyNames(new String[]{"intPrimitive"});
        config.addNameDeltaEventType("MyComplex");
        tryInvalidConfig(epService, "abc", config, "Key property 'intPrimitive' as defined in the configuration for revision event type 'abc' does not exists in event type 'MyComplex'");

        config.addNameDeltaEventType("XYZ");
        tryInvalidConfig(epService, "abc", config, "Could not locate event type for name 'XYZ' in the configuration for revision event type 'abc'");

        config.getNameDeltaEventTypes().clear();
        config.setKeyPropertyNames(new String[]{"intBoxed"});
        config.addNameDeltaEventType("MyTypeChange");  // invalid intPrimitive property type
        tryInvalidConfig(epService, "abc", config, "Property named 'intPrimitive' does not have the same type for base and delta types of revision event type 'abc'");

        config.getNameDeltaEventTypes().clear();
        epService.getEPAdministrator().getConfiguration().addRevisionEventType("abc", config);
    }

    private void runAssertionInvalidInsertInto(EPServiceProvider epService) {
        SupportMessageAssertUtil.tryInvalid(epService, "insert into RevQuote select * from " + SupportBean.class.getName(),
                "Error starting statement: Selected event type is not a valid base or delta event type of revision event type 'RevisableQuote' [");

        SupportMessageAssertUtil.tryInvalid(epService, "insert into RevQuote select intPrimitive as k0 from " + SupportBean.class.getName(),
                "Error starting statement: Selected event type is not a valid base or delta event type of revision event type 'RevisableQuote' ");
    }

    private void tryInvalidConfig(EPServiceProvider epService, String name, ConfigurationRevisionEventType config, String message) {
        try {
            epService.getEPAdministrator().getConfiguration().addRevisionEventType(name, config);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private void assertEvent(Map<String, Map<String, String>> last, EventBean eventBean, int count) {
        String error = "Error asseting count " + count;
        String key = (String) eventBean.get("k0");
        Map<String, String> vals = last.get(key);
        assertEquals(error, vals.get("p0"), eventBean.get("p0"));
        assertEquals(error, vals.get("p1"), eventBean.get("p1"));
        assertEquals(error, vals.get("p2"), eventBean.get("p2"));
        assertEquals(error, vals.get("p3"), eventBean.get("p3"));
        assertEquals(error, vals.get("p4"), eventBean.get("p4"));
        assertEquals(error, vals.get("p5"), eventBean.get("p5"));
    }

    private void add(Map<String, Map<String, String>> last, String key, String s0, String s1, String s2, String s3, String s4, String s5) {
        Map<String, String> entry = last.get(key);
        if (entry == null) {
            entry = new HashMap<String, String>();
            last.put(key, entry);
        }

        if (s0 != null) {
            entry.put("p0", s0);
        }
        if (s1 != null) {
            entry.put("p1", s1);
        }
        if (s2 != null) {
            entry.put("p2", s2);
        }
        if (s3 != null) {
            entry.put("p3", s3);
        }
        if (s4 != null) {
            entry.put("p4", s4);
        }
        if (s5 != null) {
            entry.put("p5", s5);
        }
    }

    private String next(int num) {
        return Integer.toString(num);
    }

    private void log(String text) {
        log.debug(".log " + text);
    }
}
