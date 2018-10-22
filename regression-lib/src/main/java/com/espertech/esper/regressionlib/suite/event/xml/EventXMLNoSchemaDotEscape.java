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

public class EventXMLNoSchemaDotEscape implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String stmt = "@name('s0') select a\\.b.c\\.d as val from AEvent";
        env.compileDeploy(stmt).addListener("s0");

        sendXMLEvent(env, "<myroot><a.b><c.d>value</c.d></a.b></myroot>", "AEvent");
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("value", theEvent.get("val"));

        env.undeployAll();
    }
}
