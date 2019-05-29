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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.EventTypeException;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBean_G;
import com.espertech.esper.regressionlib.support.bean.SupportMarkerImplA;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;

public class EventInfraEventSender implements RegressionExecution {

    public final static String XML_TYPENAME = "EventInfraEventSenderXML";
    public final static String MAP_TYPENAME = "EventInfraEventSenderMap";
    public final static String OA_TYPENAME = "EventInfraEventSenderOA";
    public final static String AVRO_TYPENAME = "EventInfraEventSenderAvro";
    public final static String JSON_TYPENAME = "EventInfraEventSenderJson";

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        // Bean
        runAssertionSendEvent(env, path, "SupportBean", new SupportBean());
        runAssertionInvalid(env, "SupportBean", new SupportBean_G("G1"),
            "Event object of type " + SupportBean_G.class.getName() + " does not equal, extend or implement the type " + SupportBean.class.getName() + " of event type 'SupportBean'");
        runAssertionSendEvent(env, path, "SupportMarkerInterface", new SupportMarkerImplA("Q2"), new SupportBean_G("Q3"));
        runAssertionRouteEvent(env, path, "SupportBean", new SupportBean());

        // Map
        runAssertionSendEvent(env, path, MAP_TYPENAME, new HashMap());
        runAssertionRouteEvent(env, path, MAP_TYPENAME, new HashMap());
        runAssertionInvalid(env, MAP_TYPENAME, new SupportBean(),
            "Unexpected event object of type " + SupportBean.class.getName() + ", expected java.util.Map");

        // Object-Array
        runAssertionSendEvent(env, path, OA_TYPENAME, new Object[]{});
        runAssertionRouteEvent(env, path, OA_TYPENAME, new Object[]{});
        runAssertionInvalid(env, OA_TYPENAME, new SupportBean(),
            "Unexpected event object of type " + SupportBean.class.getName() + ", expected Object[]");

        // XML
        runAssertionSendEvent(env, path, XML_TYPENAME, SupportXML.getDocument("<myevent/>").getDocumentElement());
        runAssertionRouteEvent(env, path, XML_TYPENAME, SupportXML.getDocument("<myevent/>").getDocumentElement());
        runAssertionInvalid(env, XML_TYPENAME, new SupportBean(),
            "Unexpected event object type '" + SupportBean.class.getName() + "' encountered, please supply a org.w3c.dom.Document or Element node");
        runAssertionInvalid(env, XML_TYPENAME, SupportXML.getDocument("<xxxx/>"),
            "Unexpected root element name 'xxxx' encountered, expected a root element name of 'myevent'");

        // Avro
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        runAssertionSendEvent(env, path, AVRO_TYPENAME, new GenericData.Record(schema));
        runAssertionRouteEvent(env, path, AVRO_TYPENAME, new GenericData.Record(schema));
        runAssertionInvalid(env, AVRO_TYPENAME, new SupportBean(),
            "Unexpected event object type '" + SupportBean.class.getName() + "' encountered, please supply a GenericData.Record");

        // Json
        String schemas = "@public @buseventtype @name('schema') create json schema " + JSON_TYPENAME + "()";
        env.compileDeploy(schemas, path);
        runAssertionSendEvent(env, path, JSON_TYPENAME, "{}");
        runAssertionRouteEvent(env, path, JSON_TYPENAME, "{}");
        runAssertionInvalid(env, JSON_TYPENAME, new SupportBean(),
            "Unexpected event object of type '" + SupportBean.class.getName() + "', expected a Json-formatted string-type value");

        // No such type
        try {
            env.eventService().getEventSender("ABC");
            fail();
        } catch (EventTypeException ex) {
            assertEquals("Event type named 'ABC' could not be found", ex.getMessage());
        }

        // Internal implicit wrapper type
        env.compileDeploy("insert into ABC select *, theString as value from SupportBean");
        try {
            env.eventService().getEventSender("ABC");
            fail("Event type named 'ABC' could not be found");
        } catch (EventTypeException ex) {
            assertEquals("Event type named 'ABC' could not be found", ex.getMessage());
        }

        env.undeployAll();
    }

    private void runAssertionRouteEvent(RegressionEnvironment env,
                                        RegressionPath path,
                                        String typename,
                                        Object underlying) {

        String stmtText = "@name('s0') select * from " + typename;
        env.compileDeploy(stmtText, path).addListener("s0");

        EventSender sender = env.eventService().getEventSender(typename);
        env.compileDeploy("@public @buseventtype create schema TriggerEvent();\n" +
            "@name('trigger') select * from TriggerEvent;\n");
        env.statement("trigger").addListener((newData, oldData, stmt, runtime) -> {
            sender.routeEvent(underlying);
        });

        env.sendEventMap(Collections.emptyMap(), "TriggerEvent");
        assertUnderlying(env, typename, underlying);

        env.undeployModuleContaining("s0").undeployModuleContaining("trigger");
    }

    private void runAssertionSendEvent(RegressionEnvironment env,
                                       RegressionPath path,
                                       String typename,
                                       Object... correctUnderlyings) {

        String stmtText = "@name('s0') select * from " + typename;
        env.compileDeploy(stmtText, path).addListener("s0");

        EventSender sender = env.eventService().getEventSender(typename);
        for (Object underlying : correctUnderlyings) {
            sender.sendEvent(underlying);
            assertUnderlying(env, typename, underlying);
        }

        env.undeployModuleContaining("s0");
    }

    private void assertUnderlying(RegressionEnvironment env, String typename, Object underlying) {
        if (typename.equals(JSON_TYPENAME)) {
            assertNotNull(env.listener("s0").assertOneGetNewAndReset().getUnderlying());
        } else {
            assertSame(underlying, env.listener("s0").assertOneGetNewAndReset().getUnderlying());
        }
    }

    private void runAssertionInvalid(RegressionEnvironment env,
                                     String typename,
                                     Object incorrectUnderlying,
                                     String message) {

        EventSender sender = env.eventService().getEventSender(typename);

        try {
            sender.sendEvent(incorrectUnderlying);
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, message);
        }

        try {
            sender.routeEvent(incorrectUnderlying);
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, message);
        }
    }
}
