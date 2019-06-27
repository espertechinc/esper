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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.events.SupportEventInfra;
import com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.common.internal.util.CollectionUtil.twoEntryMap;
import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag.multipleNotExists;
import static org.junit.Assert.assertEquals;

public class EventInfraPropertyDynamicNonSimple implements RegressionExecution {

    public final static String XML_TYPENAME = EventInfraPropertyDynamicNonSimple.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyDynamicNonSimple.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyDynamicNonSimple.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyDynamicNonSimple.class.getSimpleName() + "Avro";
    private final static String JSON_TYPENAME = EventInfraPropertyDynamicNonSimple.class.getSimpleName() + "Json";
    private final static String JSONPROVIDED_TYPENAME = EventInfraPropertyDynamicNonSimple.class.getSimpleName() + "JsonProvided";

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        final ValueWithExistsFlag[] notExists = multipleNotExists(4);
        RegressionPath path = new RegressionPath();

        // Bean
        SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();
        Pair[] beanTests = new Pair[]{
            new Pair<>(bean, ValueWithExistsFlag.allExist(bean.getIndexed(0), bean.getIndexed(1), bean.getMapped("keyOne"), bean.getMapped("keyTwo")))
        };
        runAssertion(env, SupportBeanComplexProps.class.getSimpleName(), FBEAN, null, beanTests, Object.class, path);

        // Map
        Pair[] mapTests = new Pair[]{
            new Pair<>(Collections.singletonMap("somekey", "10"), notExists),
            new Pair<>(twoEntryMap("indexed", new int[]{1, 2}, "mapped", twoEntryMap("keyOne", 3, "keyTwo", 4)), ValueWithExistsFlag.allExist(1, 2, 3, 4)),
        };
        runAssertion(env, MAP_TYPENAME, FMAP, null, mapTests, Object.class, path);

        // Object-Array
        Pair[] oaTests = new Pair[]{
            new Pair<>(new Object[]{null, null}, notExists),
            new Pair<>(new Object[]{new int[]{1, 2}, twoEntryMap("keyOne", 3, "keyTwo", 4)}, ValueWithExistsFlag.allExist(1, 2, 3, 4)),
        };
        runAssertion(env, OA_TYPENAME, FOA, null, oaTests, Object.class, path);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("", notExists),
            new Pair<>("<indexed>1</indexed><indexed>2</indexed><mapped id=\"keyOne\">3</mapped><mapped id=\"keyTwo\">4</mapped>", ValueWithExistsFlag.allExist("1", "2", "3", "4"))
        };
        runAssertion(env, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class, path);

        // Avro
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        GenericData.Record datumOne = new GenericData.Record(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord());
        GenericData.Record datumTwo = new GenericData.Record(schema);
        datumTwo.put("indexed", Arrays.asList(1, 2));
        datumTwo.put("mapped", twoEntryMap("keyOne", 3, "keyTwo", 4));
        Pair[] avroTests = new Pair[]{
            new Pair<>(datumOne, notExists),
            new Pair<>(datumTwo, ValueWithExistsFlag.allExist(1, 2, 3, 4)),
        };
        runAssertion(env, AVRO_TYPENAME, FAVRO, null, avroTests, Object.class, path);

        // Json
        env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema " + JSON_TYPENAME + "()", path);
        Pair[] jsonTests = new Pair[]{
            new Pair<>("{}", notExists),
            new Pair<>("{\"mapped\":{\"keyOne\":\"3\",\"keyTwo\":\"4\"},\"indexed\":[\"1\",\"2\"]}", ValueWithExistsFlag.allExist("1", "2", "3", "4"))
        };
        runAssertion(env, JSON_TYPENAME, FJSON, null, jsonTests, Object.class, path);

        // Json-Provided-Class
        Pair[] jsonProvidedTests = new Pair[]{
            new Pair<>("{}", notExists),
            new Pair<>("{\"mapped\":{\"keyOne\":\"3\",\"keyTwo\":\"4\"},\"indexed\":[\"1\",\"2\"]}", ValueWithExistsFlag.allExist(1, 2, "3", "4"))
        };
        env.compileDeploy("@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema " + JSONPROVIDED_TYPENAME + "()", path);
        runAssertion(env, JSONPROVIDED_TYPENAME, FJSON, null, jsonProvidedTests, Object.class, path);
    }

    private void runAssertion(RegressionEnvironment env,
                              String typename,
                              FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType, RegressionPath path) {

        String stmtText = "@name('s0') select " +
            "indexed[0]? as indexed1, " +
            "exists(indexed[0]?) as exists_indexed1, " +
            "indexed[1]? as indexed2, " +
            "exists(indexed[1]?) as exists_indexed2, " +
            "mapped('keyOne')? as mapped1, " +
            "exists(mapped('keyOne')?) as exists_mapped1, " +
            "mapped('keyTwo')? as mapped2,  " +
            "exists(mapped('keyTwo')?) as exists_mapped2  " +
            "from " + typename;
        env.compileDeploy(stmtText, path).addListener("s0");

        String[] propertyNames = "indexed1,indexed2,mapped1,mapped2".split(",");
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

        env.undeployAll();
    }

    public static class MyLocalJsonProvided implements Serializable {
        public int[] indexed;
        public Map<String, Object> mapped;
    }
}
