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
package com.espertech.esper.common.internal.event.json.getter.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class JsonGetterNestedBase implements JsonEventPropertyGetter {
    protected final JsonEventPropertyGetter innerGetter;
    protected final String underlyingClassName;

    public abstract String getFieldName();
    public abstract Class getFieldType();

    public JsonGetterNestedBase(JsonEventPropertyGetter innerGetter, String underlyingClassName) {
        this.innerGetter = innerGetter;
        this.underlyingClassName = underlyingClassName;
    }

    public final Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public final CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public final CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(getFieldType(), "inner", exprDotName(ref("und"), getFieldName()))
            .ifRefNullReturnNull("inner")
            .methodReturn(innerGetter.underlyingGetCodegen(ref("inner"), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public final CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public final CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(getFieldType(), "inner", exprDotName(ref("und"), getFieldName()))
            .ifRefNull("inner").blockReturn(constantFalse())
            .methodReturn(innerGetter.underlyingExistsCodegen(ref("inner"), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public final CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public final CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(getFieldType(), "inner", exprDotName(ref("und"), getFieldName()))
            .ifRefNull("inner").blockReturn(constantNull())
            .methodReturn(innerGetter.underlyingFragmentCodegen(ref("inner"), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public final boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public final Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return getJsonFragment(eventBean.getUnderlying());
    }
}
