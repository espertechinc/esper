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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.NAME_EPS;

/**
 * Strategy for subselects with "=/!=/&gt;&lt; ALL".
 */
public class SubselectForgeNREqualsDefault extends SubselectForgeNREqualsBase {
    private final ExprForge filterEval;
    private final boolean isAll;

    public SubselectForgeNREqualsDefault(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, boolean isNot, SimpleNumberCoercer coercer, ExprForge filterEval, boolean isAll) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, isNot, coercer);
        this.filterEval = filterEval;
        this.isAll = isAll;
    }

    protected CodegenExpression codegenEvaluateInternal(CodegenMethodScope parent, SubselectForgeNRSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Boolean.class, this.getClass(), classScope);
        CodegenExpressionRef left = symbols.getAddLeftResult(method);
        method.getBlock().declareVar(boolean.class, "hasNullRow", constantFalse());
        CodegenBlock foreach = method.getBlock().forEach(EventBean.class, "theEvent", symbols.getAddMatchingEvents(method));
        {
            foreach.assignArrayElement(NAME_EPS, constant(0), ref("theEvent"));
            if (filterEval != null) {
                CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(foreach, filterEval.getEvaluationType(), filterEval.evaluateCodegen(Boolean.class, method, symbols, classScope));
            }
            foreach.ifNullReturnNull(left);

            Class valueRightType;
            if (selectEval != null) {
                valueRightType = JavaClassHelper.getBoxedType(selectEval.getEvaluationType());
                foreach.declareVar(valueRightType, "valueRight", selectEval.evaluateCodegen(valueRightType, method, symbols, classScope));
            } else {
                valueRightType = Object.class;
                foreach.declareVar(valueRightType, "valueRight", exprDotUnderlying(arrayAtIndex(symbols.getAddEPS(method), constant(0))));
            }

            CodegenBlock ifRight = foreach.ifCondition(notEqualsNull(ref("valueRight")));
            {
                if (coercer == null) {
                    ifRight.declareVar(boolean.class, "eq", exprDotMethod(left, "equals", ref("valueRight")));
                    if (isAll) {
                        ifRight.ifCondition(notOptional(!isNot, ref("eq"))).blockReturn(constantFalse());
                    } else {
                        ifRight.ifCondition(notOptional(isNot, ref("eq"))).blockReturn(constantTrue());
                    }
                } else {
                    ifRight.declareVar(Number.class, "left", coercer.coerceCodegen(left, symbols.getLeftResultType()))
                            .declareVar(Number.class, "right", coercer.coerceCodegen(ref("valueRight"), valueRightType))
                            .declareVar(boolean.class, "eq", exprDotMethod(ref("left"), "equals", ref("right")));
                    if (isAll) {
                        ifRight.ifCondition(notOptional(!isNot, ref("eq"))).blockReturn(constantFalse());
                    } else {
                        ifRight.ifCondition(notOptional(isNot, ref("eq"))).blockReturn(constantTrue());
                    }
                }
            }
            ifRight.ifElse().assignRef("hasNullRow", constantTrue());
        }

        method.getBlock()
                .ifCondition(ref("hasNullRow"))
                .blockReturn(constantNull())
                .methodReturn(isAll ? constantTrue() : constantFalse());
        return localMethod(method);
    }
}
