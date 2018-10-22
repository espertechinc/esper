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
package com.espertech.esper.common.internal.context.aifactory.createexpression;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StatementAgentInstanceFactoryCreateExpressionForge {

    private final EventType statementEventType;
    private final String expressionName;

    public StatementAgentInstanceFactoryCreateExpressionForge(EventType statementEventType, String expressionName) {
        this.statementEventType = statementEventType;
        this.expressionName = expressionName;
    }

    public CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(StatementAgentInstanceFactoryCreateExpression.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(StatementAgentInstanceFactoryCreateExpression.class, "saiff", newInstance(StatementAgentInstanceFactoryCreateExpression.class))
                .exprDotMethod(ref("saiff"), "setStatementEventType", EventTypeUtility.resolveTypeCodegen(statementEventType, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("saiff"), "setExpressionName", constant(expressionName))
                .methodReturn(ref("saiff"));
        return method;
    }
}
