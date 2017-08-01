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
package com.espertech.esper.epl.datetime.reformatop;

import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class CalendarEvalStatics {

    public final static CalendarEval MINUTE_OF_HOUR = new CalendarEvalImpl(Calendar.MINUTE);
    public final static CalendarEval MONTH_OF_YEAR = new CalendarEvalImpl(Calendar.MONTH);
    public final static CalendarEval DAY_OF_MONTH = new CalendarEvalImpl(Calendar.DAY_OF_MONTH);
    public final static CalendarEval DAY_OF_WEEK = new CalendarEvalImpl(Calendar.DAY_OF_WEEK);
    public final static CalendarEval DAY_OF_YEAR = new CalendarEvalImpl(Calendar.DAY_OF_YEAR);
    public final static CalendarEval ERA = new CalendarEvalImpl(Calendar.ERA);
    public final static CalendarEval HOUR_OF_DAY = new CalendarEvalImpl(Calendar.HOUR_OF_DAY);
    public final static CalendarEval MILLIS_OF_SECOND = new CalendarEvalImpl(Calendar.MILLISECOND);
    public final static CalendarEval SECOND_OF_MINUTE = new CalendarEvalImpl(Calendar.SECOND);
    public final static CalendarEval WEEKYEAR = new CalendarEvalImpl(Calendar.WEEK_OF_YEAR);
    public final static CalendarEval YEAR = new CalendarEvalImpl(Calendar.YEAR);

    private static class CalendarEvalImpl implements CalendarEval {
        private final int field;

        public CalendarEvalImpl(int field) {
            this.field = field;
        }

        public Object evaluateInternal(Calendar cal) {
            return cal.get(field);
        }

        public CodegenExpression codegen(CodegenExpression cal) {
            return exprDotMethod(cal, "get", constant(field));
        }
    }
}
