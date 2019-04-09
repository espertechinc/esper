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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.StatementResultService;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.context.module.EPStatementInitServices.GETSTATEMENTRESULTSERVICE;

public class ListenerOnlySelectExprProcessorForge implements SelectExprProcessorForge {
    private final SelectExprProcessorForge syntheticProcessorForge;

    public ListenerOnlySelectExprProcessorForge(SelectExprProcessorForge syntheticProcessorForge) {
        this.syntheticProcessorForge = syntheticProcessorForge;
    }

    public EventType getResultEventType() {
        return syntheticProcessorForge.getResultEventType();
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod processMethod = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);

        CodegenExpressionRef isSythesize = selectSymbol.getAddSynthesize(processMethod);
        CodegenMethod syntheticMethod = syntheticProcessorForge.processCodegen(resultEventType, eventBeanFactory, processMethod, selectSymbol, exprSymbol, codegenClassScope);

        CodegenExpressionField stmtResultSvc = codegenClassScope.addFieldUnshared(true, StatementResultService.class, exprDotMethod(EPStatementInitServices.REF, GETSTATEMENTRESULTSERVICE));
        processMethod.getBlock()
            .ifCondition(or(isSythesize, exprDotMethod(stmtResultSvc, "isMakeSynthetic")))
            .blockReturn(localMethod(syntheticMethod))
            .methodReturn(constantNull());

        return processMethod;
    }
}
