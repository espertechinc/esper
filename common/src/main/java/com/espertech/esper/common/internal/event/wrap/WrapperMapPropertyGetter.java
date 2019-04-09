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
package com.espertech.esper.common.internal.event.wrap;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.event.core.DecoratingEventBean;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.WrapperEventType;
import com.espertech.esper.common.internal.event.map.MapEventType;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class WrapperMapPropertyGetter implements EventPropertyGetterSPI {
    private final WrapperEventType wrapperEventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final MapEventType underlyingMapType;
    private final EventPropertyGetterSPI mapGetter;

    public WrapperMapPropertyGetter(WrapperEventType wrapperEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, MapEventType underlyingMapType, EventPropertyGetterSPI mapGetter) {
        this.wrapperEventType = wrapperEventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.underlyingMapType = underlyingMapType;
        this.mapGetter = mapGetter;
    }

    public Object get(EventBean theEvent) {
        if (!(theEvent instanceof DecoratingEventBean)) {
            throw new PropertyAccessException("Mismatched property getter to EventBean type");
        }
        DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
        Map map = wrapperEvent.getDecoratingProperties();
        return mapGetter.get(eventBeanTypedEventFactory.adapterForTypedMap(map, underlyingMapType));
    }

    private CodegenMethod getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(EventBean.class, "theEvent").getBlock()
                .declareVarWCast(DecoratingEventBean.class, "wrapperEvent", "theEvent")
                .declareVar(Map.class, "map", exprDotMethod(ref("wrapperEvent"), "getDecoratingProperties"))
                .methodReturn(mapGetter.underlyingGetCodegen(ref("map"), codegenMethodScope, codegenClassScope));
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
        return mapGetter.getFragment(eventBeanTypedEventFactory.adapterForTypedMap(map, underlyingMapType));
    }

    private CodegenMethod getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(EventBean.class, "theEvent").getBlock()
                .declareVarWCast(DecoratingEventBean.class, "wrapperEvent", "theEvent")
                .declareVar(Map.class, "map", exprDotMethod(ref("wrapperEvent"), "getDecoratingProperties"))
                .methodReturn(mapGetter.underlyingFragmentCodegen(ref("map"), codegenMethodScope, codegenClassScope));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getCodegen(codegenMethodScope, codegenClassScope), beanExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), beanExpression);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object.class, "und");
        if (wrapperEventType.getUnderlyingType() == Pair.class) {
            method.getBlock().declareVarWCast(Pair.class, "pair", "und")
                .declareVar(Map.class, "wrapped", cast(Map.class, exprDotMethod(ref("pair"), "getSecond")))
                .methodReturn(mapGetter.underlyingGetCodegen(ref("wrapped"), codegenMethodScope, codegenClassScope));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
        return localMethod(method, ref("und"));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw implementationNotProvided();
    }

    private UnsupportedOperationException implementationNotProvided() {
        return new UnsupportedOperationException("Wrapper event type does not provide an implementation for underlying get");
    }
}
