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
package com.espertech.esper.regressionlib.suite.event.xml;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.SupportXML.sendXMLEvent;
import static org.junit.Assert.assertEquals;

public class EventXMLNoSchemaEventXML {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaEventXMLPreconfig());
        execs.add(new EventXMLNoSchemaEventXMLCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaEventXMLPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select event.type as type, event.uid as uid from MyEventWTypeAndUID";
            runAssertion(env, epl, "MyEventWTypeAndUID");
        }
    }

    public static class EventXMLNoSchemaEventXMLCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='event')" +
                "@XMLSchemaField(name='event.type', xpath='/event/@type', type='string')" +
                "@XMLSchemaField(name='event.uid', xpath='/event/@uid', type='string')" +
                "create xml schema MyEventCreateSchema();\n" +
                "@name('s0') select event.type as type, event.uid as uid from MyEventCreateSchema;\n";
            runAssertion(env, epl, "MyEventCreateSchema");
        }
    }

    private static void runAssertion(RegressionEnvironment env, String epl, String eventTypeName) {
        env.compileDeploy(epl).addListener("s0");

        sendXMLEvent(env, "<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"></event>", eventTypeName);
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("a-f-G", theEvent.get("type"));
        assertEquals("terminal.55", theEvent.get("uid"));

        env.undeployAll();
    }
}
