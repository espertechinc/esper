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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class CalendarWithDateForgeOp implements CalendarOp {

    private ExprEvaluator year;
    private ExprEvaluator month;
    private ExprEvaluator day;

    public CalendarWithDateForgeOp(ExprEvaluator year, ExprEvaluator month, ExprEvaluator day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer yearNum = getInt(year, eventsPerStream, isNewData, context);
        Integer monthNum = getInt(month, eventsPerStream, isNewData, context);
        Integer dayNum = getInt(day, eventsPerStream, isNewData, context);
        actionSetYMDCalendar(cal, yearNum, monthNum, dayNum);
    }

    public static CodegenExpression codegenCalendar(CalendarWithDateForge forge, CodegenExpression cal, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(void.class, CalendarWithDateForgeOp.class).add(Calendar.class, "value").add(params).begin();
        codegenDeclareInts(block, forge, params, context);
        String method = block.expression(staticMethod(CalendarWithDateForgeOp.class, "actionSetYMDCalendar", ref("value"), ref("year"), ref("month"), ref("day")))
                .methodEnd();
        return localMethodBuild(method).pass(cal).passAll(params).call();
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer yearNum = getInt(year, eventsPerStream, isNewData, context);
        Integer monthNum = getInt(month, eventsPerStream, isNewData, context);
        Integer dayNum = getInt(day, eventsPerStream, isNewData, context);
        return actionSetYMDLocalDateTime(ldt, yearNum, monthNum, dayNum);
    }

    public static CodegenExpression codegenLDT(CalendarWithDateForge forge, CodegenExpression ldt, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(LocalDateTime.class, CalendarWithDateForgeOp.class).add(LocalDateTime.class, "value").add(params).begin();
        codegenDeclareInts(block, forge, params, context);
        String method = block.methodReturn(staticMethod(CalendarWithDateForgeOp.class, "actionSetYMDLocalDateTime", ref("value"), ref("year"), ref("month"), ref("day")));
        return localMethodBuild(method).pass(ldt).passAll(params).call();
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer yearNum = getInt(year, eventsPerStream, isNewData, context);
        Integer monthNum = getInt(month, eventsPerStream, isNewData, context);
        Integer dayNum = getInt(day, eventsPerStream, isNewData, context);
        return actionSetYMDZonedDateTime(zdt, yearNum, monthNum, dayNum);
    }

    public static CodegenExpression codegenZDT(CalendarWithDateForge forge, CodegenExpression zdt, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(ZonedDateTime.class, CalendarWithDateForgeOp.class).add(ZonedDateTime.class, "value").add(params).begin();
        codegenDeclareInts(block, forge, params, context);
        String method = block.methodReturn(staticMethod(CalendarWithDateForgeOp.class, "actionSetYMDZonedDateTime", ref("value"), ref("year"), ref("month"), ref("day")));
        return localMethodBuild(method).pass(zdt).passAll(params).call();
    }

    protected static Integer getInt(ExprEvaluator expr, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = expr.evaluate(eventsPerStream, isNewData, context);
        if (result == null) {
            return null;
        }
        return (Integer) result;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param cal calendar
     * @param year year
     * @param month month
     * @param day day
     */
    public static void actionSetYMDCalendar(Calendar cal, Integer year, Integer month, Integer day) {
        if (year != null) {
            cal.set(Calendar.YEAR, year);
        }
        if (month != null) {
            cal.set(Calendar.MONTH, month);
        }
        if (day != null) {
            cal.set(Calendar.DATE, day);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param ldt localdatetime
     * @param year year
     * @param month month
     * @param day day
     * @return ldt
     */
    public static LocalDateTime actionSetYMDLocalDateTime(LocalDateTime ldt, Integer year, Integer month, Integer day) {
        if (year != null) {
            ldt = ldt.withYear(year);
        }
        if (month != null) {
            ldt = ldt.withMonth(month);
        }
        if (day != null) {
            ldt = ldt.withDayOfMonth(day);
        }
        return ldt;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param zdt zoneddatetime
     * @param year year
     * @param month month
     * @param day day
     * @return ldt
     */
    public static ZonedDateTime actionSetYMDZonedDateTime(ZonedDateTime zdt, Integer year, Integer month, Integer day) {
        if (year != null) {
            zdt = zdt.withYear(year);
        }
        if (month != null) {
            zdt = zdt.withMonth(month);
        }
        if (day != null) {
            zdt = zdt.withDayOfMonth(day);
        }
        return zdt;
    }

    private static void codegenDeclareInts(CodegenBlock block, CalendarWithDateForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        block.declareVar(Integer.class, "year", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.year.evaluateCodegen(params, context), forge.year.getEvaluationType(), context))
                .declareVar(Integer.class, "month", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.month.evaluateCodegen(params, context), forge.month.getEvaluationType(), context))
                .declareVar(Integer.class, "day", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.day.evaluateCodegen(params, context), forge.day.getEvaluationType(), context));
    }
}
