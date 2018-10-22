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
    private final List<ExprNode> crontab;
    private final boolean immediate;
    private ExprForge[] forges;
    private int scheduleCallbackId = -1;

    public ContextSpecConditionCrontab(List<ExprNode> crontab, boolean immediate) {
        this.crontab = crontab;
        this.immediate = immediate;
    }

    public List<ExprNode> getCrontab() {
        return crontab;
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

    public void setForges(ExprForge[] forges) {
        this.forges = forges;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned schedule callback id");
        }

        CodegenMethod method = parent.makeChild(ContextConditionDescriptorCrontab.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ContextConditionDescriptorCrontab.class, "condition", newInstance(ContextConditionDescriptorCrontab.class))
                .exprDotMethod(ref("condition"), "setEvaluators", ExprNodeUtilityCodegen.codegenEvaluators(forges, method, this.getClass(), classScope))
                .exprDotMethod(ref("condition"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref("condition"), "setImmediate", constant(immediate))
                .methodReturn(ref("condition"));
        return localMethod(method);
    }
}
