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
package com.espertech.esper.event.property;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.bean.NestedPropertyGetter;
import com.espertech.esper.event.map.MapEventPropertyGetter;
import com.espertech.esper.event.map.MapNestedPropertyGetterMapOnly;
import com.espertech.esper.event.map.MapNestedPropertyGetterMixedType;
import com.espertech.esper.event.xml.*;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.*;

/**
 * This class represents a nested property, each nesting level made up of a property instance that
 * can be of type indexed, mapped or simple itself.
 * <p>
 * The syntax for nested properties is as follows.
 * <pre>
 * a.n
 * a[1].n
 * a('1').n
 * </pre>
 */
public class NestedProperty implements Property {
    private List<Property> properties;

    /**
     * Ctor.
     *
     * @param properties is the list of Property instances representing each nesting level
     */
    public NestedProperty(List<Property> properties) {
        this.properties = properties;
    }

    /**
     * Returns the list of property instances making up the nesting levels.
     *
     * @return list of Property instances
     */
    public List<Property> getProperties() {
        return properties;
    }

    public boolean isDynamic() {
        for (Property property : properties) {
            if (property.isDynamic()) {
                return true;
            }
        }
        return false;
    }

    public EventPropertyGetterSPI getGetter(BeanEventType eventType, EventAdapterService eventAdapterService) {
        List<EventPropertyGetter> getters = new LinkedList<EventPropertyGetter>();

        Property lastProperty = null;
        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property property = it.next();
            lastProperty = property;
            EventPropertyGetter getter = property.getGetter(eventType, eventAdapterService);
            if (getter == null) {
                return null;
            }

            if (it.hasNext()) {
                Class clazz = property.getPropertyType(eventType, eventAdapterService);
                if (clazz == null) {
                    // if the property is not valid, return null
                    return null;
                }
                // Map cannot be used to further nest as the type cannot be determined
                if (clazz == Map.class) {
                    return null;
                }
                if (clazz.isArray()) {
                    return null;
                }
                eventType = eventAdapterService.getBeanEventTypeFactory().createBeanType(clazz.getName(), clazz, false, false, false);
            }
            getters.add(getter);
        }

