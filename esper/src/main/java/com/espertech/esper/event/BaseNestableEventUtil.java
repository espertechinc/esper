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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoPropertyBeanOrUnd;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventPropertyGetter;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.event.property.IndexedProperty;
import com.espertech.esper.event.property.MappedProperty;
import com.espertech.esper.event.property.Property;
import com.espertech.esper.event.property.PropertyParser;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class BaseNestableEventUtil {
    public static Map<String, Object> checkedCastUnderlyingMap(EventBean theEvent) throws PropertyAccessException {
        return (Map<String, Object>) theEvent.getUnderlying();
    }

    public static Object[] checkedCastUnderlyingObjectArray(EventBean theEvent) throws PropertyAccessException {
        return (Object[]) theEvent.getUnderlying();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
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
     * @param value value
     * @param fragmentEventType fragment type
     * @param eventAdapterService event adapter service
     * @return fragment
     */
    public static Object handleBNCreateFragmentMap(Object value, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        if (!(value instanceof Map)) {
            if (value instanceof EventBean) {
                return value;
            }
            return null;
        }
        Map subEvent = (Map) value;
        return eventAdapterService.adapterForTypedMap(subEvent, fragmentEventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param result result
     * @param eventType type
     * @param eventAdapterService event service
     * @return fragment
     */
    public static Object getBNFragmentPojo(Object result, BeanEventType eventType, EventAdapterService eventAdapterService) {
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
                events[i] = eventAdapterService.adapterForTypedBean(Array.get(result, i), eventType);
            }
            return events;
        }
        return eventAdapterService.adapterForTypedBean(result, eventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param value value
     * @param fragmentEventType fragment type
     * @param eventAdapterService service
     * @return fragment
     */
    public static Object handleBNCreateFragmentObjectArray(Object value, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        if (!(value instanceof Object[])) {
            if (value instanceof EventBean) {
                return value;
            }
            return null;
        }
        Object[] subEvent = (Object[]) value;
        return eventAdapterService.adapterForTypedObjectArray(subEvent, fragmentEventType);
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
        CodegenMethodNode method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.GET, generator);
        return localMethod(method, staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndex", ref, constant(index)));
    }

    public static Object handleBNNestedValueArrayWithMapFragment(Object value, int index, MapEventPropertyGetter getter, EventAdapterService eventAdapterService, EventType fragmentType) {
        Object valueMap = getBNArrayValueAtIndex(value, index);
        if (!(valueMap instanceof Map)) {
            if (value instanceof EventBean) {
                return getter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventAdapterService.adapterForTypedMap((Map<String, Object>) valueMap, fragmentType);
        return getter.getFragment(eventBean);
    }

    public static CodegenExpression handleBNNestedValueArrayWithMapFragmentCode(int index, MapEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, EventAdapterService eventAdapterService, EventType fragmentType, Class generator) {
        CodegenMethodNode method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.FRAGMENT, generator);
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

    public static CodegenExpression handleNestedValueArrayWithMapExistsCode(int index, MapEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, EventAdapterService eventAdapterService, EventType fragmentType, Class generator) {
        CodegenMethodNode method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.EXISTS, generator);
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
        CodegenMethodNode method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Object[].class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.GET, generator);
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
        CodegenMethodNode method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Object[].class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.EXISTS, generator);
        return localMethod(method, staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndex", ref, constant(index)));
    }

    public static Object handleNestedValueArrayWithObjectArrayFragment(Object value, int index, ObjectArrayEventPropertyGetter getter, EventType fragmentType, EventAdapterService eventAdapterService) {
        Object valueArray = getBNArrayValueAtIndex(value, index);
        if (!(valueArray instanceof Object[])) {
            if (value instanceof EventBean) {
                return getter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventAdapterService.adapterForTypedObjectArray((Object[]) valueArray, fragmentType);
        return getter.getFragment(eventBean);
    }

    public static CodegenExpression handleNestedValueArrayWithObjectArrayFragmentCodegen(int index, ObjectArrayEventPropertyGetter getter, CodegenExpression ref, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, Class generator) {
        CodegenMethodNode method = CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Object[].class, getter, CodegenLegoPropertyBeanOrUnd.AccessType.FRAGMENT, generator);
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
     * @param fragmentUnderlying fragment
     * @param fragmentEventType type
     * @param eventAdapterService svc
     * @return bean
     */
    public static EventBean getBNFragmentNonPojo(Object fragmentUnderlying, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        if (fragmentUnderlying == null) {
            return null;
        }
        if (fragmentEventType instanceof MapEventType) {
            return eventAdapterService.adapterForTypedMap((Map<String, Object>) fragmentUnderlying, fragmentEventType);
        }
        return eventAdapterService.adapterForTypedObjectArray((Object[]) fragmentUnderlying, fragmentEventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param value value
     * @param fragmentEventType fragment type
     * @param eventAdapterService svc
     * @return fragment
     */
    public static Object getBNFragmentArray(Object value, EventType fragmentEventType, EventAdapterService eventAdapterService) {
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
                    outEvents[count++] = BaseNestableEventUtil.getBNFragmentNonPojo(item, fragmentEventType, eventAdapterService);
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
                mapEvents[count++] = eventAdapterService.adapterForTypedMap(map, fragmentEventType);
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

    public static CodegenMethodNode getBeanArrayValueCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, BeanEventPropertyGetter nestedGetter, int index) {
        return codegenMethodScope.makeChild(Object.class, BaseNestableEventUtil.class, codegenClassScope).addParam(Object.class, "value").getBlock()
                .ifRefNullReturnNull("value")
                .ifConditionReturnConst(not(exprDotMethodChain(ref("value")).add("getClass").add("isArray")), null)
                .ifConditionReturnConst(relational(staticMethod(Array.class, "getLength", ref("value")), LE, constant(index)), null)
                .declareVar(Object.class, "arrayItem", staticMethod(Array.class, "get", ref("value"), constant(index)))
                .ifRefNullReturnNull("arrayItem")
                .methodReturn(nestedGetter.underlyingGetCodegen(cast(nestedGetter.getTargetType(), ref("arrayItem")), codegenMethodScope, codegenClassScope));
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

    public static CodegenMethodNode getArrayPropertyValueCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, int index, EventPropertyGetterSPI nestedGetter) {
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

    public static CodegenMethodNode getArrayPropertyFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, int index, EventPropertyGetterSPI nestedGetter) {
        return codegenMethodScope.makeChild(Object.class, BaseNestableEventUtil.class, codegenClassScope).addParam(EventBean[].class, "wrapper").getBlock()
                .ifRefNullReturnNull("wrapper")
                .ifConditionReturnConst(relational(arrayLength(ref("wrapper")), LE, constant(index)), null)
                .declareVar(EventBean.class, "inner", arrayAtIndex(ref("wrapper"), constant(index)))
                .methodReturn(nestedGetter.eventBeanFragmentCodegen(ref("inner"), codegenMethodScope, codegenClassScope));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param wrapper beans
     * @param index index
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
     * @param wrapper beans
     * @param index index
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

    public static CodegenMethodNode getArrayPropertyAsUnderlyingsArrayCodegen(Class underlyingType, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, BaseNestableEventUtil.class, codegenClassScope).addParam(EventBean[].class, "wrapper").getBlock()
                .ifRefNullReturnNull("wrapper")
                .declareVar(JavaClassHelper.getArrayType(underlyingType), "array", newArrayByLength(underlyingType, arrayLength(ref("wrapper"))))
                .forLoopIntSimple("i", arrayLength(ref("wrapper")))
                    .assignArrayElement("array", ref("i"), cast(underlyingType, exprDotMethod(arrayAtIndex(ref("wrapper"), ref("i")), "getUnderlying")))
                    .blockEnd()
                .methodReturn(ref("array"));
    }

    public static String comparePropType(String propName, Object setOneType, Object setTwoType, boolean setTwoTypeFound, String otherName) {
        // allow null for nested event types
        if ((setOneType instanceof String || setOneType instanceof EventType) && setTwoType == null) {
            return null;
        }
        if ((setTwoType instanceof String || setTwoType instanceof EventType) && setOneType == null) {
            return null;
        }
        if (!setTwoTypeFound) {
            return "The property '" + propName + "' is not provided but required";
        }
        if (setTwoType == null) {
            return null;
        }
        if (setOneType == null) {
            return "Type by name '" + otherName + "' in property '" + propName + "' incompatible with null-type or property name not found in target";
        }

        if ((setTwoType instanceof Class) && (setOneType instanceof Class)) {
            Class boxedOther = JavaClassHelper.getBoxedType((Class) setTwoType);
            Class boxedThis = JavaClassHelper.getBoxedType((Class) setOneType);
            if (!boxedOther.equals(boxedThis)) {
                if (!JavaClassHelper.isSubclassOrImplementsInterface(boxedOther, boxedThis)) {
                    return "Type by name '" + otherName + "' in property '" + propName + "' expected " + boxedThis + " but receives " + boxedOther;
                }
            }
        } else if ((setTwoType instanceof BeanEventType) && (setOneType instanceof Class)) {
            Class boxedOther = JavaClassHelper.getBoxedType(((BeanEventType) setTwoType).getUnderlyingType());
            Class boxedThis = JavaClassHelper.getBoxedType((Class) setOneType);
            if (!boxedOther.equals(boxedThis)) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected " + boxedThis + " but receives " + boxedOther;
            }
        } else if (setTwoType instanceof EventType[] && ((EventType[]) setTwoType)[0] instanceof BeanEventType && setOneType instanceof Class && ((Class) setOneType).isArray()) {
            Class boxedOther = JavaClassHelper.getBoxedType((((EventType[]) setTwoType)[0]).getUnderlyingType());
            Class boxedThis = JavaClassHelper.getBoxedType(((Class) setOneType).getComponentType());
            if (!boxedOther.equals(boxedThis)) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected " + boxedThis + " but receives " + boxedOther;
            }
        } else if ((setTwoType instanceof Map) && (setOneType instanceof Map)) {
            String messageIsDeepEquals = BaseNestableEventType.isDeepEqualsProperties(propName, (Map<String, Object>) setOneType, (Map<String, Object>) setTwoType);
            if (messageIsDeepEquals != null) {
                return messageIsDeepEquals;
            }
        } else if ((setTwoType instanceof EventType) && (setOneType instanceof EventType)) {
            boolean mismatch;
            if (setTwoType instanceof EventTypeSPI && setOneType instanceof EventTypeSPI) {
                mismatch = !((EventTypeSPI) setOneType).equalsCompareType((EventTypeSPI) setTwoType);
            } else {
                mismatch = !setOneType.equals(setTwoType);
            }
            if (mismatch) {
                EventType setOneEventType = (EventType) setOneType;
                EventType setTwoEventType = (EventType) setTwoType;
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType.getName() + "' but receives event type '" + setTwoEventType.getName() + "'";
            }
        } else if ((setTwoType instanceof String) && (setOneType instanceof EventType)) {
            EventType setOneEventType = (EventType) setOneType;
            String setTwoEventType = (String) setTwoType;
            if (!EventTypeUtility.isTypeOrSubTypeOf(setTwoEventType, setOneEventType)) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType.getName() + "' but receives event type '" + setTwoEventType + "'";
            }
        } else if ((setTwoType instanceof EventType) && (setOneType instanceof String)) {
            EventType setTwoEventType = (EventType) setTwoType;
            String setOneEventType = (String) setOneType;
            if (!EventTypeUtility.isTypeOrSubTypeOf(setOneEventType, setTwoEventType)) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType + "' but receives event type '" + setTwoEventType.getName() + "'";
            }
        } else if ((setTwoType instanceof String) && (setOneType instanceof String)) {
            if (!setTwoType.equals(setOneType)) {
                String setOneEventType = (String) setOneType;
                String setTwoEventType = (String) setTwoType;
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType + "' but receives event type '" + setTwoEventType + "'";
            }
        } else if ((setTwoType instanceof EventType[]) && (setOneType instanceof String)) {
            EventType[] setTwoTypeArr = (EventType[]) setTwoType;
            EventType setTwoFragmentType = setTwoTypeArr[0];
            String setOneTypeString = (String) setOneType;
            if (!(setOneTypeString.endsWith("[]"))) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneType + "' but receives event type '" + setTwoFragmentType.getName() + "[]'";
            }
            String setOneTypeNoArray = setOneTypeString.replaceAll("\\[\\]", "");
            if (!(setTwoFragmentType.getName().equals(setOneTypeNoArray))) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneTypeNoArray + "[]' but receives event type '" + setTwoFragmentType.getName() + "'";
            }
        } else {
            String typeOne = getTypeName(setOneType);
            String typeTwo = getTypeName(setTwoType);
            if (typeOne.equals(typeTwo)) {
                return null;
            }
            return "Type by name '" + otherName + "' in property '" + propName + "' expected " + typeOne + " but receives " + typeTwo;
        }

        return null;
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
        if (type instanceof String) {
            Class boxedType = JavaClassHelper.getBoxedType(JavaClassHelper.getPrimitiveClassForName((String) type));
            if (boxedType != null) {
                return boxedType.getName();
            }
            return (String) type;
        }
        return type.getClass().getName();
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
