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
package com.espertech.esper.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.parse.ASTUtil;
import com.espertech.esper.epl.spec.ColumnDesc;
import com.espertech.esper.epl.spec.CreateSchemaDesc;
import com.espertech.esper.epl.util.EventRepresentationUtil;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventPropertyGetter;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.event.property.*;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;

public class EventTypeUtility {

    private static final Logger log = LoggerFactory.getLogger(EventTypeUtility.class);

    public static Map<String, Object> getPropertyTypesNonPrimitive(Map<String, Object> propertyTypesMayPrimitive) {
        boolean hasPrimitive = false;
        for (Map.Entry<String, Object> entry : propertyTypesMayPrimitive.entrySet()) {
            if (entry.getValue() instanceof Class && ((Class) entry.getValue()).isPrimitive()) {
                hasPrimitive = true;
                break;
            }
        }

        if (!hasPrimitive) {
            return propertyTypesMayPrimitive;
        }

        LinkedHashMap<String, Object> props = new LinkedHashMap<>(propertyTypesMayPrimitive);
        for (Map.Entry<String, Object> entry : propertyTypesMayPrimitive.entrySet()) {
            if (!(entry.getValue() instanceof Class)) {
                continue;
            }
            Class clazz = (Class) entry.getValue();
            if (!clazz.isPrimitive()) {
                continue;
            }
            props.put(entry.getKey(), JavaClassHelper.getBoxedType(clazz));
        }
        return props;
    }

    public static EventType requireEventType(String aspectCamel, String aspectName, EventAdapterService eventAdapterService, String optionalEventTypeName) throws ExprValidationException {
        if (optionalEventTypeName == null) {
            throw new ExprValidationException(aspectCamel + " '" + aspectName + "' returns EventBean-array but does not provide the event type name");
        }
        EventType eventType = eventAdapterService.getExistsTypeByName(optionalEventTypeName);
        if (eventType == null) {
            throw new ExprValidationException(aspectCamel + " '" + aspectName + "' returns event type '" + optionalEventTypeName + "' and the event type cannot be found");
        }
        return eventType;
    }

    public static Pair<EventType[], Set<EventType>> getSuperTypesDepthFirst(ConfigurationEventTypeWithSupertype optionalConfig, EventUnderlyingType representation, Map<String, ? extends EventType> nameToTypeMap) {
        return getSuperTypesDepthFirst(optionalConfig == null ? null : optionalConfig.getSuperTypes(), representation, nameToTypeMap);
    }

    public static Pair<EventType[], Set<EventType>> getSuperTypesDepthFirst(Set<String> superTypesSet, EventUnderlyingType representation, Map<String, ? extends EventType> nameToTypeMap)
            throws EventAdapterException {
        if (superTypesSet == null || superTypesSet.isEmpty()) {
            return new Pair<>(null, null);
        }

        EventType[] superTypes = new EventType[superTypesSet.size()];
        Set<EventType> deepSuperTypes = new LinkedHashSet<>();

        int count = 0;
        for (String superName : superTypesSet) {
            EventType type = nameToTypeMap.get(superName);
            if (type == null) {
                throw new EventAdapterException("Supertype by name '" + superName + "' could not be found");
            }
            if (representation == EventUnderlyingType.MAP) {
                if (!(type instanceof MapEventType)) {
                    throw new EventAdapterException("Supertype by name '" + superName + "' is not a Map, expected a Map event type as a supertype");
                }
            } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                if (!(type instanceof ObjectArrayEventType)) {
                    throw new EventAdapterException("Supertype by name '" + superName + "' is not an Object-array type, expected a Object-array event type as a supertype");
                }
            } else if (representation == EventUnderlyingType.AVRO) {
                if (!(type instanceof AvroSchemaEventType)) {
                    throw new EventAdapterException("Supertype by name '" + superName + "' is not an Avro type, expected a Avro event type as a supertype");
                }
            } else {
                throw new IllegalStateException("Unrecognized enum " + representation);
            }
            superTypes[count++] = type;
            deepSuperTypes.add(type);
            addRecursiveSupertypes(deepSuperTypes, type);
        }

        List<EventType> superTypesListDepthFirst = new ArrayList<>(deepSuperTypes);
        Collections.reverse(superTypesListDepthFirst);

