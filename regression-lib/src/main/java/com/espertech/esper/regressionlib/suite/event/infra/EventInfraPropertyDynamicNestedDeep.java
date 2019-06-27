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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDynRoot;
import com.espertech.esper.regressionlib.support.events.SupportEventInfra;
import com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag.*;
import static org.junit.Assert.assertEquals;

public class EventInfraPropertyDynamicNestedDeep implements RegressionExecution {
    public final static String XML_TYPENAME = EventInfraPropertyDynamicNestedDeep.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyDynamicNestedDeep.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyDynamicNestedDeep.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyDynamicNestedDeep.class.getSimpleName() + "Avro";
    public final static String JSON_TYPENAME = EventInfraPropertyDynamicNestedDeep.class.getSimpleName() + "Json";
    public final static String JSONPROVIDED_TYPENAME = EventInfraPropertyDynamicNestedDeep.class.getSimpleName() + "JsonProvided";

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {

        final ValueWithExistsFlag[] notExists = multipleNotExists(6);
        RegressionPath path = new RegressionPath();

        // Bean
        SupportBeanComplexProps beanOne = SupportBeanComplexProps.makeDefaultBean();
        String n1v = beanOne.getNested().getNestedValue();
        String n1nv = beanOne.getNested().getNestedNested().getNestedNestedValue();
        SupportBeanComplexProps beanTwo = SupportBeanComplexProps.makeDefaultBean();
        beanTwo.getNested().setNestedValue("nested1");
        beanTwo.getNested().getNestedNested().setNestedNestedValue("nested2");
        Pair[] beanTests = new Pair[]{
            new Pair<>(new SupportBeanDynRoot(beanOne), allExist(n1v, n1v, n1nv, n1nv, n1nv, n1nv)),
            new Pair<>(new SupportBeanDynRoot(beanTwo), allExist("nested1", "nested1", "nested2", "nested2", "nested2", "nested2")),
            new Pair<>(new SupportBeanDynRoot("abc"), notExists)
        };
        runAssertion(env, "SupportBeanDynRoot", FBEAN, null, beanTests, Object.class, path);

        // Map
        Map<String, Object> mapOneL2 = new HashMap<>();
        mapOneL2.put("nestedNestedValue", 101);
        Map<String, Object> mapOneL1 = new HashMap<>();
        mapOneL1.put("nestedNested", mapOneL2);
        mapOneL1.put("nestedValue", 100);
        Map<String, Object> mapOneL0 = new HashMap<>();
        mapOneL0.put("nested", mapOneL1);
        Map<String, Object> mapOne = Collections.singletonMap("item", mapOneL0);
        Pair[] mapTests = new Pair[]{
            new Pair<>(mapOne, allExist(100, 100, 101, 101, 101, 101)),
            new Pair<>(Collections.emptyMap(), notExists),
        };
        runAssertion(env, MAP_TYPENAME, FMAP, null, mapTests, Object.class, path);

        // Object-Array
        Object[] oaOneL2 = new Object[]{101};
        Object[] oaOneL1 = new Object[]{oaOneL2, 100};
        Object[] oaOneL0 = new Object[]{oaOneL1};
        Object[] oaOne = new Object[]{oaOneL0};
        Pair[] oaTests = new Pair[]{
            new Pair<>(oaOne, allExist(100, 100, 101, 101, 101, 101)),
            new Pair<>(new Object[]{null}, notExists),
        };
        runAssertion(env, OA_TYPENAME, FOA, null, oaTests, Object.class, path);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("<item>\n" +
                "\t<nested nestedValue=\"100\">\n" +
                "\t\t<nestedNested nestedNestedValue=\"101\">\n" +
                "\t\t</nestedNested>\n" +
                "\t</nested>\n" +
                "</item>\n", allExist("100", "100", "101", "101", "101", "101")),
            new Pair<>("<item/>", notExists),
        };
        runAssertion(env, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class, path);

        // Avro
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        Schema nestedSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(schema.getField("item").schema().getField("nested").schema());
        Schema nestedNestedSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(nestedSchema.getField("nestedNested").schema());
        GenericData.Record nestedNestedDatum = new GenericData.Record(nestedNestedSchema);
        nestedNestedDatum.put("nestedNestedValue", 101);
        GenericData.Record nestedDatum = new GenericData.Record(nestedSchema);
        nestedDatum.put("nestedValue", 100);
        nestedDatum.put("nestedNested", nestedNestedDatum);
        GenericData.Record emptyDatum = new GenericData.Record(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord());
        Pair[] avroTests = new Pair[]{
            new Pair<>(nestedDatum, allExist(100, 100, 101, 101, 101, 101)),
            new Pair<>(emptyDatum, notExists),
            new Pair<>(null, notExists)
        };
        runAssertion(env, AVRO_TYPENAME, FAVRO, null, avroTests, Object.class, path);

