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
package com.espertech.esper.common.internal.event.json.getter.fromschema;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;
import com.espertech.esper.common.internal.event.json.getter.core.JsonEventPropertyGetter;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterDynamicNestedSchema implements JsonEventPropertyGetter {
    private final String propertyName;
    private final JsonEventPropertyGetter innerGetter;
    private final String underlyingClassName;

    public JsonGetterDynamicNestedSchema(String propertyName, JsonEventPropertyGetter innerGetter, String underlyingClassName) {
        this.propertyName = propertyName;
        this.innerGetter = innerGetter;
        this.underlyingClassName = underlyingClassName;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object inner = ((Map<String, Object>) object).get(propertyName);
        if (inner == null) {
            return null;
        }
        return innerGetter.getJsonProp(inner);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(Object.class, "inner", exprDotMethod(ref("und"), "get", constant(propertyName)))
            .ifNotInstanceOf("inner", Map.class).blockReturn(constantNull())
            .methodReturn(innerGetter.underlyingGetCodegen(cast(Map.class, ref("inner")), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(Object.class, "inner", exprDotMethod(ref("und"), "get", constant(propertyName)))
            .ifNotInstanceOf("inner", Map.class).blockReturn(constantFalse())
            .methodReturn(innerGetter.underlyingExistsCodegen(cast(Map.class, ref("inner")), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        return null;
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        Object inner = ((JsonEventObjectBase) object).get(propertyName);
        if (!(inner instanceof Map)) {
            return false;
        }
        return innerGetter.getJsonExists(inner);
    }
}
