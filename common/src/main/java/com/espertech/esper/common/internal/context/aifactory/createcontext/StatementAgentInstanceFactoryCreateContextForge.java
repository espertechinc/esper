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
package com.espertech.esper.common.internal.context.aifactory.createcontext;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StatementAgentInstanceFactoryCreateContextForge implements StatementAgentInstanceFactoryForge {

    private final String contextName;
    private final EventType statementEventType;

    public StatementAgentInstanceFactoryCreateContextForge(String contextName, EventType statementEventType) {
        this.contextName = contextName;
        this.statementEventType = statementEventType;
    }

    public CodegenMethod initializeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(StatementAgentInstanceFactoryCreateContext.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(StatementAgentInstanceFactoryCreateContext.class, "saiff", newInstance(StatementAgentInstanceFactoryCreateContext.class))
                .exprDotMethod(ref("saiff"), "setContextName", constant(contextName))
                .exprDotMethod(ref("saiff"), "setStatementEventType", EventTypeUtility.resolveTypeCodegen(statementEventType, symbols.getAddInitSvc(method)))
                .exprDotMethod(symbols.getAddInitSvc(method), "addReadyCallback", ref("saiff"))
                .methodReturn(ref("saiff"));
        return method;
    }
}
