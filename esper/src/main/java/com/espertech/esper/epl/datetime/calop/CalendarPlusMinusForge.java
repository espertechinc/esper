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

public class CalendarPlusMinusForge implements CalendarForge {

    protected final ExprForge param;
    protected final int factor;

    public CalendarPlusMinusForge(ExprForge param, int factor) {
        this.param = param;
        this.factor = factor;
    }

    public CalendarOp getEvalOp() {
        return new CalendarPlusMinusForgeOp(param.getExprEvaluator(), factor);
    }

    public CodegenExpression codegenCalendar(CodegenExpression cal, CodegenParamSetExprPremade params, CodegenContext context) {
        return CalendarPlusMinusForgeOp.codegenCalendar(this, cal, params, context);
    }

    public CodegenExpression codegenLDT(CodegenExpression ldt, CodegenParamSetExprPremade params, CodegenContext context) {
        return CalendarPlusMinusForgeOp.codegenLDT(this, ldt, params, context);
    }

    public CodegenExpression codegenZDT(CodegenExpression zdt, CodegenParamSetExprPremade params, CodegenContext context) {
        return CalendarPlusMinusForgeOp.codegenZDT(this, zdt, params, context);
    }
}
