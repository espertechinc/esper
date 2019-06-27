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
package com.espertech.esper.regressionlib.support.json;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.Json;
import com.espertech.esper.common.client.json.minimaljson.JsonValue;
import com.espertech.esper.common.client.json.minimaljson.WriterConfig;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.event.core.TypeBeanOrUnderlying;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SupportJsonEventTypeUtil {

    public static Class getUnderlyingType(RegressionEnvironment env, String statementNameOfDeployment, String typeName) {
        String deploymentId = env.deploymentId(statementNameOfDeployment);
        if (deploymentId == null) {
            throw new IllegalArgumentException("Failed to find deployment id for statement '" + statementNameOfDeployment + "'");
        }
        EventType eventType = env.runtime().getEventTypeService().getEventType(deploymentId, typeName);
        if (eventType == null) {
            throw new IllegalArgumentException("Failed to find event type '" + typeName + "' for deployment '" + deploymentId + "'");
        }
        return eventType.getUnderlyingType();
    }

    public static Class getNestedUnderlyingType(JsonEventType eventType, String propertyName) {
        Object type = eventType.getTypes().get(propertyName);
        EventType innerType;
        if (type instanceof TypeBeanOrUnderlying) {
            innerType = ((TypeBeanOrUnderlying) type).getEventType();
        } else {
            innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
        }
        return innerType.getUnderlyingType();
    }

    public static void assertJsonWrite(String jsonExpected, EventBean eventBean) {
        assertJsonWrite(Json.parse(jsonExpected), eventBean);
    }

    public static void assertJsonWrite(JsonValue expectedValue, EventBean eventBean) {
        String expectedMinimalJson = expectedValue.toString(WriterConfig.MINIMAL);
        String expectedPrettyJson = expectedValue.toString(WriterConfig.PRETTY_PRINT);

        JsonEventObject und = (JsonEventObject) eventBean.getUnderlying();
        assertEquals(expectedMinimalJson, und.toString(WriterConfig.MINIMAL));
        assertEquals(expectedPrettyJson, und.toString(WriterConfig.PRETTY_PRINT));

        StringWriter writer = new StringWriter();
        try {
            und.writeTo(writer, WriterConfig.MINIMAL);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        assertEquals(expectedMinimalJson, writer.toString());
    }

    public static void compareMaps(Map<String, Object> expected, Map<String, Object> actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());

        compareEntrySets(expected.entrySet(), actual.entrySet());
        compareKeySets(expected.keySet(), actual.keySet());
        compareValueCollection(expected.values(), actual.values());

        for (String expectedKey : actual.keySet()) {
            assertTrue(expected.containsKey(expectedKey));
        }
        assertFalse(expected.containsKey("DUMMY"));

        for (String expectedKey : actual.keySet()) {
            assertEquals(expected.get(expectedKey), actual.get(expectedKey));
        }
        assertNull(expected.get("DUMMY"));

        for (Object value : expected.values()) {
            assertTrue(expected.containsValue(value));
        }
        assertFalse(actual.containsValue("DUMMY"));
        assertEquals(actual.containsValue(null), expected.containsValue(null));

        tryInvalidModify(actual, a -> a.put("DUMMY", "DUMMY"));
        tryInvalidModify(actual, a -> a.putAll(Collections.singletonMap("DUMMY", "DUMMY")));
        tryInvalidModify(actual, a -> a.clear());
        tryInvalidModify(actual, a -> a.remove("DUMMY"));
        tryInvalidModify(actual, a -> a.remove("DUMMY", "DUMMY"));
        tryInvalidModify(actual, a -> a.replaceAll((s, f) -> "DUMMY"));
        tryInvalidModify(actual, a -> a.replace("DUMMY", "DUMMY", "DUMMY"));
        tryInvalidModify(actual, a -> a.replace("DUMMY", "DUMMY"));
        tryInvalidModify(actual, a -> a.putIfAbsent("DUMMY", "DUMMY"));
    }

    private static void compareValueCollection(Collection<Object> expected, Collection<Object> actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(), actual.toArray());
        EPAssertionUtil.assertEqualsExactOrder(iteratorToArray(expected.iterator()), iteratorToArray(actual.iterator()));

        for (Object value : expected) {
            assertTrue(actual.contains(value));
        }
        assertFalse(actual.contains("DUMMY"));

        assertTrue(actual.containsAll(expected));
        assertFalse(actual.containsAll(Arrays.asList("DUMMY")));

        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(), actual.toArray());
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new Object[0]), actual.toArray(new Object[0]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new Object[1]), actual.toArray(new Object[1]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new Object[expected.size()]), actual.toArray(new Object[actual.size()]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new Object[100]), actual.toArray(new Object[100]));

        tryInvalidModify(actual, a -> a.clear());
        tryInvalidModify(actual, a -> a.add("DUMMY"));
        tryInvalidModify(actual, a -> a.remove("DUMMY"));
        tryInvalidModify(actual, a -> a.removeAll(Arrays.asList()));
        tryInvalidModify(actual, a -> a.addAll(Arrays.asList()));
        tryInvalidModify(actual, a -> a.retainAll(Arrays.asList()));
        tryInvalidModify(actual, a -> a.removeIf(x -> true));
    }

    private static void compareKeySets(Set<String> expected, Set<String> actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        EPAssertionUtil.assertEqualsExactOrder(iteratorToArray(expected.iterator()), iteratorToArray(actual.iterator()));
        for (String expectedKey : expected) {
            assertTrue(actual.contains(expectedKey));
        }
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(), actual.toArray());
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new String[0]), actual.toArray(new String[0]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new String[1]), actual.toArray(new String[1]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new String[expected.size()]), actual.toArray(new String[actual.size()]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new String[100]), actual.toArray(new String[100]));
        assertTrue(actual.containsAll(expected));
        assertFalse(actual.containsAll(Arrays.asList("DUMMY")));

        List<String> keysActual = actual.stream().collect(Collectors.toList());
        List<String> keysExpected = expected.stream().collect(Collectors.toList());
        EPAssertionUtil.assertEqualsExactOrder(keysExpected.toArray(), keysActual.toArray());

        keysActual = actual.parallelStream().collect(Collectors.toList());
        keysExpected = expected.parallelStream().collect(Collectors.toList());
        EPAssertionUtil.assertEqualsExactOrder(keysExpected.toArray(), keysActual.toArray());

        EPAssertionUtil.assertEqualsExactOrder(forEachKeyToArray(expected), forEachKeyToArray(actual));
        EPAssertionUtil.assertEqualsExactOrder(iteratorToArray(expected.spliterator()), iteratorToArray(actual.spliterator()));

        tryInvalidModify(actual, keySet -> keySet.clear());
        tryInvalidModify(actual, keySet -> keySet.removeAll(new ArrayList<>()));
        tryInvalidModify(actual, keySet -> keySet.remove("x"));
        tryInvalidModify(actual, keySet -> keySet.retainAll(new ArrayList<>()));
        tryInvalidModify(actual, keySet -> keySet.addAll(Arrays.asList("x")));
        tryInvalidModify(actual, keySet -> keySet.removeIf(x -> true));
    }

    private static <T> void tryInvalidModify(T t, Consumer<T> consumer) {
        try {
            consumer.accept(t);
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    private static void compareEntrySets(Set<Map.Entry<String, Object>> expected, Set<Map.Entry<String, Object>> actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(), actual.toArray());

        for (Map.Entry<String, Object> expectedEntry : expected) {
            assertTrue(actual.contains(expectedEntry));
        }
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(), actual.toArray());
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new Map.Entry[0]), actual.toArray(new Map.Entry[0]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new Map.Entry[1]), actual.toArray(new Map.Entry[1]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new Map.Entry[expected.size()]), actual.toArray(new Map.Entry[actual.size()]));
        EPAssertionUtil.assertEqualsExactOrder(expected.toArray(new Map.Entry[100]), actual.toArray(new Map.Entry[100]));
        assertTrue(actual.containsAll(expected));
        assertFalse(actual.containsAll(Arrays.asList("DUMMY")));

        List<Map.Entry> keysActual = actual.stream().collect(Collectors.toList());
        List<Map.Entry> keysExpected = expected.stream().collect(Collectors.toList());
        EPAssertionUtil.assertEqualsExactOrder(keysExpected.toArray(), keysActual.toArray());

        keysActual = actual.parallelStream().collect(Collectors.toList());
        keysExpected = expected.parallelStream().collect(Collectors.toList());
        EPAssertionUtil.assertEqualsExactOrder(keysExpected.toArray(), keysActual.toArray());

        EPAssertionUtil.assertEqualsExactOrder(forEachEntryToArray(expected), forEachEntryToArray(actual));
        EPAssertionUtil.assertEqualsExactOrder(iteratorToArray(expected.spliterator()), iteratorToArray(actual.spliterator()));

        tryInvalidModify(actual, keySet -> keySet.clear());
        tryInvalidModify(actual, keySet -> keySet.removeAll(new ArrayList<>()));
        tryInvalidModify(actual, keySet -> keySet.remove("x"));
        tryInvalidModify(actual, keySet -> keySet.retainAll(new ArrayList<>()));
        tryInvalidModify(actual, keySet -> keySet.addAll(Arrays.asList()));
        tryInvalidModify(actual, keySet -> keySet.removeIf(x -> true));
    }

    private static Map.Entry<String, Object> toEntry(String name, Object value) {
        return new AbstractMap.SimpleEntry<>(name, value);
    }

    private static Object[] iteratorToArray(Iterator<?> iterator) {
        List<Object> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }
        return list.toArray();
    }

    private static Object[] iteratorToArray(Spliterator<?> iterator) {
        List<Object> list = new ArrayList<>();
        iterator.forEachRemaining(key -> list.add(key));
        return list.toArray();
    }

    private static String[] forEachKeyToArray(Set<String> set) {
        List<String> list = new ArrayList<>();
        set.forEach(key -> list.add(key));
        return list.toArray(new String[0]);
    }

    private static Map.Entry[] forEachEntryToArray(Set<Map.Entry<String, Object>> set) {
        List<Map.Entry> list = new ArrayList<>();
        set.forEach(key -> list.add(key));
        return list.toArray(new Map.Entry[0]);
    }

    public static boolean isBeanBackedJson(EventType eventType) {
        if (!(eventType instanceof JsonEventType)) {
            return false;
        }
        JsonEventType jsonEventType = (JsonEventType) eventType;
        return jsonEventType.getDetail().getOptionalUnderlyingProvided() != null;
    }
}
