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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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
 * Represents a in-subselect evaluation strategy.
 */
public class SubselectForgeNREqualsIn extends SubselectForgeNREqualsInBase {
    private final ExprForge filterEval;

    public SubselectForgeNREqualsIn(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents, boolean isNotIn, SimpleNumberCoercer coercer, ExprForge filterEval) {
        super(subselect, valueEval, selectEval, resultWhenNoMatchingEvents, isNotIn, coercer);
        this.filterEval = filterEval;
    }

    protected CodegenExpression codegenEvaluateInternal(CodegenMethodScope parent, SubselectForgeNRSymbol symbols, CodegenClassScope classScope) {
        if (subselect.getEvaluationType() == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        CodegenMethod method = parent.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), this.getClass(), classScope);
        CodegenExpressionRef left = symbols.getAddLeftResult(method);
        method.getBlock().declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "hasNullRow", constantFalse());
        CodegenBlock foreach = method.getBlock().forEach(EventBean.EPTYPE, "theEvent", symbols.getAddMatchingEvents(method));
        {
            foreach.assignArrayElement(NAME_EPS, constant(0), ref("theEvent"));
            if (filterEval != null) {
                CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(foreach, filterEval.getEvaluationType(), filterEval.evaluateCodegen(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), method, symbols, classScope));
            }
            foreach.ifNullReturnNull(left);

            EPTypeClass valueRightType;
            if (selectEval != null) {
                valueRightType = JavaClassHelper.getBoxedType((EPTypeClass) selectEval.getEvaluationType());
                foreach.declareVar(valueRightType, "valueRight", selectEval.evaluateCodegen(valueRightType, method, symbols, classScope));
            } else {
                valueRightType = subselect.rawEventType.getUnderlyingEPType();
                foreach.declareVar(valueRightType, "valueRight", cast(valueRightType, exprDotUnderlying(arrayAtIndex(symbols.getAddEPS(method), constant(0)))));
            }

            CodegenBlock ifRight = foreach.ifCondition(notEqualsNull(ref("valueRight")));
            {
                if (coercer == null) {
                    ifRight.ifCondition(exprDotMethod(left, "equals", ref("valueRight"))).blockReturn(constant(!isNotIn));
                } else {
                    ifRight.declareVar(EPTypePremade.NUMBER.getEPType(), "left", coercer.coerceCodegen(left, symbols.getLeftResultType()))
                            .declareVar(EPTypePremade.NUMBER.getEPType(), "right", coercer.coerceCodegen(ref("valueRight"), valueRightType))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "eq", exprDotMethod(ref("left"), "equals", ref("right")))
                            .ifCondition(ref("eq")).blockReturn(constant(!isNotIn));
                }
            }
            ifRight.ifElse().assignRef("hasNullRow", constantTrue());
        }

        method.getBlock()
                .ifCondition(ref("hasNullRow"))
                .blockReturn(constantNull())
                .methodReturn(constant(isNotIn));
        return localMethod(method);
    }
}
