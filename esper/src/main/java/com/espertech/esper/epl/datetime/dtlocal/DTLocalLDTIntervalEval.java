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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

class DTLocalLDTIntervalEval extends DTLocalEvaluatorIntervalBase {

    private final TimeZone timeZone;

    public DTLocalLDTIntervalEval(IntervalOp intervalOp, TimeZone timeZone) {
        super(intervalOp);
        this.timeZone = timeZone;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long time = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) target, timeZone);
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalLDTIntervalForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, forge.timeZone);
        String method = context.addMethod(Boolean.class, DTLocalLDTIntervalEval.class).add(LocalDateTime.class, "target").add(params).begin()
                .declareVar(long.class, "time", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("target"), ref(tz.getMemberName())))
                .methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long start = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) startTimestamp, timeZone);
        long end = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) endTimestamp, timeZone);
        return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalLDTIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, forge.timeZone);
        String method = context.addMethod(Boolean.class, DTLocalLDTIntervalEval.class).add(LocalDateTime.class, "startTimestamp").add(LocalDateTime.class, "endTimestamp").add(params).begin()
                .declareVar(long.class, "start", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("startTimestamp"), ref(tz.getMemberName())))
                .declareVar(long.class, "end", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("endTimestamp"), ref(tz.getMemberName())))
                .methodReturn(forge.intervalForge.codegen(ref("start"), ref("end"), params, context));
        return localMethodBuild(method).pass(start).pass(end).passAll(params).call();
    }
}
