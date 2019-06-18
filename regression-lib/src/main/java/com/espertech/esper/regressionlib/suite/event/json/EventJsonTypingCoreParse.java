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
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.support.SupportEnum.*;
import static org.junit.Assert.*;

public class EventJsonTypingCoreParse {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonTypingParseBasicType());
        execs.add(new EventJsonTypingParseBasicTypeArray());
        execs.add(new EventJsonTypingParseBasicTypeArray2Dim());
        execs.add(new EventJsonTypingParseEnumType());
        execs.add(new EventJsonTypingParseBigDecimalBigInt());
        execs.add(new EventJsonTypingParseObjectType());
        execs.add(new EventJsonTypingParseObjectArrayType());
        execs.add(new EventJsonTypingParseMapType());
        execs.add(new EventJsonTypingParseDynamicPropJsonTypes());
        execs.add(new EventJsonTypingParseDynamicPropMixedOjectArray());
        execs.add(new EventJsonTypingParseDynamicPropNestedArray());
        execs.add(new EventJsonTypingParseDynamicPropNumberFormat());
        return execs;
    }

    private static class EventJsonTypingParseMapType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 Map);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{{"c0", Map.class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            sendAssertColumn(env, "{}", null);
            sendAssertColumn(env, "{\"c0\": {\"c1\" : 10}}", Collections.singletonMap("c1", 10));
            sendAssertColumn(env, "{\"c0\": {\"c1\": {\"c2\": 20}}}", Collections.singletonMap("c1", Collections.singletonMap("c2", 20)));
            Consumer<Object> assertionOne = result -> {
                Object[] oa = (Object[]) ((Map) result).get("c1");
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{"c2", 20}, oa);
            };
            sendAssert(env, "{\"c0\": {\"c1\": [\"c2\", 20]}}", assertionOne);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertColumn(it.next(), null);
            assertColumn(it.next(), Collections.singletonMap("c1", 10));
            assertColumn(it.next(), Collections.singletonMap("c1", Collections.singletonMap("c2", 20)));
            justAssert(it.next(), assertionOne);

            env.undeployAll();
        }
    }

    private static class EventJsonTypingParseObjectArrayType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 Object[]);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{{"c0", Object[].class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            sendAssertColumn(env, "{}", null);
            sendAssertColumn(env, "{\"c0\": []}", new Object[0]);
            sendAssertColumn(env, "{\"c0\": [1.0]}", new Object[]{1.0d});
            sendAssertColumn(env, "{\"c0\": [null]}", new Object[]{null});
            sendAssertColumn(env, "{\"c0\": [true]}", new Object[]{true});
            sendAssertColumn(env, "{\"c0\": [false]}", new Object[]{false});
            sendAssertColumn(env, "{\"c0\": [\"abc\"]}", new Object[]{"abc"});
            sendAssertColumn(env, "{\"c0\": [[\"abc\"]]}", new Object[][]{{"abc"}});
            sendAssertColumn(env, "{\"c0\": [[]]}", new Object[]{new Object[0]});
            sendAssertColumn(env, "{\"c0\": [[\"abc\", 2]]}", new Object[][]{{"abc", 2}});
            sendAssertColumn(env, "{\"c0\": [[[\"abc\"], [5.0]]]}", new Object[][][]{{{"abc"}, {5d}}});
            sendAssertColumn(env, "{\"c0\": [{\"c1\": 10}]}", new Object[]{Collections.singletonMap("c1", 10)});
            sendAssertColumn(env, "{\"c0\": [{\"c1\": 10, \"c2\": \"abc\"}]}", new Object[]{CollectionUtil.buildMap("c1", 10, "c2", "abc")});

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertColumn(it.next(), null);
            assertColumn(it.next(), new Object[0]);
            assertColumn(it.next(), new Object[]{1.0d});
            assertColumn(it.next(), new Object[]{null});
            assertColumn(it.next(), new Object[]{true});
            assertColumn(it.next(), new Object[]{false});
            assertColumn(it.next(), new Object[]{"abc"});
            assertColumn(it.next(), new Object[][]{{"abc"}});
            assertColumn(it.next(), new Object[]{new Object[0]});
            assertColumn(it.next(), new Object[][]{{"abc", 2}});
            assertColumn(it.next(), new Object[][][]{{{"abc"}, {5d}}});
            assertColumn(it.next(), new Object[]{Collections.singletonMap("c1", 10)});
            assertColumn(it.next(), new Object[]{CollectionUtil.buildMap("c1", 10, "c2", "abc")});

            env.undeployAll();
        }
    }

    private static class EventJsonTypingParseObjectType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 Object);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{{"c0", Object.class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            sendAssertColumn(env, "{}", null);
            sendAssertColumn(env, "{\"c0\": 1}", 1);
            sendAssertColumn(env, "{\"c0\": 1.0}", 1.0d);
            sendAssertColumn(env, "{\"c0\": null}", null);
            sendAssertColumn(env, "{\"c0\": true}", true);
            sendAssertColumn(env, "{\"c0\": false}", false);
            sendAssertColumn(env, "{\"c0\": \"abc\"}", "abc");
            sendAssertColumn(env, "{\"c0\": [\"abc\"]}", new Object[]{"abc"});
            sendAssertColumn(env, "{\"c0\": []}", new Object[0]);
            sendAssertColumn(env, "{\"c0\": [\"abc\", 2]}", new Object[]{"abc", 2});
            sendAssertColumn(env, "{\"c0\": [[\"abc\"], [5.0]]}", new Object[][]{{"abc"}, {5d}});
            sendAssertColumn(env, "{\"c0\": {\"c1\": 10}}", Collections.singletonMap("c1", 10));
            sendAssertColumn(env, "{\"c0\": {\"c1\": 10, \"c2\": \"abc\"}}", CollectionUtil.buildMap("c1", 10, "c2", "abc"));

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertColumn(it.next(), null);
            assertColumn(it.next(), 1);
            assertColumn(it.next(), 1.0d);
            assertColumn(it.next(), null);
            assertColumn(it.next(), true);
            assertColumn(it.next(), false);
            assertColumn(it.next(), "abc");
            assertColumn(it.next(), new Object[]{"abc"});
            assertColumn(it.next(), new Object[0]);
            assertColumn(it.next(), new Object[]{"abc", 2});
            assertColumn(it.next(), new Object[][]{{"abc"}, {5d}});
            assertColumn(it.next(), Collections.singletonMap("c1", 10));
            assertColumn(it.next(), CollectionUtil.buildMap("c1", 10, "c2", "abc"));

            env.undeployAll();
        }
    }

    private static class EventJsonTypingParseBigDecimalBigInt implements RegressionExecution {
        private final static BigInteger BI = new BigInteger("123456789123456789123456789");
        private final static BigDecimal BD = new BigDecimal("123456789123456789123456789.1");

        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 BigInteger, c1 BigDecimal," +
                "c2 BigInteger[], c3 BigDecimal[], c4 BigInteger[][], c5 BigDecimal[][]);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{
                {"c0", BigInteger.class},
                {"c1", BigDecimal.class},
                {"c2", BigInteger[].class},
                {"c3", BigDecimal[].class},
                {"c4", BigInteger[][].class},
                {"c5", BigDecimal[][].class}
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String json = "{\"c0\": 123456789123456789123456789, \"c1\": 123456789123456789123456789.1," +
                "\"c2\": [123456789123456789123456789], \"c3\": [123456789123456789123456789.1]," +
                "\"c4\": [[123456789123456789123456789]], \"c5\": [[123456789123456789123456789.1]]" +
                "}";
            env.sendEventJson(json, "JsonEvent");
            assertFilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{}";
            env.sendEventJson(json, "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{\"c0\": null, \"c1\": null, \"c2\": null, \"c3\": null, \"c4\": null, \"c5\": null}";
            env.sendEventJson(json, "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(it.next());
            assertUnfilled(it.next());
            assertUnfilled(it.next());

            env.undeployAll();
        }

        private void assertUnfilled(EventBean event) {
            sendAssertFields(event, null, null, null, null, null, null);
        }

        private void assertFilled(EventBean event) {
            sendAssertFields(event, BI, BD, new BigInteger[]{BI}, new BigDecimal[]{BD}, new BigInteger[][]{{BI}}, new BigDecimal[][]{{BD}});
        }

        private void sendAssertFields(EventBean event, BigInteger c0, BigDecimal c1,
                                      BigInteger[] c2, BigDecimal[] c3, BigInteger[][] c4, BigDecimal[][] c5) {
            EPAssertionUtil.assertProps(event, "c0,c1,c2,c3,c4,c5".split(","), new Object[]{c0, c1, c2, c3, c4, c5});
        }
    }

    private static class EventJsonTypingParseEnumType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 SupportEnum, c1 SupportEnum[], c2 SupportEnum[][]);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{
                {"c0", SupportEnum.class},
                {"c1", SupportEnum[].class},
                {"c2", SupportEnum[][].class}
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String json = "{\"c0\": \"ENUM_VALUE_2\", \"c1\": [\"ENUM_VALUE_2\", \"ENUM_VALUE_1\"], \"c2\": [[\"ENUM_VALUE_2\"], [\"ENUM_VALUE_1\", \"ENUM_VALUE_3\"]]}";
            env.sendEventJson(json, "JsonEvent");
            assertFilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{}";
            env.sendEventJson(json, "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{\"c0\": null, \"c1\": null, \"c2\": null}";
            env.sendEventJson(json, "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{\"c1\": [], \"c2\": [[]]}";
            env.sendEventJson(json, "JsonEvent");
            assertEmptyArray(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(it.next());
            assertUnfilled(it.next());
            assertUnfilled(it.next());
            assertEmptyArray(it.next());

            env.undeployAll();
        }

        private void assertFilled(EventBean event) {
            assertFields(event, ENUM_VALUE_2, new SupportEnum[]{ENUM_VALUE_2, ENUM_VALUE_1},
                new SupportEnum[][]{{ENUM_VALUE_2}, {ENUM_VALUE_1, ENUM_VALUE_3}});
        }

        private void assertEmptyArray(EventBean event) {
            assertFields(event, null, new SupportEnum[0], new SupportEnum[][]{{}});
        }

        private void assertUnfilled(EventBean event) {
            assertFields(event, null, null, null);
        }

        private void assertFields(EventBean event, SupportEnum c0, SupportEnum[] c1, SupportEnum[][] c2) {
            EPAssertionUtil.assertProps(event, "c0,c1,c2".split(","), new Object[]{c0, c1, c2});
        }
    }

    private static class EventJsonTypingParseBasicTypeArray implements RegressionExecution {
        private final static String[] FIELDS_ZERO = "c0,c1,c2,c3,c4,c5,c6".split(",");
        private final static String[] FIELDS_ONE = "c7,c8,c9,c10,c11,c12".split(",");
        private final static String[] FIELDS_TWO = "c13,c14,c15,c16".split(",");

        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (" +
                "c0 string[], " +
                "c1 char[], c2 char[primitive], " +
                "c3 bool[], c4 boolean[primitive], " +
                "c5 byte[], c6 byte[primitive], " +
                "c7 short[], c8 short[primitive], " +
                "c9 int[], c10 int[primitive], " +
                "c11 long[], c12 long[primitive], " +
                "c13 double[], c14 double[primitive], " +
                "c15 float[], c16 float[primitive]);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{
                {"c0", String[].class},
                {"c1", Character[].class},
                {"c2", char[].class},
                {"c3", Boolean[].class},
                {"c4", boolean[].class},
                {"c5", Byte[].class},
                {"c6", byte[].class},
                {"c7", Short[].class},
                {"c8", short[].class},
                {"c9", Integer[].class},
                {"c10", int[].class},
                {"c11", Long[].class},
                {"c12", long[].class},
                {"c13", Double[].class},
                {"c14", double[].class},
                {"c15", Float[].class},
                {"c16", float[].class},
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String json = "{ \"c0\": [\"abc\", \"def\"],\n" +
                "\"c1\": [\"xy\", \"z\"],\n" +
                "\"c2\": [\"x\", \"yz\"],\n" +
                "\"c3\": [true, false],\n" +
                "\"c4\": [false, true],\n" +
                "\"c5\": [10, 11],\n" +
                "\"c6\": [12, 13],\n" +
                "\"c7\": [20, 21],\n" +
                "\"c8\": [22, 23],\n" +
                "\"c9\": [30, 31],\n" +
                "\"c10\": [32, 33],\n" +
                "\"c11\": [40, 41],\n" +
                "\"c12\": [42, 43],\n" +
                "\"c13\": [50, 51],\n" +
                "\"c14\": [52, 53],\n" +
                "\"c15\": [60, 61],\n" +
                "\"c16\": [62, 63]" +
                "}\n";
            env.sendEventJson(json, "JsonEvent");
            assertFilled(env.listener("s0").assertOneGetNewAndReset());

            env.sendEventJson("[]", "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{ \"c0\": [],\n" +
                "\"c1\": [],\n" +
                "\"c2\": [],\n" +
                "\"c3\": [],\n" +
                "\"c4\": [],\n" +
                "\"c5\": [],\n" +
                "\"c6\": [],\n" +
                "\"c7\": [],\n" +
                "\"c8\": [],\n" +
                "\"c9\": [],\n" +
                "\"c10\": [],\n" +
                "\"c11\": [],\n" +
                "\"c12\": [],\n" +
                "\"c13\": [],\n" +
                "\"c14\": [],\n" +
                "\"c15\": [],\n" +
                "\"c16\": []" +
                "}\n";
            env.sendEventJson(json, "JsonEvent");
            assertEmptyArray(env.listener("s0").assertOneGetNewAndReset());

            json = "{ \"c0\": null,\n" +
                "\"c1\": null,\n" +
                "\"c2\": null,\n" +
                "\"c3\": null,\n" +
                "\"c4\": null,\n" +
                "\"c5\": null,\n" +
                "\"c6\": null,\n" +
                "\"c7\": null,\n" +
                "\"c8\": null,\n" +
                "\"c9\": null,\n" +
                "\"c10\": null,\n" +
                "\"c11\": null,\n" +
                "\"c12\": null,\n" +
                "\"c13\": null,\n" +
                "\"c14\": null,\n" +
                "\"c15\": null,\n" +
                "\"c16\": null" +
                "}\n";
            env.sendEventJson(json, "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{ \"c0\": [null, \"def\", null],\n" +
                "\"c1\": [\"xy\", null],\n" +
                "\"c2\": [\"x\"],\n" +
                "\"c3\": [true, null, false],\n" +
                "\"c4\": [true],\n" +
                "\"c5\": [null, null, null],\n" +
                "\"c6\": [12],\n" +
                "\"c7\": [20, 21, null],\n" +
                "\"c8\": [23],\n" +
                "\"c9\": [null, 30, null, 31, null, 32],\n" +
                "\"c10\": [32],\n" +
                "\"c11\": [null, 40, 41, null],\n" +
                "\"c12\": [42],\n" +
                "\"c13\": [null, null, 51],\n" +
                "\"c14\": [52],\n" +
                "\"c15\": [null],\n" +
                "\"c16\": [63]" +
                "}\n";
            env.sendEventJson(json, "JsonEvent");
            assertPartialFilled(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(it.next());
            assertUnfilled(it.next());
            assertEmptyArray(it.next());
            assertUnfilled(it.next());
            assertPartialFilled(it.next());

            env.undeployAll();
        }

        private void assertEmptyArray(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ZERO, new Object[]{new String[0], new Character[0],
                new char[0], new Boolean[0], new boolean[0], new Byte[0], new byte[0]});
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{new Short[0], new short[0],
                new Integer[0], new int[0], new Long[0], new long[0]});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{new Double[0], new double[0], new Float[0], new float[0]});
        }

        private void assertPartialFilled(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ZERO, new Object[]{new String[]{null, "def", null}, new Character[]{'x', null},
                new char[]{'x'}, new Boolean[]{true, null, false}, new boolean[]{true}, new Byte[]{null, null, null}, new byte[]{12}});
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{new Short[]{20, 21, null}, new short[]{23},
                new Integer[]{null, 30, null, 31, null, 32}, new int[]{32}, new Long[]{null, 40L, 41L, null}, new long[]{42}});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{new Double[]{null, null, 51d}, new double[]{52},
                new Float[]{null}, new float[]{63}});
        }

        private void assertFilled(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ZERO, new Object[]{new String[]{"abc", "def"}, new Character[]{'x', 'z'},
                new char[]{'x', 'y'}, new Boolean[]{true, false}, new boolean[]{false, true}, new Byte[]{10, 11}, new byte[]{12, 13}});
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{new Short[]{20, 21}, new short[]{22, 23},
                new Integer[]{30, 31}, new int[]{32, 33}, new Long[]{40L, 41L}, new long[]{42, 43}});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{new Double[]{50d, 51d}, new double[]{52, 53},
                new Float[]{60f, 61f}, new float[]{62, 63}});
        }

        private void assertUnfilled(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ZERO, new Object[]{null, null, null, null, null, null, null});
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{null, null, null, null, null, null});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{null, null, null, null});
        }
    }

    private static class EventJsonTypingParseBasicTypeArray2Dim implements RegressionExecution {
        private final static String[] FIELDS_ZERO = "c0,c1,c2,c3,c4,c5,c6".split(",");
        private final static String[] FIELDS_ONE = "c7,c8,c9,c10,c11,c12".split(",");
        private final static String[] FIELDS_TWO = "c13,c14,c15,c16".split(",");

        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (" +
                "c0 string[][], " +
                "c1 char[][], c2 char[primitive][], " +
                "c3 bool[][], c4 boolean[primitive][], " +
                "c5 byte[][], c6 byte[primitive][], " +
                "c7 short[][], c8 short[primitive][], " +
                "c9 int[][], c10 int[primitive][], " +
                "c11 long[][], c12 long[primitive][], " +
                "c13 double[][], c14 double[primitive][], " +
                "c15 float[][], c16 float[primitive][]);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{
                {"c0", String[][].class},
                {"c1", Character[][].class},
                {"c2", char[][].class},
                {"c3", Boolean[][].class},
                {"c4", boolean[][].class},
                {"c5", Byte[][].class},
                {"c6", byte[][].class},
                {"c7", Short[][].class},
                {"c8", short[][].class},
                {"c9", Integer[][].class},
                {"c10", int[][].class},
                {"c11", Long[][].class},
                {"c12", long[][].class},
                {"c13", Double[][].class},
                {"c14", double[][].class},
                {"c15", Float[][].class},
                {"c16", float[][].class},
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String json = "{ \"c0\": [[\"a\", \"b\"],[\"c\"]],\n" +
                "\"c1\": [[\"xy\", \"z\"],[\"n\"]],\n" +
                "\"c2\": [[\"x\"], [\"y\", \"z\"]],\n" +
                "\"c3\": [[], [true, false], []],\n" +
                "\"c4\": [[false, true]],\n" +
                "\"c5\": [[10], [11]],\n" +
                "\"c6\": [[12, 13]],\n" +
                "\"c7\": [[20, 21], [22, 23]],\n" +
                "\"c8\": [[22], [23], []],\n" +
                "\"c9\": [[], [], [30, 31]],\n" +
                "\"c10\": [[32], [33, 34]],\n" +
                "\"c11\": [[40], [], [41]],\n" +
                "\"c12\": [[42, 43], [44]],\n" +
                "\"c13\": [[50], [51, 52], [53]],\n" +
                "\"c14\": [[54], [55, 56]],\n" +
                "\"c15\": [[60, 61], []],\n" +
                "\"c16\": [[62], [63]]" +
                "}\n";
            env.sendEventJson(json, "JsonEvent");
            assertFilled(env.listener("s0").assertOneGetNewAndReset());

            env.sendEventJson("[]", "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{ \"c0\": [],\n" +
                "\"c1\": [],\n" +
                "\"c2\": [],\n" +
                "\"c3\": [],\n" +
                "\"c4\": [],\n" +
                "\"c5\": [],\n" +
                "\"c6\": [],\n" +
                "\"c7\": [],\n" +
                "\"c8\": [],\n" +
                "\"c9\": [],\n" +
                "\"c10\": [],\n" +
                "\"c11\": [],\n" +
                "\"c12\": [],\n" +
                "\"c13\": [],\n" +
                "\"c14\": [],\n" +
                "\"c15\": [],\n" +
                "\"c16\": []" +
                "}\n";
            env.sendEventJson(json, "JsonEvent");
            assertEmptyArray(env.listener("s0").assertOneGetNewAndReset());

            json = "{ \"c0\": null,\n" +
                "\"c1\": null,\n" +
                "\"c2\": null,\n" +
                "\"c3\": null,\n" +
                "\"c4\": null,\n" +
                "\"c5\": null,\n" +
                "\"c6\": null,\n" +
                "\"c7\": null,\n" +
                "\"c8\": null,\n" +
                "\"c9\": null,\n" +
                "\"c10\": null,\n" +
                "\"c11\": null,\n" +
                "\"c12\": null,\n" +
                "\"c13\": null,\n" +
                "\"c14\": null,\n" +
                "\"c15\": null,\n" +
                "\"c16\": null" +
                "}\n";
            env.sendEventJson(json, "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            json = "{ \"c0\": [[null, \"a\"]],\n" +
                "\"c1\": [[null], [\"xy\"]],\n" +
                "\"c2\": [null, [\"x\"]],\n" +
                "\"c3\": [[null], [true]],\n" +
                "\"c4\": [[true], null],\n" +
                "\"c5\": [null, null],\n" +
                "\"c6\": [null, [12, 13]],\n" +
                "\"c7\": [[21], null],\n" +
                "\"c8\": [null, [23], null],\n" +
                "\"c9\": [[30], null, [31]],\n" +
                "\"c10\": [[]],\n" +
                "\"c11\": [[], []],\n" +
                "\"c12\": [[42]],\n" +
                "\"c13\": [null, []],\n" +
                "\"c14\": [[], null],\n" +
                "\"c15\": [[null]],\n" +
                "\"c16\": [[63]]" +
                "}\n";
            env.sendEventJson(json, "JsonEvent");
            assertSomeFilled(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(it.next());
            assertUnfilled(it.next());
            assertEmptyArray(it.next());
            assertUnfilled(it.next());
            assertSomeFilled(it.next());

            env.undeployAll();
        }

        private void assertEmptyArray(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ZERO, new Object[]{new String[0][], new Character[0][],
                new char[0][], new Boolean[0][], new boolean[0][], new Byte[0][], new byte[0][]});
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{new Short[0][], new short[0][],
                new Integer[0][], new int[0][], new Long[0][], new long[0][]});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{new Double[0][], new double[0][], new Float[0][], new float[0][]});
        }

        private void assertSomeFilled(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ZERO, new Object[]{new String[][]{{null, "a"}}, new Character[][]{{null}, {'x'}},
                new char[][]{null, {'x'}}, new Boolean[][]{{null}, {true}},
                new boolean[][]{{true}, null}, new Byte[][]{null, null}, new byte[][]{null, {12, 13}}});
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{new Short[][]{{21}, null}, new short[][]{null, {23}, null},
                new Integer[][]{{30}, null, {31}}, new int[][]{{}}, new Long[][]{{}, {}}, new long[][]{{42}}});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{new Double[][]{null, {}}, new double[][]{{}, null},
                new Float[][]{{null}}, new float[][]{{63}}});
        }

        private void assertUnfilled(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ZERO, new Object[]{null, null, null, null, null, null, null});
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{null, null, null, null, null, null});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{null, null, null, null});
        }

        private void assertFilled(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ZERO, new Object[]{new String[][]{{"a", "b"}, {"c"}}, new Character[][]{{'x', 'z'}, {'n'}},
                new char[][]{{'x'}, {'y', 'z'}}, new Boolean[][]{{}, {true, false}, {}},
                new boolean[][]{{false, true}}, new Byte[][]{{10}, {11}}, new byte[][]{{12, 13}}});
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{new Short[][]{{20, 21}, {22, 23}}, new short[][]{{22}, {23}, {}},
                new Integer[][]{{}, {}, {30, 31}}, new int[][]{{32}, {33, 34}}, new Long[][]{{40L}, {}, {41L}}, new long[][]{{42, 43}, {44}}});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{new Double[][]{{50d}, {51d, 52d}, {53d}}, new double[][]{{54}, {55, 56}},
                new Float[][]{{60f, 61f}, {}}, new float[][]{{62}, {63}}});
        }
    }

    private static class EventJsonTypingParseBasicType implements RegressionExecution {
        private final static String[] FIELDS_ONE = "c0,c1,c2,c3,c4,c5,c6".split(",");
        private final static String[] FIELDS_TWO = "c7,c8,c9,c10,c11,c12".split(",");

        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (" +
                "c0 string, c1 char, c2 character, c3 bool, c4 boolean, " +
                "c5 byte, c6 short, c7 int, c8 integer, c9 long, c10 double, c11 float, c12 null);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{
                {"c0", String.class},
                {"c1", Character.class},
                {"c2", Character.class},
                {"c3", Boolean.class},
                {"c4", Boolean.class},
                {"c5", Byte.class},
                {"c6", Short.class},
                {"c7", Integer.class},
                {"c8", Integer.class},
                {"c9", Long.class},
                {"c10", Double.class},
                {"c11", Float.class},
                {"c12", null},
            };
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String json = "{\n" +
                "  \"c0\": \"abc\",\n" +
                "  \"c1\": \"xy\",\n" +
                "  \"c2\": \"z\",\n" +
                "  \"c3\": true,\n" +
                "  \"c4\": false,\n" +
                "  \"c5\": 1,\n" +
                "  \"c6\": 10,\n" +
                "  \"c7\": 11,\n" +
                "  \"c8\": 12,\n" +
                "  \"c9\": 13,\n" +
                "  \"c10\": 14,\n" +
                "  \"c11\": 15E2,\n" +
                "  \"c12\": null\n" +
                "}";
            env.sendEventJson(json, "JsonEvent");
            assertEventFilled(env.listener("s0").assertOneGetNewAndReset());

            env.sendEventJson("{}", "JsonEvent");
            assertEventNull(env.listener("s0").assertOneGetNewAndReset());

            json = "{\n" +
                "  \"c0\": null,\n" +
                "  \"c1\": null,\n" +
                "  \"c2\": null,\n" +
                "  \"c3\": null,\n" +
                "  \"c4\": null,\n" +
                "  \"c5\": null,\n" +
                "  \"c6\": null,\n" +
                "  \"c7\": null,\n" +
                "  \"c8\": null,\n" +
                "  \"c9\": null,\n" +
                "  \"c10\": null,\n" +
                "  \"c11\": null,\n" +
                "  \"c12\": null\n" +
                "}";
            env.sendEventJson(json, "JsonEvent");
            assertEventNull(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertEventFilled(it.next());
            assertEventNull(it.next());
            assertEventNull(it.next());

            env.undeployAll();
        }

        private void assertEventNull(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{null, null, null, null, null, null, null});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{null, null, null, null, null, null});
        }

        private void assertEventFilled(EventBean event) {
            EPAssertionUtil.assertProps(event, FIELDS_ONE, new Object[]{"abc", 'x', 'z', true, false, Byte.parseByte("1"), (short) 10});
            EPAssertionUtil.assertProps(event, FIELDS_TWO, new Object[]{11, 12, 13L, 14D, 15E2f, null});
        }
    }

    private static class EventJsonTypingParseDynamicPropNumberFormat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select num1? as c0, num2? as c1, num3? as c2 from JsonEvent#keepall").addListener("s0");

            String json = "{ \"num1\": 42, \"num2\": 42.0, \"num3\": 4.2E+1}";
            env.sendEventJson(json, "JsonEvent");
            assertFill(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFill(it.next());

            env.undeployAll();
        }

        private void assertFill(EventBean event) {
            EPAssertionUtil.assertProps(event, "c0,c1,c2".split(","), new Object[]{42, 42d, 42d});
        }
    }

    private static class EventJsonTypingParseDynamicPropNestedArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select a_array? as c0 from JsonEvent#keepall").addListener("s0");
            String json;

            json = "{\n" +
                "  \"a_array\": [\n" +
                "    [1,2],\n" +
                "    [[3,4], 5]" +
                "  ]\n" +
                "}";
            env.sendEventJson(json, "JsonEvent");
            assertFillOne(env.listener("s0").assertOneGetNewAndReset());

            json = "{\n" +
                "  \"a_array\": [\n" +
                "    [6, [ [7,8], [9], []]]\n" +
                "  ]\n" +
                "}";
            env.sendEventJson(json, "JsonEvent");
            assertFillTwo(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFillOne(it.next());
            assertFillTwo(it.next());

            env.undeployAll();
        }

        private void assertFillTwo(EventBean event) {
            Object[] array6To9 = (Object[]) event.get("c0");
            assertEquals(1, array6To9.length);
            Object[] array6plus = (Object[]) array6To9[0];
            assertEquals("6", array6plus[0].toString());
            Object[] array7plus = (Object[]) array6plus[1];
            EPAssertionUtil.assertEqualsExactOrder((Object[]) array7plus[0], new Object[]{7, 8});
            EPAssertionUtil.assertEqualsExactOrder((Object[]) array7plus[1], new Object[]{9});
            EPAssertionUtil.assertEqualsExactOrder((Object[]) array7plus[2], new Object[]{});
        }

        private void assertFillOne(EventBean event) {
            Object[] array1To5 = (Object[]) event.get("c0");
            assertEquals(2, array1To5.length);
            Object[] array12 = (Object[]) array1To5[0];
            EPAssertionUtil.assertEqualsExactOrder(array12, new Object[]{1, 2});
            Object[] array345 = (Object[]) array1To5[1];
            EPAssertionUtil.assertEqualsExactOrder((Object[]) array345[0], new Object[]{3, 4});
            assertEquals(5, array345[1]);
        }
    }

    private static class EventJsonTypingParseDynamicPropMixedOjectArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select a_array? as c0 from JsonEvent#keepall").addListener("s0");

            String json = "{\n" +
                "  \"a_array\": [\n" +
                "    \"a\",\n" +
                "     1,\n" +
                "    {\n" +
                "      \"value\": \"def\"\n" +
                "    },\n" +
                "    false,\n" +
                "    null\n" +
                "  ]\n" +
                "}";
            env.sendEventJson(json, "JsonEvent");
            assertFilled(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(it.next());

            env.undeployAll();
        }

        private void assertFilled(EventBean event) {
            Object[] array = (Object[]) event.get("c0");
            assertEquals("a", array[0]);
            assertEquals(1, array[1]);
            Map<String, Object> nested = (Map<String, Object>) array[2];
            assertEquals("{value=def}", nested.toString());
            assertFalse((boolean) array[3]);
            assertNull(array[4]);
        }
    }

    private static class EventJsonTypingParseDynamicPropJsonTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select a_string? as c0, exists(a_string?) as c1," +
                "a_number? as c2, exists(a_number?) as c3," +
                "a_boolean? as c4, exists(a_boolean?) as c5," +
                "a_null? as c6, exists(a_null?) as c7," +
                "a_object? as c8, exists(a_object?) as c9, " +
                "a_array? as c10, exists(a_array?) as c11 " +
                " from JsonEvent#keepall").addListener("s0");
            for (EventPropertyDescriptor prop : env.statement("s0").getEventType().getPropertyDescriptors()) {
                assertEquals("c1,c3,c5,c7,c9,c11".contains(prop.getPropertyName()) ? Boolean.class : Object.class, prop.getPropertyType());
            }

            String json = "{\n" +
                "  \"a_string\": \"abc\",\n" +
                "  \"a_number\": 1,\n" +
                "  \"a_boolean\": true,\n" +
                "  \"a_null\": null,\n" +
                "  \"a_object\": {\n" +
                "    \"value\": \"def\"\n" +
                "  },\n" +
                "  \"a_array\": [\n" +
                "    \"a\",\n" +
                "    \"b\"\n" +
                "  ]\n" +
                "}";
            env.sendEventJson(json, "JsonEvent");
            assertFilled(env.listener("s0").assertOneGetNewAndReset());

            env.sendEventJson("{}", "JsonEvent");
            assertUnfilled(env.listener("s0").assertOneGetNewAndReset());

            env.sendEventJson("{\"a_boolean\": false}", "JsonEvent");
            assertSomeFilled(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(it.next());
            assertUnfilled(it.next());
            assertSomeFilled(it.next());

            env.undeployAll();
        }

        private void assertFilled(EventBean eventBean) {
            EPAssertionUtil.assertProps(eventBean, "c0,c1,c2,c3,c4,c5,c6,c7,c9,c11".split(","),
                new Object[]{"abc", true, 1, true, true, true, null, true, true, true});
            Map<String, Object> object = (Map<String, Object>) eventBean.get("c8");
            assertEquals("def", object.get("value"));
            Object[] array = (Object[]) eventBean.get("c10");
            assertEquals("a", array[0]);
        }

        private void assertUnfilled(EventBean eventBean) {
            EPAssertionUtil.assertProps(eventBean, "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11".split(","),
                new Object[]{null, false, null, false, null, false, null, false, null, false, null, false});
        }

        private void assertSomeFilled(EventBean eventBean) {
            EPAssertionUtil.assertProps(eventBean, "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11".split(","),
                new Object[]{null, false, null, false, false, true, null, false, null, false, null, false});
        }
    }

    private static void sendAssertColumn(RegressionEnvironment env, String json, Object c0) {
        env.sendEventJson(json, "JsonEvent");
        assertColumn(env.listener("s0").assertOneGetNewAndReset(), c0);
    }

    private static void assertColumn(EventBean event, Object c0) {
        EPAssertionUtil.assertProps(event, "c0".split(","), new Object[]{c0});
    }

    private static void sendAssert(RegressionEnvironment env, String json, Consumer<Object> assertion) {
        env.sendEventJson(json, "JsonEvent");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        justAssert(event, assertion);
    }

    private static void justAssert(EventBean event, Consumer<Object> assertion) {
        assertion.accept(event.get("c0"));
    }
}
