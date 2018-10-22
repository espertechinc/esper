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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Copy method for underlying events.
 */
public class WrapperEventBeanUndCopyMethodForge implements EventBeanCopyMethodForge {
    private final WrapperEventType wrapperEventType;
    private final EventBeanCopyMethodForge underlyingCopyMethod;

    /**
     * Ctor.
     *
     * @param wrapperEventType     wrapper type
     * @param underlyingCopyMethod for copying the underlying event
     */
    public WrapperEventBeanUndCopyMethodForge(WrapperEventType wrapperEventType, EventBeanCopyMethodForge underlyingCopyMethod) {
        this.wrapperEventType = wrapperEventType;
        this.underlyingCopyMethod = underlyingCopyMethod;
    }

    public CodegenExpression makeCopyMethodClassScoped(CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        return newInstance(WrapperEventBeanUndCopyMethod.class,
                cast(WrapperEventType.class, EventTypeUtility.resolveTypeCodegen(wrapperEventType, EPStatementInitServices.REF)),
                factory,
                underlyingCopyMethod.makeCopyMethodClassScoped(classScope));
    }

    public EventBeanCopyMethod getCopyMethod(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new WrapperEventBeanUndCopyMethod(wrapperEventType, eventBeanTypedEventFactory, underlyingCopyMethod.getCopyMethod(eventBeanTypedEventFactory));
    }
}
