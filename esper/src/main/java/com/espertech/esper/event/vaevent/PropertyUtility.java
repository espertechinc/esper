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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility for handling properties for the purpose of merging and versioning by revision event types.
 */
public class PropertyUtility {
    private static final Logger log = LoggerFactory.getLogger(PropertyUtility.class);

    /**
     * Returns a multi-key for an event and key property getters
     *
     * @param theEvent           to get keys for
     * @param keyPropertyGetters getters to use
     * @return key
     */
    public static Object getKeys(EventBean theEvent, EventPropertyGetter[] keyPropertyGetters) {
        if (keyPropertyGetters.length == 1) {
            return keyPropertyGetters[0].get(theEvent);
        }

        Object[] keys = new Object[keyPropertyGetters.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = keyPropertyGetters[i].get(theEvent);
        }
        return new MultiKeyUntyped(keys);
    }

    /**
     * From a list of property groups that include contributing event types, build a map
     * of contributing event types and their type descriptor.
     *
     * @param groups              property groups
     * @param changesetProperties properties that change between groups
     * @param keyProperties       key properties
     * @return map of event type and type information
     */
    public static Map<EventType, RevisionTypeDesc> getPerType(PropertyGroupDesc[] groups, String[] changesetProperties, String[] keyProperties) {
        Map<EventType, RevisionTypeDesc> perType = new HashMap<EventType, RevisionTypeDesc>();
        for (PropertyGroupDesc group : groups) {
            for (EventType type : group.getTypes().keySet()) {
                EventPropertyGetter[] changesetGetters = getGetters(type, changesetProperties);
                EventPropertyGetter[] keyGetters = getGetters(type, keyProperties);
                RevisionTypeDesc pair = new RevisionTypeDesc(keyGetters, changesetGetters, group);
                perType.put(type, pair);
            }
        }
        return perType;
    }

    /**
     * From a list of property groups that include multiple group numbers for each property,
     * make a map of group numbers per property.
     *
     * @param groups property groups
     * @return map of property name and group number
     */
    public static Map<String, int[]> getGroupsPerProperty(PropertyGroupDesc[] groups) {
        Map<String, int[]> groupsNumsPerProp = new HashMap<String, int[]>();
        for (PropertyGroupDesc group : groups) {
            for (String property : group.getProperties()) {
                int[] value = groupsNumsPerProp.get(property);
                if (value == null) {
                    value = new int[1];
                    groupsNumsPerProp.put(property, value);
                    value[0] = group.getGroupNum();
                } else {
                    int[] copy = new int[value.length + 1];
                    System.arraycopy(value, 0, copy, 0, value.length);
                    copy[value.length] = group.getGroupNum();
                    Arrays.sort(copy);
                    groupsNumsPerProp.put(property, copy);
                }
            }
        }
        return groupsNumsPerProp;
    }

    /**
     * Analyze multiple event types and determine common property sets that form property groups.
     *
     * @param allProperties   property names to look at
     * @param deltaEventTypes all types contributing
     * @param names           names of properies
     * @return groups
     */
    public static PropertyGroupDesc[] analyzeGroups(String[] allProperties, EventType[] deltaEventTypes, String[] names) {
        if (deltaEventTypes.length != names.length) {
            throw new IllegalArgumentException("Delta event type number and name number of elements don't match");
        }
        allProperties = copyAndSort(allProperties);

        Map<MultiKey<String>, PropertyGroupDesc> result = new LinkedHashMap<MultiKey<String>, PropertyGroupDesc>();
        int currentGroupNum = 0;

        for (int i = 0; i < deltaEventTypes.length; i++) {
            MultiKey<String> props = getPropertiesContributed(deltaEventTypes[i], allProperties);
            if (props.getArray().length == 0) {
                log.warn("Event type named '" + names[i] + "' does not contribute (or override) any properties of the revision event type");
                continue;
            }

            PropertyGroupDesc propertyGroup = result.get(props);
            Map<EventType, String> typesForGroup;
            if (propertyGroup == null) {
                typesForGroup = new HashMap<EventType, String>();
                propertyGroup = new PropertyGroupDesc(currentGroupNum++, typesForGroup, props.getArray());
                result.put(props, propertyGroup);
            } else {
                typesForGroup = propertyGroup.getTypes();
            }
            typesForGroup.put(deltaEventTypes[i], names[i]);
        }

        Collection<PropertyGroupDesc> outColl = result.values();
        PropertyGroupDesc[] array = outColl.toArray(new PropertyGroupDesc[outColl.size()]);

        if (log.isDebugEnabled()) {
            log.debug(".analyzeGroups " + Arrays.toString(array));
        }
        return array;
    }

