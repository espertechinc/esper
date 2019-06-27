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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Iterator;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Base getter for native fragments.
 */
public abstract class BaseNativePropertyGetter implements EventPropertyGetterSPI {
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final BeanEventTypeFactory beanEventTypeFactory;
    private volatile BeanEventType fragmentEventType;
    private final Class fragmentClassType;
    protected boolean isFragmentable;
    private final boolean isArray;
    private final boolean isIterable;

    public abstract Class getTargetType();

    public abstract Class getBeanPropType();

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param object                     array
     * @param fragmentEventType          fragment type
     * @param eventBeanTypedEventFactory event adapters
     * @return array
     */
    public static Object toFragmentArray(Object[] object, BeanEventType fragmentEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        EventBean[] events = new EventBean[object.length];
        int countFilled = 0;

        for (int i = 0; i < object.length; i++) {
            Object element = Array.get(object, i);
            if (element == null) {
                continue;
            }

            events[countFilled] = eventBeanTypedEventFactory.adapterForTypedBean(element, fragmentEventType);
            countFilled++;
        }

        if (countFilled == object.length) {
            return events;
        }

        if (countFilled == 0) {
            return new EventBean[0];
        }

        EventBean[] returnVal = new EventBean[countFilled];
        System.arraycopy(events, 0, returnVal, 0, countFilled);
        return returnVal;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the fragment for dynamic properties.
     *
     * @param object                     to inspect
     * @param eventBeanTypedEventFactory factory for event beans and event types
     * @param beanEventTypeFactory       bean factory
     * @return fragment
     */
    public static Object getFragmentDynamic(Object object, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        if (object == null) {
            return null;
        }

        BeanEventType fragmentEventType = null;
        boolean isArray = false;
        if (object.getClass().isArray()) {
            if (JavaClassHelper.isFragmentableType(object.getClass().getComponentType())) {
                isArray = true;
                fragmentEventType = beanEventTypeFactory.getCreateBeanType(object.getClass().getComponentType(), false);
            }
        } else {
            if (JavaClassHelper.isFragmentableType(object.getClass())) {
                fragmentEventType = beanEventTypeFactory.getCreateBeanType(object.getClass(), false);
            }
        }

        if (fragmentEventType == null) {
            return null;
        }

        if (isArray) {
            int len = Array.getLength(object);
            EventBean[] events = new EventBean[len];
            int countFilled = 0;

            for (int i = 0; i < len; i++) {
                Object element = Array.get(object, i);
                if (element == null) {
                    continue;
                }

                events[countFilled] = eventBeanTypedEventFactory.adapterForTypedBean(element, fragmentEventType);
                countFilled++;
            }

            if (countFilled == len) {
                return events;
            }

            if (countFilled == 0) {
                return new EventBean[0];
            }

            EventBean[] returnVal = new EventBean[countFilled];
            System.arraycopy(events, 0, returnVal, 0, countFilled);
            return returnVal;
        } else {
            return eventBeanTypedEventFactory.adapterForTypedBean(object, fragmentEventType);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the fragment for dynamic properties.
     *
     * @param object                     to inspect
     * @param fragmentEventType          type
     * @param eventBeanTypedEventFactory factory for event beans and event types
     * @return fragment
     */
    public static Object toFragmentIterable(Object object, BeanEventType fragmentEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        if (!(object instanceof Iterable)) {
            return null;
        }
        Iterator iterator = ((Iterable) object).iterator();
        if (!iterator.hasNext()) {
            return new EventBean[0];
        }
        ArrayDeque<EventBean> events = new ArrayDeque<EventBean>();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next == null) {
                continue;
            }

            events.add(eventBeanTypedEventFactory.adapterForTypedBean(next, fragmentEventType));
        }
        return events.toArray(new EventBean[events.size()]);
    }

    public BaseNativePropertyGetter(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class returnType, Class genericType) {
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.beanEventTypeFactory = beanEventTypeFactory;
        if (returnType.isArray()) {
            this.fragmentClassType = returnType.getComponentType();
            isArray = true;
            isIterable = false;
        } else if (JavaClassHelper.isImplementsInterface(returnType, Iterable.class)) {
            this.fragmentClassType = genericType;
            isArray = false;
            isIterable = true;
        } else {
            this.fragmentClassType = returnType;
            isArray = false;
            isIterable = false;
        }
        isFragmentable = true;
    }

    public final Object getFragment(EventBean eventBean) {
        determineFragmentable();
        if (!isFragmentable) {
            return null;
        }

        Object object = get(eventBean);
        if (object == null) {
            return null;
        }

        return getFragmentFromObject(object);
    }

    public Object getFragmentFromValue(Object valueReturnedByGet) {
        determineFragmentable();
        if (!isFragmentable) {
            return null;
        }
        return getFragmentFromObject(valueReturnedByGet);
    }

    private Object getFragmentFromObject(Object value) {
        if (isArray) {
            return toFragmentArray((Object[]) value, fragmentEventType, eventBeanTypedEventFactory);
        } else if (isIterable) {
            return toFragmentIterable(value, fragmentEventType, eventBeanTypedEventFactory);
        } else {
            return eventBeanTypedEventFactory.adapterForTypedBean(value, fragmentEventType);
        }
    }

    private CodegenMethod getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField msvc = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField mtype = codegenClassScope.addFieldUnshared(false, BeanEventType.class, cast(BeanEventType.class, EventTypeUtility.resolveTypeCodegen(fragmentEventType, EPStatementInitServices.REF)));

        CodegenBlock block = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(getTargetType(), "underlying").getBlock()
                .declareVar(getBeanPropType(), "object", underlyingGetCodegen(ref("underlying"), codegenMethodScope, codegenClassScope))
                .ifRefNullReturnNull("object");

        if (isArray) {
            return block.methodReturn(staticMethod(BaseNativePropertyGetter.class, "toFragmentArray", cast(Object[].class, ref("object")), mtype, msvc));
        }
        if (isIterable) {
            return block.methodReturn(staticMethod(BaseNativePropertyGetter.class, "toFragmentIterable", ref("object"), mtype, msvc));
        }
        return block.methodReturn(exprDotMethod(msvc, "adapterForTypedBean", ref("object"), mtype));
    }

    public final CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        determineFragmentable();
        if (!isFragmentable) {
            return constantNull();
        }
        return underlyingFragmentCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public final CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        determineFragmentable();
        if (!isFragmentable) {
            return constantNull();
        }
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    private void determineFragmentable() {
        if (fragmentEventType == null) {
            if (JavaClassHelper.isFragmentableType(fragmentClassType)) {
                fragmentEventType = beanEventTypeFactory.getCreateBeanType(fragmentClassType, false);
            } else {
                isFragmentable = false;
            }
        }
    }
}
