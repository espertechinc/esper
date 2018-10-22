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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TimerScheduleSpecComputeFromExprForge implements TimerScheduleSpecComputeForge {
    private final ExprNode dateNode;
    private final ExprNode repetitionsNode;
    private final ExprTimePeriod periodNode;

    public TimerScheduleSpecComputeFromExprForge(ExprNode dateNode, ExprNode repetitionsNode, ExprTimePeriod periodNode) {
        this.dateNode = dateNode;
        this.repetitionsNode = repetitionsNode;
        this.periodNode = periodNode;
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(TimerScheduleSpecComputeFromExpr.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(TimerScheduleSpecComputeFromExpr.class, "compute", newInstance(TimerScheduleSpecComputeFromExpr.class))
                .exprDotMethod(ref("compute"), "setDate", dateNode == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(dateNode.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(ref("compute"), "setRepetitions", repetitionsNode == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(repetitionsNode.getForge(), method, this.getClass(), classScope));
        if (periodNode != null) {
            method.getBlock().exprDotMethod(ref("compute"), "setTimePeriod", periodNode.makeTimePeriodAnonymous(method, classScope));
        }
        method.getBlock().methodReturn(ref("compute"));
        return localMethod(method);
    }

    public void verifyComputeAllConst(ExprValidationContext validationContext) throws ScheduleParameterException {
        TimerScheduleSpecComputeFromExpr.compute(
                dateNode == null ? null : dateNode.getForge().getExprEvaluator(),
                repetitionsNode == null ? null : repetitionsNode.getForge().getExprEvaluator(),
                periodNode == null ? null : periodNode.getTimePeriodEval(),
                null, null, null, validationContext.getClasspathImportService().getTimeAbacus());
    }
}
