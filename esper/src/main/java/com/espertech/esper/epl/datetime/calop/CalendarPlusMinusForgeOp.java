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
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class CalendarPlusMinusForgeOp implements CalendarOp {

    private final ExprEvaluator param;
    private final int factor;

    public CalendarPlusMinusForgeOp(ExprEvaluator param, int factor) {
        this.param = param;
        this.factor = factor;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object value = param.evaluate(eventsPerStream, isNewData, context);
        if (value instanceof Number) {
            actionCalendarPlusMinusNumber(cal, factor, ((Number) value).longValue());
        } else {
            actionCalendarPlusMinusTimePeriod(cal, factor, (TimePeriod) value);
        }
    }

    public static CodegenExpression codegenCalendar(CalendarPlusMinusForge forge, CodegenExpression cal, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class evaluationType = forge.param.getEvaluationType();
        if (JavaClassHelper.isNumeric(evaluationType)) {
            CodegenExpression longDuration = SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLong(forge.param.evaluateCodegen(evaluationType, codegenMethodScope, exprSymbol, codegenClassScope), evaluationType);
            return staticMethod(CalendarPlusMinusForgeOp.class, "actionCalendarPlusMinusNumber", cal, constant(forge.factor), longDuration);
        }
        return staticMethod(CalendarPlusMinusForgeOp.class, "actionCalendarPlusMinusTimePeriod", cal, constant(forge.factor), forge.param.evaluateCodegen(evaluationType, codegenMethodScope, exprSymbol, codegenClassScope));
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object value = param.evaluate(eventsPerStream, isNewData, context);
        if (value instanceof Number) {
            return actionLDTPlusMinusNumber(ldt, factor, ((Number) value).longValue());
        } else {
            return actionLDTPlusMinusTimePeriod(ldt, factor, (TimePeriod) value);
        }
    }

    public static CodegenExpression codegenLDT(CalendarPlusMinusForge forge, CodegenExpression ldt, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class evaluationType = forge.param.getEvaluationType();
        if (JavaClassHelper.isNumeric(evaluationType)) {
            CodegenExpression longDuration = SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLongMayNullBox(forge.param.evaluateCodegen(evaluationType, codegenMethodScope, exprSymbol, codegenClassScope), evaluationType, codegenMethodScope, codegenClassScope);
            return staticMethod(CalendarPlusMinusForgeOp.class, "actionLDTPlusMinusNumber", ldt, constant(forge.factor), longDuration);
        }
        return staticMethod(CalendarPlusMinusForgeOp.class, "actionLDTPlusMinusTimePeriod", ldt, constant(forge.factor), forge.param.evaluateCodegen(evaluationType, codegenMethodScope, exprSymbol, codegenClassScope));
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object value = param.evaluate(eventsPerStream, isNewData, context);
        if (value instanceof Number) {
            return actionZDTPlusMinusNumber(zdt, factor, ((Number) value).longValue());
        } else {
            return actionZDTPlusMinusTimePeriod(zdt, factor, (TimePeriod) value);
        }
    }

    public static CodegenExpression codegenZDT(CalendarPlusMinusForge forge, CodegenExpression zdt, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class evaluationType = forge.param.getEvaluationType();
        if (JavaClassHelper.isNumeric(evaluationType)) {
            CodegenExpression longDuration = SimpleNumberCoercerFactory.SimpleNumberCoercerLong.codegenLongMayNullBox(forge.param.evaluateCodegen(evaluationType, codegenMethodScope, exprSymbol, codegenClassScope), evaluationType, codegenMethodScope, codegenClassScope);
            return staticMethod(CalendarPlusMinusForgeOp.class, "actionZDTPlusMinusNumber", zdt, constant(forge.factor), longDuration);
        }
        return staticMethod(CalendarPlusMinusForgeOp.class, "actionZDTPlusMinusTimePeriod", zdt, constant(forge.factor), forge.param.evaluateCodegen(evaluationType, codegenMethodScope, exprSymbol, codegenClassScope));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param cal calendar
     * @param factor factor
     * @param duration duration
     */
    public static void actionCalendarPlusMinusNumber(Calendar cal, int factor, Long duration) {
        if (duration == null) {
            return;
        }
        if (duration < Integer.MAX_VALUE) {
            cal.add(Calendar.MILLISECOND, (int) (factor * duration));
            return;
        }

        int days = (int) (duration / (1000L * 60 * 60 * 24));
        int msec = (int) (duration - days * (1000L * 60 * 60 * 24));
        cal.add(Calendar.MILLISECOND, factor * msec);
        cal.add(Calendar.DATE, factor * days);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param ldt ldt
     * @param factor factor
     * @param duration duration
     * @return ldt
     */
    public static LocalDateTime actionLDTPlusMinusNumber(LocalDateTime ldt, int factor, Long duration) {
        if (duration == null) {
            return ldt;
        }
        if (duration < Integer.MAX_VALUE) {
            return ldt.plus(factor * duration, ChronoUnit.MILLIS);
        }

        int days = (int) (duration / (1000L * 60 * 60 * 24));
        int msec = (int) (duration - days * (1000L * 60 * 60 * 24));
        ldt = ldt.plus(factor * msec, ChronoUnit.MILLIS);
        return ldt.plus(factor * days, ChronoUnit.DAYS);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param zdt ldt
     * @param factor factor
     * @param duration duration
     * @return zdt
     */
    public static ZonedDateTime actionZDTPlusMinusNumber(ZonedDateTime zdt, int factor, Long duration) {
        if (duration == null) {
            return zdt;
        }
        if (duration < Integer.MAX_VALUE) {
            return zdt.plus(factor * duration, ChronoUnit.MILLIS);
        }

        int days = (int) (duration / (1000L * 60 * 60 * 24));
        int msec = (int) (duration - days * (1000L * 60 * 60 * 24));
        zdt = zdt.plus(factor * msec, ChronoUnit.MILLIS);
        return zdt.plus(factor * days, ChronoUnit.DAYS);
    }

    public static void actionSafeOverflow(Calendar cal, int factor, TimePeriod tp) {
        if (Math.abs(factor) == 1) {
            actionCalendarPlusMinusTimePeriod(cal, factor, tp);
            return;
        }
        Integer max = tp.largestAbsoluteValue();
        if (max == null || max == 0) {
            return;
        }
        actionHandleOverflow(cal, factor, tp, max);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param cal calendar
     * @param factor factor
     * @param tp duration
     */
    public static void actionCalendarPlusMinusTimePeriod(Calendar cal, int factor, TimePeriod tp) {
        if (tp == null) {
            return;
        }
        if (tp.getYears() != null) {
            cal.add(Calendar.YEAR, factor * tp.getYears());
        }
        if (tp.getMonths() != null) {
            cal.add(Calendar.MONTH, factor * tp.getMonths());
        }
        if (tp.getWeeks() != null) {
            cal.add(Calendar.WEEK_OF_YEAR, factor * tp.getWeeks());
        }
        if (tp.getDays() != null) {
            cal.add(Calendar.DATE, factor * tp.getDays());
        }
        if (tp.getHours() != null) {
            cal.add(Calendar.HOUR_OF_DAY, factor * tp.getHours());
        }
        if (tp.getMinutes() != null) {
            cal.add(Calendar.MINUTE, factor * tp.getMinutes());
        }
        if (tp.getSeconds() != null) {
            cal.add(Calendar.SECOND, factor * tp.getSeconds());
        }
        if (tp.getMilliseconds() != null) {
            cal.add(Calendar.MILLISECOND, factor * tp.getMilliseconds());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param ldt ldt
     * @param factor factor
     * @param tp duration
     * @return ldt
     */
    public static LocalDateTime actionLDTPlusMinusTimePeriod(LocalDateTime ldt, int factor, TimePeriod tp) {
        if (tp == null) {
            return ldt;
        }
        if (tp.getYears() != null) {
            ldt = ldt.plus(factor * tp.getYears(), ChronoUnit.YEARS);
        }
        if (tp.getMonths() != null) {
            ldt = ldt.plus(factor * tp.getMonths(), ChronoUnit.MONTHS);
        }
        if (tp.getWeeks() != null) {
            ldt = ldt.plus(factor * tp.getWeeks(), ChronoUnit.WEEKS);
        }
        if (tp.getDays() != null) {
            ldt = ldt.plus(factor * tp.getDays(), ChronoUnit.DAYS);
        }
        if (tp.getHours() != null) {
            ldt = ldt.plus(factor * tp.getHours(), ChronoUnit.HOURS);
        }
        if (tp.getMinutes() != null) {
            ldt = ldt.plus(factor * tp.getMinutes(), ChronoUnit.MINUTES);
        }
        if (tp.getSeconds() != null) {
            ldt = ldt.plus(factor * tp.getSeconds(), ChronoUnit.SECONDS);
        }
        if (tp.getMilliseconds() != null) {
            ldt = ldt.plus(factor * tp.getMilliseconds(), ChronoUnit.MILLIS);
        }
        return ldt;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param zdt zdt
     * @param factor factor
     * @param tp duration
     * @return zdt
     */
    public static ZonedDateTime actionZDTPlusMinusTimePeriod(ZonedDateTime zdt, int factor, TimePeriod tp) {
        if (tp == null) {
            return zdt;
        }
        if (tp.getYears() != null) {
            zdt = zdt.plus(factor * tp.getYears(), ChronoUnit.YEARS);
        }
        if (tp.getMonths() != null) {
            zdt = zdt.plus(factor * tp.getMonths(), ChronoUnit.MONTHS);
        }
        if (tp.getWeeks() != null) {
            zdt = zdt.plus(factor * tp.getWeeks(), ChronoUnit.WEEKS);
        }
        if (tp.getDays() != null) {
            zdt = zdt.plus(factor * tp.getDays(), ChronoUnit.DAYS);
        }
        if (tp.getHours() != null) {
            zdt = zdt.plus(factor * tp.getHours(), ChronoUnit.HOURS);
        }
        if (tp.getMinutes() != null) {
            zdt = zdt.plus(factor * tp.getMinutes(), ChronoUnit.MINUTES);
        }
        if (tp.getSeconds() != null) {
            zdt = zdt.plus(factor * tp.getSeconds(), ChronoUnit.SECONDS);
        }
        if (tp.getMilliseconds() != null) {
            zdt = zdt.plus(factor * tp.getMilliseconds(), ChronoUnit.MILLIS);
        }
        return zdt;
    }

    private static void actionHandleOverflow(Calendar cal, int factor, TimePeriod tp, int max) {
        if (max != 0 && factor > Integer.MAX_VALUE / max) {
            // overflow
            int first = factor / 2;
            int second = (factor - first * 2) + first;
            actionHandleOverflow(cal, first, tp, max);
            actionHandleOverflow(cal, second, tp, max);
        } else {
            // no overflow
            actionCalendarPlusMinusTimePeriod(cal, factor, tp);
        }
    }
}
