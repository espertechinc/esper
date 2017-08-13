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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Calendar;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprTimePeriodEvalDeltaNonConstCalAdd implements ExprTimePeriodEvalDeltaNonConst {
    private final Calendar cal;
    private final ExprTimePeriodForge forge;
    private final int indexMicroseconds;

    public ExprTimePeriodEvalDeltaNonConstCalAdd(TimeZone timeZone, ExprTimePeriodForge forge) {
        this.forge = forge;
        this.cal = Calendar.getInstance(timeZone);
        this.indexMicroseconds = ExprTimePeriodUtil.findIndexMicroseconds(forge.getAdders());
    }

    public synchronized long deltaAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return addSubtract(currentTime, 1, eventsPerStream, isNewData, context);
    }

    public CodegenExpression deltaAddCodegen(CodegenExpression reference, CodegenParamSetExprPremade params, CodegenContext context) {
        return addSubtractCodegen(reference, constant(1), params, context);
    }

    public synchronized long deltaSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return addSubtract(currentTime, -1, eventsPerStream, isNewData, context);
    }

    public synchronized long deltaUseEngineTime(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext) {
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        return addSubtract(currentTime, 1, eventsPerStream, true, agentInstanceContext);
    }

    public synchronized ExprTimePeriodEvalDeltaResult deltaAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // find the next-nearest reference higher then the current time, compute delta, return reference one lower
        if (reference > current) {
            while (reference > current) {
                reference = reference - deltaSubtract(reference, eventsPerStream, isNewData, context);
            }
        }

        long next = reference;
        long last;
        do {
            last = next;
            next = next + deltaAdd(last, eventsPerStream, isNewData, context);
        }
        while (next <= current);
        return new ExprTimePeriodEvalDeltaResult(next - current, last);
    }

    private long addSubtract(long currentTime, int factor, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        long remainder = forge.getTimeAbacus().calendarSet(currentTime, cal);

        ExprTimePeriodAdder.TimePeriodAdder[] adders = forge.getAdders();
        ExprEvaluator[] evaluators = forge.getEvaluators();
        int usec = 0;
        for (int i = 0; i < adders.length; i++) {
            int value = ((Number) evaluators[i].evaluate(eventsPerStream, newData, context)).intValue();
            if (i == indexMicroseconds) {
                usec = value;
            } else {
                adders[i].add(cal, factor * value);
            }
        }

        long result = forge.getTimeAbacus().calendarGet(cal, remainder);
        if (indexMicroseconds != -1) {
            result += factor * usec;
        }
        return result - currentTime;
    }

    private CodegenExpression addSubtractCodegen(CodegenExpression reference, CodegenExpression constant, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember calMember = context.makeAddMember(Calendar.class, cal);
        CodegenBlock block = context.addMethod(long.class, ExprTimePeriodEvalDeltaNonConstCalAdd.class).add(long.class, "currentTime").add(int.class, "factor").add(params).begin()
                .declareVarNoInit(long.class, "result")
                .synchronizedOn(member(calMember.getMemberId()))
                .declareVar(long.class, "remainder", forge.getTimeAbacus().calendarSetCodegen(ref("currentTime"), member(calMember.getMemberId()), context))
                .declareVar(int.class, "usec", constant(0));
        for (int i = 0; i < forge.getAdders().length; i++) {
            String refname = "v" + i;
            block.declareVar(int.class, refname, forge.getForgeRenderable().getChildNodes()[i].getForge().evaluateCodegen(params, context));
            if (i == indexMicroseconds) {
                block.assignRef("usec", ref(refname));
            } else {
                block.expression(forge.getAdders()[i].addCodegen(member(calMember.getMemberId()), op(ref("factor"), "*", ref(refname))));
            }
        }
        block.assignRef("result", forge.getTimeAbacus().calendarGetCodegen(member(calMember.getMemberId()), ref("remainder"), context));
        if (indexMicroseconds != -1) {
            block.assignRef("result", op(ref("result"), "+", op(ref("factor"), "*", ref("usec"))));
        }
        CodegenMethodId method = block.blockEnd().methodReturn(op(ref("result"), "-", ref("currentTime")));
        return localMethodBuild(method).pass(reference).pass(constant).passAll(params).call();
    }
}
