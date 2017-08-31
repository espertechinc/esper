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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;

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

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Calendar.class, ReformatToCalendarForge.class, codegenClassScope).addParam(long.class, "ts");

        methodNode.getBlock()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", member(tz.getMemberId())))
                .expression(timeAbacus.calendarSetCodegen(ref("ts"), ref("cal"), methodNode, codegenClassScope))
                .methodReturn(ref("cal"));
        return localMethod(methodNode, inner);
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(d.getTime());
        return cal;
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
        CodegenMethodNode method = codegenMethodScope.makeChild(Calendar.class, ReformatToCalendarForge.class, codegenClassScope).addParam(Date.class, "d").getBlock()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", member(tz.getMemberId())))
                .exprDotMethod(ref("cal"), "setTimeInMillis", exprDotMethod(ref("d"), "getTime"))
                .methodReturn(ref("cal"));
        return localMethodBuild(method).pass(inner).call();
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return cal;
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return inner;
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = GregorianCalendar.getInstance(timeZone);
        cal.setTimeInMillis(DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone(ldt, timeZone));
        return cal;
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
        CodegenMethodNode method = codegenMethodScope.makeChild(Calendar.class, ReformatToCalendarForge.class, codegenClassScope).addParam(LocalDateTime.class, "ldt").getBlock()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", member(tz.getMemberId())))
                .exprDotMethod(ref("cal"), "setTimeInMillis", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("ldt"), member(tz.getMemberId())))
                .methodReturn(ref("cal"));
        return localMethodBuild(method).pass(inner).call();
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = GregorianCalendar.getInstance(timeZone);
        cal.setTimeInMillis(DatetimeLongCoercerZonedDateTime.coerceZDTToMillis(zdt));
        return cal;
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
        CodegenMethodNode method = codegenMethodScope.makeChild(Calendar.class, ReformatToCalendarForge.class, codegenClassScope).addParam(ZonedDateTime.class, "zdt").getBlock()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", member(tz.getMemberId())))
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
