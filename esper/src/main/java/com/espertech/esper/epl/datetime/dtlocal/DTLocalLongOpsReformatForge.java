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

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.datetime.calop.CalendarForge;
import com.espertech.esper.epl.datetime.reformatop.ReformatForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.getCalendarOps;

public class DTLocalLongOpsReformatForge extends DTLocalForgeCalopReformatBase {

    protected final TimeZone timeZone;
    protected final TimeAbacus timeAbacus;

    public DTLocalLongOpsReformatForge(List<CalendarForge> calendarForges, ReformatForge reformatForge, TimeZone timeZone, TimeAbacus timeAbacus) {
        super(calendarForges, reformatForge);
        this.timeZone = timeZone;
        this.timeAbacus = timeAbacus;
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalLongOpsReformatEval(getCalendarOps(calendarForges), reformatForge.getOp(), timeZone, timeAbacus);
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return DTLocalLongOpsReformatEval.codegen(this, inner, codegenMethodScope, exprSymbol, codegenClassScope);
    }
}
