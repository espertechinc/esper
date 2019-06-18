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

import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.json.util.EventSenderJson;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class EventJsonDocSamples {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonDocSamplesCarLocUpdate());
        execs.add(new EventJsonDocSamplesBook());
        execs.add(new EventJsonDocSamplesCake());
        execs.add(new EventJsonDocDynamicEmpty());
        execs.add(new EventJsonDocApplicationClass());
        return execs;
    }

    private static class EventJsonDocApplicationClass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent(person " + MyLocalPersonEvent.class.getName() + ");\n" +
                "@name('s0') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            UUID uuid = UUID.randomUUID();
            JsonObject json = new JsonObject().add("person", new JsonObject().add("name", "Joe").add("id", uuid.toString()));
            env.sendEventJson(json.toString(), "JsonEvent");
            MyLocalPersonEvent person = (MyLocalPersonEvent) env.listener("s0").assertOneGetNewAndReset().get("person");

            assertEquals("Joe", person.name);
            assertEquals(uuid, person.id);

            env.undeployAll();
        }
    }

    private static class EventJsonDocDynamicEmpty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@JsonSchema(dynamic=true) @public @buseventtype create json schema SensorEvent();\n" +
                "@name('s0') select entityID? as entityId, temperature? as temperature, status? as status, \n" +
                "\tentityName? as entityName, vt? as vt, flags? as flags from SensorEvent;\n" +
                "@name('s1') select entityName?.english as englishEntityName from SensorEvent";
            env.compileDeploy(epl).addListener("s0").addListener("s1");

            String json = "{\n" +
                "   \"entityID\":\"cd9f930e\",\n" +
                "   \"temperature\" : 70,\n" +
                "   \"status\" : true,\n" +
                "   \"entityName\":{\n" +
                "      \"english\":\"Cooling Water Temperature\"\n" +
                "   },\n" +
                "   \"vt\":[\"2014-08-20T15:30:23.524Z\"],\n" +
                "   \"flags\" : null\n" +
                "}";
            env.sendEventJson(json, "SensorEvent");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "entityId,temperature,status,entityName,vt,flags".split(","),
                new Object[]{"cd9f930e", 70, true, Collections.singletonMap("english", "Cooling Water Temperature"), new Object[]{"2014-08-20T15:30:23.524Z"},
                    null});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "englishEntityName".split(","),
                new Object[]{"Cooling Water Temperature"});

            EventSenderJson sender = (EventSenderJson) env.runtime().getEventService().getEventSender("SensorEvent");
            sender.parse(json);

            env.undeployAll();
        }
    }

    private static class EventJsonDocSamplesCarLocUpdate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public @buseventtype create json schema CarLocUpdateEvent(carId string, direction int)", path);
            env.compileDeploy("@name('s0') select carId, direction, count(*) as cnt from CarLocUpdateEvent(direction = 1)#time(1 min)", path).addListener("s0");

            String event = "{" +
                "  \"carId\" : \"A123456\",\n" +
                "  \"direction\" : 1\n" +
                "}";
            env.sendEventJson(event, "CarLocUpdateEvent");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "carId,direction,cnt".split(","),
                new Object[]{"A123456", 1, 1L});

            env.undeployAll();
        }
    }

    private static class EventJsonDocSamplesBook implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create json schema Names(lastname string, firstname string);\n" +
                "@public @buseventtype create json schema BookEvent(isbn string, author Names, editor Names, title string, category string[]);\n" +
                "@name('s0') select isbn, author.lastname as authorName, editor.lastname as editorName, \n" +
                "  category[0] as primaryCategory from BookEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            String json = "{ \n" +
                "  \"isbn\": \"123-456-222\",  \n" +
                " \"author\": \n" +
                "    {\n" +
                "      \"lastname\": \"Doe\",\n" +
                "      \"firstname\": \"Jane\"\n" +
                "    },\n" +
                "\"editor\": \n" +
                "    {\n" +
                "      \"lastname\": \"Smith\",\n" +
                "      \"firstname\": \"Jane\"\n" +
                "    },\n" +
                "  \"title\": \"The Ultimate Database Study Guide\",  \n" +
                "  \"category\": [\"Non-Fiction\", \"Technology\"]\n" +
                " }";
            env.sendEventJson(json, "BookEvent");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "isbn,authorName,editorName,primaryCategory".split(","),
                new Object[]{"123-456-222", "Doe", "Smith", "Non-Fiction"});

            env.undeployAll();
        }
    }

    private static class EventJsonDocSamplesCake implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create json schema IdAndType(int string, type string);\n" +
                "create json schema Batters(machine string, batter IdAndType[]);\n" +
                "@public @buseventtype create json schema CakeEvent(id string, type string, name string, batters Batters, topping IdAndType[]);\n" +
                "@name('s0') select name, batters.batter[0].type as firstBatterType,\n" +
                "  topping[0].type as firstToppingType, batters.machine as batterMachine, batters.batter.countOf() as countBatters,\n" +
                "  topping.countOf() as countToppings from CakeEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            String json = "{\n" +
                "  \"id\": \"0001\",\n" +
                "  \"type\": \"donut\",\n" +
                "  \"name\": \"Cake\",\n" +
                "  \"batters\": \t\n" +
                "  {\n" +
                "    \"machine\": \"machine A\",\n" +
                "    \"batter\":\n" +
                "    [\n" +
                "      { \"id\": \"1001\", \"type\": \"Regular\" },\n" +
                "      { \"id\": \"1002\", \"type\": \"Chocolate\" },\n" +
                "      { \"id\": \"1003\", \"type\": \"Blueberry\" },\n" +
                "      { \"id\": \"1004\", \"type\": \"Devil's Food\" }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"topping\":\n" +
                "  [\n" +
                "    { \"id\": \"5001\", \"type\": \"None\" },\n" +
                "    { \"id\": \"5002\", \"type\": \"Glazed\" },\n" +
                "    { \"id\": \"5005\", \"type\": \"Sugar\" },\n" +
                "    { \"id\": \"5007\", \"type\": \"Powdered Sugar\" }\n" +
                "  ]\n" +
                "}";
            env.sendEventJson(json, "CakeEvent");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "name,firstBatterType,firstToppingType,batterMachine,countBatters,countToppings".split(","),
                new Object[]{"Cake", "Regular", "None", "machine A", 4, 4});

            env.undeployAll();
        }
    }

    public static class MyLocalPersonEvent {
        public String name;
        public UUID id;
    }
}
