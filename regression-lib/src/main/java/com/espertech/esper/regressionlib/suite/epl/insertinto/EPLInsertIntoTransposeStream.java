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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EPLInsertIntoTransposeStream {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoTransposeCreateSchemaPOJO());
        execs.add(new EPLInsertIntoTransposeMapAndObjectArrayAndOthers());
        execs.add(new EPLInsertIntoTransposeFunctionToStreamWithProps());
        execs.add(new EPLInsertIntoTransposeFunctionToStream());
        execs.add(new EPLInsertIntoTransposeSingleColumnInsert());
        execs.add(new EPLInsertIntoTransposeEventJoinMap());
        execs.add(new EPLInsertIntoTransposeEventJoinPOJO());
        execs.add(new EPLInsertIntoTransposePOJOPropertyStream());
        execs.add(new EPLInsertIntoInvalidTranspose());
        return execs;
    }

    private static class EPLInsertIntoTransposeCreateSchemaPOJO implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema SupportBeanTwo as " + SupportBeanTwo.class.getName() + ";\n" +
                "on SupportBean event insert into astream select transpose(" + EPLInsertIntoTransposeStream.class.getName() + ".makeSB2Event(event));\n" +
                "on SupportBean event insert into bstream select transpose(" + EPLInsertIntoTransposeStream.class.getName() + ".makeSB2Event(event));\n" +
                "@name('a') select * from astream\n;" +
                "@name('b') select * from bstream\n;";
            env.compileDeploy(epl).addListener("a").addListener("b");

            env.sendEventBean(new SupportBean("E1", 1));

            String[] fields = new String[] {"stringTwo"};
            EPAssertionUtil.assertProps(env.listener("a").assertOneGetNewAndReset(), fields, new Object[] {"E1"});
            EPAssertionUtil.assertProps(env.listener("b").assertOneGetNewAndReset(), fields, new Object[] {"E1"});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposeMapAndObjectArrayAndOthers implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                runTransposeMapAndObjectArray(env, rep);
            }
        }

        private static void runTransposeMapAndObjectArray(RegressionEnvironment env, EventRepresentationChoice representation) {
            String[] fields = "p0,p1".split(",");
            RegressionPath path = new RegressionPath();
            String schema = representation.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMySchema.class) + "create schema MySchema(p0 string, p1 int)";
            env.compileDeployWBusPublicType(schema, path);

            String generateFunction;
            if (representation.isObjectArrayEvent()) {
                generateFunction = "generateOA";
            } else if (representation.isMapEvent()) {
                generateFunction = "generateMap";
            } else if (representation.isAvroEvent()) {
                generateFunction = "generateAvro";
            } else if (representation.isJsonEvent() || representation.isJsonProvidedClassEvent()) {
                generateFunction = "generateJson";
            } else {
                throw new IllegalStateException("Unrecognized code " + representation);
            }

            String epl = "insert into MySchema select transpose(" + generateFunction + "(theString, intPrimitive)) from SupportBean";
            env.compileDeploy("@name('s0') " + epl, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

            // MySchema already exists, start second statement
            env.compileDeploy("@name('s1') " + epl, path).addListener("s1");
            env.undeployModuleContaining("s0");

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposeFunctionToStreamWithProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextOne = "insert into MyStream select 1 as dummy, transpose(custom('O' || theString, 10)) from SupportBean(theString like 'I%')";
            env.compileDeploy(stmtTextOne, path);

            String stmtTextTwo = "@name('s0') select * from MyStream";
            env.compileDeploy(stmtTextTwo, path).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            assertEquals(Pair.class, type.getUnderlyingType());

            env.sendEventBean(new SupportBean("I1", 1));
            EventBean result = env.listener("s0").assertOneGetNewAndReset();
            Pair underlying = (Pair) result.getUnderlying();
            EPAssertionUtil.assertProps(result, "dummy,theString,intPrimitive".split(","), new Object[]{1, "OI1", 10});
            assertEquals("OI1", ((SupportBean) underlying.getFirst()).getTheString());

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposeFunctionToStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextOne = "insert into OtherStream select transpose(custom('O' || theString, 10)) from SupportBean(theString like 'I%')";
            env.compileDeploy("@name('first') " + stmtTextOne, path).addListener("first");

            String stmtTextTwo = "@name('s0') select * from OtherStream(theString like 'O%')";
            env.compileDeploy(stmtTextTwo, path).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            assertEquals(SupportBean.class, type.getUnderlyingType());

            env.sendEventBean(new SupportBean("I1", 1));
            EventBean result = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(result, "theString,intPrimitive".split(","), new Object[]{"OI1", 10});
            assertEquals("OI1", ((SupportBean) result.getUnderlying()).getTheString());

            // try second statement as "OtherStream" now already exists
            env.compileDeploy("@name('second') " + stmtTextOne).addListener("second");
            env.undeployModuleContaining("s0");
            env.sendEventBean(new SupportBean("I2", 2));
            EPAssertionUtil.assertProps(env.listener("second").assertOneGetNewAndReset(), "theString,intPrimitive".split(","), new Object[]{"OI2", 10});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposeSingleColumnInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // with transpose and same input and output
            String stmtTextOne = "@name('s0') insert into SupportBean select transpose(customOne('O' || theString, 10)) from SupportBean(theString like 'I%')";
            env.compileDeploy(stmtTextOne).addListener("s0");
            assertEquals(SupportBean.class, env.statement("s0").getEventType().getUnderlyingType());

            env.sendEventBean(new SupportBean("I1", 1));
            EventBean resultOne = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(resultOne, "theString,intPrimitive".split(","), new Object[]{"OI1", 10});
            assertEquals("OI1", ((SupportBean) resultOne.getUnderlying()).getTheString());
            env.undeployModuleContaining("s0");

            // with transpose but different input and output (also test ignore column name)
            String stmtTextTwo = "@name('s0') insert into SupportBeanNumeric select transpose(customTwo(intPrimitive, intPrimitive+1)) as col1 from SupportBean(theString like 'I%')";
            env.compileDeploy(stmtTextTwo).addListener("s0");
            assertEquals(SupportBeanNumeric.class, env.statement("s0").getEventType().getUnderlyingType());

            env.sendEventBean(new SupportBean("I2", 10));
            EventBean resultTwo = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(resultTwo, "intOne,intTwo".split(","), new Object[]{10, 11});
            assertEquals(11, (int) ((SupportBeanNumeric) resultTwo.getUnderlying()).getIntTwo());
            env.undeployModuleContaining("s0");

            // invalid wrong-bean target
            tryInvalidCompile(env, "insert into SupportBeanNumeric select transpose(customOne('O', 10)) from SupportBean",
                "Expression-returned value of type '" + SupportBean.class.getName() + "' cannot be converted to target event type 'SupportBeanNumeric' with underlying type '" + SupportBeanNumeric.class.getName() + "' [insert into SupportBeanNumeric select transpose(customOne('O', 10)) from SupportBean]");

            // invalid additional properties
            tryInvalidCompile(env, "insert into SupportBean select 1 as dummy, transpose(customOne('O', 10)) from SupportBean",
                "Cannot transpose additional properties in the select-clause to target event type 'SupportBean' with underlying type '" + SupportBean.class.getName() + "', the transpose function must occur alone in the select clause [insert into SupportBean select 1 as dummy, transpose(customOne('O', 10)) from SupportBean]");

            // invalid occurs twice
            tryInvalidCompile(env, "insert into SupportBean select transpose(customOne('O', 10)), transpose(customOne('O', 11)) from SupportBean",
                "A column name must be supplied for all but one stream if multiple streams are selected via the stream.* notation");

            // invalid wrong-type target
            try {
                RegressionPath path = new RegressionPath();
                env.compileDeploy("create map schema SomeOtherStream()", path);
                env.compileWCheckedEx("insert into SomeOtherStream select transpose(customOne('O', 10)) from SupportBean", path);
                fail();
            } catch (EPCompileException ex) {
                assertEquals("Expression-returned value of type '" + SupportBean.class.getName() + "' cannot be converted to target event type 'SomeOtherStream' with underlying type 'java.util.Map' [insert into SomeOtherStream select transpose(customOne('O', 10)) from SupportBean]", ex.getMessage());
            }
            env.undeployAll();

            // invalid two parameters
            tryInvalidCompile(env, "select transpose(customOne('O', 10), customOne('O', 10)) from SupportBean",
                "Failed to validate select-clause expression 'transpose(customOne(\"O\",10),customO...(46 chars)': The transpose function requires a single parameter expression [select transpose(customOne('O', 10), customOne('O', 10)) from SupportBean]");

            // test not a top-level function or used in where-clause (possible but not useful)
            env.compileDeploy("select * from SupportBean where transpose(customOne('O', 10)) is not null");
            env.compileDeploy("select transpose(customOne('O', 10)) is not null from SupportBean");

            // invalid insert of object-array into undefined stream
            tryInvalidCompile(env, "insert into SomeOther select transpose(generateOA('a', 1)) from SupportBean",
                "Invalid expression return type '[Ljava.lang.Object;' for transpose function [insert into SomeOther select transpose(generateOA('a', 1)) from SupportBean]");

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposeEventJoinMap implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String stmtTextOne = "insert into MyStreamTE select a, b from AEventTE#keepall as a, BEventTE#keepall as b";
            env.compileDeploy(stmtTextOne, path);

            String stmtTextTwo = "@name('s0') select a.id, b.id from MyStreamTE";
            env.compileDeploy(stmtTextTwo, path).addListener("s0");

            Map<String, Object> eventOne = makeMap(new Object[][]{{"id", "A1"}});
            Map<String, Object> eventTwo = makeMap(new Object[][]{{"id", "B1"}});
            env.sendEventMap(eventOne, "AEventTE");
            env.sendEventMap(eventTwo, "BEventTE");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.id,b.id".split(","), new Object[]{"A1", "B1"});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposeEventJoinPOJO implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextOne = "insert into MyStream2Bean select a.* as a, b.* as b from SupportBean_A#keepall as a, SupportBean_B#keepall as b";
            env.compileDeploy(stmtTextOne, path);

            String stmtTextTwo = "@name('s0') select a.id, b.id from MyStream2Bean";
            env.compileDeploy(stmtTextTwo, path).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("B1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a.id,b.id".split(","), new Object[]{"A1", "B1"});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoTransposePOJOPropertyStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextOne = "insert into MyStreamComplex select nested as inneritem from SupportBeanComplexProps";
            env.compileDeploy(stmtTextOne, path);

            String stmtTextTwo = "@name('s0') select inneritem.nestedValue as result from MyStreamComplex";
            env.compileDeploy(stmtTextTwo, path).addListener("s0");

            env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "result".split(","), new Object[]{"nestedValue"});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoInvalidTranspose implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextOne = "insert into MyStreamComplexMap select nested as inneritem from ComplexMap";
            env.compileDeploy(stmtTextOne, path);

            tryInvalidCompile(env, path, "select inneritem.nestedValue as result from MyStreamComplexMap",
                "Failed to validate select-clause expression 'inneritem.nestedValue': Failed to resolve property 'inneritem.nestedValue' (property 'inneritem' is a mapped property and requires keyed access) [select inneritem.nestedValue as result from MyStreamComplexMap]");

            // test invalid unwrap-properties
            tryInvalidCompile(env,
                "create schema E1 as " + E1.class.getName() + ";\n" +
                    "create schema E2 as " + E2.class.getName() + ";\n" +
                    "create schema EnrichedE2 as " + EnrichedE2.class.getName() + ";\n" +
                    "insert into EnrichedE2 " +
                    "select e2.* as event, e1.otherId as playerId " +
                    "from E1#length(20) as e1, E2#length(1) as e2 " +
                    "where e1.id = e2.id ",
                "The 'e2.* as event' syntax is not allowed when inserting into an existing bean event type, use the 'e2 as event' syntax instead");

            env.undeployAll();
        }
    }

    public static Map localGenerateMap(String string, int intPrimitive) {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("p0", string);
        out.put("p1", intPrimitive);
        return out;
    }

    public static Object[] localGenerateOA(String string, int intPrimitive) {
        return new Object[]{string, intPrimitive};
    }

    public static SupportBeanTwo makeSB2Event(SupportBean sb) {
        return new SupportBeanTwo(sb.getTheString(), sb.getIntPrimitive());
    }

    public static GenericData.Record localGenerateAvro(String string, int intPrimitive) {
        Schema schema = record("name").fields().requiredString("p0").requiredInt("p1").endRecord();
        GenericData.Record record = new GenericData.Record(schema);
        record.put("p0", string);
        record.put("p1", intPrimitive);
        return record;
    }

    public static String localGenerateJson(String string, int intPrimitive) {
        JsonObject object = new JsonObject();
        object.add("p0", string);
        object.add("p1", intPrimitive);
        return object.toString();
    }

    private static Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        for (Object[] entry : entries) {
            result.put(entry[0], entry[1]);
        }
        return result;
    }

    public static class E1 implements Serializable {
        private final String id;
        private final String otherId;

        public E1(String id, String otherId) {
            this.id = id;
            this.otherId = otherId;
        }

        public String getId() {
            return id;
        }

        public String getOtherId() {
            return otherId;
        }
    }

    public static class E2 implements Serializable {
        private final String id;
        private final String value;

        public E2(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }

    public static class EnrichedE2 implements Serializable {
        private final E2 event;
        private final String otherId;

        public EnrichedE2(E2 event, String playerId) {
            this.event = event;
            this.otherId = playerId;
        }

        public E2 getEvent() {
            return event;
        }

        public String getOtherId() {
            return otherId;
        }
    }

    public static class MyLocalJsonProvidedMySchema implements Serializable {
        public String p0;
        public int p1;
    }
}
