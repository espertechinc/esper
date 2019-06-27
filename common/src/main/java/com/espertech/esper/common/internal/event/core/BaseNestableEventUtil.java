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
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.map.MapEventPropertyGetter;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.MappedProperty;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.event.util.CodegenLegoPropertyBeanOrUnd;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.common.internal.util.JavaClassHelper.getClassNameFullyQualPretty;

public class BaseNestableEventUtil {

    public static MapEventType makeMapTypeCompileTime(EventTypeMetadata metadata,
                                                      Map<String, Object> propertyTypes,
                                                      EventType[] optionalSuperTypes,
                                                      Set<EventType> optionalDeepSupertypes,
                                                      String startTimestampPropertyName,
                                                      String endTimestampPropertyName,
                                                      BeanEventTypeFactory beanEventTypeFactory,
                                                      EventTypeCompileTimeResolver eventTypeCompileTimeResolver) {
        if (metadata.getApplicationType() != EventTypeApplicationType.MAP) {
            throw new IllegalArgumentException("Invalid application type " + metadata.getApplicationType());
        }
        Map<String, Object> verified = resolvePropertyTypes(propertyTypes, eventTypeCompileTimeResolver);
        return new MapEventType(metadata, verified, optionalSuperTypes, optionalDeepSupertypes, startTimestampPropertyName, endTimestampPropertyName, beanEventTypeFactory);
    }

    public static ObjectArrayEventType makeOATypeCompileTime(EventTypeMetadata metadata, Map<String, Object> properyTypes, EventType[] optionalSuperTypes, Set<EventType> optionalDeepSupertypes, String startTimestampName, String endTimestampName, BeanEventTypeFactory beanEventTypeFactory, EventTypeCompileTimeResolver eventTypeCompileTimeResolver) {
        if (metadata.getApplicationType() != EventTypeApplicationType.OBJECTARR) {
            throw new IllegalArgumentException("Invalid application type " + metadata.getApplicationType());
        }
        Map<String, Object> verified = resolvePropertyTypes(properyTypes, eventTypeCompileTimeResolver);
        return new ObjectArrayEventType(metadata, verified, optionalSuperTypes, optionalDeepSupertypes, startTimestampName, endTimestampName, beanEventTypeFactory);
    }

    public static LinkedHashMap<String, Object> resolvePropertyTypes(Map<String, Object> propertyTypes, EventTypeNameResolver eventTypeNameResolver) {
        LinkedHashMap<String, Object> verified = new LinkedHashMap<>();
        for (Map.Entry<String, Object> prop : propertyTypes.entrySet()) {
            String propertyName = prop.getKey();
            Object propertyType = prop.getValue();

            if (propertyType instanceof Class || propertyType instanceof EventType || propertyType == null || propertyType instanceof TypeBeanOrUnderlying) {
                verified.put(propertyName, propertyType);
                continue;
            }

            if (propertyType instanceof EventType[]) {
                EventType[] types = (EventType[]) propertyType;
                if (types.length != 1 || types[0] == null) {
                    throw new IllegalArgumentException("Invalid null event type array");
                }
                verified.put(propertyName, propertyType);
                continue;
            }

            if (propertyType instanceof TypeBeanOrUnderlying[]) {
                TypeBeanOrUnderlying[] types = (TypeBeanOrUnderlying[]) propertyType;
                if (types.length != 1 || types[0] == null) {
                    throw new IllegalArgumentException("Invalid null event type array");
                }
                verified.put(propertyName, propertyType);
                continue;
            }

            if (propertyType instanceof Map) {
                Map<String, Object> inner = resolvePropertyTypes((Map<String, Object>) propertyType, eventTypeNameResolver);
                verified.put(propertyName, inner);
                continue;
            }

            if (!(propertyType instanceof String)) {
                throw makeUnexpectedTypeException(propertyType.toString(), propertyName);
            }

            String propertyTypeName = propertyType.toString();
            boolean isArray = EventTypeUtility.isPropertyArray(propertyTypeName);
            if (isArray) {
                propertyTypeName = EventTypeUtility.getPropertyRemoveArray(propertyTypeName);
            }

            EventType eventType = eventTypeNameResolver.getTypeByName(propertyTypeName);
            if (!(eventType instanceof BaseNestableEventType) && !(eventType instanceof BeanEventType)) {

                Class clazz = JavaClassHelper.getPrimitiveClassForName(propertyTypeName);
                if (clazz != null) {
                    verified.put(propertyName, clazz);
                    continue;
                }
                throw makeUnexpectedTypeException(propertyTypeName, propertyName);
            }

            if (eventType instanceof BaseNestableEventType) {
                Object type = !isArray ? new TypeBeanOrUnderlying(eventType) : new TypeBeanOrUnderlying[]{new TypeBeanOrUnderlying(eventType)};
                verified.put(propertyName, type);
                continue;
            }

            BeanEventType beanEventType = (BeanEventType) eventType;
            Object type = !isArray ? beanEventType.getUnderlyingType() : JavaClassHelper.getArrayType(beanEventType.getUnderlyingType());
            verified.put(propertyName, type);
        }

        return verified;
    }

