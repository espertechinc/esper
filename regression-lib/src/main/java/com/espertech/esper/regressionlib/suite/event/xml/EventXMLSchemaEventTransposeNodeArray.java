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
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EventXMLSchemaEventTransposeNodeArray {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaEventTransposeNodeArrayPreconfig());
        execs.add(new EventXMLSchemaEventTransposeNodeArrayCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaEventTransposeNodeArrayPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "SimpleEventWSchema", new RegressionPath());
        }
    }

    public static class EventXMLSchemaEventTransposeNodeArrayCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchema = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent', schemaResource='" + schemaUriSimpleSchema + "')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        // try array property insert
        env.compileDeploy("@name('s0') select nested3.nested4 as narr from " + eventTypeName + "#lastevent", path);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("narr", Node[].class, Node.class, false, false, true, false, true),
        }, env.statement("s0").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").getEventType());

        SupportXML.sendDefaultEvent(env.eventService(), "test", eventTypeName);

        EventBean result = env.statement("s0").iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(result);
        EventBean[] fragments = (EventBean[]) result.getFragment("narr");
        assertEquals(3, fragments.length);
        assertEquals("SAMPLE_V8", fragments[0].get("prop5[1]"));
        assertEquals("SAMPLE_V11", fragments[2].get("prop5[1]"));

        EventBean fragmentItem = (EventBean) result.getFragment("narr[2]");
        assertEquals(eventTypeName + ".nested3.nested4", fragmentItem.getEventType().getName());
        assertEquals("SAMPLE_V10", fragmentItem.get("prop5[0]"));

        // try array index property insert
        env.compileDeploy("@name('ii') select nested3.nested4[1] as narr from " + eventTypeName + "#lastevent", path);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("narr", Node.class, null, false, false, false, false, true),
        }, env.statement("ii").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("ii").getEventType());

        SupportXML.sendDefaultEvent(env.eventService(), "test", eventTypeName);

        EventBean resultItem = env.iterator("ii").next();
        assertEquals("b", resultItem.get("narr.id"));
        SupportEventTypeAssertionUtil.assertConsistency(resultItem);
        EventBean fragmentsInsertItem = (EventBean) resultItem.getFragment("narr");
        SupportEventTypeAssertionUtil.assertConsistency(fragmentsInsertItem);
        assertEquals("b", fragmentsInsertItem.get("id"));
        assertEquals("SAMPLE_V9", fragmentsInsertItem.get("prop5[0]"));

        env.undeployAll();
    }
}
