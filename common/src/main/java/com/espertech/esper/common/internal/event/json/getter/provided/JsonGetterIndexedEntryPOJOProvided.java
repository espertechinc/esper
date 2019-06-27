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
package com.espertech.esper.common.internal.event.json.getter.provided;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.getter.BaseNativePropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.getter.core.JsonEventPropertyGetter;

import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.event.json.getter.provided.JsonFieldGetterHelperProvided.getJsonProvidedIndexedProp;

/**
 * A getter that works on POJO events residing within a Map as an event property.
 */
public class JsonGetterIndexedEntryPOJOProvided extends BaseNativePropertyGetter implements JsonEventPropertyGetter {

    private final Field field;
    private final int index;
    private final BeanEventPropertyGetter nestedGetter;

    public JsonGetterIndexedEntryPOJOProvided(Field field, int index, BeanEventPropertyGetter nestedGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class returnType) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, returnType, null);
        this.field = field;
        this.index = index;
        this.nestedGetter = nestedGetter;
    }

    public Class getTargetType() {
        return field.getDeclaringClass();
    }

    public Class getBeanPropType() {
        return Object.class;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(field.getDeclaringClass(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFieldCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(field.getDeclaringClass(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFieldExistsCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object result = getJsonProvidedIndexedProp(object, field, index);
        if (result == null) {
            return null;
        }
        return nestedGetter.getBeanProp(result);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        Object result = getJsonProvidedIndexedProp(object, field, index);
        if (result == null) {
            return false;
        }
        return nestedGetter.isBeanExistsProperty(result);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        Object result = getJsonProvidedIndexedProp(object, field, index);
        if (result == null) {
            return null;
        }
        return getFragmentFromValue(result);
    }

    private CodegenMethod getFieldCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(field.getDeclaringClass(), "und").getBlock()
            .declareVar(Object.class, "value", exprDotName(ref("und"), field.getName()))
            .methodReturn(localMethod(BaseNestableEventUtil.getBeanArrayValueCodegen(codegenMethodScope, codegenClassScope, nestedGetter, index), ref("value")));
    }

    private CodegenMethod getFieldExistsCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(field.getDeclaringClass(), "und").getBlock()
            .declareVar(Object.class, "value", exprDotName(ref("und"), field.getName()))
            .methodReturn(localMethod(BaseNestableEventUtil.getBeanArrayValueExistsCodegen(codegenMethodScope, codegenClassScope, nestedGetter, index), ref("value")));
    }
}
