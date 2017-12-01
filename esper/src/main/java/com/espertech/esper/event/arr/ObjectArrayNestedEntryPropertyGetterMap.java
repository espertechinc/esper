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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoPropertyBeanOrUnd;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.map.MapEventPropertyGetter;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;

/**
 * A getter that works on EventBean events residing within a Map as an event property.
 */
public class ObjectArrayNestedEntryPropertyGetterMap extends ObjectArrayNestedEntryPropertyGetterBase {

    private final MapEventPropertyGetter mapGetter;

    public ObjectArrayNestedEntryPropertyGetterMap(int propertyIndex, EventType fragmentType, EventAdapterService eventAdapterService, MapEventPropertyGetter mapGetter) {
        super(propertyIndex, fragmentType, eventAdapterService);
        this.mapGetter = mapGetter;
    }

    public Object handleNestedValue(Object value) {
        if (!(value instanceof Map)) {
            if (value instanceof EventBean) {
                return mapGetter.get((EventBean) value);
            }
            return null;
        }
        return mapGetter.getMap((Map<String, Object>) value);
    }

    public Object handleNestedValueFragment(Object value) {
        if (!(value instanceof Map)) {
            if (value instanceof EventBean) {
                return mapGetter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventAdapterService.adapterForTypedMap((Map<String, Object>) value, fragmentType);
        return mapGetter.getFragment(eventBean);
    }

    public boolean handleNestedValueExists(Object value) {
        if (!(value instanceof Map)) {
            if (value instanceof EventBean) {
                return mapGetter.isExistsProperty((EventBean) value);
            }
            return false;
        }
        return mapGetter.isMapExistsProperty((Map<String, Object>) value);
    }

    public CodegenExpression handleNestedValueCodegen(CodegenExpression refName, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, mapGetter, CodegenLegoPropertyBeanOrUnd.AccessType.GET, this.getClass()), refName);
    }

    public CodegenExpression handleNestedValueExistsCodegen(CodegenExpression refName, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, mapGetter, CodegenLegoPropertyBeanOrUnd.AccessType.EXISTS, this.getClass()), refName);
    }

    public CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression refName, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(CodegenLegoPropertyBeanOrUnd.from(codegenMethodScope, codegenClassScope, Map.class, mapGetter, CodegenLegoPropertyBeanOrUnd.AccessType.FRAGMENT, this.getClass()), refName);
    }
}
