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
package com.espertech.esper.common.internal.event.json.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterNested implements JsonEventPropertyGetter {
    private final JsonUnderlyingField field;
    private final JsonEventPropertyGetter innerGetter;
    private final String underlyingClassName;

    public JsonGetterNested(JsonUnderlyingField field, JsonEventPropertyGetter innerGetter, String underlyingClassName) {
        this.field = field;
        this.innerGetter = innerGetter;
        this.underlyingClassName = underlyingClassName;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object value = JsonFieldGetterHelper.getJsonSimpleProp(field, object);
        if (value == null) {
            return null;
        }
        return innerGetter.getJsonProp(value);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(field.getPropertyType(), "inner", exprDotName(ref("und"), field.getFieldName()))
            .ifRefNullReturnNull("inner")
            .methodReturn(innerGetter.underlyingGetCodegen(ref("inner"), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(field.getPropertyType(), "inner", exprDotName(ref("und"), field.getFieldName()))
            .ifRefNull("inner").blockReturn(constantFalse())
            .methodReturn(innerGetter.underlyingExistsCodegen(ref("inner"), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(field.getPropertyType(), "inner", exprDotName(ref("und"), field.getFieldName()))
            .ifRefNull("inner").blockReturn(constantNull())
            .methodReturn(innerGetter.underlyingFragmentCodegen(ref("inner"), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return getJsonFragment(eventBean.getUnderlying());
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        Object value = JsonFieldGetterHelper.getJsonSimpleProp(field, object);
        if (value == null) {
            return false;
        }
        return innerGetter.getJsonExists(value);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        Object value = JsonFieldGetterHelper.getJsonSimpleProp(field, object);
        if (value == null) {
            return null;
        }
        return innerGetter.getJsonFragment(value);
    }
}
