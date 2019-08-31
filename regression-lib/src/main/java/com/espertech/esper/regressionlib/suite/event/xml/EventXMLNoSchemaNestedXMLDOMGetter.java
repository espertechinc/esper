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

public class EventXMLNoSchemaNestedXMLDOMGetter {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaNestedXMLDOMGetterPreconfig());
        execs.add(new EventXMLNoSchemaNestedXMLDOMGetterCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaNestedXMLDOMGetterPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "AEventWithXPath", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaNestedXMLDOMGetterCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='a')" +
                "@XMLSchemaField(name='element1', xpath='/a/b/c', type='string')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        String stmt = "@name('s0') select b.c as type, element1, result1 from " + eventTypeName;
        env.compileDeploy(stmt, path).addListener("s0");

        sendXMLEvent(env, "<a><b><c></c></b></a>", eventTypeName);
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(env, "<a><b></b></a>", eventTypeName);
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(env, "<a><b><c>text</c></b></a>", eventTypeName);
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("text", theEvent.get("type"));
        assertEquals("text", theEvent.get("element1"));

        env.undeployAll();
    }
}





