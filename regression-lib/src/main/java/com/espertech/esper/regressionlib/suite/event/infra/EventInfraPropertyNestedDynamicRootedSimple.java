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
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.bean.SupportMarkerImplA;
import com.espertech.esper.regressionlib.support.bean.SupportMarkerInterface;
import com.espertech.esper.regressionlib.support.events.SupportEventInfra;
import com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.common.internal.util.CollectionUtil.twoEntryMap;
import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag.*;
import static org.junit.Assert.assertEquals;

public class EventInfraPropertyNestedDynamicRootedSimple implements RegressionExecution {
    public final static String XML_TYPENAME = EventInfraPropertyNestedDynamicRootedSimple.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyNestedDynamicRootedSimple.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyNestedDynamicRootedSimple.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyNestedDynamicRootedSimple.class.getSimpleName() + "Avro";
    private final static Class BEAN_TYPE = SupportMarkerInterface.class;
    private final static ValueWithExistsFlag[] NOT_EXISTS = multipleNotExists(3);

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        // Bean
        Pair[] beanTests = new Pair[]{
            new Pair<>(SupportBeanComplexProps.makeDefaultBean(), allExist("simple", "nestedValue", "nestedNestedValue")),
            new Pair<>(new SupportMarkerImplA("x"), NOT_EXISTS),
        };
        runAssertion(env, BEAN_TYPE.getSimpleName(), FBEAN, null, beanTests, Object.class);

        // Map
        Map<String, Object> mapNestedNestedOne = Collections.singletonMap("nestedNestedValue", 101);
        Map<String, Object> mapNestedOne = twoEntryMap("nestedNested", mapNestedNestedOne, "nestedValue", "abc");
        Map<String, Object> mapOne = twoEntryMap("simpleProperty", 5, "nested", mapNestedOne);
        Pair[] mapTests = new Pair[]{
            new Pair<>(Collections.singletonMap("simpleProperty", "a"), new ValueWithExistsFlag[]{exists("a"), notExists(), notExists()}),
            new Pair<>(mapOne, allExist(5, "abc", 101)),
        };
        runAssertion(env, MAP_TYPENAME, FMAP, null, mapTests, Object.class);

        // Object-Array
        Object[] oaNestedNestedOne = new Object[]{101};
        Object[] oaNestedOne = new Object[]{"abc", oaNestedNestedOne};
        Object[] oaOne = new Object[]{5, oaNestedOne};
        Pair[] oaTests = new Pair[]{
            new Pair<>(new Object[]{"a", null}, new ValueWithExistsFlag[]{exists("a"), notExists(), notExists()}),
            new Pair<>(oaOne, allExist(5, "abc", 101)),
        };
        runAssertion(env, OA_TYPENAME, FOA, null, oaTests, Object.class);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("<simpleProperty>abc</simpleProperty>" +
                "<nested nestedValue=\"100\">\n" +
                "\t<nestedNested nestedNestedValue=\"101\">\n" +
                "\t</nestedNested>\n" +
                "</nested>\n", allExist("abc", "100", "101")),
            new Pair<>("<nested/>", NOT_EXISTS),
        };
        runAssertion(env, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class);

        // Avro
        Schema avroSchema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        GenericData.Record datumNull = new GenericData.Record(avroSchema);
        Schema schema = avroSchema;
        Schema nestedSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(schema.getField("nested").schema());
        Schema nestedNestedSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(nestedSchema.getField("nestedNested").schema());
        GenericData.Record nestedNestedDatum = new GenericData.Record(nestedNestedSchema);
        nestedNestedDatum.put("nestedNestedValue", 101);
        GenericData.Record nestedDatum = new GenericData.Record(nestedSchema);
        nestedDatum.put("nestedValue", 100);
        nestedDatum.put("nestedNested", nestedNestedDatum);
        GenericData.Record datumOne = new GenericData.Record(schema);
        datumOne.put("simpleProperty", "abc");
        datumOne.put("nested", nestedDatum);
        Pair[] avroTests = new Pair[]{
            new Pair<>(new GenericData.Record(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord()), NOT_EXISTS),
            new Pair<>(datumNull, new ValueWithExistsFlag[]{exists(null), notExists(), notExists()}),
            new Pair<>(datumOne, allExist("abc", 100, 101)),
        };
        runAssertion(env, AVRO_TYPENAME, FAVRO, null, avroTests, Object.class);
    }

    private void runAssertion(RegressionEnvironment env,
                              String typename,
                              SupportEventInfra.FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType) {

        String stmtText = "@name('s0') select " +
            "simpleProperty? as simple, " +
            "exists(simpleProperty?) as exists_simple, " +
            "nested?.nestedValue as nested, " +
            "exists(nested?.nestedValue) as exists_nested, " +
            "nested?.nestedNested.nestedNestedValue as nestedNested, " +
            "exists(nested?.nestedNested.nestedNestedValue) as exists_nestedNested " +
            "from " + typename;
        env.compileDeploy(stmtText).addListener("s0");

        String[] propertyNames = "simple,nested,nestedNested".split(",");
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
}
