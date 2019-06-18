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
package com.espertech.esper.regressionlib.suite.event.json;

import com.espertech.esper.common.client.json.util.EventSenderJson;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

public class EventJsonEventSender {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonEventSenderParseAndSend());
        return execs;
    }

    private static class EventJsonEventSenderParseAndSend implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype @JsonSchema create json schema MyEvent(p1 string);\n" +
                    "@name('s0') select * from MyEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            EventSenderJson sender = (EventSenderJson) env.runtime().getEventService().getEventSender("MyEvent");
            JsonEventObject underlying = (JsonEventObject) sender.parse("{\"p1\": \"abc\"}");

            sender.sendEvent(underlying);
            env.listener("s0").assertInvokedAndReset();

            env.undeployAll();
        }
    }
}
