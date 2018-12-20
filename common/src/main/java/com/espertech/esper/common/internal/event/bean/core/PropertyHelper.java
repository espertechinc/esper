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
package com.espertech.esper.common.internal.event.bean.core;

import com.espertech.esper.common.internal.event.bean.getter.ReflectionPropMethodGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventPropertyType;
import com.espertech.esper.common.internal.event.core.WriteablePropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.*;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This class offers utility methods around introspection.
 */
public class PropertyHelper {
    /**
     * Return getter for the given method.
     *
     * @param method                     to return getter for
     * @param eventBeanTypedEventFactory factory for event beans and event types
     * @param beanEventTypeFactory       bean facory
     * @return property getter
     */
    public static EventPropertyGetterSPI getGetter(Method method, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return new ReflectionPropMethodGetter(method, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    /**
     * Introspects the given class and returns event property descriptors for each property found
     * in the class itself, it's superclasses and all interfaces this class and the superclasses implements.
     *
     * @param clazz is the Class to introspect
     * @return list of properties
     */
    public static List<PropertyStem> getProperties(Class clazz) {
        // Determine all interfaces implemented and the interface's parent interfaces if any
        Set<Class> propertyOrigClasses = new HashSet<Class>();
        getImplementedInterfaceParents(clazz, propertyOrigClasses);

        // Add class itself
        propertyOrigClasses.add(clazz);

        // Get the set of property names for all classes
        return getPropertiesForClasses(propertyOrigClasses);
    }

    /**
     * Introspects the given class and returns event property descriptors for each writable property found
     * in the class itself, it's superclasses and all interfaces this class and the superclasses implements.
     *
     * @param clazz is the Class to introspect
     * @return list of properties
     */
    public static Set<WriteablePropertyDescriptor> getWritableProperties(Class clazz) {
        // Determine all interfaces implemented and the interface's parent interfaces if any
        Set<Class> propertyOrigClasses = new HashSet<Class>();
        getImplementedInterfaceParents(clazz, propertyOrigClasses);

        // Add class itself
        propertyOrigClasses.add(clazz);

        // Get the set of property names for all classes
        return getWritablePropertiesForClasses(propertyOrigClasses);
    }

    private static void getImplementedInterfaceParents(Class clazz, Set<Class> classesResult) {
        Class[] interfaces = clazz.getInterfaces();

        if (interfaces == null) {
            return;
        }

        for (int i = 0; i < interfaces.length; i++) {
            classesResult.add(interfaces[i]);
            getImplementedInterfaceParents(interfaces[i], classesResult);
        }
    }

    private static Set<WriteablePropertyDescriptor> getWritablePropertiesForClasses(Set<Class> propertyClasses) {
        Set<WriteablePropertyDescriptor> result = new HashSet<WriteablePropertyDescriptor>();

        for (Class clazz : propertyClasses) {
            addIntrospectPropertiesWritable(clazz, result);
        }

        return result;
    }

    private static List<PropertyStem> getPropertiesForClasses(Set<Class> propertyClasses) {
        List<PropertyStem> result = new LinkedList<PropertyStem>();

        for (Class clazz : propertyClasses) {
            addIntrospectProperties(clazz, result);
            addMappedProperties(clazz, result);
        }

        removeDuplicateProperties(result);
        removeJavaProperties(result);

        return result;
    }

    /**
     * Remove Java language specific properties from the given list of property descriptors.
     *
     * @param properties is the list of property descriptors
     */
    public static void removeJavaProperties(List<PropertyStem> properties) {
        List<PropertyStem> toRemove = new LinkedList<PropertyStem>();

        // add removed entries to separate list
        for (PropertyStem desc : properties) {
            if ((desc.getPropertyName().equals("class")) ||
                    (desc.getPropertyName().equals("getClass")) ||
                    (desc.getPropertyName().equals("toString")) ||
                    (desc.getPropertyName().equals("hashCode"))) {
                toRemove.add(desc);
            }
        }

        // remove
        for (PropertyStem desc : toRemove) {
            properties.remove(desc);
        }
    }

    /**
     * Removed duplicate properties using the property name to find unique properties.
     *
     * @param properties is a list of property descriptors
     */
    protected static void removeDuplicateProperties(List<PropertyStem> properties) {
        LinkedHashMap<String, PropertyStem> set = new LinkedHashMap<String, PropertyStem>();
        List<PropertyStem> toRemove = new LinkedList<PropertyStem>();

        // add duplicates to separate list
        for (PropertyStem desc : properties) {
            if (set.containsKey(desc.getPropertyName())) {
                toRemove.add(desc);
                continue;
            }
            set.put(desc.getPropertyName(), desc);
        }

        // remove duplicates
        for (PropertyStem desc : toRemove) {
            properties.remove(desc);
        }
    }

    /**
     * Adds to the given list of property descriptors the properties of the given class
     * using the Introspector to introspect properties. This also finds array and indexed properties.
     *
     * @param clazz  to introspect
     * @param result is the list to add to
     */
    protected static void addIntrospectProperties(Class clazz, List<PropertyStem> result) {
        PropertyDescriptor[] properties = introspect(clazz);
        for (int i = 0; i < properties.length; i++) {
            PropertyDescriptor property = properties[i];
            String propertyName = property.getName();
            Method readMethod = property.getReadMethod();

            EventPropertyType type = EventPropertyType.SIMPLE;
            if (property instanceof IndexedPropertyDescriptor) {
                readMethod = ((IndexedPropertyDescriptor) property).getIndexedReadMethod();
                type = EventPropertyType.INDEXED;
            }

            if (readMethod == null) {
                continue;
            }

            result.add(new PropertyStem(propertyName, readMethod, type));
        }
    }

    private static void addIntrospectPropertiesWritable(Class clazz, Set<WriteablePropertyDescriptor> result) {
        PropertyDescriptor[] properties = introspect(clazz);
        for (int i = 0; i < properties.length; i++) {
            PropertyDescriptor property = properties[i];
            String propertyName = property.getName();
            Method writeMethod = property.getWriteMethod();

            if (writeMethod == null) {
                continue;
            }

            result.add(new WriteablePropertyDescriptor(propertyName, writeMethod.getParameterTypes()[0], writeMethod, false));
        }
    }

    /**
     * Adds to the given list of property descriptors the mapped properties, ie.
     * properties that have a getter method taking a single String value as a parameter.
     *
     * @param clazz  to introspect
     * @param result is the list to add to
     */
    protected static void addMappedProperties(Class clazz, List<PropertyStem> result) {
        Set<String> uniquePropertyNames = new HashSet<String>();
        Method[] methods = clazz.getMethods();

        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            if (!methodName.startsWith("get")) {
                continue;
            }

            String inferredName = methodName.substring(3, methodName.length());
            if (inferredName.length() == 0) {
                continue;
            }

            Class<?>[] parameterTypes = methods[i].getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }

            if (parameterTypes[0] != String.class) {
                continue;
            }

            String newInferredName = null;
            // Leave uppercase inferred names such as URL
            if (inferredName.length() >= 2) {
                if ((Character.isUpperCase(inferredName.charAt(0))) &&
                        (Character.isUpperCase(inferredName.charAt(1)))) {
                    newInferredName = inferredName;
                }
            }
            // camelCase the inferred name
            if (newInferredName == null) {
                newInferredName = Character.toString(Character.toLowerCase(inferredName.charAt(0)));
                if (inferredName.length() > 1) {
                    newInferredName += inferredName.substring(1, inferredName.length());
                }
            }
            inferredName = newInferredName;

            // if the property inferred name already exists, don't supply it
            if (uniquePropertyNames.contains(inferredName)) {
                continue;
            }

            result.add(new PropertyStem(inferredName, methods[i], EventPropertyType.MAPPED));
            uniquePropertyNames.add(inferredName);
        }
    }

    /**
     * Using the Java Introspector class the method returns the property descriptors obtained through introspection.
     *
     * @param clazz to introspect
     * @return array of property descriptors
     */
    protected static PropertyDescriptor[] introspect(Class clazz) {
        BeanInfo beanInfo;

        try {
            beanInfo = Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException e) {
            return new PropertyDescriptor[0];
        }

        return beanInfo.getPropertyDescriptors();
    }

    public static String getGetterMethodName(String propertyName) {
        return getGetterSetterMethodName(propertyName, "get");
    }

    public static String getSetterMethodName(String propertyName) {
        return getGetterSetterMethodName(propertyName, "set");
    }

    public static String getIsMethodName(String propertyName) {
        return getGetterSetterMethodName(propertyName, "is");
    }

    private static String getGetterSetterMethodName(String propertyName, String operation) {
        StringWriter writer = new StringWriter();
        writer.write(operation);
        writer.write(Character.toUpperCase(propertyName.charAt(0)));
        writer.write(propertyName.substring(1));
        return writer.toString();
    }

    private static final Logger log = LoggerFactory.getLogger(PropertyHelper.class);
}
