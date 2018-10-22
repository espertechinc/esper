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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.REF_EVENTS_SHIFTED;

public class SubselectForgeRowHavingSelected implements SubselectForgeRow {

    private final ExprSubselectRowNode subselect;

    public SubselectForgeRowHavingSelected(ExprSubselectRowNode subselect) {
        this.subselect = subselect;
    }

    public CodegenExpression evaluateCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(subselect.getEvaluationType(), this.getClass(), classScope);
        CodegenMethod havingMethod = CodegenLegoMethodExpression.codegenExpression(subselect.havingExpr, method, classScope);
        CodegenExpression having = localMethod(havingMethod, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method));

        method.getBlock().applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);
        CodegenLegoBooleanExpression.codegenReturnValueIfNotNullAndNotPass(method.getBlock(), Boolean.class, having, constantNull());

        if (subselect.selectClause.length == 1) {
            CodegenMethod eval = CodegenLegoMethodExpression.codegenExpression(subselect.selectClause[0].getForge(), method, classScope);
            method.getBlock().methodReturn(localMethod(eval, REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method)));
        } else {
            method.getBlock().methodReturn(localMethod(subselect.evaluateRowCodegen(method, classScope)));
        }
        return localMethod(method);
    }

    public CodegenExpression evaluateGetCollEventsCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbol, CodegenClassScope classScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetCollScalarCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbol, CodegenClassScope classScope) {
        return constantNull();
    }

    public CodegenExpression evaluateGetBeanCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    public CodegenExpression evaluateTypableSinglerowCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    public CodegenExpression evaluateTypableMultirowCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }
}
