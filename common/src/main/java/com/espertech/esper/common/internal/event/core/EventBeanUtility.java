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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.*;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.EventBeanSummarizer;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Method to getSelectListEvents events in collections to other collections or other event types.
 */
public class EventBeanUtility {
    public final static String METHOD_FLATTENBATCHJOIN = "flattenBatchJoin";
    public final static String METHOD_FLATTENBATCHSTREAM = "flattenBatchStream";

    /**
     * Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventsPerStream events
     * @return shifted
     */
    public static EventBean[] allocatePerStreamShift(EventBean[] eventsPerStream) {
        EventBean[] evalEvents = new EventBean[eventsPerStream.length + 1];
        System.arraycopy(eventsPerStream, 0, evalEvents, 1, eventsPerStream.length);
        return evalEvents;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param matchingEvents matching
     * @return first
     */
    public static Object getNonemptyFirstEventUnderlying(Collection<EventBean> matchingEvents) {
        EventBean event = getNonemptyFirstEvent(matchingEvents);
        return event.getUnderlying();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param matchingEvents events
     * @return event
     */
    public static EventBean getNonemptyFirstEvent(Collection<EventBean> matchingEvents) {
        if (matchingEvents instanceof List) {
            return ((List<EventBean>) matchingEvents).get(0);
        }
        if (matchingEvents instanceof Deque) {
            return ((Deque<EventBean>) matchingEvents).getFirst();
        }
        return matchingEvents.iterator().next();
    }

    public static EventPropertyGetter getAssertPropertyGetter(EventType type, String propertyName) {
        EventPropertyGetter getter = type.getGetter(propertyName);
        if (getter == null) {
            throw new IllegalStateException("Property " + propertyName + " not found in type " + type.getName());
        }
        return getter;
    }

    public static EventPropertyGetter getAssertPropertyGetter(EventType[] eventTypes, int keyStreamNum, String property) {
        return getAssertPropertyGetter(eventTypes[keyStreamNum], property);
    }

    /**
     * Resizes an array of events to a new size.
     * <p>
     * Returns the same array reference if the size is the same.
     *
     * @param oldArray array to resize
     * @param newSize  new array size
     * @return resized array
     */
    public static EventBean[] resizeArray(EventBean[] oldArray, int newSize) {
        if (oldArray == null) {
            return null;
        }
        if (oldArray.length == newSize) {
            return oldArray;
        }
        EventBean[] newArray = new EventBean[newSize];
        int preserveLength = Math.min(oldArray.length, newSize);
        if (preserveLength > 0) {
            System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
        }
        return newArray;
    }

    /**
     * Flatten the vector of arrays to an array. Return null if an empty vector was passed, else
     * return an array containing all the events.
     *
     * @param eventVector vector
     * @return array with all events
     */
    public static UniformPair<EventBean[]> flattenList(ArrayDeque<UniformPair<EventBean[]>> eventVector) {
        if (eventVector.isEmpty()) {
            return null;
        }

        if (eventVector.size() == 1) {
            return eventVector.getFirst();
        }

        int totalNew = 0;
        int totalOld = 0;
        for (UniformPair<EventBean[]> pair : eventVector) {
            if (pair != null) {
                if (pair.getFirst() != null) {
                    totalNew += pair.getFirst().length;
                }
                if (pair.getSecond() != null) {
                    totalOld += pair.getSecond().length;
                }
            }
        }

        if ((totalNew + totalOld) == 0) {
            return null;
        }

        EventBean[] resultNew = null;
        if (totalNew > 0) {
            resultNew = new EventBean[totalNew];
        }

        EventBean[] resultOld = null;
        if (totalOld > 0) {
            resultOld = new EventBean[totalOld];
        }

        int destPosNew = 0;
        int destPosOld = 0;
        for (UniformPair<EventBean[]> pair : eventVector) {
            if (pair != null) {
                if (pair.getFirst() != null) {
                    System.arraycopy(pair.getFirst(), 0, resultNew, destPosNew, pair.getFirst().length);
                    destPosNew += pair.getFirst().length;
                }
                if (pair.getSecond() != null) {
                    System.arraycopy(pair.getSecond(), 0, resultOld, destPosOld, pair.getSecond().length);
                    destPosOld += pair.getSecond().length;
                }
            }
        }

        return new UniformPair<EventBean[]>(resultNew, resultOld);
    }

    /**
     * Flatten the vector of arrays to an array. Return null if an empty vector was passed, else
     * return an array containing all the events.
     *
     * @param eventVector vector
     * @return array with all events
     */
    public static EventBean[] flatten(ArrayDeque<EventBean[]> eventVector) {
        if (eventVector.isEmpty()) {
            return null;
        }

        if (eventVector.size() == 1) {
            return eventVector.getFirst();
        }

        int totalElements = 0;
        for (EventBean[] arr : eventVector) {
            if (arr != null) {
                totalElements += arr.length;
            }
        }

        if (totalElements == 0) {
            return null;
        }

        EventBean[] result = new EventBean[totalElements];
        int destPos = 0;
        for (EventBean[] arr : eventVector) {
            if (arr != null) {
                System.arraycopy(arr, 0, result, destPos, arr.length);
                destPos += arr.length;
            }
        }

        return result;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Flatten the vector of arrays to an array. Return null if an empty vector was passed, else
     * return an array containing all the events.
     *
     * @param updateVector is a list of updates of old and new events
     * @return array with all events
     */
    public static UniformPair<EventBean[]> flattenBatchStream(List<UniformPair<EventBean[]>> updateVector) {
        if (updateVector.isEmpty()) {
            return new UniformPair<EventBean[]>(null, null);
        }

        if (updateVector.size() == 1) {
            return new UniformPair<EventBean[]>(updateVector.get(0).getFirst(), updateVector.get(0).getSecond());
        }

        int totalNewEvents = 0;
        int totalOldEvents = 0;
        for (UniformPair<EventBean[]> pair : updateVector) {
            if (pair.getFirst() != null) {
                totalNewEvents += pair.getFirst().length;
            }
            if (pair.getSecond() != null) {
                totalOldEvents += pair.getSecond().length;
            }
        }

        if ((totalNewEvents == 0) && (totalOldEvents == 0)) {
            return new UniformPair<EventBean[]>(null, null);
        }

        EventBean[] newEvents = null;
        EventBean[] oldEvents = null;
        if (totalNewEvents != 0) {
            newEvents = new EventBean[totalNewEvents];
        }
        if (totalOldEvents != 0) {
            oldEvents = new EventBean[totalOldEvents];
        }

        int destPosNew = 0;
        int destPosOld = 0;
        for (UniformPair<EventBean[]> pair : updateVector) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            if (newData != null) {
                int newDataLen = newData.length;
                System.arraycopy(newData, 0, newEvents, destPosNew, newDataLen);
                destPosNew += newDataLen;
            }
            if (oldData != null) {
                int oldDataLen = oldData.length;
                System.arraycopy(oldData, 0, oldEvents, destPosOld, oldDataLen);
                destPosOld += oldDataLen;
            }
        }

        return new UniformPair<EventBean[]>(newEvents, oldEvents);
    }

    /**
     * Append arrays.
     *
     * @param source array
     * @param append array
     * @return appended array
     */
    protected static EventBean[] append(EventBean[] source, EventBean[] append) {
        EventBean[] result = new EventBean[source.length + append.length];
        System.arraycopy(source, 0, result, 0, source.length);
        System.arraycopy(append, 0, result, source.length, append.length);
        return result;
    }

    /**
     * Convert list of events to array, returning null for empty or null lists.
     *
     * @param eventList is a list of events to convert
     * @return array of events
     */
    public static EventBean[] toArray(Collection<EventBean> eventList) {
        if ((eventList == null) || (eventList.isEmpty())) {
            return null;
        }
        return eventList.toArray(new EventBean[eventList.size()]);
    }

    /**
     * Returns object array containing property values of given properties, retrieved via EventPropertyGetter
     * instances.
     *
     * @param theEvent        - event to get property values from
     * @param propertyGetters - getters to use for getting property values
     * @return object array with property values
     */
    public static Object[] getPropertyArray(EventBean theEvent, EventPropertyGetter[] propertyGetters) {
        Object[] keyValues = new Object[propertyGetters.length];
        for (int i = 0; i < propertyGetters.length; i++) {
            keyValues[i] = propertyGetters[i].get(theEvent);
        }
        return keyValues;
    }

    public static Object[] getPropertyArray(EventBean[] eventsPerStream, EventPropertyGetter[] propertyGetters, int[] streamNums) {
        Object[] keyValues = new Object[propertyGetters.length];
        for (int i = 0; i < propertyGetters.length; i++) {
            keyValues[i] = propertyGetters[i].get(eventsPerStream[streamNums[i]]);
        }
        return keyValues;
    }

    private static Object[] getPropertyArray(EventBean[] eventsPerStream, ExprEvaluator[] evaluators, ExprEvaluatorContext context) {
        Object[] keys = new Object[evaluators.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = evaluators[i].evaluate(eventsPerStream, true, context);
        }
        return keys;
    }

    public static Object coerce(Object target, Class coercionType) {
        if (coercionType == null) {
            return target;
        }
        if (target != null && !target.getClass().equals(coercionType)) {
            if (target instanceof Number) {
                return JavaClassHelper.coerceBoxed((Number) target, coercionType);
            }
        }
        return target;
    }

    /**
     * Format the event and return a string representation.
     *
     * @param theEvent is the event to format.
     * @return string representation of event
     */
    public static String printEvent(EventBean theEvent) {
        StringWriter writer = new StringWriter();
        PrintWriter buf = new PrintWriter(writer);
        printEvent(buf, theEvent);
        return writer.toString();
    }

    public static String printEvents(EventBean[] events) {
        StringWriter writer = new StringWriter();
        PrintWriter buf = new PrintWriter(writer);
        int count = 0;
        for (EventBean theEvent : events) {
            count++;
            buf.println("Event " + String.format("%6d:", count));
            printEvent(buf, theEvent);
        }
        return writer.toString();
    }

    private static void printEvent(PrintWriter writer, EventBean theEvent) {
        String[] properties = theEvent.getEventType().getPropertyNames();
        for (int i = 0; i < properties.length; i++) {
            String propName = properties[i];
            Object property = theEvent.get(propName);
            String printProperty;
            if (property == null) {
                printProperty = "null";
            } else if (property instanceof Object[]) {
                printProperty = "Array :" + Arrays.toString((Object[]) property);
            } else if (property.getClass().isArray()) {
                printProperty = "Array :" + printArray(property);
            } else {
                printProperty = property.toString();
            }
            writer.println("#" + i + "  " + propName + " = " + printProperty);
        }
    }

    private static String printArray(Object array) {
        Object[] objects = new Object[Array.getLength(array)];
        for (int i = 0; i < Array.getLength(array); i++) {
            objects[i] = Array.get(array, i);
        }
        return Arrays.toString(objects);
    }

    public static void appendEvent(StringWriter writer, EventBean theEvent) {
        String[] properties = theEvent.getEventType().getPropertyNames();
        String delimiter = "";
        for (int i = 0; i < properties.length; i++) {
            String propName = properties[i];
            Object property = theEvent.get(propName);
            String printProperty;
            if (property == null) {
                printProperty = "null";
            } else if (property.getClass().isArray()) {
                printProperty = "Array :" + Arrays.toString((Object[]) property);
            } else {
                printProperty = property.toString();
            }
            writer.append(delimiter);
            writer.append(propName);
            writer.append("=");
            writer.append(printProperty);
            delimiter = ",";
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Flattens a list of pairs of join result sets.
     *
     * @param joinPostings is the list
     * @return is the consolidate sets
     */
    public static UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> flattenBatchJoin(List<UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>> joinPostings) {
        if (joinPostings.isEmpty()) {
            return new UniformPair<>(null, null);
        }

        if (joinPostings.size() == 1) {
            return new UniformPair<>(joinPostings.get(0).getFirst(), joinPostings.get(0).getSecond());
        }

        Set<MultiKeyArrayOfKeys<EventBean>> newEvents = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
        Set<MultiKeyArrayOfKeys<EventBean>> oldEvents = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();

        for (UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> pair : joinPostings) {
            Set<MultiKeyArrayOfKeys<EventBean>> newData = pair.getFirst();
            Set<MultiKeyArrayOfKeys<EventBean>> oldData = pair.getSecond();

            if (newData != null) {
                newEvents.addAll(newData);
            }
            if (oldData != null) {
                oldEvents.addAll(oldData);
            }
        }

        return new UniformPair<>(newEvents, oldEvents);
    }

    /**
     * Expand the array passed in by the single element to add.
     *
     * @param array      to expand
     * @param eventToAdd element to add
     * @return resized array
     */
    public static EventBean[] addToArray(EventBean[] array, EventBean eventToAdd) {
        EventBean[] newArray = new EventBean[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[newArray.length - 1] = eventToAdd;
        return newArray;
    }

    /**
     * Expand the array passed in by the multiple elements to add.
     *
     * @param array       to expand
     * @param eventsToAdd elements to add
     * @return resized array
     */
    public static EventBean[] addToArray(EventBean[] array, Collection<EventBean> eventsToAdd) {
        EventBean[] newArray = new EventBean[array.length + eventsToAdd.size()];
        System.arraycopy(array, 0, newArray, 0, array.length);

        int counter = array.length;
        for (EventBean eventToAdd : eventsToAdd) {
            newArray[counter++] = eventToAdd;
        }
        return newArray;
    }

    /**
     * Create a fragment event type.
     *
     * @param propertyType         property return type
     * @param genericType          property generic type parameter, or null if none
     * @param beanEventTypeFactory for event types
     * @param publicFields indicator whether classes are public-field-property-accessible
     * @return fragment type
     */
    public static FragmentEventType createNativeFragmentType(Class propertyType, Class genericType, BeanEventTypeFactory beanEventTypeFactory, boolean publicFields) {
        boolean isIndexed = false;

        if (propertyType.isArray()) {
            isIndexed = true;
            propertyType = propertyType.getComponentType();
        } else if (JavaClassHelper.isImplementsInterface(propertyType, Iterable.class)) {
            isIndexed = true;
            if (genericType == null) {
                return null;
            }
            propertyType = genericType;
        }

        if (!JavaClassHelper.isFragmentableType(propertyType)) {
            return null;
        }

        EventType type = beanEventTypeFactory.getCreateBeanType(propertyType, publicFields);
        return new FragmentEventType(type, isIndexed, true);
    }

    public static EventBean[] getDistinctByProp(ArrayDeque<EventBean> events, EventPropertyValueGetter getter) {
        if (events == null || events.isEmpty()) {
            return new EventBean[0];
        }
        if (events.size() < 2) {
            return events.toArray(new EventBean[events.size()]);
        }

        Map<Object, EventBean> map = new LinkedHashMap<>(CollectionUtil.capacityHashMap(events.size()));
        if (events.getFirst() instanceof NaturalEventBean) {
            for (EventBean theEvent : events) {
                EventBean inner = ((NaturalEventBean) theEvent).getOptionalSynthetic();
                Object key = getter.get(inner);
                map.put(key, inner);
            }
        } else {
            for (EventBean theEvent : events) {
                Object key = getter.get(theEvent);
                map.put(key, theEvent);
            }
        }

        EventBean[] result = new EventBean[map.size()];
        int count = 0;
        for (EventBean row : map.values()) {
            result[count++] = row;
        }
        return result;
    }

    /**
     * Returns the distinct events by properties.
     *
     * @param events to inspect
     * @param getter for retrieving properties
     * @return distinct events
     */
    public static EventBean[] getDistinctByProp(EventBean[] events, EventPropertyValueGetter getter) {
        if ((events == null) || (events.length < 2) || getter == null) {
            return events;
        }

        Map<Object, EventBean> map = new LinkedHashMap<>(CollectionUtil.capacityHashMap(events.length));
        if (events[0] instanceof NaturalEventBean) {
            for (EventBean theEvent : events) {
                EventBean inner = ((NaturalEventBean) theEvent).getOptionalSynthetic();
                Object key = getter.get(inner);
                map.put(key, theEvent);
            }
        } else {
            for (EventBean theEvent : events) {
                Object key = getter.get(theEvent);
                map.put(key, theEvent);
            }
        }

        EventBean[] result = new EventBean[map.size()];
        int count = 0;
        for (EventBean row : map.values()) {
            result[count++] = row;
        }
        return result;
    }

    public static EventBean[] denaturalize(EventBean[] naturals) {
        if (naturals == null || naturals.length == 0) {
            return null;
        }
        if (!(naturals[0] instanceof NaturalEventBean)) {
            return naturals;
        }
        if (naturals.length == 1) {
            return new EventBean[]{((NaturalEventBean) naturals[0]).getOptionalSynthetic()};
        }
        EventBean[] result = new EventBean[naturals.length];
        for (int i = 0; i < naturals.length; i++) {
            result[i] = ((NaturalEventBean) naturals[i]).getOptionalSynthetic();
        }
        return result;
    }

    public static boolean eventsAreEqualsAllowNull(EventBean first, EventBean second) {
        if (first == null) {
            return second == null;
        }
        return second != null && first.equals(second);
    }

    public static void safeArrayCopy(EventBean[] eventsPerStream, EventBean[] eventsLambda) {
        if (eventsPerStream.length <= eventsLambda.length) {
            System.arraycopy(eventsPerStream, 0, eventsLambda, 0, eventsPerStream.length);
        } else {
            System.arraycopy(eventsPerStream, 0, eventsLambda, 0, eventsLambda.length);
        }
    }

    public static EventBean[] getNewDataNonRemoved(EventBean[] newData, HashSet<EventBean> removedEvents) {
        boolean filter = false;
        for (int i = 0; i < newData.length; i++) {
            if (removedEvents.contains(newData[i])) {
                filter = true;
            }
        }
        if (!filter) {
            return newData;
        }
        if (newData.length == 1) {
            return null;
        }
        ArrayDeque<EventBean> events = new ArrayDeque<EventBean>(newData.length - 1);
        for (int i = 0; i < newData.length; i++) {
            if (!removedEvents.contains(newData[i])) {
                events.add(newData[i]);
            }
        }
        if (events.isEmpty()) {
            return null;
        }
        return events.toArray(new EventBean[events.size()]);
    }

    public static EventBean[] getNewDataNonRemoved(EventBean[] newData, HashSet<EventBean> removedEvents, EventBean[][] newEventsPerView) {
        if (newData == null || newData.length == 0) {
            return null;
        }
        if (newData.length == 1) {
            if (removedEvents.contains(newData[0])) {
                return null;
            }
            boolean pass = findEvent(newData[0], newEventsPerView);
            return pass ? newData : null;
        }
        ArrayDeque<EventBean> events = new ArrayDeque<EventBean>(newData.length - 1);
        for (int i = 0; i < newData.length; i++) {
            if (!removedEvents.contains(newData[i])) {
                boolean pass = findEvent(newData[i], newEventsPerView);
                if (pass) {
                    events.add(newData[i]);
                }
            }
        }
        if (events.isEmpty()) {
            return null;
        }
        return events.toArray(new EventBean[events.size()]);
    }

    /**
     * Renders a map of elements, in which elements can be events or event arrays interspersed with other objects,
     *
     * @param map to render
     * @return comma-separated list of map entry name-value pairs
     */
    public static String toString(Map<String, Object> map) {
        if (map == null) {
            return "null";
        }
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        String delimiter = "";
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            buf.append(delimiter);
            buf.append(entry.getKey());
            buf.append("=");
            if (entry.getValue() instanceof EventBean) {
                buf.append(EventBeanSummarizer.summarize((EventBean) entry.getValue()));
            } else if (entry.getValue() instanceof EventBean[]) {
                buf.append(EventBeanSummarizer.summarize((EventBean[]) entry.getValue()));
            } else if (entry.getValue() == null) {
                buf.append("null");
            } else {
                buf.append(entry.getValue().toString());
            }
            delimiter = ", ";
        }
        return buf.toString();
    }

    public static void addToCollection(EventBean[] toAdd, Collection<EventBean> events) {
        if (toAdd == null) {
            return;
        }
        Collections.addAll(events, toAdd);
    }

    public static void addToCollection(Set<MultiKeyArrayOfKeys<EventBean>> toAdd, Collection<MultiKeyArrayOfKeys<EventBean>> events) {
        if (toAdd == null) {
            return;
        }
        events.addAll(toAdd);
    }

    public static EventBean[] toArrayNullIfEmpty(Collection<EventBean> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        return events.toArray(new EventBean[events.size()]);
    }

    public static Set<MultiKeyArrayOfKeys<EventBean>> toLinkedHashSetNullIfEmpty(Collection<MultiKeyArrayOfKeys<EventBean>> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        return new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>(events);
    }

    public static Set<MultiKeyArrayOfKeys<EventBean>> toSingletonSetIfNotNull(MultiKeyArrayOfKeys<EventBean> row) {
        if (row == null) {
            return null;
        }
        return Collections.singleton(row);
    }

    public static MultiKeyArrayOfKeys<EventBean> getLastInSet(Set<MultiKeyArrayOfKeys<EventBean>> events) {
        if (events.isEmpty()) {
            return null;
        }
        int count = 0;
        for (MultiKeyArrayOfKeys<EventBean> row : events) {
            count++;
            if (count == events.size()) {
                return row;
            }
        }
        throw new IllegalStateException("Cannot get last on empty collection");
    }

    public static EventBean[] toArrayIfNotNull(EventBean optionalEvent) {
        if (optionalEvent == null) {
            return null;
        }
        return new EventBean[]{optionalEvent};
    }

    public static boolean compareEventReferences(EventBean[] firstNonNull, EventBean[] secondNonNull) {
        if (firstNonNull.length != secondNonNull.length) {
            return false;
        }
        for (int i = 0; i < firstNonNull.length; i++) {
            if (firstNonNull[i] != secondNonNull[i]) {
                return false;
            }
        }
        return true;
    }

    public static EventBean[] copyArray(EventBean[] events) {
        EventBean[] copy = new EventBean[events.length];
        System.arraycopy(events, 0, copy, 0, copy.length);
        return copy;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventsZeroSubselect  events
     * @param newData              new data flag
     * @param matchingEvents       collection of events
     * @param exprEvaluatorContext ctx
     * @param filter               filter expression
     * @return first matching
     */
    public static EventBean evaluateFilterExpectSingleMatch(EventBean[] eventsZeroSubselect, boolean newData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, ExprEvaluator filter) {

        EventBean subSelectResult = null;
        for (EventBean subselectEvent : matchingEvents) {
            // Prepare filter expression event list
            eventsZeroSubselect[0] = subselectEvent;

            Boolean pass = (Boolean) filter.evaluate(eventsZeroSubselect, newData, exprEvaluatorContext);
            if ((pass != null) && pass) {
                if (subSelectResult != null) {
                    return null;
                }
                subSelectResult = subselectEvent;
            }
        }

        return subSelectResult;
    }

    private static boolean findEvent(EventBean theEvent, EventBean[][] eventsPerView) {
        for (int i = 0; i < eventsPerView.length; i++) {
            if (eventsPerView[i] == null) {
                continue;
            }
            for (int j = 0; j < eventsPerView[i].length; j++) {
                if (eventsPerView[i][j] == theEvent) {
                    return true;
                }
            }
        }
        return false;
    }
}