        // Json
        Pair[] jsonTests = new Pair[]{
            new Pair<>("{\n" +
                "  \"item\": {\n" +
                "    \"nested\": {\n" +
                "      \"nestedValue\": 100,\n" +
                "      \"nestedNested\": {\n" +
                "        \"nestedNestedValue\": 101\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", allExist(100, 100, 101, 101, 101, 101)),
            new Pair<>("{\n" +
                "  \"item\": {\n" +
                "    \"nested\": {\n" +
                "      }\n" +
                "    }\n" +
                "  }\n", notExists),
            new Pair<>("{ \"item\": {}}", notExists),
            new Pair<>("{}", notExists)
        };
        String schemas = "@JsonSchema(dynamic=true) create json schema Item();\n" +
            "@public @buseventtype @name('schema') create json schema " + JSON_TYPENAME + "(item Item)";
        env.compileDeploy(schemas, path);
        runAssertion(env, JSON_TYPENAME, FJSON, null, jsonTests, Object.class, path);

        // Json-Provided
        Pair[] jsonProvidedTests = new Pair[]{
            new Pair<>("{\n" +
                "  \"item\": {\n" +
                "    \"nested\": {\n" +
                "      \"nestedValue\": 100,\n" +
                "      \"nestedNested\": {\n" +
                "        \"nestedNestedValue\": 101\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", allExist(100, 100, 101, 101, 101, 101)),
            new Pair<>("{\n" +
                "  \"item\": {\n" +
                "    \"nested\": {\n" +
                "      }\n" +
                "    }\n" +
                "  }\n", new ValueWithExistsFlag[] {exists(null), exists(null), notExists(), notExists(), notExists(), notExists()}),
            new Pair<>("{ \"item\": {}}", notExists),
            new Pair<>("{}", notExists)
        };
        String schemasJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype @name('schema') create json schema " + JSONPROVIDED_TYPENAME + "()";
        env.compileDeploy(schemasJsonProvided, path);
        runAssertion(env, JSONPROVIDED_TYPENAME, FJSON, null, jsonProvidedTests, Object.class, path);
    }

    private void runAssertion(RegressionEnvironment env,
                              String typename,
                              FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType,
                              RegressionPath path) {
        runAssertionSelectNested(env, typename, send, optionalValueConversion, tests, expectedPropertyType, path);
        runAssertionBeanNav(env, typename, send, tests[0].getFirst(), path);
        env.undeployAll();
    }

    private void runAssertionBeanNav(RegressionEnvironment env,
                                     String typename,
                                     SupportEventInfra.FunctionSendEvent send,
                                     Object underlyingComplete,
                                     RegressionPath path) {
        String stmtText = "@name('s0') select * from " + typename;
        env.compileDeploy(stmtText, path).addListener("s0");

        send.apply(env, underlyingComplete, typename);
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        SupportEventTypeAssertionUtil.assertConsistency(event);

        env.undeployModuleContaining("s0");
    }

    private void runAssertionSelectNested(RegressionEnvironment env, String typename,
                                          FunctionSendEvent send,
                                          Function<Object, Object> optionalValueConversion,
                                          Pair[] tests,
                                          Class expectedPropertyType, RegressionPath path) {

        String stmtText = "@name('s0') select " +
            " item.nested?.nestedValue as n1, " +
            " exists(item.nested?.nestedValue) as exists_n1, " +
            " item.nested?.nestedValue? as n2, " +
            " exists(item.nested?.nestedValue?) as exists_n2, " +
            " item.nested?.nestedNested.nestedNestedValue as n3, " +
            " exists(item.nested?.nestedNested.nestedNestedValue) as exists_n3, " +
            " item.nested?.nestedNested?.nestedNestedValue as n4, " +
            " exists(item.nested?.nestedNested?.nestedNestedValue) as exists_n4, " +
            " item.nested?.nestedNested.nestedNestedValue? as n5, " +
            " exists(item.nested?.nestedNested.nestedNestedValue?) as exists_n5, " +
            " item.nested?.nestedNested?.nestedNestedValue? as n6, " +
            " exists(item.nested?.nestedNested?.nestedNestedValue?) as exists_n6 " +
            " from " + typename;
        env.compileDeploy(stmtText, path).addListener("s0");

        String[] propertyNames = "n1,n2,n3,n4,n5,n6".split(",");
        EventType eventType = env.statement("s0").getEventType();
        for (String propertyName : propertyNames) {
            assertEquals(expectedPropertyType, eventType.getPropertyType(propertyName));
            assertEquals(Boolean.class, eventType.getPropertyType("exists_" + propertyName));
        }

        for (Pair pair : tests) {
            send.apply(env, pair.getFirst(), typename);
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            SupportEventInfra.assertValuesMayConvert(event, propertyNames, (ValueWithExistsFlag[]) pair.getSecond(), optionalValueConversion);
        }

        env.undeployModuleContaining("s0");
    }

    private static final SupportEventInfra.FunctionSendEvent FAVRO = (env, value, typename) -> {
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        Schema itemSchema = schema.getField("item").schema();
        GenericData.Record itemDatum = new GenericData.Record(itemSchema);
        itemDatum.put("nested", value);
        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("item", itemDatum);
        env.sendEventAvro(datum, typename);
    };

    public static class MyLocalJsonProvided implements Serializable {
        public MyLocalJsonItem item;
    }

    public static class MyLocalJsonItem implements Serializable {
        public MyLocalJsonProvidedNested nested;
    }

    public static class MyLocalJsonProvidedNested implements Serializable {
        public Integer nestedValue;
        public MyLocalJsonProvidedNestedNested nestedNested;
    }

    public static class MyLocalJsonProvidedNestedNested implements Serializable {
        public Integer nestedNestedValue;
    }
}