        return new Pair<EventType[], Set<EventType>>(superTypes, new LinkedHashSet<>(superTypesListDepthFirst));
    }

    public static EventPropertyDescriptor getNestablePropertyDescriptor(EventType target, String propertyName) {
        EventPropertyDescriptor descriptor = target.getPropertyDescriptor(propertyName);
        if (descriptor != null) {
            return descriptor;
        }
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            return null;
        }
        // parse, can be an nested property
        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (property instanceof PropertyBase) {
            return target.getPropertyDescriptor(((PropertyBase) property).getPropertyNameAtomic());
        }
        if (!(property instanceof NestedProperty)) {
            return null;
        }
        NestedProperty nested = (NestedProperty) property;
        Deque<Property> properties = new ArrayDeque<Property>(nested.getProperties());
        return getNestablePropertyDescriptor(target, properties);
    }

    public static EventPropertyDescriptor getNestablePropertyDescriptor(EventType target, Deque<Property> stack) {

        Property topProperty = stack.removeFirst();
        if (stack.isEmpty()) {
            return target.getPropertyDescriptor(((PropertyBase) topProperty).getPropertyNameAtomic());
        }

        if (!(topProperty instanceof SimpleProperty)) {
            return null;
        }
        SimpleProperty simple = (SimpleProperty) topProperty;

        FragmentEventType fragmentEventType = target.getFragmentType(simple.getPropertyNameAtomic());
        if (fragmentEventType == null || fragmentEventType.getFragmentType() == null) {
            return null;
        }
        return getNestablePropertyDescriptor(fragmentEventType.getFragmentType(), stack);
    }

    public static LinkedHashMap<String, Object> buildType(List<ColumnDesc> columns, EventAdapterService eventAdapterService, Set<String> copyFrom, EngineImportService engineImportService) throws ExprValidationException {
        LinkedHashMap<String, Object> typing = new LinkedHashMap<String, Object>();
        Set<String> columnNames = new HashSet<String>();

        // Copy-from information gets added first as object-array appends information in well-defined order
        if (copyFrom != null && !copyFrom.isEmpty()) {
            for (String copyFromName : copyFrom) {
                EventType type = eventAdapterService.getExistsTypeByName(copyFromName);
                if (type == null) {
                    throw new ExprValidationException("Type by name '" + copyFromName + "' could not be located");
                }
                mergeType(typing, type, columnNames);
            }
        }

        for (ColumnDesc column : columns) {
            boolean added = columnNames.add(column.getName());
            if (!added) {
                throw new ExprValidationException("Duplicate column name '" + column.getName() + "'");
            }
            Object columnType = buildType(column, engineImportService);
            typing.put(column.getName(), columnType);
        }

        return typing;
    }

    public static Object buildType(ColumnDesc column, EngineImportService engineImportService) throws ExprValidationException {

        if (column.getType() == null) {
            return null;
        }

        if (column.isPrimitiveArray()) {
            Class primitive = JavaClassHelper.getPrimitiveClassForName(column.getType());
            if (primitive != null) {
                return Array.newInstance(primitive, 0).getClass();
            }
            throw new ExprValidationException("Type '" + column.getType() + "' is not a primitive type");
        }

        Class plain = JavaClassHelper.getClassForSimpleName(column.getType(), engineImportService.getClassForNameProvider());
        if (plain != null) {
            if (column.isArray()) {
                plain = Array.newInstance(plain, 0).getClass();
            }
            return plain;
        }

        // try imports first
        Class resolved = null;
        try {
            resolved = engineImportService.resolveClass(column.getType(), false);
        } catch (EngineImportException e) {
            // expected
        }

        // resolve from classpath when not found
        if (resolved == null) {
            try {
                resolved = JavaClassHelper.getClassForName(column.getType(), engineImportService.getClassForNameProvider());
            } catch (ClassNotFoundException e) {
                // expected
            }
        }

        // Handle resolved classes here
        if (resolved != null) {
            if (column.isArray()) {
                resolved = Array.newInstance(resolved, 0).getClass();
            }
            return resolved;
        }

        // Event types fall into here
        if (column.isArray()) {
            return column.getType() + "[]";
        }
        return column.getType();
    }

    private static void mergeType(Map<String, Object> typing, EventType typeToMerge, Set<String> columnNames)
            throws ExprValidationException {
        for (EventPropertyDescriptor prop : typeToMerge.getPropertyDescriptors()) {

            Object existing = typing.get(prop.getPropertyName());

            if (!prop.isFragment()) {
                Class assigned = prop.getPropertyType();
                if (existing != null && existing instanceof Class) {
                    if (JavaClassHelper.getBoxedType((Class) existing) != JavaClassHelper.getBoxedType(assigned)) {
                        throw new ExprValidationException("Type by name '" + typeToMerge.getName() + "' contributes property '" +
                                prop.getPropertyName() + "' defined as '" + JavaClassHelper.getClassNameFullyQualPretty(assigned) +
                                "' which overides the same property of type '" + JavaClassHelper.getClassNameFullyQualPretty((Class) existing) + "'");
                    }
                }
                typing.put(prop.getPropertyName(), prop.getPropertyType());
            } else {
                if (existing != null) {
                    throw new ExprValidationException("Property by name '" + prop.getPropertyName() + "' is defined twice by adding type '" + typeToMerge.getName() + "'");
                }

                FragmentEventType fragment = typeToMerge.getFragmentType(prop.getPropertyName());
                if (fragment == null) {
                    continue;
                }
                if (fragment.isIndexed()) {
                    typing.put(prop.getPropertyName(), new EventType[]{fragment.getFragmentType()});
                } else {
                    typing.put(prop.getPropertyName(), fragment.getFragmentType());
                }
            }

            columnNames.add(prop.getPropertyName());
        }
    }

    public static void validateTimestampProperties(EventType eventType, String startTimestampProperty, String endTimestampProperty)
            throws ConfigurationException {

        if (startTimestampProperty != null) {
            if (eventType.getGetter(startTimestampProperty) == null) {
                throw new ConfigurationException("Declared start timestamp property name '" + startTimestampProperty + "' was not found");
            }
            Class type = eventType.getPropertyType(startTimestampProperty);
            if (!JavaClassHelper.isDatetimeClass(type)) {
                throw new ConfigurationException("Declared start timestamp property '" + startTimestampProperty + "' is expected to return a Date, Calendar or long-typed value but returns '" + type.getName() + "'");
            }
        }

        if (endTimestampProperty != null) {
            if (startTimestampProperty == null) {
                throw new ConfigurationException("Declared end timestamp property requires that a start timestamp property is also declared");
            }
            if (eventType.getGetter(endTimestampProperty) == null) {
                throw new ConfigurationException("Declared end timestamp property name '" + endTimestampProperty + "' was not found");
            }
            Class type = eventType.getPropertyType(endTimestampProperty);
            if (!JavaClassHelper.isDatetimeClass(type)) {
                throw new ConfigurationException("Declared end timestamp property '" + endTimestampProperty + "' is expected to return a Date, Calendar or long-typed value but returns '" + type.getName() + "'");
            }
            Class startType = eventType.getPropertyType(startTimestampProperty);
            if (JavaClassHelper.getBoxedType(startType) != JavaClassHelper.getBoxedType(type)) {
                throw new ConfigurationException("Declared end timestamp property '" + endTimestampProperty + "' is expected to have the same property type as the start-timestamp property '" + startTimestampProperty + "'");
            }
        }
    }

    public static boolean isTypeOrSubTypeOf(EventType candidate, EventType superType) {

        if (candidate == superType) {
            return true;
        }

        if (candidate.getSuperTypes() != null) {
            for (Iterator<EventType> it = candidate.getDeepSuperTypes(); it.hasNext(); ) {
                if (it.next() == superType) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine among the Map-type properties which properties are Bean-type event type names,
     * rewrites these as Class-type instead so that they are configured as native property and do not require wrapping,
     * but may require unwrapping.
     *
     * @param typing              properties of map type
     * @param eventAdapterService event adapter service
     * @return compiled properties, same as original unless Bean-type event type names were specified.
     */
    public static Map<String, Object> compileMapTypeProperties(Map<String, Object> typing, EventAdapterService eventAdapterService) {
        Map<String, Object> compiled = new LinkedHashMap<String, Object>(typing);
        for (Map.Entry<String, Object> specifiedEntry : typing.entrySet()) {
            Object typeSpec = specifiedEntry.getValue();
            String nameSpec = specifiedEntry.getKey();

            if (typeSpec instanceof Class) {
                compiled.put(nameSpec, JavaClassHelper.getBoxedType((Class) typeSpec));
                continue;
            }

            if (!(typeSpec instanceof String)) {
                continue;
            }

            String typeNameSpec = (String) typeSpec;
            boolean isArray = EventTypeUtility.isPropertyArray(typeNameSpec);
            if (isArray) {
                typeNameSpec = EventTypeUtility.getPropertyRemoveArray(typeNameSpec);
            }

            EventType eventType = eventAdapterService.getExistsTypeByName(typeNameSpec);
            if (eventType == null || !(eventType instanceof BeanEventType)) {
                continue;
            }

            BeanEventType beanEventType = (BeanEventType) eventType;
            Class underlyingType = beanEventType.getUnderlyingType();
            if (isArray) {
                underlyingType = JavaClassHelper.getArrayType(underlyingType);
            }
            compiled.put(nameSpec, underlyingType);
        }
        return compiled;
    }

    /**
     * Returns true if the name indicates that the type is an array type.
     *
     * @param name the property name
     * @return true if array type
     */
    public static boolean isPropertyArray(String name) {
        return name.trim().endsWith("[]");
    }

    public static boolean isTypeOrSubTypeOf(String typeName, EventType sameTypeOrSubtype) {
        if (sameTypeOrSubtype.getName().equals(typeName)) {
            return true;
        }
        if (sameTypeOrSubtype.getSuperTypes() == null) {
            return false;
        }
        for (EventType superType : sameTypeOrSubtype.getSuperTypes()) {
            if (superType.getName().equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the property name without the array type extension, if present.
     *
     * @param name property name
     * @return property name with removed array extension name
     */
    public static String getPropertyRemoveArray(String name) {
        return name.replaceAll("\\[", "").replaceAll("\\]", "");
    }

    public static PropertySetDescriptor getNestableProperties(Map<String, Object> propertiesToAdd, EventAdapterService eventAdapterService, EventTypeNestableGetterFactory factory, EventType[] optionalSuperTypes)
            throws EPException {
        List<String> propertyNameList = new ArrayList<String>();
        List<EventPropertyDescriptor> propertyDescriptors = new ArrayList<EventPropertyDescriptor>();
        Map<String, Object> nestableTypes = new LinkedHashMap<String, Object>();
        Map<String, PropertySetDescriptorItem> propertyItems = new HashMap<String, PropertySetDescriptorItem>();

        // handle super-types first, such that the order of properties is well-defined from super-type to subtype
        if (optionalSuperTypes != null) {
            for (int i = 0; i < optionalSuperTypes.length; i++) {
                BaseNestableEventType superType = (BaseNestableEventType) optionalSuperTypes[i];
                for (String propertyName : superType.getPropertyNames()) {
                    if (nestableTypes.containsKey(propertyName)) {
                        continue;
                    }
                    propertyNameList.add(propertyName);
                }
                for (EventPropertyDescriptor descriptor : superType.getPropertyDescriptors()) {
                    if (nestableTypes.containsKey(descriptor.getPropertyName())) {
                        continue;
                    }
                    propertyDescriptors.add(descriptor);
                }

                propertyItems.putAll(superType.propertyItems);
                nestableTypes.putAll(superType.nestableTypes);
            }
        }

        nestableTypes.putAll(propertiesToAdd);

        // Initialize getters and names array: at this time we do not care about nested types,
        // these are handled at the time someone is asking for them
        for (Map.Entry<String, Object> entry : propertiesToAdd.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new EPException("Invalid type configuration: property name is not a String-type value");
            }
            String name = entry.getKey();

            // handle types that are String values
            if (entry.getValue() instanceof String) {
                String value = entry.getValue().toString().trim();
                Class clazz = JavaClassHelper.getPrimitiveClassForName(value);
                if (clazz != null) {
                    entry.setValue(clazz);
                }
            }

            if (entry.getValue() instanceof Class) {
                Class classType = (Class) entry.getValue();

                boolean isArray = classType.isArray();
                Class componentType = null;
                if (isArray) {
                    componentType = classType.getComponentType();
                }
                boolean isMapped = JavaClassHelper.isImplementsInterface(classType, Map.class);
                if (isMapped) {
                    componentType = Object.class; // Cannot determine the type at runtime
                }
                boolean isFragment = JavaClassHelper.isFragmentableType(classType);
                BeanEventType nativeFragmentType = null;
                FragmentEventType fragmentType = null;
                if (isFragment) {
                    fragmentType = EventBeanUtility.createNativeFragmentType(classType, null, eventAdapterService);
                    if (fragmentType != null) {
                        nativeFragmentType = (BeanEventType) fragmentType.getFragmentType();
                    } else {
                        isFragment = false;
                    }
                }
                EventPropertyGetterSPI getter = factory.getGetterProperty(name, nativeFragmentType, eventAdapterService);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, classType, componentType, false, false, isArray, isMapped, isFragment);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, classType, getter, fragmentType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            // A null-type is also allowed
            if (entry.getValue() == null) {
                EventPropertyGetterSPI getter = factory.getGetterProperty(name, null, null);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, null, null, false, false, false, false, false);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, null, getter, null);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            // Add Map itself as a property
            if (entry.getValue() instanceof Map) {
                EventPropertyGetterSPI getter = factory.getGetterProperty(name, null, null);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, Map.class, null, false, false, false, true, false);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, Map.class, getter, null);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            if (entry.getValue() instanceof EventType) {
                // Add EventType itself as a property
                EventType eventType = (EventType) entry.getValue();
                EventPropertyGetterSPI getter = factory.getGetterEventBean(name, eventType.getUnderlyingType());
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, eventType.getUnderlyingType(), null, false, false, false, false, true);
                FragmentEventType fragmentEventType = new FragmentEventType(eventType, false, false);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, eventType.getUnderlyingType(), getter, fragmentEventType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            if (entry.getValue() instanceof EventType[]) {
                // Add EventType array itself as a property, type is expected to be first array element
                EventType eventType = ((EventType[]) entry.getValue())[0];
                Object prototypeArray = Array.newInstance(eventType.getUnderlyingType(), 0);
                EventPropertyGetterSPI getter = factory.getGetterEventBeanArray(name, eventType);
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, prototypeArray.getClass(), eventType.getUnderlyingType(), false, false, true, false, true);
                FragmentEventType fragmentEventType = new FragmentEventType(eventType, true, false);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, prototypeArray.getClass(), getter, fragmentEventType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            if (entry.getValue() instanceof String) {
                String propertyName = entry.getValue().toString();
                boolean isArray = EventTypeUtility.isPropertyArray(propertyName);
                if (isArray) {
                    propertyName = EventTypeUtility.getPropertyRemoveArray(propertyName);
                }

                // Add EventType itself as a property
                EventType eventType = eventAdapterService.getExistsTypeByName(propertyName);
                if (!(eventType instanceof BaseNestableEventType) && !(eventType instanceof BeanEventType)) {
                    throw new EPException("Nestable type configuration encountered an unexpected property type name '"
                            + entry.getValue() + "' for property '" + name + "', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type");
                }

                Class underlyingType = eventType.getUnderlyingType();
                Class propertyComponentType = null;
                if (isArray) {
                    propertyComponentType = underlyingType;
                    if (underlyingType != Object[].class) {
                        underlyingType = Array.newInstance(underlyingType, 0).getClass();
                    }
                }
                EventPropertyGetterSPI getter;
                if (!isArray) {
                    getter = factory.getGetterBeanNested(name, eventType, eventAdapterService);
                } else {
                    getter = factory.getGetterBeanNestedArray(name, eventType, eventAdapterService);
                }
                EventPropertyDescriptor descriptor = new EventPropertyDescriptor(name, underlyingType, propertyComponentType, false, false, isArray, false, true);
                FragmentEventType fragmentEventType = new FragmentEventType(eventType, isArray, false);
                PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, underlyingType, getter, fragmentEventType);
                propertyNameList.add(name);
                propertyDescriptors.add(descriptor);
                propertyItems.put(name, item);
                continue;
            }

            generateExceptionNestedProp(name, entry.getValue());
        }

        return new PropertySetDescriptor(propertyNameList, propertyDescriptors, propertyItems, nestableTypes);
    }

    private static void generateExceptionNestedProp(String name, Object value) throws EPException {
        String clazzName = (value == null) ? "null" : value.getClass().getSimpleName();
        throw new EPException("Nestable type configuration encountered an unexpected property type of '"
                + clazzName + "' for property '" + name + "', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type");
    }

    public static Class getNestablePropertyType(String propertyName,
                                                Map<String, PropertySetDescriptorItem> simplePropertyTypes,
                                                Map<String, Object> nestableTypes,
                                                EventAdapterService eventAdapterService) {
        PropertySetDescriptorItem item = simplePropertyTypes.get(ASTUtil.unescapeDot(propertyName));
        if (item != null) {
            return item.getSimplePropertyType();
        }

        // see if this is a nested property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            // dynamic simple property
            if (propertyName.endsWith("?")) {
                return Object.class;
            }

            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);

            if (property instanceof SimpleProperty) {
                PropertySetDescriptorItem propitem = simplePropertyTypes.get(propertyName);
                if (propitem != null) {
                    return propitem.getSimplePropertyType();
                }
                return null;
            }

            if (property instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) property;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                } else if (type instanceof EventType[]) {
                    return ((EventType[]) type)[0].getUnderlyingType();
                } else if (type instanceof String) {
                    String propTypeName = type.toString();
                    boolean isArray = EventTypeUtility.isPropertyArray(propTypeName);
                    if (isArray) {
                        propTypeName = EventTypeUtility.getPropertyRemoveArray(propTypeName);
                    }
                    EventType innerType = eventAdapterService.getExistsTypeByName(propTypeName);
                    return innerType == null ? null : innerType.getUnderlyingType();
                }
                if (!(type instanceof Class)) {
                    return null;
                }
                if (!((Class) type).isArray()) {
                    return null;
                }
                // its an array
                return ((Class) type).getComponentType();
            } else if (property instanceof MappedProperty) {
                MappedProperty mappedProp = (MappedProperty) property;
                Object type = nestableTypes.get(mappedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                if (type instanceof Class) {
                    if (JavaClassHelper.isImplementsInterface((Class) type, Map.class)) {
                        return Object.class;
                    }
                }
                return null;
            } else {
                return null;
            }
        }

        // Map event types allow 2 types of properties inside:
        //   - a property that is a Java object is interrogated via bean property getters and BeanEventType
        //   - a property that is a Map itself is interrogated via map property getters
        // The property getters therefore act on

        // Take apart the nested property into a map key and a nested value class property name
        String propertyMap = ASTUtil.unescapeDot(propertyName.substring(0, index));
        String propertyNested = propertyName.substring(index + 1, propertyName.length());
        boolean isRootedDynamic = false;

        // If the property is dynamic, remove the ? since the property type is defined without
        if (propertyMap.endsWith("?")) {
            propertyMap = propertyMap.substring(0, propertyMap.length() - 1);
            isRootedDynamic = true;
        }

        Object nestedType = nestableTypes.get(propertyMap);
        if (nestedType == null) {
            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyMap);
            if (property instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) property;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                // handle map-in-map case
                if (type instanceof String) {
                    String propTypeName = type.toString();
                    boolean isArray = EventTypeUtility.isPropertyArray(propTypeName);
                    if (isArray) {
                        propTypeName = EventTypeUtility.getPropertyRemoveArray(propTypeName);
                    }
                    EventType innerType = eventAdapterService.getExistsTypeByName(propTypeName);
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    return innerType.getPropertyType(propertyNested);
                } else if (type instanceof EventType[]) {
                    // handle eventtype[] in map
                    EventType innerType = ((EventType[]) type)[0];
                    return innerType.getPropertyType(propertyNested);
                } else {
                    // handle array class in map case
                    if (!(type instanceof Class)) {
                        return null;
                    }
                    if (!((Class) type).isArray()) {
                        return null;
                    }
                    Class componentType = ((Class) type).getComponentType();
                    EventType nestedEventType = eventAdapterService.addBeanType(componentType.getName(), componentType, false, false, false);
                    return nestedEventType.getPropertyType(propertyNested);
                }
            } else if (property instanceof MappedProperty) {
                return null;    // Since no type information is available for the property
            } else {
                return isRootedDynamic ? Object.class : null;
            }
        }

        // If there is a map value in the map, return the Object value if this is a dynamic property
        if (nestedType == Map.class) {
            Property prop = PropertyParser.parseAndWalk(propertyNested, isRootedDynamic);
            return isRootedDynamic ? Object.class : JavaClassHelper.getBoxedType(prop.getPropertyTypeMap(null, eventAdapterService));   // we don't have a definition of the nested props
        } else if (nestedType instanceof Map) {
            Property prop = PropertyParser.parseAndWalk(propertyNested, isRootedDynamic);
            Map nestedTypes = (Map) nestedType;
            return isRootedDynamic ? Object.class : JavaClassHelper.getBoxedType(prop.getPropertyTypeMap(nestedTypes, eventAdapterService));
        } else if (nestedType instanceof Class) {
            Class simpleClass = (Class) nestedType;
            if (JavaClassHelper.isJavaBuiltinDataType(simpleClass)) {
                return null;
            }
            if (simpleClass.isArray() &&
                    (JavaClassHelper.isJavaBuiltinDataType(simpleClass.getComponentType()) || simpleClass.getComponentType() == Object.class)) {
                return null;
            }
            EventType nestedEventType = eventAdapterService.addBeanType(simpleClass.getName(), simpleClass, false, false, false);
            return isRootedDynamic ? Object.class : JavaClassHelper.getBoxedType(nestedEventType.getPropertyType(propertyNested));
        } else if (nestedType instanceof EventType) {
            EventType innerType = (EventType) nestedType;
            return isRootedDynamic ? Object.class : JavaClassHelper.getBoxedType(innerType.getPropertyType(propertyNested));
        } else if (nestedType instanceof EventType[]) {
            return null;    // requires indexed property
        } else if (nestedType instanceof String) {
            String nestedName = nestedType.toString();
            boolean isArray = EventTypeUtility.isPropertyArray(nestedName);
            if (isArray) {
                nestedName = EventTypeUtility.getPropertyRemoveArray(nestedName);
            }
            EventType innerType = eventAdapterService.getExistsTypeByName(nestedName);
            if (!(innerType instanceof BaseNestableEventType)) {
                return null;
            }
            return isRootedDynamic ? Object.class : JavaClassHelper.getBoxedType(innerType.getPropertyType(propertyNested));
        } else {
            String message = "Nestable map type configuration encountered an unexpected value type of '"
                    + nestedType.getClass() + "' for property '" + propertyName + "', expected Class, Map.class or Map<String, Object> as value type";
            throw new PropertyAccessException(message);
        }
    }

    public static EventPropertyGetterSPI getNestableGetter(String propertyName,
                                                           Map<String, PropertySetDescriptorItem> propertyGetters,
                                                           Map<String, EventPropertyGetterSPI> propertyGetterCache,
                                                           Map<String, Object> nestableTypes,
                                                           EventAdapterService eventAdapterService,
                                                           EventTypeNestableGetterFactory factory,
                                                           boolean isObjectArray) {
        EventPropertyGetterSPI cachedGetter = propertyGetterCache.get(propertyName);
        if (cachedGetter != null) {
            return cachedGetter;
        }

        String unescapePropName = ASTUtil.unescapeDot(propertyName);
        PropertySetDescriptorItem item = propertyGetters.get(unescapePropName);
        if (item != null) {
            EventPropertyGetterSPI getter = item.getPropertyGetter();
            propertyGetterCache.put(propertyName, getter);
            return getter;
        }

        // see if this is a nested property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
            if (prop instanceof DynamicProperty) {
                EventPropertyGetterSPI getterDyn = factory.getPropertyProvidedGetter(nestableTypes, propertyName, prop, eventAdapterService);
                propertyGetterCache.put(propertyName, getterDyn);
                return getterDyn;
            } else if (prop instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) prop;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                } else if (type instanceof EventType[]) {
                    EventPropertyGetterSPI getterArr = factory.getGetterIndexedEventBean(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex());
                    propertyGetterCache.put(propertyName, getterArr);
                    return getterArr;
                } else if (type instanceof String) {
                    String nestedTypeName = type.toString();
                    boolean isArray = EventTypeUtility.isPropertyArray(nestedTypeName);
                    if (isArray) {
                        nestedTypeName = EventTypeUtility.getPropertyRemoveArray(nestedTypeName);
                    }
                    EventType innerType = eventAdapterService.getExistsTypeByName(nestedTypeName);
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    EventPropertyGetterSPI typeGetter;
                    if (!isArray) {
                        typeGetter = factory.getGetterBeanNested(indexedProp.getPropertyNameAtomic(), innerType, eventAdapterService);
                    } else {
                        typeGetter = factory.getGetterIndexedUnderlyingArray(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), eventAdapterService, innerType);
                    }
                    propertyGetterCache.put(propertyName, typeGetter);
                    return typeGetter;
                }
                // handle map type name in map
                if (!(type instanceof Class)) {
                    return null;
                }
                if (!((Class) type).isArray()) {
                    return null;
                }

                // its an array
                Class componentType = ((Class) type).getComponentType();
                EventPropertyGetterSPI indexedGetter = factory.getGetterIndexedPOJO(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), eventAdapterService, componentType);
                propertyGetterCache.put(propertyName, indexedGetter);
                return indexedGetter;
            } else if (prop instanceof MappedProperty) {
                MappedProperty mappedProp = (MappedProperty) prop;
                Object type = nestableTypes.get(mappedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                if (type instanceof Class) {
                    if (JavaClassHelper.isImplementsInterface((Class) type, Map.class)) {
                        return factory.getGetterMappedProperty(mappedProp.getPropertyNameAtomic(), mappedProp.getKey());
                    }
                }
                return null;
            } else {
                return null;
            }
        }

        // Take apart the nested property into a map key and a nested value class property name
        String propertyMap = ASTUtil.unescapeDot(propertyName.substring(0, index));
        String propertyNested = propertyName.substring(index + 1, propertyName.length());
        boolean isRootedDynamic = false;

        // If the property is dynamic, remove the ? since the property type is defined without
        if (propertyMap.endsWith("?")) {
            propertyMap = propertyMap.substring(0, propertyMap.length() - 1);
            isRootedDynamic = true;
        }

        Object nestedType = nestableTypes.get(propertyMap);
        if (nestedType == null) {
            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyMap);
            if (property instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) property;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                if (type instanceof String) {
                    String nestedTypeName = type.toString();
                    boolean isArray = EventTypeUtility.isPropertyArray(nestedTypeName);
                    if (isArray) {
                        nestedTypeName = EventTypeUtility.getPropertyRemoveArray(nestedTypeName);
                    }
                    EventTypeSPI innerType = (EventTypeSPI) eventAdapterService.getExistsTypeByName(nestedTypeName);
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    EventPropertyGetterSPI typeGetter;
                    if (!isArray) {
                        typeGetter = factory.getGetterNestedEntryBean(propertyMap, innerType.getGetter(propertyNested), innerType, eventAdapterService);
                    } else {
                        EventPropertyGetterSPI innerGetter = innerType.getGetterSPI(propertyNested);
                        if (innerGetter == null) {
                            return null;
                        }
                        typeGetter = factory.getGetterNestedEntryBeanArray(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), innerGetter, innerType, eventAdapterService);
                    }
                    propertyGetterCache.put(propertyName, typeGetter);
                    return typeGetter;
                } else if (type instanceof EventType[]) {
                    EventTypeSPI componentType = (EventTypeSPI) ((EventType[]) type)[0];
                    EventPropertyGetterSPI nestedGetter = componentType.getGetterSPI(propertyNested);
                    if (nestedGetter == null) {
                        return null;
                    }
                    EventPropertyGetterSPI typeGetter = factory.getGetterIndexedEntryEventBeanArrayElement(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), nestedGetter);
                    propertyGetterCache.put(propertyName, typeGetter);
                    return typeGetter;
                } else {
                    if (!(type instanceof Class)) {
                        return null;
                    }
                    if (!((Class) type).isArray()) {
                        return null;
                    }
                    Class componentType = ((Class) type).getComponentType();
                    EventTypeSPI nestedEventType = (EventTypeSPI) eventAdapterService.addBeanType(componentType.getName(), componentType, false, false, false);

                    final BeanEventPropertyGetter nestedGetter = (BeanEventPropertyGetter) nestedEventType.getGetterSPI(propertyNested);
                    if (nestedGetter == null) {
                        return null;
                    }
                    Class propertyTypeGetter = nestedEventType.getPropertyType(propertyNested);
                    // construct getter for nested property
                    EventPropertyGetterSPI indexGetter = factory.getGetterIndexedEntryPOJO(indexedProp.getPropertyNameAtomic(), indexedProp.getIndex(), nestedGetter, eventAdapterService, propertyTypeGetter);
                    propertyGetterCache.put(propertyName, indexGetter);
                    return indexGetter;
                }
            } else if (property instanceof MappedProperty) {
                return null;    // Since no type information is available for the property
            } else {
                if (isRootedDynamic) {
                    Property prop = PropertyParser.parseAndWalk(propertyNested, true);
                    if (!isObjectArray) {
                        EventPropertyGetterSPI getterNested = prop.getGetterMap(null, eventAdapterService);
                        EventPropertyGetterSPI dynamicGetter = factory.getGetterNestedPropertyProvidedGetterDynamic(nestableTypes, propertyMap, getterNested, eventAdapterService);
                        propertyGetterCache.put(propertyName, dynamicGetter);
                        return dynamicGetter;
                    }
                    return null;
                }
                return null;
            }
        }

        // The map contains another map, we resolve the property dynamically
        if (nestedType == Map.class) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyNested);
            MapEventPropertyGetter getterNestedMap = prop.getGetterMap(null, eventAdapterService);
            if (getterNestedMap == null) {
                return null;
            }
            EventPropertyGetterSPI mapGetter = factory.getGetterNestedMapProp(propertyMap, getterNestedMap);
            propertyGetterCache.put(propertyName, mapGetter);
            return mapGetter;
        } else if (nestedType instanceof Map) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyNested);
            Map nestedTypes = (Map) nestedType;
            MapEventPropertyGetter getterNestedMap = prop.getGetterMap(nestedTypes, eventAdapterService);
            if (getterNestedMap == null) {
                return null;
            }
            EventPropertyGetterSPI mapGetter = factory.getGetterNestedMapProp(propertyMap, getterNestedMap);
            propertyGetterCache.put(propertyName, mapGetter);
            return mapGetter;
        } else if (nestedType instanceof Class) {
            // ask the nested class to resolve the property
            Class simpleClass = (Class) nestedType;
            if (simpleClass.isArray()) {
                return null;
            }
            EventTypeSPI nestedEventType = (EventTypeSPI) eventAdapterService.addBeanType(simpleClass.getName(), simpleClass, false, false, false);
            final BeanEventPropertyGetter nestedGetter = (BeanEventPropertyGetter) nestedEventType.getGetterSPI(propertyNested);
            if (nestedGetter == null) {
                return null;
            }

            Class propertyType;
            Class propertyComponentType;
            EventPropertyDescriptor desc = nestedEventType.getPropertyDescriptor(propertyNested);
            if (desc == null) {
                propertyType = nestedEventType.getPropertyType(propertyNested);
                propertyComponentType = propertyType.isArray() ? propertyType.getComponentType() : JavaClassHelper.getGenericType(propertyType, 0);
            } else {
                propertyType = desc.getPropertyType();
                propertyComponentType = desc.getPropertyComponentType();
            }

            // construct getter for nested property
            EventPropertyGetterSPI getter = factory.getGetterNestedPOJOProp(propertyMap, nestedGetter, eventAdapterService, propertyType, propertyComponentType);
            propertyGetterCache.put(propertyName, getter);
            return getter;
        } else if (nestedType instanceof EventType) {
            // ask the nested class to resolve the property
            EventTypeSPI innerType = (EventTypeSPI) nestedType;
            final EventPropertyGetterSPI nestedGetter = innerType.getGetterSPI(propertyNested);
            if (nestedGetter == null) {
                return null;
            }

            // construct getter for nested property
            EventPropertyGetterSPI getter = factory.getGetterNestedEventBean(propertyMap, nestedGetter);
            propertyGetterCache.put(propertyName, getter);
            return getter;
        } else if (nestedType instanceof EventType[]) {
            EventType[] typeArray = (EventType[]) nestedType;
            EventPropertyGetterSPI beanArrGetter = factory.getGetterEventBeanArray(propertyMap, typeArray[0]);
            propertyGetterCache.put(propertyName, beanArrGetter);
            return beanArrGetter;
        } else if (nestedType instanceof String) {
            String nestedName = nestedType.toString();
            boolean isArray = EventTypeUtility.isPropertyArray(nestedName);
            if (isArray) {
                nestedName = EventTypeUtility.getPropertyRemoveArray(nestedName);
            }
            EventType innerType = eventAdapterService.getExistsTypeByName(nestedName);
            if (!(innerType instanceof BaseNestableEventType)) {
                return null;
            }
            EventPropertyGetterSPI innerGetter = ((EventTypeSPI) innerType).getGetterSPI(propertyNested);
            if (innerGetter == null) {
                return null;
            }
            EventPropertyGetterSPI outerGetter;
            if (!isArray) {
                outerGetter = factory.getGetterNestedEntryBean(propertyMap, innerGetter, innerType, eventAdapterService);
            } else {
                outerGetter = factory.getGetterNestedEntryBeanArray(propertyMap, 0, innerGetter, innerType, eventAdapterService);
            }
            propertyGetterCache.put(propertyName, outerGetter);
            return outerGetter;
        } else {
            String message = "Nestable type configuration encountered an unexpected value type of '"
                    + nestedType.getClass() + " for property '" + propertyName + "', expected Class, Map.class or Map<String, Object> as value type";
            throw new PropertyAccessException(message);
        }
    }

    public static LinkedHashMap<String, Object> validateObjectArrayDef(String[] propertyNames, Object[] propertyTypes) {
        if (propertyNames.length != propertyTypes.length) {
            throw new ConfigurationException("Number of property names and property types do not match, found " + propertyNames.length + " property names and " +
                    propertyTypes.length + " property types");
        }

        // validate property names for no-duplicates
        Set<String> propertyNamesSet = new HashSet<String>();
        LinkedHashMap<String, Object> propertyTypesMap = new LinkedHashMap<String, Object>();
        for (int i = 0; i < propertyNames.length; i++) {
            String propertyName = propertyNames[i];
            if (propertyNamesSet.contains(propertyName)) {     // duplicate prop check
                throw new ConfigurationException("Property '" + propertyName + "' is listed twice in the type definition");
            }
            propertyNamesSet.add(propertyName);
            propertyTypesMap.put(propertyName, propertyTypes[i]);
        }
        return propertyTypesMap;
    }

    public static EventType createNonVariantType(boolean isAnonymous, CreateSchemaDesc spec, Annotation[] annotations, ConfigurationInformation configSnapshot, EventAdapterService eventAdapterService, EngineImportService engineImportService)
            throws ExprValidationException {
        if (spec.getAssignedType() == CreateSchemaDesc.AssignedType.VARIANT) {
            throw new IllegalStateException("Variant type is not allowed in this context");
        }

        EventType eventType;
        if (spec.getTypes().isEmpty()) {
            EventUnderlyingType representation = EventRepresentationUtil.getRepresentation(annotations, configSnapshot, spec.getAssignedType());
            Map<String, Object> typing = EventTypeUtility.buildType(spec.getColumns(), eventAdapterService, spec.getCopyFrom(), engineImportService);
            Map<String, Object> compiledTyping = EventTypeUtility.compileMapTypeProperties(typing, eventAdapterService);

            ConfigurationEventTypeWithSupertype config;
            if (representation == EventUnderlyingType.MAP) {
                config = new ConfigurationEventTypeMap();
            } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                config = new ConfigurationEventTypeObjectArray();
            } else if (representation == EventUnderlyingType.AVRO) {
                config = new ConfigurationEventTypeAvro();
            } else {
                throw new IllegalStateException("Unrecognized representation '" + representation + "'");
            }

            if (spec.getInherits() != null) {
                config.getSuperTypes().addAll(spec.getInherits());
            }
            config.setStartTimestampPropertyName(spec.getStartTimestampProperty());
            config.setEndTimestampPropertyName(spec.getEndTimestampProperty());

            if (representation == EventUnderlyingType.MAP) {
                if (isAnonymous) {
                    eventType = eventAdapterService.createAnonymousMapType(spec.getSchemaName(), compiledTyping, true);
                } else {
                    eventType = eventAdapterService.addNestableMapType(spec.getSchemaName(), compiledTyping, (ConfigurationEventTypeMap) config, false, false, true, false, false);
                }
            } else if (representation == EventUnderlyingType.OBJECTARRAY) {
                if (isAnonymous) {
                    eventType = eventAdapterService.createAnonymousObjectArrayType(spec.getSchemaName(), compiledTyping);
                } else {
                    eventType = eventAdapterService.addNestableObjectArrayType(spec.getSchemaName(), compiledTyping, (ConfigurationEventTypeObjectArray) config, false, false, true, false, false, false, null);
                }
            } else if (representation == EventUnderlyingType.AVRO) {
                if (isAnonymous) {
                    eventType = eventAdapterService.createAnonymousAvroType(spec.getSchemaName(), compiledTyping, annotations, null, null);
                } else {
                    eventType = eventAdapterService.addAvroType(spec.getSchemaName(), compiledTyping, false, false, true, false, false, annotations, (ConfigurationEventTypeAvro) config, null, null);
                }
            } else {
                throw new IllegalStateException("Unrecognized representation " + representation);
            }
        } else {
            // Java Object/Bean/POJO type definition
            if (spec.getCopyFrom() != null && !spec.getCopyFrom().isEmpty()) {
                throw new ExprValidationException("Copy-from types are not allowed with class-provided types");
            }
            if (spec.getTypes().size() != 1) {
                throw new IllegalStateException("Multiple types provided");
            }
            String typeName = spec.getTypes().iterator().next();
            try {
                // use the existing configuration, if any, possibly adding the start and end timestamps
                ConfigurationEventTypeLegacy config = eventAdapterService.getClassLegacyConfigs(typeName);
                if (spec.getStartTimestampProperty() != null || spec.getEndTimestampProperty() != null) {
                    if (config == null) {
                        config = new ConfigurationEventTypeLegacy();
                    }
                    config.setStartTimestampPropertyName(spec.getStartTimestampProperty());
                    config.setEndTimestampPropertyName(spec.getEndTimestampProperty());
                    eventAdapterService.setClassLegacyConfigs(Collections.singletonMap(typeName, config));
                }
                if (isAnonymous) {
                    String className = spec.getTypes().iterator().next();
                    Class clazz;
                    try {
                        clazz = engineImportService.resolveClass(className, false);
                    } catch (EngineImportException e) {
                        throw new ExprValidationException("Failed to resolve class '" + className + "': " + e.getMessage(), e);
                    }
                    eventType = eventAdapterService.createAnonymousBeanType(spec.getSchemaName(), clazz);
                } else {
                    eventType = eventAdapterService.addBeanType(spec.getSchemaName(), spec.getTypes().iterator().next(), false, false, false, true);
                }
            } catch (EventAdapterException ex) {
                Class clazz;
                try {
                    clazz = engineImportService.resolveClass(typeName, false);
                    if (isAnonymous) {
                        eventType = eventAdapterService.createAnonymousBeanType(spec.getSchemaName(), clazz);
                    } else {
                        eventType = eventAdapterService.addBeanType(spec.getSchemaName(), clazz, false, false, true);
                    }
                } catch (EngineImportException e) {
                    log.debug("Engine import failed to resolve event type '" + typeName + "'");
                    throw ex;
                }
            }
        }
        return eventType;
    }

    public static WriteablePropertyDescriptor findWritable(String propertyName, Set<WriteablePropertyDescriptor> writables) {
        for (WriteablePropertyDescriptor writable : writables) {
            if (writable.getPropertyName().equals(propertyName)) {
                return writable;
            }
        }
        return null;
    }

    public static TimestampPropertyDesc validatedDetermineTimestampProps(EventType type, String startProposed, String endProposed, EventType[] superTypes)
            throws EPException {
        // determine start&end timestamp as inherited
        String startTimestampPropertyName = startProposed;
        String endTimestampPropertyName = endProposed;

        if (superTypes != null && superTypes.length > 0) {
            for (EventType superType : superTypes) {
                if (superType.getStartTimestampPropertyName() != null) {
                    if (startTimestampPropertyName != null && !startTimestampPropertyName.equals(superType.getStartTimestampPropertyName())) {
                        throw getExceptionTimestampInherited("start", startTimestampPropertyName, superType.getStartTimestampPropertyName(), superType);
                    }
                    startTimestampPropertyName = superType.getStartTimestampPropertyName();
                }
                if (superType.getEndTimestampPropertyName() != null) {
                    if (endTimestampPropertyName != null && !endTimestampPropertyName.equals(superType.getEndTimestampPropertyName())) {
                        throw getExceptionTimestampInherited("end", endTimestampPropertyName, superType.getEndTimestampPropertyName(), superType);
                    }
                    endTimestampPropertyName = superType.getEndTimestampPropertyName();
                }
            }
        }

        validateTimestampProperties(type, startTimestampPropertyName, endTimestampPropertyName);
        return new TimestampPropertyDesc(startTimestampPropertyName, endTimestampPropertyName);
    }

    private static EPException getExceptionTimestampInherited(String tstype, String firstName, String secondName, EventType superType) {
        String message = "Event type declares " + tstype + " timestamp as property '" + firstName + "' however inherited event type '" + superType.getName() +
                "' declares " + tstype + " timestamp as property '" + secondName + "'";
        return new EPException(message);
    }

    private static void addRecursiveSupertypes(Set<EventType> superTypes, EventType child) {
        if (child.getSuperTypes() != null) {
            for (int i = 0; i < child.getSuperTypes().length; i++) {
                superTypes.add(child.getSuperTypes()[i]);
                addRecursiveSupertypes(superTypes, child.getSuperTypes()[i]);
            }
        }
    }

    public static String disallowedAtTypeMessage() {
        return "The @type annotation is only allowed when the invocation target returns EventBean instances";
    }

    public static void validateEventBeanClassVisibility(Class clazz) {
        if (!Modifier.isPublic(clazz.getModifiers())) {
            throw new EventAdapterException("Event class '" + clazz.getName() + "' does not have public visibility");
        }
    }

    public static class TimestampPropertyDesc {
        private final String start;
        private final String end;

        public TimestampPropertyDesc(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }
    }
}
