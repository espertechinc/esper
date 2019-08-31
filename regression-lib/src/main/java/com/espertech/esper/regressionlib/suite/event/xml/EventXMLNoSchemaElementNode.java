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
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.SupportXML.sendXMLEvent;
import static org.junit.Assert.assertEquals;

public class EventXMLNoSchemaElementNode {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaElementNodePreconfig());
        execs.add(new EventXMLNoSchemaElementNodeCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaElementNodePreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "MyEvent", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaElementNodeCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='batch-event')" +
                "@XMLSchemaField(name='event.type', xpath='//event/@type', type='string')" +
                "@XMLSchemaField(name='event.uid', xpath='//event/@uid', type='string')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        String stmt = "@name('s0') select event.type as type, event.uid as uid from " + eventTypeName;
        env.compileDeploy(stmt, path).addListener("s0");

        String xml = "<batch-event>" +
            "<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"/>" +
            "</batch-event>";
        sendXMLEvent(env, xml, eventTypeName);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("a-f-G", theEvent.get("type"));
        assertEquals("terminal.55", theEvent.get("uid"));

        env.undeployAll();
    }
}
