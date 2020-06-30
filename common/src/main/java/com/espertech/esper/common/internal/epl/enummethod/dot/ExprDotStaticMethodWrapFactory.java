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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.epl.expression.chain.Chainable;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class ExprDotStaticMethodWrapFactory {

    public static ExprDotStaticMethodWrap make(Method method, List<Chainable> chain, String optionalEventTypeName, ExprValidationContext validationContext)
            throws ExprValidationException {

        if (chain.isEmpty() || (!EnumMethodResolver.isEnumerationMethod(chain.get(0).getRootNameOrEmptyString(), validationContext.getClasspathImportService()))) {
            return null;
        }

        EPTypeClass methodReturnType = ClassHelperGenericType.getMethodReturnEPType(method);

        if (methodReturnType.getType().isArray() && methodReturnType.getType().getComponentType() == EventBean.class) {
            EventType eventType = requireEventType(method, optionalEventTypeName, validationContext);
            return new ExprDotStaticMethodWrapEventBeanArr(eventType);
        }

        if (methodReturnType.getType().isArray()) {
            EPTypeClass componentType = JavaClassHelper.getArrayComponentType(methodReturnType);
            if (JavaClassHelper.isJavaBuiltinDataType(componentType)) {
                EPTypeClass returnType = ClassHelperGenericType.getMethodReturnEPType(method);
                return new ExprDotStaticMethodWrapArrayScalar(method.getName(), returnType);
            }

            BeanEventType type = makeBeanType(method.getName(), componentType, validationContext);
            return new ExprDotStaticMethodWrapArrayEvents(null, type);
        }

        if (JavaClassHelper.isImplementsInterface(methodReturnType.getType(), Collection.class)) {
            EPTypeClass genericType = JavaClassHelper.getSingleParameterTypeOrObject(methodReturnType);

            if (genericType.getType() == EventBean.class) {
                EventType eventType = requireEventType(method, optionalEventTypeName, validationContext);
                return new ExprDotStaticMethodWrapEventBeanColl(eventType);
            }

            if (JavaClassHelper.isJavaBuiltinDataType(genericType)) {
                return new ExprDotStaticMethodWrapCollection(method.getName(), genericType);
            }
        }

        if (JavaClassHelper.isImplementsInterface(methodReturnType.getType(), Iterable.class)) {
            EPTypeClass genericType = JavaClassHelper.getSingleParameterTypeOrObject(methodReturnType);

            if (JavaClassHelper.isJavaBuiltinDataType(genericType)) {
                return new ExprDotStaticMethodWrapIterableScalar(method.getName(), genericType);
            }

            BeanEventType type = makeBeanType(method.getName(), genericType, validationContext);
            return new ExprDotStaticMethodWrapIterableEvents(validationContext.getEventBeanTypedEventFactory(), type);
        }
        return null;
    }

    private static BeanEventType makeBeanType(String methodName, EPTypeClass clazz, ExprValidationContext validationContext) {
        String eventTypeName = validationContext.getStatementCompileTimeService().getEventTypeNameGeneratorStatement().getAnonymousTypeNameUDFMethod(methodName, clazz.getTypeName());
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



