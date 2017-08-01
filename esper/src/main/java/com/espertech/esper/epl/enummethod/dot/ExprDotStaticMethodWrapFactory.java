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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class ExprDotStaticMethodWrapFactory {

    public static ExprDotStaticMethodWrap make(Method method, EventAdapterService eventAdapterService, List<ExprChainedSpec> modifiedChain, String optionalEventTypeName)
            throws ExprValidationException {

        if (modifiedChain.isEmpty() || (!EnumMethodEnum.isEnumerationMethod(modifiedChain.get(0).getName()))) {
            return null;
        }

        if (method.getReturnType().isArray() && method.getReturnType().getComponentType() == EventBean.class) {
            EventType eventType = requireEventType(method, eventAdapterService, optionalEventTypeName);
            return new ExprDotStaticMethodWrapEventBeanArr(eventType);
        }

        if (method.getReturnType().isArray()) {
            Class componentType = method.getReturnType().getComponentType();
            if (componentType == null || JavaClassHelper.isJavaBuiltinDataType(componentType)) {
                return new ExprDotStaticMethodWrapArrayScalar(method.getName(), method.getReturnType());
            }
            BeanEventType type = (BeanEventType) eventAdapterService.addBeanType(componentType.getName(), componentType, false, false, false);
            return new ExprDotStaticMethodWrapArrayEvents(eventAdapterService, type);
        }

        if (JavaClassHelper.isImplementsInterface(method.getReturnType(), Collection.class)) {
            Class genericType = JavaClassHelper.getGenericReturnType(method, true);

            if (genericType == EventBean.class) {
                EventType eventType = requireEventType(method, eventAdapterService, optionalEventTypeName);
                return new ExprDotStaticMethodWrapEventBeanColl(eventType);
            }

            if (genericType == null || JavaClassHelper.isJavaBuiltinDataType(genericType)) {
                return new ExprDotStaticMethodWrapCollection(method.getName(), genericType);
            }
        }

        if (JavaClassHelper.isImplementsInterface(method.getReturnType(), Iterable.class)) {
            Class genericType = JavaClassHelper.getGenericReturnType(method, true);
            if (genericType == null || JavaClassHelper.isJavaBuiltinDataType(genericType)) {
                return new ExprDotStaticMethodWrapIterableScalar(method.getName(), genericType);
            }
            BeanEventType type = (BeanEventType) eventAdapterService.addBeanType(genericType.getName(), genericType, false, false, false);
            return new ExprDotStaticMethodWrapIterableEvents(eventAdapterService, type);
        }
        return null;
    }

    private static EventType requireEventType(Method method, EventAdapterService eventAdapterService, String optionalEventTypeName) throws ExprValidationException {
        return EventTypeUtility.requireEventType("Method", method.getName(), eventAdapterService, optionalEventTypeName);
    }
}



