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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterAndIndexed;
import com.espertech.esper.event.vaevent.PropertyUtility;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for an iterable property identified by a given index, using vanilla reflection.
 */
public class IterableMethodPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final Method method;
    private final int index;

    /**
     * Constructor.
     *
     * @param method              is the method to use to retrieve a value from the object
     * @param index               is tge index within the array to get the property from
     * @param eventAdapterService factory for event beans and event types
     */
    public IterableMethodPropertyGetter(Method method, int index, EventAdapterService eventAdapterService) {
        super(eventAdapterService, JavaClassHelper.getGenericReturnType(method, false), null);
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

    protected static String getBeanPropCodegen(CodegenContext context, Class beanPropType, Class targetType, Method method) {
        return context.addMethod(beanPropType, IterableMethodPropertyGetter.class).add(targetType, "object").add(int.class, "index").begin()
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

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constant(true);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getBeanPropCodegen(context, getBeanPropType(), getTargetType(), method), underlyingExpression, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constant(true);
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenContext context, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(getBeanPropCodegen(context, getBeanPropType(), getTargetType(), method), castUnderlying(getTargetType(), beanExpression), constant(index));
    }
}
