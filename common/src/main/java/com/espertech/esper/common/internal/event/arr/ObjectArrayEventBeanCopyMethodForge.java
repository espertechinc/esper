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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Copy method for Object array-underlying events.
 */
public class ObjectArrayEventBeanCopyMethodForge implements EventBeanCopyMethodForge {
    private final ObjectArrayEventType objectArrayEventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    /**
     * Ctor.
     *
     * @param objectArrayEventType       map event type
     * @param eventBeanTypedEventFactory for copying events
     */
    public ObjectArrayEventBeanCopyMethodForge(ObjectArrayEventType objectArrayEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.objectArrayEventType = objectArrayEventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public CodegenExpression makeCopyMethodClassScoped(CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        return newInstance(ObjectArrayEventBeanCopyMethod.class,
                cast(ObjectArrayEventType.class, EventTypeUtility.resolveTypeCodegen(objectArrayEventType, EPStatementInitServices.REF)),
                factory);
    }

    public EventBeanCopyMethod getCopyMethod(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new ObjectArrayEventBeanCopyMethod(objectArrayEventType, eventBeanTypedEventFactory);
    }
}
