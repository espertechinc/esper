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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
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

    private String handleNestedValueCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Object.class, "value", this.getClass())
            .ifNotInstanceOf("value", Map.class)
                .ifInstanceOf("value", EventBean.class)
                    .declareVarWCast(EventBean.class, "bean", "value")
                    .blockReturn(mapGetter.codegenEventBeanGet(ref("bean"), context))
                .blockReturn(constantNull())
            .declareVarWCast(Map.class, "map", "value")
            .methodReturn(mapGetter.codegenUnderlyingGet(ref("map"), context));
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

    private String handleNestedValueFragmentCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Object.class, "value", this.getClass())
                .ifNotInstanceOf("value", Map.class)
                .ifInstanceOf("value", EventBean.class)
                .declareVarWCast(EventBean.class, "bean", "value")
                .blockReturn(mapGetter.codegenEventBeanFragment(ref("bean"), context))
                .blockReturn(constantNull())
                .methodReturn(mapGetter.codegenUnderlyingFragment(cast(Map.class, ref("value")), context));
    }

    public CodegenExpression handleNestedValueCodegen(CodegenExpression name, CodegenContext context) {
        return localMethod(handleNestedValueCodegen(context), name);
    }

    public CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression name, CodegenContext context) {
        return localMethod(handleNestedValueFragmentCodegen(context), name);
    }
}
