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
package com.espertech.esper.common.internal.epl.rowrecog.nfa;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

/**
 * NFA state for a single match that applies a filter.
 */
public class RowRecogNFAStateFilterForge extends RowRecogNFAStateForgeBase {
    private final ExprNode expression;

    /**
     * Ctor.
     *
     * @param nodeNum                     node num
     * @param variableName                variable name
     * @param streamNum                   stream number
     * @param multiple                    true for multiple matches
     * @param expression                  filter expression
     * @param exprRequiresMultimatchState indicator for multi-match state required
     */
    public RowRecogNFAStateFilterForge(String nodeNum, String variableName, int streamNum, boolean multiple, boolean exprRequiresMultimatchState, ExprNode expression) {
        super(nodeNum, variableName, streamNum, multiple, null, exprRequiresMultimatchState);
        this.expression = expression;
    }

    public String toString() {
        return "FilterEvent";
    }

    protected Class getEvalClass() {
        return RowRecogNFAStateFilterEval.class;
    }

    protected void assignInline(CodegenExpression eval, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(eval, "setExpression", ExprNodeUtilityCodegen.codegenEvaluator(expression.getForge(), method, this.getClass(), classScope));
        if (classScope.isInstrumented()) {
            method.getBlock().exprDotMethod(eval, "setExpressionTextForAudit", constant(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expression)));
        }
    }
}
