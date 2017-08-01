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

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.calop.CalendarForge;
import com.espertech.esper.epl.datetime.interval.IntervalForge;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.getCalendarOps;

public class DTLocalLongOpsIntervalForge extends DTLocalForgeCalOpsIntervalBase {

    protected final TimeZone timeZone;
    protected final TimeAbacus timeAbacus;

    public DTLocalLongOpsIntervalForge(List<CalendarForge> calendarForges, IntervalForge intervalForge, TimeZone timeZone, TimeAbacus timeAbacus) {
        super(calendarForges, intervalForge);
        this.timeZone = timeZone;
        this.timeAbacus = timeAbacus;
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalLongOpsIntervalEval(getCalendarOps(calendarForges), intervalForge.getOp(), timeZone, timeAbacus);
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        return DTLocalLongOpsIntervalEval.codegenPointInTime(this, inner, innerType, params, context);
    }

    public DTLocalEvaluatorIntervalComp makeEvaluatorComp() {
        return new DTLocalLongOpsIntervalEval(getCalendarOps(calendarForges), intervalForge.getOp(), timeZone, timeAbacus);
    }

    public CodegenExpression codegen(CodegenExpressionRef start, CodegenExpressionRef end, CodegenParamSetExprPremade params, CodegenContext context) {
        return DTLocalLongOpsIntervalEval.codegenStartEnd(this, start, end, params, context);
    }
}
