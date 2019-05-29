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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.MappedEventBean;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter that works on EventBean events residing within a Map as an event property.
 */
public class MapNestedEntryPropertyGetterMap extends MapNestedEntryPropertyGetterBase {

    private final MapEventPropertyGetter mapGetter;

    public MapNestedEntryPropertyGetterMap(String propertyMap, EventType fragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory, MapEventPropertyGetter mapGetter) {
        super(propertyMap, fragmentType, eventBeanTypedEventFactory);
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

    public boolean handleNestedValueExists(Object value) {
        if (!(value instanceof Map)) {
            if (value instanceof EventBean) {
                return mapGetter.isMapExistsProperty(((MappedEventBean) value).getProperties());
            }
            return false;
        }
        return mapGetter.isMapExistsProperty((Map<String, Object>) value);
    }

    private CodegenMethod handleNestedValueCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object.class, "value").getBlock()
                .ifNotInstanceOf("value", Map.class)
                .ifInstanceOf("value", EventBean.class)
                .declareVarWCast(EventBean.class, "bean", "value")
                .blockReturn(mapGetter.eventBeanGetCodegen(ref("bean"), codegenMethodScope, codegenClassScope))
                .blockReturn(constantNull())
                .declareVarWCast(Map.class, "map", "value")
                .methodReturn(mapGetter.underlyingGetCodegen(ref("map"), codegenMethodScope, codegenClassScope));
    }

    private CodegenMethod handleNestedValueExistsCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(Object.class, "value").getBlock()
            .ifNotInstanceOf("value", Map.class)
            .ifInstanceOf("value", EventBean.class)
            .declareVarWCast(EventBean.class, "bean", "value")
            .blockReturn(mapGetter.eventBeanExistsCodegen(ref("bean"), codegenMethodScope, codegenClassScope))
            .blockReturn(constantFalse())
            .declareVarWCast(Map.class, "map", "value")
            .methodReturn(mapGetter.underlyingExistsCodegen(ref("map"), codegenMethodScope, codegenClassScope));
    }

    public Object handleNestedValueFragment(Object value) {
        if (!(value instanceof Map)) {
            if (value instanceof EventBean) {
                return mapGetter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventBeanTypedEventFactory.adapterForTypedMap((Map<String, Object>) value, fragmentType);
        return mapGetter.getFragment(eventBean);
    }

    private CodegenMethod handleNestedValueFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object.class, "value").getBlock()
                .ifNotInstanceOf("value", Map.class)
                .ifInstanceOf("value", EventBean.class)
                .declareVarWCast(EventBean.class, "bean", "value")
                .blockReturn(mapGetter.eventBeanFragmentCodegen(ref("bean"), codegenMethodScope, codegenClassScope))
                .blockReturn(constantNull())
                .methodReturn(mapGetter.underlyingFragmentCodegen(cast(Map.class, ref("value")), codegenMethodScope, codegenClassScope));
    }

    public CodegenExpression handleNestedValueCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(handleNestedValueCodegen(codegenMethodScope, codegenClassScope), name);
    }

    public CodegenExpression handleNestedValueExistsCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(handleNestedValueExistsCodegen(codegenMethodScope, codegenClassScope), name);
    }

    public CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(handleNestedValueFragmentCodegen(codegenMethodScope, codegenClassScope), name);
    }
}
