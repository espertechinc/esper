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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventXMLNoSchemaSimpleXMLDOMGetter {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaSimpleXMLDOMGetterPreconfig());
        execs.add(new EventXMLNoSchemaSimpleXMLDOMGetterCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaSimpleXMLDOMGetterPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "TestXMLNoSchemaType", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaSimpleXMLDOMGetterCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='myevent', xpathPropertyExpr=false)" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        String stmt = "@name('s0') select element1, invalidelement, " +
            "element4.element41 as nestedElement," +
            "element2.element21('e21_2') as mappedElement," +
            "element2.element21[1] as indexedElement," +
            "element3.myattribute as invalidattribute " +
            "from " + eventTypeName + "#length(100)";
        env.compileDeploy(stmt, path).addListener("s0");

        // Generate document with the specified in element1 to confirm we have independent events
        EventXMLNoSchemaSimpleXMLXPathProperties.sendEvent(env, "EventA", eventTypeName);
        assertDataGetter(env, "EventA", false);

        EventXMLNoSchemaSimpleXMLXPathProperties.sendEvent(env, "EventB", eventTypeName);
        assertDataGetter(env, "EventB", false);

        env.undeployAll();
    }

    protected static void assertDataGetter(RegressionEnvironment env, String element1, boolean isInvalidReturnsEmptyString) {
        assertNotNull(env.listener("s0").getLastNewData());
        EventBean theEvent = env.listener("s0").getLastNewData()[0];

        assertEquals(element1, theEvent.get("element1"));
        assertEquals("VAL4-1", theEvent.get("nestedElement"));
        assertEquals("VAL21-2", theEvent.get("mappedElement"));
        assertEquals("VAL21-2", theEvent.get("indexedElement"));

        if (isInvalidReturnsEmptyString) {
            assertEquals("", theEvent.get("invalidelement"));
            assertEquals("", theEvent.get("invalidattribute"));
        } else {
            assertEquals(null, theEvent.get("invalidelement"));
            assertEquals(null, theEvent.get("invalidattribute"));
        }
    }
}

