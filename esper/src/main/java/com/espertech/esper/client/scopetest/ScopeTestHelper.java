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
package com.espertech.esper.client.scopetest;

import java.io.StringWriter;
import java.lang.reflect.Constructor;

/**
 * Helper for asserting conditions.
 */
public class ScopeTestHelper {
    private static final String JUNIT_ASSERTIONFAILED_ERROR = "junit.framework.AssertionFailedError";

    /**
     * Assert a condition is false.
     *
     * @param condition to assert
     */
    public static void assertFalse(boolean condition) {
        assertTrue(!condition);
    }

    /**
     * Assert a condition is true.
     *
     * @param condition to assert
     */
    public static void assertTrue(boolean condition) {
        assertTrue(null, condition);
    }

    /**
     * Assert a condition is true.
     *
     * @param message   an optional message
     * @param condition to assert
     */
    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }

    /**
     * Assert a condition is false.
     *
     * @param message   an optional message
     * @param condition to assert
     */
    public static void assertFalse(String message, boolean condition) {
        if (condition) {
            fail(message);
        }
    }

    /**
     * Assert that two values equal.
     *
     * @param message  an optional message
     * @param expected expected value
     * @param actual   actual value
     */
    public static void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        failNotEquals(message, expected, actual);
    }

    /**
     * Assert that two values equal.
     *
     * @param expected expected value
     * @param actual   actual value
     */
    public static void assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Fail assertion.
     */
    public static void fail() {
        fail(null);
    }

    /**
     * Assert that two values are the same.
     *
     * @param message  an optional message
     * @param expected expected value
     * @param actual   actual value
     */
    public static void assertSame(String message, Object expected, Object actual) {
        if (expected == actual) {
            return;
        }
        failNotSame(message, expected, actual);
    }

    /**
     * Assert that two values are the same.
     *
     * @param expected expected value
     * @param actual   actual value
     */
    public static void assertSame(Object expected, Object actual) {
        if (expected == actual) {
            return;
        }
        failNotSame(null, expected, actual);
    }

    /**
     * Assert that a value is null.
     *
     * @param message an optional message
     * @param object  the object to check
     */
    public static void assertNull(String message, Object object) {
        assertTrue(message, object == null);
    }

    /**
     * Assert that a value is not null.
     *
     * @param object the object to check
     */
    public static void assertNotNull(Object object) {
        assertTrue(object != null);
    }

    /**
     * Assert that a value is not null.
     *
     * @param object  the object to check
     * @param message message
     */
    public static void assertNotNull(String message, Object object) {
        assertTrue(message, object != null);
    }

    /**
     * Assert that a value is null.
     *
     * @param object the object to check
     */
    public static void assertNull(Object object) {
        assertTrue(object == null);
    }

    /**
     * Fail assertion formatting a message for not-same.
     *
     * @param message  an optional message
     * @param expected expected value
     * @param actual   actual value
     */
    public static void failNotSame(String message, Object expected, Object actual) {
        fail(format(message, expected, actual, true));
    }

    /**
     * Fail assertion formatting a message for not-equals.
     *
     * @param message  an optional message
     * @param expected expected value
     * @param actual   actual value
     */
    public static void failNotEquals(String message, Object expected, Object actual) {
        fail(format(message, expected, actual, false));
    }

    /**
     * Fail assertion.
     *
     * @param message an optional message
     */
    public static void fail(String message) {

        // Find JUnit Assert class in classpath
        Class junitAssertionFailedError = null;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            junitAssertionFailedError = Class.forName(JUNIT_ASSERTIONFAILED_ERROR, true, cl);
        } catch (ClassNotFoundException ex) {
            // expected
        }

        // no JUnit found
        if (junitAssertionFailedError == null) {
            throw new AssertionError("Failed assertion and no JUnit found in classpath: " + message);
        }

        // throw JUnit AssertionFailedError instead, to be consistent with code that uses JUnit to assert
        Constructor ctor;
        try {
            ctor = junitAssertionFailedError.getConstructor(new Class[]{String.class});
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Failed to find JUnit method 'fail' method: " + e.getMessage());
        }

        try {
            throw (AssertionError) ctor.newInstance(new Object[]{message});
        } catch (Exception e) {
            throw new AssertionError("Failed to call ctor of '" + JUNIT_ASSERTIONFAILED_ERROR + "': " + e.getMessage());
        }
    }

    private static String format(String message, Object expected, Object actual, boolean isSame) {
        StringWriter buf = new StringWriter();
        if (message != null && !message.isEmpty()) {
            buf.append(message);
            buf.append(' ');
        }
        buf.append("expected");
        if (isSame) {
            buf.append(" same");
        }
        buf.append(":<");
        buf.append(expected == null ? "null" : expected.toString());
        buf.append("> but was:<");
        buf.append(actual == null ? "null" : actual.toString());
        buf.append(">");
        return buf.toString();
    }
}
