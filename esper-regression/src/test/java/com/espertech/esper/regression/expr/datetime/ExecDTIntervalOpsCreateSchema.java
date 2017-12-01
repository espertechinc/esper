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
package com.espertech.esper.regression.expr.datetime;

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.ConfigurationEventTypeXMLDOM;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.generic.GenericData;

import javax.xml.xpath.XPathConstants;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecDTIntervalOpsCreateSchema implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCreateSchema(epService);
    }

    private void runAssertionCreateSchema(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionCreateSchema(epService, rep);
        }
    }

    private void tryAssertionCreateSchema(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {

        String startA = "2002-05-30T09:00:00.000";
        String endA = "2002-05-30T09:00:01.000";
        String startB = "2002-05-30T09:00:00.500";
        String endB = "2002-05-30T09:00:00.700";

        // test Map type Long-type timestamps
        runAssertionCreateSchemaWTypes(epService, eventRepresentationEnum, "long",
                DateTime.parseDefaultMSec(startA), DateTime.parseDefaultMSec(endA),
                DateTime.parseDefaultMSec(startB), DateTime.parseDefaultMSec(endB));

        // test Map type Calendar-type timestamps
        if (!eventRepresentationEnum.isAvroEvent()) {
            runAssertionCreateSchemaWTypes(epService, eventRepresentationEnum, "java.util.Calendar",
                    DateTime.parseDefaultCal(startA), DateTime.parseDefaultCal(endA),
                    DateTime.parseDefaultCal(startB), DateTime.parseDefaultCal(endB));
        }

        // test Map type Date-type timestamps
        if (!eventRepresentationEnum.isAvroEvent()) {
            runAssertionCreateSchemaWTypes(epService, eventRepresentationEnum, "java.util.Date",
                    DateTime.parseDefaultDate(startA), DateTime.parseDefaultDate(endA),
                    DateTime.parseDefaultDate(startB), DateTime.parseDefaultDate(endB));
        }

        // test Map type LocalDateTime-type timestamps
        if (!eventRepresentationEnum.isAvroEvent()) {
            runAssertionCreateSchemaWTypes(epService, eventRepresentationEnum, "java.time.LocalDateTime",
                    DateTime.parseDefaultLocalDateTime(startA), DateTime.parseDefaultLocalDateTime(endA),
                    DateTime.parseDefaultLocalDateTime(startB), DateTime.parseDefaultLocalDateTime(endB));
        }

        // test Map type ZonedDateTime-type timestamps
        if (!eventRepresentationEnum.isAvroEvent()) {
            runAssertionCreateSchemaWTypes(epService, eventRepresentationEnum, "java.time.ZonedDateTime",
                    DateTime.parseDefaultZonedDateTime(startA), DateTime.parseDefaultZonedDateTime(endA),
                    DateTime.parseDefaultZonedDateTime(startB), DateTime.parseDefaultZonedDateTime(endB));
        }

        // test Bean-type Date-type timestamps
        String epl = eventRepresentationEnum.getAnnotationText() + " create schema SupportBean as " + SupportBean.class.getName() + " starttimestamp longPrimitive endtimestamp longBoxed";
        epService.getEPAdministrator().createEPL(epl);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select a.get('month') as val0 from SupportBean a");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean theEvent = new SupportBean();
        theEvent.setLongPrimitive(DateTime.parseDefaultMSec(startA));
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(4, listener.assertOneGetNewAndReset().get("val0"));

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl.trim(), model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(epl.trim(), stmt.getText());

        // try XML
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("ABC");
        desc.setStartTimestampPropertyName("mystarttimestamp");
        desc.setEndTimestampPropertyName("myendtimestamp");
        desc.addXPathProperty("mystarttimestamp", "/test/prop", XPathConstants.NUMBER);
        try {
            epService.getEPAdministrator().getConfiguration().addEventType("TypeXML", desc);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Declared start timestamp property 'mystarttimestamp' is expected to return a Date, Calendar or long-typed value but returns 'java.lang.Double'", ex.getMessage());
        }
    }

    private void runAssertionCreateSchemaWTypes(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum, String typeOfDatetimeProp, Object startA, Object endA, Object startB, Object endB) {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema TypeA as (startts " + typeOfDatetimeProp + ", endts " + typeOfDatetimeProp + ") starttimestamp startts endtimestamp endts");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema TypeB as (startts " + typeOfDatetimeProp + ", endts " + typeOfDatetimeProp + ") starttimestamp startts endtimestamp endts");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select a.includes(b) as val0 from TypeA#lastevent as a, TypeB#lastevent as b");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        makeSendEvent(epService, "TypeA", eventRepresentationEnum, startA, endA);
        makeSendEvent(epService, "TypeB", eventRepresentationEnum, startB, endB);
        assertEquals(true, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("TypeA", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("TypeB", true);
    }

    private void makeSendEvent(EPServiceProvider epService, String typeName, EventRepresentationChoice eventRepresentationEnum, Object startTs, Object endTs) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{startTs, endTs}, typeName);
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<>();
            theEvent.put("startts", startTs);
            theEvent.put("endts", endTs);
            epService.getEPRuntime().sendEvent(theEvent, typeName);
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, typeName));
            record.put("startts", startTs);
            record.put("endts", endTs);
            epService.getEPRuntime().sendEventAvro(record, typeName);
        } else {
            throw new IllegalStateException("Unrecognized enum " + eventRepresentationEnum);
        }
    }
}
