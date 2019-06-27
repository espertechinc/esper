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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.json.SupportJsonEventTypeUtil;

import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

public class EventJsonInherits {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonInheritsTwoLevel());
        execs.add(new EventJsonInheritsFourLevel());
        execs.add(new EventJsonInheritsFourLevelSparseOne());
        execs.add(new EventJsonInheritsFourLevelSparseTwo());
        execs.add(new EventJsonInheritsFourLevelEmpty());
        execs.add(new EventJsonInheritsTwoLevelBranched());
        execs.add(new EventJsonInheritsTwoLevelWArrayAndObject());
        execs.add(new EventJsonInheritsAcrossModules());
        execs.add(new EventJsonInheritsDynamicPropsParentOnly());
        execs.add(new EventJsonInheritsDynamicPropsChildOnly());
        return execs;
    }

    private static class EventJsonInheritsDynamicPropsParentOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@JsonSchema(dynamic=true) create json schema ParentEvent();\n" +
                    "@public @buseventtype create json schema ChildEvent() inherits ParentEvent;\n" +
                    "@name('s0') select value? as c0 from ChildEvent#keepall;\n" +
                    "@name('s1') select * from ChildEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0").addListener("s1");
            runAssertionDynamicProps(env);
            env.undeployAll();
        }
    }

    private static class EventJsonInheritsDynamicPropsChildOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create json schema ParentEvent();\n" +
                    "@JsonSchema(dynamic=true) @public @buseventtype create json schema ChildEvent() inherits ParentEvent;\n" +
                    "@name('s0') select value? as c0 from ChildEvent#keepall;\n" +
                    "@name('s1') select * from ChildEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0").addListener("s1");
            runAssertionDynamicProps(env);
            env.undeployAll();
        }
    }

    private static class EventJsonInheritsAcrossModules implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("module A; create json schema A(a1 string)", path);
            env.compileDeploy("module B; create json schema B(b1 string) inherits A", path);
            env.compileDeploy("module C; @public @buseventtype create json schema C(c1 string) inherits B", path);
            env.compileDeploy("@name('s0') select * from C#keepall", path).addListener("s0");

            env.sendEventJson("{ \"a1\": \"a\", \"b1\": \"b\", \"c1\": \"c\"}", "C");
            assertEvent(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertEvent(it.next());

            env.undeployAll();
        }

        private void assertEvent(EventBean event) {
            EPAssertionUtil.assertProps(event, "a1,b1,c1".split(","), new Object[]{"a", "b", "c"});
        }
    }

    private static class EventJsonInheritsTwoLevelWArrayAndObject implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create json schema NestedObject(n1 string);\n" +
                "@public @buseventtype create json schema P(pn NestedObject, pa int[primitive]);\n" +
                "@public @buseventtype create json schema C(cn NestedObject, ca int[primitive]) inherits P;\n" +
                "@name('s0') select * from C#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventJson("{ \"pn\": {\"n1\": \"a\"}, \"pa\": [1, 2], \"cn\": {\"n1\": \"b\"}, \"ca\": [3, 4] }", "C");
            assertEvent(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertEvent(it.next());

            env.undeployAll();
        }

        private void assertEvent(EventBean event) {
            JsonEventObjectBase und = (JsonEventObjectBase) event.getUnderlying();

            assertEquals("{\"n1\":\"a\"}", und.get("pn").toString());
            assertEquals("{\"n1\":\"b\"}", und.get("cn").toString());
            EPAssertionUtil.assertEqualsExactOrder(new int[]{1, 2}, (int[]) und.get("pa"));
            EPAssertionUtil.assertEqualsExactOrder(new int[]{3, 4}, (int[]) und.get("ca"));
            assertEquals("{\"pn\":{\"n1\":\"a\"},\"pa\":[1,2],\"cn\":{\"n1\":\"b\"},\"ca\":[3,4]}", und.toString());
        }
    }

    private static class EventJsonInheritsTwoLevelBranched implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl =
                "@public @buseventtype create json schema P(p1 string);\n" +
                    "@public @buseventtype create json schema C1(c11 string, c12 int) inherits P;\n" +
                    "@public @buseventtype create json schema C2(c21 string) inherits P;\n" +
                    "@public @buseventtype create json schema C3() inherits P;\n" +
                    "@name('sp') select * from P;\n" +
                    "@name('sc1') select * from C1#keepall;\n" +
                    "@name('sc2') select * from C2#keepall;\n" +
                    "@name('sc3') select * from C3#keepall;\n";
            env.compileDeploy(epl).addListener("sp").addListener("sc1").addListener("sc2").addListener("sc3");

            String jsonOne = "{\"p1\":\"PA\",\"c11\":\"x\",\"c12\":50}";
            env.sendEventJson(jsonOne, "C1");
            EventBean eventOne = assertInvoked(env, "sc1", "sp", "sc2,sc3");
            assertC1(jsonOne, eventOne);

            String jsonTwo = "{\"p1\":\"PB\",\"c21\":\"y\"}";
            env.sendEventJson(jsonTwo, "C2");
            EventBean eventTwo = assertInvoked(env, "sc2", "sp", "sc1,sc3");
            assertC2(jsonTwo, eventTwo);

            String jsonThree = "{\"p1\":\"PC\"}";
            env.sendEventJson(jsonThree, "C3");
            EventBean eventThree = assertInvoked(env, "sc3", "sp", "sc1,sc2");
            assertC3(jsonThree, eventThree);

            String jsonFour = "{\"p1\":\"PD\"}";
            env.sendEventJson(jsonFour, "P");
            EventBean eventFour = assertInvoked(env, "sp", null, "sc1,sc2,sc3");
            assertP(jsonFour, eventFour);

            env.milestone(0);

            Iterator<EventBean> itSC1 = env.statement("sc1").iterator();
            assertC1(jsonOne, itSC1.next());

            Iterator<EventBean> itSC2 = env.statement("sc2").iterator();
            assertC2(jsonTwo, itSC2.next());

            Iterator<EventBean> itSC3 = env.statement("sc3").iterator();
            assertC3(jsonThree, itSC3.next());

            env.undeployAll();
        }

        private void assertP(String jsonFour, EventBean eventFour) {
            LinkedHashMap<String, Object> expectedP = new LinkedHashMap<>();
            expectedP.put("p1", "PD");
            assertAny(expectedP, jsonFour, eventFour);
        }

        private void assertC3(String jsonThree, EventBean eventThree) {
            LinkedHashMap<String, Object> expectedC3 = new LinkedHashMap<>();
            expectedC3.put("p1", "PC");
            assertAny(expectedC3, jsonThree, eventThree);
        }

        private void assertC2(String jsonTwo, EventBean eventTwo) {
            LinkedHashMap<String, Object> expectedC2 = new LinkedHashMap<>();
            expectedC2.put("p1", "PB");
            expectedC2.put("c21", "y");
            assertAny(expectedC2, jsonTwo, eventTwo);
        }

        private void assertC1(String jsonOne, EventBean event) {
            LinkedHashMap<String, Object> expectedC1 = new LinkedHashMap<>();
            expectedC1.put("p1", "PA");
            expectedC1.put("c11", "x");
            expectedC1.put("c12", 50);
            assertAny(expectedC1, jsonOne, event);
        }

        private void assertAny(LinkedHashMap<String, Object> expected, String jsonOne, EventBean event) {
            JsonEventObject und = (JsonEventObject) event.getUnderlying();
            SupportJsonEventTypeUtil.compareMaps(expected, und);
            assertEquals(jsonOne, und.toString());
        }

        private EventBean assertInvoked(RegressionEnvironment env, String undStmt, String invokedOther, String notInvokedCsv) {
            EventBean event = env.listener(undStmt).assertOneGetNewAndReset();
            if (invokedOther != null) {
                env.listener(invokedOther).assertInvokedAndReset();
            }
            String[] splitNotInvoked = notInvokedCsv.split(",");
            for (String s : splitNotInvoked) {
                assertFalse(env.listener(s).isInvoked());
            }
            return event;
        }
    }

    private static class EventJsonInheritsFourLevelEmpty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create json schema A();\n" +
                    "@public @buseventtype create json schema B() inherits A;\n" +
                    "@public @buseventtype create json schema C() inherits B;\n" +
                    "@public @buseventtype create json schema D() inherits C;\n" +
                    "@name('sd') select * from D#keepall;\n";
            env.compileDeploy(epl).addListener("sd");

            env.sendEventJson("{}", "D");
            assertEvent(env.listener("sd").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("sd").iterator();
            assertEvent(it.next());

            env.undeployAll();
        }

        private void assertEvent(EventBean event) {
            JsonEventObjectBase und = (JsonEventObjectBase) event.getUnderlying();

            assertEquals(0, und.getNativeSize());
            assertNoSuchElement(() -> und.getNativeValue(0));
            assertNoSuchElement(() -> und.getNativeKey(0));
            assertNoSuchElement(() -> und.getNativeEntry(0));
            assertEquals(-1, und.getNativeNum("x"));
            assertTrue(und.getJsonValues().isEmpty());

            SupportJsonEventTypeUtil.compareMaps(new LinkedHashMap<>(), und);
            assertEquals("{}", und.toString());
        }
    }

    private static class EventJsonInheritsFourLevelSparseTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create json schema A();\n" +
                    "@public @buseventtype create json schema B(b1 double) inherits A;\n" +
                    "@public @buseventtype create json schema C() inherits B;\n" +
                    "@public @buseventtype create json schema D(d1 string) inherits C;\n" +
                    "@name('sd') select * from D#keepall;\n";
            env.compileDeploy(epl).addListener("sd");

            env.sendEventJson("{\"b1\": 4, \"d1\": \"def\"}", "D");
            assertEvent(env.listener("sd").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("sd").iterator();
            assertEvent(it.next());

            env.undeployAll();
        }

        private void assertEvent(EventBean event) {
            JsonEventObjectBase und = (JsonEventObjectBase) event.getUnderlying();

            assertEquals(2, und.getNativeSize());
            assertByIndex(2, und::getNativeValue, new Object[]{4d, "def"});
            assertByIndex(2, und::getNativeKey, new Object[]{"b1", "d1"});
            assertByIndex(2, i -> und.getNativeEntry(i), new Object[]{toEntry("b1", 4d), toEntry("d1", "def")});
            assertByName(und, "b1,d1");
            assertTrue(und.getJsonValues().isEmpty());

            Map<String, Object> compared = new LinkedHashMap<>();
            compared.put("b1", 4d);
            compared.put("d1", "def");
            SupportJsonEventTypeUtil.compareMaps(compared, und);

            assertEquals("{\"b1\":4.0,\"d1\":\"def\"}", und.toString());
        }
    }

    private static class EventJsonInheritsFourLevelSparseOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create json schema A(a1 double);\n" +
                    "@public @buseventtype create json schema B() inherits A;\n" +
                    "@public @buseventtype create json schema C(c1 string) inherits B;\n" +
                    "@public @buseventtype create json schema D() inherits C;\n" +
                    "@name('sd') select * from D#keepall;\n";
            env.compileDeploy(epl).addListener("sd");

            env.sendEventJson("{\"a1\": 4, \"c1\": \"def\"}", "D");
            assertEvent(env.listener("sd").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("sd").iterator();
            assertEvent(it.next());

            env.undeployAll();
        }

        private void assertEvent(EventBean event) {
            JsonEventObjectBase und = (JsonEventObjectBase) event.getUnderlying();

            assertEquals(2, und.getNativeSize());
            assertByIndex(2, und::getNativeValue, new Object[]{4d, "def"});
            assertByIndex(2, und::getNativeKey, new Object[]{"a1", "c1"});
            assertByIndex(2, und::getNativeEntry, new Object[]{toEntry("a1", 4d), toEntry("c1", "def")});
            assertByName(und, "a1,c1");
            assertTrue(und.getJsonValues().isEmpty());

            Map<String, Object> compared = new LinkedHashMap<>();
            compared.put("a1", 4d);
            compared.put("c1", "def");
            SupportJsonEventTypeUtil.compareMaps(compared, und);

            assertEquals("{\"a1\":4.0,\"c1\":\"def\"}", und.toString());
        }
    }

    private static class EventJsonInheritsFourLevel implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@public @buseventtype create json schema A(a1 double);\n" +
                    "@public @buseventtype create json schema B(b1 string, b2 int) inherits A;\n" +
                    "@public @buseventtype create json schema C(c1 string) inherits B;\n" +
                    "@public @buseventtype create json schema D(d1 double, d2 int) inherits C;\n" +
                    "@name('sa') select * from A#keepall;\n" +
                    "@name('sb') select * from B#keepall;\n" +
                    "@name('sc') select * from C#keepall;\n" +
                    "@name('sd') select * from D#keepall;\n";
            env.compileDeploy(epl).addListener("sa").addListener("sb").addListener("sc").addListener("sd");

            env.sendEventJson("{\"d2\": 1, \"d1\": 2, \"c1\": \"def\", \"b2\": 3, \"b1\": \"x\", \"a1\": 4}", "D");
            EventBean eventOne = env.listener("sd").assertOneGetNewAndReset();
            assertEvent(eventOne);
            env.listener("sa").assertInvokedAndReset();
            env.listener("sb").assertInvokedAndReset();
            env.listener("sc").assertInvokedAndReset();

            env.milestone(0);

            Iterator<EventBean> it = env.statement("sd").iterator();
            assertEvent(it.next());

            env.undeployAll();
        }

        private void assertEvent(EventBean event) {
            JsonEventObjectBase und = (JsonEventObjectBase) event.getUnderlying();
            assertEquals(6, und.getNativeSize());
            assertByIndex(6, und::getNativeValue, new Object[]{4d, "x", 3, "def", 2d, 1});
            assertByIndex(6, und::getNativeKey, new Object[]{"a1", "b1", "b2", "c1", "d1", "d2"});
            assertByIndex(6, und::getNativeEntry, new Object[]{toEntry("a1", 4d),
                toEntry("b1", "x"), toEntry("b2", 3), toEntry("c1", "def"), toEntry("d1", 2d), toEntry("d2", 1)});
            assertByName(und, "a1,b1,b2,c1,d1,d2");
            assertTrue(und.getJsonValues().isEmpty());

            Map<String, Object> compared = new LinkedHashMap<>();
            compared.put("a1", 4d);
            compared.put("b1", "x");
            compared.put("b2", 3);
            compared.put("c1", "def");
            compared.put("d1", 2d);
            compared.put("d2", 1);
            SupportJsonEventTypeUtil.compareMaps(compared, und);

            assertEquals("{\"a1\":4.0,\"b1\":\"x\",\"b2\":3,\"c1\":\"def\",\"d1\":2.0,\"d2\":1}", und.toString());
        }
    }

    private static class EventJsonInheritsTwoLevel implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema ParentJson(p1 string, p2 int);\n" +
                "@public @buseventtype create json schema ChildJson(c1 string, c2 int) inherits ParentJson;\n" +
                "@name('s0') select * from ChildJson#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventJson("{\"p1\": \"abc\", \"p2\": 10, \"c1\": \"def\", \"c2\": 20}", "ChildJson");
            assertEvent(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertEvent(it.next());

            env.undeployAll();
        }

        private void assertEvent(EventBean event) {
            JsonEventObjectBase und = (JsonEventObjectBase) event.getUnderlying();

            assertByIndex(4, und::getNativeValue, new Object[]{"abc", 10, "def", 20});
            assertByIndex(4, und::getNativeKey, new Object[]{"p1", "p2", "c1", "c2"});
            assertByIndex(4, und::getNativeEntry, new Object[]{toEntry("p1", "abc"), toEntry("p2", 10), toEntry("c1", "def"), toEntry("c2", 20)});
            assertEquals(4, und.getNativeSize());
            assertByName(und, "p1,p2,c1,c2");
            assertTrue(und.getJsonValues().isEmpty());

            Map<String, Object> compared = new LinkedHashMap<>();
            compared.put("p1", "abc");
            compared.put("p2", 10);
            compared.put("c1", "def");
            compared.put("c2", 20);
            SupportJsonEventTypeUtil.compareMaps(compared, und);

            assertEquals("{\"p1\":\"abc\",\"p2\":10,\"c1\":\"def\",\"c2\":20}", und.toString());
        }
    }

    private static Map.Entry<String, Object> toEntry(String name, Object value) {
        return new AbstractMap.SimpleEntry<>(name, value);
    }

    private static void assertByName(JsonEventObjectBase und, String csv) {
        String[] split = csv.split(",");
        for (int i = 0; i < split.length; i++) {
            assertTrue(und.containsKey(split[i]));
            assertEquals(i, und.getNativeNum(split[i]));
        }
    }

    private static void assertByIndex(int numFields, Function<Integer, Object> indexFunction, Object[] expected) {
        Object[] actual = new Object[numFields];
        for (int i = 0; i < numFields; i++) {
            actual[i] = indexFunction.apply(i);
        }
        EPAssertionUtil.assertEqualsExactOrder(expected, actual);
    }

    private static void assertNoSuchElement(Runnable runnable) {
        try {
            runnable.run();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }
    }

    private static void runAssertionDynamicProps(RegressionEnvironment env) {
        String jsonOne = "{\"value\":10}";
        Object expectedOne = 10;
        sendAssertDynamicProp(env, jsonOne, "ChildEvent", expectedOne);

        String jsonTwo = "{\"value\":\"abc\"}";
        Object expectedTwo = "abc";
        sendAssertDynamicProp(env, jsonTwo, "ChildEvent", expectedTwo);

        env.milestone(0);

        Iterator<EventBean> itS0 = env.statement("s0").iterator();
        assertEquals(10, itS0.next().get("c0"));
        assertEquals("abc", itS0.next().get("c0"));

        Iterator<EventBean> itS1 = env.statement("s1").iterator();
        assertEventJson(itS1.next(), jsonOne, expectedOne);
        assertEventJson(itS1.next(), jsonTwo, expectedTwo);
    }

    private static void sendAssertDynamicProp(RegressionEnvironment env, String json, String eventTypeName, Object expected) {
        env.sendEventJson(json, eventTypeName);
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));

        assertEventJson(env.listener("s1").assertOneGetNewAndReset(), json, expected);
    }

    private static void assertEventJson(EventBean eventBean, String json, Object expected) {
        JsonEventObjectBase event = (JsonEventObjectBase) eventBean.getUnderlying();
        SupportJsonEventTypeUtil.compareMaps(Collections.singletonMap("value", expected), event);
        assertEquals(json, event.toString());
    }
}
