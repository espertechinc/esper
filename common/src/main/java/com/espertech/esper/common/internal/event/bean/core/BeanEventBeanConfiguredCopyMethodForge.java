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
package com.espertech.esper.common.internal.event.bean.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.util.MethodResolver;

import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Copies an event for modification.
 */
public class BeanEventBeanConfiguredCopyMethodForge implements EventBeanCopyMethodForge {

    private final BeanEventType beanEventType;
    private final Method copyMethod;

    /**
     * Ctor.
     *
     * @param beanEventType type of bean to copy
     * @param copyMethod    method to copy the event
     */
    public BeanEventBeanConfiguredCopyMethodForge(BeanEventType beanEventType, Method copyMethod) {
        this.beanEventType = beanEventType;
        this.copyMethod = copyMethod;
    }

    public CodegenExpression makeCopyMethodClassScoped(CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        return newInstance(BeanEventBeanConfiguredCopyMethod.class,
                cast(BeanEventType.class, EventTypeUtility.resolveTypeCodegen(beanEventType, EPStatementInitServices.REF)),
                factory, MethodResolver.resolveMethodCodegenExactNonStatic(copyMethod));
    }

    public EventBeanCopyMethod getCopyMethod(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new BeanEventBeanConfiguredCopyMethod(beanEventType, eventBeanTypedEventFactory, copyMethod);
    }
}
