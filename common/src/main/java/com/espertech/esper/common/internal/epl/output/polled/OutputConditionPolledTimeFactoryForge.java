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
package com.espertech.esper.common.internal.epl.output.polled;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public final class OutputConditionPolledTimeFactoryForge implements OutputConditionPolledFactoryForge {
    protected final ExprTimePeriod timePeriod;

    public OutputConditionPolledTimeFactoryForge(ExprTimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(OutputConditionPolledFactory.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(TimePeriodCompute.class, "delta", timePeriod.getTimePeriodComputeForge().makeEvaluator(method, classScope))
                .methodReturn(newInstance(OutputConditionPolledTimeFactory.class, ref("delta")));
        return localMethod(method);
    }
}