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
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.epl.pattern.core.PatternDeltaComputeUtil;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for making observer instances.
 */
public class TimerIntervalObserverForge implements ObserverForge, ScheduleHandleCallbackProvider {
    private final static String NAME = "Timer-interval observer";

    protected ExprNode parameter;
    protected int scheduleCallbackId = -1;
    protected TimeAbacus timeAbacus;
    protected MatchedEventConvertorForge convertor;

    public void setObserverParameters(List<ExprNode> parameters, MatchedEventConvertorForge convertor, ExprValidationContext validationContext) throws ObserverParameterException {
        ObserverParameterUtil.validateNoNamedParameters(NAME, parameters);
        String errorMessage = NAME + " requires a single numeric or time period parameter";
        if (parameters.size() != 1) {
            throw new ObserverParameterException(errorMessage);
        }
        if (!(parameters.get(0) instanceof ExprTimePeriod)) {
            Class returnType = parameters.get(0).getForge().getEvaluationType();
            if (!(JavaClassHelper.isNumeric(returnType))) {
                throw new ObserverParameterException(errorMessage);
            }
        }

        parameter = parameters.get(0);
        this.convertor = convertor;
        this.timeAbacus = validationContext.getClasspathImportService().getTimeAbacus();
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

        CodegenMethod method = parent.makeChild(TimerIntervalObserverFactory.class, TimerIntervalObserverForge.class, classScope);
        CodegenExpression patternDelta = PatternDeltaComputeUtil.makePatternDeltaAnonymous(parameter, convertor, timeAbacus, method, classScope);

        method.getBlock()
                .declareVar(TimerIntervalObserverFactory.class, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETPATTERNFACTORYSERVICE).add("observerTimerInterval"))
                .exprDotMethod(ref("factory"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref("factory"), "setDeltaCompute", patternDelta)
                .methodReturn(ref("factory"));
        return localMethod(method);
    }
}
