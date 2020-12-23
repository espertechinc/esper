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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.util.CallbackAttributionOutputRate;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.schedule.ScheduleHandleTracked;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class OutputConditionTimeForge implements OutputConditionFactoryForge, ScheduleHandleCallbackProvider {
    private final ExprTimePeriod timePeriod;
    private final boolean isStartConditionOnCreation;
    private int scheduleCallbackId = -1;
    protected final StateMgmtSetting stateMgmtSetting;

    public OutputConditionTimeForge(ExprTimePeriod timePeriod, boolean isStartConditionOnCreation, StateMgmtSetting stateMgmtSetting) {
        this.timePeriod = timePeriod;
        this.isStartConditionOnCreation = isStartConditionOnCreation;
        this.stateMgmtSetting = stateMgmtSetting;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned callback id");
        }
        CodegenMethod method = parent.makeChild(OutputConditionFactory.EPTYPE, this.getClass(), classScope);
        method.getBlock()
                .declareVar(TimePeriodCompute.EPTYPE, "delta", timePeriod.getTimePeriodComputeForge().makeEvaluator(method, classScope))
                .methodReturn(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETRESULTSETPROCESSORHELPERFACTORY)
                        .add("makeOutputConditionTime", constant(timePeriod.hasVariable()), ref("delta"), constant(isStartConditionOnCreation), constant(scheduleCallbackId), stateMgmtSetting.toExpression()));
        return localMethod(method);
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public void collectSchedules(CallbackAttributionOutputRate callbackAttribution, List<ScheduleHandleTracked> scheduleHandleCallbackProviders) {
        scheduleHandleCallbackProviders.add(new ScheduleHandleTracked(callbackAttribution, this));
    }
}
