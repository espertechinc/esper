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
import com.espertech.esper.common.internal.util.FileUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventXMLSchemaWithRestriction {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaWithRestrictionPreconfig());
        execs.add(new EventXMLSchemaWithRestrictionCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaWithRestrictionPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "OrderEvent", new RegressionPath());
        }
    }

    public static class EventXMLSchemaWithRestrictionCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            InputStream schemaStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("regression/simpleSchemaWithRestriction.xsd");
            assertNotNull(schemaStream);
            String schemaTextSimpleSchemaWithRestriction = FileUtil.linesToText(FileUtil.readFile(schemaStream));
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='order', schemaText='" + schemaTextSimpleSchemaWithRestriction + "')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        String text = "@name('s0') select order_amount from " + eventTypeName;
        env.compileDeploy(text, path).addListener("s0");

        SupportXML.sendXMLEvent(env,
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<order>\n" +
                "<order_amount>202.1</order_amount>" +
                "</order>", eventTypeName);
        EventBean theEvent = env.listener("s0").getLastNewData()[0];
        assertEquals(Double.class, theEvent.get("order_amount").getClass());
        assertEquals(202.1d, theEvent.get("order_amount"));
        env.listener("s0").reset();

        env.undeployAll();
    }
}