    private static MultiKey<String> getPropertiesContributed(EventType deltaEventType, String[] allPropertiesSorted) {

        TreeSet<String> props = new TreeSet<String>();
        for (String property : deltaEventType.getPropertyNames()) {
            for (String propInAll : allPropertiesSorted) {
                if (propInAll.equals(property)) {
                    props.add(property);
                    break;
                }
            }
        }
        return new MultiKey<String>(props.toArray(new String[props.size()]));
    }

    /**
     * Copy an sort the input array.
     *
     * @param input to sort
     * @return sorted copied array
     */
    protected static String[] copyAndSort(String[] input) {
        String[] result = new String[input.length];
        System.arraycopy(input, 0, result, 0, input.length);
        Arrays.sort(result);
        return result;
    }

    /**
     * Return getters for property names.
     *
     * @param eventType     type to get getters from
     * @param propertyNames names to get
     * @return getters
     */
    public static EventPropertyGetter[] getGetters(EventType eventType, String[] propertyNames) {
        EventPropertyGetter[] getters = new EventPropertyGetter[propertyNames.length];
        for (int i = 0; i < getters.length; i++) {
            getters[i] = eventType.getGetter(propertyNames[i]);
        }
        return getters;
    }

    /**
     * Remove from values all removeValues and build a unique sorted result array.
     *
     * @param values       to consider
     * @param removeValues values to remove from values
     * @return sorted unique
     */
    protected static String[] uniqueExclusiveSort(String[] values, String[] removeValues) {
        Set<String> unique = new HashSet<String>();
        unique.addAll(Arrays.asList(values));
        for (String removeValue : removeValues) {
            unique.remove(removeValue);
        }
        String[] uniqueArr = unique.toArray(new String[unique.size()]);
        Arrays.sort(uniqueArr);
        return uniqueArr;
    }

    public static PropertyAccessException getMismatchException(Method method, Object object, ClassCastException e) {
        return getMismatchException(method.getDeclaringClass(), object, e);
    }

    public static PropertyAccessException getMismatchException(Field field, Object object, ClassCastException e) {
        return getMismatchException(field.getDeclaringClass(), object, e);
    }

    public static PropertyAccessException getInvocationTargetException(Method method, InvocationTargetException e) {
        Class declaring = method.getDeclaringClass();
        String message = "Failed to invoke method " + method.getName() + " on class " + JavaClassHelper.getClassNameFullyQualPretty(declaring) + ": " + e.getTargetException().getMessage();
        throw new PropertyAccessException(message, e);
    }

    public static PropertyAccessException getIllegalAccessException(Field field, IllegalAccessException e) {
        return getAccessExceptionField(field, e);
    }

    public static PropertyAccessException getIllegalArgumentException(Field field, IllegalArgumentException e) {
        return getAccessExceptionField(field, e);
    }

    private static PropertyAccessException getAccessExceptionField(Field field, Exception e) {
        Class declaring = field.getDeclaringClass();
        String message = "Failed to obtain field value for field " + field.getName() + " on class " + JavaClassHelper.getClassNameFullyQualPretty(declaring) + ": " + e.getMessage();
        throw new PropertyAccessException(message, e);
    }

    private static PropertyAccessException getMismatchException(Class declared, Object object, ClassCastException e) {
        String classNameExpected = JavaClassHelper.getClassNameFullyQualPretty(declared);
        String classNameReceived;
        if (object != null) {
            classNameReceived = JavaClassHelper.getClassNameFullyQualPretty(object.getClass());
        } else {
            classNameReceived = "null";
        }

        if (classNameExpected.equals(classNameReceived)) {
            classNameExpected = JavaClassHelper.getClassNameFullyQualPrettyWithClassloader(declared);
            classNameReceived = object != null ? JavaClassHelper.getClassNameFullyQualPrettyWithClassloader(object.getClass()) : "null";
        }

        String message = "Mismatched getter instance to event bean type, expected " + classNameExpected + " but received " + classNameReceived;
        throw new PropertyAccessException(message, e);
    }

    public static PropertyAccessException getIllegalAccessException(Method method, IllegalAccessException e) {
        return getAccessExceptionMethod(method, e);
    }

    public static PropertyAccessException getIllegalArgumentException(Method method, IllegalArgumentException e) {
        return getAccessExceptionMethod(method, e);
    }

    private static PropertyAccessException getAccessExceptionMethod(Method method, Exception e) {
        Class declaring = method.getDeclaringClass();
        String message = "Failed to invoke method " + method.getName() + " on class " + JavaClassHelper.getClassNameFullyQualPretty(declaring) + ": " + e.getMessage();
        throw new PropertyAccessException(message, e);
    }
}
