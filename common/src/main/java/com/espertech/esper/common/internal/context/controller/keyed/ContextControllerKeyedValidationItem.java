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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.function.Supplier;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class ContextControllerKeyedValidationItem implements Supplier<EventType> {
    private final EventType eventType;
    private final String[] propertyNames;

    public ContextControllerKeyedValidationItem(EventType eventType, String[] propertyNames) {
        this.eventType = eventType;
        this.propertyNames = propertyNames;
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventType get() {
        return eventType;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public CodegenExpression make(CodegenExpressionRef addInitSvc) {
        return newInstance(ContextControllerKeyedValidationItem.class, EventTypeUtility.resolveTypeCodegen(eventType, addInitSvc), constant(propertyNames));
    }
}
