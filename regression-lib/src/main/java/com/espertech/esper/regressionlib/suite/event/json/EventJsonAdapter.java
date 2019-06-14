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
package com.espertech.esper.regressionlib.suite.event.json;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.json.minimaljson.JsonWriter;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.client.json.util.JsonFieldAdapterString;
import com.espertech.esper.common.client.render.JSONEventRenderer;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.json.SupportJsonFieldAdapterStringDate;
import com.espertech.esper.regressionlib.support.json.SupportJsonFieldAdapterStringPoint;

import java.awt.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EventJsonAdapter {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonAdapterInsertInto());
        execs.add(new EventJsonAdapterCreateSchemaWStringTransform());
        execs.add(new EventJsonAdapterInvalid());
        execs.add(new EventJsonAdapterDocSample());
        return execs;
    }

    private static class EventJsonAdapterDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
            Date date;
            try {
                date = sdf.parse("22-09-2018");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            String epl = "@public @buseventtype @JsonSchemaField(name=myDate, adapter='" + MyDateJSONParser.class.getName() + "')\n" +
                "create json schema JsonEvent(myDate Date);\n" +
                "@name('s0') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventJson("{\"myDate\" : \"22-09-2018\"}", "JsonEvent");
            assertEquals(date, env.listener("s0").assertOneGetNew().get("myDate"));

            JSONEventRenderer renderer = env.runtime().getRenderEventService().getJSONRenderer(env.runtime().getEventTypeService().getBusEventType("JsonEvent"));
            assertEquals("{\"hello\":{\"myDate\":\"22-09-2018\"}}", renderer.render("hello", env.listener("s0").assertOneGetNewAndReset()));

            env.undeployAll();
        }
    }

    private static class EventJsonAdapterInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "@JsonSchemaField(name=mydate, adapter=x) create json schema JsonEvent(mydate Date)",
                "Failed to resolve Json schema field adapter class: Could not load class by name 'x', please check imports");

            SupportMessageAssertUtil.tryInvalidCompile(env, "@JsonSchemaField(name=mydate, adapter='java.lang.String') create json schema JsonEvent(mydate Date)",
                "Json schema field adapter class does not implement interface 'JsonFieldAdapterString");

            SupportMessageAssertUtil.tryInvalidCompile(env, "@JsonSchemaField(name=mydate, adapter='" + InvalidAdapterJSONDate.class.getName() + "') create json schema JsonEvent(mydate Date)",
                "Json schema field adapter class 'InvalidAdapterJSONDate' does not have a default constructor");

            SupportMessageAssertUtil.tryInvalidCompile(env, "@JsonSchemaField(name=mydate, adapter='" + SupportJsonFieldAdapterStringDate.class.getSimpleName() + "') create json schema JsonEvent(mydate String)",
                "Json schema field adapter class 'SupportJsonFieldAdapterStringDate' mismatches the return type of the parse method, expected 'String' but found 'Date'");
        }
    }

    private static class EventJsonAdapterInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n" +
                    "@JsonSchemaField(name=mydate, adapter=" + SupportJsonFieldAdapterStringDate.class.getSimpleName() + ") " +
                    "@JsonSchemaField(name=point, adapter=" + SupportJsonFieldAdapterStringPoint.class.getSimpleName() + ") " +
                    EventRepresentationChoice.JSON.getAnnotationText() + " insert into JsonEvent select point, mydate from LocalEvent;\n" +
                    "@name('s0') select point, mydate from JsonEvent;\n" +
                    "@name('s1') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0").addListener("s1");

            env.sendEventBean(new LocalEvent(new Point(7, 14), DateTime.parseDefaultDate("2002-05-01T08:00:01.999")));

            String jsonFilled = "{\"point\":\"7,14\",\"mydate\":\"2002-05-01T08:00:01.999\"}";
            doAssert(env, jsonFilled, new Object[]{new Point(7, 14), DateTime.parseDefaultDate("2002-05-1T08:00:01.999")});

            env.undeployAll();
        }
    }

    private static class EventJsonAdapterCreateSchemaWStringTransform implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@JsonSchemaField(name=point, adapter=" + SupportJsonFieldAdapterStringPoint.class.getSimpleName() + ") " +
                "@JsonSchemaField(name=mydate, adapter=" + SupportJsonFieldAdapterStringDate.class.getSimpleName() + ") " +
                "create json schema JsonEvent(point java.awt.Point, mydate Date);\n" +
                "@name('s0') select point, mydate from JsonEvent;\n" +
                "@name('s1') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0").addListener("s1");

            String jsonFilled = "{\"point\":\"7,14\",\"mydate\":\"2002-05-01T08:00:01.999\"}";
            sendAssert(env, jsonFilled, new Object[]{new Point(7, 14), DateTime.parseDefaultDate("2002-05-1T08:00:01.999")});

            String jsonNulled = "{\"point\":null,\"mydate\":null}";
            sendAssert(env, jsonNulled, new Object[]{null, null});

            env.undeployAll();
        }
    }

    private static void sendAssert(RegressionEnvironment env, String json, Object[] expected) {
        env.sendEventJson(json, "JsonEvent");
        doAssert(env, json, expected);
    }

    private static void doAssert(RegressionEnvironment env, String json, Object[] expected) {
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "point,mydate".split(","), expected);

        JsonEventObject event = (JsonEventObject) env.listener("s1").assertOneGetNewAndReset().getUnderlying();
        assertEquals(json, event.toString());
    }

    public static class LocalEvent {
        private final Point point;
        private final Date mydate;

        public LocalEvent(Point point, Date mydate) {
            this.point = point;
            this.mydate = mydate;
        }

        public Point getPoint() {
            return point;
        }

        public Date getMydate() {
            return mydate;
        }
    }

    public static class InvalidAdapterJSONDate implements JsonFieldAdapterString<Date> {
        public InvalidAdapterJSONDate(int a) {
        }

        public Date parse(String value) {
            throw new UnsupportedOperationException();
        }

        public void write(Date value, JsonWriter writer) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    public static class MyDateJSONParser implements JsonFieldAdapterString<Date> {
        public Date parse(String value) {
            try {
                return value == null ? null : new SimpleDateFormat("dd-MM-yyyy").parse(value);
            } catch (ParseException e) {
                throw new EPException("Failed to parse: " + e.getMessage(), e);
            }
        }

        public void write(Date value, JsonWriter writer) throws IOException {
            if (value == null) {
                writer.writeLiteral("null");
                return;
            }
            writer.writeString(new SimpleDateFormat("dd-MM-yyyy").format(value));
        }
    }
}
