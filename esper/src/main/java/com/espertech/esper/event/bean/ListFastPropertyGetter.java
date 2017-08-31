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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterAndIndexed;
import com.espertech.esper.event.vaevent.PropertyUtility;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for a list property identified by a given index, using the CGLIB fast method.
 */
public class ListFastPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final FastMethod fastMethod;
    private final int index;

    /**
     * Constructor.
     *
     * @param method              the underlying method
     * @param fastMethod          is the method to use to retrieve a value from the object
     * @param index               is tge index within the array to get the property from
     * @param eventAdapterService factory for event beans and event types
     */
    public ListFastPropertyGetter(Method method, FastMethod fastMethod, int index, EventAdapterService eventAdapterService) {
        super(eventAdapterService, JavaClassHelper.getGenericReturnType(method, false), null);
        this.index = index;
        this.fastMethod = fastMethod;

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

    public Object getBeanPropInternal(Object object, int index) throws PropertyAccessException {
        try {
            Object value = fastMethod.invoke(object, null);
            if (!(value instanceof List)) {
                return null;
            }
            List valueList = (List) value;
            if (valueList.size() <= index) {
                return null;
            }
            return valueList.get(index);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(fastMethod.getJavaMethod(), object, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(fastMethod.getJavaMethod(), e);
        }
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        Object underlying = obj.getUnderlying();
        return getBeanProp(underlying);
    }

    public String toString() {
        return "ListFastPropertyGetter " +
                " fastMethod=" + fastMethod.toString() +
                " index=" + index;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Class getBeanPropType() {
        return JavaClassHelper.getGenericReturnType(fastMethod.getJavaMethod(), false);
    }

    public Class getTargetType() {
        return fastMethod.getDeclaringClass();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(ListMethodPropertyGetter.getBeanPropInternalCodegen(codegenMethodScope, getBeanPropType(), getTargetType(), fastMethod.getJavaMethod(), codegenClassScope), underlyingExpression, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(ListMethodPropertyGetter.getBeanPropInternalCodegen(codegenMethodScope, getBeanPropType(), getTargetType(), fastMethod.getJavaMethod(), codegenClassScope), castUnderlying(getTargetType(), beanExpression), key);
    }
}
