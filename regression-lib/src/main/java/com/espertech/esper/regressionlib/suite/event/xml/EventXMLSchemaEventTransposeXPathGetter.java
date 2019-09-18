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

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;

public class EventXMLSchemaEventTransposeXPathGetter {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaEventTransposeXPathGetterPreconfig());
        execs.add(new EventXMLSchemaEventTransposeXPathGetterCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaEventTransposeXPathGetterPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "TestXMLSchemaTypeWithSS", new RegressionPath());
        }
    }

    public static class EventXMLSchemaEventTransposeXPathGetterCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchema = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent', schemaResource='" + schemaUriSimpleSchema + "', xpathPropertyExpr=true)" +
                "@XMLSchemaNamespacePrefix(prefix='ss', namespace='samples:schemas:simpleSchema')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        // Note that XPath Node results when transposed must be queried by XPath that is also absolute.
        // For example: "nested1" => "/n0:simpleEvent/n0:nested1" results in a Node.
        // That result Node's "prop1" =>  "/n0:simpleEvent/n0:nested1/n0:prop1" and "/n0:nested1/n0:prop1" does NOT result in a value.
        // Therefore property transposal is disabled for Property-XPath expressions.

        // note class not a fragment
        env.compileDeploy("@name('s0') insert into MyNestedStream select nested1 from " + eventTypeName + "#lastevent", path);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, false),
        }, env.statement("s0").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").getEventType());

        EventType type = env.runtime().getEventTypeService().getEventTypePreconfigured(eventTypeName);
        SupportEventTypeAssertionUtil.assertConsistency(type);
        assertNull(type.getFragmentType("nested1"));
        assertNull(type.getFragmentType("nested1.nested2"));

        SupportXML.sendDefaultEvent(env.eventService(), "ABC", eventTypeName);
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").iterator().next());

        env.undeployAll();
    }
}
