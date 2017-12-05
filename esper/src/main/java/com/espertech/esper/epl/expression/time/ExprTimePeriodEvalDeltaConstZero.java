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

public class ExprTimePeriodEvalDeltaConstZero implements ExprTimePeriodEvalDeltaConst {
    public final static ExprTimePeriodEvalDeltaConstZero INSTANCE = new ExprTimePeriodEvalDeltaConstZero();

    private ExprTimePeriodEvalDeltaConstZero() {
    }

    public ExprTimePeriodEvalDeltaConst make(String validateMsgName, String validateMsgValue, ExprEvaluatorContext exprEvaluatorContext, TimeAbacus timeAbacus) {
        return this;
    }

    public boolean equalsTimePeriod(ExprTimePeriodEvalDeltaConst otherComputation) {
        return otherComputation instanceof ExprTimePeriodEvalDeltaConstZero;
    }

    public long deltaAdd(long fromTime) {
        return 0;
    }

    public CodegenExpression deltaAddCodegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw new UnsupportedOperationException();
    }

    public long deltaSubtract(long fromTime) {
        return 0;
    }

    public ExprTimePeriodEvalDeltaResult deltaAddWReference(long fromTime, long reference) {
        return new ExprTimePeriodEvalDeltaResult(ExprTimePeriodEvalDeltaConstGivenDelta.deltaAddWReference(fromTime, reference, 0), reference);
    }
}
