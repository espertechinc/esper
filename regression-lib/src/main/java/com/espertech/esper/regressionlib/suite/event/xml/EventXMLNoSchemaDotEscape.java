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

public class EventXMLNoSchemaDotEscape {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaDotEscapePreconfig());
        execs.add(new EventXMLNoSchemaDotEscapeCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaDotEscapePreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "AEvent", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaDotEscapeCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='myroot')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        String stmt = "@name('s0') select a\\.b.c\\.d as val from " + eventTypeName;
        env.compileDeploy(stmt, path).addListener("s0");

        sendXMLEvent(env, "<myroot><a.b><c.d>value</c.d></a.b></myroot>", eventTypeName);
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("value", theEvent.get("val"));

        env.undeployAll();
    }
}
