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
import java.time.temporal.ChronoField;
import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class CalendarWithTimeForgeOp implements CalendarOp {

    private ExprEvaluator hour;
    private ExprEvaluator min;
    private ExprEvaluator sec;
    private ExprEvaluator msec;

    public CalendarWithTimeForgeOp(ExprEvaluator hour, ExprEvaluator min, ExprEvaluator sec, ExprEvaluator msec) {
        this.hour = hour;
        this.min = min;
        this.sec = sec;
        this.msec = msec;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer hourNum = CalendarWithDateForgeOp.getInt(hour, eventsPerStream, isNewData, context);
        Integer minNum = CalendarWithDateForgeOp.getInt(min, eventsPerStream, isNewData, context);
        Integer secNum = CalendarWithDateForgeOp.getInt(sec, eventsPerStream, isNewData, context);
        Integer msecNum = CalendarWithDateForgeOp.getInt(msec, eventsPerStream, isNewData, context);
        actionSetHMSMCalendar(cal, hourNum, minNum, secNum, msecNum);
    }

    public static CodegenExpression codegenCalendar(CalendarWithTimeForge forge, CodegenExpression cal, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(void.class, CalendarWithTimeForgeOp.class).add(Calendar.class, "cal").add(params).begin();
        codegenDeclareInts(block, forge, params, context);
        block.expression(staticMethod(CalendarWithTimeForgeOp.class, "actionSetHMSMCalendar", ref("cal"), ref("hour"), ref("minute"), ref("second"), ref("msec")));
        return localMethodBuild(block.methodEnd()).pass(cal).passAll(params).call();
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer hourNum = CalendarWithDateForgeOp.getInt(hour, eventsPerStream, isNewData, context);
        Integer minNum = CalendarWithDateForgeOp.getInt(min, eventsPerStream, isNewData, context);
        Integer secNum = CalendarWithDateForgeOp.getInt(sec, eventsPerStream, isNewData, context);
        Integer msecNum = CalendarWithDateForgeOp.getInt(msec, eventsPerStream, isNewData, context);
        return actionSetHMSMLocalDateTime(ldt, hourNum, minNum, secNum, msecNum);
    }

    public static CodegenExpression codegenLDT(CalendarWithTimeForge forge, CodegenExpression ldt, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(LocalDateTime.class, CalendarWithTimeForgeOp.class).add(LocalDateTime.class, "ldt").add(params).begin();
        codegenDeclareInts(block, forge, params, context);
        String method = block.methodReturn(staticMethod(CalendarWithTimeForgeOp.class, "actionSetHMSMLocalDateTime", ref("ldt"), ref("hour"), ref("minute"), ref("second"), ref("msec")));
        return localMethodBuild(method).pass(ldt).passAll(params).call();
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer hourNum = CalendarWithDateForgeOp.getInt(hour, eventsPerStream, isNewData, context);
        Integer minNum = CalendarWithDateForgeOp.getInt(min, eventsPerStream, isNewData, context);
        Integer secNum = CalendarWithDateForgeOp.getInt(sec, eventsPerStream, isNewData, context);
        Integer msecNum = CalendarWithDateForgeOp.getInt(msec, eventsPerStream, isNewData, context);
        return actionSetHMSMZonedDateTime(zdt, hourNum, minNum, secNum, msecNum);
    }

    public static CodegenExpression codegenZDT(CalendarWithTimeForge forge, CodegenExpression zdt, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(ZonedDateTime.class, CalendarWithTimeForgeOp.class).add(ZonedDateTime.class, "zdt").add(params).begin();
        codegenDeclareInts(block, forge, params, context);
        String method = block.methodReturn(staticMethod(CalendarWithTimeForgeOp.class, "actionSetHMSMZonedDateTime", ref("zdt"), ref("hour"), ref("minute"), ref("second"), ref("msec")));
        return localMethodBuild(method).pass(zdt).passAll(params).call();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param cal cal
     * @param hour hour
     * @param minute min
     * @param second sec
     * @param msec msec
     * @return ldt
     */
    public static void actionSetHMSMCalendar(Calendar cal, Integer hour, Integer minute, Integer second, Integer msec) {
        if (hour != null) {
            cal.set(Calendar.HOUR_OF_DAY, hour);
        }
        if (minute != null) {
            cal.set(Calendar.MINUTE, minute);
        }
        if (second != null) {
            cal.set(Calendar.SECOND, second);
        }
        if (msec != null) {
            cal.set(Calendar.MILLISECOND, msec);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param ldt ldt
     * @param hour hour
     * @param minute min
     * @param second sec
     * @param msec msec
     * @return ldt
     */
    public static LocalDateTime actionSetHMSMLocalDateTime(LocalDateTime ldt, Integer hour, Integer minute, Integer second, Integer msec) {
        if (hour != null) {
            ldt = ldt.with(ChronoField.HOUR_OF_DAY, hour);
        }
        if (minute != null) {
            ldt = ldt.with(ChronoField.MINUTE_OF_HOUR, minute);
        }
        if (second != null) {
            ldt = ldt.with(ChronoField.SECOND_OF_MINUTE, second);
        }
        if (msec != null) {
            ldt = ldt.with(ChronoField.MILLI_OF_SECOND, msec);
        }
        return ldt;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param zdt zdt
     * @param hour hour
     * @param minute min
     * @param second sec
     * @param msec msec
     * @return ldt
     */
    public static ZonedDateTime actionSetHMSMZonedDateTime(ZonedDateTime zdt, Integer hour, Integer minute, Integer second, Integer msec) {
        if (hour != null) {
            zdt = zdt.with(ChronoField.HOUR_OF_DAY, hour);
        }
        if (minute != null) {
            zdt = zdt.with(ChronoField.MINUTE_OF_HOUR, minute);
        }
        if (second != null) {
            zdt = zdt.with(ChronoField.SECOND_OF_MINUTE, second);
        }
        if (msec != null) {
            zdt = zdt.with(ChronoField.MILLI_OF_SECOND, msec);
        }
        return zdt;
    }

    private static void codegenDeclareInts(CodegenBlock block, CalendarWithTimeForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        block.declareVar(Integer.class, "hour", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.hour.evaluateCodegen(params, context), forge.hour.getEvaluationType(), context))
                .declareVar(Integer.class, "minute", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.min.evaluateCodegen(params, context), forge.min.getEvaluationType(), context))
                .declareVar(Integer.class, "second", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.sec.evaluateCodegen(params, context), forge.sec.getEvaluationType(), context))
                .declareVar(Integer.class, "msec", SimpleNumberCoercerFactory.SimpleNumberCoercerInt.coerceCodegenMayNull(forge.msec.evaluateCodegen(params, context), forge.msec.getEvaluationType(), context));
    }
}
