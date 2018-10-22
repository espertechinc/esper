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

/**
 * The '*' state in the regex NFA states.
 */
public class RowRecogNFAStateZeroToManyForge extends RowRecogNFAStateForgeBase {
    private ExprNode expression;

    public RowRecogNFAStateZeroToManyForge(String nodeNum, String variableName, int streamNum, boolean multiple, boolean isGreedy, boolean exprRequiresMultimatchState, ExprNode expression) {
        super(nodeNum, variableName, streamNum, multiple, isGreedy, exprRequiresMultimatchState);
        this.expression = expression;
        this.addState(this);
    }

    public String toString() {
        if (expression == null) {
            return "ZeroMany-Unfiltered";
        }
        return "ZeroMany-Filtered";
    }

    protected Class getEvalClass() {
        return expression == null ? RowRecogNFAStateZeroToManyEvalNoCond.class : RowRecogNFAStateZeroToManyEvalCond.class;
    }

    protected void assignInline(CodegenExpression eval, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (expression != null) {
            method.getBlock().exprDotMethod(eval, "setExpression", ExprNodeUtilityCodegen.codegenEvaluator(expression.getForge(), method, this.getClass(), classScope));
        }
    }
}
