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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.regressionlib.support.json.SupportJsonEventTypeUtil.compareMaps;

public class EventJsonUnderlying {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonUnderlyingMapDynamicZeroDeclared());
        execs.add(new EventJsonUnderlyingMapDynamicOneDeclared());
        execs.add(new EventJsonUnderlyingMapDynamicTwoDeclared());
        execs.add(new EventJsonUnderlyingMapNonDynamicZeroDeclared());
        execs.add(new EventJsonUnderlyingMapNonDynamicOneDeclared());
        execs.add(new EventJsonUnderlyingMapNonDynamicTwoDeclared());
        return execs;
    }

    private static class EventJsonUnderlyingMapNonDynamicZeroDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            env.sendEventJson("{\"a\" : 1, \"b\": 2, \"c\": 3}\n", "JsonEvent");
            env.assertEventNew("s0", event -> compareMapWBean(new LinkedHashMap<>(), event));

            env.milestone(0);

            env.assertIterator("s0", iterator -> compareMapWBean(new LinkedHashMap<>(), iterator.next()));

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapNonDynamicOneDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@public @buseventtype create json schema JsonEvent(a int);\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            env.sendEventJson("{\"a\" : 1, \"b\": 2, \"c\": 3}\n", "JsonEvent");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            env.assertEventNew("s0", event -> compareMapWBean(expectedOne, event));

            env.sendEventJson("{\"a\" : 10}\n", "JsonEvent");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            env.assertEventNew("s0", event -> compareMapWBean(expectedTwo, event));

            env.sendEventJson("{}\n", "JsonEvent");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            env.assertEventNew("s0", event -> compareMapWBean(expectedThree, event));

            env.milestone(0);

            env.assertIterator("s0", it -> {
                compareMapWBean(expectedOne, it.next());
                compareMapWBean(expectedTwo, it.next());
                compareMapWBean(expectedThree, it.next());
            });

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapNonDynamicTwoDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@public @buseventtype create json schema JsonEvent(a int, b int);\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            env.sendEventJson("{\"a\" : 1, \"b\": 2, \"c\": 3}\n", "JsonEvent");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            expectedOne.put("b", 2);
            env.assertEventNew("s0", event -> compareMapWBean(expectedOne, event));

            env.sendEventJson("{\"a\" : 10}\n", "JsonEvent");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            expectedTwo.put("b", null);
            env.assertEventNew("s0", event -> compareMapWBean(expectedTwo, event));

            env.sendEventJson("{}\n", "JsonEvent");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            expectedThree.put("b", null);
            env.assertEventNew("s0", event -> compareMapWBean(expectedThree, event));

            env.milestone(0);

            env.assertIterator("s0", it -> {
                compareMapWBean(expectedOne, it.next());
                compareMapWBean(expectedTwo, it.next());
                compareMapWBean(expectedThree, it.next());
            });

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapDynamicZeroDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            env.sendEventJson("{\"a\" : 1, \"b\": 2, \"c\": 3}\n", "JsonEvent");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            expectedOne.put("b", 2);
            expectedOne.put("c", 3);
            env.assertEventNew("s0", event -> compareMapWBean(expectedOne, event));

            env.sendEventJson("{\"a\" : 10}\n", "JsonEvent");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            env.assertEventNew("s0", event -> compareMapWBean(expectedTwo, event));

            env.sendEventJson("{\"a\" : null, \"c\": 101, \"d\": 102}\n", "JsonEvent");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            expectedThree.put("c", 101);
            expectedThree.put("d", 102);
            env.assertEventNew("s0", event -> compareMapWBean(expectedThree, event));

            env.sendEventJson("{}\n", "JsonEvent");
            env.assertEventNew("s0", event -> compareMapWBean(new LinkedHashMap<>(), event));

            env.milestone(0);

            env.assertIterator("s0", it -> {
                compareMapWBean(expectedOne, it.next());
                compareMapWBean(expectedTwo, it.next());
                compareMapWBean(expectedThree, it.next());
                compareMapWBean(new LinkedHashMap<>(), it.next());
            });

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapDynamicOneDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent(a int);\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            env.sendEventJson("{\"a\" : 1, \"b\": 2, \"c\": 3}\n", "JsonEvent");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            expectedOne.put("b", 2);
            expectedOne.put("c", 3);
            env.assertEventNew("s0", event -> compareMapWBean(expectedOne, event));

            env.sendEventJson("{\"a\" : 10}\n", "JsonEvent");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            env.assertEventNew("s0", event -> compareMapWBean(expectedTwo, event));

            env.sendEventJson("{\"a\" : null, \"c\": 101, \"d\": 102}\n", "JsonEvent");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            expectedThree.put("c", 101);
            expectedThree.put("d", 102);
            env.assertEventNew("s0", event -> compareMapWBean(expectedThree, event));

            env.sendEventJson("{}\n", "JsonEvent");
            Map<String, Object> expectedFour = new LinkedHashMap<>();
            expectedFour.put("a", null);
            env.assertEventNew("s0", event -> compareMapWBean(expectedFour, event));

            env.milestone(0);

            env.assertIterator("s0", it -> {
                compareMapWBean(expectedOne, it.next());
                compareMapWBean(expectedTwo, it.next());
                compareMapWBean(expectedThree, it.next());
                compareMapWBean(expectedFour, it.next());
            });

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapDynamicTwoDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent(a int, b int);\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            env.sendEventJson("{\"a\" : 1, \"b\": 2, \"c\": 3}\n", "JsonEvent");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            expectedOne.put("b", 2);
            expectedOne.put("c", 3);
            env.assertEventNew("s0", event -> compareMapWBean(expectedOne, event));

            env.sendEventJson("{\"a\" : 10}\n", "JsonEvent");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            expectedTwo.put("b", null);
            env.assertEventNew("s0", event -> compareMapWBean(expectedTwo, event));

            env.sendEventJson("{\"a\" : null, \"c\": 101, \"d\": 102}\n", "JsonEvent");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            expectedThree.put("b", null);
            expectedThree.put("c", 101);
            expectedThree.put("d", 102);
            env.assertEventNew("s0", event -> compareMapWBean(expectedThree, event));

            env.sendEventJson("{}\n", "JsonEvent");
            Map<String, Object> expectedFour = new LinkedHashMap<>();
            expectedFour.put("a", null);
            expectedFour.put("b", null);
            env.assertEventNew("s0", event -> compareMapWBean(expectedFour, event));

            env.milestone(0);

            env.assertIterator("s0", it -> {
                compareMapWBean(expectedOne, it.next());
                compareMapWBean(expectedTwo, it.next());
                compareMapWBean(expectedThree, it.next());
                compareMapWBean(expectedFour, it.next());
            });

            env.undeployAll();
        }
    }

    private static void compareMapWBean(Map<String, Object> expected, EventBean event) {
        compareMaps(expected, (Map<String, Object>) event.getUnderlying());
    }
}
