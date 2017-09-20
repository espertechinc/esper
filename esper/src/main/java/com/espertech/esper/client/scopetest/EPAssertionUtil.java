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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Assertion methods for event processing applications.
 */
public class EPAssertionUtil {
    private static final Logger log = LoggerFactory.getLogger(EPAssertionUtil.class);

    /**
     * Deep compare two 2-dimensional string arrays for the exact same length of arrays and order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(String[][] expected, String[][] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            ScopeTestHelper.assertTrue(Arrays.equals(actual[i], expected[i]));
        }
    }

    /**
     * Compare two 2-dimensional arrays, and using property names for messages, against expected values.
     *
     * @param actual        array of objects
     * @param propertyNames property names
     * @param expected      expected values
     */
    public static void assertEqualsExactOrder(Object[][] actual, String[] propertyNames, Object[][] expected) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            Object[] propertiesThisRow = expected[i];
            for (int j = 0; j < propertiesThisRow.length; j++) {
                String name = propertyNames[j];
                Object value = propertiesThisRow[j];
                Object eventProp = actual[i][j];
                ScopeTestHelper.assertEquals("Error asserting property named " + name, value, eventProp);
            }
        }
    }

    /**
     * Compare the collection of object arrays, and using property names for messages, against expected values.
     *
     * @param actual        colleciton of array of objects
     * @param propertyNames property names
     * @param expected      expected values
     */
    public static void assertEqualsExactOrder(Collection<Object[]> actual, String[] propertyNames, Object[][] expected) {
        Object[][] arr = actual.toArray(new Object[actual.size()][]);
        assertEqualsExactOrder(arr, propertyNames, expected);
    }

    /**
     * Compare the iterator-returned events against the expected events
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(EventBean[] expected, Iterator<EventBean> actual) {
        assertEqualsExactOrder((Object[]) expected, actual);
    }

    /**
     * Compare the underlying events returned by the iterator to the expected values.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrderUnderlying(Object[] expected, Iterator<EventBean> actual) {
        ArrayList<Object> underlyingValues = new ArrayList<Object>();
        while (actual.hasNext()) {
            underlyingValues.add(actual.next().getUnderlying());
        }

        try {
            actual.next();
            ScopeTestHelper.fail();
        } catch (NoSuchElementException ex) {
            // Expected exception - next called after hasNext returned false, for testing
        }

        Object[] data = null;
        if (underlyingValues.size() > 0) {
            data = underlyingValues.toArray();
        }

        assertEqualsExactOrder(expected, data);
    }

    /**
     * Comparing the underlying events to the expected events using equals-semantics.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrderUnderlying(Object[] expected, EventBean[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }

        ArrayList<Object> underlying = new ArrayList<Object>();
        for (EventBean theEvent : actual) {
            underlying.add(theEvent.getUnderlying());
        }

        assertEqualsExactOrder(expected, underlying.toArray());
    }

    /**
     * Compare the objects in the 2-dimension object arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(Object[][] expected, List<Object[]> actual) {
        if (compareArrayAndCollSize(expected, actual)) {
            return;
        }

        for (int i = 0; i < expected.length; i++) {
            Object[] receivedThisRow = actual.get(i);
            Object[] propertiesThisRow = expected[i];
            ScopeTestHelper.assertEquals(receivedThisRow.length, propertiesThisRow.length);

            for (int j = 0; j < propertiesThisRow.length; j++) {
                Object expectedValue = propertiesThisRow[j];
                Object receivedValue = receivedThisRow[j];
                ScopeTestHelper.assertEquals("Error asserting property", expectedValue, receivedValue);
            }
        }
    }

    /**
     * Compare the objects in the two object arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(Object[] expected, Object[] actual) {
        assertEqualsExactOrder(null, expected, actual);
    }

    /**
     * Compare the objects in the two object arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     * @param message  an optional message that can be output when assrtion fails
     */
    public static void assertEqualsExactOrder(String message, Object[] expected, Object[] actual) {
        if (compareArraySize(message, expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            Object value = actual[i];
            Object expectedValue = expected[i];
            String text = message != null ? message + ", " : "";
            text += "Failed to assert at element " + i;
            assertEqualsAllowArray(text, expectedValue, value);
        }
    }

    /**
     * Compare the objects in the expected arrays and actual collection assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(Object[] expected, Collection actual) {
        Object[] actualArray = null;
        if (actual != null) {
            actualArray = actual.toArray();
        }
        assertEqualsExactOrder(expected, actualArray);
    }

    /**
     * Reference-equals the objects in the two object arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertSameExactOrder(Object[] expected, Object[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            ScopeTestHelper.assertSame("at element " + i, expected[i], actual[i]);
        }
    }

    /**
     * Compare the integer values in the two int arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(int[] expected, int[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            ScopeTestHelper.assertEquals(expected[i], actual[i]);
        }
    }

    /**
     * Compare the integer values in the two int arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(int[] expected, Integer[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            ScopeTestHelper.assertEquals(expected[i], actual[i]);
        }
    }

    /**
     * Compare the short values in the two short arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(short[] expected, short[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            ScopeTestHelper.assertEquals(expected[i], actual[i]);
        }
    }

    /**
     * Compare the long values in the long arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(long[] expected, long[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            ScopeTestHelper.assertEquals(expected[i], actual[i]);
        }
    }

    /**
     * Compare the String values in the two String arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(String[] expected, String[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            ScopeTestHelper.assertEquals(expected[i], actual[i]);
        }
    }

    /**
     * Compare the boolean values in the two bool arrays assuming the exact same order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(boolean[] expected, boolean[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            ScopeTestHelper.assertEquals(expected[i], actual[i]);
        }
    }

    /**
     * Compare the objects returned by the iterator to the an object array.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsExactOrder(Object[] expected, Iterator actual) {
        ArrayList<Object> values = new ArrayList<Object>();
        while (actual.hasNext()) {
            values.add(actual.next());
        }

        try {
            actual.next();
            ScopeTestHelper.fail();
        } catch (NoSuchElementException ex) {
            // Expected exception - next called after hasNext returned false, for testing
        }

        Object[] data = null;
        if (values.size() > 0) {
            data = values.toArray();
        }

        assertEqualsExactOrder(expected, data);
    }

    /**
     * Assert that each integer value in the expected array is contained in the actual array.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsAnyOrder(int[] expected, Set<Integer> actual) {
        if (compareArrayAndCollSize(expected, actual)) {
            return;
        }
        for (int anExpected : expected) {
            ScopeTestHelper.assertTrue("not found: " + anExpected, actual.contains(anExpected));
        }
    }

    /**
     * Compare the two integer arrays allowing any order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsAnyOrder(int[] expected, int[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }

        Set<Integer> intSet = new HashSet<Integer>();
        for (int anActual : actual) {
            intSet.add(anActual);
        }

        assertEqualsAnyOrder(expected, intSet);
    }

    /**
     * Compare the two object arrays allowing any order.
     *
     * @param expected is the expected values
     * @param actual   is the actual values
     */
    public static void assertEqualsAnyOrder(Object[] expected, Object[] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        Object[] received = new Object[actual.length];
        System.arraycopy(actual, 0, received, 0, actual.length);

        // For each expected object find a received object
        int numMatches = 0;
        for (Object expectedObject : expected) {
            boolean found = false;
            for (int i = 0; i < received.length; i++) {
                // Ignore found received objects
                if (received[i] == null) {
                    continue;
                }

                if (received[i] instanceof Object[] && expectedObject instanceof Object[]) {
                    boolean result = Arrays.equals((Object[]) received[i], (Object[]) expectedObject);
                    if (result) {
                        found = true;
                        numMatches++;
                        // Blank out received object so as to not match again
                        received[i] = null;
                        break;
                    }
                } else {
                    if (received[i].equals(expectedObject)) {
                        found = true;
                        numMatches++;
                        // Blank out received object so as to not match again
                        received[i] = null;
                        break;
                    }
                }
            }

            if (!found) {
                if (expectedObject instanceof Object[]) {
                    log.error(".assertEqualsAnyOrder Not found in received results is expected=" + Arrays.toString((Object[]) expectedObject));
                } else {
                    log.error(".assertEqualsAnyOrder Not found in received results is expected=" + expectedObject);
                }
                log.error(".assertEqualsAnyOrder received=" + CollectionUtil.toStringArray(received));
            }
            ScopeTestHelper.assertTrue("Failed to find value " + expectedObject + ", check the error logs", found);
        }

        // Must have matched exactly the number of objects times
        ScopeTestHelper.assertEquals(numMatches, expected.length);
    }

    /**
     * Compare the property values returned by events of both iterators with the expected values, using exact-order semantics.
     *
     * @param iterator      provides events
     * @param safeIterator  provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     */
    public static void assertPropsPerRow(Iterator<EventBean> iterator, SafeIterator<EventBean> safeIterator, String[] propertyNames, Object[][] expected) {
        assertPropsPerRow(EPAssertionUtil.iteratorToArray(iterator), propertyNames, expected);
        assertPropsPerRow(EPAssertionUtil.iteratorToArray(safeIterator), propertyNames, expected);
        safeIterator.close();
    }

    /**
     * Compare the property values returned by events of both iterators with the expected values, using any-order semantics.
     *
     * @param iterator      provides events
     * @param safeIterator  provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     */
    public static void assertPropsPerRowAnyOrder(Iterator<EventBean> iterator, SafeIterator<EventBean> safeIterator, String[] propertyNames, Object[][] expected) {
        assertPropsPerRowAnyOrder(EPAssertionUtil.iteratorToArray(iterator), propertyNames, expected);
        assertPropsPerRowAnyOrder(EPAssertionUtil.iteratorToArray(safeIterator), propertyNames, expected);
        safeIterator.close();
    }

    /**
     * Compare property values for insert and remove stream pair
     * @param pair pair
     * @param propertyNames names
     * @param expectedNew expected insert-stream values
     * @param expectedOld expected remove-stream values
     */
    public static void assertPropsPerRowAnyOrder(UniformPair<EventBean[]> pair, String[] propertyNames, Object[][] expectedNew, Object[][] expectedOld) {
        assertPropsPerRowAnyOrder(pair.getFirst(), propertyNames, expectedNew);
        assertPropsPerRowAnyOrder(pair.getSecond(), propertyNames, expectedOld);
    }

    /**
     * Compare the property values returned by events of the iterator with the expected values, using any-order semantics.
     *
     * @param safeIterator  provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     */
    public static void assertPropsPerRowAnyOrder(SafeIterator<EventBean> safeIterator, String[] propertyNames, Object[][] expected) {
        assertPropsPerRowAnyOrder(EPAssertionUtil.iteratorToArray(safeIterator), propertyNames, expected);
        safeIterator.close();
    }

    /**
     * Compare the property values returned by events of the iterator with the expected values, using any-order semantics.
     *
     * @param iterator      provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     */
    public static void assertPropsPerRowAnyOrder(Iterator<EventBean> iterator, String[] propertyNames, Object[][] expected) {
        assertPropsPerRowAnyOrder(EPAssertionUtil.iteratorToArray(iterator), propertyNames, expected);
    }

    /**
     * Compare the property values returned by events of both iterators with the expected values, using exact-order semantics.
     *
     * @param iterator      provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     */
    public static void assertPropsPerRow(Iterator<EventBean> iterator, String[] propertyNames, Object[][] expected) {
        assertPropsPerRow(EPAssertionUtil.iteratorToArray(iterator), propertyNames, expected);
    }

    /**
     * Compare the Map values identified by property names against expected values.
     *
     * @param actual        array of Maps, one for each row
     * @param propertyNames property names
     * @param expected      expected values
     */
    public static void assertPropsPerRow(Map[] actual, String[] propertyNames, Object[][] expected) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            Object[] propertiesThisRow = expected[i];
            for (int j = 0; j < propertiesThisRow.length; j++) {
                String name = propertyNames[j];
                Object value = propertiesThisRow[j];
                Object eventProp = actual[i].get(name);
                ScopeTestHelper.assertEquals("Error asserting property named " + name, value, eventProp);
            }
        }
    }

    /**
     * Compare the property values of events with the expected values, using exact-order semantics.
     *
     * @param received      provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     */
    public static void assertPropsPerRow(EventBean[] received, String[] propertyNames, Object[][] expected) {
        assertPropsPerRow(received, propertyNames, expected, "");
    }

    /**
     * Compare the property values of events with the expected values, using exact-order semantics.
     *
     * @param actual        provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     * @param streamName    an optional name for the stream for use in messages
     */
    public static void assertPropsPerRow(EventBean[] actual, String[] propertyNames, Object[][] expected, String streamName) {
        if (compareArraySize(expected, actual)) {
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            Object[] propertiesThisRow = expected[i];
            ScopeTestHelper.assertEquals("Number of properties expected mismatches for row " + i, propertyNames.length, propertiesThisRow.length);
            for (int j = 0; j < propertiesThisRow.length; j++) {
                String name = propertyNames[j];
                Object value = propertiesThisRow[j];
                Object eventProp = actual[i].get(name);
                StringWriter writer = new StringWriter();
                writer.append("Error asserting property named ");
                writer.append(name);
                writer.append(" for row ");
                writer.append(Integer.toString(i));
                if (streamName != null && streamName.trim().length() != 0) {
                    writer.append(" for stream ");
                    writer.append(streamName);
                }
                assertEqualsAllowArray(writer.toString(), value, eventProp);
            }
        }
    }

    /**
     * Compare the property values of events with the expected values, using any-order semantics.
     *
     * @param actual        provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     */
    public static void assertPropsPerRowAnyOrder(EventBean[] actual, String[] propertyNames, Object[][] expected) {
        if (compareArraySize(expected, actual)) {
            return;
        }

        // build expected
        Object[] expectedArray = new Object[expected.length];
        System.arraycopy(expected, 0, expectedArray, 0, expectedArray.length);

        // build received
        Object[] receivedArray = new Object[actual.length];
        for (int i = 0; i < actual.length; i++) {
            Object[] data = new Object[propertyNames.length];
            receivedArray[i] = data;
            for (int j = 0; j < propertyNames.length; j++) {
                String name = propertyNames[j];
                Object eventProp = actual[i].get(name);
                data[j] = eventProp;
            }
        }

        assertEqualsAnyOrder(expectedArray, receivedArray);
    }

    /**
     * Assert that the property values of a single event match the expected values.
     *
     * @param received      provides events
     * @param propertyNames array of property names
     * @param expected      expected values
     */
    public static void assertProps(EventBean received, String[] propertyNames, Object[] expected) {
        if (compareArraySize(expected, propertyNames)) {
            return;
        }

        for (int j = 0; j < expected.length; j++) {
            String name = propertyNames[j].trim();
            Object value = expected[j];
            Object eventProp = received.get(name);
            assertEqualsAllowArray("Failed to assert property '" + name + "'", value, eventProp);
        }
    }

    /**
     * Assert that the property values of a new event and a removed event match the expected insert and removed values.
     *
     * @param received        provides events
     * @param propertyNames   array of property names
     * @param expectedInsert  expected values insert stream
     * @param expectedRemoved expected values remove stream
     */
    public static void assertProps(UniformPair<EventBean> received, String[] propertyNames, Object[] expectedInsert, Object[] expectedRemoved) {
        assertProps(received.getFirst(), propertyNames, expectedInsert);
        assertProps(received.getSecond(), propertyNames, expectedRemoved);
    }

    /**
     * Assert that the property values of a new event and a removed event match the expected insert and removed values.
     *
     * @param received        provides events
     * @param propertyNames   array of property names
     * @param expectedInsert  expected values insert stream
     * @param expectedRemoved expected values remove stream
     */
    public static void assertPropsPerRow(UniformPair<EventBean[]> received, String[] propertyNames, Object[][] expectedInsert, Object[][] expectedRemoved) {
        assertPropsPerRow(received.getFirst(), propertyNames, expectedInsert);
        assertPropsPerRow(received.getSecond(), propertyNames, expectedRemoved);
    }

    /**
     * Assert that the property values of the events (insert and remove pair) match the expected insert and removed values
     * for a single property.
     *
     * @param received        provides events
     * @param propertyName    property name
     * @param expectedInsert  expected values insert stream
     * @param expectedRemoved expected values remove stream
     */
    public static void assertPropsPerRow(UniformPair<EventBean[]> received, String propertyName, Object[] expectedInsert, Object[] expectedRemoved) {
        Object[] propsInsert = eventsToObjectArr(received.getFirst(), propertyName);
        assertEqualsExactOrder(expectedInsert, propsInsert);

        Object[] propsRemove = eventsToObjectArr(received.getSecond(), propertyName);
        assertEqualsExactOrder(expectedRemoved, propsRemove);
    }

    /**
     * Assert that the underlying objects of the events (insert and remove pair) match the expected insert and removed objects.
     *
     * @param received                 provides events
     * @param expectedUnderlyingInsert expected underlying object insert stream
     * @param expectedUnderlyingRemove expected underlying object remove stream
     */
    public static void assertUnderlyingPerRow(UniformPair<EventBean[]> received, Object[] expectedUnderlyingInsert, Object[] expectedUnderlyingRemove) {
        EventBean[] newEvents = received.getFirst();
        EventBean[] oldEvents = received.getSecond();

        if (expectedUnderlyingInsert != null) {
            ScopeTestHelper.assertEquals(expectedUnderlyingInsert.length, newEvents.length);
            for (int i = 0; i < expectedUnderlyingInsert.length; i++) {
                ScopeTestHelper.assertSame(expectedUnderlyingInsert[i], newEvents[i].getUnderlying());
            }
        } else {
            ScopeTestHelper.assertNull(newEvents);
        }

        if (expectedUnderlyingRemove != null) {
            ScopeTestHelper.assertEquals(expectedUnderlyingRemove.length, oldEvents.length);
            for (int i = 0; i < expectedUnderlyingRemove.length; i++) {
                ScopeTestHelper.assertSame(expectedUnderlyingRemove[i], oldEvents[i].getUnderlying());
            }
        } else {
            ScopeTestHelper.assertNull(oldEvents);
        }
    }

    /**
     * Asserts that the property values of a single event, using property names as provided by the event type in sorted order by property name, match against the expected values.
     *
     * @param received provides events
     * @param expected expected values
     */
    public static void assertAllPropsSortedByName(EventBean received, Object[] expected) {
        if (expected == null) {
            if (received == null) {
                return;
            }
        } else {
            ScopeTestHelper.assertNotNull(received);
        }

        if (expected != null) {
            String[] propertyNames = received.getEventType().getPropertyNames();
            String[] propertyNamesSorted = new String[propertyNames.length];
            System.arraycopy(propertyNames, 0, propertyNamesSorted, 0, propertyNames.length);
            Arrays.sort(propertyNamesSorted);

            for (int j = 0; j < expected.length; j++) {
                String name = propertyNamesSorted[j].trim();
                Object value = expected[j];
                Object eventProp = received.get(name);
                ScopeTestHelper.assertEquals("Error asserting property named '" + name + "'", value, eventProp);
            }
        }
    }

    /**
     * Compare the values of a Map against the expected values.
     *
     * @param received      provides properties
     * @param expected      expected values
     * @param propertyNames property names to assert
     */
    public static void assertPropsMap(Map received, String[] propertyNames, Object... expected) {
        if (expected == null) {
            if (received == null) {
                return;
            }
        } else {
            ScopeTestHelper.assertNotNull(received);
            ScopeTestHelper.assertEquals("Mismatch in number of values to compare", expected.length, propertyNames.length);
        }

        if (expected != null) {
            for (int j = 0; j < expected.length; j++) {
                String name = propertyNames[j].trim();
                Object value = expected[j];
                Object eventProp = received.get(name);
                assertEqualsAllowArray("Error asserting property named '" + name + "'", value, eventProp);
            }
        }
    }

    /**
     * Compare the values of a object array (single row) against the expected values.
     *
     * @param received      provides properties
     * @param expected      expected values
     * @param propertyNames property names to assert
     */
    public static void assertPropsObjectArray(Object[] received, String[] propertyNames, Object... expected) {
        if (expected == null) {
            if (received == null) {
                return;
            }
        } else {
            ScopeTestHelper.assertNotNull(received);
        }

        if (expected != null) {
            for (int j = 0; j < expected.length; j++) {
                String name = propertyNames[j].trim();
                Object value = expected[j];
                Object eventProp = received[j];
                ScopeTestHelper.assertEquals("Error asserting property named '" + name + "'", value, eventProp);
            }
        }
    }

    /**
     * Compare the properties of an object against the expected values.
     *
     * @param propertyNames property names
     * @param received      provides events
     * @param expected      expected values
     */
    public static void assertPropsPOJO(Object received, String[] propertyNames, Object... expected) {
        if (received == null) {
            throw new IllegalArgumentException("No object provided to compare to");
        }
        EventBean pojoEvent = getEventAdapterService().adapterForBean(received);
        assertProps(pojoEvent, propertyNames, expected);
    }

    /**
     * Compare two 2-dimensional event arrays.
     *
     * @param expected expected values
     * @param actual   actual values
     */
    public static void assertEqualsAnyOrder(EventBean[][] expected, EventBean[][] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }

        // For each expected object find a received object
        int numMatches = 0;
        boolean[] foundReceived = new boolean[actual.length];
        for (EventBean[] expectedObject : expected) {
            boolean found = false;
            for (int i = 0; i < actual.length; i++) {
                // Ignore found received objects
                if (foundReceived[i]) {
                    continue;
                }

                boolean match = compareEqualsExactOrder(actual[i], expectedObject);
                if (match) {
                    found = true;
                    numMatches++;
                    foundReceived[i] = true;
                    break;
                }
            }

            if (!found) {
                log.error(".assertEqualsAnyOrder Not found in received results is expected=" + Arrays.toString(expectedObject));
                log.error(".assertEqualsAnyOrder received=" + Arrays.toString(actual));
            }
            ScopeTestHelper.assertTrue(found);
        }

        // Must have matched exactly the number of objects times
        ScopeTestHelper.assertEquals(numMatches, expected.length);
    }

    /**
     * Compare two 2-dimensional object arrays using reference-equals semantics.
     *
     * @param expected expected values
     * @param actual   actual values
     */
    public static void assertSameAnyOrder(Object[][] expected, Object[][] actual) {
        if (compareArraySize(expected, actual)) {
            return;
        }

        // For each expected object find a received object
        int numMatches = 0;
        boolean[] foundReceived = new boolean[actual.length];
        for (Object[] expectedArr : expected) {
            boolean found = false;
            for (int i = 0; i < actual.length; i++) {
                // Ignore found received objects
                if (foundReceived[i]) {
                    continue;
                }

                boolean match = compareRefExactOrder(actual[i], expectedArr);
                if (match) {
                    found = true;
                    numMatches++;
                    // Blank out received object so as to not match again
                    foundReceived[i] = true;
                    break;
                }
            }

            if (!found) {
                log.error(".assertEqualsAnyOrder Not found in received results is expected=" + Arrays.toString(expectedArr));
                for (int j = 0; j < actual.length; j++) {
                    log.error(".assertEqualsAnyOrder                              received (" + j + "):" + Arrays.toString(actual[j]));
                }
                ScopeTestHelper.fail();
            }
        }

        // Must have matched exactly the number of objects times
        ScopeTestHelper.assertEquals(numMatches, expected.length);
    }

    /**
     * Asserts that all values in the given object array are boolean-typed values and are true
     *
     * @param objects values to assert that they are all true
     */
    public static void assertAllBooleanTrue(Object[] objects) {
        for (Object object : objects) {
            ScopeTestHelper.assertTrue((Boolean) object);
        }
    }

    /**
     * Assert the class of the objects in the object array matches the expected classes in the classes array.
     *
     * @param classes is the expected class
     * @param objects is the objects to check the class for
     */
    public static void assertTypeEqualsAnyOrder(Class[] classes, Object[] objects) {
        ScopeTestHelper.assertEquals(classes.length, objects.length);
        Class[] resultClasses = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            resultClasses[i] = objects[i].getClass();
        }
        assertEqualsAnyOrder(resultClasses, classes);
    }

    /**
     * Convert an iterator of event beans to an array of event beans.
     *
     * @param iterator to convert
     * @return array of events
     */
    public static EventBean[] iteratorToArray(Iterator<EventBean> iterator) {
        if (iterator == null) {
            ScopeTestHelper.fail("Null iterator");
        }
        return CollectionUtil.iteratorToArrayEvents(iterator);
    }

    /**
     * Convert an iterator of event beans to an array of underlying objects.
     *
     * @param iterator to convert
     * @return array of event underlying objects
     */
    public static Object[] iteratorToArrayUnderlying(Iterator<EventBean> iterator) {
        ArrayList<Object> events = new ArrayList<Object>();
        for (; iterator.hasNext(); ) {
            events.add(iterator.next().getUnderlying());
        }
        return events.toArray();
    }

    /**
     * Count the number of object provided by an iterator.
     *
     * @param iterator to count
     * @param <T>      type
     * @return count
     */
    public static <T> int iteratorCount(Iterator<T> iterator) {
        int count = 0;
        for (; iterator.hasNext(); ) {
            iterator.next();
            count++;
        }
        return count;
    }

    /**
     * Compare properties of events against a list of maps.
     *
     * @param received actual events
     * @param expected expected values
     */
    public static void assertPropsPerRow(EventBean[] received, List<Map<String, Object>> expected) {
        if ((expected == null) && (received == null)) {
            return;
        }
        if (expected == null || received == null) {
            ScopeTestHelper.fail();
        } else {
            ScopeTestHelper.assertEquals(expected.size(), received.length);
            for (int i = 0; i < expected.size(); i++) {
                assertProps(received[i], expected.get(i));
            }
        }
    }

    /**
     * Compare properties of events against a list of maps.
     *
     * @param iterator actual events
     * @param expected expected values
     */
    public static void assertPropsPerRow(Iterator<EventBean> iterator, List<Map<String, Object>> expected) {
        ArrayList<EventBean> values = new ArrayList<EventBean>();
        while (iterator.hasNext()) {
            values.add(iterator.next());
        }

        try {
            iterator.next();
            ScopeTestHelper.fail();
        } catch (NoSuchElementException ex) {
            // Expected exception - next called after hasNext returned false, for testing
        }

        EventBean[] data = null;
        if (values.size() > 0) {
            data = values.toArray(new EventBean[values.size()]);
        }

        assertPropsPerRow(data, expected);
    }

    /**
     * Concatenate two arrays.
     *
     * @param srcOne array to concatenate
     * @param srcTwo array to concatenate
     * @return concatenated array
     */
    public static Object[] concatenateArray(Object[] srcOne, Object[] srcTwo) {
        Object[] result = new Object[srcOne.length + srcTwo.length];
        System.arraycopy(srcOne, 0, result, 0, srcOne.length);
        System.arraycopy(srcTwo, 0, result, srcOne.length, srcTwo.length);
        return result;
    }

    /**
     * Concatenate two arrays.
     *
     * @param first array to concatenate
     * @param more  array to concatenate
     * @return concatenated array
     */
    public static Object[][] concatenateArray2Dim(Object[][] first, Object[][]... more) {
        int len = first.length;
        for (int i = 0; i < more.length; i++) {
            Object[][] next = more[i];
            len += next.length;
        }

        Object[][] result = new Object[len][];
        int count = 0;
        for (int i = 0; i < first.length; i++) {
            result[count] = first[i];
            count++;
        }

        for (int i = 0; i < more.length; i++) {
            Object[][] next = more[i];
            for (int j = 0; j < next.length; j++) {
                result[count] = next[j];
                count++;
            }
        }

        return result;
    }

    /**
     * Concatenate multiple arrays.
     *
     * @param more arrays to concatenate
     * @return concatenated array
     */
    public static Object[] concatenateArray(Object[]... more) {
        List list = new ArrayList();
        for (int i = 0; i < more.length; i++) {
            for (int j = 0; j < more[i].length; j++) {
                list.add(more[i][j]);
            }
        }
        return list.toArray();
    }

    /**
     * Sort events according to natural ordering of the values or a property.
     *
     * @param events   to sort
     * @param property name of property providing sort values
     * @return sorted array
     */
    public static EventBean[] sort(Iterator<EventBean> events, final String property) {
        return sort(iteratorToArray(events), property);
    }

    /**
     * Sort events according to natural ordering of the values or a property.
     *
     * @param events   to sort
     * @param property name of property providing sort values
     * @return sorted array
     */
    public static EventBean[] sort(EventBean[] events, final String property) {
        List<EventBean> list = Arrays.asList(events);
        Collections.sort(list, new Comparator<EventBean>() {
            public int compare(EventBean o1, EventBean o2) {
                Comparable val1 = (Comparable) o1.get(property);
                Comparable val2 = (Comparable) o2.get(property);
                return val1.compareTo(val2);
            }
        });
        return list.toArray(new EventBean[list.size()]);
    }

    /**
     * Assert that a string set does not contain one or more values.
     *
     * @param stringSet to compare against
     * @param values    to find
     */
    public static void assertNotContains(String[] stringSet, String... values) {
        Set<String> set = new HashSet<String>(Arrays.asList(stringSet));
        for (String value : values) {
            ScopeTestHelper.assertFalse(set.contains(value));
        }
    }

    /**
     * Assert that a string set does contain each of one or more values.
     *
     * @param stringSet to compare against
     * @param values    to find
     */
    public static void assertContains(String[] stringSet, String... values) {
        Set<String> set = new HashSet<String>(Arrays.asList(stringSet));
        for (String value : values) {
            ScopeTestHelper.assertTrue(set.contains(value));
        }
    }

    /**
     * Return an array of underlying objects for an array of events.
     *
     * @param events to return underlying objects
     * @return events
     */
    public static Object[] getUnderlying(EventBean[] events) {
        Object[] arr = new Object[events.length];
        for (int i = 0; i < events.length; i++) {
            arr[i] = events[i].getUnderlying();
        }
        return arr;
    }

    /**
     * Assert that all properties of an event have the same value as passed in.
     *
     * @param received      to inspect
     * @param propertyNames property names
     * @param expected      value
     */
    public static void assertPropsAllValuesSame(EventBean received, String[] propertyNames, Object expected) {
        for (String field : propertyNames) {
            ScopeTestHelper.assertEquals("Field " + field, expected, received.get(field));
        }
    }

    /**
     * Extract the property value of the event property for the given events and return an object array of values.
     *
     * @param events       to extract value from
     * @param propertyName name of property to extract values for
     * @return value object array
     */
    public static Object[] eventsToObjectArr(EventBean[] events, String propertyName) {
        if (events == null) {
            return null;
        }
        Object[] objects = new Object[events.length];
        for (int i = 0; i < events.length; i++) {
            objects[i] = events[i].get(propertyName);
        }
        return objects;
    }

    /**
     * Extract the property value of the event properties for the given events and return an object array of values.
     *
     * @param events        to extract value from
     * @param propertyNames names of properties to extract values for
     * @return value object array
     */
    public static Object[][] eventsToObjectArr(EventBean[] events, String[] propertyNames) {
        if (events == null) {
            return null;
        }
        Object[][] objects = new Object[events.length][];
        for (int i = 0; i < events.length; i++) {
            EventBean theEvent = events[i];
            Object[] values = new Object[propertyNames.length];
            for (int j = 0; j < propertyNames.length; j++) {
                values[j] = theEvent.get(propertyNames[j]);
            }
            objects[i] = values;
        }
        return objects;
    }

    /**
     * Extract the property value of the event property for the given events and return an object array of values.
     *
     * @param iterator     events to extract value from
     * @param propertyName name of property to extract values for
     * @return value object array
     */
    public static Object[] iteratorToObjectArr(Iterator<EventBean> iterator, String propertyName) {
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return CollectionUtil.OBJECTARRAY_EMPTY;
        }
        return eventsToObjectArr(iteratorToArray(iterator), propertyName);
    }

    /**
     * Extract the property value of the event properties for the given events and return an object array of values.
     *
     * @param iterator      events to extract value from
     * @param propertyNames names of properties to extract values for
     * @return value object array
     */
    public static Object[][] iteratorToObjectArr(Iterator<EventBean> iterator, String[] propertyNames) {
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return CollectionUtil.OBJECTARRAYARRAY_EMPTY;
        }
        return eventsToObjectArr(iteratorToArray(iterator), propertyNames);
    }

    /**
     * Compare the events in the two object arrays assuming the exact same order.
     *
     * @param actual   is the actual results
     * @param expected is the expected values
     * @return indicate whether compared successfully
     */
    public static boolean compareEqualsExactOrder(EventBean[] actual, EventBean[] expected) {
        if ((expected == null) && (actual == null)) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }

        if (expected.length != actual.length) {
            return false;
        }

        for (int i = 0; i < expected.length; i++) {
            if ((actual[i] == null) && (expected[i] == null)) {
                continue;
            }

            if (!actual[i].equals(expected[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reference-compare the objects in the two object arrays assuming the exact same order.
     *
     * @param actual   is the actual results
     * @param expected is the expected values
     * @return indicate whether compared successfully
     */
    public static boolean compareRefExactOrder(Object[] actual, Object[] expected) {
        if ((expected == null) && (actual == null)) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }

        if (expected.length != actual.length) {
            return false;
        }

        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Assert that property values of rows, wherein each row can either be Map or POJO objects, matches the expected values.
     *
     * @param received      array of objects may contain Map and POJO events
     * @param propertyNames property names
     * @param expected      expected value
     */
    public static void assertPropsPerRow(Object[] received, String[] propertyNames, Object[][] expected) {
        ScopeTestHelper.assertEquals("Mismatch in number of rows received", expected.length, received.length);
        for (int row = 0; row < received.length; row++) {
            assertProps(received[row], propertyNames, expected[row]);
        }
    }

    /**
     * Assert that property values of rows, wherein each row can either be Map or POJO objects, matches the expected values.
     *
     * @param received      array of objects may contain Map and POJO events
     * @param propertyNames property names
     * @param expected      expected value
     */
    public static void assertPropsPerRow(List<Object[]> received, String[] propertyNames, Object[][] expected) {
        ScopeTestHelper.assertEquals(received.size(), expected.length);
        for (int row = 0; row < received.size(); row++) {
            assertProps(received.get(row), propertyNames, expected[row]);
        }
    }

    /**
     * Assert that property values, wherein the row can either be a Map or a POJO object, matches the expected values.
     *
     * @param received      Map or POJO
     * @param propertyNames property names
     * @param expected      expected value
     */
    public static void assertProps(Object received, String[] propertyNames, Object[] expected) {
        if (received instanceof Map) {
            assertPropsMap((Map) received, propertyNames, expected);
        } else if (received instanceof Object[]) {
            assertPropsObjectArray((Object[]) received, propertyNames, expected);
        } else if (received instanceof EventBean) {
            assertProps((EventBean) received, propertyNames, expected);
        } else {
            assertPropsPOJO(received, propertyNames, expected);
        }
    }

    /**
     * For a given array, copy the array elements into a new array of Object[] type.
     *
     * @param array input array
     * @return object array
     */
    public static Object[] toObjectArray(Object array) {
        if ((array == null) || (!array.getClass().isArray())) {
            throw new IllegalArgumentException("Object not an array but type '" + (array == null ? "null" : array.getClass()) + "'");
        }
        int size = Array.getLength(array);
        Object[] val = new Object[size];
        for (int i = 0; i < size; i++) {
            val[i] = Array.get(array, i);
        }
        return val;
    }

    /**
     * Assert that two property values are the same, allowing arrays as properties.
     *
     * @param message  to use
     * @param expected expected value
     * @param actual   actual value
     */
    public static void assertEqualsAllowArray(String message, Object expected, Object actual) {
        if ((expected != null) && (expected.getClass().isArray()) && (actual != null) && (actual.getClass().isArray())) {
            Object[] valueArray = toObjectArray(expected);
            Object[] eventPropArray = toObjectArray(actual);
            assertEqualsExactOrder(message, valueArray, eventPropArray);
            return;
        }
        ScopeTestHelper.assertEquals(message, expected, actual);
    }

    private static EventAdapterService getEventAdapterService() {
        return SupportEventAdapterService.getService();
    }

    /**
     * Assert that the event properties of the event match the properties provided by the map, taking the map properties as the comparison source.
     *
     * @param received event
     * @param expected expected values
     */
    public static void assertProps(EventBean received, Map<String, Object> expected) {
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            Object valueExpected = entry.getValue();
            Object property = received.get(entry.getKey());

            ScopeTestHelper.assertEquals(valueExpected, property);
        }
    }

    private static boolean compareArrayAndCollSize(Object expected, Collection actual) {
        if (expected == null && (actual == null || actual.size() == 0)) {
            return true;
        }
        if (expected == null || actual == null) {
            if (expected == null) {
                ScopeTestHelper.assertNull("Expected is null but actual is not null", actual);
            }
            ScopeTestHelper.assertNull("Actual is null but expected is not null", expected);
        } else {
            int expectedLength = Array.getLength(expected);
            int actualLength = actual.size();
            ScopeTestHelper.assertEquals("Mismatch in the number of expected and actual length", expectedLength, actualLength);
        }
        return false;
    }

    private static boolean compareArraySize(Object expected, Object actual) {
        return compareArraySize(null, expected, actual);
    }

    private static boolean compareArraySize(String message, Object expected, Object actual) {
        if ((expected == null) && (actual == null || Array.getLength(actual) == 0)) {
            return true;
        }
        if (expected == null || actual == null) {
            String prefix = message != null ? message + ", " : "";
            if (expected == null) {
                ScopeTestHelper.assertNull(prefix + "Expected is null but actual is not null", actual);
            }
            ScopeTestHelper.assertNull(prefix + "Actual is null but expected is not null", expected);
        } else {
            int expectedLength = Array.getLength(expected);
            int actualLength = Array.getLength(actual);
            String prefix = message != null ? message + ", " : "";
            ScopeTestHelper.assertEquals(prefix + "Mismatch in the number of expected and actual number of values asserted", expectedLength, actualLength);
        }
        return false;
    }

    /**
     * Compare two strings removing all newline characters.
     *
     * @param expected expected value
     * @param received received value
     */
    public static void assertEqualsIgnoreNewline(String expected, String received) {
        String expectedClean = removeNewline(expected);
        String receivedClean = removeNewline(received);
        if (!expectedClean.equals(receivedClean)) {
            log.error("Expected: " + expectedClean);
            log.error("Received: " + receivedClean);
            ScopeTestHelper.assertEquals("Mismatch ", expected, received);
        }
    }

    /**
     * Assert that a map of collections (Map&lt;String, Collection&gt;) has expected keys and values.
     *
     * @param map             of string keys and collection-type values
     * @param keys            array of key values
     * @param expectedList    for each key a string that is a comma-separated list of values
     * @param collectionValue the function to apply to each collection value to convert to a string
     */
    public static void assertMapOfCollection(Map map, String[] keys, String[] expectedList, AssertionCollectionValueString collectionValue) {
        ScopeTestHelper.assertEquals(expectedList.length, keys.length);
        if (keys.length == 0 && map.isEmpty()) {
            return;
        }

        ScopeTestHelper.assertEquals(map.size(), keys.length);

        for (int i = 0; i < keys.length; i++) {
            Collection value = (Collection) map.get(keys[i]);
            String[] itemsExpected = expectedList[i].split(",");
            ScopeTestHelper.assertEquals(itemsExpected.length, value.size());

            Iterator it = value.iterator();
            for (int j = 0; j < itemsExpected.length; j++) {
                String received = collectionValue.extractValue(it.next());
                ScopeTestHelper.assertEquals(itemsExpected[j], received);
            }
        }
    }

    private static String removeNewline(String raw) {
        raw = raw.replaceAll("\t", "");
        raw = raw.replaceAll("\n", "");
        raw = raw.replaceAll("\r", "");
        return raw;
    }

    /**
     * Callback for extracting individual collection items for assertion.
     */
    public static interface AssertionCollectionValueString {

        /**
         * Extract value.
         *
         * @param collectionItem to extract from
         * @return extracted value
         */
        public String extractValue(Object collectionItem);
    }
}
