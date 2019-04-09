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
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.WrapperEventType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class WrapperUnderlyingPropertyGetter implements EventPropertyGetterSPI {
    private final WrapperEventType wrapperEventType;
    private final EventPropertyGetterSPI underlyingGetter;

    public WrapperUnderlyingPropertyGetter(WrapperEventType wrapperEventType, EventPropertyGetterSPI underlyingGetter) {
        this.wrapperEventType = wrapperEventType;
        this.underlyingGetter = underlyingGetter;
    }

    public Object get(EventBean theEvent) {
        if (!(theEvent instanceof DecoratingEventBean)) {
            throw new PropertyAccessException("Mismatched property getter to EventBean type");
        }
        DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
        EventBean wrappedEvent = wrapperEvent.getUnderlyingEvent();
        if (wrappedEvent == null) {
            return null;
        }
        return underlyingGetter.get(wrappedEvent);
    }

    private CodegenMethod getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(EventBean.class, "theEvent").getBlock()
            .declareVarWCast(DecoratingEventBean.class, "wrapperEvent", "theEvent")
            .declareVar(EventBean.class, "wrappedEvent", exprDotMethod(ref("wrapperEvent"), "getUnderlyingEvent"))
            .ifRefNullReturnNull("wrappedEvent")
            .methodReturn(underlyingGetter.eventBeanGetCodegen(ref("wrappedEvent"), codegenMethodScope, codegenClassScope));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean theEvent) {
        if (!(theEvent instanceof DecoratingEventBean)) {
            throw new PropertyAccessException("Mismatched property getter to EventBean type");
        }
        DecoratingEventBean wrapperEvent = (DecoratingEventBean) theEvent;
        EventBean wrappedEvent = wrapperEvent.getUnderlyingEvent();
        if (wrappedEvent == null) {
            return null;
        }
        return underlyingGetter.getFragment(wrappedEvent);
    }

    private CodegenMethod getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(EventBean.class, "theEvent").getBlock()
            .declareVarWCast(DecoratingEventBean.class, "wrapperEvent", "theEvent")
            .declareVar(EventBean.class, "wrappedEvent", exprDotMethod(ref("wrapperEvent"), "getUnderlyingEvent"))
            .ifRefNullReturnNull("wrappedEvent")
            .methodReturn(underlyingGetter.eventBeanFragmentCodegen(ref("wrappedEvent"), codegenMethodScope, codegenClassScope));
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
        Class undType = wrapperEventType.getUnderlyingEventType().getUnderlyingType();
        if (wrapperEventType.getUnderlyingType() == Pair.class) {
            method.getBlock().declareVarWCast(Pair.class, "pair", "und")
                .declareVar(undType, "wrapped", cast(undType, exprDotMethod(ref("pair"), "getFirst")))
                .methodReturn(underlyingGetter.underlyingGetCodegen(ref("wrapped"), codegenMethodScope, codegenClassScope));
            return localMethod(method, ref("und"));
        } else {
            method.getBlock().declareVar(undType, "wrapped", cast(undType, ref("und")))
                .methodReturn(underlyingGetter.underlyingGetCodegen(ref("wrapped"), codegenMethodScope, codegenClassScope));
            return localMethod(method, ref("und"));
        }
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
