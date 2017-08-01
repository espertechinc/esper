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
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ReformatToCalendarForge implements ReformatForge, ReformatOp {

    private final TimeZone timeZone;
    private final TimeAbacus timeAbacus;

    public ReformatToCalendarForge(TimeZone timeZone, TimeAbacus timeAbacus) {
        this.timeZone = timeZone;
        this.timeAbacus = timeAbacus;
    }

    public ReformatOp getOp() {
        return this;
    }

    public Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance(timeZone);
        timeAbacus.calendarSet(ts, cal);
        return cal;
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, timeZone);
        String method = context.addMethod(Calendar.class, ReformatToCalendarForge.class).add(long.class, "ts").add(params).begin()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", ref(tz.getMemberName())))
                .expression(timeAbacus.calendarSetCodegen(ref("ts"), ref("cal"), context))
                .methodReturn(ref("cal"));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(d.getTime());
        return cal;
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, timeZone);
        String method = context.addMethod(Calendar.class, ReformatToCalendarForge.class).add(Date.class, "d").begin()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", ref(tz.getMemberName())))
                .exprDotMethod(ref("cal"), "setTimeInMillis", exprDotMethod(ref("d"), "getTime"))
                .methodReturn(ref("cal"));
        return localMethodBuild(method).pass(inner).call();
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return cal;
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        return inner;
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = GregorianCalendar.getInstance(timeZone);
        cal.setTimeInMillis(DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone(ldt, timeZone));
        return cal;
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, timeZone);
        String method = context.addMethod(Calendar.class, ReformatToCalendarForge.class).add(LocalDateTime.class, "ldt").begin()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", ref(tz.getMemberName())))
                .exprDotMethod(ref("cal"), "setTimeInMillis", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("ldt"), ref(tz.getMemberName())))
                .methodReturn(ref("cal"));
        return localMethodBuild(method).pass(inner).call();
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = GregorianCalendar.getInstance(timeZone);
        cal.setTimeInMillis(DatetimeLongCoercerZonedDateTime.coerceZDTToMillis(zdt));
        return cal;
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, timeZone);
        String method = context.addMethod(Calendar.class, ReformatToCalendarForge.class).add(ZonedDateTime.class, "zdt").begin()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", ref(tz.getMemberName())))
                .exprDotMethod(ref("cal"), "setTimeInMillis", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("zdt")))
                .methodReturn(ref("cal"));
        return localMethodBuild(method).pass(inner).call();
    }

    public Class getReturnType() {
        return Calendar.class;
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodEnum currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }
}
