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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertSame;

public class ExecEventMapNestedConfigRuntime implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("NestedMap", getTestDefinition());
        runAssertion(epService);
    }

    protected static void runAssertion(EPServiceProvider epService) {
        String statementText = "select nested as a, " +
                "nested.n1 as b," +
                "nested.n2 as c," +
                "nested.n2.n1n1 as d " +
                "from NestedMap#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> mapEvent = getTestData();
        epService.getEPRuntime().sendEvent(mapEvent, "NestedMap");

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(mapEvent.get("nested"), theEvent.get("a"));
        assertSame("abc", theEvent.get("b"));
        assertSame(((Map) mapEvent.get("nested")).get("n2"), theEvent.get("c"));
        assertSame("def", theEvent.get("d"));
        statement.stop();
    }

    protected static Map<String, Object> getTestDefinition() {
        Map<String, Object> propertiesNestedNested = new HashMap<String, Object>();
        propertiesNestedNested.put("n1n1", String.class);

        Map<String, Object> propertiesNested = new HashMap<String, Object>();
        propertiesNested.put("n1", String.class);
        propertiesNested.put("n2", propertiesNestedNested);

        Map<String, Object> root = new HashMap<String, Object>();
        root.put("nested", propertiesNested);

        return root;
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
