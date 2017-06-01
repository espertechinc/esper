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
import com.espertech.esper.event.EventPropertyGetterSPI;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class WrapperUnderlyingPropertyGetter implements EventPropertyGetterSPI {
    private final EventPropertyGetterSPI underlyingGetter;

    public WrapperUnderlyingPropertyGetter(EventPropertyGetterSPI underlyingGetter) {
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

    private String getCodegen(CodegenContext context) {
        return context.addMethod(Object.class, EventBean.class, "theEvent", this.getClass())
                .declareVarWCast(DecoratingEventBean.class, "wrapperEvent", "theEvent")
                .declareVar(EventBean.class, "wrappedEvent", exprDotMethod(ref("wrapperEvent"), "getUnderlyingEvent"))
                .ifRefNullReturnNull("wrappedEvent")
                .methodReturn(underlyingGetter.codegenEventBeanGet(ref("wrappedEvent"), context));
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

    private String getFragmentCodegen(CodegenContext context) {
        return context.addMethod(Object.class, EventBean.class, "theEvent", this.getClass())
                .declareVarWCast(DecoratingEventBean.class, "wrapperEvent", "theEvent")
                .declareVar(EventBean.class, "wrappedEvent", exprDotMethod(ref("wrapperEvent"), "getUnderlyingEvent"))
                .ifRefNullReturnNull("wrappedEvent")
                .methodReturn(underlyingGetter.codegenEventBeanFragment(ref("wrappedEvent"), context));
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return localMethod(getCodegen(context), beanExpression);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), beanExpression);
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        throw implementationNotProvided();
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        throw implementationNotProvided();
    }

    private UnsupportedOperationException implementationNotProvided() {
        return new UnsupportedOperationException("Wrapper event type does not provide an implementation for underlying get");
    }
}
