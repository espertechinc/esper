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
import com.espertech.esper.common.client.EventPropertyGetterIndexed;
import com.espertech.esper.common.client.EventPropertyGetterMapped;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertPropEquals;
import static com.espertech.esper.common.internal.util.CollectionUtil.twoEntryMap;
import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static org.junit.Assert.*;

public class EventInfraPropertyMappedIndexed implements RegressionExecution {
    private final static Class BEAN_TYPE = MyIMEvent.class;
    public final static String XML_TYPENAME = EventInfraPropertyMappedIndexed.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyMappedIndexed.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyMappedIndexed.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyMappedIndexed.class.getSimpleName() + "Avro";
    public final static String JSON_TYPENAME = EventInfraPropertyMappedIndexed.class.getSimpleName() + "Json";
    public final static String JSONPROVIDED_TYPENAME = EventInfraPropertyMappedIndexed.class.getSimpleName() + "JsonProvided";

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        runAssertion(env, BEAN_TYPE.getSimpleName(), FBEAN, new MyIMEvent(new String[]{"v1", "v2"}, Collections.singletonMap("k1", "v1")), path);

        runAssertion(env, MAP_TYPENAME, FMAP, twoEntryMap("indexed", new String[]{"v1", "v2"}, "mapped", Collections.singletonMap("k1", "v1")), path);

        runAssertion(env, OA_TYPENAME, FOA, new Object[]{new String[]{"v1", "v2"}, Collections.singletonMap("k1", "v1")}, path);

        // Avro
        Schema avroSchema = env.runtimeAvroSchemaPreconfigured(AVRO_TYPENAME);
        GenericData.Record datum = new GenericData.Record(avroSchema);
        datum.put("indexed", Arrays.asList("v1", "v2"));
        datum.put("mapped", Collections.singletonMap("k1", "v1"));
        runAssertion(env, AVRO_TYPENAME, FAVRO, datum, path);

        // Json
        env.compileDeploy("@public @buseventtype @name('schema') create json schema " + JSON_TYPENAME + "(indexed string[], mapped java.util.Map)", path);
        String json = "{\"mapped\":{\"k1\":\"v1\"},\"indexed\":[\"v1\",\"v2\"]}";
        runAssertion(env, JSON_TYPENAME, FJSON, json, path);

