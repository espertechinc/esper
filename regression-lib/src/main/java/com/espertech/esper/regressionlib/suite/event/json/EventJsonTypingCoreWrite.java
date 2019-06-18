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
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.regressionlib.support.json.SupportJsonEventTypeUtil.assertJsonWrite;

public class EventJsonTypingCoreWrite {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonTypingWriteBasicType());
        execs.add(new EventJsonTypingWriteBasicTypeArray());
        execs.add(new EventJsonTypingWriteBasicTypeArray2Dim());
        execs.add(new EventJsonTypingWriteEnumType());
        execs.add(new EventJsonTypingWriteBigDecimalBigInt());
        execs.add(new EventJsonTypingWriteObjectType());
        execs.add(new EventJsonTypingWriteObjectArrayType());
        execs.add(new EventJsonTypingWriteMapType());
        execs.add(new EventJsonTypingParseDynamicPropJsonTypes());
        execs.add(new EventJsonTypingWriteDynamicPropMixedOjectArray());
        execs.add(new EventJsonTypingWriteDynamicPropNestedArray());
        execs.add(new EventJsonTypingWriteDynamicPropNumberFormat());
        execs.add(new EventJsonTypingWriteNested());
        execs.add(new EventJsonTypingWriteNestedArray());
        return execs;
    }

    private static class EventJsonTypingWriteNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create json schema Book(bookId string, price BigDecimal);\n", path);
            env.compileDeploy("@public create json schema Shelf(shelfId string, book Book);\n", path);
            env.compileDeploy("@public create json schema Isle(isleId string, shelf Shelf);\n", path);
            env.compileDeploy("@public @buseventtype create json schema Library(libraryId string, isle Isle);\n", path);
            env.compileDeploy("@name('s0') select * from Library;\n", path).addListener("s0");
            String json;

            json = "{\n" +
                "  \"libraryId\": \"L\",\n" +
                "  \"isle\": {\n" +
                "    \"isleId\": \"I1\",\n" +
                "    \"shelf\": {\n" +
                "      \"shelfId\": \"S11\",\n" +
                "      \"book\": {\n" +
                "        \"bookId\": \"B111\",\n" +
                "        \"price\": 20\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
            sendAssertLibrary(env, json, "L", "I1", "S11", "B111");

            json = "{\n" +
                "  \"libraryId\": \"L\",\n" +
                "  \"isle\": null\n" +
                "}";
            sendAssertLibrary(env, json, "L", null, null, null);

            json = "{\n" +
                "  \"libraryId\": \"L\",\n" +
                "  \"isle\": {\n" +
                "    \"isleId\": \"I1\",\n" +
                "    \"shelf\": null\n" +
                "  }\n" +
                "}";
            sendAssertLibrary(env, json, "L", "I1", null, null);

            json = "{\n" +
                "  \"libraryId\": \"L\",\n" +
                "  \"isle\": {\n" +
                "    \"isleId\": \"I1\",\n" +
                "    \"shelf\": {\n" +
                "      \"shelfId\": \"S11\",\n" +
                "      \"book\": null\n" +
                "    }\n" +
                "  }\n" +
                "}";
            sendAssertLibrary(env, json, "L", "I1", "S11", null);

            env.undeployAll();
        }

        private void sendAssertLibrary(RegressionEnvironment env, String json, String libraryId, String isleId, String shelfId, String bookId) {
            env.sendEventJson(json, "Library");
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertJsonWrite(json, event);
            EPAssertionUtil.assertProps(event, "libraryId,isle.isleId,isle.shelf.shelfId,isle.shelf.book.bookId".split(","),
                new Object[]{libraryId, isleId, shelfId, bookId});
        }
    }

    private static class EventJsonTypingWriteNestedArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create json schema Book(bookId string, price BigDecimal);\n", path);
            env.compileDeploy("@public create json schema Shelf(shelfId string, books Book[]);\n", path);
            env.compileDeploy("@public create json schema Isle(isleId string, shelfs Shelf[]);\n", path);
            env.compileDeploy("@public @buseventtype create json schema Library(libraryId string, isles Isle[]);\n", path);
            env.compileDeploy("@name('s0') select * from Library#keepall;\n", path).addListener("s0");

            String jsonOne = "{\n" +
                "  \"libraryId\": \"L1\",\n" +
                "  \"isles\": [\n" +
                "    {\n" +
                "      \"isleId\": \"I1\",\n" +
                "      \"shelfs\": [\n" +
                "        {\n" +
                "          \"shelfId\": \"S1\",\n" +
                "          \"books\": [\n" +
                "            {\n" +
                "              \"bookId\": \"B1\",\n" +
                "              \"price\": 10\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
            env.sendEventJson(jsonOne, "Library");
            assertJsonWrite(jsonOne, env.listener("s0").assertOneGetNewAndReset());

            JsonObject book111 = buildBook("B111", 20);
            JsonObject shelf11 = buildShelf("S11", book111);
            JsonObject isle1 = buildIsle("I1", shelf11);
            JsonObject libraryOne = buildLibrary("L1", isle1);
            String jsonTwo = libraryOne.toString();
            env.sendEventJson(jsonTwo, "Library");
            assertJsonWrite(jsonTwo, env.listener("s0").assertOneGetNewAndReset());

            JsonObject book112 = buildBook("B112", 21);
            shelf11.get("books").asArray().add(book112);
            JsonObject shelf12 = buildShelf("S12", book111, book112);
            JsonObject isle2 = buildIsle("I2", shelf11, shelf12);
            JsonObject libraryTwo = buildLibrary("L", isle1, isle2);
            String jsonThree = libraryTwo.toString();
            env.sendEventJson(jsonThree, "Library");
            assertJsonWrite(jsonThree, env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(jsonOne, it.next());
            assertJsonWrite(jsonTwo, it.next());
            assertJsonWrite(jsonThree, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteBasicType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (" +
                "c0 string, c1 char, c2 character, c3 bool, c4 boolean, " +
                "c5 byte, c6 short, c7 int, c8 integer, c9 long, c10 double, c11 float, c12 null);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            String jsonOne = "{\n" +
                "  \"c0\": \"abc\",\n" +
                "  \"c1\": \"x\",\n" +
                "  \"c2\": \"z\",\n" +
                "  \"c3\": true,\n" +
                "  \"c4\": false,\n" +
                "  \"c5\": 1,\n" +
                "  \"c6\": 10,\n" +
                "  \"c7\": 11,\n" +
                "  \"c8\": 12,\n" +
                "  \"c9\": 13,\n" +
                "  \"c10\": 14.0,\n" +
                "  \"c11\": 1500.0,\n" +
                "  \"c12\": null\n" +
                "}";
            assertJsonWrite(jsonOne, sendGet(env, jsonOne));

            String jsonTwo = "{\n" +
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
            assertJsonWrite(jsonTwo, sendGet(env, jsonTwo));

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(jsonOne, it.next());
            assertJsonWrite(jsonTwo, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteBasicTypeArray implements RegressionExecution {
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

            String jsonOne = "{ \"c0\": [\"abc\", \"def\"],\n" +
                "\"c1\": [\"x\", \"z\"],\n" +
                "\"c2\": [\"x\", \"y\"],\n" +
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
                "\"c13\": [50.0, 51.0],\n" +
                "\"c14\": [52.0, 53.0],\n" +
                "\"c15\": [60.0, 61.0],\n" +
                "\"c16\": [62.0, 63.0]" +
                "}\n";
            assertJsonWrite(jsonOne, sendGet(env, jsonOne));

            String jsonTwo = "{ \"c0\": [],\n" +
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
            assertJsonWrite(jsonTwo, sendGet(env, jsonTwo));

            String jsonThree = "{ \"c0\": null,\n" +
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
            assertJsonWrite(jsonThree, sendGet(env, jsonThree));

            String jsonFour = "{ \"c0\": [null, \"def\", null],\n" +
                "\"c1\": [\"x\", null],\n" +
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
                "\"c13\": [null, null, 51.0],\n" +
                "\"c14\": [52.0],\n" +
                "\"c15\": [null],\n" +
                "\"c16\": [63.0]" +
                "}\n";
            assertJsonWrite(jsonFour, sendGet(env, jsonFour));

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(jsonOne, it.next());
            assertJsonWrite(jsonTwo, it.next());
            assertJsonWrite(jsonThree, it.next());
            assertJsonWrite(jsonFour, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteBasicTypeArray2Dim implements RegressionExecution {
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

            String jsonOne = "{ \"c0\": [[\"a\", \"b\"],[\"c\"]],\n" +
                "\"c1\": [[\"x\", \"z\"],[\"n\"]],\n" +
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
                "\"c13\": [[50.0], [51.0, 52.0], [53.0]],\n" +
                "\"c14\": [[54.0], [55.0, 56.0]],\n" +
                "\"c15\": [[60.0, 61.0], []],\n" +
                "\"c16\": [[62.0], [63.0]]" +
                "}\n";
            assertJsonWrite(jsonOne, sendGet(env, jsonOne));

            String jsonTwo = "{ \"c0\": [],\n" +
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
            assertJsonWrite(jsonTwo, sendGet(env, jsonTwo));

            String jsonThree = "{ \"c0\": null,\n" +
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
            assertJsonWrite(jsonThree, sendGet(env, jsonThree));

            String jsonFour = "{ \"c0\": [[null, \"a\"]],\n" +
                "\"c1\": [[null], [\"x\"]],\n" +
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
                "\"c16\": [[63.0]]" +
                "}\n";
            assertJsonWrite(jsonFour, sendGet(env, jsonFour));

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(jsonOne, it.next());
            assertJsonWrite(jsonTwo, it.next());
            assertJsonWrite(jsonThree, it.next());
            assertJsonWrite(jsonFour, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteEnumType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 SupportEnum, c1 SupportEnum[], c2 SupportEnum[][]);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            String jsonOne = "{\"c0\": \"ENUM_VALUE_2\", \"c1\": [\"ENUM_VALUE_2\", \"ENUM_VALUE_1\"], \"c2\": [[\"ENUM_VALUE_2\"], [\"ENUM_VALUE_1\", \"ENUM_VALUE_3\"]]}";
            assertJsonWrite(jsonOne, sendGet(env, jsonOne));

            String jsonTwo = "{\"c0\": null, \"c1\": null, \"c2\": null}";
            assertJsonWrite(jsonTwo, sendGet(env, jsonTwo));

            String jsonThree = "{\"c0\": null, \"c1\": [], \"c2\": [[]]}";
            assertJsonWrite(jsonThree, sendGet(env, jsonThree));

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(jsonOne, it.next());
            assertJsonWrite(jsonTwo, it.next());
            assertJsonWrite(jsonThree, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteBigDecimalBigInt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 BigInteger, c1 BigDecimal," +
                "c2 BigInteger[], c3 BigDecimal[], c4 BigInteger[][], c5 BigDecimal[][]);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            String jsonOne = "{\"c0\": 123456789123456789123456789, \"c1\": 123456789123456789123456789.1," +
                "\"c2\": [123456789123456789123456789], \"c3\": [123456789123456789123456789.1]," +
                "\"c4\": [[123456789123456789123456789]], \"c5\": [[123456789123456789123456789.1]]" +
                "}";
            assertJsonWrite(jsonOne, sendGet(env, jsonOne));

            String jsonTwo = "{\"c0\": null, \"c1\": null, \"c2\": null, \"c3\": null, \"c4\": null, \"c5\": null}";
            assertJsonWrite(jsonTwo, sendGet(env, jsonTwo));

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(jsonOne, it.next());
            assertJsonWrite(jsonTwo, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteObjectType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 Object);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{{"c0", Object.class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String[] jsons = new String[]{
                "{\"c0\": 1}",
                "{\"c0\": 1.0}",
                "{\"c0\": null}",
                "{\"c0\": true}",
                "{\"c0\": false}",
                "{\"c0\": \"abc\"}",
                "{\"c0\": [\"abc\"]}",
                "{\"c0\": []}",
                "{\"c0\": [\"abc\", 2]}",
                "{\"c0\": [[\"abc\"], [5.0]]}",
                "{\"c0\": {\"c1\": 10}}",
                "{\"c0\": {\"c1\": 10, \"c2\": \"abc\"}}",
            };
            for (String json : jsons) {
                sendAssert(env, json);
            }

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            for (String json : jsons) {
                assertJsonWrite(json, it.next());
            }

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteObjectArrayType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 Object[]);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{{"c0", Object[].class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String[] jsons = new String[]{
                "{\"c0\": []}",
                "{\"c0\": [1.0]}",
                "{\"c0\": [null]}",
                "{\"c0\": [true]}",
                "{\"c0\": [false]}",
                "{\"c0\": [\"abc\"]}",
                "{\"c0\": [[\"abc\"]]}",
                "{\"c0\": [[]]}",
                "{\"c0\": [[\"abc\", 2]]}",
                "{\"c0\": [[[\"abc\"], [5.0]]]}",
                "{\"c0\": [{\"c1\": 10}]}",
                "{\"c0\": [{\"c1\": 10, \"c2\": \"abc\"}]}",
            };
            for (String json : jsons) {
                sendAssert(env, json);
            }

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            for (String json : jsons) {
                assertJsonWrite(json, it.next());
            }

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteMapType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (c0 Map);\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");
            Object[][] namesAndTypes = new Object[][]{{"c0", Map.class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(namesAndTypes, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String[] jsons = new String[]{
                "{\"c0\": {\"c1\" : 10}}",
                "{\"c0\": {\"c1\": [\"c2\", 20]}}",
            };
            for (String json : jsons) {
                sendAssert(env, json);
            }

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            for (String json : jsons) {
                assertJsonWrite(json, it.next());
            }

            env.undeployAll();
        }
    }

    private static class EventJsonTypingParseDynamicPropJsonTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select * from JsonEvent#keepall").addListener("s0");

            String jsonOne = "{\n" +
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
            sendAssert(env, jsonOne);

            String jsonTwo = "{}";
            sendAssert(env, jsonTwo);

            String jsonThree = "{\"a_boolean\": false}";
            sendAssert(env, jsonThree);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(jsonOne, it.next());
            assertJsonWrite(jsonTwo, it.next());
            assertJsonWrite(jsonThree, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteDynamicPropMixedOjectArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select * from JsonEvent#keepall").addListener("s0");

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
            sendAssert(env, json);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(json, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteDynamicPropNestedArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select * from JsonEvent#keepall").addListener("s0");

            String jsonOne = "{\n" +
                "  \"a_array\": [\n" +
                "    [1,2],\n" +
                "    [[3,4], 5]" +
                "  ]\n" +
                "}";
            sendAssert(env, jsonOne);

            String jsonTwo = "{\n" +
                "  \"a_array\": [\n" +
                "    [6, [ [7,8], [9], []]]\n" +
                "  ]\n" +
                "}";
            sendAssert(env, jsonTwo);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(jsonOne, it.next());
            assertJsonWrite(jsonTwo, it.next());

            env.undeployAll();
        }
    }

    private static class EventJsonTypingWriteDynamicPropNumberFormat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@JsonSchema(dynamic=true) @public @buseventtype create json schema JsonEvent();\n" +
                "@name('s0') select * from JsonEvent#keepall").addListener("s0");

            String json = "{ \"num1\": 42, \"num2\": 42.0, \"num3\": 43.0}";
            sendAssert(env, json);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertJsonWrite(json, it.next());

            env.undeployAll();
        }
    }

    private static void sendAssert(RegressionEnvironment env, String json) {
        assertJsonWrite(json, sendGet(env, json));
    }

    private static EventBean sendGet(RegressionEnvironment env, String json) {
        env.sendEventJson(json, "JsonEvent");
        return env.listener("s0").assertOneGetNewAndReset();
    }

    private static JsonObject buildBook(String bookId, int price) {
        JsonObject book = new JsonObject();
        book.add("bookId", bookId);
        book.add("price", price);
        return book;
    }

    private static JsonObject buildShelf(String shelfId, JsonObject... books) {
        JsonObject shelf = new JsonObject();
        shelf.add("shelfId", shelfId);
        shelf.add("books", arrayOfObjects(books));
        return shelf;
    }

    private static JsonObject buildIsle(String isleId, JsonObject... shelfs) {
        JsonObject shelf = new JsonObject();
        shelf.add("isleId", isleId);
        shelf.add("shelfs", arrayOfObjects(shelfs));
        return shelf;
    }

    private static JsonObject buildLibrary(String libraryId, JsonObject... isles) {
        JsonObject shelf = new JsonObject();
        shelf.add("libraryId", libraryId);
        shelf.add("isles", arrayOfObjects(isles));
        return shelf;
    }

    private static JsonArray arrayOfObjects(JsonObject[] objects) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < objects.length; i++) {
            array.add(objects[i]);
        }
        return array;
    }
}
