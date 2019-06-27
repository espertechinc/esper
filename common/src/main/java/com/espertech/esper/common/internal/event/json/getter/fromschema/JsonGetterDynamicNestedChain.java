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
import com.espertech.esper.common.internal.event.json.getter.core.JsonEventPropertyGetter;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterDynamicNestedChain implements JsonEventPropertyGetter {
    private final String underlyingClassName;
    private final JsonEventPropertyGetter[] getters;

    public JsonGetterDynamicNestedChain(String underlyingClassName, JsonEventPropertyGetter[] getters) {
        this.underlyingClassName = underlyingClassName;
        this.getters = getters;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpression resultExpression = getters[0].underlyingGetCodegen(underlyingExpression, codegenMethodScope, codegenClassScope);
        return localMethod(handleGetterTrailingChainCodegen(codegenMethodScope, codegenClassScope), resultExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpression resultExpression = getters[0].underlyingGetCodegen(underlyingExpression, codegenMethodScope, codegenClassScope);
        return localMethod(handleGetterTrailingExistsCodegen(codegenMethodScope, codegenClassScope), resultExpression);
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

    private CodegenMethod handleGetterTrailingChainCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object.class, "result");
        for (int i = 1; i < getters.length; i++) {
            JsonEventPropertyGetter getter = getters[i];
            method.getBlock()
                .ifRefNullReturnNull("result")
                .ifNotInstanceOf("result", Map.class).blockReturn(constantNull())
                .assignRef("result", getter.underlyingGetCodegen(castRef(Map.class, "result"), codegenMethodScope, codegenClassScope));
        }
        method.getBlock().methodReturn(ref("result"));
        return method;
    }

    private CodegenMethod handleGetterTrailingExistsCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(Object.class, "result");
        for (int i = 1; i < getters.length - 1; i++) {
            JsonEventPropertyGetter getter = getters[i];
            method.getBlock()
                .ifRefNull("result").blockReturn(constantFalse())
                .ifNotInstanceOf("result", Map.class).blockReturn(constantFalse())
                .assignRef("result", getter.underlyingGetCodegen(castRef(Map.class, "result"), codegenMethodScope, codegenClassScope));
        }
        method.getBlock()
            .ifRefNull("result").blockReturn(constantFalse())
            .ifNotInstanceOf("result", Map.class).blockReturn(constantFalse())
            .methodReturn(getters[getters.length - 1].underlyingExistsCodegen(castRef(Map.class, "result"), codegenMethodScope, codegenClassScope));
        return method;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object result = getters[0].getJsonProp(object);
        for (int i = 1; i < getters.length; i++) {
            if (!(result instanceof Map)) {
                return null;
            }
            JsonEventPropertyGetter getter = getters[i];
            result = getter.getJsonProp(result);
        }
        return result;
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        Object result = getters[0].getJsonProp(object);
        for (int i = 1; i < getters.length - 1; i++) {
            if (!(result instanceof Map)) {
                return false;
            }
            JsonEventPropertyGetter getter = getters[i];
            result = getter.getJsonProp(result);
        }
        if (!(result instanceof Map)) {
            return false;
        }
        return getters[getters.length - 1].getJsonExists(result);
    }
}
