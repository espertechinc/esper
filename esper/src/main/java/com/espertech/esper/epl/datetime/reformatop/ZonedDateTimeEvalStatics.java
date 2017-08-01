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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.enumValue;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class ZonedDateTimeEvalStatics {

    public final static ZonedDateTimeEval MINUTE_OF_HOUR = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.getMinute();
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "getMinute");
        }
    };

    public final static ZonedDateTimeEval MONTH_OF_YEAR = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.getMonthValue();
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "getMonthValue");
        }
    };

    public final static ZonedDateTimeEval DAY_OF_MONTH = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.getDayOfMonth();
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "getDayOfMonth");
        }
    };

    public final static ZonedDateTimeEval DAY_OF_WEEK = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.getDayOfWeek();
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "getDayOfWeek");
        }
    };

    public final static ZonedDateTimeEval DAY_OF_YEAR = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.getDayOfYear();
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "getDayOfYear");
        }
    };

    public final static ZonedDateTimeEval ERA = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.get(ChronoField.ERA);
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "get", enumValue(ChronoField.class, "ERA"));
        }
    };

    public final static ZonedDateTimeEval HOUR_OF_DAY = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.getHour();
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "getHour");
        }
    };

    public final static ZonedDateTimeEval MILLIS_OF_SECOND = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.get(ChronoField.MILLI_OF_SECOND);
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "get", enumValue(ChronoField.class, "MILLI_OF_SECOND"));
        }
    };

    public final static ZonedDateTimeEval SECOND_OF_MINUTE = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.getSecond();
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "getSecond");
        }
    };

    public final static ZonedDateTimeEval WEEKYEAR = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "get", enumValue(ChronoField.class, "ALIGNED_WEEK_OF_YEAR"));
        }
    };

    public final static ZonedDateTimeEval YEAR = new ZonedDateTimeEval() {
        public Object evaluateInternal(ZonedDateTime ldt) {
            return ldt.getYear();
        }

        public CodegenExpression codegen(CodegenExpression inner) {
            return exprDotMethod(inner, "getYear");
        }
    };
}
