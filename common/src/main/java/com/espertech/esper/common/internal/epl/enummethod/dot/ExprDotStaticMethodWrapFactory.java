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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class ExprDotStaticMethodWrapFactory {

    public static ExprDotStaticMethodWrap make(Method method, List<ExprChainedSpec> modifiedChain, String optionalEventTypeName, ExprValidationContext validationContext)
            throws ExprValidationException {

        if (modifiedChain.isEmpty() || (!EnumMethodResolver.isEnumerationMethod(modifiedChain.get(0).getName(), validationContext.getClasspathImportService()))) {
            return null;
        }

        if (method.getReturnType().isArray() && method.getReturnType().getComponentType() == EventBean.class) {
            EventType eventType = requireEventType(method, optionalEventTypeName, validationContext);
            return new ExprDotStaticMethodWrapEventBeanArr(eventType);
        }

        if (method.getReturnType().isArray()) {
            Class componentType = method.getReturnType().getComponentType();
            if (componentType == null || JavaClassHelper.isJavaBuiltinDataType(componentType)) {
                return new ExprDotStaticMethodWrapArrayScalar(method.getName(), method.getReturnType());
            }

            BeanEventType type = makeBeanType(method.getName(), componentType, validationContext);
            return new ExprDotStaticMethodWrapArrayEvents(null, type);
        }

        if (JavaClassHelper.isImplementsInterface(method.getReturnType(), Collection.class)) {
            Class genericType = JavaClassHelper.getGenericReturnType(method, true);

            if (genericType == EventBean.class) {
                EventType eventType = requireEventType(method, optionalEventTypeName, validationContext);
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

            BeanEventType type = makeBeanType(method.getName(), genericType, validationContext);
            return new ExprDotStaticMethodWrapIterableEvents(validationContext.getEventBeanTypedEventFactory(), type);
        }
        return null;
    }

    private static BeanEventType makeBeanType(String methodName, Class clazz, ExprValidationContext validationContext) {
        String eventTypeName = validationContext.getStatementCompileTimeService().getEventTypeNameGeneratorStatement().getAnonymousTypeNameUDFMethod(methodName, clazz.getName());
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, validationContext.getModuleName(), EventTypeTypeClass.UDFDERIVED, EventTypeApplicationType.CLASS, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        BeanEventTypeStem stem = validationContext.getStatementCompileTimeService().getBeanEventTypeStemService().getCreateStem(clazz, null);
        BeanEventType beantype = new BeanEventType(stem, metadata, validationContext.getStatementCompileTimeService().getBeanEventTypeFactoryPrivate(), null, null, null, null);
        validationContext.getStatementCompileTimeService().getEventTypeCompileTimeRegistry().newType(beantype);
        return beantype;
    }

    private static EventType requireEventType(Method method, String optionalEventTypeName, ExprValidationContext ctx) throws ExprValidationException {
        return EventTypeUtility.requireEventType("Method", method.getName(), optionalEventTypeName, ctx.getStatementCompileTimeService().getEventTypeCompileTimeResolver());
    }
}



