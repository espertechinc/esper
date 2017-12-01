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
package com.espertech.esper.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.collection.NullIterator;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.text.Collator;
import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Utility for handling collection or array tasks.
 */
public class CollectionUtil {
    public final static String METHOD_SHRINKARRAYEVENTS = "shrinkArrayEvents";
    public final static String METHOD_SHRINKARRAYEVENTARRAY = "shrinkArrayEventArray";
    public final static String METHOD_SHRINKARRAYOBJECTS = "shrinkArrayObjects";
    public final static String METHOD_TOARRAYEVENTS = "toArrayEvents";
    public final static String METHOD_TOARRAYOBJECTS = "toArrayObjects";
    public final static String METHOD_TOARRAYEVENTSARRAY = "toArrayEventsArray";
    public final static String METHOD_TOARRAYNULLFOREMPTYEVENTS = "toArrayNullForEmptyEvents";
    public final static String METHOD_TOARRAYNULLFOREMPTYOBJECTS = "toArrayNullForEmptyObjects";
    public final static String METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS = "toArrayNullForEmptyValueEvents";
    public final static String METHOD_TOARRAYNULLFOREMPTYVALUEVALUES = "toArrayNullForEmptyValueValues";
    public final static String METHOD_TOARRAYMAYNULL = "toArrayMayNull";
    public final static String METHOD_ITERATORTOARRAYEVENTS = "iteratorToArrayEvents";

