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
package com.espertech.esper.regressionlib.suite.event.infra;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDynRoot;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.events.SupportEventInfra;
import com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.Collections;
import java.util.function.Function;

import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag.exists;
import static com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag.notExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventInfraPropertyDynamicNested implements RegressionExecution {
    private final static String BEAN_TYPENAME = SupportBeanDynRoot.class.getSimpleName();
    public final static String XML_TYPENAME = EventInfraPropertyDynamicNested.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyDynamicNested.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyDynamicNested.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyDynamicNested.class.getSimpleName() + "Avro";
    public final static String JSON_TYPENAME = EventInfraPropertyDynamicNested.class.getSimpleName() + "Json";
    public final static String JSONPROVIDED_TYPENAME = EventInfraPropertyDynamicNested.class.getSimpleName() + "JsonProvided";

    public void run(RegressionEnvironment env) {
        runAssertion(env, EventRepresentationChoice.OBJECTARRAY, "");
        runAssertion(env, EventRepresentationChoice.MAP, "");
        runAssertion(env, EventRepresentationChoice.AVRO, "@AvroSchemaField(name='myid',schema='[\"int\",{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]')");
        runAssertion(env, EventRepresentationChoice.DEFAULT, "");
        runAssertion(env, EventRepresentationChoice.JSON, "");
    }

    private void runAssertion(RegressionEnvironment env, EventRepresentationChoice outputEventRep, String additionalAnnotations) {
        RegressionPath path = new RegressionPath();

        // Bean
        Pair[] beanTests = new Pair[]{
            new Pair<>(new SupportBeanDynRoot(new SupportBean_S0(101)), exists(101)),
            new Pair<>(new SupportBeanDynRoot("abc"), notExists()),
            new Pair<>(new SupportBeanDynRoot(new SupportBean_A("e1")), exists("e1")),
            new Pair<>(new SupportBeanDynRoot(new SupportBean_B("e2")), exists("e2")),
            new Pair<>(new SupportBeanDynRoot(new SupportBean_S1(102)), exists(102))
        };
        runAssertion(env, outputEventRep, additionalAnnotations, BEAN_TYPENAME, FBEAN, null, beanTests, Object.class, path);

        // Map
        Pair[] mapTests = new Pair[]{
            new Pair<>(Collections.emptyMap(), notExists()),
            new Pair<>(Collections.singletonMap("item", Collections.singletonMap("id", 101)), exists(101)),
            new Pair<>(Collections.singletonMap("item", Collections.emptyMap()), notExists()),
        };
        runAssertion(env, outputEventRep, additionalAnnotations, MAP_TYPENAME, FMAP, null, mapTests, Object.class, path);

        // Object array
        Pair[] oaTests = new Pair[]{
            new Pair<>(new Object[]{null}, notExists()),
            new Pair<>(new Object[]{new SupportBean_S0(101)}, exists(101)),
            new Pair<>(new Object[]{"abc"}, notExists()),
        };
        runAssertion(env, outputEventRep, additionalAnnotations, OA_TYPENAME, FOA, null, oaTests, Object.class, path);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("<item id=\"101\"/>", exists("101")),
            new Pair<>("<item/>", notExists()),
        };
        if (!outputEventRep.isAvroOrJsonEvent()) {
            runAssertion(env, outputEventRep, additionalAnnotations, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class, path);
        }

        // Avro
        Pair[] avroTests = new Pair[]{
            new Pair<>(null, exists(null)),
            new Pair<>(101, exists(101)),
            new Pair<>("abc", exists("abc")),
        };
        runAssertion(env, outputEventRep, additionalAnnotations, AVRO_TYPENAME, FAVRO, null, avroTests, Object.class, path);

        // Json
        Pair[] jsonTests = new Pair[]{
            new Pair<>("{}", notExists()),
            new Pair<>("{\"item\": { \"id\": 101} }", exists(101)),
            new Pair<>("{\"item\": { \"id\": \"abc\"} }", exists("abc")),
        };
        String schemasJson = "@JsonSchema(dynamic=true) create json schema Undefined();\n" +
            "@public @buseventtype @name('schema') create json schema " + JSON_TYPENAME + "(item Undefined)";
        env.compileDeploy(schemasJson, path);
        runAssertion(env, outputEventRep, additionalAnnotations, JSON_TYPENAME, FJSON, null, jsonTests, Object.class, path);

        // Json-Provided (class is provided)
        String schemasJsonProvided =
            "@JsonSchema(className='" + MyLocalJsonProvidedItem.class.getName() + "') @public @buseventtype @name('schema') create json schema Item();\n" +
            "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype @name('schema') create json schema " + JSONPROVIDED_TYPENAME + "(item Item)";
        env.compileDeploy(schemasJsonProvided, path);
        runAssertion(env, outputEventRep, additionalAnnotations, JSONPROVIDED_TYPENAME, FJSON, null, jsonTests, Object.class, path);
    }

    private void runAssertion(RegressionEnvironment env,
                              EventRepresentationChoice eventRepresentationEnum,
                              String additionalAnnotations,
                              String typename,
                              SupportEventInfra.FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType,
                              RegressionPath path) {
        String stmtText = "@name('s0') " + eventRepresentationEnum.getAnnotationText() + additionalAnnotations + " select " +
            "item.id? as myid, " +
            "exists(item.id?) as exists_myid " +
            "from " + typename + ";\n" +
            "@name('s1') select * from " + typename + ";\n";
        env.compileDeploy(stmtText, path).addListener("s0").addListener("s1");

        EventType eventType = env.statement("s0").getEventType();
        assertEquals(expectedPropertyType, eventType.getPropertyType("myid"));
        assertEquals(Boolean.class, JavaClassHelper.getBoxedType(eventType.getPropertyType("exists_myid")));
        assertTrue(eventRepresentationEnum.matchesClass(eventType.getUnderlyingType()));

        for (Pair pair : tests) {
            send.apply(env, pair.getFirst(), typename);
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            ValueWithExistsFlag expected = (ValueWithExistsFlag) pair.getSecond();
            SupportEventInfra.assertValueMayConvert(event, "myid", expected, optionalValueConversion);

            EventBean out = env.listener("s1").assertOneGetNewAndReset();
            EventPropertyGetter getter = out.getEventType().getGetter("item.id?");

            if (!typename.equals(XML_TYPENAME)) {
                assertEquals(expected.getValue(), getter.get(out));
            } else {
                Node item = (Node) getter.get(out);
                assertEquals(expected.getValue(), item == null ? null : item.getTextContent());
            }
            assertEquals(expected.isExists(), getter.isExistsProperty(out));
        }

        env.undeployAll();
    }

    private static final SupportEventInfra.FunctionSendEvent FAVRO = (env, value, typeName) -> {
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(typeName));
        Schema itemSchema = schema.getField("item").schema();
        GenericData.Record itemDatum = new GenericData.Record(itemSchema);
        itemDatum.put("id", value);
        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("item", itemDatum);
        env.sendEventAvro(datum, typeName);
    };

    public static class MyLocalJsonProvided implements Serializable {
        public MyLocalJsonProvidedItem item;
    }

    public static class MyLocalJsonProvidedItem implements Serializable {
        public Object id;
    }
}
