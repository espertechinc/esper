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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GE;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterNestedArrayIndexed implements JsonEventPropertyGetter {
    private final JsonUnderlyingField field;
    private final int index;
    private final JsonEventPropertyGetter innerGetter;
    private final String underlyingClassName;
    private final EventType eventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public JsonGetterNestedArrayIndexed(JsonUnderlyingField field, int index, JsonEventPropertyGetter innerGetter, String underlyingClassName, EventType eventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.field = field;
        this.index = index;
        this.innerGetter = innerGetter;
        this.underlyingClassName = underlyingClassName;
        this.eventType = eventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object item = JsonFieldGetterHelper.getJsonIndexedProp(object, field.getPropertyNumber(), index);
        if (item == null) {
            return null;
        }
        return innerGetter.getJsonProp(item);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(field.getPropertyType(), "inner", exprDotName(ref("und"), field.getFieldName()))
            .ifRefNullReturnNull("inner")
            .ifCondition(relational(constant(index), GE, exprDotName(ref("inner"), "length")))
            .blockReturn(constantNull())
            .methodReturn(innerGetter.underlyingGetCodegen(arrayAtIndex(ref("inner"), constant(index)), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(field.getPropertyType(), "inner", exprDotName(ref("und"), field.getFieldName()))
            .ifRefNullReturnFalse("inner")
            .ifCondition(relational(constant(index), GE, exprDotName(ref("inner"), "length")))
            .blockReturn(constantFalse())
            .methodReturn(innerGetter.underlyingExistsCodegen(arrayAtIndex(ref("inner"), constant(index)), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(underlyingClassName, "und");
        method.getBlock()
            .declareVar(field.getPropertyType(), "inner", exprDotName(ref("und"), field.getFieldName()))
            .ifRefNullReturnNull("inner")
            .ifCondition(relational(constant(index), GE, exprDotName(ref("inner"), "length")))
            .blockReturn(constantNull())
            .methodReturn(innerGetter.underlyingFragmentCodegen(arrayAtIndex(ref("inner"), constant(index)), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return getJsonFragment(eventBean.getUnderlying());
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        Object item = JsonFieldGetterHelper.getJsonIndexedProp(object, field.getPropertyNumber(), index);
        if (item == null) {
            return false;
        }
        return innerGetter.getJsonExists(item);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        Object item = JsonFieldGetterHelper.getJsonIndexedProp(object, field.getPropertyNumber(), index);
        if (item == null) {
            return null;
        }
        return innerGetter.getJsonFragment(item);
    }

}
