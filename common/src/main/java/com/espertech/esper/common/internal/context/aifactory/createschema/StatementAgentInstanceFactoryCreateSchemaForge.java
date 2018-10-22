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
package com.espertech.esper.common.internal.context.aifactory.createschema;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class StatementAgentInstanceFactoryCreateSchemaForge {

    private final EventType eventType;

    public StatementAgentInstanceFactoryCreateSchemaForge(EventType eventType) {
        this.eventType = eventType;
    }

    public CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(StatementAgentInstanceFactoryCreateSchema.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(StatementAgentInstanceFactoryCreateSchema.class, "saiff", newInstance(StatementAgentInstanceFactoryCreateSchema.class))
                .exprDotMethod(ref("saiff"), "setEventType", EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)))
                .methodReturn(ref("saiff"));
        return method;
    }
}
