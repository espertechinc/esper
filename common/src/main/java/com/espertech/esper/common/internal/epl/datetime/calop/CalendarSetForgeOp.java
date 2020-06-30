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
package com.espertech.esper.common.internal.epl.datetime.calop;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

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

    public static CodegenExpression codegenCalendar(CalendarSetForge forge, CodegenExpression cal, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression calField = constant(forge.fieldName.getCalendarField());
        EPTypeClass evaluationType = (EPTypeClass) forge.valueExpr.getEvaluationType();
        if (evaluationType.getType().isPrimitive()) {
            CodegenExpression valueExpr = forge.valueExpr.evaluateCodegen(evaluationType, codegenMethodScope, exprSymbol, codegenClassScope);
            return exprDotMethod(cal, "set", calField, valueExpr);
        }

        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.VOID.getEPType(), CalendarSetForgeOp.class, codegenClassScope).addParam(EPTypePremade.CALENDAR.getEPType(), "cal");
        CodegenExpression valueExpr = forge.valueExpr.evaluateCodegen(evaluationType, methodNode, exprSymbol, codegenClassScope);
        CodegenExpression coercion = SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(valueExpr, evaluationType, methodNode, codegenClassScope);
        methodNode.getBlock()
                .declareVar(EPTypePremade.INTEGERBOXED.getEPType(), "value", coercion)
                .ifRefNullReturnNull("value")
                .expression(exprDotMethod(cal, "set", calField, ref("value")))
                .methodEnd();
        return localMethod(methodNode, cal);
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer value = CalendarOpUtil.getInt(valueExpr, eventsPerStream, isNewData, context);
        if (value == null) {
            return ldt;
        }
        return ldt.with(fieldName.getChronoField(), value);
    }

    public static CodegenExpression codegenLDT(CalendarSetForge forge, CodegenExpression ldt, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ChronoField chronoField = forge.fieldName.getChronoField();
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.LOCALDATETIME.getEPType(), CalendarSetForgeOp.class, codegenClassScope).addParam(EPTypePremade.LOCALDATETIME.getEPType(), "ldt");
        EPTypeClass evaluationType = (EPTypeClass) forge.valueExpr.getEvaluationType();

        CodegenExpression valueExpr = forge.valueExpr.evaluateCodegen(evaluationType, methodNode, exprSymbol, codegenClassScope);
        CodegenExpression coercion = SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(valueExpr, evaluationType, methodNode, codegenClassScope);
        methodNode.getBlock()
                .declareVar(EPTypePremade.INTEGERBOXED.getEPType(), "value", coercion)
                .ifRefNull("value").blockReturn(ref("ldt"))
                .methodReturn(exprDotMethod(ref("ldt"), "with", enumValue(ChronoField.class, chronoField.name()), ref("value")));
        return localMethod(methodNode, ldt);
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer value = CalendarOpUtil.getInt(valueExpr, eventsPerStream, isNewData, context);
        if (value == null) {
            return zdt;
        }
        return zdt.with(fieldName.getChronoField(), value);
    }

    public static CodegenExpression codegenZDT(CalendarSetForge forge, CodegenExpression zdt, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ChronoField chronoField = forge.fieldName.getChronoField();
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.ZONEDDATETIME.getEPType(), CalendarSetForgeOp.class, codegenClassScope).addParam(EPTypePremade.ZONEDDATETIME.getEPType(), "zdt");
        EPTypeClass evaluationType = (EPTypeClass) forge.valueExpr.getEvaluationType();

        CodegenExpression valueExpr = forge.valueExpr.evaluateCodegen(evaluationType, methodNode, exprSymbol, codegenClassScope);
        CodegenExpression coercion = SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(valueExpr, evaluationType, methodNode, codegenClassScope);
        methodNode.getBlock()
                .declareVar(EPTypePremade.INTEGERBOXED.getEPType(), "value", coercion)
                .ifRefNull("value").blockReturn(ref("zdt"))
                .methodReturn(exprDotMethod(ref("zdt"), "with", enumValue(ChronoField.class, chronoField.name()), ref("value")));
        return localMethod(methodNode, zdt);
    }
}
