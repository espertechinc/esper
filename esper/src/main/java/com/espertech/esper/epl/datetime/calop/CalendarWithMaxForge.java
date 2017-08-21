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
package com.espertech.esper.epl.datetime.calop;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;
import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class CalendarWithMaxForge implements CalendarForge, CalendarOp {

    private final CalendarFieldEnum fieldName;

    public CalendarWithMaxForge(CalendarFieldEnum fieldName) {
        this.fieldName = fieldName;
    }

    public CalendarOp getEvalOp() {
        return this;
    }

    public CodegenExpression codegenCalendar(CodegenExpression cal, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return exprDotMethod(cal, "set", constant(fieldName.getCalendarField()), exprDotMethod(cal, "getActualMaximum", constant(fieldName.getCalendarField())));
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        cal.set(fieldName.getCalendarField(), cal.getActualMaximum(fieldName.getCalendarField()));
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ValueRange range = ldt.range(fieldName.getChronoField());
        return ldt.with(fieldName.getChronoField(), range.getMaximum());
    }

    public CodegenExpression codegenLDT(CodegenExpression ldt, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return codegenLDTZDTMinMax(ldt, true, fieldName);
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ValueRange range = zdt.range(fieldName.getChronoField());
        return zdt.with(fieldName.getChronoField(), range.getMaximum());
    }

    public CodegenExpression codegenZDT(CodegenExpression zdt, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return codegenLDTZDTMinMax(zdt, true, fieldName);
    }

    protected static CodegenExpression codegenLDTZDTMinMax(CodegenExpression val, boolean max, CalendarFieldEnum fieldName) {
        CodegenExpression chronoField = enumValue(ChronoField.class, fieldName.getChronoField().name());
        CodegenExpression valueRange = exprDotMethod(val, "range", chronoField);
        return exprDotMethod(val, "with", chronoField, exprDotMethod(valueRange, max ? "getMaximum" : "getMinimum"));
    }
}
