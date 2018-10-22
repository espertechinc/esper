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
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.REF_EVENTS_SHIFTED;

public class SubselectForgeNRExistsAggregated implements SubselectForgeNR {
    private final ExprForge havingEval;

    public SubselectForgeNRExistsAggregated(ExprForge havingEval) {
        this.havingEval = havingEval;
    }

    public CodegenExpression evaluateMatchesCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(boolean.class, this.getClass(), classScope);
        CodegenMethod havingMethod = CodegenLegoMethodExpression.codegenExpression(havingEval, method, classScope);
        CodegenExpression having = localMethod(havingMethod, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method));

        method.getBlock().applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);
        CodegenLegoBooleanExpression.codegenReturnValueIfNullOrNotPass(method.getBlock(), Boolean.class, having, constantFalse());
        method.getBlock().methodReturn(constantTrue());
        return localMethod(method);
    }
}
