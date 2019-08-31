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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertSame;

public class EventXMLNoSchemaPropertyDynamicDOMGetter {

    protected final static String NOSCHEMA_XML = "<simpleEvent>\n" +
        "\t<type>abc</type>\n" +
        "\t<dyn>1</dyn>\n" +
        "\t<dyn>2</dyn>\n" +
        "\t<nested>\n" +
        "\t\t<nes2>3</nes2>\n" +
        "\t</nested>\n" +
        "\t<map id='a'>4</map>\n" +
        "</simpleEvent>";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaPropertyDynamicDOMGetterPreconfig());
        execs.add(new EventXMLNoSchemaPropertyDynamicDOMGetterCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaPropertyDynamicDOMGetterPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "MyEventSimpleEvent", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaPropertyDynamicDOMGetterCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent')" +
                " create xml schema MyEventCreateSchema as ()";
            RegressionPath path = new RegressionPath();
            env.eplToModelCompileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        String stmtText = "@name('s0') select type?,dyn[1]?,nested.nes2?,map('a')? from " + eventTypeName;
        env.compileDeploy(stmtText, path).addListener("s0");

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("type?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("dyn[1]?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested.nes2?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("map('a')?", Node.class, null, false, false, false, false, false),
        }, env.statement("s0").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").getEventType());

        Document root = SupportXML.sendXMLEvent(env, NOSCHEMA_XML, eventTypeName);
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertSame(root.getDocumentElement().getChildNodes().item(1), theEvent.get("type?"));
        assertSame(root.getDocumentElement().getChildNodes().item(5), theEvent.get("dyn[1]?"));
        assertSame(root.getDocumentElement().getChildNodes().item(7).getChildNodes().item(1), theEvent.get("nested.nes2?"));
        assertSame(root.getDocumentElement().getChildNodes().item(9), theEvent.get("map('a')?"));
        SupportEventTypeAssertionUtil.assertConsistency(theEvent);

        env.undeployAll();
    }
}
