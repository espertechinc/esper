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
import com.espertech.esper.common.client.json.util.EventSenderJson;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.client.render.JSONEventRenderer;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.json.SupportClientsEvent;
import com.espertech.esper.regressionlib.support.json.SupportUsersEvent;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;

public class EventJsonProvidedUnderlyingClass {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonProvidedClassUsersEvent());
        execs.add(new EventJsonProvidedClassUsersEventWithCreateSchema());
        execs.add(new EventJsonProvidedClassClientsEvent());
        execs.add(new EventJsonProvidedClassClientsEventWithCreateSchema());
        execs.add(new EventJsonProvidedClassInvalid());
        execs.add(new EventJsonProvidedClassFieldTypeMismatchInvalid());
        execs.add(new EventJsonProvidedClassCreateSchemaTypeMismatchInvalid());
        execs.add(new EventJsonProvidedClassSetNullForPrimitive());
        execs.add(new EventJsonProvidedClassWArrayPatternInsert());
        return execs;
    }

    private static class EventJsonProvidedClassWArrayPatternInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl =
                "@public @buseventtype @JsonSchema(className='" + MyLocalJsonProvidedEventOne.class.getName() + "') create json schema EventOne();\n" +
                "@public @buseventtype @JsonSchema(className='" + MyLocalJsonProvidedEventTwo.class.getName() + "') create json schema EventTwo();\n" +
                "@public @buseventtype @JsonSchema(className='" + MyLocalJsonProvidedEventOut.class.getName() + "') create json schema EventOut();\n" +
                "@name('s0') insert into EventOut select s as startEvent, e as endEvents from pattern [" +
                    "every s=EventOne -> e=EventTwo(id=s.id) until timer:interval(10 sec)]";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventJson("{\"id\":\"G1\"}", "EventOne");
            env.sendEventJson("{\"id\":\"G1\",\"val\":2}", "EventTwo");
            env.sendEventJson("{\"id\":\"G1\",\"val\":3}", "EventTwo");
            env.advanceTime(10000);

            MyLocalJsonProvidedEventOut out = (MyLocalJsonProvidedEventOut) env.listener("s0").assertOneGetNewAndReset().getUnderlying();
            assertEquals("G1", out.startEvent.id);
            assertEquals("G1", out.endEvents[0].id);
            assertEquals(2, out.endEvents[0].val);
            assertEquals("G1", out.endEvents[1].id);
            assertEquals(3, out.endEvents[1].val);

            env.undeployAll();
        }
    }

    private static class EventJsonProvidedClassSetNullForPrimitive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype @JsonSchema(className='" + MyLocalJsonProvidedPrimitiveInt.class.getName() + "') create json schema MySchema();\n" +
                "insert into MySchema select intBoxed as primitiveInt from SupportBean;\n" +
                "@name('s0') select * from MySchema;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            assertEquals(-1, env.listener("s0").assertOneGetNewAndReset().get("primitiveInt"));

            env.undeployAll();
        }
    }

    private static class EventJsonProvidedClassCreateSchemaTypeMismatchInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String prefix = "@JsonSchema(className='" + MyLocalJsonProvidedStringInt.class.getName() + "') ";
            String epl = prefix + "create json schema MySchema(c0 int)";
            tryInvalidSchema(env, epl, MyLocalJsonProvidedStringInt.class,
                "Public field 'c0' of class '%CLASS%' declared as type 'java.lang.String' cannot receive a value of type 'java.lang.Integer'");
        }
    }

    private static class EventJsonProvidedClassFieldTypeMismatchInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String prefix = EventRepresentationChoice.JSONCLASSPROVIDED.getAnnotationTextWJsonProvided(MyLocalJsonProvidedStringInt.class);
            tryInvalidSchema(env, prefix + "select 0 as dummy from SupportBean", MyLocalJsonProvidedStringInt.class,
                "Failed to find public field 'dummy' on class '%CLASS%'");
            tryInvalidSchema(env, prefix + "select 0 as c0 from SupportBean", MyLocalJsonProvidedStringInt.class,
                "Public field 'c0' of class '%CLASS%' declared as type 'java.lang.String' cannot receive a value of type 'java.lang.Integer'");
            tryInvalidSchema(env, prefix + "select new {a=0} as c0 from SupportBean", MyLocalJsonProvidedStringInt.class,
                "Public field 'c0' of class '%CLASS%' declared as type 'java.lang.String' cannot receive a value of type 'java.util.Map'");
        }
    }

    private static class EventJsonProvidedClassInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "@JsonSchema(dynamic=true, className='" + SupportClientsEvent.class.getName() + "') create json schema Clients()";
            tryInvalidCompile(env, epl,
                "The dynamic flag is not supported when used with a provided JSON event class");

            epl = "create json schema ABC();\n" +
                "@JsonSchema(className='" + SupportClientsEvent.class.getName() + "') create json schema Clients() inherits ABC";
            tryInvalidCompile(env, epl,
                "Specifying a supertype is not supported with a provided JSON event class");

            epl = "@JsonSchema(className='" + MyLocalNonPublicInvalid.class.getName() + "') create json schema Clients()";
            tryInvalidCompile(env, epl,
                "Provided JSON event class is not public");

            epl = "@JsonSchema(className='" + MyLocalNoDefaultCtorInvalid.class.getName() + "') create json schema Clients()";
            tryInvalidCompile(env, epl,
                "Provided JSON event class does not have a public default constructor or is a non-static inner class");

            epl = "@JsonSchema(className='" + MyLocalInstanceInvalid.class.getName() + "') create json schema Clients()";
            tryInvalidCompile(env, epl,
                "Provided JSON event class does not have a public default constructor or is a non-static inner class");
        }
    }

    private static class EventJsonProvidedClassClientsEventWithCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schema =
                "create json schema Partner(id long, name string, since java.time.OffsetDateTime);\n" +
                    "create json schema Client(" +
                    "_id long,\n" +
                    "`index` int,\n" +
                    "guid java.util.UUID,\n" +
                    "isActive boolean,\n" +
                    "balance BigDecimal,\n" +
                    "picture string,\n" +
                    "age int,\n" +
                    "eyeColor " + SupportClientsEvent.EyeColor.class.getName() + ",\n" +
                    "name string,\n" +
                    "gender string,\n" +
                    "company string,\n" +
                    "emails string[],\n" +
                    "phones long[],\n" +
                    "address string,\n" +
                    "about string,\n" +
                    "registered java.time.LocalDate,\n" +
                    "latitude double,\n" +
                    "longitude double,\n" +
                    "tags string[],\n" +
                    "partners Partner[]\n" +
                    ");\n" +
                    "@public @buseventtype create json schema Clients(clients Client[]);\n" +
                    "@name('s0') select * from Clients;\n";
            env.compileDeploy(schema).addListener("s0");

            env.sendEventJson(getClientsJson(), "Clients");

            JsonEventObject event = (JsonEventObject) env.listener("s0").assertOneGetNewAndReset().getUnderlying();
            assertEquals(getClientsJsonReplaceWhitespace(), event.toString());

            env.undeployAll();
        }
    }

    private static class EventJsonProvidedClassUsersEventWithCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schema =
                "create json schema Friend(id string, name string);\n" +
                    "create json schema User(" +
                    "_id string,\n" +
                    "`index` int,\n" +
                    "guid string,\n" +
                    "isActive boolean,\n" +
                    "balance string,\n" +
                    "picture string,\n" +
                    "age int,\n" +
                    "eyeColor string,\n" +
                    "name string,\n" +
                    "gender string,\n" +
                    "company string,\n" +
                    "email string,\n" +
                    "phone string,\n" +
                    "address string,\n" +
                    "about string,\n" +
                    "registered string,\n" +
                    "latitude double,\n" +
                    "longitude double,\n" +
                    "tags string[],\n" +
                    "friends Friend[],\n" +
                    "greeting string,\n" +
                    "favoriteFruit string\n" +
                    ");\n" +
                    "@public @buseventtype create json schema Users(users User[]);\n" +
                    "@name('s0') select * from Users;\n";
            env.compileDeploy(schema).addListener("s0");

            env.sendEventJson(getUsersJson(), "Users");

            JsonEventObject event = (JsonEventObject) env.listener("s0").assertOneGetNewAndReset().getUnderlying();
            assertEquals(getUsersJsonReplaceWhitespace(), event.toString());

            env.undeployAll();
        }
    }

    private static class EventJsonProvidedClassClientsEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype @JsonSchema(className='" + SupportClientsEvent.class.getName() + "') create json schema Clients();\n" +
                "@name('s0') select * from Clients;";
            env.compileDeploy(epl).addListener("s0");

            // try sender parse-only
            EventSenderJson sender = (EventSenderJson) env.runtime().getEventService().getEventSender("Clients");
            SupportClientsEvent clients = (SupportClientsEvent) sender.parse(getClientsJson());
            assertClientsPremade(clients);

            // try send-event
            sender.sendEvent(getClientsJson());
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertClientsPremade((SupportClientsEvent) event.getUnderlying());

            // try write
            JSONEventRenderer render = env.runtime().getRenderEventService().getJSONRenderer(event.getEventType());
            assertEquals(getClientsJsonReplaceWhitespace(), render.render(event));

            env.undeployAll();
        }

        private void assertClientsPremade(SupportClientsEvent clients) {
            assertEquals(1, clients.clients.size());
            SupportClientsEvent.Client first = clients.clients.get(0);
            assertEquals(getClientObject().clients.get(0), first);
        }
    }

    private static class EventJsonProvidedClassUsersEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype @JsonSchema(className='" + SupportUsersEvent.class.getName() + "') create json schema Users();\n" +
                "@name('s0') select * from Users;";
            env.compileDeploy(epl).addListener("s0");

            // try sender parse-only
            EventSenderJson sender = (EventSenderJson) env.runtime().getEventService().getEventSender("Users");
            SupportUsersEvent users = (SupportUsersEvent) sender.parse(getUsersJson());
            assertUsersPremade(users);

            // try send-event
            sender.sendEvent(getUsersJson());
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertUsersPremade((SupportUsersEvent) event.getUnderlying());

            // try write
            JSONEventRenderer render = env.runtime().getRenderEventService().getJSONRenderer(event.getEventType());
            assertEquals(getUsersJsonReplaceWhitespace(), render.render(event));

            env.undeployAll();
        }

        private void assertUsersPremade(SupportUsersEvent users) {
            assertEquals(2, users.users.size());
            SupportUsersEvent.User first = users.users.get(0);
            assertEquals("45166552176594981065", first._id);
            SupportUsersEvent.User second = users.users.get(1);
            assertEquals("23504426278646846580", second._id);
            EPAssertionUtil.assertEqualsExactOrder(getUserObject().users.toArray(), users.users.toArray());
        }
    }

    private static SupportUsersEvent getUserObject() {
        SupportUsersEvent.Friend u0f0 = new SupportUsersEvent.Friend();
        u0f0.id = "3987";
        u0f0.name = "dWwKYheGgTZejIMYdglXvvrWAzUqsk";
        SupportUsersEvent.Friend u0f1 = new SupportUsersEvent.Friend();
        u0f1.id = "4673";
        u0f1.name = "EqVIiZyuhSCkWXvqSxgyQihZaiwSra";

        SupportUsersEvent.User u0 = new SupportUsersEvent.User();
        u0._id = "45166552176594981065";
        u0.index = 692815193;
        u0.guid = "oLzFhQttjjCGmijYulZg";
        u0.isActive = true;
        u0.balance = "XtMtTkSfmQtyRHS1086c";
        u0.picture = "i0wzskJJ2SvxXL1UbXEzy332JksricBvitJkeKt3JcoZGx10JxhbdkQ8YoyJ0cL1MGFwC9bpAzQXSFBEcAUQ8lGQekvJZDeJ5C5p";
        u0.age = 23;
        u0.eyeColor = "XqoN9IzOBVixZhrofJpd";
        u0.name = "xBavaMCv6j0eYkT6HMcB";
        u0.gender = "VnuP3BaA3flaA6dLGvqO";
        u0.company = "L9yT2IsGTjOgQc0prb4r";
        u0.email = "rfmlFaVxGBSZFybTIKz0";
        u0.phone = "vZsxzv8DlzimJauTSBre";
        u0.address = "fZgFDv9tX1oonnVjcNVv";
        u0.about = "WysqSAN1psGsJBCFSR7P";
        u0.registered = "Lsw4RK5gtyNWGYp9dDhy";
        u0.latitude = 2.6395313895198393;
        u0.longitude = 110.5363758848371;
        u0.tags = Arrays.asList("Hx6qJTHe8y", "23vYh8ILj6", "geU64sSQgH", "ezNI8Gx5vq");
        u0.friends = Arrays.asList(u0f0, u0f1);
        u0.greeting = "xfS8vUXYq4wzufBLP6CY";
        u0.favoriteFruit = "KT0tVAxXRawtbeQIWAot";

        SupportUsersEvent.User u1 = new SupportUsersEvent.User();
        u1._id = "23504426278646846580";
        u1.index = 675066974;
        u1.guid = "MfiCc1n1WfG6d6iXcdNf";
        u1.isActive = true;
        u1.balance = "OQEwTOBvwK0b8dJYFpBU";
        u1.picture = "avtMGQxSrO1h86V7KVaKaWOWohfCnENnMfKcLbydRSMq2eHc533hC4n7GMwsGhXz10EyVBhnP1LUFZ0ooZd9GmIynRomjCjP8tEN";
        u1.age = 33;
        u1.eyeColor = "Fjsm1nmwyphAw7DRnfZ7";
        u1.name = "NnjrrCj1TTObhT9gHMH2";
        u1.gender = "ISVVoyQ4cbEjQVoFy5z0";
        u1.company = "AfcGdkzUQMzg69yjvmL5";
        u1.email = "mXLtlNEJjw5heFiYykwV";
        u1.phone = "zXbn9iJ5ljRHForNOa79";
        u1.address = "XXQUcaDIX2qpyZKtw8zl";
        u1.about = "GBVYHdxZYgGCey6yogEi";
        u1.registered = "bTJynDeyvZRbsYQIW9ys";
        u1.latitude = 16.675958191062414;
        u1.longitude = 114.20858157883556;
        u1.tags = Collections.emptyList();
        u1.friends = Collections.emptyList();
        u1.greeting = "EQqKZyiGnlyHeZf9ojnl";
        u1.favoriteFruit = "9aUx0u6G840i0EeKFM4Z";

        SupportUsersEvent event = new SupportUsersEvent();
        event.users = Arrays.asList(u0, u1);
        return event;
    }

    private static String getUsersJsonReplaceWhitespace() {
        return getUsersJson().replaceAll("\n", "").replaceAll(" ", "");
    }

    private static String getClientsJsonReplaceWhitespace() {
        return getClientsJson().replaceAll("\n", "").replaceAll(" ", "");
    }

    private static void tryInvalidSchema(RegressionEnvironment env, String epl, Class provided, String message) {
        tryInvalidCompile(env, epl, message.replace("%CLASS%", provided.getName()));
    }

    private static String getUsersJson() {
        return "{\n" +
            "  \"users\": [\n" +
            "    {\n" +
            "      \"_id\": \"45166552176594981065\",\n" +
            "      \"index\": 692815193,\n" +
            "      \"guid\": \"oLzFhQttjjCGmijYulZg\",\n" +
            "      \"isActive\": true,\n" +
            "      \"balance\": \"XtMtTkSfmQtyRHS1086c\",\n" +
            "      \"picture\": \"i0wzskJJ2SvxXL1UbXEzy332JksricBvitJkeKt3JcoZGx10JxhbdkQ8YoyJ0cL1MGFwC9bpAzQXSFBEcAUQ8lGQekvJZDeJ5C5p\",\n" +
            "      \"age\": 23,\n" +
            "      \"eyeColor\": \"XqoN9IzOBVixZhrofJpd\",\n" +
            "      \"name\": \"xBavaMCv6j0eYkT6HMcB\",\n" +
            "      \"gender\": \"VnuP3BaA3flaA6dLGvqO\",\n" +
            "      \"company\": \"L9yT2IsGTjOgQc0prb4r\",\n" +
            "      \"email\": \"rfmlFaVxGBSZFybTIKz0\",\n" +
            "      \"phone\": \"vZsxzv8DlzimJauTSBre\",\n" +
            "      \"address\": \"fZgFDv9tX1oonnVjcNVv\",\n" +
            "      \"about\": \"WysqSAN1psGsJBCFSR7P\",\n" +
            "      \"registered\": \"Lsw4RK5gtyNWGYp9dDhy\",\n" +
            "      \"latitude\": 2.6395313895198393,\n" +
            "      \"longitude\": 110.5363758848371,\n" +
            "      \"tags\": [\n" +
            "        \"Hx6qJTHe8y\",\n" +
            "        \"23vYh8ILj6\",\n" +
            "        \"geU64sSQgH\",\n" +
            "        \"ezNI8Gx5vq\"\n" +
            "      ],\n" +
            "      \"friends\": [\n" +
            "        {\n" +
            "          \"id\": \"3987\",\n" +
            "          \"name\": \"dWwKYheGgTZejIMYdglXvvrWAzUqsk\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"id\": \"4673\",\n" +
            "          \"name\": \"EqVIiZyuhSCkWXvqSxgyQihZaiwSra\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"greeting\": \"xfS8vUXYq4wzufBLP6CY\",\n" +
            "      \"favoriteFruit\": \"KT0tVAxXRawtbeQIWAot\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"_id\": \"23504426278646846580\",\n" +
            "      \"index\": 675066974,\n" +
            "      \"guid\": \"MfiCc1n1WfG6d6iXcdNf\",\n" +
            "      \"isActive\": true,\n" +
            "      \"balance\": \"OQEwTOBvwK0b8dJYFpBU\",\n" +
            "      \"picture\": \"avtMGQxSrO1h86V7KVaKaWOWohfCnENnMfKcLbydRSMq2eHc533hC4n7GMwsGhXz10EyVBhnP1LUFZ0ooZd9GmIynRomjCjP8tEN\",\n" +
            "      \"age\": 33,\n" +
            "      \"eyeColor\": \"Fjsm1nmwyphAw7DRnfZ7\",\n" +
            "      \"name\": \"NnjrrCj1TTObhT9gHMH2\",\n" +
            "      \"gender\": \"ISVVoyQ4cbEjQVoFy5z0\",\n" +
            "      \"company\": \"AfcGdkzUQMzg69yjvmL5\",\n" +
            "      \"email\": \"mXLtlNEJjw5heFiYykwV\",\n" +
            "      \"phone\": \"zXbn9iJ5ljRHForNOa79\",\n" +
            "      \"address\": \"XXQUcaDIX2qpyZKtw8zl\",\n" +
            "      \"about\": \"GBVYHdxZYgGCey6yogEi\",\n" +
            "      \"registered\": \"bTJynDeyvZRbsYQIW9ys\",\n" +
            "      \"latitude\": 16.675958191062414,\n" +
            "      \"longitude\": 114.20858157883556,\n" +
            "      \"tags\": [],\n" +
            "      \"friends\": [],\n" +
            "      \"greeting\": \"EQqKZyiGnlyHeZf9ojnl\",\n" +
            "      \"favoriteFruit\": \"9aUx0u6G840i0EeKFM4Z\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private static String getClientsJson() {
        return "{\n" +
            "  \"clients\": [\n" +
            "    {\n" +
            "      \"_id\": 4063715686146184700,\n" +
            "      \"index\": 1951037102,\n" +
            "      \"guid\": \"b7dc7f66-4f6d-4f03-14d7-83da210dfba6\",\n" +
            "      \"isActive\": true,\n" +
            "      \"balance\": 0.8509300187678505,\n" +
            "      \"picture\": \"TB6izKKNN5ihBLFiRekRmcntxaVAke1rL7rhUDQPACG4DxrLCvOfKNjy5KZl9Rg0QUknq7RFifVbZg4RbnVjdEThMdD1UAZQk3Le\",\n" +
            "      \"age\": 9,\n" +
            "      \"eyeColor\": \"BROWN\",\n" +
            "      \"name\": \"PTgbx3rVSkXaSVlKV2SK\",\n" +
            "      \"gender\": \"D7TzVALMRaVmCEkC8bzT\",\n" +
            "      \"company\": \"m4dcMP9VIFlniImW4Ezc\",\n" +
            "      \"emails\": [\n" +
            "        \"puYIMDORrusZXRUZjMQM\",\n" +
            "        \"vxMKjpYtPjJPRvDYuCjZ\"\n" +
            "      ],\n" +
            "      \"phones\": [\n" +
            "        1206223281\n" +
            "      ],\n" +
            "      \"address\": \"Hf2YGJnogcwkIwj5hTJz\",\n" +
            "      \"about\": \"d0FcpUETNRV2ky15EmBc\",\n" +
            "      \"registered\": \"1961-10-09\",\n" +
            "      \"latitude\": 26.91225115361936,\n" +
            "      \"longitude\": 74.26256260138875,\n" +
            "      \"tags\": [],\n" +
            "      \"partners\": [\n" +
            "        {\n" +
            "          \"id\": -4413101314901277000,\n" +
            "          \"name\": \"YjiSvZzaXYhJMkZddxlVPdHfoIthbY\",\n" +
            "          \"since\": \"1974-11-01T07:58:27.373380998Z\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"id\": -7309654308880836000,\n" +
            "          \"name\": \"HxHDrtpnXAxCooxasYVLZLqYImRLzW\",\n" +
            "          \"since\": \"1927-02-02T14:34:09.672667878Z\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private static SupportClientsEvent getClientObject() {
        SupportClientsEvent.Client client = new SupportClientsEvent.Client();
        client._id = 4063715686146184700L;
        client.index = 1951037102;
        client.guid = UUID.fromString("b7dc7f66-4f6d-4f03-14d7-83da210dfba6");
        client.isActive = true;
        client.balance = new BigDecimal("0.8509300187678505");
        client.picture = "TB6izKKNN5ihBLFiRekRmcntxaVAke1rL7rhUDQPACG4DxrLCvOfKNjy5KZl9Rg0QUknq7RFifVbZg4RbnVjdEThMdD1UAZQk3Le";
        client.age = 9;
        client.eyeColor = SupportClientsEvent.EyeColor.BROWN;
        client.name = "PTgbx3rVSkXaSVlKV2SK";
        client.gender = "D7TzVALMRaVmCEkC8bzT";
        client.company = "m4dcMP9VIFlniImW4Ezc";
        client.emails = new String[]{"puYIMDORrusZXRUZjMQM", "vxMKjpYtPjJPRvDYuCjZ"};
        client.phones = new long[]{1206223281};
        client.address = "Hf2YGJnogcwkIwj5hTJz";
        client.about = "d0FcpUETNRV2ky15EmBc";
        client.registered = LocalDate.parse("1961-10-09");
        client.latitude = 26.91225115361936;
        client.longitude = 74.26256260138875;
        client.tags = Collections.emptyList();

        SupportClientsEvent.Partner partnerOne = new SupportClientsEvent.Partner();
        partnerOne.id = -4413101314901277000L;
        partnerOne.name = "YjiSvZzaXYhJMkZddxlVPdHfoIthbY";
        partnerOne.since = OffsetDateTime.parse("1974-11-01T07:58:27.373380998Z");
        SupportClientsEvent.Partner partnerTwo = new SupportClientsEvent.Partner();
        partnerTwo.id = -7309654308880836000L;
        partnerTwo.name = "HxHDrtpnXAxCooxasYVLZLqYImRLzW";
        partnerTwo.since = OffsetDateTime.parse("1927-02-02T14:34:09.672667878Z");
        client.partners = Arrays.asList(partnerOne, partnerTwo);

        SupportClientsEvent event = new SupportClientsEvent();
        event.clients = Collections.singletonList(client);
        return event;
    }

    public static class MyLocalNoDefaultCtorInvalid implements Serializable {
        public MyLocalNoDefaultCtorInvalid(String id) {
        }
    }

    private static class MyLocalNonPublicInvalid implements Serializable {
        public MyLocalNonPublicInvalid() {
        }
    }

    public class MyLocalInstanceInvalid implements Serializable {
        public MyLocalInstanceInvalid() {
        }
    }

    public static class MyLocalJsonProvidedStringInt implements Serializable {
        public String c0;
    }

    public static class MyLocalJsonProvidedPrimitiveInt implements Serializable {
        public int primitiveInt = -1;
    }

    public static class MyLocalJsonProvidedEventOne implements Serializable {
        public String id;
    }

    public static class MyLocalJsonProvidedEventTwo implements Serializable {
        public String id;
        public int val;
    }

    public static class MyLocalJsonProvidedEventOut implements Serializable {
        public MyLocalJsonProvidedEventOne startEvent;
        public MyLocalJsonProvidedEventTwo[] endEvents;
    }
}
