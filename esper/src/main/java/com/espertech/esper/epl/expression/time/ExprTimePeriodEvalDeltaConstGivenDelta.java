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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;

public class ExprTimePeriodEvalDeltaConstGivenDelta implements ExprTimePeriodEvalDeltaConst, ExprTimePeriodEvalDeltaConstFactory {
    private final long timeDelta;

    public ExprTimePeriodEvalDeltaConstGivenDelta(long timeDelta) {
        this.timeDelta = timeDelta;
    }

    public ExprTimePeriodEvalDeltaConst make(String validateMsgName, String validateMsgValue, ExprEvaluatorContext exprEvaluatorContext, TimeAbacus timeAbacus) {
        return this;
    }

    public boolean equalsTimePeriod(ExprTimePeriodEvalDeltaConst otherComputation) {
        if (otherComputation instanceof ExprTimePeriodEvalDeltaConstGivenDelta) {
            ExprTimePeriodEvalDeltaConstGivenDelta other = (ExprTimePeriodEvalDeltaConstGivenDelta) otherComputation;
            return other.timeDelta == timeDelta;
        }
        return false;
    }

    public long deltaAdd(long fromTime) {
        return timeDelta;
    }

    public CodegenExpression deltaAddCodegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constant(timeDelta);
    }

    public long deltaSubtract(long fromTime) {
        return timeDelta;
    }

    public ExprTimePeriodEvalDeltaResult deltaAddWReference(long fromTime, long reference) {
        return new ExprTimePeriodEvalDeltaResult(deltaAddWReference(fromTime, reference, timeDelta), reference);
    }

    protected static long deltaAddWReference(long current, long reference, long msec) {
        // Example:  current c=2300, reference r=1000, interval i=500, solution s=200
        //
        // int n = ((2300 - 1000) / 500) = 2
        // r + (n + 1) * i - c = 200
        //
        // Negative example:  current c=2300, reference r=4200, interval i=500, solution s=400
        // int n = ((2300 - 4200) / 500) = -3
        // r + (n + 1) * i - c = 4200 - 3*500 - 2300 = 400
        //
        long n = (current - reference) / msec;
        if (reference > current) { // References in the future need to deduct one window
            n--;
        }
        long solution = reference + (n + 1) * msec - current;
        if (solution == 0) {
            return msec;
        }
        return solution;
    }

}
