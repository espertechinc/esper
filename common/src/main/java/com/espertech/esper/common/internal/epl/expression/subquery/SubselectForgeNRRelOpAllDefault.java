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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.type.RelationalOpEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.NAME_EPS;

public class SubselectForgeNRRelOpAllDefault extends SubselectForgeNRRelOpBase {
    private final ExprForge filterOrHavingEval;

    public SubselectForgeNRRelOpAllDefault(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, RelationalOpEnum.Computer computer, ExprForge filterOrHavingEval) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, computer);
        this.filterOrHavingEval = filterOrHavingEval;
    }

    protected CodegenExpression codegenEvaluateInternal(CodegenMethodScope parent, SubselectForgeNRSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Boolean.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(boolean.class, "hasRows", constantFalse())
                .declareVar(boolean.class, "hasNullRow", constantFalse());
        CodegenBlock foreach = method.getBlock().forEach(EventBean.class, "subselectEvent", symbols.getAddMatchingEvents(method));
        {
            foreach.assignArrayElement(NAME_EPS, constant(0), ref("subselectEvent"));
            if (filterOrHavingEval != null) {
                CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(foreach, filterOrHavingEval.getEvaluationType(), filterOrHavingEval.evaluateCodegen(Boolean.class, method, symbols, classScope));
            }
            foreach.assignRef("hasRows", constantTrue());

            Class valueRightType;
            if (selectEval != null) {
                valueRightType = JavaClassHelper.getBoxedType(selectEval.getEvaluationType());
                foreach.declareVar(valueRightType, "valueRight", selectEval.evaluateCodegen(valueRightType, method, symbols, classScope));
            } else {
                valueRightType = Object.class;
                foreach.declareVar(valueRightType, "valueRight", exprDotUnderlying(arrayAtIndex(symbols.getAddEPS(method), constant(0))));
            }

            foreach.ifRefNull("valueRight")
                    .assignRef("hasNullRow", constantTrue())
                    .ifElse()
                    .ifCondition(notEqualsNull(symbols.getAddLeftResult(method)))
                    .ifCondition(not(computer.codegen(symbols.getAddLeftResult(method), symbols.getLeftResultType(), ref("valueRight"), valueRightType)))
                    .blockReturn(constantFalse());
        }

        method.getBlock()
                .ifCondition(not(ref("hasRows")))
                .blockReturn(constantTrue())
                .ifCondition(equalsNull(symbols.getAddLeftResult(method)))
                .blockReturn(constantNull())
                .ifCondition(ref("hasNullRow"))
                .blockReturn(constantNull())
                .methodReturn(constantTrue());
        return localMethod(method);
    }
}
