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
import com.espertech.esper.common.internal.event.util.PropertyUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.util.CollectionUtil.getMapKeyExistsChecked;
import static com.espertech.esper.common.internal.util.CollectionUtil.getMapValueChecked;

/**
 * Getter for a key property identified by a given key value, using vanilla reflection.
 */
public class KeyedMapMethodPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndMapped {
    private final Method method;
    private final Object key;

    public KeyedMapMethodPropertyGetter(Method method, Object key, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, JavaClassHelper.getGenericReturnTypeMap(method, false), null);
        this.key = key;
        this.method = method;
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), mapKey);
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, key);
    }

    public Object getBeanPropInternal(Object object, Object key) throws PropertyAccessException {
        try {
            Object result = method.invoke(object, (Object[]) null);
            return getMapValueChecked(result, key);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(method, object, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(method, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(method, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(method, e);
        }
    }

    public boolean getBeanPropExistsInternal(Object object, Object key) throws PropertyAccessException {
        try {
            Object result = method.invoke(object, (Object[]) null);
            return getMapKeyExistsChecked(result, key);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(method, object, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(method, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(method, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(method, e);
        }
    }

    static CodegenMethod getBeanPropInternalCodegen(CodegenMethodScope codegenMethodScope, Class beanPropType, Class targetType, Method method, CodegenClassScope codegenClassScope) throws PropertyAccessException {
        return codegenMethodScope.makeChild(beanPropType, KeyedMapMethodPropertyGetter.class, codegenClassScope).addParam(targetType, "object").addParam(Object.class, "key").getBlock()
                .declareVar(method.getReturnType(), "result", exprDotMethod(ref("object"), method.getName()))
                .ifRefNotTypeReturnConst("result", Map.class, null)
                .methodReturn(cast(beanPropType, exprDotMethod(cast(Map.class, ref("result")), "get", ref("key"))));
    }

    static CodegenMethod getBeanPropExistsInternalCodegen(CodegenMethodScope codegenMethodScope, Class beanPropType, Class targetType, Method method, CodegenClassScope codegenClassScope) throws PropertyAccessException {
        return codegenMethodScope.makeChild(boolean.class, KeyedMapMethodPropertyGetter.class, codegenClassScope).addParam(targetType, "object").addParam(Object.class, "key").getBlock()
            .declareVar(method.getReturnType(), "result", exprDotMethod(ref("object"), method.getName()))
            .ifRefNotTypeReturnConst("result", Map.class, false)
            .methodReturn(exprDotMethod(cast(Map.class, ref("result")), "containsKey", ref("key")));
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        Object underlying = obj.getUnderlying();
        return getBeanProp(underlying);
    }

    public String toString() {
        return "KeyedMapMethodPropertyGetter " +
                " method=" + method.toString() +
                " key=" + key;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object underlying = eventBean.getUnderlying();
        return getBeanPropExistsInternal(underlying, key);
    }

    public Class getBeanPropType() {
        return JavaClassHelper.getGenericReturnTypeMap(method, false);
    }

    public Class getTargetType() {
        return method.getDeclaringClass();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getBeanPropInternalCodegen(codegenMethodScope, getBeanPropType(), getTargetType(), method, codegenClassScope), underlyingExpression, constant(key));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getBeanPropExistsInternalCodegen(codegenMethodScope, getBeanPropType(), getTargetType(), method, codegenClassScope), underlyingExpression, constant(key));
    }

    public CodegenExpression eventBeanGetMappedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(getBeanPropInternalCodegen(codegenMethodScope, getBeanPropType(), getTargetType(), method, codegenClassScope), castUnderlying(getTargetType(), beanExpression), key);
    }
}