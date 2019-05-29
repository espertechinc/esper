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
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.*;

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

            Map<String, Object> actualOne = sendJsonGetUnderlying(env, "{\"a\" : 1, \"b\": 2, \"c\": 3}\n");
            compareMaps(new LinkedHashMap<>(), actualOne);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            compareMapWBean(new LinkedHashMap<>(), it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapNonDynamicOneDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@public @buseventtype create json schema JsonEvent(a int);\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            Map<String, Object> actualOne = sendJsonGetUnderlying(env, "{\"a\" : 1, \"b\": 2, \"c\": 3}\n");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            compareMaps(expectedOne, actualOne);

            Map<String, Object> actualTwo = sendJsonGetUnderlying(env, "{\"a\" : 10}\n");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            compareMaps(expectedTwo, actualTwo);

            Map<String, Object> actualThree = sendJsonGetUnderlying(env, "{}\n");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            compareMaps(expectedThree, actualThree);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            compareMapWBean(expectedOne, it.next());
            compareMapWBean(expectedTwo, it.next());
            compareMapWBean(expectedThree, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapNonDynamicTwoDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@public @buseventtype create json schema JsonEvent(a int, b int);\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            Map<String, Object> actualOne = sendJsonGetUnderlying(env, "{\"a\" : 1, \"b\": 2, \"c\": 3}\n");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            expectedOne.put("b", 2);
            compareMaps(expectedOne, actualOne);

            Map<String, Object> actualTwo = sendJsonGetUnderlying(env, "{\"a\" : 10}\n");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            expectedTwo.put("b", null);
            compareMaps(expectedTwo, actualTwo);

            Map<String, Object> actualThree = sendJsonGetUnderlying(env, "{}\n");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            expectedThree.put("b", null);
            compareMaps(expectedThree, actualThree);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            compareMapWBean(expectedOne, it.next());
            compareMapWBean(expectedTwo, it.next());
            compareMapWBean(expectedThree, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapDynamicZeroDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            Map<String, Object> actualOne = sendJsonGetUnderlying(env, "{\"a\" : 1, \"b\": 2, \"c\": 3}\n");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            expectedOne.put("b", 2);
            expectedOne.put("c", 3);
            compareMaps(expectedOne, actualOne);

            Map<String, Object> actualTwo = sendJsonGetUnderlying(env, "{\"a\" : 10}\n");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            compareMaps(expectedTwo, actualTwo);

            Map<String, Object> actualThree = sendJsonGetUnderlying(env, "{\"a\" : null, \"c\": 101, \"d\": 102}\n");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            expectedThree.put("c", 101);
            expectedThree.put("d", 102);
            compareMaps(expectedThree, actualThree);

            Map<String, Object> actualFour = sendJsonGetUnderlying(env, "{}\n");
            compareMaps(new LinkedHashMap<>(), actualFour);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            compareMapWBean(expectedOne, it.next());
            compareMapWBean(expectedTwo, it.next());
            compareMapWBean(expectedThree, it.next());
            compareMapWBean(new LinkedHashMap<>(), it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapDynamicOneDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent(a int);\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            Map<String, Object> actualOne = sendJsonGetUnderlying(env, "{\"a\" : 1, \"b\": 2, \"c\": 3}\n");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            expectedOne.put("b", 2);
            expectedOne.put("c", 3);
            compareMaps(expectedOne, actualOne);

            Map<String, Object> actualTwo = sendJsonGetUnderlying(env, "{\"a\" : 10}\n");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            compareMaps(expectedTwo, actualTwo);

            Map<String, Object> actualThree = sendJsonGetUnderlying(env, "{\"a\" : null, \"c\": 101, \"d\": 102}\n");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            expectedThree.put("c", 101);
            expectedThree.put("d", 102);
            compareMaps(expectedThree, actualThree);

            Map<String, Object> actualFour = sendJsonGetUnderlying(env, "{}\n");
            Map<String, Object> expectedFour = new LinkedHashMap<>();
            expectedFour.put("a", null);
            compareMaps(expectedFour, actualFour);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            compareMapWBean(expectedOne, it.next());
            compareMapWBean(expectedTwo, it.next());
            compareMapWBean(expectedThree, it.next());
            compareMapWBean(expectedFour, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonUnderlyingMapDynamicTwoDeclared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent(a int, b int);\n" +
                "@name('s0') select *  from JsonEvent#keepall").addListener("s0");

            Map<String, Object> actualOne = sendJsonGetUnderlying(env, "{\"a\" : 1, \"b\": 2, \"c\": 3}\n");
            Map<String, Object> expectedOne = new LinkedHashMap<>();
            expectedOne.put("a", 1);
            expectedOne.put("b", 2);
            expectedOne.put("c", 3);
            compareMaps(expectedOne, actualOne);

            Map<String, Object> actualTwo = sendJsonGetUnderlying(env, "{\"a\" : 10}\n");
            Map<String, Object> expectedTwo = new LinkedHashMap<>();
            expectedTwo.put("a", 10);
            expectedTwo.put("b", null);
            compareMaps(expectedTwo, actualTwo);

            Map<String, Object> actualThree = sendJsonGetUnderlying(env, "{\"a\" : null, \"c\": 101, \"d\": 102}\n");
            Map<String, Object> expectedThree = new LinkedHashMap<>();
            expectedThree.put("a", null);
            expectedThree.put("b", null);
            expectedThree.put("c", 101);
            expectedThree.put("d", 102);
            compareMaps(expectedThree, actualThree);

            Map<String, Object> actualFour = sendJsonGetUnderlying(env, "{}\n");
            Map<String, Object> expectedFour = new LinkedHashMap<>();
            expectedFour.put("a", null);
            expectedFour.put("b", null);
            compareMaps(expectedFour, actualFour);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            compareMapWBean(expectedOne, it.next());
            compareMapWBean(expectedTwo, it.next());
            compareMapWBean(expectedThree, it.next());
            compareMapWBean(expectedFour, it.next());

            env.undeployAll();
        }
    }

    private static JsonEventObject sendJsonGetUnderlying(RegressionEnvironment env, String json) {
        env.sendEventJson(json, "JsonEvent");
        EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
        return (JsonEventObject) eventBean.getUnderlying();
    }

    private static void compareMapWBean(Map<String, Object> expected, EventBean event) {
        compareMaps(expected, (Map<String, Object>) event.getUnderlying());
    }
}
