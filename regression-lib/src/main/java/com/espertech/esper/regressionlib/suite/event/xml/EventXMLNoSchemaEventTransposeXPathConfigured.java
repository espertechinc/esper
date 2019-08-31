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
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class EventXMLNoSchemaEventTransposeXPathConfigured {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaEventTransposeXPathConfiguredPreconfig());
        execs.add(new EventXMLNoSchemaEventTransposeXPathConfiguredCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaEventTransposeXPathConfiguredPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "MyXMLEvent", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaEventTransposeXPathConfiguredCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent')" +
                "@XMLSchemaNamespacePrefix(prefix='ss', namespace='samples:schemas:simpleSchema')" +
                "@XMLSchemaField(name='nested1simple', xpath='/ss:simpleEvent/ss:nested1', type='node', eventTypeName='MyNestedEvent')" +
                "@XMLSchemaField(name='nested4array', xpath='//ss:nested4', type='nodeset', eventTypeName='MyNestedArrayEvent')" +
                "create xml schema MyEventCreateSchema();\n";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        env.compileDeploy("@name('insert') insert into Nested3Stream select nested1simple, nested4array from " + eventTypeName, path);
        env.compileDeploy("@name('s0') select * from " + eventTypeName, path);
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("insert").getEventType());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").getEventType());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1simple", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("nested4array", Node[].class, Node.class, false, false, true, false, true),
        }, env.statement("insert").getEventType().getPropertyDescriptors());

        FragmentEventType fragmentTypeNested1 = env.statement("insert").getEventType().getFragmentType("nested1simple");
        assertFalse(fragmentTypeNested1.isIndexed());
        assertEquals(0, fragmentTypeNested1.getFragmentType().getPropertyDescriptors().length);
        SupportEventTypeAssertionUtil.assertConsistency(fragmentTypeNested1.getFragmentType());

        FragmentEventType fragmentTypeNested4 = env.statement("insert").getEventType().getFragmentType("nested4array");
        assertTrue(fragmentTypeNested4.isIndexed());
        assertEquals(0, fragmentTypeNested4.getFragmentType().getPropertyDescriptors().length);
        SupportEventTypeAssertionUtil.assertConsistency(fragmentTypeNested4.getFragmentType());

        SupportXML.sendDefaultEvent(env.eventService(), "ABC", eventTypeName);

        EventBean received = env.iterator("insert").next();
        EPAssertionUtil.assertProps(received, "nested1simple.prop1,nested1simple.prop2,nested1simple.attr1,nested1simple.nested2.prop3[1]".split(","), new Object[]{"SAMPLE_V1", "true", "SAMPLE_ATTR1", "4"});
        EPAssertionUtil.assertProps(received, "nested4array[0].id,nested4array[0].prop5[1],nested4array[1].id".split(","), new Object[]{"a", "SAMPLE_V8", "b"});

        // assert event and fragments alone
        EventBean wildcardStmtEvent = env.iterator("s0").next();
        SupportEventTypeAssertionUtil.assertConsistency(wildcardStmtEvent);

        FragmentEventType eventType = wildcardStmtEvent.getEventType().getFragmentType("nested1simple");
        assertFalse(eventType.isIndexed());
        assertFalse(eventType.isNative());
        assertEquals("MyNestedEvent", eventType.getFragmentType().getName());
        assertTrue(wildcardStmtEvent.get("nested1simple") instanceof Node);
        assertEquals("SAMPLE_V1", ((EventBean) wildcardStmtEvent.getFragment("nested1simple")).get("prop1"));

        eventType = wildcardStmtEvent.getEventType().getFragmentType("nested4array");
        assertTrue(eventType.isIndexed());
        assertFalse(eventType.isNative());
        assertEquals("MyNestedArrayEvent", eventType.getFragmentType().getName());
        EventBean[] eventsArray = (EventBean[]) wildcardStmtEvent.getFragment("nested4array");
        assertEquals(3, eventsArray.length);
        assertEquals("SAMPLE_V8", eventsArray[0].get("prop5[1]"));
        assertEquals("SAMPLE_V9", eventsArray[1].get("prop5[0]"));
        assertEquals(NodeList.class, wildcardStmtEvent.getEventType().getPropertyType("nested4array"));
        assertTrue(wildcardStmtEvent.get("nested4array") instanceof NodeList);

        env.undeployAll();
    }
}
