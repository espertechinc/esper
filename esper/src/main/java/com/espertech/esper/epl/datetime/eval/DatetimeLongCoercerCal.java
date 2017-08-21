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
package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class DatetimeLongCoercerCal implements DatetimeLongCoercer {
    public long coerce(Object date) {
        return ((Calendar) date).getTimeInMillis();
    }

    public CodegenExpression codegen(CodegenExpression value, Class valueType, CodegenClassScope codegenClassScope) {
        if (valueType != Calendar.class) {
            throw new IllegalStateException("Expected a Calendar type");
        }
        return exprDotMethod(value, "getTimeInMillis");
    }
}
