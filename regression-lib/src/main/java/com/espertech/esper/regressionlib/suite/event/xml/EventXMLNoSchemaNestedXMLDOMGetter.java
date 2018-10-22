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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import static com.espertech.esper.regressionlib.support.util.SupportXML.sendXMLEvent;
import static org.junit.Assert.assertEquals;

public class EventXMLNoSchemaNestedXMLDOMGetter implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        String stmt = "@name('s0') select b.c as type, element1, result1 from AEventWithXPath";
        env.compileDeploy(stmt).addListener("s0");

        sendXMLEvent(env, "<a><b><c></c></b></a>", "AEventWithXPath");
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(env, "<a><b></b></a>", "AEventWithXPath");
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(env, "<a><b><c>text</c></b></a>", "AEventWithXPath");
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("text", theEvent.get("type"));
        assertEquals("text", theEvent.get("element1"));

        env.undeployAll();
    }
}





