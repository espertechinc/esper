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

import static org.junit.Assert.assertSame;

public class EventXMLSchemaPropertyDynamicDOMGetter {
    protected final static String SCHEMA_XML = "<simpleEvent xmlns=\"samples:schemas:simpleSchema\" \n" +
        "  xmlns:ss=\"samples:schemas:simpleSchema\" \n" +
        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
        "  xsi:schemaLocation=\"samples:schemas:simpleSchema simpleSchema.xsd\">" +
        "<type>abc</type>\n" +
        "<dyn>1</dyn>\n" +
        "<dyn>2</dyn>\n" +
        "<nested>\n" +
        "<nes2>3</nes2>\n" +
        "</nested>\n" +
        "<map id='a'>4</map>\n" +
        "</simpleEvent>";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaPropertyDynamicDOMGetterPreconfig());
        execs.add(new EventXMLSchemaPropertyDynamicDOMGetterCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaPropertyDynamicDOMGetterPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "MyEventWithPrefix", new RegressionPath());
        }
    }

    public static class EventXMLSchemaPropertyDynamicDOMGetterCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchema = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent', schemaResource='" + schemaUriSimpleSchema + "', xpathPropertyExpr=true, eventSenderValidatesRoot=false, defaultNamespace='samples:schemas:simpleSchema')" +
                "@XMLSchemaNamespacePrefix(prefix='ss', namespace='samples:schemas:simpleSchema')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        String stmtText = "@name('s0') select type?,dyn[1]?,nested.nes2?,map('a')? from " + eventTypeName;
        env.compileDeploy(stmtText, path).addListener("s0");

        env.assertStatement("s0", statement -> {
            SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("type?", Node.class),
                new SupportEventPropDesc("dyn[1]?", Node.class),
                new SupportEventPropDesc("nested.nes2?", Node.class),
                new SupportEventPropDesc("map('a')?", Node.class));
            SupportEventTypeAssertionUtil.assertConsistency(statement.getEventType());
        });

        Document root = SupportXML.getDocument(SCHEMA_XML);
        env.sendEventXMLDOM(root, eventTypeName);

        env.assertEventNew("s0", theEvent -> {
            assertSame(root.getDocumentElement().getChildNodes().item(0), theEvent.get("type?"));
            assertSame(root.getDocumentElement().getChildNodes().item(4), theEvent.get("dyn[1]?"));
            assertSame(root.getDocumentElement().getChildNodes().item(6).getChildNodes().item(1), theEvent.get("nested.nes2?"));
            assertSame(root.getDocumentElement().getChildNodes().item(8), theEvent.get("map('a')?"));
            SupportEventTypeAssertionUtil.assertConsistency(theEvent);
        });

        env.undeployAll();
    }
}
