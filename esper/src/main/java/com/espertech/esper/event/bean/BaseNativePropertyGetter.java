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
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Iterator;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Base getter for native fragments.
 */
public abstract class BaseNativePropertyGetter implements EventPropertyGetterSPI {
    private final EventAdapterService eventAdapterService;
    private volatile BeanEventType fragmentEventType;
    private final Class fragmentClassType;
    private boolean isFragmentable;
    private final boolean isArray;
    private final boolean isIterable;

    public abstract Class getTargetType();

    public abstract Class getBeanPropType();

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param object              array
     * @param fragmentEventType   fragment type
     * @param eventAdapterService event adapters
     * @return array
     */
    public static Object toFragmentArray(Object[] object, BeanEventType fragmentEventType, EventAdapterService eventAdapterService) {
        EventBean[] events = new EventBean[object.length];
        int countFilled = 0;

        for (int i = 0; i < object.length; i++) {
            Object element = Array.get(object, i);
            if (element == null) {
                continue;
            }

            events[countFilled] = eventAdapterService.adapterForTypedBean(element, fragmentEventType);
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
     * @param object              to inspect
     * @param eventAdapterService factory for event beans and event types
     * @return fragment
     */
    public static Object getFragmentDynamic(Object object, EventAdapterService eventAdapterService) {
        if (object == null) {
            return null;
        }

        BeanEventType fragmentEventType = null;
        boolean isArray = false;
        if (object.getClass().isArray()) {
            if (JavaClassHelper.isFragmentableType(object.getClass().getComponentType())) {
                isArray = true;
                fragmentEventType = eventAdapterService.getBeanEventTypeFactory().createBeanTypeDefaultName(object.getClass().getComponentType());
            }
        } else {
            if (JavaClassHelper.isFragmentableType(object.getClass())) {
                fragmentEventType = eventAdapterService.getBeanEventTypeFactory().createBeanTypeDefaultName(object.getClass());
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

                events[countFilled] = eventAdapterService.adapterForTypedBean(element, fragmentEventType);
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
            return eventAdapterService.adapterForTypedBean(object, fragmentEventType);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the fragment for dynamic properties.
     *
     * @param object              to inspect
     * @param fragmentEventType   type
     * @param eventAdapterService factory for event beans and event types
     * @return fragment
     */
    public static Object toFragmentIterable(Object object, BeanEventType fragmentEventType, EventAdapterService eventAdapterService) {
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

            events.add(eventAdapterService.adapterForTypedBean(next, fragmentEventType));
        }
        return events.toArray(new EventBean[events.size()]);
    }

    /**
     * Constructor.
     *
     * @param eventAdapterService factory for event beans and event types
     * @param returnType          type of the entry returned
     * @param genericType         type generic parameter, if any
     */
    public BaseNativePropertyGetter(EventAdapterService eventAdapterService, Class returnType, Class genericType) {
        this.eventAdapterService = eventAdapterService;
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

    public Object getFragment(EventBean eventBean) {
        determineFragmentable();
        if (!isFragmentable) {
            return null;
        }

        Object object = get(eventBean);
        if (object == null) {
            return null;
        }

        if (isArray) {
            return toFragmentArray((Object[]) object, fragmentEventType, eventAdapterService);
        } else if (isIterable) {
            return toFragmentIterable(object, fragmentEventType, eventAdapterService);
        } else {
            return eventAdapterService.adapterForTypedBean(object, fragmentEventType);
        }
    }

    private CodegenMethodNode getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember msvc = codegenClassScope.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mtype = codegenClassScope.makeAddMember(BeanEventType.class, fragmentEventType);

        CodegenBlock block = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(getTargetType(), "underlying").getBlock()
                .declareVar(getBeanPropType(), "object", underlyingGetCodegen(ref("underlying"), codegenMethodScope, codegenClassScope))
                .ifRefNullReturnNull("object");

        if (isArray) {
            return block.methodReturn(staticMethod(BaseNativePropertyGetter.class, "toFragmentArray", cast(Object[].class, ref("object")), member(mtype.getMemberId()), member(msvc.getMemberId())));
        }
        if (isIterable) {
            return block.methodReturn(staticMethod(BaseNativePropertyGetter.class, "toFragmentIterable", ref("object"), member(mtype.getMemberId()), member(msvc.getMemberId())));
        }
        return block.methodReturn(exprDotMethod(member(msvc.getMemberId()), "adapterForTypedBean", ref("object"), member(mtype.getMemberId())));
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
                fragmentEventType = eventAdapterService.getBeanEventTypeFactory().createBeanTypeDefaultName(fragmentClassType);
            } else {
                isFragmentable = false;
            }
        }
    }
}
