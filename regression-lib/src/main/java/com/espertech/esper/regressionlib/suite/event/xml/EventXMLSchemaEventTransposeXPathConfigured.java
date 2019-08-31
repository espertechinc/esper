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
import com.espertech.esper.common.internal.event.xml.XPathNamespaceContext;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EventXMLSchemaEventTransposeXPathConfigured {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaEventTransposeXPathConfiguredPreconfig());
        execs.add(new EventXMLSchemaEventTransposeXPathConfiguredCreateSchema());
        execs.add(new EventXMLSchemaEventTransposeXPathConfiguredXPathExpression());
        return execs;
    }

    public static class EventXMLSchemaEventTransposeXPathConfiguredPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "MyXMLEventXPC", new RegressionPath());
        }
    }

    public static class EventXMLSchemaEventTransposeXPathConfiguredCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchema = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent', schemaResource='" + schemaUriSimpleSchema + "', autoFragment=false)" +
                "@XMLSchemaNamespacePrefix(prefix='ss', namespace='samples:schemas:simpleSchema')" +
                "@XMLSchemaField(name='nested1simple', xpath='/ss:simpleEvent/ss:nested1', type='NODE', eventTypeName='MyNestedEventXPC')" +
                "@XMLSchemaField(name='nested4array', xpath='//ss:nested4', type='nodeset', eventTypeName='MyNestedArrayEventXPC')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    public static class EventXMLSchemaEventTransposeXPathConfiguredXPathExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            try {
                XPathNamespaceContext ctx = new XPathNamespaceContext();
                ctx.addPrefix("n0", "samples:schemas:simpleSchema");

                Node node = SupportXML.getDocument().getDocumentElement();

                XPath pathOne = XPathFactory.newInstance().newXPath();
                pathOne.setNamespaceContext(ctx);
                XPathExpression pathExprOne = pathOne.compile("/n0:simpleEvent/n0:nested1");
                Node result = (Node) pathExprOne.evaluate(node, XPathConstants.NODE);
                //System.out.println("Result:\n" + SchemaUtil.serialize(result));

                XPath pathTwo = XPathFactory.newInstance().newXPath();
                pathTwo.setNamespaceContext(ctx);
                XPathExpression pathExprTwo = pathTwo.compile("/n0:simpleEvent/n0:nested1/n0:prop1");
                String resultTwo = (String) pathExprTwo.evaluate(result, XPathConstants.STRING);
                //System.out.println("Result 2: <" + resultTwo + ">");

                XPath pathThree = XPathFactory.newInstance().newXPath();
                pathThree.setNamespaceContext(ctx);
                XPathExpression pathExprThree = pathThree.compile("/n0:simpleEvent/n0:nested3");
                String resultThress = (String) pathExprThree.evaluate(result, XPathConstants.STRING);
                //System.out.println("Result 3: <" + resultThress + ">");
            } catch (Throwable t) {
                fail();
            }
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        env.compileDeploy("@name('insert') insert into Nested3Stream select nested1simple, nested4array from " + eventTypeName + "#lastevent", path);
        env.compileDeploy("@name('sw') select * from " + eventTypeName + "#lastevent", path);
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("insert").getEventType());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("sw").getEventType());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1simple", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("nested4array", Node[].class, Node.class, false, false, true, false, true),
        }, env.statement("insert").getEventType().getPropertyDescriptors());

        FragmentEventType fragmentTypeNested1 = env.statement("insert").getEventType().getFragmentType("nested1simple");
        assertFalse(fragmentTypeNested1.isIndexed());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("prop1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("prop2", Boolean.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("attr1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested2", Node.class, null, false, false, false, false, false),
        }, fragmentTypeNested1.getFragmentType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(fragmentTypeNested1.getFragmentType());

        FragmentEventType fragmentTypeNested4 = env.statement("insert").getEventType().getFragmentType("nested4array");
        assertTrue(fragmentTypeNested4.isIndexed());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("prop5", String[].class, null, false, false, true, false, false),
            new EventPropertyDescriptor("prop6", String[].class, null, false, false, true, false, false),
            new EventPropertyDescriptor("prop7", String[].class, null, false, false, true, false, false),
            new EventPropertyDescriptor("prop8", String[].class, null, false, false, true, false, false),
            new EventPropertyDescriptor("id", String.class, null, false, false, false, false, false),
        }, fragmentTypeNested4.getFragmentType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(fragmentTypeNested4.getFragmentType());

        FragmentEventType fragmentTypeNested4Item = env.statement("insert").getEventType().getFragmentType("nested4array[0]");
        assertFalse(fragmentTypeNested4Item.isIndexed());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("prop5", String[].class, null, false, false, true, false, false),
            new EventPropertyDescriptor("prop6", String[].class, null, false, false, true, false, false),
            new EventPropertyDescriptor("prop7", String[].class, null, false, false, true, false, false),
            new EventPropertyDescriptor("prop8", String[].class, null, false, false, true, false, false),
            new EventPropertyDescriptor("id", String.class, null, false, false, false, false, false),
        }, fragmentTypeNested4Item.getFragmentType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(fragmentTypeNested4Item.getFragmentType());

        SupportXML.sendDefaultEvent(env.eventService(), "ABC", eventTypeName);

        EventBean received = env.statement("insert").iterator().next();
        EPAssertionUtil.assertProps(received, "nested1simple.prop1,nested1simple.prop2,nested1simple.attr1,nested1simple.nested2.prop3[1]".split(","), new Object[]{"SAMPLE_V1", true, "SAMPLE_ATTR1", 4});
        EPAssertionUtil.assertProps(received, "nested4array[0].id,nested4array[0].prop5[1],nested4array[1].id".split(","), new Object[]{"a", "SAMPLE_V8", "b"});

        // assert event and fragments alone
        EventBean wildcardStmtEvent = env.statement("sw").iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(wildcardStmtEvent);

        FragmentEventType eventType = wildcardStmtEvent.getEventType().getFragmentType("nested1simple");
        assertFalse(eventType.isIndexed());
        assertFalse(eventType.isNative());
        assertEquals("MyNestedEventXPC", eventType.getFragmentType().getName());
        assertTrue(wildcardStmtEvent.get("nested1simple") instanceof Node);
        assertEquals("SAMPLE_V1", ((EventBean) wildcardStmtEvent.getFragment("nested1simple")).get("prop1"));

        eventType = wildcardStmtEvent.getEventType().getFragmentType("nested4array");
        assertTrue(eventType.isIndexed());
        assertFalse(eventType.isNative());
        assertEquals("MyNestedArrayEventXPC", eventType.getFragmentType().getName());
        EventBean[] eventsArray = (EventBean[]) wildcardStmtEvent.getFragment("nested4array");
        assertEquals(3, eventsArray.length);
        assertEquals("SAMPLE_V8", eventsArray[0].get("prop5[1]"));
        assertEquals("SAMPLE_V9", eventsArray[1].get("prop5[0]"));
        assertEquals(NodeList.class, wildcardStmtEvent.getEventType().getPropertyType("nested4array"));
        assertTrue(wildcardStmtEvent.get("nested4array") instanceof NodeList);

        EventBean nested4arrayItem = (EventBean) wildcardStmtEvent.getFragment("nested4array[1]");
        assertEquals("b", nested4arrayItem.get("id"));

        env.undeployAll();
    }
}
