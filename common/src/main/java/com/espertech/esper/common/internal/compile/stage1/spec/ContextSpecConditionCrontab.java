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
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorCrontab;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextSpecConditionCrontab implements ContextSpecCondition, ScheduleHandleCallbackProvider {
    private final List<List<ExprNode>> crontabs;
    private final boolean immediate;
    private ExprForge[][] forgesPerCrontab;
    private int scheduleCallbackId = -1;

    public ContextSpecConditionCrontab(List<List<ExprNode>> crontabs, boolean immediate) {
        this.crontabs = crontabs;
        this.immediate = immediate;
    }

    public List<List<ExprNode>> getCrontabs() {
        return crontabs;
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

    public void setForgesPerCrontab(ExprForge[][] forgesPerCrontab) {
        this.forgesPerCrontab = forgesPerCrontab;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned schedule callback id");
        }

        CodegenMethod method = parent.makeChild(ContextConditionDescriptorCrontab.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ContextConditionDescriptorCrontab.class, "condition", newInstance(ContextConditionDescriptorCrontab.class))
                .exprDotMethod(ref("condition"), "setEvaluatorsPerCrontab", ExprNodeUtilityCodegen.codegenEvaluators(forgesPerCrontab, method, this.getClass(), classScope))
                .exprDotMethod(ref("condition"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref("condition"), "setImmediate", constant(immediate))
                .methodReturn(ref("condition"));
        return localMethod(method);
    }
}
