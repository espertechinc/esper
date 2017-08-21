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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.datetime.calop.CalendarForge;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class DTLocalUtil {
    public static List<CalendarOp> getCalendarOps(List<CalendarForge> forges) {
        List<CalendarOp> ops = new ArrayList<>(forges.size());
        for (CalendarForge forge : forges) {
            ops.add(forge.getEvalOp());
        }
        return ops;
    }

    protected static void evaluateCalOpsCalendar(List<CalendarOp> calendarOps, Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        for (CalendarOp calendarOp : calendarOps) {
            calendarOp.evaluate(cal, eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    protected static void evaluateCalOpsCalendarCodegen(CodegenBlock block, List<CalendarForge> calendarForges, CodegenExpressionRef cal, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        for (CalendarForge calendarForge : calendarForges) {
            block.expression(calendarForge.codegenCalendar(cal, codegenMethodScope, exprSymbol, codegenClassScope));
        }
    }

    protected static LocalDateTime evaluateCalOpsLDT(List<CalendarOp> calendarOps, LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        for (CalendarOp calendarOp : calendarOps) {
            ldt = calendarOp.evaluate(ldt, eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return ldt;
    }

    protected static void evaluateCalOpsLDTCodegen(CodegenBlock block, String resultVariable, List<CalendarForge> calendarForges, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        for (CalendarForge calendarForge : calendarForges) {
            block.assignRef(resultVariable, calendarForge.codegenLDT(ref(resultVariable), codegenMethodScope, exprSymbol, codegenClassScope));
        }
    }

    protected static ZonedDateTime evaluateCalOpsZDT(List<CalendarOp> calendarOps, ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        for (CalendarOp calendarOp : calendarOps) {
            zdt = calendarOp.evaluate(zdt, eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return zdt;
    }

    protected static void evaluateCalOpsZDTCodegen(CodegenBlock block, String resultVariable, List<CalendarForge> calendarForges, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        for (CalendarForge calendarForge : calendarForges) {
            block.assignRef(resultVariable, calendarForge.codegenZDT(ref(resultVariable), codegenMethodScope, exprSymbol, codegenClassScope));
        }
    }
}
