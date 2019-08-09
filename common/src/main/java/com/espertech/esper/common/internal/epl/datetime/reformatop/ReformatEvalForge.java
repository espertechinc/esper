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
package com.espertech.esper.common.internal.epl.datetime.reformatop;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.settings.RuntimeSettingsTimeZoneField;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ReformatEvalForge implements ReformatForge, ReformatOp {

    private final CalendarEval calendarEval;
    private final LocalDateTimeEval localDateTimeEval;
    private final ZonedDateTimeEval zonedDateTimeEval;
    private final TimeAbacus timeAbacus;

    public ReformatEvalForge(CalendarEval calendarEval, LocalDateTimeEval localDateTimeEval, ZonedDateTimeEval zonedDateTimeEval, TimeAbacus timeAbacus) {
        this.calendarEval = calendarEval;
        this.localDateTimeEval = localDateTimeEval;
        this.zonedDateTimeEval = zonedDateTimeEval;
        this.timeAbacus = timeAbacus;
    }

    public ReformatOp getOp() {
        return this;
    }

    public Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance();
        timeAbacus.calendarSet(ts, cal);
        return calendarEval.evaluateInternal(cal);
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenMethod method = codegenMethodScope.makeChild(int.class, ReformatEvalForge.class, codegenClassScope).addParam(long.class, "ts");
        method.getBlock()
            .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", timeZoneField))
            .expression(timeAbacus.calendarSetCodegen(ref("ts"), ref("cal"), method, codegenClassScope))
            .methodReturn(calendarEval.codegen(ref("cal")));
        return localMethodBuild(method).pass(inner).call();
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d.getTime());
        return calendarEval.evaluateInternal(cal);
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenMethod methodNode = codegenMethodScope.makeChild(int.class, ReformatEvalForge.class, codegenClassScope).addParam(Date.class, "d");

        methodNode.getBlock()
            .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", timeZoneField))
            .expression(exprDotMethod(ref("cal"), "setTimeInMillis", exprDotMethod(ref("d"), "getTime")))
            .methodReturn(calendarEval.codegen(ref("cal")));
        return localMethod(methodNode, inner);
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return localDateTimeEval.evaluateInternal(ldt);
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return localDateTimeEval.codegen(inner);
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return zonedDateTimeEval.evaluateInternal(zdt);
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return zonedDateTimeEval.codegen(inner);
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return calendarEval.evaluateInternal(cal);
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return calendarEval.codegen(inner);
    }

    public Class getReturnType() {
        return Integer.class;
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodDesc currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }
}
