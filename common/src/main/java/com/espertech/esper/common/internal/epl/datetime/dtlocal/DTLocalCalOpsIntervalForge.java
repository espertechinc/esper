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
package com.espertech.esper.common.internal.epl.datetime.dtlocal;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarForge;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalForge;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;

import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.common.internal.epl.datetime.dtlocal.DTLocalUtil.getCalendarOps;

public class DTLocalCalOpsIntervalForge extends DTLocalForgeCalOpsIntervalBase {

    public DTLocalCalOpsIntervalForge(List<CalendarForge> calendarForges, IntervalForge intervalForge) {
        super(calendarForges, intervalForge);
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalCalOpsIntervalEval(getCalendarOps(calendarForges), intervalForge.getOp(), TimeZone.getDefault());
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return DTLocalCalOpsIntervalEval.codegenPointInTime(this, inner, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public DTLocalEvaluatorIntervalComp makeEvaluatorComp() {
        return new DTLocalCalOpsIntervalEval(getCalendarOps(calendarForges), intervalForge.getOp(), TimeZone.getDefault());
    }

    public CodegenExpression codegen(CodegenExpressionRef start, CodegenExpressionRef end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return DTLocalCalOpsIntervalEval.codegenStartEnd(this, start, end, codegenMethodScope, exprSymbol, codegenClassScope);
    }
}
