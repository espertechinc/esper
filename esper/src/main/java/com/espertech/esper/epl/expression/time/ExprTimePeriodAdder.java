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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprTimePeriodAdder {
    public interface TimePeriodAdder {
        public double compute(Double value);

        public void add(Calendar cal, int value);

        boolean isMicroseconds();

        CodegenExpression computeCodegen(CodegenExpression doubleValue);
        CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value);
    }

    public static class TimePeriodAdderYear implements TimePeriodAdder {
        private static final double MULTIPLIER = 365 * 24 * 60 * 60;

        public double compute(Double value) {
            return value * MULTIPLIER;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.YEAR, value);
        }

        public boolean isMicroseconds() {
            return false;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return computeCodegenTimesMultiplier(doubleValue, MULTIPLIER);
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return addCodegenCalendar(cal, value, Calendar.YEAR);
        }
    }

    public static class TimePeriodAdderMonth implements TimePeriodAdder {
        private static final double MULTIPLIER = 30 * 24 * 60 * 60;

        public double compute(Double value) {
            return value * MULTIPLIER;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.MONTH, value);
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return addCodegenCalendar(cal, value, Calendar.MONTH);
        }

        public boolean isMicroseconds() {
            return false;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return computeCodegenTimesMultiplier(doubleValue, MULTIPLIER);
        }
    }

    public static class TimePeriodAdderWeek implements TimePeriodAdder {
        private static final double MULTIPLIER = 7 * 24 * 60 * 60;

        public double compute(Double value) {
            return value * MULTIPLIER;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.WEEK_OF_YEAR, value);
        }

        public boolean isMicroseconds() {
            return false;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return computeCodegenTimesMultiplier(doubleValue, MULTIPLIER);
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return addCodegenCalendar(cal, value, Calendar.WEEK_OF_YEAR);
        }
    }

    public static class TimePeriodAdderDay implements TimePeriodAdder {
        private static final double MULTIPLIER = 24 * 60 * 60;

        public double compute(Double value) {
            return value * MULTIPLIER;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.DAY_OF_MONTH, value);
        }

        public boolean isMicroseconds() {
            return false;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return computeCodegenTimesMultiplier(doubleValue, MULTIPLIER);
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return addCodegenCalendar(cal, value, Calendar.DAY_OF_MONTH);
        }
    }

    public static class TimePeriodAdderHour implements TimePeriodAdder {
        private static final double MULTIPLIER = 60 * 60;

        public double compute(Double value) {
            return value * MULTIPLIER;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.HOUR_OF_DAY, value);
        }

        public boolean isMicroseconds() {
            return false;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return computeCodegenTimesMultiplier(doubleValue, MULTIPLIER);
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return addCodegenCalendar(cal, value, Calendar.HOUR_OF_DAY);
        }
    }

    public static class TimePeriodAdderMinute implements TimePeriodAdder {
        private static final double MULTIPLIER = 60;

        public double compute(Double value) {
            return value * MULTIPLIER;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.MINUTE, value);
        }

        public boolean isMicroseconds() {
            return false;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return computeCodegenTimesMultiplier(doubleValue, MULTIPLIER);
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return addCodegenCalendar(cal, value, Calendar.MINUTE);
        }
    }

    public static class TimePeriodAdderSecond implements TimePeriodAdder {
        public double compute(Double value) {
            return value;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.SECOND, value);
        }

        public boolean isMicroseconds() {
            return false;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return doubleValue;
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return addCodegenCalendar(cal, value, Calendar.SECOND);
        }
    }

    public static class TimePeriodAdderMSec implements TimePeriodAdder {
        public double compute(Double value) {
            return value / 1000d;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.MILLISECOND, value);
        }

        public boolean isMicroseconds() {
            return false;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return op(doubleValue, "/", constant(1000d));
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return addCodegenCalendar(cal, value, Calendar.MILLISECOND);
        }
    }

    public static class TimePeriodAdderUSec implements TimePeriodAdder {
        public double compute(Double value) {
            return value / 1000000d;
        }

        public void add(Calendar cal, int value) {
            // no action : calendar does not add microseconds
        }

        public boolean isMicroseconds() {
            return true;
        }

        public CodegenExpression computeCodegen(CodegenExpression doubleValue) {
            return op(doubleValue, "/", constant(1000000d));
        }

        public CodegenExpression addCodegen(CodegenExpression cal, CodegenExpression value) {
            return noop();
        }
    }

    private static CodegenExpression computeCodegenTimesMultiplier(CodegenExpression doubleValue, double multiplier) {
        return op(doubleValue, "*", constant(multiplier));
    }

    private static CodegenExpression addCodegenCalendar(CodegenExpression cal, CodegenExpression value, int unit) {
        return exprDotMethod(cal, "add", constant(unit), value);
    }
}
