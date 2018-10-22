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
import com.espertech.esper.common.internal.event.core.EventPropertyGetterAndIndexed;
import com.espertech.esper.common.internal.event.util.PropertyUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for an iterable property identified by a given index, using vanilla reflection.
 */
public class IterableMethodPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final Method method;
    private final int index;

    public IterableMethodPropertyGetter(Method method, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, JavaClassHelper.getGenericReturnType(method, false), null);
        this.index = index;
        this.method = method;

        if (index < 0) {
            throw new IllegalArgumentException("Invalid negative index value");
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the iterable at a certain index, or null.
     *
     * @param value the iterable
     * @param index index
     * @return value at index
     */
    public static Object getBeanEventIterableValue(Object value, int index) {
        if (!(value instanceof Iterable)) {
            return null;
        }

        Iterator it = ((Iterable) value).iterator();

        if (index == 0) {
            if (it.hasNext()) {
                return it.next();
            }
            return null;
        }

        int count = 0;
        while (true) {
            if (!it.hasNext()) {
                return null;
            }
            if (count < index) {
                it.next();
            } else {
                return it.next();
            }
            count++;
        }
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, index);
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), index);
    }

    private Object getBeanPropInternal(Object object, int index) throws PropertyAccessException {
        try {
            Object value = method.invoke(object, (Object[]) null);
            return getBeanEventIterableValue(value, index);
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

    protected static CodegenMethod getBeanPropCodegen(CodegenMethodScope codegenMethodScope, Class beanPropType, Class targetType, Method method, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(beanPropType, IterableMethodPropertyGetter.class, codegenClassScope).addParam(targetType, "object").addParam(int.class, "index").getBlock()
                .declareVar(Object.class, "value", exprDotMethod(ref("object"), method.getName()))
                .methodReturn(cast(beanPropType, staticMethod(IterableMethodPropertyGetter.class, "getBeanEventIterableValue", ref("value"), ref("index"))));
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        Object underlying = obj.getUnderlying();
        return getBeanProp(underlying);
    }

    public String toString() {
        return "IterableMethodPropertyGetter " +
                " method=" + method.toString() +
                " index=" + index;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Class getBeanPropType() {
        return JavaClassHelper.getGenericReturnType(method, false);
    }

    public Class getTargetType() {
        return method.getDeclaringClass();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constant(true);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getBeanPropCodegen(codegenMethodScope, getBeanPropType(), getTargetType(), method, codegenClassScope), underlyingExpression, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constant(true);
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(getBeanPropCodegen(codegenMethodScope, getBeanPropType(), getTargetType(), method, codegenClassScope), castUnderlying(getTargetType(), beanExpression), constant(index));
    }
}
