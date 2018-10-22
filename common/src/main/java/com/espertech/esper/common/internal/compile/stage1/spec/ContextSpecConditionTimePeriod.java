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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorTimePeriod;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecConditionTimePeriod implements ContextSpecCondition, ScheduleHandleCallbackProvider {
    private ExprTimePeriod timePeriod;
    private boolean immediate;
    private int scheduleCallbackId = -1;

    public ContextSpecConditionTimePeriod(ExprTimePeriod timePeriod, boolean immediate) {
        this.timePeriod = timePeriod;
        this.immediate = immediate;
    }

    public ExprTimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(ExprTimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned schedule callback id");
        }

        CodegenMethod method = parent.makeChild(ContextConditionDescriptorTimePeriod.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ContextConditionDescriptorTimePeriod.class, "condition", newInstance(ContextConditionDescriptorTimePeriod.class))
                .declareVar(TimePeriodCompute.class, "eval", timePeriod.getTimePeriodComputeForge().makeEvaluator(method, classScope))
                .exprDotMethod(ref("condition"), "setTimePeriodCompute", ref("eval"))
                .exprDotMethod(ref("condition"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref("condition"), "setImmediate", constant(immediate))
                .methodReturn(ref("condition"));
        return localMethod(method);
    }
}