        // Json+ProvidedClass
        env.compileDeploy("@public @buseventtype @name('schema') @JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') create json schema " + JSONPROVIDED_TYPENAME + "()", path);
        runAssertion(env, JSONPROVIDED_TYPENAME, FJSON, json, path);
    }

    private void runAssertion(RegressionEnvironment env,
                              String typename,
                              FunctionSendEvent send,
                              Object underlying, RegressionPath path) {

        runAssertionTypeValidProp(env, typename, underlying);
        runAssertionTypeInvalidProp(env, typename);

        String stmtText = "@name('s0') select * from " + typename;
        env.compileDeploy(stmtText, path).addListener("s0");

        send.apply(env, underlying, typename);

        env.assertEventNew("s0", event -> {
            EventPropertyGetterMapped mappedGetter = event.getEventType().getGetterMapped("mapped");
            assertEquals("v1", mappedGetter.get(event, "k1"));

            EventPropertyGetterIndexed indexedGetter = event.getEventType().getGetterIndexed("indexed");
            assertEquals("v2", indexedGetter.get(event, 1));

            runAssertionEventInvalidProp(event);
            SupportEventTypeAssertionUtil.assertConsistency(event);
        });

        env.undeployAll();
    }

    private void runAssertionEventInvalidProp(EventBean event) {
        for (String prop : Arrays.asList("xxxx", "mapped[1]", "indexed('a')", "mapped.x", "indexed.x")) {
            SupportMessageAssertUtil.tryInvalidProperty(event, prop);
            SupportMessageAssertUtil.tryInvalidGetFragment(event, prop);
        }
    }

    private void runAssertionTypeValidProp(RegressionEnvironment env, String typeName, Object underlying) {
        env.assertThat(() -> {
            EventType eventType = env.runtime().getEventTypeService().getBusEventType(typeName);

            Object[][] expectedType = new Object[][]{{"indexed", underlying instanceof GenericData.Record ? Collection.class : String[].class, null, null}, {"mapped", Map.class, null, null}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, eventType, SupportEventTypeAssertionEnum.getSetWithFragment());

            EPAssertionUtil.assertEqualsAnyOrder(new String[]{"indexed", "mapped"}, eventType.getPropertyNames());

            assertNotNull(eventType.getGetter("mapped"));
            assertNotNull(eventType.getGetter("mapped('a')"));
            assertNotNull(eventType.getGetter("indexed"));
            assertNotNull(eventType.getGetter("indexed[0]"));
            assertTrue(eventType.isProperty("mapped"));
            assertTrue(eventType.isProperty("mapped('a')"));
            assertTrue(eventType.isProperty("indexed"));
            assertTrue(eventType.isProperty("indexed[0]"));
            assertEquals(Map.class, eventType.getPropertyType("mapped"));
            boolean mappedReturnsObject = typeName.equals(MAP_TYPENAME) || typeName.equals(OA_TYPENAME) || typeName.equals(JSON_TYPENAME) || typeName.equals(JSONPROVIDED_TYPENAME);
            assertEquals(mappedReturnsObject ? Object.class : String.class, eventType.getPropertyType("mapped('a')"));
            assertEquals(underlying instanceof GenericData.Record ? Collection.class : String[].class, eventType.getPropertyType("indexed"));
            assertEquals(String.class, eventType.getPropertyType("indexed[0]"));

            EPTypeClass indexedType;
            if (typeName.equals(AVRO_TYPENAME)) {
                indexedType = EPTypeClassParameterized.from(Collection.class, String.class);
            } else {
                indexedType = new EPTypeClass(String[].class);
            }
            assertPropEquals(new SupportEventPropDesc("indexed", indexedType).indexed().componentType(String.class), eventType.getPropertyDescriptor("indexed"));

            EPTypeClass mappedType;
            Class componentType;
            if (typeName.equals(MAP_TYPENAME) || typeName.equals(OA_TYPENAME) || typeName.equals(JSON_TYPENAME)) {
                componentType = Object.class;
                mappedType = new EPTypeClass(Map.class);
            } else {
                componentType = String.class;
                mappedType = EPTypeClassParameterized.from(Map.class, String.class, String.class);
            }
            assertPropEquals(new SupportEventPropDesc("mapped", mappedType).componentType(componentType).mapped(), eventType.getPropertyDescriptor("mapped"));

            assertNull(eventType.getFragmentType("indexed"));
            assertNull(eventType.getFragmentType("mapped"));
        });
    }

    private void runAssertionTypeInvalidProp(RegressionEnvironment env, String typeName) {
        env.assertThat(() -> {
            EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured(typeName);

            for (String prop : Arrays.asList("xxxx", "myString[0]", "indexed('a')", "indexed.x", "mapped[0]", "mapped.x")) {
                assertEquals(false, eventType.isProperty(prop));
                assertEquals(null, eventType.getPropertyType(prop));
                assertNull(eventType.getPropertyDescriptor(prop));
            }
        });
    }

    public static class MyIMEvent implements Serializable {
        private static final long serialVersionUID = -4510830296204659734L;
        private final String[] indexed;
        private final Map<String, String> mapped;

        public MyIMEvent(String[] indexed, Map<String, String> mapped) {
            this.indexed = indexed;
            this.mapped = mapped;
        }

        public String[] getIndexed() {
            return indexed;
        }

        public Map<String, String> getMapped() {
            return mapped;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = -3336398496266088571L;
        public String[] indexed;
        public Map<String, String> mapped;
    }
}
