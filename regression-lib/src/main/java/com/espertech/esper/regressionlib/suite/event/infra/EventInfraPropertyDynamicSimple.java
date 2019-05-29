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
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportMarkerImplA;
import com.espertech.esper.regressionlib.support.bean.SupportMarkerImplB;
import com.espertech.esper.regressionlib.support.bean.SupportMarkerImplC;
import com.espertech.esper.regressionlib.support.bean.SupportMarkerInterface;
import com.espertech.esper.regressionlib.support.events.SupportEventInfra;
import com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.function.Function;

import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag.exists;
import static com.espertech.esper.regressionlib.support.events.ValueWithExistsFlag.notExists;
import static org.junit.Assert.assertEquals;

public class EventInfraPropertyDynamicSimple implements RegressionExecution {
    public final static String XML_TYPENAME = EventInfraPropertyDynamicSimple.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyDynamicSimple.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyDynamicSimple.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyDynamicSimple.class.getSimpleName() + "Avro";
    public final static String JSON_TYPENAME = EventInfraPropertyDynamicSimple.class.getSimpleName() + "Json";

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        // Bean
        Pair[] beanTests = new Pair[]{
            new Pair<>(new SupportMarkerImplA("e1"), exists("e1")),
            new Pair<>(new SupportMarkerImplB(1), exists(1)),
            new Pair<>(new SupportMarkerImplC(), notExists())
        };
        runAssertion(env, SupportMarkerInterface.class.getSimpleName(), FBEAN, null, beanTests, Object.class, path);

        // Map
        Pair[] mapTests = new Pair[]{
            new Pair<>(Collections.singletonMap("somekey", "10"), notExists()),
            new Pair<>(Collections.singletonMap("id", "abc"), exists("abc")),
            new Pair<>(Collections.singletonMap("id", 10), exists(10)),
        };
        runAssertion(env, MAP_TYPENAME, FMAP, null, mapTests, Object.class, path);

        // Object-Array
        Pair[] oaTests = new Pair[]{
            new Pair<>(new Object[]{1, null}, exists(null)),
            new Pair<>(new Object[]{2, "abc"}, exists("abc")),
            new Pair<>(new Object[]{3, 10}, exists(10)),
        };
        runAssertion(env, OA_TYPENAME, FOA, null, oaTests, Object.class, path);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("", notExists()),
            new Pair<>("<id>10</id>", exists("10")),
            new Pair<>("<id>abc</id>", exists("abc")),
        };
        runAssertion(env, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class, path);

        // Avro
        Schema avroSchema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        GenericData.Record datumEmpty = new GenericData.Record(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord());
        GenericData.Record datumOne = new GenericData.Record(avroSchema);
        datumOne.put("id", 101);
        GenericData.Record datumTwo = new GenericData.Record(avroSchema);
        datumTwo.put("id", null);
        Pair[] avroTests = new Pair[]{
            new Pair<>(datumEmpty, notExists()),
            new Pair<>(datumOne, exists(101)),
            new Pair<>(datumTwo, exists(null))
        };
        runAssertion(env, AVRO_TYPENAME, FAVRO, null, avroTests, Object.class, path);

        // Json
        env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema " + JSON_TYPENAME + "()", path);
        Pair[] jsonTests = new Pair[]{
            new Pair<>("{}", notExists()),
            new Pair<>("{\"id\": 10}", exists(10)),
            new Pair<>("{\"id\": \"abc\"}", exists("abc"))
        };
        runAssertion(env, JSON_TYPENAME, FJSON, null, jsonTests, Object.class, path);
    }

    private void runAssertion(RegressionEnvironment env,
                              String typename,
                              FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType,
                              RegressionPath path) {

        String stmtText = "@name('s0') select id? as myid, exists(id?) as exists_myid from " + typename;
        env.compileDeploy(stmtText, path).addListener("s0");

        assertEquals(expectedPropertyType, env.statement("s0").getEventType().getPropertyType("myid"));
        assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("exists_myid"));

        for (Pair pair : tests) {
            send.apply(env, pair.getFirst(), typename);
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            SupportEventInfra.assertValueMayConvert(event, "myid", (ValueWithExistsFlag) pair.getSecond(), optionalValueConversion);
        }

        env.undeployAll();
    }

    private void addMapEventType(RegressionEnvironment env) {
    }

    private void addOAEventType(RegressionEnvironment env) {
    }

    private void addAvroEventType(RegressionEnvironment env) {
    }

}
