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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDynRoot;
import com.espertech.esper.regressionlib.support.events.SupportEventInfra;
import com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.common.internal.util.CollectionUtil.twoEntryMap;
import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag.*;
import static org.junit.Assert.assertEquals;

public class EventInfraPropertyDynamicNestedRootedNonSimple implements RegressionExecution {
    public final static String XML_TYPENAME = EventInfraPropertyDynamicNestedRootedNonSimple.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyDynamicNestedRootedNonSimple.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyDynamicNestedRootedNonSimple.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyDynamicNestedRootedNonSimple.class.getSimpleName() + "Avro";
    private final static Class BEAN_TYPE = SupportBeanDynRoot.class;
    public final static String JSON_TYPENAME = EventInfraPropertyDynamicNestedRootedNonSimple.class.getSimpleName() + "Json";
    public final static String JSONPROVIDED_TYPENAME = EventInfraPropertyDynamicNestedRootedNonSimple.class.getSimpleName() + "JsonProvided";

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        final ValueWithExistsFlag[] notExists = ValueWithExistsFlag.multipleNotExists(6);
        RegressionPath path = new RegressionPath();

        // Bean
        SupportBeanComplexProps inner = SupportBeanComplexProps.makeDefaultBean();
        Pair[] beanTests = new Pair[]{
            new Pair<>(new SupportBeanDynRoot("xxx"), notExists),
            new Pair<>(new SupportBeanDynRoot(inner), allExist(inner.getIndexed(0), inner.getIndexed(1), inner.getArrayProperty()[1], inner.getMapped("keyOne"), inner.getMapped("keyTwo"), inner.getMapProperty().get("xOne"))),
        };
        runAssertion(env, BEAN_TYPE.getSimpleName(), FBEAN, null, beanTests, Object.class, path);

        // Map
        Map<String, Object> mapNestedOne = new HashMap();
        mapNestedOne.put("indexed", new int[]{1, 2});
        mapNestedOne.put("arrayProperty", null);
        mapNestedOne.put("mapped", twoEntryMap("keyOne", 100, "keyTwo", 200));
        mapNestedOne.put("mapProperty", null);
        Map<String, Object> mapOne = Collections.singletonMap("item", mapNestedOne);
        Pair[] mapTests = new Pair[]{
            new Pair<>(Collections.emptyMap(), notExists),
            new Pair<>(mapOne, new ValueWithExistsFlag[]{exists(1), exists(2), notExists(), exists(100), exists(200), notExists()}),
        };
        runAssertion(env, MAP_TYPENAME, FMAP, null, mapTests, Object.class, path);

