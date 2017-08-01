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
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.calop.CalendarForge;

import java.util.List;

import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.getCalendarOps;

public class DTLocalCalOpsLocalDateTimeForge extends DTLocalForgeCalOpsCalBase implements DTLocalForge {
    public DTLocalCalOpsLocalDateTimeForge(List<CalendarForge> calendarForges) {
        super(calendarForges);
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalCalOpsLocalDateTimeEval(getCalendarOps(calendarForges));
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        return DTLocalCalOpsLocalDateTimeEval.codegen(this, inner, innerType, params, context);
    }
}
