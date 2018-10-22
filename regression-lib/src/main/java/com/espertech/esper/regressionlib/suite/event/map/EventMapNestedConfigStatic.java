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
package com.espertech.esper.regressionlib.suite.event.map;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertSame;

public class EventMapNestedConfigStatic implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        runAssertion(env, new RegressionPath());
    }

    protected static void runAssertion(RegressionEnvironment env, RegressionPath path) {
        String statementText = "@name('s0') select nested as a, " +
            "nested.n1 as b," +
            "nested.n2 as c," +
            "nested.n2.n1n1 as d " +
            "from NestedMapWithSimpleProps#length(5)";
        env.compileDeploy(statementText, path).addListener("s0");

        Map<String, Object> mapEvent = getTestData();
        env.sendEventMap(mapEvent, "NestedMapWithSimpleProps");

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertSame(mapEvent.get("nested"), theEvent.get("a"));
        assertSame("abc", theEvent.get("b"));
        assertSame(((Map) mapEvent.get("nested")).get("n2"), theEvent.get("c"));
        assertSame("def", theEvent.get("d"));

        env.undeployAll();
    }

    private static Map<String, Object> getTestData() {
        Map nestedNested = new HashMap<String, Object>();
        nestedNested.put("n1n1", "def");

        Map nested = new HashMap<String, Object>();
        nested.put("n1", "abc");
        nested.put("n2", nestedNested);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("nested", nested);

        return map;
    }

}
