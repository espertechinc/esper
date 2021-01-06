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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EventXMLSchemaWithAll {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaWithAllPreconfig());
        execs.add(new EventXMLSchemaWithAllCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaWithAllPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "PageVisitEvent", new RegressionPath());
        }
    }

    public static class EventXMLSchemaWithAllCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchemaWithAll = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchemaWithAll.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='event-page-visit', schemaResource='" + schemaUriSimpleSchemaWithAll + "')" +
                "@XMLSchemaNamespacePrefix(prefix='ss', namespace='samples:schemas:simpleSchemaWithAll')" +
                "@XMLSchemaField(name='url', xpath='/ss:event-page-visit/ss:url', type='string')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        // url='page4'
        String text = "@name('s0') select a.url as sesja from pattern [ every a=" + eventTypeName + "(url='page1') ]";
        env.compileDeploy(text, path).addListener("s0");

        SupportXML.sendXMLEvent(env,
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<event-page-visit xmlns=\"samples:schemas:simpleSchemaWithAll\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchemaWithAll simpleSchemaWithAll.xsd\">\n" +
                "<url>page1</url>" +
                "</event-page-visit>", eventTypeName);
        EventBean theEvent = env.listener("s0").getLastNewData()[0];
        assertEquals("page1", theEvent.get("sesja"));
        env.listener("s0").reset();

        SupportXML.sendXMLEvent(env,
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<event-page-visit xmlns=\"samples:schemas:simpleSchemaWithAll\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchemaWithAll simpleSchemaWithAll.xsd\">\n" +
                "<url>page2</url>" +
                "</event-page-visit>", eventTypeName);
        env.assertListenerNotInvoked("s0");

        EventType type = env.compileDeploy("@name('s1') select * from " + eventTypeName, path).statement("s1").getEventType();
        SupportEventPropUtil.assertPropsEquals(type.getPropertyDescriptors(),
            new SupportEventPropDesc("sessionId", Node.class).fragment(),
            new SupportEventPropDesc("customerId", Node.class).fragment(),
            new SupportEventPropDesc("url", String.class),
            new SupportEventPropDesc("method", Node.class).fragment());

        env.undeployAll();
    }
}
