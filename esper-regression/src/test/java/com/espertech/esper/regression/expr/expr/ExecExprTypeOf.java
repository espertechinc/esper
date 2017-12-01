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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecExprTypeOf implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionFragment(epService);
        runAssertionNamedUnnamedPOJO(epService);
        runAssertionVariantStream(epService);
        runAssertionInvalid(epService);
        runAssertionDynamicProps(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        SupportMessageAssertUtil.tryInvalid(epService, "select typeof(xx) from java.lang.Object",
                "Error starting statement: Failed to validate select-clause expression 'typeof(xx)': Property named 'xx' is not valid in any stream [select typeof(xx) from java.lang.Object]");
    }

    private void runAssertionDynamicProps(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL(EventRepresentationChoice.MAP.getAnnotationText() + " create schema MyDynoPropSchema as (key string)");

        String stmtText = "select typeof(prop?), typeof(key) from MyDynoPropSchema as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionDynamicProps(epService, listener);

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        tryAssertionDynamicProps(epService, listener);

        stmt.destroy();
    }

    private void tryAssertionDynamicProps(EPServiceProvider epService, SupportUpdateListener listener) {

        String[] fields = new String[]{"typeof(prop?)", "typeof(key)"};

        sendSchemaEvent(epService, 1, "E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"Integer", "String"});

        sendSchemaEvent(epService, "test", "E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"String", "String"});

        sendSchemaEvent(epService, null, "E3");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, "String"});
    }

    private void sendSchemaEvent(EPServiceProvider epService, Object prop, String key) {
        Map<String, Object> theEvent = new HashMap<>();
        theEvent.put("prop", prop);
        theEvent.put("key", key);

        if (EventRepresentationChoice.getEngineDefault(epService).isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(theEvent, "MyDynoPropSchema");
        } else {
            epService.getEPRuntime().sendEvent(theEvent, "MyDynoPropSchema");
        }
    }

    private void runAssertionVariantStream(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionVariantStream(epService, rep);
        }
    }

    private void tryAssertionVariantStream(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema EventOne as (key string)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema EventTwo as (key string)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema S0 as " + SupportBean_S0.class.getName());
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create variant schema VarSchema as *");

        epService.getEPAdministrator().createEPL("insert into VarSchema select * from EventOne");
        epService.getEPAdministrator().createEPL("insert into VarSchema select * from EventTwo");
        epService.getEPAdministrator().createEPL("insert into VarSchema select * from S0");
        epService.getEPAdministrator().createEPL("insert into VarSchema select * from SupportBean");

        String stmtText = "select typeof(A) as t0 from VarSchema as A";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"value"}, "EventOne");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(Collections.singletonMap("key", "value"), "EventOne");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SchemaBuilder.record("EventOne").fields().requiredString("key").endRecord());
            record.put("key", "value");
            epService.getEPRuntime().sendEventAvro(record, "EventOne");
        } else {
            fail();
        }
        assertEquals("EventOne", listener.assertOneGetNewAndReset().get("t0"));

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"value"}, "EventTwo");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(Collections.singletonMap("key", "value"), "EventTwo");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SchemaBuilder.record("EventTwo").fields().requiredString("key").endRecord());
            record.put("key", "value");
            epService.getEPRuntime().sendEventAvro(record, "EventTwo");
        } else {
            fail();
        }
        assertEquals("EventTwo", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals("S0", listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals("SupportBean", listener.assertOneGetNewAndReset().get("t0"));

        stmt.destroy();
        listener.reset();
        stmt = epService.getEPAdministrator().createEPL("select * from VarSchema match_recognize(\n" +
                "  measures A as a, B as b\n" +
                "  pattern (A B)\n" +
                "  define A as typeof(A) = \"EventOne\",\n" +
                "         B as typeof(B) = \"EventTwo\"\n" +
                "  )");
        stmt.addListener(listener);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"value"}, "EventOne");
            epService.getEPRuntime().sendEvent(new Object[]{"value"}, "EventTwo");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(Collections.singletonMap("key", "value"), "EventOne");
            epService.getEPRuntime().sendEvent(Collections.singletonMap("key", "value"), "EventTwo");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = SchemaBuilder.record("EventTwo").fields().requiredString("key").endRecord();
            GenericData.Record eventOne = new GenericData.Record(schema);
            eventOne.put("key", "value");
            GenericData.Record eventTwo = new GenericData.Record(schema);
            eventTwo.put("key", "value");
            epService.getEPRuntime().sendEventAvro(eventOne, "EventOne");
            epService.getEPRuntime().sendEventAvro(eventTwo, "EventTwo");
        } else {
            fail();
        }
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "EventOne,EventTwo,S0,VarSchema".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void runAssertionNamedUnnamedPOJO(EPServiceProvider epService) {
        // test name-provided or no-name-provided
        epService.getEPAdministrator().getConfiguration().addEventType("ISupportA", ISupportA.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ISupportABCImpl", ISupportABCImpl.class);

        String stmtText = "select typeof(A) as t0 from ISupportA as A";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new ISupportAImpl(null, null));
        assertEquals(ISupportAImpl.class.getName(), listener.assertOneGetNewAndReset().get("t0"));

        epService.getEPRuntime().sendEvent(new ISupportABCImpl(null, null, null, null));
        assertEquals("ISupportABCImpl", listener.assertOneGetNewAndReset().get("t0"));

        stmt.destroy();
    }

    private void runAssertionFragment(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionFragment(epService, rep);
        }
    }

    private void tryAssertionFragment(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema InnerSchema as (key string)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MySchema as (inside InnerSchema, insidearr InnerSchema[])");

        String[] fields = new String[]{"t0", "t1"};
        String stmtText = eventRepresentationEnum.getAnnotationText() + " select typeof(s0.inside) as t0, typeof(s0.insidearr) as t1 from MySchema as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[2], "MySchema");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(new HashMap<String, Object>(), "MySchema");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            epService.getEPRuntime().sendEventAvro(new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "MySchema")), "MySchema");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{new Object[2], null}, "MySchema");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new HashMap<>();
            theEvent.put("inside", new HashMap<String, Object>());
            epService.getEPRuntime().sendEvent(theEvent, "MySchema");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema mySchema = SupportAvroUtil.getAvroSchema(epService, "MySchema");
            Schema innerSchema = SupportAvroUtil.getAvroSchema(epService, "InnerSchema");
            GenericData.Record event = new GenericData.Record(mySchema);
            event.put("inside", new GenericData.Record(innerSchema));
            epService.getEPRuntime().sendEventAvro(event, "MySchema");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"InnerSchema", null});

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{null, new Object[2][]}, "MySchema");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new HashMap<>();
            theEvent.put("insidearr", new Map[0]);
            epService.getEPRuntime().sendEvent(theEvent, "MySchema");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "MySchema"));
            event.put("insidearr", Collections.emptyList());
            epService.getEPRuntime().sendEventAvro(event, "MySchema");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, "InnerSchema[]"});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("InnerSchema", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MySchema", true);
    }
}
