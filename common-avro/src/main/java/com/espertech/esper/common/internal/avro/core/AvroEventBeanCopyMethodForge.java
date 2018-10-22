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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Copy method for Map-underlying events.
 */
public class AvroEventBeanCopyMethodForge implements EventBeanCopyMethodForge {
    private final AvroEventType avroEventType;

    public AvroEventBeanCopyMethodForge(AvroEventType avroEventType) {
        this.avroEventType = avroEventType;
    }

    public CodegenExpression makeCopyMethodClassScoped(CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        return newInstance(AvroEventBeanCopyMethod.class,
                cast(AvroEventType.class, EventTypeUtility.resolveTypeCodegen(avroEventType, EPStatementInitServices.REF)),
                factory);
    }

    public EventBeanCopyMethod getCopyMethod(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new AvroEventBeanCopyMethod(avroEventType, eventBeanTypedEventFactory);
    }
}
