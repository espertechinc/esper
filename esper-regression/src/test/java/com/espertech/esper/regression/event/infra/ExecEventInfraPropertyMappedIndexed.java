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
package com.espertech.esper.regression.event.infra;

import com.espertech.esper.avro.core.AvroConstant;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static org.apache.avro.SchemaBuilder.*;
import static org.junit.Assert.*;

public class ExecEventInfraPropertyMappedIndexed implements RegressionExecution {
    private final static Class BEAN_TYPE = MyIMEvent.class;

    public void run(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPE);
        addMapEventType(epService);
        addOAEventType(epService);
        addAvroEventType(epService);

        runAssertion(epService, BEAN_TYPE.getSimpleName(), FBEAN, new MyIMEvent(new String[]{"v1", "v2"}, Collections.singletonMap("k1", "v1")));

        runAssertion(epService, MAP_TYPENAME, FMAP, twoEntryMap("indexed", new String[]{"v1", "v2"}, "mapped", Collections.singletonMap("k1", "v1")));

        runAssertion(epService, OA_TYPENAME, FOA, new Object[]{new String[]{"v1", "v2"}, Collections.singletonMap("k1", "v1")});

        // Avro
        GenericData.Record datum = new GenericData.Record(getAvroSchema());
        datum.put("indexed", Arrays.asList("v1", "v2"));
        datum.put("mapped", Collections.singletonMap("k1", "v1"));
        runAssertion(epService, AVRO_TYPENAME, FAVRO, datum);
    }

    private void runAssertion(EPServiceProvider epService,
                              String typename,
                              FunctionSendEvent send,
                              Object underlying) {

        runAssertionTypeValidProp(epService, typename, underlying);
        runAssertionTypeInvalidProp(epService, typename);

        String stmtText = "select * from " + typename;

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        send.apply(epService, underlying);
        EventBean event = listener.assertOneGetNewAndReset();

        EventPropertyGetterMapped mappedGetter = event.getEventType().getGetterMapped("mapped");
        assertEquals("v1", mappedGetter.get(event, "k1"));

        EventPropertyGetterIndexed indexedGetter = event.getEventType().getGetterIndexed("indexed");
        assertEquals("v2", indexedGetter.get(event, 1));

        runAssertionEventInvalidProp(event);
        SupportEventTypeAssertionUtil.assertConsistency(event);

        stmt.destroy();
    }

    private void addMapEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, twoEntryMap("indexed", String[].class, "mapped", Map.class));
    }

    private void addOAEventType(EPServiceProvider epService) {
        String[] names = {"indexed", "mapped"};
        Object[] types = {String[].class, Map.class};
        epService.getEPAdministrator().getConfiguration().addEventType(OA_TYPENAME, names, types);
    }

    private void addAvroEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(getAvroSchema()));
    }

    private static Schema getAvroSchema() {
        return record("AvroSchema").fields()
                .name("indexed").type(array().items().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
                .name("mapped").type(map().values().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
                .endRecord();
    }

    private void runAssertionEventInvalidProp(EventBean event) {
        for (String prop : Arrays.asList("xxxx", "mapped[1]", "indexed('a')", "mapped.x", "indexed.x")) {
            SupportMessageAssertUtil.tryInvalidProperty(event, prop);
            SupportMessageAssertUtil.tryInvalidGetFragment(event, prop);
        }
    }

    private void runAssertionTypeValidProp(EPServiceProvider epService, String typeName, Object underlying) {
        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType(typeName);

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
        assertEquals(underlying instanceof Map || underlying instanceof Object[] ? Object.class : String.class, eventType.getPropertyType("mapped('a')"));
        assertEquals(underlying instanceof GenericData.Record ? Collection.class : String[].class, eventType.getPropertyType("indexed"));
        assertEquals(String.class, eventType.getPropertyType("indexed[0]"));

        assertEquals(new EventPropertyDescriptor("indexed", underlying instanceof GenericData.Record ? Collection.class : String[].class, String.class, false, false, true, false, false), eventType.getPropertyDescriptor("indexed"));
        assertEquals(new EventPropertyDescriptor("mapped", Map.class, underlying instanceof Map || underlying instanceof Object[] ? Object.class : String.class, false, false, false, true, false), eventType.getPropertyDescriptor("mapped"));

        assertNull(eventType.getFragmentType("indexed"));
        assertNull(eventType.getFragmentType("mapped"));
    }

    private void runAssertionTypeInvalidProp(EPServiceProvider epService, String typeName) {
        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType(typeName);

        for (String prop : Arrays.asList("xxxx", "myString[0]", "indexed('a')", "indexed.x", "mapped[0]", "mapped.x")) {
            assertEquals(false, eventType.isProperty(prop));
            assertEquals(null, eventType.getPropertyType(prop));
            assertNull(eventType.getPropertyDescriptor(prop));
        }
    }


    public static class MyIMEvent {
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
}
