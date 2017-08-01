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

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprForge;

public class CalendarWithTimeForge implements CalendarForge {

    protected ExprForge hour;
    protected ExprForge min;
    protected ExprForge sec;
    protected ExprForge msec;

    public CalendarWithTimeForge(ExprForge hour, ExprForge min, ExprForge sec, ExprForge msec) {
        this.hour = hour;
        this.min = min;
        this.sec = sec;
        this.msec = msec;
    }

    public CalendarOp getEvalOp() {
        return new CalendarWithTimeForgeOp(hour.getExprEvaluator(), min.getExprEvaluator(), sec.getExprEvaluator(), msec.getExprEvaluator());
    }

    public CodegenExpression codegenCalendar(CodegenExpression cal, CodegenParamSetExprPremade params, CodegenContext context) {
        return CalendarWithTimeForgeOp.codegenCalendar(this, cal, params, context);
    }

    public CodegenExpression codegenLDT(CodegenExpression ldt, CodegenParamSetExprPremade params, CodegenContext context) {
        return CalendarWithTimeForgeOp.codegenLDT(this, ldt, params, context);
    }

    public CodegenExpression codegenZDT(CodegenExpression zdt, CodegenParamSetExprPremade params, CodegenContext context) {
        return CalendarWithTimeForgeOp.codegenZDT(this, zdt, params, context);
    }
}
