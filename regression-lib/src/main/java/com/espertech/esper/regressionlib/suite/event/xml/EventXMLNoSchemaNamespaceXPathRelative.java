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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.SupportXML.sendXMLEvent;
import static org.junit.Assert.assertEquals;

public class EventXMLNoSchemaNamespaceXPathRelative {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaNamespaceXPathRelativePreconfig());
        execs.add(new EventXMLNoSchemaNamespaceXPathRelativeCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaNamespaceXPathRelativePreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "StockQuoteSimpleConfig", new RegressionPath());
        }
    }

    ConfigurationCommonEventTypeXMLDOM stockQuoteSimpleConfig = new ConfigurationCommonEventTypeXMLDOM();

    public static class EventXMLNoSchemaNamespaceXPathRelativeCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='getQuote', defaultNamespace='http://services.samples/xsd', rootElementNamespace='http://services.samples/xsd', xpathResolvePropertiesAbsolute=false," +
                "  xpathPropertyExpr=true)" +
                "@XMLSchemaNamespacePrefix(prefix='m0', namespace='http://services.samples/xsd')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        String stmt = "@name('s0') select request.symbol as symbol_a, symbol as symbol_b from " + eventTypeName;
        env.compileDeploy(stmt, path).addListener("s0");

        String xml = "<m0:getQuote xmlns:m0=\"http://services.samples/xsd\"><m0:request><m0:symbol>IBM</m0:symbol></m0:request></m0:getQuote>";
        sendXMLEvent(env, xml, eventTypeName);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("IBM", theEvent.get("symbol_a"));
        assertEquals("IBM", theEvent.get("symbol_b"));

        env.undeployAll();
    }
}
