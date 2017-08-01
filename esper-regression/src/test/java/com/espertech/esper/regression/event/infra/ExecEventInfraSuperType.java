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

import com.espertech.esper.client.ConfigurationEventTypeAvro;
import com.espertech.esper.client.ConfigurationEventTypeObjectArray;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static com.espertech.esper.client.scopetest.SupportUpdateListener.getInvokedFlagsAndReset;
import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static org.apache.avro.SchemaBuilder.record;

public class ExecEventInfraSuperType implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        addMapEventTypes(epService);
        addOAEventTypes(epService);
        addAvroEventTypes(epService);
        addBeanTypes(epService);

        // Bean
        runAssertion(epService, "Bean", FBEANWTYPE, new Bean_Type_Root(), new Bean_Type_1(), new Bean_Type_2(), new Bean_Type_2_1());

        // Map
        runAssertion(epService, "Map", FMAPWTYPE, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());

        // OA
        runAssertion(epService, "OA", FOAWTYPE, new Object[0], new Object[0], new Object[0], new Object[0]);

        // Avro
        Schema fake = record("fake").fields().endRecord();
        runAssertion(epService, "Avro", FAVROWTYPE, new GenericData.Record(fake), new GenericData.Record(fake), new GenericData.Record(fake), new GenericData.Record(fake));
    }

    private void runAssertion(EPServiceProvider epService,
                              String typePrefix,
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
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, false, false, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(epService, type_1, typePrefix + "_" + typeNames[1]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, true, false, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(epService, type_2, typePrefix + "_" + typeNames[2]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, false, true, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(epService, type_2_1, typePrefix + "_" + typeNames[3]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, false, true, true}, getInvokedFlagsAndReset(listeners));

        for (int i = 0; i < statements.length; i++) {
            statements[i].destroy();
        }
    }

    private void addMapEventTypes(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("Map_Type_Root", Collections.emptyMap());
        epService.getEPAdministrator().getConfiguration().addEventType("Map_Type_1", Collections.emptyMap(), new String[]{"Map_Type_Root"});
        epService.getEPAdministrator().getConfiguration().addEventType("Map_Type_2", Collections.emptyMap(), new String[]{"Map_Type_Root"});
        epService.getEPAdministrator().getConfiguration().addEventType("Map_Type_2_1", Collections.emptyMap(), new String[]{"Map_Type_2"});
    }

    private void addOAEventTypes(EPServiceProvider epService) {
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

    private void addAvroEventTypes(EPServiceProvider epService) {
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

    private void addBeanTypes(EPServiceProvider epService) {
        for (Class clazz : Arrays.asList(Bean_Type_Root.class, Bean_Type_1.class, Bean_Type_2.class, Bean_Type_2_1.class)) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
    }

    public static class Bean_Type_Root {
    }

    public static class Bean_Type_1 extends Bean_Type_Root {
    }

    public static class Bean_Type_2 extends Bean_Type_Root {
    }

    public static class Bean_Type_2_1 extends Bean_Type_2 {
    }
}
