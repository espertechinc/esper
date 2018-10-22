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
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacusField;
import com.espertech.esper.common.internal.epl.expression.time.adder.TimePeriodAdderUtil;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriodForge;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriodUtil;
import com.espertech.esper.common.internal.settings.RuntimeSettingsTimeZoneField;

import java.util.TimeZone;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TimePeriodComputeNCGivenTPCalForge implements TimePeriodComputeForge {
    private final ExprTimePeriodForge timePeriodForge;
    private final int indexMicroseconds;

    public TimePeriodComputeNCGivenTPCalForge(ExprTimePeriodForge timePeriodForge) {
        this.timePeriodForge = timePeriodForge;
        this.indexMicroseconds = ExprTimePeriodUtil.findIndexMicroseconds(timePeriodForge.getAdders());
    }

    public TimePeriodCompute getEvaluator() {
        return new TimePeriodComputeNCGivenTPCalForgeEval(timePeriodForge.getEvaluators(), timePeriodForge.getAdders(), timePeriodForge.getTimeAbacus(), TimeZone.getDefault(), indexMicroseconds);
    }

    public CodegenExpression makeEvaluator(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(TimePeriodComputeNCGivenTPCalForgeEval.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(TimePeriodComputeNCGivenTPCalForgeEval.class, "eval", newInstance(TimePeriodComputeNCGivenTPCalForgeEval.class))
                .exprDotMethod(ref("eval"), "setAdders", TimePeriodAdderUtil.makeArray(timePeriodForge.getAdders(), parent, classScope))
                .exprDotMethod(ref("eval"), "setEvaluators", ExprNodeUtilityCodegen.codegenEvaluators(timePeriodForge.getForges(), method, this.getClass(), classScope))
                .exprDotMethod(ref("eval"), "setTimeAbacus", classScope.addOrGetFieldSharable(TimeAbacusField.INSTANCE))
                .exprDotMethod(ref("eval"), "setTimeZone", classScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE))
                .exprDotMethod(ref("eval"), "setIndexMicroseconds", constant(indexMicroseconds))
                .methodReturn(ref("eval"));
        return localMethod(method);
    }
}
