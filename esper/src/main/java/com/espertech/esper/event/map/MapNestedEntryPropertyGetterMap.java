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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.EventAdapterService;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter that works on EventBean events residing within a Map as an event property.
 */
public class MapNestedEntryPropertyGetterMap extends MapNestedEntryPropertyGetterBase {

    private final MapEventPropertyGetter mapGetter;

    public MapNestedEntryPropertyGetterMap(String propertyMap, EventType fragmentType, EventAdapterService eventAdapterService, MapEventPropertyGetter mapGetter) {
        super(propertyMap, fragmentType, eventAdapterService);
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

    private CodegenMethodNode handleNestedValueCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object.class, "value").getBlock()
            .ifNotInstanceOf("value", Map.class)
                .ifInstanceOf("value", EventBean.class)
                    .declareVarWCast(EventBean.class, "bean", "value")
                    .blockReturn(mapGetter.eventBeanGetCodegen(ref("bean"), codegenMethodScope, codegenClassScope))
                .blockReturn(constantNull())
            .declareVarWCast(Map.class, "map", "value")
            .methodReturn(mapGetter.underlyingGetCodegen(ref("map"), codegenMethodScope, codegenClassScope));
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

    private CodegenMethodNode handleNestedValueFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
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

    public CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(handleNestedValueFragmentCodegen(codegenMethodScope, codegenClassScope), name);
    }
}
