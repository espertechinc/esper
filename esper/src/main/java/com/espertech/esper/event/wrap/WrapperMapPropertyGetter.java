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
package com.espertech.esper.event.wrap;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.DecoratingEventBean;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.WrapperEventType;
import com.espertech.esper.event.map.MapEventType;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class WrapperMapPropertyGetter implements EventPropertyGetterSPI {
    private final WrapperEventType wrapperEventType;
    private final EventAdapterService eventAdapterService;
    private final MapEventType underlyingMapType;
    private final EventPropertyGetterSPI mapGetter;

    public WrapperMapPropertyGetter(WrapperEventType wrapperEventType, EventAdapterService eventAdapterService, MapEventType underlyingMapType, EventPropertyGetterSPI mapGetter) {
        this.wrapperEventType = wrapperEventType;
        this.eventAdapterService = eventAdapterService;
        this.underlyingMapType = underlyingMapType;
        this.mapGetter = mapGetter;
    }

    public Object get(EventBean theEvent) {
        if (!(theEvent instanceof DecoratingEventBean)) {
            throw new PropertyAccessException("Mismatched property getter to EventBean type");
        }
        DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
        Map map = wrapperEvent.getDecoratingProperties();
        return mapGetter.get(eventAdapterService.adapterForTypedMap(map, underlyingMapType));
    }

    private String getCodegen(CodegenContext context) {
        return context.addMethod(Object.class, this.getClass()).add(EventBean.class, "theEvent").begin()
                .declareVarWCast(DecoratingEventBean.class, "wrapperEvent", "theEvent")
                .declareVar(Map.class, "map", exprDotMethod(ref("wrapperEvent"), "getDecoratingProperties"))
                .methodReturn(mapGetter.underlyingGetCodegen(ref("map"), context));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean theEvent) {
        if (!(theEvent instanceof DecoratingEventBean)) {
            throw new PropertyAccessException("Mismatched property getter to EventBean type");
        }
        DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
        Map map = wrapperEvent.getDecoratingProperties();
        return mapGetter.getFragment(eventAdapterService.adapterForTypedMap(map, underlyingMapType));
    }

    private String getFragmentCodegen(CodegenContext context) {
        return context.addMethod(Object.class, this.getClass()).add(EventBean.class, "theEvent").begin()
                .declareVarWCast(DecoratingEventBean.class, "wrapperEvent", "theEvent")
                .declareVar(Map.class, "map", exprDotMethod(ref("wrapperEvent"), "getDecoratingProperties"))
                .methodReturn(mapGetter.underlyingFragmentCodegen(ref("map"), context));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return localMethod(getCodegen(context), beanExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), beanExpression);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        throw implementationNotProvided();
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        throw implementationNotProvided();
    }

    private UnsupportedOperationException implementationNotProvided() {
        return new UnsupportedOperationException("Wrapper event type does not provide an implementation for underlying get");
    }
}
