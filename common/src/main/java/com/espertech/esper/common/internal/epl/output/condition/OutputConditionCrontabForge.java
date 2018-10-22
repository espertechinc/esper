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


import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.schedule.ScheduleExpressionUtil;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Output condition handling crontab-at schedule output.
 */
public class OutputConditionCrontabForge implements OutputConditionFactoryForge, ScheduleHandleCallbackProvider {
    protected final ExprForge[] scheduleSpecEvaluators;
    protected final boolean isStartConditionOnCreation;
    private int scheduleCallbackId = -1;

    public OutputConditionCrontabForge(List<ExprNode> scheduleSpecExpressionList,
                                       boolean isStartConditionOnCreation,
                                       StatementRawInfo statementRawInfo,
                                       StatementCompileTimeServices services) throws ExprValidationException {
        this.scheduleSpecEvaluators = ScheduleExpressionUtil.crontabScheduleValidate(ExprNodeOrigin.OUTPUTLIMIT, scheduleSpecExpressionList, false, statementRawInfo, services);
        this.isStartConditionOnCreation = isStartConditionOnCreation;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Unassigned schedule");
        }
        CodegenMethod method = parent.makeChild(OutputConditionFactory.class, this.getClass(), classScope);
        method.getBlock().declareVar(ExprEvaluator[].class, "evals", newArrayByLength(ExprEvaluator.class, constant(scheduleSpecEvaluators.length)));
        for (int i = 0; i < scheduleSpecEvaluators.length; i++) {
            method.getBlock().assignArrayElement("evals", constant(i), ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(scheduleSpecEvaluators[i], method, this.getClass(), classScope));
        }
        method.getBlock().methodReturn(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETRESULTSETPROCESSORHELPERFACTORY)
                .add("makeOutputConditionCrontab", ref("evals"), constant(isStartConditionOnCreation), constant(scheduleCallbackId)));
        return localMethod(method);
    }

    public void collectSchedules(List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
        scheduleHandleCallbackProviders.add(this);
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }
}
