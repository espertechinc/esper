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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterAndIndexed;
import com.espertech.esper.common.internal.event.util.PropertyUtility;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for an array property identified by a given index, using vanilla reflection.
 */
public class ArrayMethodPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final Method method;
    private final int index;

    public ArrayMethodPropertyGetter(Method method, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, method.getReturnType().getComponentType(), null);
        this.index = index;
        this.method = method;

        if (index < 0) {
            throw new IllegalArgumentException("Invalid negative index value");
        }
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), index);
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, index);
    }

    private Object getBeanPropInternal(Object object, int index) throws PropertyAccessException {
        try {
            Object value = method.invoke(object, (Object[]) null);
            return CollectionUtil.arrayValueAtIndex(value, index);
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

    private boolean getBeanPropInternalExists(Object object, int index) throws PropertyAccessException {
        try {
            Object value = method.invoke(object, (Object[]) null);
            return CollectionUtil.arrayExistsAtIndex(value, index);
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

    protected static CodegenMethod getBeanPropInternalCode(CodegenMethodScope codegenMethodScope, Method method, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(JavaClassHelper.getBoxedType(method.getReturnType().getComponentType()), ArrayMethodPropertyGetter.class, codegenClassScope).addParam(method.getDeclaringClass(), "obj").addParam(int.class, "index").getBlock()
                .declareVar(method.getReturnType(), "array", exprDotMethod(ref("obj"), method.getName()))
                .ifRefNullReturnNull("array")
                .ifConditionReturnConst(relational(arrayLength(ref("array")), CodegenExpressionRelational.CodegenRelational.LE, ref("index")), null)
                .methodReturn(arrayAtIndex(ref("array"), ref("index")));
    }

    protected static CodegenMethod getBeanPropInternalExistsCode(CodegenMethodScope codegenMethodScope, Method method, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(boolean.class, ArrayMethodPropertyGetter.class, codegenClassScope).addParam(method.getDeclaringClass(), "obj").addParam(int.class, "index").getBlock()
            .declareVar(method.getReturnType(), "array", exprDotMethod(ref("obj"), method.getName()))
            .ifRefNullReturnFalse("array")
            .ifConditionReturnConst(relational(arrayLength(ref("array")), CodegenExpressionRelational.CodegenRelational.LE, ref("index")), false)
            .methodReturn(constantTrue());
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        Object underlying = obj.getUnderlying();
        return getBeanProp(underlying);
    }

    public String toString() {
        return "ArrayMethodPropertyGetter " +
                " method=" + method.toString() +
                " index=" + index;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object underlying = eventBean.getUnderlying();
        return getBeanPropInternalExists(underlying, index);
    }

    public Class getBeanPropType() {
        return method.getReturnType().getComponentType();
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
        return localMethod(getBeanPropInternalCode(codegenMethodScope, method, codegenClassScope), underlyingExpression, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getBeanPropInternalExistsCode(codegenMethodScope, method, codegenClassScope), underlyingExpression, constant(index));
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(getBeanPropInternalCode(codegenMethodScope, method, codegenClassScope), castUnderlying(getTargetType(), beanExpression), key);
    }
}
