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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.epl.pattern.core.PatternDeltaComputeUtil;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TimerWithinOrMaxCountGuardForge implements GuardForge, ScheduleHandleCallbackProvider {

    private ExprNode timeExpr;
    private ExprNode numCountToExpr;
    private int scheduleCallbackId = -1;
    private TimeAbacus timeAbacus;

    /**
     * For converting matched-events maps to events-per-stream.
     */
    protected transient MatchedEventConvertorForge convertor;

    public void setGuardParameters(List<ExprNode> parameters, MatchedEventConvertorForge convertor, StatementCompileTimeServices services) throws GuardParameterException {
        String message = "Timer-within-or-max-count guard requires two parameters: "
                + "numeric or time period parameter and an integer-value expression parameter";

        if (parameters.size() != 2) {
            throw new GuardParameterException(message);
        }

        if (!JavaClassHelper.isNumeric(parameters.get(0).getForge().getEvaluationType())) {
            throw new GuardParameterException(message);
        }

        Class paramOneType = parameters.get(1).getForge().getEvaluationType();
        if (paramOneType != Integer.class && paramOneType != int.class) {
            throw new GuardParameterException(message);
        }

        this.timeExpr = parameters.get(0);
        this.numCountToExpr = parameters.get(1);
        this.convertor = convertor;
        this.timeAbacus = services.getClasspathImportServiceCompileTime().getTimeAbacus();
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    public void collectSchedule(List<ScheduleHandleCallbackProvider> schedules) {
        schedules.add(this);
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned schedule callback id");
        }

        CodegenMethod method = parent.makeChild(TimerWithinOrMaxCountGuardFactory.class, this.getClass(), classScope);
        CodegenExpression patternDelta = PatternDeltaComputeUtil.makePatternDeltaAnonymous(timeExpr, convertor, timeAbacus, method, classScope);

        CodegenExpression convertorExpr;
        if (numCountToExpr.getForge().getForgeConstantType().isCompileTimeConstant()) {
            convertorExpr = constantNull();
        } else {
            convertorExpr = ExprNodeUtilityCodegen.codegenEvaluator(numCountToExpr.getForge(), method, this.getClass(), classScope);
        }

        method.getBlock()
                .declareVar(TimerWithinOrMaxCountGuardFactory.class, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETPATTERNFACTORYSERVICE).add("guardTimerWithinOrMax"))
                .exprDotMethod(ref("factory"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref("factory"), "setDeltaCompute", patternDelta)
                .exprDotMethod(ref("factory"), "setOptionalConvertor", convertorExpr)
                .exprDotMethod(ref("factory"), "setCountEval", ExprNodeUtilityCodegen.codegenEvaluator(numCountToExpr.getForge(), method, this.getClass(), classScope))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }
}
