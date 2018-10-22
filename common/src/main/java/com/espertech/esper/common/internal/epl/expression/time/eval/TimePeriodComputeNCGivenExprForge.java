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
package com.espertech.esper.common.internal.epl.expression.time.eval;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacusField;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TimePeriodComputeNCGivenExprForge implements TimePeriodComputeForge {
    private final ExprForge secondsEvaluator;
    private final TimeAbacus timeAbacus;

    public TimePeriodComputeNCGivenExprForge(ExprForge secondsEvaluator, TimeAbacus timeAbacus) {
        this.secondsEvaluator = secondsEvaluator;
        this.timeAbacus = timeAbacus;
    }

    public TimePeriodCompute getEvaluator() {
        return new TimePeriodComputeNCGivenExprEval(secondsEvaluator.getExprEvaluator(), timeAbacus);
    }

    public CodegenExpression makeEvaluator(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(TimePeriodComputeNCGivenExprEval.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(TimePeriodComputeNCGivenExprEval.class, "eval", newInstance(TimePeriodComputeNCGivenExprEval.class))
                .exprDotMethod(ref("eval"), "setSecondsEvaluator", ExprNodeUtilityCodegen.codegenEvaluator(secondsEvaluator, method, this.getClass(), classScope))
                .exprDotMethod(ref("eval"), "setTimeAbacus", classScope.addOrGetFieldSharable(TimeAbacusField.INSTANCE))
                .methodReturn(ref("eval"));
        return localMethod(method);
    }
}
