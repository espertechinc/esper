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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Most getter tests can be found in Event+Infra.
 */
public class EventJsonGetter {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonGetterMapType());
        return execs;
    }

    private static class EventJsonGetterMapType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@public @buseventtype create json schema JsonEvent(prop java.util.Map);\n" +
                "@name('s0') select * from JsonEvent").addListener("s0");

            env.sendEventJson(new JsonObject().add("prop", new JsonObject().add("x", "y")).toString(), "JsonEvent");
            EventBean event = env.listener("s0").assertOneGetNewAndReset();

            EventPropertyGetter getterMapped = event.getEventType().getGetter("prop('x')");
            assertEquals("y", getterMapped.get(event));

            assertNull(event.getEventType().getGetter("prop.somefield?"));

            env.undeployAll();
        }
    }
}
