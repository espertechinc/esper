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
package com.espertech.esper.regression.event.variant;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.*;

import static junit.framework.TestCase.*;

public class ExecEventVariantStreamDefault implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanVariantStream.class);
        epService.getEPAdministrator().getConfiguration().addImport(this.getClass().getName());

        runAssertionSingleColumnConversion(epService);
        runAssertionCoercionBoxedTypeMatch(epService);
        runAssertionSuperTypesInterfaces(epService);
        runAssertionNamedWin(epService);
        runAssertionPatternSubquery(epService);
        runAssertionDynamicMapType(epService);
        runAssertionInvalidInsertInto(epService);
        runAssertionInvalidConfig(epService);
    }

    private void runAssertionSingleColumnConversion(EPServiceProvider epService) {

        ConfigurationVariantStream variant = new ConfigurationVariantStream();
        variant.addEventTypeName("SupportBean");
        variant.addEventTypeName("SupportBeanVariantStream");
        epService.getEPAdministrator().getConfiguration().addVariantStream("AllEvents", variant);

        epService.getEPAdministrator().createEPL("insert into AllEvents select * from SupportBean");
        epService.getEPAdministrator().createEPL("create window MainEventWindow#length(10000) as AllEvents");
        epService.getEPAdministrator().createEPL("insert into MainEventWindow select " + this.getClass().getSimpleName() + ".preProcessEvent(event) from AllEvents as event");

        EPStatement statement = epService.getEPAdministrator().createEPL("select * from MainEventWindow where theString = 'E'");
        statement.addListenerWithReplay(new SupportUpdateListener());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        epService.getEPAdministrator().destroyAllStatements();
    }

    public static Object preProcessEvent(Object o) {
        return new SupportBean("E2", 0);
    }

    private void runAssertionCoercionBoxedTypeMatch(EPServiceProvider epService) {
        ConfigurationVariantStream variant = new ConfigurationVariantStream();
        variant.addEventTypeName("SupportBean");
        variant.addEventTypeName("SupportBeanVariantStream");
        epService.getEPAdministrator().getConfiguration().addVariantStream("MyVariantStreamOne", variant);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyVariantStreamOne");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmt.addListener(listenerOne);
        EventType typeSelectAll = stmt.getEventType();
        assertEventTypeDefault(typeSelectAll);
        assertEquals(Object.class, stmt.getEventType().getUnderlyingType());

        epService.getEPAdministrator().createEPL("insert into MyVariantStreamOne select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyVariantStreamOne select * from SupportBeanVariantStream");

        // try wildcard
        Object eventOne = new SupportBean("E0", -1);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, listenerOne.assertOneGetNewAndReset().getUnderlying());

        Object eventTwo = new SupportBeanVariantStream("E1");
        epService.getEPRuntime().sendEvent(eventTwo);
        assertSame(eventTwo, listenerOne.assertOneGetNewAndReset().getUnderlying());

        stmt.destroy();
        String fields = "theString,boolBoxed,intPrimitive,longPrimitive,doublePrimitive,enumValue";
        stmt = epService.getEPAdministrator().createEPL("select " + fields + " from MyVariantStreamOne");
        stmt.addListener(listenerOne);
        assertEventTypeDefault(stmt.getEventType());

        // coerces to the higher resolution type, accepts boxed versus not boxed
        epService.getEPRuntime().sendEvent(new SupportBeanVariantStream("s1", true, 1, 20, 30, SupportEnum.ENUM_VALUE_1));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields.split(","), new Object[]{"s1", true, 1, 20L, 30d, SupportEnum.ENUM_VALUE_1});

        SupportBean bean = new SupportBean("s2", 99);
        bean.setLongPrimitive(33);
        bean.setDoublePrimitive(50);
        bean.setEnumValue(SupportEnum.ENUM_VALUE_3);
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields.split(","), new Object[]{"s2", null, 99, 33L, 50d, SupportEnum.ENUM_VALUE_3});

        // make sure a property is not known since the property is not found on SupportBeanVariantStream
        try {
            epService.getEPAdministrator().createEPL("select charBoxed from MyVariantStreamOne");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'charBoxed': Property named 'charBoxed' is not valid in any stream [select charBoxed from MyVariantStreamOne]", ex.getMessage());
        }

        // try dynamic property: should exists but not show up as a declared property
        stmt.destroy();
        fields = "v1,v2,v3";
        stmt = epService.getEPAdministrator().createEPL("select longBoxed? as v1,charBoxed? as v2,doubleBoxed? as v3 from MyVariantStreamOne");
        stmt.addListener(listenerOne);
        assertEventTypeDefault(typeSelectAll);  // asserts prior "select *" event type

        bean = new SupportBean();
        bean.setLongBoxed(33L);
        bean.setCharBoxed('a');
        bean.setDoubleBoxed(Double.NaN);
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields.split(","), new Object[]{33L, 'a', Double.NaN});

        epService.getEPRuntime().sendEvent(new SupportBeanVariantStream("s2"));
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields.split(","), new Object[]{null, null, null});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSuperTypesInterfaces(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanVariantOne", SupportBeanVariantOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanVariantTwo", SupportBeanVariantTwo.class);

        ConfigurationVariantStream variant = new ConfigurationVariantStream();
        variant.addEventTypeName("SupportBeanVariantOne");
        variant.addEventTypeName("SupportBeanVariantTwo");
        epService.getEPAdministrator().getConfiguration().addVariantStream("MyVariantStreamTwo", variant);
        epService.getEPAdministrator().createEPL("insert into MyVariantStreamTwo select * from SupportBeanVariantOne");
        epService.getEPAdministrator().createEPL("insert into MyVariantStreamTwo select * from SupportBeanVariantTwo");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyVariantStreamTwo");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmt.addListener(listenerOne);
        EventType eventType = stmt.getEventType();

        String[] expected = "p0,p1,p2,p3,p4,p5,indexed,mapped,inneritem".split(",");
        String[] propertyNames = eventType.getPropertyNames();
        EPAssertionUtil.assertEqualsAnyOrder(expected, propertyNames);
        assertEquals(ISupportBaseAB.class, eventType.getPropertyType("p0"));
        assertEquals(ISupportAImplSuperG.class, eventType.getPropertyType("p1"));
        assertEquals(AbstractList.class, eventType.getPropertyType("p2"));
        assertEquals(List.class, eventType.getPropertyType("p3"));
        assertEquals(Collection.class, eventType.getPropertyType("p4"));
        assertEquals(Collection.class, eventType.getPropertyType("p5"));
        assertEquals(int[].class, eventType.getPropertyType("indexed"));
        assertEquals(Map.class, eventType.getPropertyType("mapped"));
        assertEquals(SupportBeanVariantOne.SupportBeanVariantOneInner.class, eventType.getPropertyType("inneritem"));

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select p0,p1,p2,p3,p4,p5,indexed[0] as p6,indexArr[1] as p7,mappedKey('a') as p8,inneritem as p9,inneritem.val as p10 from MyVariantStreamTwo");
        stmt.addListener(listenerOne);
        eventType = stmt.getEventType();
        assertEquals(Integer.class, eventType.getPropertyType("p6"));
        assertEquals(Integer.class, eventType.getPropertyType("p7"));
        assertEquals(String.class, eventType.getPropertyType("p8"));
        assertEquals(SupportBeanVariantOne.SupportBeanVariantOneInner.class, eventType.getPropertyType("p9"));
        assertEquals(String.class, eventType.getPropertyType("p10"));

        SupportBeanVariantOne ev1 = new SupportBeanVariantOne();
        epService.getEPRuntime().sendEvent(ev1);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), "p6,p7,p8,p9,p10".split(","), new Object[]{1, 2, "val1", ev1.getInneritem(), ev1.getInneritem().getVal()});

        SupportBeanVariantTwo ev2 = new SupportBeanVariantTwo();
        epService.getEPRuntime().sendEvent(ev2);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), "p6,p7,p8,p9,p10".split(","), new Object[]{10, 20, "val2", ev2.getInneritem(), ev2.getInneritem().getVal()});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertEventTypeDefault(EventType eventType) {
        String[] expected = "theString,boolBoxed,intPrimitive,longPrimitive,doublePrimitive,enumValue".split(",");
        String[] propertyNames = eventType.getPropertyNames();
        EPAssertionUtil.assertEqualsAnyOrder(expected, propertyNames);
        assertEquals(String.class, eventType.getPropertyType("theString"));
        assertEquals(Boolean.class, eventType.getPropertyType("boolBoxed"));
        assertEquals(Integer.class, eventType.getPropertyType("intPrimitive"));
        assertEquals(Long.class, eventType.getPropertyType("longPrimitive"));
        assertEquals(Double.class, eventType.getPropertyType("doublePrimitive"));
        assertEquals(SupportEnum.class, eventType.getPropertyType("enumValue"));
        for (String expectedProp : expected) {
            assertNotNull(eventType.getGetter(expectedProp));
            assertTrue(eventType.isProperty(expectedProp));
        }

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("theString", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("boolBoxed", Boolean.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("intPrimitive", Integer.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("longPrimitive", Long.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("doublePrimitive", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("enumValue", SupportEnum.class, null, false, false, false, false, false),
        }, eventType.getPropertyDescriptors());
    }

    private void runAssertionNamedWin(EPServiceProvider epService) {
        ConfigurationVariantStream variant = new ConfigurationVariantStream();
        variant.addEventTypeName("SupportBeanVariantStream");
        variant.addEventTypeName("SupportBean");
        epService.getEPAdministrator().getConfiguration().addVariantStream("MyVariantStreamThree", variant);

        // test named window
        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyVariantWindow#unique(theString) as select * from MyVariantStreamThree");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmt.addListener(listenerOne);
        epService.getEPAdministrator().createEPL("insert into MyVariantWindow select * from MyVariantStreamThree");
        epService.getEPAdministrator().createEPL("insert into MyVariantStreamThree select * from SupportBeanVariantStream");
        epService.getEPAdministrator().createEPL("insert into MyVariantStreamThree select * from SupportBean");

        Object eventOne = new SupportBean("E1", -1);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, listenerOne.assertOneGetNewAndReset().getUnderlying());

        Object eventTwo = new SupportBeanVariantStream("E2");
        epService.getEPRuntime().sendEvent(eventTwo);
        assertSame(eventTwo, listenerOne.assertOneGetNewAndReset().getUnderlying());

        Object eventThree = new SupportBean("E2", -1);
        epService.getEPRuntime().sendEvent(eventThree);
        assertSame(eventThree, listenerOne.getLastNewData()[0].getUnderlying());
        assertSame(eventTwo, listenerOne.getLastOldData()[0].getUnderlying());
        listenerOne.reset();

        Object eventFour = new SupportBeanVariantStream("E1");
        epService.getEPRuntime().sendEvent(eventFour);
        assertSame(eventFour, listenerOne.getLastNewData()[0].getUnderlying());
        assertSame(eventOne, listenerOne.getLastOldData()[0].getUnderlying());
        listenerOne.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPatternSubquery(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        ConfigurationVariantStream variant = new ConfigurationVariantStream();
        variant.addEventTypeName("SupportBeanVariantStream");
        variant.addEventTypeName("SupportBean");
        epService.getEPAdministrator().getConfiguration().addVariantStream("MyVariantStreamFour", variant);

        epService.getEPAdministrator().createEPL("insert into MyVariantStreamFour select * from SupportBeanVariantStream");
        epService.getEPAdministrator().createEPL("insert into MyVariantStreamFour select * from SupportBean");

        // test pattern
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from pattern [a=MyVariantStreamFour -> b=MyVariantStreamFour]");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmt.addListener(listenerOne);
        Object[] events = {new SupportBean("E1", -1), new SupportBeanVariantStream("E2")};
        epService.getEPRuntime().sendEvent(events[0]);
        epService.getEPRuntime().sendEvent(events[1]);
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), "a,b".split(","), events);

        // test subquery
        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBean_A as a where exists(select * from MyVariantStreamFour#lastevent as b where b.theString=a.id)");
        stmt.addListener(listenerOne);
        events = new Object[]{new SupportBean("E1", -1), new SupportBeanVariantStream("E2"), new SupportBean_A("E2")};

        epService.getEPRuntime().sendEvent(events[0]);
        epService.getEPRuntime().sendEvent(events[2]);
        assertFalse(listenerOne.isInvoked());

        epService.getEPRuntime().sendEvent(events[1]);
        epService.getEPRuntime().sendEvent(events[2]);
        assertTrue(listenerOne.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionDynamicMapType(EPServiceProvider epService) {
        Map<String, Object> types = new HashMap<String, Object>();
        types.put("someprop", String.class);

        epService.getEPAdministrator().getConfiguration().addEventType("MyEvent", types);
        epService.getEPAdministrator().getConfiguration().addEventType("MySecondEvent", types);

        ConfigurationVariantStream variant = new ConfigurationVariantStream();
        variant.addEventTypeName("MyEvent");
        variant.addEventTypeName("MySecondEvent");
        epService.getEPAdministrator().getConfiguration().addVariantStream("MyVariant", variant);

        epService.getEPAdministrator().createEPL("insert into MyVariant select * from MyEvent");
        epService.getEPAdministrator().createEPL("insert into MyVariant select * from MySecondEvent");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyVariant");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmt.addListener(listenerOne);
        epService.getEPRuntime().sendEvent(new HashMap(), "MyEvent");
        assertNotNull(listenerOne.assertOneGetNewAndReset());
        epService.getEPRuntime().sendEvent(new HashMap(), "MySecondEvent");
        assertNotNull(listenerOne.assertOneGetNewAndReset());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalidInsertInto(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanVariantStream", SupportBeanVariantStream.class);

        ConfigurationVariantStream variant = new ConfigurationVariantStream();
        variant.addEventTypeName("SupportBean");
        variant.addEventTypeName("SupportBeanVariantStream");
        epService.getEPAdministrator().getConfiguration().addVariantStream("MyVariantStreamFive", variant);

        SupportMessageAssertUtil.tryInvalid(epService, "insert into MyVariantStreamFive select * from " + SupportBean_A.class.getName(),
                "Error starting statement: Selected event type is not a valid event type of the variant stream 'MyVariantStreamFive'");

        SupportMessageAssertUtil.tryInvalid(epService, "insert into MyVariantStreamFive select intPrimitive as k0 from " + SupportBean.class.getName(),
                "Error starting statement: Selected event type is not a valid event type of the variant stream 'MyVariantStreamFive' ");
    }

    private void runAssertionInvalidConfig(EPServiceProvider epService) {
        ConfigurationVariantStream config = new ConfigurationVariantStream();
        tryInvalidConfig(epService, "abc", config, "Invalid variant stream configuration, no event type name has been added and default type variance requires at least one type, for name 'abc'");

        config.addEventTypeName("dummy");
        tryInvalidConfig(epService, "abc", config, "Event type by name 'dummy' could not be found for use in variant stream configuration by name 'abc'");
    }

    private void tryInvalidConfig(EPServiceProvider epService, String name, ConfigurationVariantStream config, String message) {
        try {
            epService.getEPAdministrator().getConfiguration().addVariantStream(name, config);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals(message, ex.getMessage());
        }
    }
}
