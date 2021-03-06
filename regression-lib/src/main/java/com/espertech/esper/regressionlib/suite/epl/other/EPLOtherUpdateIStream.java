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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.soda.Expressions;
import com.espertech.esper.common.client.soda.UpdateClause;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanCopyMethod;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.util.*;

import static org.apache.avro.SchemaBuilder.*;
import static org.junit.Assert.*;

public class EPLOtherUpdateIStream {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherUpdateBean());
        execs.add(new EPLOtherUpdateFieldUpdateOrder());
        execs.add(new EPLOtherUpdateInvalid());
        execs.add(new EPLOtherUpdateInsertIntoWBeanWhere());
        execs.add(new EPLOtherUpdateInsertIntoWMapNoWhere());
        execs.add(new EPLOtherUpdateFieldsWithPriority());
        execs.add(new EPLOtherUpdateNamedWindow());
        execs.add(new EPLOtherUpdateTypeWidener());
        execs.add(new EPLOtherUpdateInsertDirectBeanTypeInheritance());
        execs.add(new EPLOtherUpdateSODA());
        execs.add(new EPLOtherUpdateXMLEvent());
        execs.add(new EPLOtherUpdateWrappedObject());
        execs.add(new EPLOtherUpdateSendRouteSenderPreprocess());
        execs.add(new EPLOtherUpdateCopyMethod());
        execs.add(new EPLOtherUpdateSubquery());
        execs.add(new EPLOtherUpdateUnprioritizedOrder());
        execs.add(new EPLOtherUpdateListenerDeliveryMultiupdate());
        execs.add(new EPLOtherUpdateListenerDeliveryMultiupdateMixed());
        execs.add(new EPLOtherUpdateSubqueryMultikeyWArray());
        execs.add(new EPLOtherUpdateMapSetMapPropsBean());
        execs.add(new EPLOtherUpdateMapSetMapPropsRep());
        execs.add(new EPLOtherUpdateNWSetMapProps());
        execs.add(new EPLOtherUpdateArrayElement());
        execs.add(new EPLOtherUpdateArrayElementBoxed());
        execs.add(new EPLOtherUpdateArrayElementInvalid());
        execs.add(new EPLOtherUpdateExpression());
        execs.add(new EPLOtherUpdateIStreamEnumAnyOf());
        return execs;
    }

    private static class EPLOtherUpdateIStreamEnumAnyOf implements RegressionExecution {

        @Override
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema MyEvent as " + SupportEventWithListOfObject.class.getName() + ";\n" +
                    "@name('update') update istream MyEvent set updated = true where mylist.anyOf(e -> e is not null); \n" +
                    "@name('s0') select updated from MyEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEventWithListOfObject(Arrays.asList("first", "second")), "MyEvent");
            env.assertEqualsNew("s0", "updated", true);

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateArrayElementInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplSchema = "@name('create') @public create schema MySchema(doublearray double[primitive], intarray int[primitive], notAnArray int)";
            env.compile(eplSchema, path);

            // invalid property
            env.tryInvalidCompile(path, "update istream MySchema set c1[0]=1",
                    "Failed to validate assignment expression 'c1[0]=1': Property 'c1[0]' is not available for write access");

            // index expression is not Integer
            env.tryInvalidCompile(path, "update istream MySchema set doublearray[null]=1",
                    "Incorrect index expression for array operation, expected an expression returning an integer value but the expression 'null' returns 'null' for expression 'doublearray'");

            // type incompatible cannot assign
            env.tryInvalidCompile(path, "update istream MySchema set intarray[notAnArray]='x'",
                    "Failed to validate assignment expression 'intarray[notAnArray]=\"x\"': Invalid assignment of column '\"x\"' of type 'String' to event property 'intarray' typed as 'int', column and parameter types mismatch");

            // not-an-array
            env.tryInvalidCompile(path, "update istream MySchema set notAnArray[notAnArray]=1",
                    "Failed to validate assignment expression 'notAnArray[notAnArray]=1': Property 'notAnArray' type is not array");

            // not found
            env.tryInvalidCompile(path, "update istream MySchema set dummy[intPrimitive]=1",
                    "Failed to validate update assignment expression 'intPrimitive': Property named 'intPrimitive' is not valid in any stream");

            path.clear();

            // runtime-behavior for index-overflow and null-array and null-index and
            String epl = "@name('create') @public @buseventtype create schema MySchema(doublearray double[primitive], indexvalue int, rhsvalue int);\n" +
                    "update istream MySchema set doublearray[indexvalue]=rhsvalue;\n";
            env.compileDeploy(epl);

            // index returned is too large
            try {
                env.sendEventMap(CollectionUtil.buildMap("doublearray", new double[3], "indexvalue", 10, "rhsvalue", 1), "MySchema");
                fail();
            } catch (RuntimeException ex) {
                assertTrue(ex.getMessage().contains("Array length 3 less than index 10 for property 'doublearray'"));
            }

            // index returned null
            env.sendEventMap(CollectionUtil.buildMap("doublearray", new double[3], "indexvalue", null, "rhsvalue", 1), "MySchema");

            // rhs returned null for array-of-primitive
            env.sendEventMap(CollectionUtil.buildMap("doublearray", new double[3], "indexvalue", 1, "rhsvalue", null), "MySchema");

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "@public @buseventtype create map schema MyEvent(a int, b int);\n" +
                            "inlined_class \"\"\"\n" +
                            "  public class Helper {\n" +
                            "    public static void swap(java.util.Map event) {\n" +
                            "      Object temp = event.get(\"a\");\n" +
                            "      event.put(\"a\", event.get(\"b\"));\n" +
                            "      event.put(\"b\", temp);\n" +
                            "    }\n" +
                            "  }\n" +
                            "\"\"\"\n" +
                            "update istream MyEvent as me set Helper.swap(me);\n" +
                            "@name('s0') select * from MyEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventMap(CollectionUtil.buildMap("a", 1, "b", 10), "MyEvent");
            env.assertPropsNew("s0", "a,b".split(","), new Object[]{10, 1});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateArrayElementBoxed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                    "@public @buseventtype create schema MyEvent(dbls double[]);\n" +
                            "update istream MyEvent set dbls[3-2] = 1;\n" +
                            "@name('s0') select dbls as c0 from MyEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventMap(Collections.singletonMap("dbls", new Double[3]), "MyEvent");
            env.assertEventNew("s0", event -> assertArrayEquals(new Double[]{null, 1d, null}, (Double[]) event.get("c0")));

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateArrayElement implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema Arriving(position int, intarray int[], objectarray java.lang.Object[]);\n" +
                    "update istream Arriving set intarray[position] = 1, objectarray[position] = 1;\n" +
                    "@name('s0') select * from Arriving;\n";
            env.compileDeploy(epl).addListener("s0");

            assertUpdate(env, 1, new int[]{0, 1, 0}, new Object[]{null, 1, null});
            assertUpdate(env, 0, new int[]{1, 0, 0}, new Object[]{1, null, null});
            assertUpdate(env, 2, new int[]{0, 0, 1}, new Object[]{null, null, 1});

            env.undeployAll();
        }

        private void assertUpdate(RegressionEnvironment env, int position, int[] expectedInt, Object[] expectedObj) {
            env.sendEventMap(CollectionUtil.buildMap("position", position, "intarray", new int[3], "objectarray", new Object[3]), "Arriving");
            env.assertPropsNew("s0", "position,intarray,objectarray".split(","), new Object[]{position, expectedInt, expectedObj});
        }
    }

    private static class EPLOtherUpdateSubqueryMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema Arriving(value int);\n" +
                    "update istream Arriving set value = (select sum(value) as c0 from SupportEventWithIntArray#keepall group by array);\n" +
                    "@name('s0') select * from Arriving;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEventWithIntArray("E1", new int[]{1, 2}, 10));
            env.sendEventBean(new SupportEventWithIntArray("E2", new int[]{1, 2}, 11));

            env.milestone(0);
            assertUpdate(env, 21);

            env.sendEventBean(new SupportEventWithIntArray("E3", new int[]{1, 2}, 12));
            assertUpdate(env, 33);

            env.milestone(1);

            env.sendEventBean(new SupportEventWithIntArray("E4", new int[]{1}, 13));
            assertUpdate(env, null);

            env.undeployAll();
        }

        private void assertUpdate(RegressionEnvironment env, Integer expected) {
            env.sendEventMap(new HashMap<>(), "Arriving");
            env.assertEqualsNew("s0", "value", expected);
        }
    }

    public static class EPLOtherUpdateBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String text = "@Name('Insert') @public insert into MyStream select * from SupportBean";
            env.compileDeploy(text, path).addListener("Insert");

            text = "@Name('Update') update istream MyStream set intPrimitive=10, theString='O_' || theString where intPrimitive=1";
            env.compileDeploy(text, path).addListener("Update");

            text = "@Name('Select') select * from MyStream";
            env.compileDeploy(text, path).addListener("Select");

            String[] fields = "theString,intPrimitive".split(",");
            env.sendEventBean(new SupportBean("E1", 9));
            env.assertPropsNew("Select", fields, new Object[]{"E1", 9});
            env.assertPropsNew("Insert", fields, new Object[]{"E1", 9});
            env.assertListenerNotInvoked("Update");

            env.sendEventBean(new SupportBean("E2", 1));
            env.assertPropsNew("Select", fields, new Object[]{"O_E2", 10});
            env.assertPropsNew("Insert", fields, new Object[]{"E2", 1});
            env.assertPropsIRPair("Update", fields, new Object[]{"O_E2", 10}, new Object[]{"E2", 1});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 1));
            env.assertPropsNew("Select", fields, new Object[]{"O_E3", 10});
            env.assertPropsNew("Insert", fields, new Object[]{"E3", 1});
            env.assertPropsIRPair("Update", fields, new Object[]{"O_E3", 10}, new Object[]{"E3", 1});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateFieldUpdateOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('update') update istream SupportBean " +
                    "set intPrimitive=myvar, intBoxed=intPrimitive");
            env.assertStatement("update", statement -> assertEquals(StatementType.UPDATE, statement.getProperty(StatementProperty.STATEMENTTYPE)));

            env.compileDeploy("@name('s0') select * from SupportBean").addListener("s0");
            String[] fields = "intPrimitive,intBoxed".split(",");

            env.sendEventBean(makeSupportBean("E1", 1, 2));
            env.assertPropsNew("s0", fields, new Object[]{10, 1});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public insert into SupportBeanStream select * from SupportBean", path);
            env.compileDeploy("@public insert into SupportBeanStreamTwo select * from pattern[a=SupportBean -> b=SupportBean]", path);
            env.compileDeploy("@public insert into SupportBeanStreamRO select * from SupportBeanReadOnly", path);

            env.tryInvalidCompile(path, "update istream SupportBeanStream set intPrimitive=longPrimitive",
                    "Failed to validate assignment expression 'intPrimitive=longPrimitive': Invalid assignment of column 'longPrimitive' of type 'Long' to event property 'intPrimitive' typed as 'int', column and parameter types mismatch [update istream SupportBeanStream set intPrimitive=longPrimitive]");
            env.tryInvalidCompile(path, "update istream SupportBeanStream set xxx='abc'",
                    "Failed to validate assignment expression 'xxx=\"abc\"': Property 'xxx' is not available for write access [update istream SupportBeanStream set xxx='abc']");
            env.tryInvalidCompile(path, "update istream SupportBeanStream set intPrimitive=null",
                    "Failed to validate assignment expression 'intPrimitive=null': Invalid assignment of column 'null' of null type to event property 'intPrimitive' typed as 'int', nullable type mismatch [update istream SupportBeanStream set intPrimitive=null]");
            env.tryInvalidCompile(path, "update istream SupportBeanStreamTwo set a.intPrimitive=10",
                    "Failed to validate assignment expression 'a.intPrimitive=10': Property 'a.intPrimitive' is not available for write access [update istream SupportBeanStreamTwo set a.intPrimitive=10]");
            env.tryInvalidCompile(path, "update istream SupportBeanStreamRO set side='a'",
                    "Failed to validate assignment expression 'side=\"a\"': Property 'side' is not available for write access [update istream SupportBeanStreamRO set side='a']");
            env.tryInvalidCompile(path, "update istream SupportBean set longPrimitive=sum(intPrimitive)",
                    "Aggregation functions may not be used within update-set [update istream SupportBean set longPrimitive=sum(intPrimitive)]");
            env.tryInvalidCompile(path, "update istream SupportBean set longPrimitive=longPrimitive where sum(intPrimitive) = 1",
                    "Aggregation functions may not be used within an update-clause [update istream SupportBean set longPrimitive=longPrimitive where sum(intPrimitive) = 1]");
            env.tryInvalidCompile(path, "update istream SupportBean set longPrimitive=prev(1, longPrimitive)",
                    "Failed to validate update assignment expression 'prev(1,longPrimitive)': Previous function cannot be used in this context [update istream SupportBean set longPrimitive=prev(1, longPrimitive)]");
            env.tryInvalidCompile(path, "update istream MyXmlEvent set abc=1",
                    "Failed to validate assignment expression 'abc=1': Property 'abc' is not available for write access [update istream MyXmlEvent set abc=1]");
            env.tryInvalidCompile(path, "update istream SupportBeanErrorTestingOne set value='1'",
                    "The update-clause requires the underlying event representation to support copy (via Serializable by default) [update istream SupportBeanErrorTestingOne set value='1']");
            env.tryInvalidCompile(path, "update istream SupportBean set longPrimitive=(select p0 from MyMapTypeInv#lastevent where theString=p3)",
                    "Failed to plan subquery number 1 querying MyMapTypeInv: Failed to validate filter expression 'theString=p3': Property named 'theString' must be prefixed by a stream name, use the stream name itself or use the as-clause to name the stream with the property in the format \"stream.property\" [update istream SupportBean set longPrimitive=(select p0 from MyMapTypeInv#lastevent where theString=p3)]");
            env.tryInvalidCompile(path, "update istream XYZ.GYH set a=1",
                    "Failed to resolve event type, named window or table by name 'XYZ.GYH' [update istream XYZ.GYH set a=1]");

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateInsertIntoWBeanWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('insert') @public insert into MyStreamBW select * from SupportBean", path);
            env.addListener("insert");

            env.compileDeploy("@name('update_1') update istream MyStreamBW set intPrimitive=10, theString='O_' || theString where intPrimitive=1", path);
            env.addListener("update_1");

            env.compileDeploy("@name('s0') select * from MyStreamBW", path);
            env.addListener("s0");

            String[] fields = "theString,intPrimitive".split(",");
            env.sendEventBean(new SupportBean("E1", 9));
            env.assertPropsNew("s0", fields, new Object[]{"E1", 9});
            env.assertPropsNew("insert", fields, new Object[]{"E1", 9});
            env.assertListenerNotInvoked("update_1");

            env.sendEventBean(new SupportBean("E2", 1));
            env.assertPropsNew("s0", fields, new Object[]{"O_E2", 10});
            env.assertPropsNew("insert", fields, new Object[]{"E2", 1});
            env.assertPropsIRPair("update_1", fields, new Object[]{"O_E2", 10}, new Object[]{"E2", 1});
            env.listenerReset("update_1");

            env.sendEventBean(new SupportBean("E3", 2));
            env.assertPropsNew("s0", fields, new Object[]{"E3", 2});
            env.assertPropsNew("insert", fields, new Object[]{"E3", 2});
            env.assertListenerNotInvoked("update_1");

            env.sendEventBean(new SupportBean("E4", 1));
            env.assertPropsNew("s0", fields, new Object[]{"O_E4", 10});
            env.assertPropsNew("insert", fields, new Object[]{"E4", 1});
            env.assertPropsIRPair("update_1", fields, new Object[]{"O_E4", 10}, new Object[]{"E4", 1});

            env.compileDeploy("@name('update_2') update istream MyStreamBW as xyz set intPrimitive=xyz.intPrimitive + 1000 where intPrimitive=2", path);
            env.addListener("update_2");

            env.sendEventBean(new SupportBean("E5", 2));
            env.assertPropsNew("s0", fields, new Object[]{"E5", 1002});
            env.assertPropsNew("insert", fields, new Object[]{"E5", 2});
            env.assertPropsIRPair("update_2", fields, new Object[]{"E5", 1002}, new Object[]{"E5", 2});

            env.undeployModuleContaining("update_1");

            env.sendEventBean(new SupportBean("E6", 1));
            env.assertPropsNew("s0", fields, new Object[]{"E6", 1});
            env.assertPropsNew("insert", fields, new Object[]{"E6", 1});
            env.assertListenerNotInvoked("update_2");

            env.sendEventBean(new SupportBean("E7", 2));
            env.assertPropsNew("s0", fields, new Object[]{"E7", 1002});
            env.assertPropsNew("insert", fields, new Object[]{"E7", 2});
            env.assertPropsIRPair("update_2", fields, new Object[]{"E7", 1002}, new Object[]{"E7", 2});
            env.listenerReset("update_2");
            env.assertIterator("update_2", iterator -> assertFalse(iterator.hasNext()));

            env.sendEventBean(new SupportBean("E8", 2));
            env.assertPropsNew("s0", fields, new Object[]{"E8", 1002});
            env.assertPropsNew("insert", fields, new Object[]{"E8", 2});

            env.setSubscriber("update_2");

            env.sendEventBean(new SupportBean("E9", 2));
            env.assertPropsNew("s0", fields, new Object[]{"E9", 1002});
            env.assertPropsNew("insert", fields, new Object[]{"E9", 2});
            env.assertSubscriber("update_2", subscriber -> {
                SupportBean.compare(subscriber.getOldDataListFlattened()[0], "E9", 2);
                SupportBean.compare(subscriber.getNewDataListFlattened()[0], "E9", 1002);
                subscriber.reset();
            });

            env.undeployModuleContaining("update_2");

            env.sendEventBean(new SupportBean("E10", 2));
            env.assertPropsNew("s0", fields, new Object[]{"E10", 2});
            env.assertPropsNew("insert", fields, new Object[]{"E10", 2});

            env.compileDeploy("@name('update_3') update istream MyStreamBW set intPrimitive=intBoxed", path);
            env.addListener("update_3");

            env.sendEventBean(new SupportBean("E11", 2));
            env.assertListener("update_3", listener -> EPAssertionUtil.assertProps(listener.assertOneGetNew(), fields, new Object[]{"E11", 2}));

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateInsertIntoWMapNoWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('insert') @public insert into MyStreamII select * from MyMapTypeII", path).addListener("insert");

            EPCompiled update = env.compile("@name('update') update istream MyStreamII set p0=p1, p1=p0", path);
            env.deploy(update);

            env.compileDeploy("@name('s0') select * from MyStreamII", path).addListener("s0");

            String[] fields = "p0,p1,p2".split(",");
            env.sendEventMap(makeMap("p0", 10L, "p1", 1L, "p2", 100L), "MyMapTypeII");
            env.assertPropsNew("s0", fields, new Object[]{1L, 10L, 100L});
            env.assertPropsNew("insert", fields, new Object[]{10L, 1L, 100L});

            env.undeployModuleContaining("update");
            env.deploy(update).addListener("update");

            env.sendEventMap(makeMap("p0", 5L, "p1", 4L, "p2", 101L), "MyMapTypeII");
            env.assertPropsNew("s0", fields, new Object[]{4L, 5L, 101L});
            env.assertPropsNew("insert", fields, new Object[]{5L, 4L, 101L});

            env.undeployModuleContaining("update");

            env.sendEventMap(makeMap("p0", 20L, "p1", 0L, "p2", 102L), "MyMapTypeII");
            env.assertPropsNew("s0", fields, new Object[]{20L, 0L, 102L});
            env.assertPropsNew("insert", fields, new Object[]{20L, 0L, 102L});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateFieldsWithPriority implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionFieldsWithPriority(env, rep);
            }
        }
    }

    private static class EPLOtherUpdateInsertDirectBeanTypeInheritance implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                    "@public create schema BaseInterface as " + BaseInterface.class.getName() + ";\n" +
                            "@public create schema BaseOne as " + BaseOne.class.getName() + ";\n" +
                            "@public create schema BaseOneA as " + BaseOneA.class.getName() + ";\n" +
                            "@public create schema BaseOneB as " + BaseOneB.class.getName() + ";\n" +
                            "@public create schema BaseTwo as " + BaseTwo.class.getName() + ";\n";
            env.compileDeploy(epl, path);

            // test update applies to child types via interface
            env.compileDeploy("@name('insert') insert into BaseOne select p0 as i, p1 as p from MyMapTypeIDB", path);
            env.compileDeploy("@Name('a') update istream BaseInterface set i='XYZ' where i like 'E%'", path);
            env.compileDeploy("@name('s0') select * from BaseOne", path).addListener("s0");

            String[] fields = "i,p".split(",");
            env.sendEventMap(makeMap("p0", "E1", "p1", "E1"), "MyMapTypeIDB");
            env.assertPropsNew("s0", fields, new Object[]{"XYZ", "E1"});

            env.sendEventMap(makeMap("p0", "F1", "p1", "E2"), "MyMapTypeIDB");
            env.assertPropsNew("s0", fields, new Object[]{"F1", "E2"});

            env.compileDeploy("@Priority(2) @Name('b') update istream BaseOne set i='BLANK'", path);

            env.sendEventMap(makeMap("p0", "somevalue", "p1", "E3"), "MyMapTypeIDB");
            env.assertPropsNew("s0", fields, new Object[]{"BLANK", "E3"});

            env.compileDeploy("@Priority(3) @Name('c') update istream BaseOneA set i='FINAL'", path);

            env.sendEventMap(makeMap("p0", "somevalue", "p1", "E4"), "MyMapTypeIDB");
            env.assertPropsNew("s0", fields, new Object[]{"BLANK", "E4"});

            env.undeployModuleContaining("insert");
            env.compileDeploy("@name('insert') insert into BaseOneA select p0 as i, p1 as p, 'a' as pa from MyMapTypeIDB", path);

            env.sendEventMap(makeMap("p0", "somevalue", "p1", "E5"), "MyMapTypeIDB");
            env.assertPropsNew("s0", fields, new Object[]{"FINAL", "E5"});

            env.undeployModuleContaining("insert");
            env.compileDeploy("@name('insert') insert into BaseOneB select p0 as i, p1 as p, 'b' as pb from MyMapTypeIDB", path);

            env.sendEventMap(makeMap("p0", "somevalue", "p1", "E6"), "MyMapTypeIDB");
            env.assertPropsNew("s0", fields, new Object[]{"BLANK", "E6"});

            env.undeployModuleContaining("insert");
            env.compileDeploy("@name('insert') insert into BaseTwo select p0 as i, p1 as p from MyMapTypeIDB", path);

            env.undeployModuleContaining("s0");
            env.compileDeploy("@name('s0') select * from BaseInterface", path).addListener("s0");

            env.sendEventMap(makeMap("p0", "E2", "p1", "E7"), "MyMapTypeIDB");
            env.assertPropsNew("s0", new String[]{"i"}, new Object[]{"XYZ"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "p0,p1".split(",");
            RegressionPath path = new RegressionPath();

            env.compileDeploy("@name('window') @public create window AWindow#keepall select * from MyMapTypeNW", path).addListener("window");
            env.compileDeploy("@name('insert') insert into AWindow select * from MyMapTypeNW", path).addListener("insert");
            env.compileDeploy("@name('select') select * from AWindow", path).addListener("select");
            env.compileDeploy("update istream AWindow set p1='newvalue'", path);

            env.milestone(0);

            env.sendEventMap(makeMap("p0", "E1", "p1", "oldvalue"), "MyMapTypeNW");
            env.assertPropsNew("window", fields, new Object[]{"E1", "newvalue"});
            env.assertPropsNew("insert", fields, new Object[]{"E1", "oldvalue"});
            env.assertPropsNew("select", fields, new Object[]{"E1", "newvalue"});

            env.compileDeploy("@name('onselect') on SupportBean(theString='A') select win.* from AWindow as win", path).addListener("onselect");
            env.sendEventBean(new SupportBean("A", 0));
            env.assertPropsNew("onselect", fields, new Object[]{"E1", "newvalue"});

            env.milestone(1);

            env.compileDeploy("@name('oninsert') @public on SupportBean(theString='B') insert into MyOtherStream select win.* from AWindow as win", path).addListener("oninsert");
            env.sendEventBean(new SupportBean("B", 1));
            env.assertPropsNew("oninsert", fields, new Object[]{"E1", "newvalue"});

            env.milestone(2);

            env.compileDeploy("update istream MyOtherStream set p0='a', p1='b'", path);
            env.compileDeploy("@name('s0') select * from MyOtherStream", path).addListener("s0");
            env.sendEventBean(new SupportBean("B", 1));
            env.assertPropsNew("oninsert", fields, new Object[]{"E1", "newvalue"});
            env.assertPropsNew("s0", fields, new Object[]{"a", "b"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateTypeWidener implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "theString,longBoxed,intBoxed".split(",");

            env.compileDeploy("@public insert into AStream select * from SupportBean", path);
            env.compileDeploy("update istream AStream set longBoxed=intBoxed, intBoxed=null", path);
            env.compileDeploy("@name('s0') select * from AStream", path).addListener("s0");

            SupportBean bean = new SupportBean("E1", 0);
            bean.setLongBoxed(888L);
            bean.setIntBoxed(999);
            env.sendEventBean(bean);
            env.assertPropsNew("s0", fields, new Object[]{"E1", 999L, null});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateSendRouteSenderPreprocess implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test map
            env.compileDeploy("@name('s0') select * from MyMapTypeSR").addListener("s0");
            env.compileDeploy("update istream MyMapTypeSR set p0='a'");

            String[] fields = "p0,p1".split(",");
            env.sendEventMap(makeMap("p0", "E1", "p1", "E1"), "MyMapTypeSR");
            env.assertPropsNew("s0", fields, new Object[]{"a", "E1"});

            env.sendEventMap(makeMap("p0", "E2", "p1", "E2"), "MyMapTypeSR");
            env.assertPropsNew("s0", fields, new Object[]{"a", "E2"});

            env.compileDeploy("@name('trigger') select * from SupportBean");
            env.statement("trigger").addListener(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    env.eventService().routeEventMap(makeMap("p0", "E3", "p1", "E3"), "MyMapTypeSR");
                }
            });
            env.sendEventBean(new SupportBean());
            env.assertPropsNew("s0", fields, new Object[]{"a", "E3"});

            env.compileDeploy("@Drop @name('drop') update istream MyMapTypeSR set p0='a'");
            env.sendEventMap(makeMap("p0", "E4", "p1", "E4"), "MyMapTypeSR");
            env.sendEventMap(makeMap("p0", "E5", "p1", "E5"), "MyMapTypeSR");
            env.sendEventBean(new SupportBean());
            env.assertListenerNotInvoked("s0");

            env.undeployModuleContaining("drop");
            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("trigger");

            // test bean
            env.compileDeploy("@name('s0') select * from SupportBean").addListener("s0");
            env.compileDeploy("update istream SupportBean set intPrimitive=999");

            fields = "theString,intPrimitive".split(",");
            env.sendEventBean(new SupportBean("E1", 0));
            env.assertPropsNew("s0", fields, new Object[]{"E1", 999});

            env.sendEventBean(new SupportBean("E2", 0));
            env.assertPropsNew("s0", fields, new Object[]{"E2", 999});

            env.compileDeploy("@name('trigger') select * from MyMapTypeSR");
            env.statement("trigger").addListener(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    env.eventService().routeEventBean(new SupportBean("E3", 0), "SupportBean");
                }
            });
            env.sendEventMap(makeMap("p0", "", "p1", ""), "MyMapTypeSR");
            env.assertPropsNew("s0", fields, new Object[]{"E3", 999});

            env.compileDeploy("@Drop update istream SupportBean set intPrimitive=1");
            env.sendEventBean(new SupportBean("E4", 0));
            env.sendEventBean(new SupportBean("E4", 0));
            env.sendEventMap(makeMap("p0", "", "p1", ""), "MyMapTypeSR");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.OBSERVEROPS);
        }
    }

    private static class EPLOtherUpdateSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setUpdateClause(UpdateClause.create("MyMapTypeSODA", Expressions.eq(Expressions.property("p1"), Expressions.constant("newvalue"))));
            model.getUpdateClause().setOptionalAsClauseStreamName("mytype");
            model.getUpdateClause().setOptionalWhereClause(Expressions.eq("p0", "E1"));
            assertEquals("update istream MyMapTypeSODA as mytype set p1=\"newvalue\" where p0=\"E1\"", model.toEPL());

            // test map
            env.compileDeploy("@name('s0') select * from MyMapTypeSODA").addListener("s0");
            env.compileDeploy(model);

            String[] fields = "p0,p1".split(",");
            env.sendEventMap(makeMap("p0", "E1", "p1", "E1"), "MyMapTypeSODA");
            env.assertPropsNew("s0", fields, new Object[]{"E1", "newvalue"});

            // test unmap
            String text = "update istream MyMapTypeSODA as mytype set p1=\"newvalue\" where p0=\"E1\"";
            env.eplToModelCompileDeploy(text);

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateXMLEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String xml = "<simpleEvent><prop1>SAMPLE_V1</prop1></simpleEvent>";
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            Document simpleDoc = SupportXML.getDocument(xml);

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public insert into ABCStreamXML select 1 as valOne, 2 as valTwo, * from MyXMLEvent", path);
            env.compileDeploy("update istream ABCStreamXML set valOne = 987, valTwo=123 where prop1='SAMPLE_V1'", path);
            env.compileDeploy("@name('s0') select * from ABCStreamXML", path).addListener("s0");

            env.sendEventXMLDOM(simpleDoc, "MyXMLEvent");
            env.assertPropsNew("s0", "valOne,valTwo,prop1".split(","), new Object[]{987, 123, "SAMPLE_V1"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateWrappedObject implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public insert into ABCStreamWO select 1 as valOne, 2 as valTwo, * from SupportBean", path);
            env.compileDeploy("@name('update') update istream ABCStreamWO set valOne = 987, valTwo=123", path);
            env.compileDeploy("@name('s0') select * from ABCStreamWO", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            env.assertPropsNew("s0", "valOne,valTwo,theString".split(","), new Object[]{987, 123, "E1"});

            env.undeployModuleContaining("update");
            env.compileDeploy("@name('update') update istream ABCStreamWO set theString = 'A'", path);

            env.sendEventBean(new SupportBean("E2", 0));
            env.assertPropsNew("s0", "valOne,valTwo,theString".split(","), new Object[]{1, 2, "A"});

            env.undeployModuleContaining("update");
            env.compileDeploy("update istream ABCStreamWO set theString = 'B', valOne = 555", path);

            env.sendEventBean(new SupportBean("E3", 0));
            env.assertPropsNew("s0", "valOne,valTwo,theString".split(","), new Object[]{555, 2, "B"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateCopyMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public insert into ABCStreamCM select * from SupportBeanCopyMethod", path);
            env.compileDeploy("update istream ABCStreamCM set valOne = 'x', valTwo='y'", path);
            env.compileDeploy("@name('s0') select * from ABCStreamCM", path).addListener("s0");

            env.sendEventBean(new SupportBeanCopyMethod("1", "2"));
            env.assertPropsNew("s0", "valOne,valTwo".split(","), new Object[]{"x", "y"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String[] fields = "theString,intPrimitive".split(",");
            env.compileDeploy("@public insert into ABCStreamSQ select * from SupportBean", path);
            env.compileDeploy("@name('update') update istream ABCStreamSQ set theString = (select s0 from MyMapTypeSelect#lastevent) where intPrimitive in (select w0 from MyMapTypeWhere#keepall)", path);
            env.compileDeploy("@name('s0') select * from ABCStreamSQ", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            env.assertPropsNew("s0", fields, new Object[]{"E1", 0});

            env.sendEventMap(makeMap("w0", 1), "MyMapTypeWhere");
            env.sendEventBean(new SupportBean("E2", 1));
            env.assertPropsNew("s0", fields, new Object[]{null, 1});

            env.sendEventBean(new SupportBean("E3", 2));
            env.assertPropsNew("s0", fields, new Object[]{"E3", 2});

            env.sendEventMap(makeMap("s0", "newvalue"), "MyMapTypeSelect");
            env.sendEventBean(new SupportBean("E4", 1));
            env.assertPropsNew("s0", fields, new Object[]{"newvalue", 1});

            env.sendEventMap(makeMap("s0", "othervalue"), "MyMapTypeSelect");
            env.sendEventBean(new SupportBean("E5", 1));
            env.assertPropsNew("s0", fields, new Object[]{"othervalue", 1});

            // test correlated subquery
            env.undeployModuleContaining("update");
            env.compileDeploy("@name('update') update istream ABCStreamSQ set intPrimitive = (select s1 from MyMapTypeSelect#keepall where s0 = ABCStreamSQ.theString)", path);

            // note that this will log an error (int primitive set to null), which is good, and leave the value unchanged
            env.sendEventBean(new SupportBean("E6", 8));
            env.assertPropsNew("s0", fields, new Object[]{"E6", 8});

            env.sendEventMap(makeMap("s0", "E7", "s1", 91), "MyMapTypeSelect");
            env.sendEventBean(new SupportBean("E7", 0));
            env.assertPropsNew("s0", fields, new Object[]{"E7", 91});

            // test correlated with as-clause
            env.undeployModuleContaining("update");
            env.compileDeploy("@name('update') update istream ABCStreamSQ as mystream set intPrimitive = (select s1 from MyMapTypeSelect#keepall where s0 = mystream.theString)", path);

            // note that this will log an error (int primitive set to null), which is good, and leave the value unchanged
            env.sendEventBean(new SupportBean("E8", 111));
            env.assertPropsNew("s0", fields, new Object[]{"E8", 111});

            env.sendEventMap(makeMap("s0", "E9", "s1", -1), "MyMapTypeSelect");
            env.sendEventBean(new SupportBean("E9", 0));
            env.assertPropsNew("s0", fields, new Object[]{"E9", -1});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateUnprioritizedOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "s0,s1".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public insert into ABCStreamUO select * from MyMapTypeUO", path);
            env.compileDeploy("@Name('A') update istream ABCStreamUO set s0='A'", path);
            env.compileDeploy("@Name('B') update istream ABCStreamUO set s0='B'", path);
            env.compileDeploy("@Name('C') update istream ABCStreamUO set s0='C'", path);
            env.compileDeploy("@Name('D') update istream ABCStreamUO set s0='D'", path);
            env.compileDeploy("@name('s0') select * from ABCStreamUO", path).addListener("s0");

            env.sendEventMap(makeMap("s0", "", "s1", 1), "MyMapTypeUO");
            env.assertPropsNew("s0", fields, new Object[]{"D", 1});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateListenerDeliveryMultiupdate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportUpdateListener listenerInsert = new SupportUpdateListener();
            SupportUpdateListener[] listeners = new SupportUpdateListener[5];
            for (int i = 0; i < listeners.length; i++) {
                listeners[i] = new SupportUpdateListener();
            }

            RegressionPath path = new RegressionPath();
            String[] fields = "theString,intPrimitive,value1".split(",");
            env.compileDeploy("@name('insert') @public insert into ABCStreamLD select *, 'orig' as value1 from SupportBean", path).statement("insert").addListener(listenerInsert);
            env.compileDeploy("@Name('A') update istream ABCStreamLD set theString='A', value1='a' where intPrimitive in (1,2)", path).statement("A").addListener(listeners[0]);
            env.compileDeploy("@Name('B') update istream ABCStreamLD set theString='B', value1='b' where intPrimitive in (1,3)", path).statement("B").addListener(listeners[1]);
            env.compileDeploy("@Name('C') update istream ABCStreamLD set theString='C', value1='c' where intPrimitive in (2,3)", path).statement("C").addListener(listeners[2]);
            env.compileDeploy("@name('s0') select * from ABCStreamLD", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertPropsNew("insert", fields, new Object[]{"E1", 1, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E1", 1, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 1, "a"});
            EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"A", 1, "a"});
            EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 1, "b"});
            assertFalse(listeners[2].isInvoked());
            env.assertPropsNew("s0", fields, new Object[]{"B", 1, "b"});
            reset(listeners);

            env.sendEventBean(new SupportBean("E2", 2));
            env.assertPropsNew("insert", fields, new Object[]{"E2", 2, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E2", 2, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 2, "a"});
            assertFalse(listeners[1].isInvoked());
            EPAssertionUtil.assertProps(listeners[2].assertOneGetOld(), fields, new Object[]{"A", 2, "a"});
            EPAssertionUtil.assertProps(listeners[2].assertOneGetNew(), fields, new Object[]{"C", 2, "c"});
            env.assertPropsNew("s0", fields, new Object[]{"C", 2, "c"});
            reset(listeners);

            env.sendEventBean(new SupportBean("E3", 3));
            env.assertPropsNew("insert", fields, new Object[]{"E3", 3, "orig"});
            assertFalse(listeners[0].isInvoked());
            EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"E3", 3, "orig"});
            EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 3, "b"});
            EPAssertionUtil.assertProps(listeners[2].assertOneGetOld(), fields, new Object[]{"B", 3, "b"});
            EPAssertionUtil.assertProps(listeners[2].assertOneGetNew(), fields, new Object[]{"C", 3, "c"});
            env.assertPropsNew("s0", fields, new Object[]{"C", 3, "c"});
            reset(listeners);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.OBSERVEROPS);
        }
    }

    private static class EPLOtherUpdateListenerDeliveryMultiupdateMixed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportUpdateListener listenerInsert = new SupportUpdateListener();
            SupportUpdateListener[] listeners = new SupportUpdateListener[5];
            for (int i = 0; i < listeners.length; i++) {
                listeners[i] = new SupportUpdateListener();
            }

            RegressionPath path = new RegressionPath();
            String[] fields = "theString,intPrimitive,value1".split(",");
            env.compileDeploy("@name('insert') @public insert into ABCStreamLDM select *, 'orig' as value1 from SupportBean", path).statement("insert").addListener(listenerInsert);
            env.compileDeploy("@name('s0') select * from ABCStreamLDM", path).addListener("s0");

            env.compileDeploy("@Name('A') update istream ABCStreamLDM set theString='A', value1='a'", path);
            env.compileDeploy("@Name('B') update istream ABCStreamLDM set theString='B', value1='b'", path).statement("B").addListener(listeners[1]);
            env.compileDeploy("@Name('C') update istream ABCStreamLDM set theString='C', value1='c'", path);
            env.compileDeploy("@Name('D') update istream ABCStreamLDM set theString='D', value1='d'", path).statement("D").addListener(listeners[3]);
            env.compileDeploy("@Name('E') update istream ABCStreamLDM set theString='E', value1='e'", path);

            env.sendEventBean(new SupportBean("E4", 4));
            env.assertPropsNew("insert", fields, new Object[]{"E4", 4, "orig"});
            assertFalse(listeners[0].isInvoked());
            EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"A", 4, "a"});
            EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 4, "b"});
            assertFalse(listeners[2].isInvoked());
            EPAssertionUtil.assertProps(listeners[3].assertOneGetOld(), fields, new Object[]{"C", 4, "c"});
            EPAssertionUtil.assertProps(listeners[3].assertOneGetNew(), fields, new Object[]{"D", 4, "d"});
            assertFalse(listeners[4].isInvoked());
            env.assertPropsNew("s0", fields, new Object[]{"E", 4, "e"});
            reset(listeners);

            env.statement("B").removeAllListeners();
            env.statement("D").removeAllListeners();
            env.statement("A").addListener(listeners[0]);
            env.statement("E").addListener(listeners[4]);

            env.sendEventBean(new SupportBean("E5", 5));
            env.assertPropsNew("insert", fields, new Object[]{"E5", 5, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E5", 5, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 5, "a"});
            assertFalse(listeners[1].isInvoked());
            assertFalse(listeners[2].isInvoked());
            assertFalse(listeners[3].isInvoked());
            EPAssertionUtil.assertProps(listeners[4].assertOneGetOld(), fields, new Object[]{"D", 5, "d"});
            EPAssertionUtil.assertProps(listeners[4].assertOneGetNew(), fields, new Object[]{"E", 5, "e"});
            env.assertPropsNew("s0", fields, new Object[]{"E", 5, "e"});
            reset(listeners);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.OBSERVEROPS);
        }
    }

    private static class EPLOtherUpdateNWSetMapProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                runAssertionNamedWindowSetMapProps(env, rep);
            }
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.SERDEREQUIRED);
        }
    }

    private static class EPLOtherUpdateMapSetMapPropsBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionSetMapPropsBean(env);
        }
    }

    private static class EPLOtherUpdateMapSetMapPropsRep implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                runAssertionUpdateIStreamSetMapProps(env, rep);
            }
        }
    }

    private static void runAssertionSetMapPropsBean(RegressionEnvironment env) {
        // test update-istream with bean
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@public @buseventtype create schema MyMapPropEvent as " + MyMapPropEvent.class.getName(), path);
        env.compileDeploy("@public insert into MyStream select * from MyMapPropEvent", path);
        env.compileDeploy("@name('s0') update istream MyStream set props('abc') = 1, array[2] = 10", path).addListener("s0");

        env.sendEventBean(new MyMapPropEvent());
        env.assertPropsIRPair("s0", "props('abc'),array[2]".split(","), new Object[]{1, 10}, new Object[]{null, null});

        env.undeployAll();
    }

    private static void runAssertionUpdateIStreamSetMapProps(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        // test update-istream with map
        RegressionPath path = new RegressionPath();
        String eplType = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMapProp.class) + " @name('type') @public @buseventtype create schema MyInfraTypeWithMapProp(simple String, myarray int[], mymap java.util.Map)";
        env.compileDeploy(eplType, path);

        env.compileDeploy("@name('update') update istream MyInfraTypeWithMapProp set simple='A', mymap('abc') = 1, myarray[2] = 10", path).addListener("update");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{null, new int[10], new HashMap<String, Object>()}, "MyInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(makeMapEvent(new HashMap<>(), new int[10]), "MyInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = env.runtimeAvroSchemaByDeployment("type", "MyInfraTypeWithMapProp");
            GenericData.Record event = new GenericData.Record(schema);
            event.put("myarray", Arrays.asList(0, 0, 0, 0, 0));
            event.put("mymap", new HashMap());
            event.put("simple", "");
            env.sendEventAvro(event, "MyInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonArray myarray = new JsonArray().add(0).add(0).add(0).add(0).add(0);
            JsonObject mymap = new JsonObject();
            JsonObject event = new JsonObject().add("myarray", myarray).add("mymap", mymap);
            env.sendEventJson(event.toString(), "MyInfraTypeWithMapProp");
        } else {
            fail();
        }
        String simpleExpected = eventRepresentationEnum.isAvroEvent() ? "" : null;
        env.assertPropsIRPair("update", "simple,mymap('abc'),myarray[2]".split(","), new Object[]{"A", 1, 10}, new Object[]{simpleExpected, null, 0});

        env.undeployAll();
    }

    private static void runAssertionNamedWindowSetMapProps(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        // test named-window update
        RegressionPath path = new RegressionPath();
        String eplTypes = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMapProp.class) + " @name('type') @public @buseventtype create schema MyNWInfraTypeWithMapProp(simple String, myarray int[], mymap java.util.Map)";
        env.compileDeploy(eplTypes, path);

        env.compileDeploy("@name('window') @public create window MyWindowWithMapProp#keepall as MyNWInfraTypeWithMapProp", path);
        env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMapProp.class) + " insert into MyWindowWithMapProp select * from MyNWInfraTypeWithMapProp", path);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{null, new int[10], new HashMap<String, Object>()}, "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(makeMapEvent(new HashMap<>(), new int[10]), "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = env.runtimeAvroSchemaByDeployment("type", "MyNWInfraTypeWithMapProp");
            GenericData.Record event = new GenericData.Record(schema);
            event.put("myarray", Arrays.asList(0, 0, 0, 0, 0));
            event.put("mymap", new HashMap());
            env.sendEventAvro(event, "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonArray myarray = new JsonArray().add(0).add(0).add(0).add(0).add(0);
            JsonObject mymap = new JsonObject();
            JsonObject event = new JsonObject().add("myarray", myarray).add("mymap", mymap);
            env.sendEventJson(event.toString(), "MyNWInfraTypeWithMapProp");
        } else {
            fail();
        }

        env.compileDeploy("on SupportBean update MyWindowWithMapProp set simple='A', mymap('abc') = intPrimitive, myarray[2] = intPrimitive", path);
        env.sendEventBean(new SupportBean("E1", 10));
        env.assertPropsPerRowIterator("window", "simple,mymap('abc'),myarray[2]".split(","), new Object[][]{{"A", 10, 10}});

        // test null and array too small
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{null, new int[2], null}, "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(makeMapEvent(null, new int[2]), "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(record("name").fields()
                    .optionalString("simple")
                    .name("myarray").type(array().items().longType()).noDefault()
                    .name("mymap").type(map().values().stringType()).noDefault()
                    .endRecord());
            event.put("myarray", Arrays.asList(0, 0));
            event.put("mymap", null);
            env.sendEventAvro(event, "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonArray myarray = new JsonArray().add(0).add(0);
            JsonObject event = new JsonObject().add("myarray", myarray);
            env.sendEventJson(event.toString(), "MyNWInfraTypeWithMapProp");
        } else {
            fail();
        }
        env.sendEventBean(new SupportBean("E2", 20));
        env.assertPropsPerRowIteratorAnyOrder("window", "simple,mymap('abc'),myarray[2]".split(","), new Object[][]{{"A", 20, 20}, {"A", null, null}});

        env.undeployAll();
    }

    private static Map<String, Object> makeMapEvent(Map<String, Object> mymap, int[] myarray) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("mymap", mymap);
        map.put("myarray", myarray);
        return map;
    }

    private static void tryAssertionFieldsWithPriority(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        RegressionPath path = new RegressionPath();
        String prefix = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSB.class);
        env.compileDeploy(prefix + " @public insert into MyStream select theString, intPrimitive from SupportBean(theString not like 'Z%')", path);
        env.compileDeploy(prefix + " @public insert into MyStream select 'AX'||theString as theString, intPrimitive from SupportBean(theString like 'Z%')", path);
        env.compileDeploy(prefix + " @Name('a') @Priority(12) update istream MyStream set intPrimitive=-2 where intPrimitive=-1", path);
        env.compileDeploy(prefix + " @Name('b') @Priority(11) update istream MyStream set intPrimitive=-1 where theString like 'D%'", path);
        env.compileDeploy(prefix + " @Name('c') @Priority(9) update istream MyStream set intPrimitive=9 where theString like 'A%'", path);
        env.compileDeploy(prefix + " @Name('d') @Priority(8) update istream MyStream set intPrimitive=8 where theString like 'A%' or theString like 'C%'", path);
        env.compileDeploy(" @Name('e') @Priority(10) update istream MyStream set intPrimitive=10 where theString like 'A%'", path);
        env.compileDeploy(" @Name('f') @Priority(7) update istream MyStream set intPrimitive=7 where theString like 'A%' or theString like 'C%'", path);
        env.compileDeploy(" @Name('g') @Priority(6) update istream MyStream set intPrimitive=6 where theString like 'A%'", path);
        env.compileDeploy(" @Name('h') @Drop update istream MyStream set intPrimitive=6 where theString like 'B%'", path);

        env.compileDeploy("@name('s0') select * from MyStream where intPrimitive > 0", path).addListener("s0");

        String[] fields = "theString,intPrimitive".split(",");
        env.sendEventBean(new SupportBean("A1", 0));
        env.assertPropsNew("s0", fields, new Object[]{"A1", 10});

        env.sendEventBean(new SupportBean("B1", 0));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean("C1", 0));
        env.assertPropsNew("s0", fields, new Object[]{"C1", 8});

        env.sendEventBean(new SupportBean("D1", 100));
        env.assertListenerNotInvoked("s0");

        env.undeployModuleContaining("s0");
        env.compileDeploy("@name('s0') select * from MyStream", path).addListener("s0");
        env.assertStatement("s0", statement -> assertTrue(eventRepresentationEnum.matchesClass(statement.getEventType().getUnderlyingType())));

        env.sendEventBean(new SupportBean("D1", -2));
        env.assertPropsNew("s0", fields, new Object[]{"D1", -2});

        env.sendEventBean(new SupportBean("Z1", -3));
        env.assertPropsNew("s0", fields, new Object[]{"AXZ1", 10});

        env.undeployModuleContaining("e");
        env.sendEventBean(new SupportBean("Z2", 0));
        env.assertPropsNew("s0", fields, new Object[]{"AXZ2", 9});

        env.undeployModuleContaining("c");
        env.undeployModuleContaining("d");
        env.undeployModuleContaining("f");
        env.undeployModuleContaining("g");
        env.sendEventBean(new SupportBean("Z3", 0));
        env.assertPropsNew("s0", fields, new Object[]{"AXZ3", 0});

        env.undeployAll();
    }

    private static void reset(SupportUpdateListener[] listeners) {
        for (SupportUpdateListener listener : listeners) {
            listener.reset();
        }
    }

    private static Map<String, Object> makeMap(String prop1, Object val1, String prop2, Object val2, String prop3, Object val3) {
        Map<String, Object> map = new HashMap<>();
        map.put(prop1, val1);
        map.put(prop2, val2);
        map.put(prop3, val3);
        return map;
    }

    private static Map<String, Object> makeMap(String prop1, Object val1, String prop2, Object val2) {
        Map<String, Object> map = new HashMap<>();
        map.put(prop1, val1);
        map.put(prop2, val2);
        return map;
    }

    private static Map<String, Object> makeMap(String prop1, Object val1) {
        Map<String, Object> map = new HashMap<>();
        map.put(prop1, val1);
        return map;
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    public static interface BaseInterface extends Serializable {
        public String getI();

        public void setI(String i);
    }

    public static class BaseOne implements BaseInterface, Serializable {
        private String i;
        private String p;

        public BaseOne() {
        }

        public BaseOne(String i, String p) {
            this.i = i;
            this.p = p;
        }

        public String getP() {
            return p;
        }

        public void setP(String p) {
            this.p = p;
        }

        public String getI() {
            return i;
        }

        public void setI(String i) {
            this.i = i;
        }
    }

    public static class BaseTwo implements BaseInterface, Serializable {
        private String i;
        private String p;

        public BaseTwo() {
        }

        public BaseTwo(String p) {
            this.p = p;
        }

        public void setP(String p) {
            this.p = p;
        }

        public String getP() {
            return p;
        }

        public String getI() {
            return i;
        }

        public void setI(String i) {
            this.i = i;
        }
    }

    public static class BaseOneA extends BaseOne {
        private String pa;

        public BaseOneA() {
        }

        public BaseOneA(String i, String p, String pa) {
            super(i, p);
            this.pa = pa;
        }

        public String getPa() {
            return pa;
        }

        public void setPa(String pa) {
            this.pa = pa;
        }
    }

    public static class BaseOneB extends BaseOne {
        private String pb;

        public BaseOneB() {
        }

        public BaseOneB(String i, String p, String pb) {
            super(i, p);
            this.pb = pb;
        }

        public String getPb() {
            return pb;
        }

        public void setPb(String pb) {
            this.pb = pb;
        }
    }

    public static void setIntBoxedValue(SupportBean sb, int value) {
        sb.setIntBoxed(value);
    }

    public static class MyMapPropEvent implements Serializable {
        private Map props = new HashMap();
        private Object[] array = new Object[10];

        public void setProps(String name, Object value) {
            props.put(name, value);
        }

        public void setArray(int index, Object value) {
            array[index] = value;
        }

        public Map getProps() {
            return props;
        }

        public void setProps(Map props) {
            this.props = props;
        }

        public Object[] getArray() {
            return array;
        }

        public void setArray(Object[] array) {
            this.array = array;
        }

        public Object getArray(int index) {
            return array[index];
        }
    }

    public static class MyLocalJsonProvidedMapProp implements Serializable {
        private static final long serialVersionUID = 2968655066129958928L;
        public String simple;
        public Integer[] myarray;
        public Map mymap;
    }

    public static class MyLocalJsonProvidedSB implements Serializable {
        private static final long serialVersionUID = 5566215092313226334L;
        public String theString;
        public int intPrimitive;
    }

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class SupportEventWithListOfObject implements Serializable {
        private static final long serialVersionUID = 4081092838548265144L;
        private List<Object> mylist;
        private boolean updated;

        public SupportEventWithListOfObject(List<Object> mylist) {
            this.mylist = mylist;
            this.updated = false;
        }

        public boolean isUpdated() {
            return updated;
        }

        public void setUpdated(boolean updated) {
            this.updated = updated;
        }

        public List<Object> getMylist() {
            return mylist;
        }

        public void setMylist(List<Object> mylist) {
            this.mylist = mylist;
        }
    }
}
