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

import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.getCalendarOps;

public class DTLocalDateOpsIntervalForge extends DTLocalForgeCalOpsIntervalBase {

    protected final TimeZone timeZone;

    public DTLocalDateOpsIntervalForge(List<CalendarForge> calendarForges, IntervalForge intervalForge, TimeZone timeZone) {
        super(calendarForges, intervalForge);
        this.timeZone = timeZone;
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalDateOpsIntervalEval(getCalendarOps(calendarForges), intervalForge.getOp(), timeZone);
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        return DTLocalDateOpsIntervalEval.codegenPointInTime(this, inner, innerType, params, context);
    }

    public DTLocalEvaluatorIntervalComp makeEvaluatorComp() {
        return new DTLocalDateOpsIntervalEval(getCalendarOps(calendarForges), intervalForge.getOp(), timeZone);
    }

    public CodegenExpression codegen(CodegenExpressionRef start, CodegenExpressionRef end, CodegenParamSetExprPremade params, CodegenContext context) {
        return DTLocalDateOpsIntervalEval.codegenStartEnd(this, start, end, params, context);
    }
}
