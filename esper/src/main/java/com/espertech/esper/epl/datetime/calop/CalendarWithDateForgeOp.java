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
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class CalendarWithDateForgeOp implements CalendarOp {
    public final static String METHOD_ACTIONSETYMDCALENDAR = "actionSetYMDCalendar";

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

    public static CodegenExpression codegenCalendar(CalendarWithDateForge forge, CodegenExpression cal, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(void.class, CalendarWithDateForgeOp.class, codegenClassScope).addParam(Calendar.class, "value");


        CodegenBlock block = methodNode.getBlock();
        codegenDeclareInts(block, forge, methodNode, exprSymbol, codegenClassScope);
        block.staticMethod(CalendarWithDateForgeOp.class, METHOD_ACTIONSETYMDCALENDAR, ref("value"), ref("year"), ref("month"), ref("day"))
                .methodEnd();
        return localMethod(methodNode, cal);
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer yearNum = getInt(year, eventsPerStream, isNewData, context);
        Integer monthNum = getInt(month, eventsPerStream, isNewData, context);
        Integer dayNum = getInt(day, eventsPerStream, isNewData, context);
        return actionSetYMDLocalDateTime(ldt, yearNum, monthNum, dayNum);
    }

    public static CodegenExpression codegenLDT(CalendarWithDateForge forge, CodegenExpression ldt, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(LocalDateTime.class, CalendarWithDateForgeOp.class, codegenClassScope).addParam(LocalDateTime.class, "value");


        CodegenBlock block = methodNode.getBlock();
        codegenDeclareInts(block, forge, methodNode, exprSymbol, codegenClassScope);
        block.methodReturn(staticMethod(CalendarWithDateForgeOp.class, "actionSetYMDLocalDateTime", ref("value"), ref("year"), ref("month"), ref("day")));
        return localMethod(methodNode, ldt);
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer yearNum = getInt(year, eventsPerStream, isNewData, context);
        Integer monthNum = getInt(month, eventsPerStream, isNewData, context);
        Integer dayNum = getInt(day, eventsPerStream, isNewData, context);
        return actionSetYMDZonedDateTime(zdt, yearNum, monthNum, dayNum);
    }

    public static CodegenExpression codegenZDT(CalendarWithDateForge forge, CodegenExpression zdt, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(ZonedDateTime.class, CalendarWithDateForgeOp.class, codegenClassScope).addParam(ZonedDateTime.class, "value");

        CodegenBlock block = methodNode.getBlock();
        codegenDeclareInts(block, forge, methodNode, exprSymbol, codegenClassScope);
        block.methodReturn(staticMethod(CalendarWithDateForgeOp.class, "actionSetYMDZonedDateTime", ref("value"), ref("year"), ref("month"), ref("day")));
        return localMethod(methodNode, zdt);
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

    private static void codegenDeclareInts(CodegenBlock block, CalendarWithDateForge forge, CodegenMethodNode methodNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class yearType = forge.year.getEvaluationType();
        Class monthType = forge.month.getEvaluationType();
        Class dayType = forge.day.getEvaluationType();
        block.declareVar(Integer.class, "year", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.year.evaluateCodegen(yearType, methodNode, exprSymbol, codegenClassScope), yearType, methodNode, codegenClassScope))
                .declareVar(Integer.class, "month", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.month.evaluateCodegen(monthType, methodNode, exprSymbol, codegenClassScope), monthType, methodNode, codegenClassScope))
                .declareVar(Integer.class, "day", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.day.evaluateCodegen(dayType, methodNode, exprSymbol, codegenClassScope), dayType, methodNode, codegenClassScope));
    }
}
