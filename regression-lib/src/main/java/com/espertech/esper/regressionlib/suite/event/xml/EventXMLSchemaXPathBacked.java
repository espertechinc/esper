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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EventXMLSchemaXPathBacked {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaXPathBackedPreconfig());
        execs.add(new EventXMLSchemaXPathBackedCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaXPathBackedPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, true, "XMLSchemaConfigOne", new RegressionPath());
        }
    }

    public static class EventXMLSchemaXPathBackedCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchema = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent', schemaResource='" + schemaUriSimpleSchema + "', xpathPropertyExpr=true)" +
                "@XMLSchemaNamespacePrefix(prefix='ss', namespace='samples:schemas:simpleSchema')" +
                "@XMLSchemaField(name='customProp', xpath='count(/ss:simpleEvent/ss:nested3/ss:nested4)', type='number')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, true, "MyEventCreateSchema", path);
        }
    }

    protected static void runAssertion(RegressionEnvironment env, boolean xpath, String typeName, RegressionPath path) {

        String stmtSelectWild = "@name('s0') select * from " + typeName;
        env.compileDeploy(stmtSelectWild, path).addListener("s0");
        env.assertStatement("s0", statement -> {
            EventType type = statement.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(type);

            SupportEventPropUtil.assertPropsEquals(type.getPropertyDescriptors(),
                new SupportEventPropDesc("nested1", Node.class).fragment(!xpath),
                new SupportEventPropDesc("prop4", String.class),
                new SupportEventPropDesc("nested3", Node.class).fragment(!xpath),
                new SupportEventPropDesc("customProp", Double.class));
        });
        env.undeployModuleContaining("s0");

        String stmt = "@name('s0') select nested1 as nodeProp," +
            "prop4 as nested1Prop," +
            "nested1.prop2 as nested2Prop," +
            "nested3.nested4('a').prop5[1] as complexProp," +
            "nested1.nested2.prop3[2] as indexedProp," +
            "customProp," +
            "prop4.attr2 as attrOneProp," +
            "nested3.nested4[2].id as attrTwoProp" +
            " from " + typeName + "#length(100)";

        env.compileDeploy(stmt, path).addListener("s0");
        env.assertStatement("s0", statement -> {
            EventType type = statement.getEventType();
            SupportEventTypeAssertionUtil.assertConsistency(type);
            SupportEventPropUtil.assertPropsEquals(type.getPropertyDescriptors(),
                new SupportEventPropDesc("nodeProp", Node.class).fragment(!xpath),
                new SupportEventPropDesc("nested1Prop", String.class),
                new SupportEventPropDesc("nested2Prop", Boolean.class),
                new SupportEventPropDesc("complexProp", String.class),
                new SupportEventPropDesc("indexedProp", Integer.class),
                new SupportEventPropDesc("customProp", Double.class),
                new SupportEventPropDesc("attrOneProp", Boolean.class),
                new SupportEventPropDesc("attrTwoProp", String.class));
        });

        Document doc = SupportXML.makeDefaultEvent("test");
        env.sendEventXMLDOM(doc, typeName);

        env.assertListener("s0", listener -> {
            assertNotNull(listener.getLastNewData());
            EventBean theEvent = listener.getLastNewData()[0];

            assertSame(doc.getDocumentElement().getChildNodes().item(1), theEvent.get("nodeProp"));
            assertEquals("SAMPLE_V6", theEvent.get("nested1Prop"));
            assertEquals(true, theEvent.get("nested2Prop"));
            assertEquals("SAMPLE_V8", theEvent.get("complexProp"));
            assertEquals(5, theEvent.get("indexedProp"));
            assertEquals(3.0, theEvent.get("customProp"));
            assertEquals(true, theEvent.get("attrOneProp"));
            assertEquals("c", theEvent.get("attrTwoProp"));
        });

        /**
         * Comment-in for performance testing
         long start = System.nanoTime();
         for (int i = 0; i < 1000; i++)
         {
         sendEvent("test");
         }
         long end = System.nanoTime();
         double delta = (end - start) / 1000d / 1000d / 1000d;
         System.out.println(delta);
         */

        env.undeployAll();
    }
}
