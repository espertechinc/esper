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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class CalendarSetForgeOp implements CalendarOp {

    private final CalendarFieldEnum fieldName;
    private final ExprEvaluator valueExpr;

    public CalendarSetForgeOp(CalendarFieldEnum fieldName, ExprEvaluator valueExpr) {
        this.fieldName = fieldName;
        this.valueExpr = valueExpr;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer value = CalendarOpUtil.getInt(valueExpr, eventsPerStream, isNewData, context);
        if (value == null) {
            return;
        }
        cal.set(fieldName.getCalendarField(), value);
    }

    public static CodegenExpression codegenCalendar(CalendarSetForge forge, CodegenExpression cal, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenExpression calField = constant(forge.fieldName.getCalendarField());
        CodegenExpression valueExpr = forge.valueExpr.evaluateCodegen(params, context);
        if (forge.valueExpr.getEvaluationType().isPrimitive()) {
            return exprDotMethod(cal, "set", calField, valueExpr);
        }
        CodegenMethodId method = context.addMethod(void.class, CalendarSetForgeOp.class).add(Calendar.class, "cal").add(params).begin()
                .declareVar(Integer.class, "value", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(valueExpr, forge.valueExpr.getEvaluationType(), context))
                .ifRefNullReturnNull("value")
                .expression(exprDotMethod(cal, "set", calField, ref("value")))
                .methodEnd();
        return localMethodBuild(method).pass(cal).passAll(params).call();
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer value = CalendarOpUtil.getInt(valueExpr, eventsPerStream, isNewData, context);
        if (value == null) {
            return ldt;
        }
        return ldt.with(fieldName.getChronoField(), value);
    }

    public static CodegenExpression codegenLDT(CalendarSetForge forge, CodegenExpression ldt, CodegenParamSetExprPremade params, CodegenContext context) {
        ChronoField chronoField = forge.fieldName.getChronoField();
        CodegenMethodId method = context.addMethod(LocalDateTime.class, CalendarSetForgeOp.class).add(LocalDateTime.class, "ldt").add(params).begin()
                .declareVar(Integer.class, "value", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.valueExpr.evaluateCodegen(params, context), forge.valueExpr.getEvaluationType(), context))
                .ifRefNull("value").blockReturn(ref("ldt"))
                .methodReturn(exprDotMethod(ref("ldt"), "with", enumValue(ChronoField.class, chronoField.name()), ref("value")));
        return localMethodBuild(method).pass(ldt).passAll(params).call();
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer value = CalendarOpUtil.getInt(valueExpr, eventsPerStream, isNewData, context);
        if (value == null) {
            return zdt;
        }
        return zdt.with(fieldName.getChronoField(), value);
    }

    public static CodegenExpression codegenZDT(CalendarSetForge forge, CodegenExpression zdt, CodegenParamSetExprPremade params, CodegenContext context) {
        ChronoField chronoField = forge.fieldName.getChronoField();
        CodegenMethodId method = context.addMethod(ZonedDateTime.class, CalendarSetForgeOp.class).add(ZonedDateTime.class, "zdt").add(params).begin()
                .declareVar(Integer.class, "value", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.valueExpr.evaluateCodegen(params, context), forge.valueExpr.getEvaluationType(), context))
                .ifRefNull("value").blockReturn(ref("zdt"))
                .methodReturn(exprDotMethod(ref("zdt"), "with", enumValue(ChronoField.class, chronoField.name()), ref("value")));
        return localMethodBuild(method).pass(zdt).passAll(params).call();
    }
}
