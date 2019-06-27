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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.ISupportABCImpl;
import com.espertech.esper.regressionlib.support.bean.ISupportAImpl;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.junit.Assert;

import java.io.Serializable;
import java.util.*;

import static com.espertech.esper.common.internal.support.EventRepresentationChoice.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExprCoreTypeOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprCoreTypeOfFragment());
        execs.add(new ExprCoreTypeOfNamedUnnamedPOJO());
        execs.add(new ExprCoreTypeOfInvalid());
        execs.add(new ExprCoreTypeOfDynamicProps());
        execs.add(new ExprCoreTypeOfVariantStream());
        return execs;
    }

    private static class ExprCoreTypeOfInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select typeof(xx) from SupportBean",
                "Failed to validate select-clause expression 'typeof(xx)': Property named 'xx' is not valid in any stream [select typeof(xx) from SupportBean]");
        }
    }

    private static class ExprCoreTypeOfDynamicProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String schema = MAP.getAnnotationText() + " create schema MyDynoPropSchema as (key string);\n";
            env.compileDeployWBusPublicType(schema, path);

            String epl = "@name('s0') select typeof(prop?), typeof(key) from MyDynoPropSchema as s0";
            env.compileDeploy(epl, path).addListener("s0");

            tryAssertionDynamicProps(env);

            env.undeployModuleContaining("s0");

            env.eplToModelCompileDeploy(epl, path).addListener("s0");

            tryAssertionDynamicProps(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreTypeOfVariantStream implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : values()) {
                tryAssertionVariantStream(env, rep);
            }
        }
    }

    private static class ExprCoreTypeOfNamedUnnamedPOJO implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test name-provided or no-name-provided
            String epl = "@name('s0') select typeof(A) as t0 from ISupportA as A";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new ISupportAImpl(null, null));
            Assert.assertEquals(ISupportAImpl.class.getSimpleName(), env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.sendEventBean(new ISupportABCImpl(null, null, null, null));
            Assert.assertEquals("ISupportABCImpl", env.listener("s0").assertOneGetNewAndReset().get("t0"));

            env.undeployAll();
        }
    }

    private static class ExprCoreTypeOfFragment implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : values()) {
                tryAssertionFragment(env, rep);
            }
        }
    }

    private static void tryAssertionVariantStream(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        String eplSchemas =
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedWKey.class) + " create schema EventOne as (key string);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedWKey.class) + " create schema EventTwo as (key string);\n" +
                " create schema S0 as " + SupportBean_S0.class.getName() + ";\n" +
                " create variant schema VarSchema as *;\n";
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType(eplSchemas, path);

        env.compileDeploy("insert into VarSchema select * from EventOne", path);
        env.compileDeploy("insert into VarSchema select * from EventTwo", path);
        env.compileDeploy("insert into VarSchema select * from S0", path);
        env.compileDeploy("insert into VarSchema select * from SupportBean", path);

        String stmtText = "@name('s0') select typeof(A) as t0 from VarSchema as A";
        env.compileDeploy(stmtText, path).addListener("s0");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{"value"}, "EventOne");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(Collections.singletonMap("key", "value"), "EventOne");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SchemaBuilder.record("EventOne").fields().requiredString("key").endRecord());
            record.put("key", "value");
            env.sendEventAvro(record, "EventOne");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            env.sendEventJson(new JsonObject().add("key", "value").toString(), "EventOne");
        } else {
            fail();
        }
        Assert.assertEquals("EventOne", env.listener("s0").assertOneGetNewAndReset().get("t0"));

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{"value"}, "EventTwo");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(Collections.singletonMap("key", "value"), "EventTwo");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SchemaBuilder.record("EventTwo").fields().requiredString("key").endRecord());
            record.put("key", "value");
            env.sendEventAvro(record, "EventTwo");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            env.sendEventJson(new JsonObject().add("key", "value").toString(), "EventTwo");
        } else {
            fail();
        }
        Assert.assertEquals("EventTwo", env.listener("s0").assertOneGetNewAndReset().get("t0"));

        env.sendEventBean(new SupportBean_S0(1), "S0");
        Assert.assertEquals("S0", env.listener("s0").assertOneGetNewAndReset().get("t0"));

        env.sendEventBean(new SupportBean());
        Assert.assertEquals("SupportBean", env.listener("s0").assertOneGetNewAndReset().get("t0"));

        env.undeployModuleContaining("s0");
        env.compileDeploy("@name('s0') select * from VarSchema match_recognize(\n" +
            "  measures A as a, B as b\n" +
            "  pattern (A B)\n" +
            "  define A as typeof(A) = \"EventOne\",\n" +
            "         B as typeof(B) = \"EventTwo\"\n" +
            "  )", path).addListener("s0");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{"value"}, "EventOne");
            env.sendEventObjectArray(new Object[]{"value"}, "EventTwo");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(Collections.singletonMap("key", "value"), "EventOne");
            env.sendEventMap(Collections.singletonMap("key", "value"), "EventTwo");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = SchemaBuilder.record("EventTwo").fields().requiredString("key").endRecord();
            GenericData.Record eventOne = new GenericData.Record(schema);
            eventOne.put("key", "value");
            GenericData.Record eventTwo = new GenericData.Record(schema);
            eventTwo.put("key", "value");
            env.sendEventAvro(eventOne, "EventOne");
            env.sendEventAvro(eventTwo, "EventTwo");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            env.sendEventJson(new JsonObject().add("key", "value").toString(), "EventOne");
            env.sendEventJson(new JsonObject().add("key", "value").toString(), "EventTwo");
        } else {
            fail();
        }
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void tryAssertionFragment(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedInnerSchema.class) + " create schema InnerSchema as (key string);\n" +
            eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMySchema.class) + " create schema MySchema as (inside InnerSchema, insidearr InnerSchema[]);\n" +
            eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedOut.class) + " @name('s0') select typeof(s0.inside) as t0, typeof(s0.insidearr) as t1 from MySchema as s0;\n";
        String[] fields = new String[]{"t0", "t1"};
        env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");
        String deploymentId = env.deploymentId("s0");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[2], "MySchema");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(new HashMap<String, Object>(), "MySchema");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventType(deploymentId, "MySchema"));
            env.sendEventAvro(new GenericData.Record(schema), "MySchema");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            env.sendEventJson("{}", "MySchema");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{new Object[2], null}, "MySchema");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new HashMap<>();
            theEvent.put("inside", new HashMap<String, Object>());
            env.sendEventMap(theEvent, "MySchema");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema mySchema = SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventType(deploymentId, "MySchema"));
            Schema innerSchema = SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventType(deploymentId, "InnerSchema"));
            GenericData.Record event = new GenericData.Record(mySchema);
            event.put("inside", new GenericData.Record(innerSchema));
            env.sendEventAvro(event, "MySchema");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonObject theEvent = new JsonObject().add("inside", new JsonObject());
            env.sendEventJson(theEvent.toString(), "MySchema");
        } else {
            fail();
        }

        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"InnerSchema", null});

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{null, new Object[2][]}, "MySchema");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new HashMap<>();
            theEvent.put("insidearr", new Map[0]);
            env.sendEventMap(theEvent, "MySchema");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema mySchema = SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventType(deploymentId, "MySchema"));
            GenericData.Record event = new GenericData.Record(mySchema);
            event.put("insidearr", Collections.emptyList());
            env.sendEventAvro(event, "MySchema");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonObject theEvent = new JsonObject().add("insidearr", new JsonArray().add(new JsonObject()));
            env.sendEventJson(theEvent.toString(), "MySchema");
        } else {
            fail();
        }

        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, "InnerSchema[]"});

        env.undeployAll();
    }

    private static void tryAssertionDynamicProps(RegressionEnvironment env) {

        String[] fields = new String[]{"typeof(prop?)", "typeof(key)"};

        sendSchemaEvent(env, 1, "E1");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"Integer", "String"});

        sendSchemaEvent(env, "test", "E2");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"String", "String"});

        sendSchemaEvent(env, null, "E3");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, "String"});
    }

    private static void sendSchemaEvent(RegressionEnvironment env, Object prop, String key) {
        Map<String, Object> theEvent = new HashMap<>();
        theEvent.put("prop", prop);
        theEvent.put("key", key);
        env.sendEventMap(theEvent, "MyDynoPropSchema");
    }

    public static class MyLocalJsonProvidedInnerSchema implements Serializable {
        public String key;
    }

    public static class MyLocalJsonProvidedMySchema implements Serializable {
        public MyLocalJsonProvidedInnerSchema inside;
        public MyLocalJsonProvidedInnerSchema[] insidearr;
    }

    public static class MyLocalJsonProvidedOut implements Serializable {
        public String t0;
        public String t1;
    }

    public static class MyLocalJsonProvidedWKey implements Serializable {
        public String key;
    }
}