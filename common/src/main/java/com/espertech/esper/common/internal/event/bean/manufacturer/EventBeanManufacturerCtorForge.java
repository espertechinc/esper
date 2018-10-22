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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.*;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EventBeanManufacturerCtorForge implements EventBeanManufacturerForge {

    private final Constructor constructor;
    private final BeanEventType beanEventType;

    public EventBeanManufacturerCtorForge(Constructor constructor, BeanEventType beanEventType) {
        this.constructor = constructor;
        this.beanEventType = beanEventType;
    }

    public CodegenExpression make(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField beanType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(beanEventType, EPStatementInitServices.REF));
        CodegenExpression ctor = staticMethod(EventBeanManufacturerCtorForge.class, "resolveConstructor", constant(constructor.getParameterTypes()), constant(constructor.getDeclaringClass()));
        return newInstance(EventBeanManufacturerCtor.class, ctor, beanType, factory);
    }

    public EventBeanManufacturer getManufacturer(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new EventBeanManufacturerCtor(constructor, beanEventType, eventBeanTypedEventFactory);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param classes   classes
     * @param declaring declaring
     * @return ctor
     */
    public static Constructor resolveConstructor(Class[] classes, Class declaring) {
        try {
            return declaring.getConstructor(classes);
        } catch (Throwable t) {
            throw new EPException("Failed to resolve constructor for class " + declaring.getClass() + " params " + Arrays.toString(classes));
        }
    }
}
