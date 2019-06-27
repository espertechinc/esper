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
import com.espertech.esper.common.client.render.JSONEventRenderer;
import com.espertech.esper.common.client.render.XMLEventRenderer;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static org.junit.Assert.assertEquals;

public class EventInfraEventRenderer implements RegressionExecution {
    public final static String XML_TYPENAME = "EventInfraEventRendererXML";
    private final static Class BEAN_TYPE = EventInfraEventRenderer.MyEvent.class;
    public final static String MAP_TYPENAME = "EventInfraEventRendererMap";
    public final static String OA_TYPENAME = "EventInfraEventRendererOA";
    public final static String AVRO_TYPENAME = "EventInfraEventRendererAvro";
    public final static String JSON_TYPENAME = "EventInfraEventRendererJson";
    public final static String JSONPROVIDED_TYPENAME = "EventInfraEventRendererJsonProvided";

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        // Bean
        runAssertion(env, BEAN_TYPE.getSimpleName(), FBEAN, new MyEvent(1, "abc", new MyInsideEvent(10)), path);

        // Map
        Map<String, Object> mapInner = new HashMap();
        mapInner.put("myInsideInt", 10);
        Map<String, Object> topInner = new HashMap();
        topInner.put("myInt", 1);
        topInner.put("myString", "abc");
        topInner.put("nested", mapInner);
        runAssertion(env, MAP_TYPENAME, FMAP, topInner, path);

        // Object-array
        Object[] oaInner = new Object[]{10};
        Object[] oaTop = new Object[]{1, "abc", oaInner};
        runAssertion(env, OA_TYPENAME, FOA, oaTop, path);

        // XML
        String xml = "<myevent myInt=\"1\" myString=\"abc\"><nested myInsideInt=\"10\"/></myevent>";
        runAssertion(env, XML_TYPENAME, FXML, xml, path);

        // Avro
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        Schema innerSchema = schema.getField("nested").schema();
        GenericData.Record avroInner = new GenericData.Record(innerSchema);
        avroInner.put("myInsideInt", 10);
        GenericData.Record avro = new GenericData.Record(schema);
        avro.put("myInt", 1);
        avro.put("myString", "abc");
        avro.put("nested", avroInner);
        runAssertion(env, AVRO_TYPENAME, FAVRO, avro, path);

        // Json
        String schemasJson = "create json schema Nested(myInsideInt int);\n" +
            "@public @buseventtype @name('schema') create json schema " + JSON_TYPENAME + "(myInt int, myString string, nested Nested)";
        env.compileDeploy(schemasJson, path);
        String json = "{\n" +
            "  \"myInt\": 1,\n" +
            "  \"myString\": \"abc\",\n" +
            "  \"nested\": {\n" +
            "    \"myInsideInt\": 10\n" +
            "  }\n" +
            "}";
        runAssertion(env, JSON_TYPENAME, FJSON, json, path);

        // Json-Class-Provided
        String schemas = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') " +
            "@public @buseventtype @name('schema') create json schema " + JSONPROVIDED_TYPENAME + "()";
        env.compileDeploy(schemas, path);
        runAssertion(env, JSONPROVIDED_TYPENAME, FJSON, json, path);
    }

    private void runAssertion(RegressionEnvironment env, String typename, FunctionSendEvent send, Object event, RegressionPath path) {
        String epl = "@name('s0') select * from " + typename;
        env.compileDeploy(epl, path).addListener("s0");
        send.apply(env, event, typename);

        EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();

        JSONEventRenderer jsonEventRenderer = env.runtime().getRenderEventService().getJSONRenderer(env.statement("s0").getEventType());
        String json = jsonEventRenderer.render(eventBean).replaceAll("(\\s|\\n|\\t)", "");
        assertEquals("{\"myInt\":1,\"myString\":\"abc\",\"nested\":{\"myInsideInt\":10}}", json);

        XMLEventRenderer xmlEventRenderer = env.runtime().getRenderEventService().getXMLRenderer(env.statement("s0").getEventType());
        String xml = xmlEventRenderer.render("root", eventBean).replaceAll("(\\s|\\n|\\t)", "");
        assertEquals("<?xmlversion=\"1.0\"encoding=\"UTF-8\"?><root><myInt>1</myInt><myString>abc</myString><nested><myInsideInt>10</myInsideInt></nested></root>", xml);

        env.undeployAll();
    }

    public final static class MyEvent {
        private int myInt;
        private String myString;
        private MyInsideEvent nested;

        public MyEvent(int myInt, String myString, MyInsideEvent nested) {
            this.myInt = myInt;
            this.myString = myString;
            this.nested = nested;
        }

        public int getMyInt() {
            return myInt;
        }

        public String getMyString() {
            return myString;
        }

        public MyInsideEvent getNested() {
            return nested;
        }
    }

    public final static class MyInsideEvent {
        private int myInsideInt;

        public MyInsideEvent(int myInsideInt) {
            this.myInsideInt = myInsideInt;
        }

        public int getMyInsideInt() {
            return myInsideInt;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public int myInt;
        public String myString;
        public MyLocalJsonProvidedNested nested;
    }

    public static class MyLocalJsonProvidedNested implements Serializable {
        public int myInsideInt;
    }
}
