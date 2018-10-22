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
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.epl.pattern.core.PatternDeltaComputeUtil;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Forge for {@link TimerWithinGuard} instances.
 */
public class TimerWithinGuardForge implements GuardForge, ScheduleHandleCallbackProvider {

    private ExprNode timeExpr;
    private MatchedEventConvertorForge convertor;
    private TimeAbacus timeAbacus;
    private int scheduleCallbackId = -1;

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    public void setGuardParameters(List<ExprNode> parameters, MatchedEventConvertorForge convertor, StatementCompileTimeServices services) throws GuardParameterException {
        String errorMessage = "Timer-within guard requires a single numeric or time period parameter";
        if (parameters.size() != 1) {
            throw new GuardParameterException(errorMessage);
        }

        if (!JavaClassHelper.isNumeric(parameters.get(0).getForge().getEvaluationType())) {
            throw new GuardParameterException(errorMessage);
        }

        this.convertor = convertor;
        this.timeExpr = parameters.get(0);
        this.timeAbacus = services.getClasspathImportServiceCompileTime().getTimeAbacus();
    }

    public void collectSchedule(List<ScheduleHandleCallbackProvider> schedules) {
        schedules.add(this);
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned schedule callback id");
        }

        CodegenMethod method = parent.makeChild(TimerWithinGuardFactory.class, this.getClass(), classScope);
        CodegenExpression patternDelta = PatternDeltaComputeUtil.makePatternDeltaAnonymous(timeExpr, convertor, timeAbacus, method, classScope);

        method.getBlock()
                .declareVar(TimerWithinGuardFactory.class, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETPATTERNFACTORYSERVICE).add("guardTimerWithin"))
                .exprDotMethod(ref("factory"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref("factory"), "setDeltaCompute", patternDelta)
                .methodReturn(ref("factory"));
        return localMethod(method);
    }
}
