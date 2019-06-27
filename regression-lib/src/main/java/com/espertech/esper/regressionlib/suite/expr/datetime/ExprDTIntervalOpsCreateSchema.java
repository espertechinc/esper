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
package com.espertech.esper.regressionlib.suite.expr.datetime;

import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExprDTIntervalOpsCreateSchema implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionCreateSchema(env, rep);
        }

        // test Bean-type Date-type timestamps
        String startA = "2002-05-30T09:00:00.000";
        String epl = " create schema SupportBeanXXX as " + SupportBean.class.getName() + " starttimestamp longPrimitive endtimestamp longBoxed;\n";
        epl += "@name('s0') select a.get('month') as val0 from SupportBeanXXX a;\n";
        env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

        SupportBean theEvent = new SupportBean();
        theEvent.setLongPrimitive(DateTime.parseDefaultMSec(startA));
        env.eventService().sendEventBean(theEvent, "SupportBeanXXX");
        assertEquals(4, env.listener("s0").assertOneGetNewAndReset().get("val0"));

        env.undeployAll();
    }

    private void tryAssertionCreateSchema(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        String startA = "2002-05-30T09:00:00.000";
        String endA = "2002-05-30T09:00:01.000";
        String startB = "2002-05-30T09:00:00.500";
        String endB = "2002-05-30T09:00:00.700";

        // test Map type Long-type timestamps
        runAssertionCreateSchemaWTypes(env, eventRepresentationEnum, "long",
            DateTime.parseDefaultMSec(startA), DateTime.parseDefaultMSec(endA),
            DateTime.parseDefaultMSec(startB), DateTime.parseDefaultMSec(endB),
            MyLocalJsonProvidedLong.class);

        // test Map type Calendar-type timestamps
        if (!eventRepresentationEnum.isAvroOrJsonEvent()) {
            runAssertionCreateSchemaWTypes(env, eventRepresentationEnum, "java.util.Calendar",
                DateTime.parseDefaultCal(startA), DateTime.parseDefaultCal(endA),
                DateTime.parseDefaultCal(startB), DateTime.parseDefaultCal(endB),
                MyLocalJsonProvidedCalendar.class);
        }

        // test Map type Date-type timestamps
        if (!eventRepresentationEnum.isAvroOrJsonEvent()) {
            runAssertionCreateSchemaWTypes(env, eventRepresentationEnum, "java.util.Date",
                DateTime.parseDefaultDate(startA), DateTime.parseDefaultDate(endA),
                DateTime.parseDefaultDate(startB), DateTime.parseDefaultDate(endB),
                MyLocalJsonProvidedDate.class);
        }

        // test Map type LocalDateTime-type timestamps
        if (!eventRepresentationEnum.isAvroOrJsonEvent()) {
            runAssertionCreateSchemaWTypes(env, eventRepresentationEnum, "java.time.LocalDateTime",
                DateTime.parseDefaultLocalDateTime(startA), DateTime.parseDefaultLocalDateTime(endA),
                DateTime.parseDefaultLocalDateTime(startB), DateTime.parseDefaultLocalDateTime(endB),
                MyLocalJsonProvidedLocalDateTime.class);
        }

        // test Map type ZonedDateTime-type timestamps
        if (!eventRepresentationEnum.isAvroOrJsonEvent()) {
            runAssertionCreateSchemaWTypes(env, eventRepresentationEnum, "java.time.ZonedDateTime",
                DateTime.parseDefaultZonedDateTime(startA), DateTime.parseDefaultZonedDateTime(endA),
                DateTime.parseDefaultZonedDateTime(startB), DateTime.parseDefaultZonedDateTime(endB),
                MyLocalJsonProvidedZonedDateTime.class);
        }
    }

    private void runAssertionCreateSchemaWTypes(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String typeOfDatetimeProp, Object startA, Object endA, Object startB, Object endB, Class jsonClass) {
        String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(jsonClass) + " create schema TypeA as (startts " + typeOfDatetimeProp + ", endts " + typeOfDatetimeProp + ") starttimestamp startts endtimestamp endts;\n";
        epl += eventRepresentationEnum.getAnnotationTextWJsonProvided(jsonClass) + " create schema TypeB as (startts " + typeOfDatetimeProp + ", endts " + typeOfDatetimeProp + ") starttimestamp startts endtimestamp endts;\n";
        epl += "@name('s0') select a.includes(b) as val0 from TypeA#lastevent as a, TypeB#lastevent as b;\n";
        env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

        makeSendEvent(env, "TypeA", eventRepresentationEnum, startA, endA);
        makeSendEvent(env, "TypeB", eventRepresentationEnum, startB, endB);
        assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("val0"));

        env.undeployAll();
    }

    private void makeSendEvent(RegressionEnvironment env, String typeName, EventRepresentationChoice eventRepresentationEnum, Object startTs, Object endTs) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{startTs, endTs}, typeName);
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<>();
            theEvent.put("startts", startTs);
            theEvent.put("endts", endTs);
            env.sendEventMap(theEvent, typeName);
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(typeName)));
            record.put("startts", startTs);
            record.put("endts", endTs);
            env.eventService().sendEventAvro(record, typeName);
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            String json = "{\"startts\": \"" + startTs + "\", \"endts\": \"" + endTs + "\"}";
            env.eventService().sendEventJson(json, typeName);
        } else {
            throw new IllegalStateException("Unrecognized enum " + eventRepresentationEnum);
        }
    }

    public static class MyLocalJsonProvidedLong implements Serializable {
        public long startts;
        public long endts;
    }

    public static class MyLocalJsonProvidedCalendar implements Serializable {
        public Calendar startts;
        public Calendar endts;
    }

    public static class MyLocalJsonProvidedDate implements Serializable {
        public Date startts;
        public Date endts;
    }

    public static class MyLocalJsonProvidedLocalDateTime implements Serializable {
        public LocalDateTime startts;
        public LocalDateTime endts;
    }

    public static class MyLocalJsonProvidedZonedDateTime implements Serializable {
        public ZonedDateTime startts;
        public ZonedDateTime endts;
    }
}
