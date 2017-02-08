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
package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportMarkerInterface;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.client.scopetest.SupportUpdateListener.getInvokedFlagsAndReset;
import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static org.apache.avro.SchemaBuilder.record;

public class TestEventInfraSuperType extends TestCase {
    private EPServiceProvider epService;

    protected void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        addMapEventTypes();
        addOAEventTypes();
        addAvroEventTypes();
        addBeanTypes();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportMarkerInterface.class);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testIt() throws Exception {
        // Bean
        runAssertion("Bean", FBEANWTYPE, new Bean_Type_Root(), new Bean_Type_1(), new Bean_Type_2(), new Bean_Type_2_1());

        // Map
        runAssertion("Map", FMAPWTYPE, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());

        // OA
        runAssertion("OA", FOAWTYPE, new Object[0], new Object[0], new Object[0], new Object[0]);

        // Avro
        Schema fake = record("fake").fields().endRecord();
        runAssertion("Avro", FAVROWTYPE, new GenericData.Record(fake), new GenericData.Record(fake), new GenericData.Record(fake), new GenericData.Record(fake));
    }

    private void runAssertion(String typePrefix,
                              FunctionSendEventWType sender,
                              Object root, Object type_1, Object type_2, Object type_2_1) {

        String[] typeNames = "Type_Root,Type_1,Type_2,Type_2_1".split(",");
        EPStatement[] statements = new EPStatement[4];
        SupportUpdateListener[] listeners = new SupportUpdateListener[4];
        for (int i = 0; i < typeNames.length; i++) {
            statements[i] = epService.getEPAdministrator().createEPL("select * from " + typePrefix + "_" + typeNames[i]);
            listeners[i] = new SupportUpdateListener();
            statements[i].addListener(listeners[i]);
        }

        sender.apply(epService, root, typePrefix + "_" + typeNames[0]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[] {true, false, false, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(epService, type_1, typePrefix + "_" + typeNames[1]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[] {true, true, false, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(epService, type_2, typePrefix + "_" + typeNames[2]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[] {true, false, true, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(epService, type_2_1, typePrefix + "_" + typeNames[3]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[] {true, false, true, true}, getInvokedFlagsAndReset(listeners));

        for (int i = 0; i < statements.length; i++) {
            statements[i].destroy();
        }
    }

    private void addMapEventTypes() {
        epService.getEPAdministrator().getConfiguration().addEventType("Map_Type_Root", Collections.emptyMap());
        epService.getEPAdministrator().getConfiguration().addEventType("Map_Type_1", Collections.emptyMap(), new String[] {"Map_Type_Root"});
        epService.getEPAdministrator().getConfiguration().addEventType("Map_Type_2", Collections.emptyMap(), new String[] {"Map_Type_Root"});
        epService.getEPAdministrator().getConfiguration().addEventType("Map_Type_2_1", Collections.emptyMap(), new String[] {"Map_Type_2"});
    }

    private void addOAEventTypes() {
        epService.getEPAdministrator().getConfiguration().addEventType("OA_Type_Root", new String[0], new Object[0]);

        ConfigurationEventTypeObjectArray array_1 = new ConfigurationEventTypeObjectArray();
        array_1.setSuperTypes(Collections.singleton("OA_Type_Root"));
        epService.getEPAdministrator().getConfiguration().addEventType("OA_Type_1", new String[0], new Object[0], array_1);

        ConfigurationEventTypeObjectArray array_2 = new ConfigurationEventTypeObjectArray();
        array_2.setSuperTypes(Collections.singleton("OA_Type_Root"));
        epService.getEPAdministrator().getConfiguration().addEventType("OA_Type_2", new String[0], new Object[0], array_2);

        ConfigurationEventTypeObjectArray array_2_1 = new ConfigurationEventTypeObjectArray();
        array_2_1.setSuperTypes(Collections.singleton("OA_Type_2"));
        epService.getEPAdministrator().getConfiguration().addEventType("OA_Type_2_1", new String[0], new Object[0], array_2_1);
    }

    private void addAvroEventTypes() {
        Schema fake = record("fake").fields().endRecord();
        ConfigurationEventTypeAvro avro_root = new ConfigurationEventTypeAvro();
        avro_root.setAvroSchema(fake);
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("Avro_Type_Root", avro_root);
        ConfigurationEventTypeAvro avro_1 = new ConfigurationEventTypeAvro();
        avro_1.setSuperTypes(Collections.singleton("Avro_Type_Root"));
        avro_1.setAvroSchema(fake);
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("Avro_Type_1", avro_1);
        ConfigurationEventTypeAvro avro_2 = new ConfigurationEventTypeAvro();
        avro_2.setSuperTypes(Collections.singleton("Avro_Type_Root"));
        avro_2.setAvroSchema(fake);
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("Avro_Type_2", avro_2);
        ConfigurationEventTypeAvro avro_2_1 = new ConfigurationEventTypeAvro();
        avro_2_1.setSuperTypes(Collections.singleton("Avro_Type_2"));
        avro_2_1.setAvroSchema(fake);
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("Avro_Type_2_1", avro_2_1);
    }

    private void addBeanTypes() {
        for (Class clazz : Arrays.asList(Bean_Type_Root.class, Bean_Type_1.class, Bean_Type_2.class, Bean_Type_2_1.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
    }

    private static class Bean_Type_Root {}
    private static class Bean_Type_1 extends Bean_Type_Root {}
    private static class Bean_Type_2 extends Bean_Type_Root {}
    private static class Bean_Type_2_1 extends Bean_Type_2 {}
}
