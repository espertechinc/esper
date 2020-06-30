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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.type.RelationalOpEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubselectForgeNRRelOpAllAnyAggregated extends SubselectForgeNRRelOpBase {
    private final ExprForge havingEval;

    public SubselectForgeNRRelOpAllAnyAggregated(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, RelationalOpEnum.Computer computer, ExprForge havingEval) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, computer);
        this.havingEval = havingEval;
    }

    protected CodegenExpression codegenEvaluateInternal(CodegenMethodScope parent, SubselectForgeNRSymbol symbols, CodegenClassScope classScope) {
        if (subselect.getEvaluationType() == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        CodegenMethod method = parent.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), this.getClass(), classScope);
        CodegenExpression eps = symbols.getAddEPS(method);
        CodegenExpression evalCtx = symbols.getAddExprEvalCtx(method);
        CodegenExpressionRef left = symbols.getAddLeftResult(method);

        if (havingEval != null) {
            CodegenExpression having = localMethod(CodegenLegoMethodExpression.codegenExpression(havingEval, method, classScope), eps, constantTrue(), evalCtx);
            CodegenLegoBooleanExpression.codegenReturnValueIfNullOrNotPass(method.getBlock(), havingEval.getEvaluationType(), having, constantNull());
        }

        CodegenExpression rhsSide = localMethod(CodegenLegoMethodExpression.codegenExpression(selectEval, method, classScope), eps, constantTrue(), evalCtx);
        EPTypeClass rhsType = JavaClassHelper.getBoxedType((EPTypeClass) selectEval.getEvaluationType());
        method.getBlock()
                .declareVar(rhsType, "rhs", rhsSide)
                .ifRefNullReturnNull("rhs")
                .methodReturn(computer.codegen(left, symbols.getLeftResultType(), ref("rhs"), rhsType));

        return localMethod(method);
    }
}
