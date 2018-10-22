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
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarForge;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;

import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.common.internal.epl.datetime.dtlocal.DTLocalUtil.getCalendarOps;

public class DTLocalCalOpsDateForge extends DTLocalForgeCalOpsCalBase implements DTLocalForge {

    public DTLocalCalOpsDateForge(List<CalendarForge> calendarForges) {
        super(calendarForges);
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalCalOpsDateEval(getCalendarOps(calendarForges), TimeZone.getDefault());
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return DTLocalCalOpsDateEval.codegen(this, inner, innerType, codegenMethodScope, exprSymbol, codegenClassScope);
    }
}
