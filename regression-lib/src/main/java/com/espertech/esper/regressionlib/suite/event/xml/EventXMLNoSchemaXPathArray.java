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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.SupportXML.sendXMLEvent;

public class EventXMLNoSchemaXPathArray {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaXPathArrayPreconfig());
        execs.add(new EventXMLNoSchemaXPathArrayCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaXPathArrayPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "Event", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaXPathArrayCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='Event')" +
                "@XMLSchemaField(name='A', xpath='//Field[@Name=\"A\"]/@Value', type='nodeset', castToType='string[]')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        String xml = "<Event IsTriggering=\"True\">\n" +
            "<Field Name=\"A\" Value=\"987654321\"/>\n" +
            "<Field Name=\"B\" Value=\"2196958725202\"/>\n" +
            "<Field Name=\"C\" Value=\"1232363702\"/>\n" +
            "<Participants>\n" +
            "<Participant>\n" +
            "<Field Name=\"A\" Value=\"9876543210\"/>\n" +
            "<Field Name=\"B\" Value=\"966607340\"/>\n" +
            "<Field Name=\"D\" Value=\"353263010930650\"/>\n" +
            "</Participant>\n" +
            "</Participants>\n" +
            "</Event>";

        env.compileDeploy("@name('s0') select * from " + eventTypeName, path).addListener("s0");

        sendXMLEvent(env, xml, eventTypeName);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, "A".split(","), new Object[]{new Object[]{"987654321", "9876543210"}});

        env.undeployAll();
    }
}
