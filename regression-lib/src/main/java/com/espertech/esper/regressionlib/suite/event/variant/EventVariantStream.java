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
package com.espertech.esper.regressionlib.suite.event.variant;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;

import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.*;

public class EventVariantStream {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventVariantDynamicMapType());
        execs.add(new EventVariantNamedWin());
        execs.add(new EventVariantSingleColumnConversion());
        execs.add(new EventVariantCoercionBoxedTypeMatch());
        execs.add(new EventVariantSuperTypesInterfaces());
        execs.add(new EventVariantPatternSubquery());
        execs.add(new EventVariantInvalidInsertInto());
        execs.add(new EventVariantSimple("VarStreamABPredefined"));
        execs.add(new EventVariantSimple("VarStreamAny"));
        execs.add(new EventVariantInsertInto());
        execs.add(new EventVariantMetadata());
        execs.add(new EventVariantAnyType());
        execs.add(new EventVariantAnyTypeStaggered());
        execs.add(new EventVariantInsertWrap());
        execs.add(new EventVariantSingleStreamWrap());
        execs.add(new EventVariantWildcardJoin());
        execs.add(new EventVariantWithLateCreateSchema());
        return execs;
    }

    private static class EventVariantWithLateCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variant schema MyVariants as *", path);
            env.compileDeploy("@name('out') select * from MyVariants#length(10)", path);
            env.compileDeploy("@public @buseventtype create map schema SomeEventOne as (id string)", path);
            env.compileDeploy("@public @buseventtype create objectarray schema SomeEventTwo as (id string)", path);
            env.compileDeploy("insert into MyVariants select * from SomeEventOne", path);
            env.compileDeploy("insert into MyVariants select * from SomeEventTwo", path);

            env.sendEventMap(Collections.singletonMap("id", "E1"), "SomeEventOne");
            env.sendEventObjectArray(new Object[] {"E2"}, "SomeEventTwo");
            env.sendEventMap(Collections.singletonMap("id", "E3"), "SomeEventOne");
            env.sendEventObjectArray(new Object[] {"E4"}, "SomeEventTwo");

            env.milestone(0);

            EPAssertionUtil.assertPropsPerRow(env.iterator("out"), "id".split(","), new Object[][] {{"E1"}, {"E2"}, {"E3"}, {"E4"}});

            env.undeployAll();
        }
    }

    private static class EventVariantWildcardJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variant schema MyVariantWJ as *;\n" +
                "insert into MyVariantWJ select * from SupportBean sb unidirectional, SupportBean_S0#keepall as s0;\n" +
                "@name('s0') select * from MyVariantWJ";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(10));
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"sb.theString", "s0.id"}, new Object[]{"E1", 10});

            env.undeployAll();
        }
    }

    private static class EventVariantSingleStreamWrap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variant schema MyVariantSSW as *;\n" +
                "insert into OneStream select *, 'a' as field from SupportBean;\n" +
                "insert into MyVariantSSW select * from OneStream;\n" +
                "@name('s0') select * from MyVariantSSW";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"theString", "field"}, new Object[]{"E1", "a"});

            env.undeployAll();
        }
    }

    public static class EventVariantSimple implements RegressionExecution {
        private final String variantStreamName;

        private EventVariantSimple(String variantStreamName) {
            this.variantStreamName = variantStreamName;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"id"};

            env.milestone(0);

            String epl = "@name('s0') select irstream id? as id from " + variantStreamName + "#length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.compileDeploy("insert into " + variantStreamName + " select * from SupportBean_A");
            env.compileDeploy("insert into " + variantStreamName + " select * from SupportBean_B");

            env.milestone(1);

            env.sendEventBean(new SupportBean_A("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}});

            env.milestone(2);

            env.sendEventBean(new SupportBean_B("E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(3);

            env.sendEventBean(new SupportBean_B("E3"));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"E3"});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"E1"});
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E2"}, {"E3"}});

            env.undeployAll();
        }
    }

    private static class EventVariantDynamicMapType implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("insert into MyVariantTwoTyped select * from MyEvent");
            env.compileDeploy("insert into MyVariantTwoTyped select * from MySecondEvent");
            env.compileDeploy("@name('s0') select * from MyVariantTwoTyped").addListener("s0");

            env.sendEventMap(new HashMap<>(), "MyEvent");
            assertNotNull(env.listener("s0").assertOneGetNewAndReset());

            env.sendEventMap(new HashMap<>(), "MySecondEvent");
            assertNotNull(env.listener("s0").assertOneGetNewAndReset());

            env.undeployAll();
        }
    }

    private static class EventVariantSingleColumnConversion implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into MyVariantTwoTypedSBVariant select * from SupportBean");
            env.compileDeploy("create window MainEventWindow#length(10000) as MyVariantTwoTypedSBVariant", path);
            env.compileDeploy("insert into MainEventWindow select " + EventVariantStream.class.getSimpleName() + ".preProcessEvent(event) from MyVariantTwoTypedSBVariant as event", path);
            env.compileDeploy("@name('s0') select * from MainEventWindow where theString = 'E'", path);
            env.statement("s0").addListenerWithReplay(env.listenerNew());

            env.sendEventBean(new SupportBean("E1", 1));

            env.undeployAll();
        }
    }

    private static class EventVariantCoercionBoxedTypeMatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String fields;
            SupportBean bean;

            env.compileDeploy("@name('s0') select * from MyVariantTwoTypedSB").addListener("s0");
            EventType typeSelectAll = env.statement("s0").getEventType();
            assertEventTypeDefault(typeSelectAll);
            assertEquals(Object.class, env.statement("s0").getEventType().getUnderlyingType());

            env.compileDeploy("insert into MyVariantTwoTypedSB select * from SupportBean");
            env.compileDeploy("insert into MyVariantTwoTypedSB select * from SupportBeanVariantStream");

            // try wildcard
            Object eventOne = new SupportBean("E0", -1);
            env.sendEventBean(eventOne);
            assertSame(eventOne, env.listener("s0").assertOneGetNewAndReset().getUnderlying());

            Object eventTwo = new SupportBeanVariantStream("E1");
            env.sendEventBean(eventTwo);
            assertSame(eventTwo, env.listener("s0").assertOneGetNewAndReset().getUnderlying());

            env.undeployModuleContaining("s0");

            fields = "theString,boolBoxed,intPrimitive,longPrimitive,doublePrimitive,enumValue";
            env.compileDeploy("@name('s0') select " + fields + " from MyVariantTwoTypedSB").addListener("s0");
            assertEventTypeDefault(env.statement("s0").getEventType());

            // coerces to the higher resolution type, accepts boxed versus not boxed
            env.sendEventBean(new SupportBeanVariantStream("s1", true, 1, 20, 30, SupportEnum.ENUM_VALUE_1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields.split(","), new Object[]{"s1", true, 1, 20L, 30d, SupportEnum.ENUM_VALUE_1});

            bean = new SupportBean("s2", 99);
            bean.setLongPrimitive(33);
            bean.setDoublePrimitive(50);
            bean.setEnumValue(SupportEnum.ENUM_VALUE_3);
            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields.split(","), new Object[]{"s2", null, 99, 33L, 50d, SupportEnum.ENUM_VALUE_3});
            env.undeployModuleContaining("s0");

            // make sure a property is not known since the property is not found on SupportBeanVariantStream
            tryInvalidCompile(env, "select charBoxed from MyVariantTwoTypedSB",
                "Failed to validate select-clause expression 'charBoxed': Property named 'charBoxed' is not valid in any stream");

            // try dynamic property: should exists but not show up as a declared property
            fields = "v1,v2,v3";
            env.compileDeploy("@name('s0') select longBoxed? as v1,charBoxed? as v2,doubleBoxed? as v3 from MyVariantTwoTypedSB").addListener("s0");

            bean = new SupportBean();
            bean.setLongBoxed(33L);
            bean.setCharBoxed('a');
            bean.setDoubleBoxed(Double.NaN);
            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields.split(","), new Object[]{33L, 'a', Double.NaN});

            env.sendEventBean(new SupportBeanVariantStream("s2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields.split(","), new Object[]{null, null, null});

            env.undeployAll();
        }
    }

    private static class EventVariantSuperTypesInterfaces implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("insert into MyVariantStreamTwo select * from SupportBeanVariantOne");
            env.compileDeploy("insert into MyVariantStreamTwo select * from SupportBeanVariantTwo");

            env.compileDeploy("@name('s0') select * from MyVariantStreamTwo").addListener("s0");
            EventType eventType = env.statement("s0").getEventType();

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

            env.undeployModuleContaining("s0");

            env.compileDeploy("@name('s0') select p0,p1,p2,p3,p4,p5,indexed[0] as p6,indexArr[1] as p7,mappedKey('a') as p8,inneritem as p9,inneritem.val as p10 from MyVariantStreamTwo");
            env.addListener("s0");
            eventType = env.statement("s0").getEventType();
            assertEquals(Integer.class, eventType.getPropertyType("p6"));
            assertEquals(Integer.class, eventType.getPropertyType("p7"));
            assertEquals(String.class, eventType.getPropertyType("p8"));
            assertEquals(SupportBeanVariantOne.SupportBeanVariantOneInner.class, eventType.getPropertyType("p9"));
            assertEquals(String.class, eventType.getPropertyType("p10"));

            SupportBeanVariantOne ev1 = new SupportBeanVariantOne();
            env.sendEventBean(ev1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p6,p7,p8,p9,p10".split(","), new Object[]{1, 2, "val1", ev1.getInneritem(), ev1.getInneritem().getVal()});

            SupportBeanVariantTwo ev2 = new SupportBeanVariantTwo();
            env.sendEventBean(ev2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p6,p7,p8,p9,p10".split(","), new Object[]{10, 20, "val2", ev2.getInneritem(), ev2.getInneritem().getVal()});

            env.undeployAll();
        }
    }

    private static class EventVariantNamedWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test named window
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('window') create window MyVariantWindow#unique(theString) as select * from MyVariantTwoTypedSB", path);
            env.addListener("window");
            env.compileDeploy("insert into MyVariantWindow select * from MyVariantTwoTypedSB", path);
            env.compileDeploy("insert into MyVariantTwoTypedSB select * from SupportBeanVariantStream", path);
            env.compileDeploy("insert into MyVariantTwoTypedSB select * from SupportBean", path);

            Object eventOne = new SupportBean("E1", -1);
            env.sendEventBean(eventOne);
            assertSame(eventOne, env.listener("window").assertOneGetNewAndReset().getUnderlying());

            env.milestone(0);

            Object eventTwo = new SupportBeanVariantStream("E2");
            env.sendEventBean(eventTwo);
            assertSame(eventTwo, env.listener("window").assertOneGetNewAndReset().getUnderlying());

            env.milestone(1);

            Object eventThree = new SupportBean("E2", -1);
            env.sendEventBean(eventThree);
            assertEquals(eventThree, env.listener("window").getLastNewData()[0].getUnderlying());
            assertEquals(eventTwo, env.listener("window").getLastOldData()[0].getUnderlying());
            env.listener("window").reset();

            env.milestone(2);

            Object eventFour = new SupportBeanVariantStream("E1");
            env.sendEventBean(eventFour);
            assertEquals(eventFour, env.listener("window").getLastNewData()[0].getUnderlying());
            assertEquals(eventOne, env.listener("window").getLastOldData()[0].getUnderlying());
            env.listener("window").reset();

            env.undeployAll();
        }
    }

    private static class EventVariantPatternSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("insert into MyVariantStreamFour select * from SupportBeanVariantStream");
            env.compileDeploy("insert into MyVariantStreamFour select * from SupportBean");

            // test pattern
            env.compileDeploy("@name('s0') select * from pattern [a=MyVariantStreamFour -> b=MyVariantStreamFour]");
            env.addListener("s0");
            Object[] events = {new SupportBean("E1", -1), new SupportBeanVariantStream("E2")};
            env.sendEventBean(events[0]);
            env.sendEventBean(events[1]);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b".split(","), events);
            env.undeployModuleContaining("s0");

            // test subquery
            env.compileDeploy("@name('s0') select * from SupportBean_A as a where exists(select * from MyVariantStreamFour#lastevent as b where b.theString=a.id)");
            env.addListener("s0");
            events = new Object[]{new SupportBean("E1", -1), new SupportBeanVariantStream("E2"), new SupportBean_A("E2")};

            env.sendEventBean(events[0]);
            env.sendEventBean(events[2]);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(events[1]);
            env.sendEventBean(events[2]);
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EventVariantInvalidInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "insert into MyVariantStreamFive select * from SupportBean_A",
                "Selected event type is not a valid event type of the variant stream 'MyVariantStreamFive'");

            tryInvalidCompile(env, "insert into MyVariantStreamFive select intPrimitive as k0 from SupportBean",
                "Selected event type is not a valid event type of the variant stream 'MyVariantStreamFive' ");
        }
    }

    private static class EventVariantInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into MyStream select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("insert into VarStreamAny select theString as abc from MyStream", path);
            env.compileDeploy("@Name('Target') select * from VarStreamAny#keepall()", path);

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            EventBean[] arr = EPAssertionUtil.iteratorToArray(env.iterator("Target"));
            EPAssertionUtil.assertPropsPerRow(arr, new String[]{"abc"}, new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static class EventVariantMetadata implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from VarStreamAny");
            EventType type = env.statement("s0").getEventType();
            env.undeployAll();

            // assert type metadata
            assertEquals(EventTypeApplicationType.VARIANT, type.getMetadata().getApplicationType());
            assertEquals("VarStreamAny", type.getMetadata().getName());
            assertEquals(EventTypeTypeClass.VARIANT, type.getMetadata().getTypeClass());
        }
    }

    private static class EventVariantAnyType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("insert into VarStreamAny select * from SupportBean");
            env.compileDeploy("insert into VarStreamAny select * from SupportBeanVariantStream");
            env.compileDeploy("insert into VarStreamAny select * from SupportBean_A");
            env.compileDeploy("insert into VarStreamAny select symbol as theString, volume as intPrimitive, feed as id from SupportMarketDataBean");
            env.compileDeploy("@name('s0') select * from VarStreamAny").addListener("s0");
            assertEquals(0, env.statement("s0").getEventType().getPropertyNames().length);

            Object eventOne = new SupportBean("E0", -1);
            env.sendEventBean(eventOne);
            assertSame(eventOne, env.listener("s0").assertOneGetNewAndReset().getUnderlying());

            Object eventTwo = new SupportBean_A("E1");
            env.sendEventBean(eventTwo);
            assertSame(eventTwo, env.listener("s0").assertOneGetNewAndReset().getUnderlying());

            env.undeployModuleContaining("s0");

            env.compileDeploy("@name('s0') select theString,id,intPrimitive from VarStreamAny").addListener("s0");

            EventType eventType = env.statement("s0").getEventType();
            assertEquals(Object.class, eventType.getPropertyType("theString"));
            assertEquals(Object.class, eventType.getPropertyType("id"));
            assertEquals(Object.class, eventType.getPropertyType("intPrimitive"));

            String[] fields = "theString,id,intPrimitive".split(",");
            env.sendEventBean(new SupportBeanVariantStream("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", null, null});

            env.sendEventBean(new SupportBean("E2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", null, 10});

            env.sendEventBean(new SupportBean_A("E3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, "E3", null});

            env.sendEventBean(new SupportMarketDataBean("s1", 100, 1000L, "f1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"s1", "f1", 1000L});

            env.undeployAll();
        }
    }

    private static class EventVariantAnyTypeStaggered implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into MyStream select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("insert into VarStreamMD select theString as abc from MyStream", path);
            env.compileDeploy("@Name('Target') select * from VarStreamMD#keepall", path);

            env.sendEventBean(new SupportBean("E1", 1));

            EventBean[] arr = EPAssertionUtil.iteratorToArray(env.iterator("Target"));
            EPAssertionUtil.assertPropsPerRow(arr, new String[]{"abc"}, new Object[][]{{"E1"}});

            env.compileDeploy("insert into MyStream2 select feed from SupportMarketDataBean", path);
            env.compileDeploy("insert into VarStreamMD select feed as abc from MyStream2", path);

            env.sendEventBean(new SupportMarketDataBean("IBM", 1, 1L, "E2"));

            arr = EPAssertionUtil.iteratorToArray(env.iterator("Target"));
            EPAssertionUtil.assertPropsPerRow(arr, new String[]{"abc"}, new Object[][]{{"E1"}, {"E2"}});

            env.undeployAll();
        }
    }

    private static class EventVariantInsertWrap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test inserting a wrapper of underlying plus properties
            env.compileDeploy("insert into VarStreamAny select 'test' as eventConfigId, * from SupportBean");
            env.compileDeploy("@name('s0') select * from VarStreamAny").addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("test", event.get("eventConfigId"));
            assertEquals(1, event.get("intPrimitive"));

            env.undeployAll();
        }
    }

    private static void assertEventTypeDefault(EventType eventType) {
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

    public static Object preProcessEvent(Object o) {
        return new SupportBean("E2", 0);
    }
}

