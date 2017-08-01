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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsCalendarCodegen;

public class DTLocalCalOpsLongEval extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {

    private final TimeZone timeZone;
    private final TimeAbacus timeAbacus;

    public DTLocalCalOpsLongEval(List<CalendarOp> calendarOps, TimeZone timeZone, TimeAbacus timeAbacus) {
        super(calendarOps);
        this.timeZone = timeZone;
        this.timeAbacus = timeAbacus;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Long longValue = (Long) target;
        Calendar cal = Calendar.getInstance(timeZone);
        long remainder = timeAbacus.calendarSet(longValue, cal);

        DTLocalUtil.evaluateCalOpsCalendar(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);

        return timeAbacus.calendarGet(cal, remainder);
    }

    public static CodegenExpression codegen(DTLocalCalOpsLongForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, forge.timeZone);
        CodegenBlock block = context.addMethod(long.class, DTLocalCalOpsLongEval.class).add(long.class, "target").add(params).begin()
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", ref(tz.getMemberName())))
                .declareVar(long.class, "remainder", forge.timeAbacus.calendarSetCodegen(ref("target"), ref("cal"), context));
        evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), params, context);
        String method = block.methodReturn(forge.timeAbacus.calendarGetCodegen(ref("cal"), ref("remainder"), context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }
}
