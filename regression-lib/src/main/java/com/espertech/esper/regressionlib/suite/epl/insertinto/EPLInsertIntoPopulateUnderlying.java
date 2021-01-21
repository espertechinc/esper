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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.*;
import com.espertech.esper.regressionlib.support.bean.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.*;

import static junit.framework.TestCase.fail;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.*;

public class EPLInsertIntoPopulateUnderlying {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoCtor());
        execs.add(new EPLInsertIntoCtorWithPattern());
        execs.add(new EPLInsertIntoBeanJoin());
        execs.add(new EPLInsertIntoPopulateBeanSimple());
        execs.add(new EPLInsertIntoBeanWildcard());
        execs.add(new EPLInsertIntoPopulateBeanObjects());
        execs.add(new EPLInsertIntoPopulateUnderlyingSimple());
        execs.add(new EPLInsertIntoCharSequenceCompat());
        execs.add(new EPLInsertIntoBeanFactoryMethod());
        execs.add(new EPLInsertIntoArrayPOJOInsert());
        execs.add(new EPLInsertIntoArrayMapInsert());
        execs.add(new EPLInsertIntoWindowAggregationAtEventBean());
        execs.add(new EPLInsertIntoInvalid());
        return execs;
    }

    private static class EPLInsertIntoWindowAggregationAtEventBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') insert into SupportBeanArrayEvent select window(*) @eventbean from SupportBean#keepall").addListener("s0");

            SupportBean e1 = new SupportBean("E1", 1);
            env.sendEventBean(e1);
            env.assertEventNew("s0", event -> assertMyEventTargetWithArray(event, e1));

            SupportBean e2 = new SupportBean("E2", 2);
            env.sendEventBean(e2);
            env.assertEventNew("s0", event -> assertMyEventTargetWithArray(event, e1, e2));

            env.undeployAll();
        }

        private static void assertMyEventTargetWithArray(EventBean eventBean, SupportBean... beans) {
            SupportBeanArrayEvent und = (SupportBeanArrayEvent) eventBean.getUnderlying();
            EPAssertionUtil.assertEqualsExactOrder(und.getArray(), beans);
        }
    }

    private static class EPLInsertIntoCtor implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // simple type and null values
            String eplOne = "@name('s0') insert into SupportBeanCtorOne select theString, intBoxed, intPrimitive, boolPrimitive from SupportBean";
            env.compileDeploy(eplOne).addListener("s0");

            sendReceive(env, "E1", 2, true, 100);
            sendReceive(env, "E2", 3, false, 101);
            sendReceive(env, null, 4, true, null);
            env.undeployModuleContaining("s0");

            // boxable type and null values
            String eplTwo = "@name('s0') insert into SupportBeanCtorOne select theString, null, intBoxed from SupportBean";
            env.compileDeploy(eplTwo).addListener("s0");
            sendReceiveTwo(env, "E1", 100);
            env.undeployModuleContaining("s0");

            // test join wildcard
            String eplThree = "@name('s0') insert into SupportBeanCtorTwo select * from SupportBean_ST0#lastevent, SupportBean_ST1#lastevent";
            env.compileDeploy(eplThree).addListener("s0");

            env.sendEventBean(new SupportBean_ST0("ST0", 1));
            env.sendEventBean(new SupportBean_ST1("ST1", 2));
            env.assertEventNew("s0", event -> {
                SupportBeanCtorTwo theEvent = (SupportBeanCtorTwo) event.getUnderlying();
                assertNotNull(theEvent.getSt0());
                assertNotNull(theEvent.getSt1());
            });
            env.undeployModuleContaining("s0");

            // test (should not use column names)
            String eplFour = "@name('s0') insert into SupportBeanCtorOne(theString, intPrimitive) select 'E1', 5 from SupportBean";
            env.compileDeploy(eplFour).addListener("s0");
            env.sendEventBean(new SupportBean("x", -1));
            env.assertEventNew("s0", event -> {
                SupportBeanCtorOne eventOne = (SupportBeanCtorOne) event.getUnderlying();
                assertEquals("E1", eventOne.getTheString());
                assertEquals(99, eventOne.getIntPrimitive());
                assertEquals((Integer) 5, eventOne.getIntBoxed());
            });

            // test Ctor accepting same types
            env.undeployAll();
            String epl = "@name('s0') insert into SupportEventWithCtorSameType select c1,c2 from SupportBean(theString='b1')#lastevent as c1, SupportBean(theString='b2')#lastevent as c2";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("b1", 1));
            env.sendEventBean(new SupportBean("b2", 2));
            env.assertEventNew("s0", event -> {
                SupportEventWithCtorSameType result = (SupportEventWithCtorSameType) event.getUnderlying();
                assertEquals(1, result.getB1().getIntPrimitive());
                assertEquals(2, result.getB2().getIntPrimitive());
            });

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoCtorWithPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test valid case of array insert
            String epl = "@name('s0') insert into SupportBeanCtorThree select s, e FROM PATTERN [" +
                "every s=SupportBean_ST0 -> [2] e=SupportBean_ST1]";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_ST0("E0", 1));
            env.sendEventBean(new SupportBean_ST1("E1", 2));
            env.sendEventBean(new SupportBean_ST1("E2", 3));
            env.assertEventNew("s0", event -> {
                SupportBeanCtorThree three = (SupportBeanCtorThree) event.getUnderlying();
                assertEquals("E0", three.getSt0().getId());
                assertEquals(2, three.getSt1().length);
                assertEquals("E1", three.getSt1()[0].getId());
                assertEquals("E2", three.getSt1()[1].getId());
            });

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoBeanJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportBean_N n1 = new SupportBean_N(1, 10, 100d, 1000d, true, true);
            // test wildcard
            String stmtTextOne = "@name('s0') insert into SupportBeanObject select * from SupportBean_N#lastevent as one, SupportBean_S0#lastevent as two";
            env.compileDeploy(stmtTextOne).addListener("s0");

            env.sendEventBean(n1);
            SupportBean_S0 s01 = new SupportBean_S0(1);
            env.sendEventBean(s01);
            env.assertEventNew("s0", event -> {
                SupportBeanObject theEvent = (SupportBeanObject) event.getUnderlying();
                assertSame(n1, theEvent.getOne());
                assertSame(s01, theEvent.getTwo());
            });
            env.undeployModuleContaining("s0");

            // test select stream names
            stmtTextOne = "@name('s0') insert into SupportBeanObject select one, two from SupportBean_N#lastevent as one, SupportBean_S0#lastevent as two";
            env.compileDeploy(stmtTextOne).addListener("s0");

            env.sendEventBean(n1);
            env.sendEventBean(s01);
            env.assertEventNew("s0", event -> {
                SupportBeanObject theEvent = (SupportBeanObject) event.getUnderlying();
                assertSame(n1, theEvent.getOne());
                assertSame(s01, theEvent.getTwo());
            });
            env.undeployModuleContaining("s0");

            // test fully-qualified class name as target
            stmtTextOne = "@name('s0') insert into SupportBeanObject select one, two from SupportBean_N#lastevent as one, SupportBean_S0#lastevent as two";
            env.compileDeploy(stmtTextOne).addListener("s0");

            env.sendEventBean(n1);
            env.sendEventBean(s01);
            env.assertEventNew("s0", event -> {
                SupportBeanObject theEvent = (SupportBeanObject) event.getUnderlying();
                assertSame(n1, theEvent.getOne());
                assertSame(s01, theEvent.getTwo());
            });
            env.undeployModuleContaining("s0");

            // test local class and auto-import
            stmtTextOne = "@name('s0') insert into " + EPLInsertIntoPopulateUnderlying.class.getName() + "$MyLocalTarget select 1 as value from SupportBean_N";
            env.compileDeploy(stmtTextOne).addListener("s0");
            env.sendEventBean(n1);
            env.assertEventNew("s0", event -> {
                MyLocalTarget eventLocal = (MyLocalTarget) event.getUnderlying();
                assertEquals(1, eventLocal.getValue());
            });
            env.undeployAll();
        }
    }

    private static class EPLInsertIntoInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String text = "insert into SupportBeanCtorOne select 1 from SupportBean";
            env.tryInvalidCompile(text, "Failed to find a suitable constructor for class '" + SupportBeanCtorOne.class.getName() + "': Could not find constructor in class '" + SupportBeanCtorOne.class.getName() + "' with matching parameter number and expected parameter type(s) 'int'");

            text = "insert into SupportBean(intPrimitive) select 1L from SupportBean";
            env.tryInvalidCompile(text, "Invalid assignment of column 'intPrimitive' of type 'long' to event property 'intPrimitive' typed as 'int', column and parameter types mismatch [insert into SupportBean(intPrimitive) select 1L from SupportBean]");

            text = "insert into SupportBean(intPrimitive) select null from SupportBean";
            env.tryInvalidCompile(text, "Invalid assignment of column 'intPrimitive' of null type to event property 'intPrimitive' typed as 'int', nullable type mismatch [insert into SupportBean(intPrimitive) select null from SupportBean]");

            text = "insert into SupportBeanReadOnly select 'a' as geom from SupportBean";
            env.tryInvalidCompile(text, "Failed to find a suitable constructor for class '" + SupportBeanReadOnly.class.getName() + "': Could not find constructor in class '" + SupportBeanReadOnly.class.getName() + "' with matching parameter number and expected parameter type(s) 'String' (nearest matching constructor taking no parameters) [insert into SupportBeanReadOnly select 'a' as geom from SupportBean]");

            text = "insert into SupportBean select 3 as dummyField from SupportBean";
            env.tryInvalidCompile(text, "Column 'dummyField' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?) [insert into SupportBean select 3 as dummyField from SupportBean]");

            text = "insert into SupportBean select 3 from SupportBean";
            env.tryInvalidCompile(text, "Column '3' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?) [insert into SupportBean select 3 from SupportBean]");

            text = "insert into SupportBeanInterfaceProps(isa) select isbImpl from MyMap";
            env.tryInvalidCompile(text, "Invalid assignment of column 'isa' of type '" + ISupportBImpl.class.getName() + "' to event property 'isa' typed as '" + ISupportA.class.getName() + "', column and parameter types mismatch [insert into SupportBeanInterfaceProps(isa) select isbImpl from MyMap]");

            text = "insert into SupportBeanInterfaceProps(isg) select isabImpl from MyMap";
            env.tryInvalidCompile(text, "Invalid assignment of column 'isg' of type '" + ISupportBaseABImpl.class.getName() + "' to event property 'isg' typed as '" + ISupportAImplSuperG.class.getName() + "', column and parameter types mismatch [insert into SupportBeanInterfaceProps(isg) select isabImpl from MyMap]");

            text = "insert into SupportBean(dummy) select 3 from SupportBean";
            env.tryInvalidCompile(text, "Column 'dummy' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?) [insert into SupportBean(dummy) select 3 from SupportBean]");

            text = "insert into SupportBeanReadOnly(side) select 'E1' from MyMap";
            env.tryInvalidCompile(text, "Failed to find a suitable constructor for class '" + SupportBeanReadOnly.class.getName() + "': Could not find constructor in class '" + SupportBeanReadOnly.class.getName() + "' with matching parameter number and expected parameter type(s) 'String' (nearest matching constructor taking no parameters) [insert into SupportBeanReadOnly(side) select 'E1' from MyMap]");

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public insert into ABCStream select *, 1+1 from SupportBean", path);
            text = "@public insert into ABCStream(string) select 'E1' from MyMap";
            env.tryInvalidCompile(path, text, "Event type named 'ABCStream' has already been declared with differing column name or type information: Type by name 'ABCStream' is not a compatible type (target type underlying is '" + Pair.class.getName() + "') [@public insert into ABCStream(string) select 'E1' from MyMap]");

            text = "insert into xmltype select 1 from SupportBean";
            env.tryInvalidCompile(text, "Event type named 'xmltype' has already been declared with differing column name or type information: Type by name 'xmltype' is not a compatible type (target type underlying is '" + Node.class.getName() + "') [insert into xmltype select 1 from SupportBean]");

            text = "insert into MyMap(dummy) select 1 from SupportBean";
            env.tryInvalidCompile(text, "Event type named 'MyMap' has already been declared with differing column name or type information: Type by name 'MyMap' expects 10 properties but receives 1 properties [insert into MyMap(dummy) select 1 from SupportBean]");

            // setter throws exception
            String stmtTextOne = "@name('s0') insert into SupportBeanErrorTestingTwo(value) select 'E1' from MyMap";
            env.compileDeploy(stmtTextOne).addListener("s0");

            try {
                env.sendEventMap(new HashMap(), "MyMap");
                fail();
            } catch (EPException ex) {
                // expected
            }
            env.undeployAll();

            // surprise - wrong type than defined
            stmtTextOne = "@name('s0') insert into SupportBean(intPrimitive) select anint from MyMap";
            env.compileDeploy(stmtTextOne).addListener("s0");
            Map<String, Object> map = new HashMap<>();
            map.put("anint", "notAnInt");
            try {
                env.sendEventBean(map, "MyMap");
                env.assertEqualsNew("s0", "intPrimitive", 0);
            } catch (RuntimeException ex) {
                // an exception is possible and up to the implementation.
            }

            // ctor throws exception
            env.undeployAll();
            String stmtTextThree = "@name('s0') insert into SupportBeanCtorOne select 'E1' from SupportBean";
            env.compileDeploy(stmtTextThree).addListener("s0");
            try {
                env.sendEventBean(new SupportBean("E1", 1));
                fail(); // rethrowing handler registered
            } catch (RuntimeException ex) {
                // expected
            }

            // allow automatic cast of same-type event
            path.clear();
            env.compileDeploy("@public create schema MapOneA as (prop1 string)", path);
            env.compileDeploy("@public create schema MapTwoA as (prop1 string)", path);
            env.compileDeploy("insert into MapOneA select * from MapTwoA", path);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    private static class EPLInsertIntoPopulateBeanSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test select column names
            String stmtTextOne = "@name('i1') insert into SupportBean select " +
                "'E1' as theString, 1 as intPrimitive, 2 as intBoxed, 3L as longPrimitive," +
                "null as longBoxed, true as boolPrimitive, " +
                "'x' as charPrimitive, 0xA as bytePrimitive, " +
                "8.0f as floatPrimitive, 9.0d as doublePrimitive, " +
                "0x05 as shortPrimitive, SupportEnum.ENUM_VALUE_2 as enumValue " +
                " from MyMap";
            env.compileDeploy(stmtTextOne);

            String stmtTextTwo = "@name('s0') select * from SupportBean";
            env.compileDeploy(stmtTextTwo).addListener("s0");

            env.sendEventMap(new HashMap(), "MyMap");
            env.assertEventNew("s0", event -> {
                SupportBean received = (SupportBean) event.getUnderlying();
                assertEquals("E1", received.getTheString());
                SupportBean.compare(received, "intPrimitive,intBoxed,longPrimitive,longBoxed,boolPrimitive,charPrimitive,bytePrimitive,floatPrimitive,doublePrimitive,shortPrimitive,enumValue".split(","),
                    new Object[]{1, 2, 3L, null, true, 'x', (byte) 10, 8f, 9d, (short) 5, SupportEnum.ENUM_VALUE_2});
            });
            // test insert-into column names
            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("i1");

            stmtTextOne = "@name('s0') insert into SupportBean(theString, intPrimitive, intBoxed, longPrimitive," +
                "longBoxed, boolPrimitive, charPrimitive, bytePrimitive, floatPrimitive, doublePrimitive, " +
                "shortPrimitive, enumValue) select " +
                "'E1', 1, 2, 3L," +
                "null, true, " +
                "'x', 0xA, " +
                "8.0f, 9.0d, " +
                "0x05 as shortPrimitive, SupportEnum.ENUM_VALUE_2 " +
                " from MyMap";
            env.compileDeploy(stmtTextOne).addListener("s0");

            env.sendEventMap(new HashMap(), "MyMap");
            env.assertEventNew("s0", event -> {
                SupportBean received = (SupportBean) event.getUnderlying();
                assertEquals("E1", received.getTheString());
                SupportBean.compare(received,
                    "intPrimitive,intBoxed,longPrimitive,longBoxed,boolPrimitive,charPrimitive,bytePrimitive,floatPrimitive,doublePrimitive,shortPrimitive,enumValue".split(","),
                    new Object[]{1, 2, 3L, null, true, 'x', (byte) 10, 8f, 9d, (short) 5, SupportEnum.ENUM_VALUE_2});
            });

            // test convert Integer boxed to Long boxed
            env.undeployModuleContaining("s0");
            stmtTextOne = "@name('s0') insert into SupportBean(longBoxed, doubleBoxed) select intBoxed, floatBoxed from MyMap";
            env.compileDeploy(stmtTextOne).addListener("s0");

            Map<String, Object> vals = new HashMap<>();
            vals.put("intBoxed", 4);
            vals.put("floatBoxed", 0f);
            env.sendEventMap(vals, "MyMap");
            env.assertPropsNew("s0", "longBoxed,doubleBoxed".split(","), new Object[]{4L, 0d});
            env.undeployAll();

            // test new-to-map conversion
            env.compileDeploy("@name('s0') insert into MyEventWithMapFieldSetter(id, themap) " +
                "select 'test' as id, new {somefield = theString} as themap from SupportBean").addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertEventNew("s0", event -> EPAssertionUtil.assertPropsMap((Map) event.get("themap"), "somefield".split(","), "E1"));

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoBeanWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtTextOne = "@name('s0') insert into SupportBean select * from MySupportMap";
            env.compileDeploy(stmtTextOne).addListener("s0");

            Map<String, Object> vals = new HashMap<>();
            vals.put("intPrimitive", 4);
            vals.put("longBoxed", 100L);
            vals.put("theString", "E1");
            vals.put("boolPrimitive", true);

            env.sendEventMap(vals, "MySupportMap");
            env.assertPropsNew("s0",
                "intPrimitive,longBoxed,theString,boolPrimitive".split(","),
                new Object[]{4, 100L, "E1", true});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoPopulateBeanObjects implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // arrays and maps
            String stmtTextOne = "@name('s0') insert into SupportBeanComplexProps(arrayProperty,objectArray,mapProperty) select " +
                "intArr,{10,20,30},mapProp" +
                " from MyMap as m";
            env.compileDeploy(stmtTextOne).addListener("s0");

            Map<String, Object> mymapVals = new HashMap<>();
            mymapVals.put("intArr", new int[]{-1, -2});
            Map<String, Object> inner = new HashMap<>();
            inner.put("mykey", "myval");
            mymapVals.put("mapProp", inner);
            env.sendEventMap(mymapVals, "MyMap");
            env.assertEventNew("s0", event -> {
                SupportBeanComplexProps theEvent = (SupportBeanComplexProps) event.getUnderlying();
                assertEquals(-2, theEvent.getArrayProperty()[1]);
                assertEquals(20, theEvent.getObjectArray()[1]);
                assertEquals("myval", theEvent.getMapProperty().get("mykey"));
            });
            env.undeployModuleContaining("s0");

            // inheritance
            stmtTextOne = "@name('s0') insert into SupportBeanInterfaceProps(isa,isg) select " +
                "isaImpl,isgImpl" +
                " from MyMap";
            env.compileDeploy(stmtTextOne).addListener("s0");

            mymapVals = new HashMap<>();
            mymapVals.put("mapProp", inner);
            env.sendEventMap(mymapVals, "MyMap");
            env.assertEventNew("s0", event -> {
                assertTrue(event.getUnderlying() instanceof SupportBeanInterfaceProps);
                assertEquals(SupportBeanInterfaceProps.class, event.getEventType().getUnderlyingType());
            });
            env.undeployModuleContaining("s0");

            // object values from Map same type
            stmtTextOne = "@name('s0') insert into SupportBeanComplexProps(nested) select nested from MyMap";
            env.compileDeploy(stmtTextOne).addListener("s0");

            mymapVals = new HashMap<>();
            mymapVals.put("nested", new SupportBeanComplexProps.SupportBeanSpecialGetterNested("111", "222"));
            env.sendEventMap(mymapVals, "MyMap");
            env.assertEventNew("s0", event -> {
                SupportBeanComplexProps eventThree = (SupportBeanComplexProps) event.getUnderlying();
                assertEquals("111", eventThree.getNested().getNestedValue());
            });
            env.undeployModuleContaining("s0");

            // object to Object
            stmtTextOne = "@name('s0') insert into SupportBeanArrayCollMap(anyObject) select nested from SupportBeanComplexProps";
            env.compileDeploy(stmtTextOne).addListener("s0");

            env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());
            env.assertEventNew("s0", event -> {
                SupportBeanArrayCollMap eventFour = (SupportBeanArrayCollMap) event.getUnderlying();
                assertEquals("nestedValue", ((SupportBeanComplexProps.SupportBeanSpecialGetterNested) eventFour.getAnyObject()).getNestedValue());
            });
            env.undeployModuleContaining("s0");

            // test null value
            String stmtTextThree = "@name('s0') insert into SupportBean select 'B' as theString, intBoxed as intPrimitive from SupportBean(theString='A')";
            env.compileDeploy(stmtTextThree).addListener("s0");

            env.sendEventBean(new SupportBean("A", 0));
            env.assertEventNew("s0", event -> {
                SupportBean received = (SupportBean) event.getUnderlying();
                assertEquals(0, received.getIntPrimitive());
            });

            SupportBean bean = new SupportBean("A", 1);
            bean.setIntBoxed(20);
            env.sendEventBean(bean);
            env.assertEventNew("s0", event -> {
                SupportBean received = (SupportBean) event.getUnderlying();
                assertEquals(20, received.getIntPrimitive());
            });

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoPopulateUnderlyingSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionPopulateUnderlying(env, "MyMapType");
            tryAssertionPopulateUnderlying(env, "MyOAType");
            tryAssertionPopulateUnderlying(env, "MyAvroType");
        }
    }

    private static class EPLInsertIntoCharSequenceCompat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
                    continue; // Json doesn't allow CharSequence by itself unless registering an adapter
                }
                RegressionPath path = new RegressionPath();
                env.compileDeploy(rep.getAnnotationText() + "create schema ConcreteType as (value java.lang.CharSequence)", path);
                env.compileDeploy("insert into ConcreteType select \"Test\" as value from SupportBean", path);
                env.undeployAll();
            }
        }
    }

    private static class EPLInsertIntoBeanFactoryMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test factory method on the same event class
            String stmtTextOne = "@name('s0') insert into SupportBeanString select 'abc' as theString from MyMap";
            env.compileDeploy(stmtTextOne).addListener("s0").setSubscriber("s0");

            env.sendEventMap(new HashMap(), "MyMap");
            env.assertEventNew("s0", event -> {
                assertEquals("abc", event.get("theString"));
            });
            env.assertSubscriber("s0", subscriber -> assertEquals("abc", subscriber.assertOneGetNewAndReset()));
            env.undeployModuleContaining("s0");

            // test factory method fully-qualified
            stmtTextOne = "@name('s0') insert into SupportSensorEvent(id, type, device, measurement, confidence)" +
                "select 2, 'A01', 'DHC1000', 100, 5 from MyMap";
            env.compileDeploy(stmtTextOne).addListener("s0");

            env.sendEventMap(new HashMap(), "MyMap");
            env.assertPropsNew("s0", "id,type,device,measurement,confidence".split(","), new Object[]{2, "A01", "DHC1000", 100.0, 5.0});

            try {
                SupportBeanString.class.newInstance();
                fail();
            } catch (InstantiationException ex) {
                // expected
            } catch (Exception ex) {
                fail();
            }

            try {
                SupportSensorEvent.class.newInstance();
                fail();
            } catch (IllegalAccessException ex) {
                // expected
            } catch (InstantiationException e) {
                fail();
            }

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoArrayPOJOInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String epl = "@public create schema FinalEventInvalidNonArray as " + FinalEventInvalidNonArray.class.getName() + ";\n" +
                "@public create schema FinalEventInvalidArray as " + FinalEventInvalidArray.class.getName() + ";\n" +
                "@public create schema FinalEventValid as " + FinalEventValid.class.getName() + ";\n";
            env.compileDeploy(epl, path);
            env.advanceTime(0);

            // Test valid case of array insert
            String validEpl = "@name('s0') INSERT INTO FinalEventValid SELECT s as startEvent, e as endEvent FROM PATTERN [" +
                "every s=SupportBean_S0 -> e=SupportBean(theString=s.p00) until timer:interval(10 sec)]";
            env.compileDeploy(validEpl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "G1"));
            env.sendEventBean(new SupportBean("G1", 2));
            env.sendEventBean(new SupportBean("G1", 3));
            env.advanceTime(10000);

            env.assertEventNew("s0", event -> {
                FinalEventValid outEvent = (FinalEventValid) event.getUnderlying();
                assertEquals(1, outEvent.getStartEvent().getId());
                assertEquals("G1", outEvent.getStartEvent().getP00());
                assertEquals(2, outEvent.getEndEvent().length);
                assertEquals(2, outEvent.getEndEvent()[0].getIntPrimitive());
                assertEquals(3, outEvent.getEndEvent()[1].getIntPrimitive());
            });

            // Test invalid case of non-array destination insert
            String invalidEpl = "INSERT INTO FinalEventInvalidNonArray SELECT s as startEvent, e as endEvent FROM PATTERN [" +
                "every s=SupportBean_S0 -> e=SupportBean(theString=s.p00) until timer:interval(10 sec)]";
            env.tryInvalidCompile(path, invalidEpl, "Invalid assignment of column 'endEvent' of type '" + SupportBean.class.getName() + "[]' to event property 'endEvent' typed as '" + SupportBean.class.getName() + "', column and parameter types mismatch [INSERT INTO FinalEventInvalidNonArray SELECT s as startEvent, e as endEvent FROM PATTERN [every s=SupportBean_S0 -> e=SupportBean(theString=s.p00) until timer:interval(10 sec)]]");

            // Test invalid case of array destination insert from non-array var
            String invalidEplTwo = "INSERT INTO FinalEventInvalidArray SELECT s as startEvent, e as endEvent FROM PATTERN [" +
                "every s=SupportBean_S0 -> e=SupportBean(theString=s.p00) until timer:interval(10 sec)]";
            env.tryInvalidCompile(path, invalidEplTwo, "Invalid assignment of column 'startEvent' of type '" + SupportBean_S0.class.getName() + "' to event property 'startEvent' typed as '" + SupportBean_S0.class.getName() + "[]', column and parameter types mismatch [INSERT INTO FinalEventInvalidArray SELECT s as startEvent, e as endEvent FROM PATTERN [every s=SupportBean_S0 -> e=SupportBean(theString=s.p00) until timer:interval(10 sec)]]");

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoArrayMapInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                if (rep.isAvroEvent()) {
                    env.assertThat(() -> {
                        tryAssertionArrayMapInsert(env, rep);
                    });
                } else {
                    tryAssertionArrayMapInsert(env, rep);
                }
            }
        }
    }

    private static void tryAssertionArrayMapInsert(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        RegressionPath path = new RegressionPath();
        String schema =
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedEventOne.class) + " @public @buseventtype create schema EventOne(id string);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedEventTwo.class) + " @public @buseventtype create schema EventTwo(id string, val int);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedFinalEventValid.class) + " @public @buseventtype create schema FinalEventValid (startEvent EventOne, endEvent EventTwo[]);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedFinalEventInvalidNonArray.class) + " @public @buseventtype create schema FinalEventInvalidNonArray (startEvent EventOne, endEvent EventTwo);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedFinalEventInvalidArray.class) + " @public @buseventtype create schema FinalEventInvalidArray (startEvent EventOne, endEvent EventTwo);\n";
        env.compileDeploy(schema, path);

        env.advanceTime(0);

        // Test valid case of array insert
        String validEpl = "@name('s0') INSERT INTO FinalEventValid SELECT s as startEvent, e as endEvent FROM PATTERN [" +
            "every s=EventOne -> e=EventTwo(id=s.id) until timer:interval(10 sec)]";
        env.compileDeploy(validEpl, path).addListener("s0");

        sendEventOne(env, eventRepresentationEnum, "G1");
        sendEventTwo(env, eventRepresentationEnum, "G1", 2);
        sendEventTwo(env, eventRepresentationEnum, "G1", 3);
        env.advanceTime(10000);

        env.assertEventNew("s0", event -> {
            EventBean startEventOne;
            EventBean endEventOne;
            EventBean endEventTwo;
            if (eventRepresentationEnum.isObjectArrayEvent()) {
                Object[] outArray = (Object[]) event.getUnderlying();
                startEventOne = (EventBean) outArray[0];
                endEventOne = ((EventBean[]) outArray[1])[0];
                endEventTwo = ((EventBean[]) outArray[1])[1];
            } else if (eventRepresentationEnum.isMapEvent()) {
                Map outMap = (Map) event.getUnderlying();
                startEventOne = (EventBean) outMap.get("startEvent");
                endEventOne = ((EventBean[]) outMap.get("endEvent"))[0];
                endEventTwo = ((EventBean[]) outMap.get("endEvent"))[1];
            } else if (eventRepresentationEnum.isAvroEvent() || eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                startEventOne = (EventBean) event.getFragment("startEvent");
                EventBean[] endEvents = (EventBean[]) event.getFragment("endEvent");
                endEventOne = endEvents[0];
                endEventTwo = endEvents[1];
            } else {
                throw new IllegalStateException("Unrecognized enum " + eventRepresentationEnum);
            }
            assertEquals("G1", startEventOne.get("id"));
            assertEquals(2, endEventOne.get("val"));
            assertEquals(3, endEventTwo.get("val"));
        });

        // Test invalid case of non-array destination insert
        String invalidEplOne = "INSERT INTO FinalEventInvalidNonArray SELECT s as startEvent, e as endEvent FROM PATTERN [" +
            "every s=EventOne -> e=EventTwo(id=s.id) until timer:interval(10 sec)]";
        env.assertThat(() -> {
            try {
                env.compileWCheckedEx(invalidEplOne, path);
                fail();
            } catch (EPCompileException ex) {
                String expected;
                if (eventRepresentationEnum.isAvroEvent()) {
                    expected = "Property 'endEvent' is incompatible, expecting an array of compatible schema 'EventTwo' but received schema 'EventTwo'";
                } else {
                    expected = "Event type named 'FinalEventInvalidNonArray' has already been declared with differing column name or type information: Type by name 'FinalEventInvalidNonArray' in property 'endEvent' expected event type 'EventTwo' but receives event type array 'EventTwo'";
                }
                SupportMessageAssertUtil.assertMessage(ex, expected);
            }
        });

        // Test invalid case of array destination insert from non-array var
        String invalidEplTwo = "INSERT INTO FinalEventInvalidArray SELECT s as startEvent, e as endEvent FROM PATTERN [" +
            "every s=EventOne -> e=EventTwo(id=s.id) until timer:interval(10 sec)]";
        env.assertThat(() -> {
            try {
                env.compileWCheckedEx(invalidEplTwo, path);
                fail();
            } catch (EPCompileException ex) {
                String expected;
                if (eventRepresentationEnum.isAvroEvent()) {
                    expected = "Property 'endEvent' is incompatible, expecting an array of compatible schema 'EventTwo' but received schema 'EventTwo'";
                } else {
                    expected = "Event type named 'FinalEventInvalidArray' has already been declared with differing column name or type information: Type by name 'FinalEventInvalidArray' in property 'endEvent' expected event type 'EventTwo' but receives event type array 'EventTwo'";
                }
                SupportMessageAssertUtil.assertMessage(ex, expected);
            }
        });

        env.undeployAll();
    }

    private static void sendEventTwo(RegressionEnvironment env, EventRepresentationChoice
        eventRepresentationEnum, String id, int val) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{id, val}, "EventTwo");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
            theEvent.put("id", id);
            theEvent.put("val", val);
            env.sendEventMap(theEvent, "EventTwo");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = record("name").fields().requiredString("id").requiredInt("val").endRecord();
            GenericData.Record record = new GenericData.Record(schema);
            record.put("id", id);
            record.put("val", val);
            env.sendEventAvro(record, "EventTwo");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("id", id);
            object.add("val", val);
            env.sendEventJson(object.toString(), "EventTwo");
        } else {
            fail();
        }
    }

    private static void sendEventOne(RegressionEnvironment env, EventRepresentationChoice
        eventRepresentationEnum, String id) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{id}, "EventOne");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
            theEvent.put("id", id);
            env.sendEventMap(theEvent, "EventOne");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = record("name").fields().requiredString("id").endRecord();
            GenericData.Record record = new GenericData.Record(schema);
            record.put("id", id);
            env.sendEventAvro(record, "EventOne");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("id", id);
            env.sendEventJson(object.toString(), "EventOne");
        } else {
            fail();
        }
    }

    public static class FinalEventInvalidNonArray {
        private SupportBean_S0 startEvent;
        private SupportBean endEvent;

        public SupportBean_S0 getStartEvent() {
            return startEvent;
        }

        public void setStartEvent(SupportBean_S0 startEvent) {
            this.startEvent = startEvent;
        }

        public SupportBean getEndEvent() {
            return endEvent;
        }

        public void setEndEvent(SupportBean endEvent) {
            this.endEvent = endEvent;
        }
    }

    public static class FinalEventInvalidArray {
        private SupportBean_S0[] startEvent;
        private SupportBean[] endEvent;

        public SupportBean_S0[] getStartEvent() {
            return startEvent;
        }

        public void setStartEvent(SupportBean_S0[] startEvent) {
            this.startEvent = startEvent;
        }

        public SupportBean[] getEndEvent() {
            return endEvent;
        }

        public void setEndEvent(SupportBean[] endEvent) {
            this.endEvent = endEvent;
        }
    }

    public static class FinalEventValid {
        private SupportBean_S0 startEvent;
        private SupportBean[] endEvent;

        public SupportBean_S0 getStartEvent() {
            return startEvent;
        }

        public void setStartEvent(SupportBean_S0 startEvent) {
            this.startEvent = startEvent;
        }

        public SupportBean[] getEndEvent() {
            return endEvent;
        }

        public void setEndEvent(SupportBean[] endEvent) {
            this.endEvent = endEvent;
        }
    }

    public static class MyLocalTarget {
        public int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    private static void sendReceiveTwo(RegressionEnvironment env, String
        theString, Integer intBoxed) {
        SupportBean bean = new SupportBean(theString, -1);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
        env.assertEventNew("s0", event -> {
            SupportBeanCtorOne theEvent = (SupportBeanCtorOne) event.getUnderlying();
            assertEquals(theString, theEvent.getTheString());
            assertEquals(null, theEvent.getIntBoxed());
            assertEquals(intBoxed, (Integer) theEvent.getIntPrimitive());
        });
    }

    private static void sendReceive(RegressionEnvironment env, String theString,
                                    int intPrimitive, boolean boolPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setBoolPrimitive(boolPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
        env.assertEventNew("s0", event -> {
            SupportBeanCtorOne theEvent = (SupportBeanCtorOne) event.getUnderlying();
            assertEquals(theString, theEvent.getTheString());
            assertEquals(intBoxed, theEvent.getIntBoxed());
            assertEquals(boolPrimitive, theEvent.isBoolPrimitive());
            assertEquals(intPrimitive, theEvent.getIntPrimitive());
        });
    }

    private static void tryAssertionPopulateUnderlying(RegressionEnvironment env, String typeName) {
        env.compileDeploy("@name('select') select * from " + typeName);

        String stmtTextOne = "@name('s0') insert into " + typeName + " select intPrimitive as intVal, theString as stringVal, doubleBoxed as doubleVal from SupportBean";
        env.compileDeploy(stmtTextOne).addListener("s0");

        env.assertThat(() -> assertSame(env.statement("select").getEventType(), env.statement("s0").getEventType()));

        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(1000);
        bean.setTheString("E1");
        bean.setDoubleBoxed(1001d);
        env.sendEventBean(bean);

        env.assertPropsNew("s0", "intVal,stringVal,doubleVal".split(","), new Object[]{1000, "E1", 1001d});
        env.undeployAll();
    }

    public static class MyLocalJsonProvidedEventOne implements Serializable {
        public String id;
    }

    public static class MyLocalJsonProvidedEventTwo implements Serializable {
        public String id;
        public int val;
    }

    public static class MyLocalJsonProvidedFinalEventValid implements Serializable {
        public MyLocalJsonProvidedEventOne startEvent;
        public MyLocalJsonProvidedEventTwo[] endEvent;
    }

    public static class MyLocalJsonProvidedFinalEventInvalidNonArray implements Serializable {
        public MyLocalJsonProvidedEventOne startEvent;
        public MyLocalJsonProvidedEventTwo endEvent;
    }

    public static class MyLocalJsonProvidedFinalEventInvalidArray implements Serializable {
        public MyLocalJsonProvidedEventOne startEvent;
        public MyLocalJsonProvidedEventTwo endEvent;
    }
}