        GenericPropertyDesc finalPropertyType = lastProperty.getPropertyTypeGeneric(eventType, eventAdapterService);
        return new NestedPropertyGetter(getters, eventAdapterService, finalPropertyType.getType(), finalPropertyType.getGeneric());
    }

    public Class getPropertyType(BeanEventType eventType, EventAdapterService eventAdapterService) {
        Class result = null;
        boolean boxed = false;

        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property property = it.next();
            boxed |= !(property instanceof SimpleProperty);
            result = property.getPropertyType(eventType, eventAdapterService);

            if (result == null) {
                // property not found, return null
                return null;
            }

            if (it.hasNext()) {
                // Map cannot be used to further nest as the type cannot be determined
                if (result == Map.class) {
                    return null;
                }

                if (result.isArray() || result.isPrimitive() || JavaClassHelper.isJavaBuiltinDataType(result)) {
                    return null;
                }

                eventType = eventAdapterService.getBeanEventTypeFactory().createBeanType(result.getName(), result, false, false, false);
            }
        }

        return !boxed ? result : JavaClassHelper.getBoxedType(result);
    }

    public GenericPropertyDesc getPropertyTypeGeneric(BeanEventType eventType, EventAdapterService eventAdapterService) {
        GenericPropertyDesc result = null;

        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property property = it.next();
            result = property.getPropertyTypeGeneric(eventType, eventAdapterService);

            if (result == null) {
                // property not found, return null
                return null;
            }

            if (it.hasNext()) {
                // Map cannot be used to further nest as the type cannot be determined
                if (result.getType() == Map.class) {
                    return null;
                }

                if (result.getType().isArray()) {
                    return null;
                }

                eventType = eventAdapterService.getBeanEventTypeFactory().createBeanType(result.getType().getName(), result.getType(), false, false, false);
            }
        }

        return result;
    }

    public Class getPropertyTypeMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        Map currentDictionary = optionalMapPropTypes;

        int count = 0;
        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            count++;
            Property property = it.next();
            PropertyBase theBase = (PropertyBase) property;
            String propertyName = theBase.getPropertyNameAtomic();

            Object nestedType = null;
            if (currentDictionary != null) {
                nestedType = currentDictionary.get(propertyName);
            }

            if (nestedType == null) {
                if (property instanceof DynamicProperty) {
                    return Object.class;
                } else {
                    return null;
                }
            }

            if (!it.hasNext()) {
                if (nestedType instanceof Class) {
                    return (Class) nestedType;
                }
                if (nestedType instanceof Map) {
                    return Map.class;
                }
            }

            if (nestedType == Map.class) {
                return Object.class;
            }

            if (nestedType instanceof Class) {
                Class pojoClass = (Class) nestedType;
                if (!pojoClass.isArray()) {
                    BeanEventType beanType = eventAdapterService.getBeanEventTypeFactory().createBeanType(pojoClass.getName(), pojoClass, false, false, false);
                    String remainingProps = toPropertyEPL(properties, count);
                    return beanType.getPropertyType(remainingProps);
                } else if (property instanceof IndexedProperty) {
                    Class componentType = pojoClass.getComponentType();
                    BeanEventType beanType = eventAdapterService.getBeanEventTypeFactory().createBeanType(componentType.getName(), componentType, false, false, false);
                    String remainingProps = toPropertyEPL(properties, count);
                    return beanType.getPropertyType(remainingProps);
                }
            }

            if (nestedType instanceof String) {
                // property type is the name of a map event type
                String nestedName = nestedType.toString();
                boolean isArray = EventTypeUtility.isPropertyArray(nestedName);
                if (isArray) {
                    nestedName = EventTypeUtility.getPropertyRemoveArray(nestedName);
                }

                EventType innerType = eventAdapterService.getExistsTypeByName(nestedName);
                if (innerType == null) {
                    return null;
                }

                String remainingProps = toPropertyEPL(properties, count);
                return innerType.getPropertyType(remainingProps);
            } else if (nestedType instanceof EventType) {
                // property type is the name of a map event type
                EventType innerType = (EventType) nestedType;
                String remainingProps = toPropertyEPL(properties, count);
                return innerType.getPropertyType(remainingProps);
            } else {
                if (!(nestedType instanceof Map)) {
                    String message = "Nestable map type configuration encountered an unexpected value type of '"
                            + nestedType.getClass() + "' for property '" + propertyName + "', expected Class, Map.class or Map<String, Object> as value type";
                    throw new PropertyAccessException(message);
                }
            }

            currentDictionary = (Map) nestedType;
        }
        throw new IllegalStateException("Unexpected end of nested property");
    }

    public MapEventPropertyGetter getGetterMap(Map optionalMapPropTypes, EventAdapterService eventAdapterService) {
        List<EventPropertyGetterSPI> getters = new LinkedList<EventPropertyGetterSPI>();
        Map currentDictionary = optionalMapPropTypes;

        int count = 0;
        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            count++;
            Property property = it.next();

            // manufacture a getter for getting the item out of the map
            EventPropertyGetterSPI getter = property.getGetterMap(currentDictionary, eventAdapterService);
            if (getter == null) {
                return null;
            }
            getters.add(getter);

            PropertyBase theBase = (PropertyBase) property;
            String propertyName = theBase.getPropertyNameAtomic();

            // For the next property if there is one, check how to property type is defined
            if (!it.hasNext()) {
                continue;
            }

            if (currentDictionary != null) {
                // check the type that this property will return
                Object propertyReturnType = currentDictionary.get(propertyName);

                if (propertyReturnType == null) {
                    currentDictionary = null;
                }
                if (propertyReturnType != null) {
                    if (propertyReturnType instanceof Map) {
                        currentDictionary = (Map) propertyReturnType;
                    } else if (propertyReturnType == Map.class) {
                        currentDictionary = null;
                    } else if (propertyReturnType instanceof String) {
                        String nestedName = propertyReturnType.toString();
                        boolean isArray = EventTypeUtility.isPropertyArray(nestedName);
                        if (isArray) {
                            nestedName = EventTypeUtility.getPropertyRemoveArray(nestedName);
                        }

                        EventType innerType = eventAdapterService.getExistsTypeByName(nestedName);
                        if (innerType == null) {
                            return null;
                        }

                        String remainingProps = toPropertyEPL(properties, count);
                        EventPropertyGetterSPI getterInner = ((EventTypeSPI) innerType).getGetterSPI(remainingProps);
                        if (getterInner == null) {
                            return null;
                        }
                        getters.add(getterInner);
                        break; // the single Pojo getter handles the rest
                    } else if (propertyReturnType instanceof EventType) {
                        EventType innerType = (EventType) propertyReturnType;
                        String remainingProps = toPropertyEPL(properties, count);
                        EventPropertyGetterSPI getterInner = ((EventTypeSPI) innerType).getGetterSPI(remainingProps);
                        if (getterInner == null) {
                            return null;
                        }
                        getters.add(getterInner);
                        break; // the single Pojo getter handles the rest
                    } else {
                        // treat the return type of the map property as a POJO
                        Class pojoClass = (Class) propertyReturnType;
                        if (!pojoClass.isArray()) {
                            BeanEventType beanType = eventAdapterService.getBeanEventTypeFactory().createBeanType(pojoClass.getName(), pojoClass, false, false, false);
                            String remainingProps = toPropertyEPL(properties, count);
                            EventPropertyGetterSPI getterInner = beanType.getGetterSPI(remainingProps);
                            if (getterInner == null) {
                                return null;
                            }
                            getters.add(getterInner);
                            break; // the single Pojo getter handles the rest
                        } else {
                            Class componentType = pojoClass.getComponentType();
                            BeanEventType beanType = eventAdapterService.getBeanEventTypeFactory().createBeanType(componentType.getName(), componentType, false, false, false);
                            String remainingProps = toPropertyEPL(properties, count);
                            EventPropertyGetterSPI getterInner = beanType.getGetterSPI(remainingProps);
                            if (getterInner == null) {
                                return null;
                            }
                            getters.add(getterInner);
                            break; // the single Pojo getter handles the rest
                        }
                    }
                }
            }
        }

        boolean hasNonmapGetters = false;
        for (int i = 0; i < getters.size(); i++) {
            if (!(getters.get(i) instanceof MapEventPropertyGetter)) {
                hasNonmapGetters = true;
            }
        }
        if (!hasNonmapGetters) {
            return new MapNestedPropertyGetterMapOnly(getters, eventAdapterService);
        } else {
            return new MapNestedPropertyGetterMixedType(getters);
        }
    }

    public void toPropertyEPL(StringWriter writer) {
        String delimiter = "";
        for (Property property : properties) {
            writer.append(delimiter);
            property.toPropertyEPL(writer);
            delimiter = ".";
        }
    }

    public String[] toPropertyArray() {
        List<String> propertyNames = new ArrayList<String>();
        for (Property property : properties) {
            String[] nested = property.toPropertyArray();
            propertyNames.addAll(Arrays.asList(nested));
        }
        return propertyNames.toArray(new String[propertyNames.size()]);
    }

    public EventPropertyGetterSPI getGetterDOM() {
        List<EventPropertyGetter> getters = new LinkedList<EventPropertyGetter>();

        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property property = it.next();
            EventPropertyGetter getter = property.getGetterDOM();
            if (getter == null) {
                return null;
            }

            getters.add(getter);
        }

        return new DOMNestedPropertyGetter(getters, null);
    }

    public EventPropertyGetterSPI getGetterDOM(SchemaElementComplex parentComplexProperty, EventAdapterService eventAdapterService, BaseXMLEventType eventType, String propertyExpression) {
        List<EventPropertyGetter> getters = new LinkedList<EventPropertyGetter>();

        SchemaElementComplex complexElement = parentComplexProperty;

        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property property = it.next();
            EventPropertyGetter getter = property.getGetterDOM(complexElement, eventAdapterService, eventType, propertyExpression);
            if (getter == null) {
                return null;
            }

            if (it.hasNext()) {
                SchemaItem childSchemaItem = property.getPropertyTypeSchema(complexElement, eventAdapterService);
                if (childSchemaItem == null) {
                    // if the property is not valid, return null
                    return null;
                }

                if ((childSchemaItem instanceof SchemaItemAttribute) || (childSchemaItem instanceof SchemaElementSimple)) {
                    return null;
                }

                complexElement = (SchemaElementComplex) childSchemaItem;

                if (complexElement.isArray()) {
                    if ((property instanceof SimpleProperty) || (property instanceof DynamicSimpleProperty)) {
                        return null;
                    }
                }
            }

            getters.add(getter);
        }

        return new DOMNestedPropertyGetter(getters, new FragmentFactoryDOMGetter(eventAdapterService, eventType, propertyExpression));
    }

    public SchemaItem getPropertyTypeSchema(SchemaElementComplex parentComplexProperty, EventAdapterService eventAdapterService) {
        Property lastProperty = null;
        SchemaElementComplex complexElement = parentComplexProperty;

        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property property = it.next();
            lastProperty = property;

            if (it.hasNext()) {
                SchemaItem childSchemaItem = property.getPropertyTypeSchema(complexElement, eventAdapterService);
                if (childSchemaItem == null) {
                    // if the property is not valid, return null
                    return null;
                }

                if ((childSchemaItem instanceof SchemaItemAttribute) || (childSchemaItem instanceof SchemaElementSimple)) {
                    return null;
                }

                complexElement = (SchemaElementComplex) childSchemaItem;
            }
        }

        return lastProperty.getPropertyTypeSchema(complexElement, eventAdapterService);
    }

    public ObjectArrayEventPropertyGetter getGetterObjectArray(Map<String, Integer> indexPerProperty, Map<String, Object> nestableTypes, EventAdapterService eventAdapterService) {
        throw new UnsupportedOperationException("Object array nested property getter not implemented as not implicitly nestable");
    }

    public String getPropertyNameAtomic() {
        throw new UnsupportedOperationException("Nested properties do not provide an atomic property name");
    }

    private static String toPropertyEPL(List<Property> property, int startFromIndex) {
        String delimiter = "";
        StringWriter writer = new StringWriter();
        for (int i = startFromIndex; i < property.size(); i++) {
            writer.append(delimiter);
            property.get(i).toPropertyEPL(writer);
            delimiter = ".";
        }
        return writer.getBuffer().toString();
    }
}