        // Object-Array
        Object[] oaNestedOne = new Object[]{new int[]{1, 2}, twoEntryMap("keyOne", 100, "keyTwo", 200), new int[]{1000, 2000}, Collections.singletonMap("xOne", "abc")};
        Object[] oaOne = new Object[]{null, oaNestedOne};
        Pair[] oaTests = new Pair[]{
            new Pair<>(new Object[]{null, null}, notExists),
            new Pair<>(oaOne, allExist(1, 2, 2000, 100, 200, "abc")),
        };
        runAssertion(env, OA_TYPENAME, FOA, null, oaTests, Object.class, path);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("", notExists),
            new Pair<>("<item>" +
                "<indexed>1</indexed><indexed>2</indexed><mapped id=\"keyOne\">3</mapped><mapped id=\"keyTwo\">4</mapped>" +
                "</item>", new ValueWithExistsFlag[]{exists("1"), exists("2"), notExists(), exists("3"), exists("4"), notExists()})
        };
        runAssertion(env, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class, path);

        // Avro
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        Schema itemSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(schema.getField("item").schema());
        GenericData.Record datumOne = new GenericData.Record(schema);
        datumOne.put("item", null);
        GenericData.Record datumItemTwo = new GenericData.Record(itemSchema);
        datumItemTwo.put("indexed", Arrays.asList(1, 2));
        datumItemTwo.put("mapped", twoEntryMap("keyOne", 3, "keyTwo", 4));
        GenericData.Record datumTwo = new GenericData.Record(schema);
        datumTwo.put("item", datumItemTwo);
        Pair[] avroTests = new Pair[]{
            new Pair<>(new GenericData.Record(schema), notExists),
            new Pair<>(datumOne, notExists),
            new Pair<>(datumTwo, new ValueWithExistsFlag[]{exists(1), exists(2), notExists(), exists(3), exists(4), notExists()}),
        };
        runAssertion(env, AVRO_TYPENAME, FAVRO, null, avroTests, Object.class, path);

        // Json
        Pair[] jsonTests = new Pair[]{
            new Pair<>("{}", notExists),
            new Pair<>("{ \"item\" : {}}", notExists),
            new Pair<>("{\n" +
                "  \"item\": {\n" +
                "    \"indexed\": [1,2],\n" +
                "    \"mapped\": {\n" +
                "      \"keyOne\": 3,\n" +
                "      \"keyTwo\": 4\n" +
                "    }\n" +
                "  }\n" +
                "}", new ValueWithExistsFlag[]{exists(1), exists(2), notExists(), exists(3), exists(4), notExists()}),
        };
        String schemasJson = "@public @buseventtype @name('schema') @JsonSchema(dynamic=true) create json schema " + JSON_TYPENAME + "()";
        env.compileDeploy(schemasJson, path);
        runAssertion(env, JSON_TYPENAME, FJSON, null, jsonTests, Object.class, path);

        // Json-Class-Provided
        ValueWithExistsFlag[] jsonProvidedNulls = new ValueWithExistsFlag[]{exists(null), notExists(), notExists(), exists(null), notExists(), notExists()};
        Pair[] jsonProvidedTests = new Pair[]{
            new Pair<>("{}", notExists),
            new Pair<>("{ \"item\" : {}}", jsonProvidedNulls),
            new Pair<>("{\n" +
                "  \"item\": {\n" +
                "    \"indexed\": [1,2],\n" +
                "    \"mapped\": {\n" +
                "      \"keyOne\": 3,\n" +
                "      \"keyTwo\": 4\n" +
                "    }\n" +
                "  }\n" +
                "}", new ValueWithExistsFlag[]{exists(1), exists(2), notExists(), exists(3), exists(4), notExists()})
        };
        String schemasJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype @name('schema') @JsonSchema(dynamic=true) create json schema " + JSONPROVIDED_TYPENAME + "()";
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

        String stmtText = "@name('s0') select " +
            "item?.indexed[0] as indexed1, " +
            "exists(item?.indexed[0]) as exists_indexed1, " +
            "item?.indexed[1]? as indexed2, " +
            "exists(item?.indexed[1]?) as exists_indexed2, " +
            "item?.arrayProperty[1]? as array, " +
            "exists(item?.arrayProperty[1]?) as exists_array, " +
            "item?.mapped('keyOne') as mapped1, " +
            "exists(item?.mapped('keyOne')) as exists_mapped1, " +
            "item?.mapped('keyTwo')? as mapped2,  " +
            "exists(item?.mapped('keyTwo')?) as exists_mapped2,  " +
            "item?.mapProperty('xOne')? as map, " +
            "exists(item?.mapProperty('xOne')?) as exists_map " +
            " from " + typename;
        env.compileDeploy(stmtText, path).addListener("s0");

        String[] propertyNames = "indexed1,indexed2,array,mapped1,mapped2,map".split(",");
        EventType eventType = env.statement("s0").getEventType();
        for (String propertyName : propertyNames) {
            assertEquals(expectedPropertyType, eventType.getPropertyType(propertyName));
            assertEquals(Boolean.class, eventType.getPropertyType("exists_" + propertyName));
        }

        for (Pair pair : tests) {
            send.apply(env, pair.getFirst(), typename);
            SupportEventInfra.assertValuesMayConvert(env.listener("s0").assertOneGetNewAndReset(), propertyNames, (ValueWithExistsFlag[]) pair.getSecond(), optionalValueConversion);
        }

        env.undeployAll();
    }

    public static class MyLocalJsonProvided implements Serializable {
        public MyLocalJsonProvidedItem item;
    }

    public static class MyLocalJsonProvidedItem implements Serializable {
        public Object[] indexed;
        public Map<String, Object> mapped;
    }
}