    private static EPException makeUnexpectedTypeException(String propertyTypeName, String propertyName) {
        return new EPException("Nestable type configuration encountered an unexpected property type name '"
            + propertyTypeName + "' for property '" + propertyName + "', expected java.lang.Class or java.util.Map or the name of a previously-declared event type");
    }

    public static Map<String, Object> checkedCastUnderlyingMap(EventBean theEvent) throws PropertyAccessException {
        return (Map<String, Object>) theEvent.getUnderlying();
    }

    public static Object[] checkedCastUnderlyingObjectArray(EventBean theEvent) throws PropertyAccessException {
        return (Object[]) theEvent.getUnderlying();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value value
     * @param index index
     * @return array value or null
     */
    public static Object getBNArrayValueAtIndex(Object value, int index) {
        if (!value.getClass().isArray()) {
            return null;
        }
        if (Array.getLength(value) <= index) {
            return null;
        }
        return Array.get(value, index);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value value
     * @param index index
     * @return array value or null
     */
    public static Object getBNArrayValueAtIndexWithNullCheck(Object value, int index) {
        if (value == null) {
            return null;
        }
        return getBNArrayValueAtIndex(value, index);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value                      value
     * @param fragmentEventType          fragment type
     * @param eventBeanTypedEventFactory event adapter service
     * @return fragment
     */
    public static Object handleBNCreateFragmentMap(Object value, EventType fragmentEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (!(value instanceof Map)) {
            if (value instanceof EventBean) {
                return value;
            }
            return null;
        }
        Map subEvent = (Map) value;
        return eventBeanTypedEventFactory.adapterForTypedMap(subEvent, fragmentEventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param result                     result
     * @param eventType                  type
     * @param eventBeanTypedEventFactory event service
     * @return fragment
     */
    public static Object getBNFragmentPojo(Object result, BeanEventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (result == null) {
            return null;
        }
        if (result instanceof EventBean[]) {
            return result;
        }
        if (result instanceof EventBean) {
            return result;
        }
        if (result.getClass().isArray()) {
            int len = Array.getLength(result);
            EventBean[] events = new EventBean[len];
            for (int i = 0; i < events.length; i++) {
                events[i] = eventBeanTypedEventFactory.adapterForTypedBean(Array.get(result, i), eventType);
            }
            return events;
        }
        return eventBeanTypedEventFactory.adapterForTypedBean(result, eventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value                      value
     * @param fragmentEventType          fragment type
     * @param eventBeanTypedEventFactory service
     * @return fragment
     */
    public static Object handleBNCreateFragmentObjectArray(Object value, EventType fragmentEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (!(value instanceof Object[])) {
            if (value instanceof EventBean) {
                return value;
            }
            return null;
        }
        Object[] subEvent = (Object[]) value;
        return eventBeanTypedEventFactory.adapterForTypedObjectArray(subEvent, fragmentEventType);
    }


    public static Object handleNestedValueArrayWithMap(Object value, int index, MapEventPropertyGetter getter) {
        Object valueMap = getBNArrayValueAtIndex(value, index);
        if (!(valueMap instanceof Map)) {
            if (valueMap instanceof EventBean) {
                return getter.get((EventBean) valueMap);
            }
            return null;
        }
        return getter.getMap((Map<String, Object>) valueMap);
    }

    public static CodegenExpression handleNestedValueArrayWithMapCode(int index, MapEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, Class generator) {
        CodegenMethod method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.GET, generator);
        return localMethod(method, staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndex", ref, constant(index)));
    }

    public static Object handleBNNestedValueArrayWithMapFragment(Object value, int index, MapEventPropertyGetter getter, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType fragmentType) {
        Object valueMap = getBNArrayValueAtIndex(value, index);
        if (!(valueMap instanceof Map)) {
            if (value instanceof EventBean) {
                return getter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventBeanTypedEventFactory.adapterForTypedMap((Map<String, Object>) valueMap, fragmentType);
        return getter.getFragment(eventBean);
    }

    public static CodegenExpression handleBNNestedValueArrayWithMapFragmentCode(int index, MapEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType fragmentType, Class generator) {
        CodegenMethod method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.FRAGMENT, generator);
        return localMethod(method, staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndex", ref, constant(index)));
    }

    public static boolean handleNestedValueArrayWithMapExists(Object value, int index, MapEventPropertyGetter getter) {
        Object valueMap = getBNArrayValueAtIndex(value, index);
        if (!(valueMap instanceof Map)) {
            if (valueMap instanceof EventBean) {
                return getter.isExistsProperty((EventBean) valueMap);
            }
            return false;
        }
        return getter.isMapExistsProperty((Map<String, Object>) valueMap);
    }

    public static CodegenExpression handleNestedValueArrayWithMapExistsCode(int index, MapEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, Class generator) {
        CodegenMethod method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.EXISTS, generator);
        return localMethod(method, staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndex", ref, constant(index)));
    }

    public static Object handleNestedValueArrayWithObjectArray(Object value, int index, ObjectArrayEventPropertyGetter getter) {
        Object valueArray = getBNArrayValueAtIndex(value, index);
        if (!(valueArray instanceof Object[])) {
            if (valueArray instanceof EventBean) {
                return getter.get((EventBean) valueArray);
            }
            return null;
        }
        return getter.getObjectArray((Object[]) valueArray);
    }

    public static CodegenExpression handleNestedValueArrayWithObjectArrayCodegen(int index, ObjectArrayEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, Class generator) {
        CodegenMethod method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Object[].class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.GET, generator);
        return localMethod(method, staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndex", ref, constant(index)));
    }

    public static boolean handleNestedValueArrayWithObjectArrayExists(Object value, int index, ObjectArrayEventPropertyGetter getter) {
        Object valueArray = getBNArrayValueAtIndex(value, index);
        if (!(valueArray instanceof Object[])) {
            if (valueArray instanceof EventBean) {
                return getter.isExistsProperty((EventBean) valueArray);
            }
            return false;
        }
        return getter.isObjectArrayExistsProperty((Object[]) valueArray);
    }

    public static CodegenExpression handleNestedValueArrayWithObjectArrayExistsCodegen(int index, ObjectArrayEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, Class generator) {
        CodegenMethod method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Object[].class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.EXISTS, generator);
        return localMethod(method, staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndex", ref, constant(index)));
    }

    public static Object handleNestedValueArrayWithObjectArrayFragment(Object value, int index, ObjectArrayEventPropertyGetter getter, EventType fragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        Object valueArray = getBNArrayValueAtIndex(value, index);
        if (!(valueArray instanceof Object[])) {
            if (value instanceof EventBean) {
                return getter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventBeanTypedEventFactory.adapterForTypedObjectArray((Object[]) valueArray, fragmentType);
        return getter.getFragment(eventBean);
    }

    public static CodegenExpression handleNestedValueArrayWithObjectArrayFragmentCodegen(int index, ObjectArrayEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, Class generator) {
        CodegenMethod method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Object[].class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.FRAGMENT, generator);
        return localMethod(method, staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndex", ref, constant(index)));
    }

    public static Object getMappedPropertyValue(Object value, String key) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            return null;
        }
        Map innerMap = (Map) value;
        return innerMap.get(key);
    }

    public static boolean getMappedPropertyExists(Object value, String key) {
        if (value == null) {
            return false;
        }
        if (!(value instanceof Map)) {
            return false;
        }
        Map innerMap = (Map) value;
        return innerMap.containsKey(key);
    }

    public static MapIndexedPropPair getIndexedAndMappedProps(String[] properties) {
        Set<String> mapPropertiesToCopy = new HashSet<String>();
        Set<String> arrayPropertiesToCopy = new HashSet<String>();
        for (int i = 0; i < properties.length; i++) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(properties[i]);
            if (prop instanceof MappedProperty) {
                MappedProperty mappedProperty = (MappedProperty) prop;
                mapPropertiesToCopy.add(mappedProperty.getPropertyNameAtomic());
            }
            if (prop instanceof IndexedProperty) {
                IndexedProperty indexedProperty = (IndexedProperty) prop;
                arrayPropertiesToCopy.add(indexedProperty.getPropertyNameAtomic());
            }
        }
        return new MapIndexedPropPair(mapPropertiesToCopy, arrayPropertiesToCopy);
    }

    public static boolean isExistsIndexedValue(Object value, int index) {
        if (value == null) {
            return false;
        }
        if (!value.getClass().isArray()) {
            return false;
        }
        if (index >= Array.getLength(value)) {
            return false;
        }
        return true;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param fragmentUnderlying         fragment
     * @param fragmentEventType          type
     * @param eventBeanTypedEventFactory svc
     * @return bean
     */
    public static EventBean getBNFragmentNonPojo(Object fragmentUnderlying, EventType fragmentEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (fragmentUnderlying == null) {
            return null;
        }
        if (fragmentEventType instanceof MapEventType) {
            return eventBeanTypedEventFactory.adapterForTypedMap((Map<String, Object>) fragmentUnderlying, fragmentEventType);
        }
        return eventBeanTypedEventFactory.adapterForTypedObjectArray((Object[]) fragmentUnderlying, fragmentEventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value                      value
     * @param fragmentEventType          fragment type
     * @param eventBeanTypedEventFactory svc
     * @return fragment
     */
    public static Object getBNFragmentArray(Object value, EventType fragmentEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (value instanceof Object[]) {
            Object[] subEvents = (Object[]) value;

            int countNull = 0;
            for (Object subEvent : subEvents) {
                if (subEvent != null) {
                    countNull++;
                }
            }

            EventBean[] outEvents = new EventBean[countNull];
            int count = 0;
            for (Object item : subEvents) {
                if (item != null) {
                    outEvents[count++] = BaseNestableEventUtil.getBNFragmentNonPojo(item, fragmentEventType, eventBeanTypedEventFactory);
                }
            }

            return outEvents;
        }

        if (!(value instanceof Map[])) {
            return null;
        }
        Map[] mapTypedSubEvents = (Map[]) value;

        int countNull = 0;
        for (Map map : mapTypedSubEvents) {
            if (map != null) {
                countNull++;
            }
        }

        EventBean[] mapEvents = new EventBean[countNull];
        int count = 0;
        for (Map map : mapTypedSubEvents) {
            if (map != null) {
                mapEvents[count++] = eventBeanTypedEventFactory.adapterForTypedMap(map, fragmentEventType);
            }
        }

        return mapEvents;
    }

    public static Object getBeanArrayValue(BeanEventPropertyGetter nestedGetter, Object value, int index) {

        if (value == null) {
            return null;
        }
        if (!value.getClass().isArray()) {
            return null;
        }
        if (Array.getLength(value) <= index) {
            return null;
        }
        Object arrayItem = Array.get(value, index);
        if (arrayItem == null) {
            return null;
        }

        return nestedGetter.getBeanProp(arrayItem);
    }

    public static CodegenMethod getBeanArrayValueCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, BeanEventPropertyGetter nestedGetter, int index) {
        return codegenMethodScope.makeChild(Object.class, BaseNestableEventUtil.class, codegenClassScope).addParam(Object.class, "value").getBlock()
            .ifRefNullReturnNull("value")
            .ifConditionReturnConst(not(exprDotMethodChain(ref("value")).add("getClass").add("isArray")), null)
            .ifConditionReturnConst(relational(staticMethod(Array.class, "getLength", ref("value")), LE, constant(index)), null)
            .declareVar(Object.class, "arrayItem", staticMethod(Array.class, "get", ref("value"), constant(index)))
            .ifRefNullReturnNull("arrayItem")
            .methodReturn(nestedGetter.underlyingGetCodegen(cast(nestedGetter.getTargetType(), ref("arrayItem")), codegenMethodScope, codegenClassScope));
    }

    public static CodegenMethod getBeanArrayValueExistsCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, BeanEventPropertyGetter nestedGetter, int index) {
        return codegenMethodScope.makeChild(boolean.class, BaseNestableEventUtil.class, codegenClassScope).addParam(Object.class, "value").getBlock()
            .ifRefNullReturnFalse("value")
            .ifConditionReturnConst(not(exprDotMethodChain(ref("value")).add("getClass").add("isArray")), false)
            .ifConditionReturnConst(relational(staticMethod(Array.class, "getLength", ref("value")), LE, constant(index)), false)
            .declareVar(Object.class, "arrayItem", staticMethod(Array.class, "get", ref("value"), constant(index)))
            .ifRefNullReturnFalse("arrayItem")
            .methodReturn(nestedGetter.underlyingExistsCodegen(cast(nestedGetter.getTargetType(), ref("arrayItem")), codegenMethodScope, codegenClassScope));
    }

    public static Object getArrayPropertyValue(EventBean[] wrapper, int index, EventPropertyGetter nestedGetter) {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.length <= index) {
            return null;
        }
        EventBean innerArrayEvent = wrapper[index];
        return nestedGetter.get(innerArrayEvent);
    }

    public static CodegenMethod getArrayPropertyValueCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, int index, EventPropertyGetterSPI nestedGetter) {
        return codegenMethodScope.makeChild(Object.class, BaseNestableEventUtil.class, codegenClassScope).addParam(EventBean[].class, "wrapper").getBlock()
            .ifRefNullReturnNull("wrapper")
            .ifConditionReturnConst(relational(arrayLength(ref("wrapper")), LE, constant(index)), null)
            .declareVar(EventBean.class, "inner", arrayAtIndex(ref("wrapper"), constant(index)))
            .methodReturn(nestedGetter.eventBeanGetCodegen(ref("inner"), codegenMethodScope, codegenClassScope));
    }

    public static Object getArrayPropertyFragment(EventBean[] wrapper, int index, EventPropertyGetter nestedGetter) {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.length <= index) {
            return null;
        }
        EventBean innerArrayEvent = wrapper[index];
        return nestedGetter.getFragment(innerArrayEvent);
    }

    public static CodegenMethod getArrayPropertyFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, int index, EventPropertyGetterSPI nestedGetter) {
        return codegenMethodScope.makeChild(Object.class, BaseNestableEventUtil.class, codegenClassScope).addParam(EventBean[].class, "wrapper").getBlock()
            .ifRefNullReturnNull("wrapper")
            .ifConditionReturnConst(relational(arrayLength(ref("wrapper")), LE, constant(index)), null)
            .declareVar(EventBean.class, "inner", arrayAtIndex(ref("wrapper"), constant(index)))
            .methodReturn(nestedGetter.eventBeanFragmentCodegen(ref("inner"), codegenMethodScope, codegenClassScope));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param wrapper beans
     * @param index   index
     * @return underlying
     */
    public static Object getBNArrayPropertyUnderlying(EventBean[] wrapper, int index) {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.length <= index) {
            return null;
        }

        return wrapper[index].getUnderlying();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param wrapper beans
     * @param index   index
     * @return fragment
     */
    public static Object getBNArrayPropertyBean(EventBean[] wrapper, int index) {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.length <= index) {
            return null;
        }

        return wrapper[index];
    }

    public static Object getArrayPropertyAsUnderlyingsArray(Class underlyingType, EventBean[] wrapper) {
        if (wrapper != null) {
            Object array = Array.newInstance(underlyingType, wrapper.length);
            for (int i = 0; i < wrapper.length; i++) {
                Array.set(array, i, wrapper[i].getUnderlying());
            }
            return array;
        }

        return null;
    }

    public static CodegenMethod getArrayPropertyAsUnderlyingsArrayCodegen(Class underlyingType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, BaseNestableEventUtil.class, codegenClassScope).addParam(EventBean[].class, "wrapper").getBlock()
            .ifRefNullReturnNull("wrapper")
            .declareVar(JavaClassHelper.getArrayType(underlyingType), "array", newArrayByLength(underlyingType, arrayLength(ref("wrapper"))))
            .forLoopIntSimple("i", arrayLength(ref("wrapper")))
            .assignArrayElement("array", ref("i"), cast(underlyingType, exprDotMethod(arrayAtIndex(ref("wrapper"), ref("i")), "getUnderlying")))
            .blockEnd()
            .methodReturn(ref("array"));
    }

    public static ExprValidationException comparePropType(String propName, Object setOneType, Object setTwoType, boolean setTwoTypeFound, String otherName) {
        // allow null for nested event types
        if (isNestedType(setOneType) && setTwoType == null) {
            return null;
        }
        if (isNestedType(setTwoType) && setOneType == null) {
            return null;
        }
        if (!setTwoTypeFound) {
            return new ExprValidationException("The property '" + propName + "' is not provided but required");
        }
        if (setTwoType == null) {
            return null;
        }
        if (setOneType == null) {
            return new ExprValidationException("Type by name '" + otherName + "' in property '" + propName + "' incompatible with null-type or property name not found in target");
        }

        if ((setTwoType instanceof Class) && (setOneType instanceof Class)) {
            Class boxedOther = JavaClassHelper.getBoxedType((Class) setTwoType);
            Class boxedThis = JavaClassHelper.getBoxedType((Class) setOneType);
            if (!boxedOther.equals(boxedThis)) {
                if (!JavaClassHelper.isSubclassOrImplementsInterface(boxedOther, boxedThis)) {
                    return makeExpectedReceivedException(otherName, propName, boxedThis, boxedOther);
                }
            }
        } else if ((setTwoType instanceof EventType && isNativeUnderlyingType((EventType) setTwoType)) && (setOneType instanceof Class)) {
            Class boxedOther = JavaClassHelper.getBoxedType(((EventType) setTwoType).getUnderlyingType());
            Class boxedThis = JavaClassHelper.getBoxedType((Class) setOneType);
            if (!boxedOther.equals(boxedThis)) {
                return makeExpectedReceivedException(otherName, propName, boxedThis, boxedOther);
            }
        } else if (setTwoType instanceof EventType[] && (isNativeUnderlyingType(((EventType[]) setTwoType)[0]) && setOneType instanceof Class && ((Class) setOneType).isArray())) {
            Class boxedOther = JavaClassHelper.getBoxedType((((EventType[]) setTwoType)[0]).getUnderlyingType());
            Class boxedThis = JavaClassHelper.getBoxedType(((Class) setOneType).getComponentType());
            if (!boxedOther.equals(boxedThis)) {
                return makeExpectedReceivedException(otherName, propName, boxedThis, boxedOther);
            }
        } else if ((setTwoType instanceof Map) && (setOneType instanceof Map)) {
            ExprValidationException messageIsDeepEquals = BaseNestableEventType.isDeepEqualsProperties(propName, (Map<String, Object>) setOneType, (Map<String, Object>) setTwoType);
            if (messageIsDeepEquals != null) {
                return messageIsDeepEquals;
            }
        } else if ((setTwoType instanceof EventType) && (setOneType instanceof EventType)) {
            boolean mismatch;
            if (setTwoType instanceof EventTypeSPI && setOneType instanceof EventTypeSPI) {
                ExprValidationException compared = ((EventTypeSPI) setOneType).equalsCompareType((EventTypeSPI) setTwoType);
                mismatch = compared != null;
            } else {
                mismatch = !setOneType.equals(setTwoType);
            }
            if (mismatch) {
                EventType setOneEventType = (EventType) setOneType;
                EventType setTwoEventType = (EventType) setTwoType;
                return getMismatchMessageEventType(otherName, propName, setOneEventType, setTwoEventType);
            }
        } else if ((setTwoType instanceof TypeBeanOrUnderlying) && (setOneType instanceof EventType)) {
            EventType setOneEventType = (EventType) setOneType;
            EventType setTwoEventType = ((TypeBeanOrUnderlying) setTwoType).getEventType();
            if (!EventTypeUtility.isTypeOrSubTypeOf(setTwoEventType, setOneEventType)) {
                return getMismatchMessageEventType(otherName, propName, setOneEventType, setTwoEventType);
            }
        } else if ((setTwoType instanceof EventType) && (setOneType instanceof TypeBeanOrUnderlying)) {
            EventType setOneEventType = ((TypeBeanOrUnderlying) setOneType).getEventType();
            EventType setTwoEventType = (EventType) setTwoType;
            if (!EventTypeUtility.isTypeOrSubTypeOf(setTwoEventType, setOneEventType)) {
                return getMismatchMessageEventType(otherName, propName, setOneEventType, setTwoEventType);
            }
        } else if ((setTwoType instanceof TypeBeanOrUnderlying) && (setOneType instanceof TypeBeanOrUnderlying)) {
            EventType setOneEventType = ((TypeBeanOrUnderlying) setOneType).getEventType();
            EventType setTwoEventType = ((TypeBeanOrUnderlying) setTwoType).getEventType();
            if (!EventTypeUtility.isTypeOrSubTypeOf(setOneEventType, setTwoEventType)) {
                return getMismatchMessageEventType(otherName, propName, setOneEventType, setTwoEventType);
            }
        } else if ((setTwoType instanceof EventType[]) && (setOneType instanceof TypeBeanOrUnderlying[])) {
            EventType setTwoEventType = ((EventType[]) setTwoType)[0];
            EventType setOneEventType = ((TypeBeanOrUnderlying[]) setOneType)[0].getEventType();
            if (!EventTypeUtility.isTypeOrSubTypeOf(setOneEventType, setTwoEventType)) {
                return getMismatchMessageEventType(otherName, propName, setOneEventType, setTwoEventType);
            }
        } else {
            String typeOne = getTypeName(setOneType);
            String typeTwo = getTypeName(setTwoType);
            if (typeOne.equals(typeTwo)) {
                return null;
            }
            return new ExprValidationException("Type by name '" + otherName + "' in property '" + propName + "' expected " + typeOne + " but receives " + typeTwo);
        }

        return null;
    }

    private static ExprValidationException makeExpectedReceivedException(String otherName, String propName, Class boxedThis, Class boxedOther) {
        return new ExprValidationException("Type by name '" + otherName + "' in property '" + propName + "' expected " + getClassNameFullyQualPretty(boxedThis) + " but receives " + getClassNameFullyQualPretty(boxedOther));
    }

    private static ExprValidationException getMismatchMessageEventType(String otherName, String propName, EventType setOneEventType, EventType setTwoEventType) {
        return new ExprValidationException("Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType.getName() + "' but receives event type '" + setTwoEventType.getName() + "'");
    }

    private static boolean isNestedType(Object type) {
        return type instanceof TypeBeanOrUnderlying ||
            type instanceof EventType ||
            type instanceof TypeBeanOrUnderlying[] ||
            type instanceof EventType[];
    }

    private static String getTypeName(Object type) {
        if (type == null) {
            return "null";
        }
        if (type instanceof Class) {
            return ((Class) type).getName();
        }
        if (type instanceof EventType) {
            return "event type '" + ((EventType) type).getName() + "'";
        }
        if (type instanceof EventType[]) {
            return "event type array '" + ((EventType[]) type)[0].getName() + "'";
        }
        if (type instanceof TypeBeanOrUnderlying) {
            return "event type '" + ((TypeBeanOrUnderlying) type).getEventType().getName() + "'";
        }
        if (type instanceof TypeBeanOrUnderlying[]) {
            return "event type array '" + ((TypeBeanOrUnderlying[]) type)[0].getEventType().getName() + "'";
        }
        return type.getClass().getName();
    }

    private static boolean isNativeUnderlyingType(EventType eventType) {
        if (eventType instanceof BeanEventType) {
            return true;
        }
        if (eventType instanceof JsonEventType) {
            return ((JsonEventType) eventType).getDetail().getOptionalUnderlyingProvided() != null;
        }
        return false;
    }

    public static class MapIndexedPropPair {
        private final Set<String> mapProperties;
        private final Set<String> arrayProperties;

        public MapIndexedPropPair(Set<String> mapProperties, Set<String> arrayProperties) {
            this.mapProperties = mapProperties;
            this.arrayProperties = arrayProperties;
        }

        public Set<String> getMapProperties() {
            return mapProperties;
        }

        public Set<String> getArrayProperties() {
            return arrayProperties;
        }
    }
}
