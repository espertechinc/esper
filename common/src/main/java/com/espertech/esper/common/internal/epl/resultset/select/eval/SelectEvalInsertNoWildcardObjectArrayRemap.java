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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;

public class SelectEvalInsertNoWildcardObjectArrayRemap implements SelectExprProcessorForge {

    protected final SelectExprForgeContext context;
    protected final EventType resultEventType;
    protected final int[] remapped;

    public SelectEvalInsertNoWildcardObjectArrayRemap(SelectExprForgeContext context, EventType resultEventType, int[] remapped) {
        this.context = context;
        this.resultEventType = resultEventType;
        this.remapped = remapped;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventTypeExpr, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return SelectEvalInsertNoWildcardObjectArrayRemapWWiden.processCodegen(resultEventTypeExpr, eventBeanFactory, codegenMethodScope, exprSymbol, codegenClassScope, context.getExprForges(), resultEventType.getPropertyNames(), remapped, null);
    }
}
