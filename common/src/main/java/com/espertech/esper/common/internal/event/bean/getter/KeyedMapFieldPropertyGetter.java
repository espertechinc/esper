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
package com.espertech.esper.common.internal.event.bean.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterAndMapped;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Field;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for a key property identified by a given key value, using vanilla reflection.
 */
public class KeyedMapFieldPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndMapped {
    private final Field field;
    private final Object key;

    public KeyedMapFieldPropertyGetter(Field field, Object key, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, JavaClassHelper.getGenericFieldTypeMap(field, false), null);
        this.key = key;
        this.field = field;
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        return BeanFieldGetterHelper.getFieldMap(field, eventBean.getUnderlying(), mapKey);
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return BeanFieldGetterHelper.getFieldMap(field, object, key);
    }

    private CodegenMethod getBeanPropInternalCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) throws PropertyAccessException {
        return codegenMethodScope.makeChild(getBeanPropType(), this.getClass(), codegenClassScope).addParam(getTargetType(), "object").addParam(Object.class, "key").getBlock()
                .declareVar(Object.class, "result", exprDotName(ref("object"), field.getName()))
                .ifRefNotTypeReturnConst("result", Map.class, null)
                .declareVarWCast(Map.class, "map", "result")
                .methodReturn(cast(getBeanPropType(), exprDotMethod(ref("map"), "get", ref("key"))));
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        Object underlying = obj.getUnderlying();
        return getBeanProp(underlying);
    }

    public String toString() {
        return "KeyedMapFieldPropertyGetter " +
                " field=" + field.toString() +
                " key=" + key;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Class getBeanPropType() {
        return JavaClassHelper.getGenericFieldTypeMap(field, false);
    }

    public Class getTargetType() {
        return field.getDeclaringClass();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getBeanPropInternalCodegen(codegenMethodScope, codegenClassScope), underlyingExpression, constant(key));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanGetMappedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(getBeanPropInternalCodegen(codegenMethodScope, codegenClassScope), castUnderlying(getTargetType(), beanExpression), key);
    }
}