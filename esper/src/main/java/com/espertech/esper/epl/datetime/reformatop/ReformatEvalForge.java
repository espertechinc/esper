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
package com.espertech.esper.epl.datetime.reformatop;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ReformatEvalForge implements ReformatForge, ReformatOp {

    private final CalendarEval calendarEval;
    private final LocalDateTimeEval localDateTimeEval;
    private final ZonedDateTimeEval zonedDateTimeEval;
    private final TimeZone timeZone;
    private final TimeAbacus timeAbacus;

    public ReformatEvalForge(CalendarEval calendarEval, LocalDateTimeEval localDateTimeEval, ZonedDateTimeEval zonedDateTimeEval, TimeZone timeZone, TimeAbacus timeAbacus) {
        this.calendarEval = calendarEval;
        this.localDateTimeEval = localDateTimeEval;
        this.zonedDateTimeEval = zonedDateTimeEval;
        this.timeZone = timeZone;
        this.timeAbacus = timeAbacus;
    }

    public ReformatOp getOp() {
        return this;
    }

    public Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance(timeZone);
        timeAbacus.calendarSet(ts, cal);
        return calendarEval.evaluateInternal(cal);
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, timeZone);
        String method = context.addMethod(int.class, ReformatEvalForge.class).add(long.class, "ts").add(params).begin()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", ref(tz.getMemberName())))
                .expression(timeAbacus.calendarSetCodegen(ref("ts"), ref("cal"), context))
                .methodReturn(calendarEval.codegen(ref("cal")));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(d.getTime());
        return calendarEval.evaluateInternal(cal);
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, timeZone);
        String method = context.addMethod(int.class, ReformatEvalForge.class).add(Date.class, "d").add(params).begin()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", ref(tz.getMemberName())))
                .expression(exprDotMethod(ref("cal"), "setTimeInMillis", exprDotMethod(ref("d"), "getTime")))
                .methodReturn(calendarEval.codegen(ref("cal")));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return localDateTimeEval.evaluateInternal(ldt);
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        return localDateTimeEval.codegen(inner);
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return zonedDateTimeEval.evaluateInternal(zdt);
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        return zonedDateTimeEval.codegen(inner);
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return calendarEval.evaluateInternal(cal);
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        return calendarEval.codegen(inner);
    }

    public Class getReturnType() {
        return Integer.class;
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodEnum currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }
}