    public final static Iterator<EventBean> NULL_EVENT_ITERATOR = new NullIterator<EventBean>();
    public final static Iterable<EventBean> NULL_EVENT_ITERABLE = new Iterable<EventBean>() {
        public Iterator<EventBean> iterator() {
            return NULL_EVENT_ITERATOR;
        }
    };
    public final static SortedMap EMPTY_SORTED_MAP = new TreeMap();
    public final static EventBean[] EVENTBEANARRAY_EMPTY = new EventBean[0];
    public final static EventBean[][] EVENTBEANARRAYARRAY_EMPTY = new EventBean[0][];
    public final static Set<EventBean> SINGLE_NULL_ROW_EVENT_SET = new HashSet<EventBean>();
    public final static String[] STRINGARRAY_EMPTY = new String[0];
    private static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);
    public final static Object[] OBJECTARRAY_EMPTY = new Object[0];
    public final static Object[][] OBJECTARRAYARRAY_EMPTY = new Object[0][];

    static {
        SINGLE_NULL_ROW_EVENT_SET.add(null);
    }

    public final static StopCallback STOP_CALLBACK_NONE;

    static {
        STOP_CALLBACK_NONE = new StopCallback() {
            public void stop() {
                // no action
            }
        };
    }

    public static String toString(Collection<Integer> stack, String delimiterChars) {
        if (stack.isEmpty()) {
            return "";
        }
        if (stack.size() == 1) {
            return Integer.toString(stack.iterator().next());
        }
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (Integer item : stack) {
            writer.append(delimiter);
            writer.append(Integer.toString(item));
            delimiter = delimiterChars;
        }
        return writer.toString();
    }

    public static Object arrayExpandAddElements(Object array, Object[] elementsToAdd) {
        Class cl = array.getClass();
        if (!cl.isArray()) return null;
        int length = Array.getLength(array);
        int newLength = length + elementsToAdd.length;
        Class componentType = array.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, newLength);
        System.arraycopy(array, 0, newArray, 0, length);
        for (int i = 0; i < elementsToAdd.length; i++) {
            Array.set(newArray, length + i, elementsToAdd[i]);
        }
        return newArray;
    }

    public static Object arrayShrinkRemoveSingle(Object array, int index) {
        Class cl = array.getClass();
        if (!cl.isArray()) return null;
        int length = Array.getLength(array);
        int newLength = length - 1;
        Class componentType = array.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, newLength);
        if (index > 0) {
            System.arraycopy(array, 0, newArray, 0, index);
        }
        if (index < newLength) {
            System.arraycopy(array, index + 1, newArray, index, newLength - index);
        }
        return newArray;
    }

    public static Object arrayExpandAddElements(Object array, Collection elementsToAdd) {
        Class cl = array.getClass();
        if (!cl.isArray()) return null;
        int length = Array.getLength(array);
        int newLength = length + elementsToAdd.size();
        Class componentType = array.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, newLength);
        System.arraycopy(array, 0, newArray, 0, length);
        int count = 0;
        for (Object element : elementsToAdd) {
            Array.set(newArray, length + count, element);
            count++;
        }
        return newArray;
    }

    public static Object arrayExpandAddSingle(Object array, Object elementsToAdd) {
        Class cl = array.getClass();
        if (!cl.isArray()) return null;
        int length = Array.getLength(array);
        int newLength = length + 1;
        Class componentType = array.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, newLength);
        System.arraycopy(array, 0, newArray, 0, length);
        Array.set(newArray, length, elementsToAdd);
        return newArray;
    }

    public static int[] addValue(int[] ints, int i) {
        int[] copy = new int[ints.length + 1];
        System.arraycopy(ints, 0, copy, 0, ints.length);
        copy[ints.length] = i;
        return copy;
    }

    public static Object[] addValue(Object[] values, Object value) {
        Object[] copy = new Object[values.length + 1];
        System.arraycopy(values, 0, copy, 0, values.length);
        copy[values.length] = value;
        return copy;
    }

    public static int findItem(String[] items, String item) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns an array of integer values from the set of integer values
     *
     * @param set to return array for
     * @return array
     */
    public static int[] intArray(Collection<Integer> set) {
        if (set == null) {
            return new int[0];
        }
        int[] result = new int[set.size()];
        int index = 0;
        for (Integer value : set) {
            result[index++] = value;
        }
        return result;
    }

    public static String[] copySortArray(String[] values) {
        if (values == null) {
            return null;
        }
        String[] copy = new String[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        Arrays.sort(copy);
        return copy;
    }

    public static boolean sortCompare(String[] valuesOne, String[] valuesTwo) {
        if (valuesOne == null) {
            return valuesTwo == null;
        }
        if (valuesTwo == null) {
            return false;
        }
        String[] copyOne = copySortArray(valuesOne);
        String[] copyTwo = copySortArray(valuesTwo);
        return Arrays.equals(copyOne, copyTwo);
    }

    /**
     * Returns a list of the elements invoking toString on non-null elements.
     *
     * @param collection to render
     * @param <T>        type
     * @return comma-separate list of values (no escape)
     */
    public static <T> String toString(Collection<T> collection) {
        if (collection == null) {
            return "null";
        }
        if (collection.isEmpty()) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        String delimiter = "";
        for (T t : collection) {
            if (t == null) {
                continue;
            }
            buf.append(delimiter);
            buf.append(t);
            delimiter = ", ";
        }
        return buf.toString();
    }

    public static boolean compare(String[] otherIndexProps, String[] thisIndexProps) {
        if (otherIndexProps != null && thisIndexProps != null) {
            return Arrays.equals(otherIndexProps, thisIndexProps);
        }
        return otherIndexProps == null && thisIndexProps == null;
    }

    public static boolean isAllNullArray(Object array) {
        if (array == null) {
            throw new NullPointerException();
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Expected array but received " + array.getClass());
        }
        for (int i = 0; i < Array.getLength(array); i++) {
            if (Array.get(array, i) != null) {
                return false;
            }
        }
        return true;
    }

    public static String toStringArray(Object[] received) {
        StringBuilder buf = new StringBuilder();
        String delimiter = "";
        buf.append("[");
        for (Object t : received) {
            buf.append(delimiter);
            if (t == null) {
                buf.append("null");
            } else if (t instanceof Object[]) {
                buf.append(toStringArray((Object[]) t));
            } else {
                buf.append(t);
            }
            delimiter = ", ";
        }
        buf.append("]");
        return buf.toString();
    }

    public static Map<String, Object> populateNameValueMap(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        int count = values.length / 2;
        if (values.length != count * 2) {
            throw new IllegalArgumentException("Expected an event number of name-value pairs");
        }
        for (int i = 0; i < count; i++) {
            int index = i * 2;
            Object keyValue = values[index];
            if (!(keyValue instanceof String)) {
                throw new IllegalArgumentException("Expected string-type key value at index " + index + " but found " + keyValue);
            }
            String key = (String) keyValue;
            Object value = values[index + 1];
            if (result.containsKey(key)) {
                throw new IllegalArgumentException("Found two or more values for key '" + key + "'");
            }
            result.put(key, value);
        }
        return result;
    }

    public static Object addArrays(Object first, Object second) {
        if (first != null && !first.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array: " + first);
        }
        if (second != null && !second.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array: " + second);
        }
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        int firstLength = Array.getLength(first);
        int secondLength = Array.getLength(second);
        int total = firstLength + secondLength;
        Object dest = Array.newInstance(first.getClass().getComponentType(), total);
        System.arraycopy(first, 0, dest, 0, firstLength);
        System.arraycopy(second, 0, dest, firstLength, secondLength);
        return dest;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param events events
     * @return array or null
     */
    public static EventBean[] toArrayNullForEmptyEvents(Collection<EventBean> events) {
        return events.isEmpty() ? null : events.toArray(new EventBean[events.size()]);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param values values
     * @return array or null
     */
    public static Object[] toArrayNullForEmptyObjects(Collection<Object> values) {
        return values.isEmpty() ? null : values.toArray(new Object[values.size()]);
    }

    public static EventBean[] addArrayWithSetSemantics(EventBean[] arrayOne, EventBean[] arrayTwo) {
        if (arrayOne.length == 0) {
            return arrayTwo;
        }
        if (arrayTwo.length == 0) {
            return arrayOne;
        }
        if (arrayOne.length == 1 && arrayTwo.length == 1) {
            if (arrayOne[0].equals(arrayTwo[0])) {
                return arrayOne;
            } else {
                return new EventBean[]{arrayOne[0], arrayOne[0]};
            }
        }
        if (arrayOne.length == 1 && arrayTwo.length > 1) {
            if (searchArray(arrayTwo, arrayOne[0]) != -1) {
                return arrayTwo;
            }
        }
        if (arrayOne.length > 1 && arrayTwo.length == 1) {
            if (searchArray(arrayOne, arrayTwo[0]) != -1) {
                return arrayOne;
            }
        }
        Set<EventBean> set = new HashSet<EventBean>();
        for (EventBean event : arrayOne) {
            set.add(event);
        }
        for (EventBean event : arrayTwo) {
            set.add(event);
        }
        return set.toArray(new EventBean[set.size()]);
    }

    public static String[] toArray(Collection<String> strings) {
        if (strings.isEmpty()) {
            return STRINGARRAY_EMPTY;
        }
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param events values
     * @return array
     */
    public static EventBean[] toArrayEvents(Collection<EventBean> events) {
        if (events.isEmpty()) {
            return EVENTBEANARRAY_EMPTY;
        }
        return events.toArray(new EventBean[events.size()]);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param values values
     * @return array
     */
    public static Object[] toArrayObjects(List<Object> values) {
        if (values.isEmpty()) {
            return OBJECTARRAY_EMPTY;
        }
        return values.toArray(new Object[values.size()]);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param arrays values
     * @return array
     */
    public static EventBean[][] toArrayEventsArray(ArrayDeque<EventBean[]> arrays) {
        if (arrays.isEmpty()) {
            return EVENTBEANARRAYARRAY_EMPTY;
        }
        return arrays.toArray(new EventBean[arrays.size()][]);
    }

    public static <T> int searchArray(T[] array, T item) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(item)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean removeEventByKeyLazyListMap(Object key, EventBean bean, Map<Object, Object> eventMap) {
        Object listOfBeans = eventMap.get(key);
        if (listOfBeans == null) {
            return false;
        }

        if (listOfBeans instanceof List) {
            List<EventBean> events = (List<EventBean>) listOfBeans;
            boolean result = events.remove(bean);
            if (events.isEmpty()) {
                eventMap.remove(key);
            }
            return result;
        } else if (listOfBeans != null && listOfBeans.equals(bean)) {
            eventMap.remove(key);
            return true;
        }

        return false;
    }

    public static void addEventByKeyLazyListMapBack(Object sortKey, EventBean eventBean, Map<Object, Object> eventMap) {
        Object existing = eventMap.get(sortKey);
        if (existing == null) {
            eventMap.put(sortKey, eventBean);
        } else {
            if (existing instanceof List) {
                List<EventBean> existingList = (List<EventBean>) existing;
                existingList.add(eventBean);
            } else {
                List<EventBean> existingList = new LinkedList<EventBean>();
                existingList.add((EventBean) existing);
                existingList.add(eventBean);
                eventMap.put(sortKey, existingList);
            }
        }
    }

    public static void addEventByKeyLazyListMapFront(Object key, EventBean bean, Map<Object, Object> eventMap) {
        Object current = eventMap.get(key);
        if (current != null) {
            if (current instanceof List) {
                List<EventBean> events = (List<EventBean>) current;
                events.add(0, bean);    // add to front, newest are listed first
            } else {
                EventBean theEvent = (EventBean) current;
                List<EventBean> events = new LinkedList<EventBean>();
                events.add(bean);
                events.add(theEvent);
                eventMap.put(key, events);
            }
        } else {
            eventMap.put(key, bean);
        }
    }

    public static boolean isAnySet(boolean[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i]) {
                return true;
            }
        }
        return false;
    }

    public static <K, V> Map<K, V> twoEntryMap(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static Collection arrayToCollectionAllowNull(Object array) {
        if (array == null) {
            return null;
        }
        if (array instanceof Object[]) {
            return Arrays.asList((Object[]) array);
        }
        int len = Array.getLength(array);
        if (len == 0) {
            return Collections.emptyList();
        }
        if (len == 1) {
            return Collections.singletonList(Array.get(array, 0));
        }
        Deque dq = new ArrayDeque(len);
        for (int i = 0; i < len; i++) {
            dq.add(Array.get(array, i));
        }
        return dq;
    }

    public static CodegenExpression arrayToCollectionAllowNullCodegen(CodegenMethodScope codegenMethodScope, Class arrayType, CodegenExpression array, CodegenClassScope codegenClassScope) {
        if (!arrayType.isArray()) {
            throw new IllegalArgumentException("Expected array type and received " + arrayType);
        }
        CodegenBlock block = codegenMethodScope.makeChild(Collection.class, CollectionUtil.class, codegenClassScope).addParam(arrayType, "array").getBlock()
                .ifRefNullReturnNull("array");
        if (!arrayType.getComponentType().isPrimitive()) {
            return localMethodBuild(block.methodReturn(staticMethod(Arrays.class, "asList", ref("array")))).pass(array).call();
        }
        CodegenMethodNode method = block.ifCondition(equalsIdentity(arrayLength(ref("array")), constant(0)))
                .blockReturn(staticMethod(Collections.class, "emptyList"))
                .ifCondition(equalsIdentity(arrayLength(ref("array")), constant(1)))
                .blockReturn(staticMethod(Collections.class, "singletonList", arrayAtIndex(ref("array"), constant(0))))
                .declareVar(ArrayDeque.class, "dq", newInstance(ArrayDeque.class, arrayLength(ref("array"))))
                .forLoopIntSimple("i", arrayLength(ref("array")))
                .expression(exprDotMethod(ref("dq"), "add", arrayAtIndex(ref("array"), ref("i"))))
                .blockEnd()
                .methodReturn(ref("dq"));
        return localMethodBuild(method).pass(array).call();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param iterable iterable
     * @return collection
     */
    public static Collection iterableToCollection(Iterable iterable) {
        ArrayList items = new ArrayList();
        Iterator iterator = iterable.iterator();
        for (; iterator.hasNext(); ) {
            items.add(iterator.next());
        }
        return items;
    }

    public static int capacityHashMap(int expectedSize) {
        if (expectedSize < 3) {
            return expectedSize + 1;
        }
        if (expectedSize < MAX_POWER_OF_TWO) {
            // This is the calculation used in JDK8 to resize when a putAll
            // happens; it seems to be the most conservative calculation we
            // can make.  0.75 is the default load factor.
            return (int) ((float) expectedSize / 0.75F + 1.0F);
        }
        return Integer.MAX_VALUE; // any large value
    }

    public static EventBean[] toArrayMayNull(EventBean event) {
        return event != null ? new EventBean[]{event} : null;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param collection collection
     * @return array or null
     */
    public static EventBean[] toArrayMayNull(Collection<EventBean> collection) {
        if (collection != null) {
            return collection.toArray(new EventBean[collection.size()]);
        }
        return null;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param count  cnt
     * @param events events
     * @return shrank array
     */
    public static EventBean[] shrinkArrayEvents(int count, EventBean[] events) {
        EventBean[] outEvents = new EventBean[count];
        System.arraycopy(events, 0, outEvents, 0, count);
        return outEvents;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param count cnt
     * @param keys  values
     * @return shrank array
     */
    public static Object[] shrinkArrayObjects(int count, Object[] keys) {
        Object[] outKeys = new Object[count];
        System.arraycopy(keys, 0, outKeys, 0, count);
        return outKeys;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param count       cnt
     * @param eventArrays events
     * @return shrank array
     */
    public static EventBean[][] shrinkArrayEventArray(int count, EventBean[][] eventArrays) {
        EventBean[][] outGens = new EventBean[count][];
        System.arraycopy(eventArrays, 0, outGens, 0, count);
        return outGens;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param events events
     * @return null or array
     */
    public static EventBean[] toArrayNullForEmptyValueEvents(Map<Object, EventBean> events) {
        return events.isEmpty() ? null : events.values().toArray(new EventBean[events.size()]);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param values events
     * @return null or array
     */
    public static Object[] toArrayNullForEmptyValueValues(Map<Object, Object> values) {
        return values.isEmpty() ? null : values.values().toArray(new Object[values.size()]);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param iterator iterator
     * @return array of events
     */
    public static EventBean[] iteratorToArrayEvents(Iterator<EventBean> iterator) {
        if (iterator == null) {
            return null;
        }
        ArrayList<EventBean> events = new ArrayList<EventBean>();
        for (; iterator.hasNext(); ) {
            events.add(iterator.next());
        }
        return events.toArray(new EventBean[events.size()]);
    }

    /**
     * Compares two nullable values using Collator, for use with string-typed values.
     *
     * @param valueOne     first value to compare
     * @param valueTwo     second value to compare
     * @param isDescending true for descending
     * @param collator     the Collator for comparing
     * @return compare result
     */
    public static int compareValuesCollated(Object valueOne, Object valueTwo, boolean isDescending, Collator collator) {
        if (valueOne == null || valueTwo == null) {
            // A null value is considered equal to another null
            // value and smaller than any nonnull value
            if (valueOne == null && valueTwo == null) {
                return 0;
            }
            if (valueOne == null) {
                if (isDescending) {
                    return 1;
                }
                return -1;
            }
            if (isDescending) {
                return -1;
            }
            return 1;
        }

        if (isDescending) {
            return collator.compare(valueTwo, valueOne);
        }

        return collator.compare(valueOne, valueTwo);
    }

    /**
     * Compares two nullable values.
     *
     * @param valueOne     first value to compare
     * @param valueTwo     second value to compare
     * @param isDescending true for descending
     * @return compare result
     */
    public static int compareValues(Object valueOne, Object valueTwo, boolean isDescending) {
        if (valueOne == null || valueTwo == null) {
            // A null value is considered equal to another null
            // value and smaller than any nonnull value
            if (valueOne == null && valueTwo == null) {
                return 0;
            }
            if (valueOne == null) {
                if (isDescending) {
                    return 1;
                }
                return -1;
            }
            if (isDescending) {
                return -1;
            }
            return 1;
        }

        Comparable comparable1;
        if (valueOne instanceof Comparable) {
            comparable1 = (Comparable) valueOne;
        } else {
            throw new ClassCastException("Cannot sort objects of type " + valueOne.getClass());
        }

        if (isDescending) {
            return -1 * comparable1.compareTo(valueTwo);
        }

        return comparable1.compareTo(valueTwo);
    }
}