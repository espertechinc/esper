/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.support.SupportEventTypeAssertionUtil;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static org.apache.avro.SchemaBuilder.*;

public class TestEventInfraGetterMappedIndexed extends TestCase {
    private final static Class BEAN_TYPE = MyIMEvent.class;

    private EPServiceProvider epService;

    protected void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPE);
        addMapEventType();
        addOAEventType();
        addAvroEventType();

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testIt() {

        runAssertion(BEAN_TYPE.getSimpleName(), FBEAN, new MyIMEvent(Collections.singletonMap("k1", "v1"), new String[] {"v1", "v2"}));

        runAssertion(MAP_TYPENAME, FMAP, twoEntryMap("mapped", Collections.singletonMap("k1", "v1"), "indexed", new String[] {"v1", "v2"}));

        runAssertion(OA_TYPENAME, FOA, new Object[] {Collections.singletonMap("k1", "v1"), new String[] {"v1", "v2"}});

        // Avro
        GenericData.Record datum = new GenericData.Record(getAvroSchema());
        datum.put("indexed", Arrays.asList("v1", "v2"));
        datum.put("mapped", Collections.singletonMap("k1", "v1"));
        runAssertion(AVRO_TYPENAME, FAVRO, datum);
    }

    private void runAssertion(String typename,
                              FunctionSendEvent send,
                              Object underlying) {

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

        SupportEventTypeAssertionUtil.assertConsistency(event);
        stmt.destroy();
    }

    private void addMapEventType() {
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, twoEntryMap("mapped", Map.class, "indexed", String[].class));
    }

    private void addOAEventType() {
        String[] names = {"mapped", "indexed"};
        Object[] types = {Map.class, String[].class};
        epService.getEPAdministrator().getConfiguration().addEventType(OA_TYPENAME, names, types);
    }

    private void addAvroEventType() {
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(getAvroSchema()));
    }

    private static Schema getAvroSchema() {
        return record("AvroSchema").fields()
                .name("mapped").type(map().values().stringType()).noDefault()
                .name("indexed").type(array().items().stringType()).noDefault()
                .endRecord();
    }

    public static class MyIMEvent {
        private final Map<String, String> mapped;
        private final String[] indexed;

        public MyIMEvent(Map<String, String> mapped, String[] indexed) {
            this.mapped = mapped;
            this.indexed = indexed;
        }

        public Map<String, String> getMapped() {
            return mapped;
        }

        public String[] getIndexed() {
            return indexed;
        }
    }
}
