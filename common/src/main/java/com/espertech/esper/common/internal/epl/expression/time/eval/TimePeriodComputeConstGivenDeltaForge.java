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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class TimePeriodComputeConstGivenDeltaForge implements TimePeriodComputeForge {
    private final long timeDelta;

    public TimePeriodComputeConstGivenDeltaForge(long timeDelta) {
        this.timeDelta = timeDelta;
    }

    public TimePeriodCompute getEvaluator() {
        return new TimePeriodComputeConstGivenDeltaEval(timeDelta);
    }

    public CodegenExpression makeEvaluator(CodegenMethodScope parent, CodegenClassScope classScope) {
        return newInstance(TimePeriodComputeConstGivenDeltaEval.class, constant(timeDelta));
    }
}
