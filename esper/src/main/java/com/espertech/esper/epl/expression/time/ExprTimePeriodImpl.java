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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Expression representing a time period.
 * <p>
 * Child nodes to this expression carry the actual parts and must return a numeric value.
 */
public class ExprTimePeriodImpl extends ExprNodeBase implements ExprTimePeriod, ExprEvaluator {
    private final TimeZone timeZone;
    private final boolean hasYear;
    private final boolean hasMonth;
    private final boolean hasWeek;
    private final boolean hasDay;
    private final boolean hasHour;
    private final boolean hasMinute;
    private final boolean hasSecond;
    private final boolean hasMillisecond;
    private final boolean hasMicrosecond;
    private boolean hasVariable;
    private transient ExprEvaluator[] evaluators;
    private transient TimePeriodAdder[] adders;
    private final TimeAbacus timeAbacus;
    private static final long serialVersionUID = -7229827032500659319L;

    public ExprTimePeriodImpl(TimeZone timeZone, boolean hasYear, boolean hasMonth, boolean hasWeek, boolean hasDay, boolean hasHour, boolean hasMinute, boolean hasSecond, boolean hasMillisecond, boolean hasMicrosecond, TimeAbacus timeAbacus) {
        this.timeZone = timeZone;
        this.hasYear = hasYear;
        this.hasMonth = hasMonth;
        this.hasWeek = hasWeek;
        this.hasDay = hasDay;
        this.hasHour = hasHour;
        this.hasMinute = hasMinute;
        this.hasSecond = hasSecond;
        this.hasMillisecond = hasMillisecond;
        this.hasMicrosecond = hasMicrosecond;
        this.timeAbacus = timeAbacus;
    }

    public ExprTimePeriodEvalDeltaConst constEvaluator(ExprEvaluatorContext context) {
        if (!hasMonth && !hasYear) {
            double seconds = evaluateAsSeconds(null, true, context);
            long msec = timeAbacus.deltaForSecondsDouble(seconds);
            return new ExprTimePeriodEvalDeltaConstGivenDelta(msec);
        } else {
            int[] values = new int[adders.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = ((Number) evaluators[i].evaluate(null, true, context)).intValue();
            }
            return new ExprTimePeriodEvalDeltaConstGivenCalAdd(adders, values, timeZone, timeAbacus);
        }
    }

    public ExprTimePeriodEvalDeltaNonConst nonconstEvaluator() {
        if (!hasMonth && !hasYear) {
            return new ExprTimePeriodEvalDeltaNonConstMsec(this);
        } else {
            return new ExprTimePeriodEvalDeltaNonConstCalAdd(timeZone, this);
        }
    }

    public TimeAbacus getTimeAbacus() {
        return timeAbacus;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw new IllegalStateException("Time-Period expression must be evaluated via any of " + ExprTimePeriod.class.getSimpleName() + " interface methods");
    }

    protected TimePeriodAdder[] getAdders() {
        return adders;
    }

    public ExprEvaluator[] getEvaluators() {
        return evaluators;
    }

    /**
     * Indicator whether the time period has a day part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasDay() {
        return hasDay;
    }

    /**
     * Indicator whether the time period has a hour part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasHour() {
        return hasHour;
    }

    /**
     * Indicator whether the time period has a minute part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasMinute() {
        return hasMinute;
    }

    /**
     * Indicator whether the time period has a second part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasSecond() {
        return hasSecond;
    }

    /**
     * Indicator whether the time period has a millisecond part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasMillisecond() {
        return hasMillisecond;
    }

    public boolean isHasMicrosecond() {
        return hasMicrosecond;
    }

    /**
     * Indicator whether the time period has a year part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasYear() {
        return hasYear;
    }

    /**
     * Indicator whether the time period has a month part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasMonth() {
        return hasMonth;
    }

    /**
     * Indicator whether the time period has a week part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasWeek() {
        return hasWeek;
    }

    /**
     * Indicator whether the time period has a variable in any of the child expressions.
     *
     * @return true for variable present, false for not present
     */
    public boolean hasVariable() {
        return hasVariable;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());
        for (ExprNode childNode : this.getChildNodes()) {
            validate(childNode);
        }

        ArrayDeque<TimePeriodAdder> list = new ArrayDeque<TimePeriodAdder>();
        if (hasYear) {
            list.add(new TimePeriodAdderYear());
        }
        if (hasMonth) {
            list.add(new TimePeriodAdderMonth());
        }
        if (hasWeek) {
            list.add(new TimePeriodAdderWeek());
        }
        if (hasDay) {
            list.add(new TimePeriodAdderDay());
        }
        if (hasHour) {
            list.add(new TimePeriodAdderHour());
        }
        if (hasMinute) {
            list.add(new TimePeriodAdderMinute());
        }
        if (hasSecond) {
            list.add(new TimePeriodAdderSecond());
        }
        if (hasMillisecond) {
            list.add(new TimePeriodAdderMSec());
        }
        if (hasMicrosecond) {
            list.add(new TimePeriodAdderUSec());
        }
        adders = list.toArray(new TimePeriodAdder[list.size()]);
        return null;
    }

    private void validate(ExprNode expression) throws ExprValidationException {
        if (expression == null) {
            return;
        }
        Class returnType = expression.getExprEvaluator().getType();
        if (!JavaClassHelper.isNumeric(returnType)) {
            throw new ExprValidationException("Time period expression requires a numeric parameter type");
        }
        if ((hasMonth || hasYear) && (JavaClassHelper.getBoxedType(returnType) != Integer.class)) {
            throw new ExprValidationException("Time period expressions with month or year component require integer values, received a " + returnType.getSimpleName() + " value");
        }
        if (expression instanceof ExprVariableNode) {
            hasVariable = true;
        }
    }

    public double evaluateAsSeconds(EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTimePeriod(this);
        }
        double seconds = 0;
        for (int i = 0; i < adders.length; i++) {
            Double result = eval(evaluators[i], eventsPerStream, newData, context);
            if (result == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTimePeriod(null);
                }
                throw new EPException("Failed to evaluate time period, received a null value for '" + ExprNodeUtility.toExpressionStringMinPrecedenceSafe(this) + "'");
            }
            seconds += adders[i].compute(result);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprTimePeriod(seconds);
        }
        return seconds;
    }

    private Double eval(ExprEvaluator expr, EventBean[] events, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = expr.evaluate(events, isNewData, exprEvaluatorContext);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof BigInteger) {
            return ((Number) value).doubleValue();
        }
        return ((Number) value).doubleValue();
    }

    public TimePeriod evaluateGetTimePeriod(EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        int exprCtr = 0;

        Integer year = null;
        if (hasYear) {
            year = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer month = null;
        if (hasMonth) {
            month = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer week = null;
        if (hasWeek) {
            week = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer day = null;
        if (hasDay) {
            day = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer hours = null;
        if (hasHour) {
            hours = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer minutes = null;
        if (hasMinute) {
            minutes = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer seconds = null;
        if (hasSecond) {
            seconds = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer milliseconds = null;
        if (hasMillisecond) {
            milliseconds = getInt(evaluators[exprCtr++].evaluate(eventsPerStream, newData, context));
        }

        Integer microseconds = null;
        if (hasMicrosecond) {
            microseconds = getInt(evaluators[exprCtr].evaluate(eventsPerStream, newData, context));
        }
        return new TimePeriod(year, month, week, day, hours, minutes, seconds, milliseconds, microseconds);
    }

    private Integer getInt(Object evaluated) {
        if (evaluated == null) {
            return null;
        }
        return ((Number) evaluated).intValue();
    }

    public static interface TimePeriodAdder {
        public double compute(Double value);

        public void add(Calendar cal, int value);

        boolean isMicroseconds();
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
    }

    public static class TimePeriodAdderMonth implements TimePeriodAdder {
        private static final double MULTIPLIER = 30 * 24 * 60 * 60;

        public double compute(Double value) {
            return value * MULTIPLIER;
        }

        public void add(Calendar cal, int value) {
            cal.add(Calendar.MONTH, value);
        }

        public boolean isMicroseconds() {
            return false;
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
    }

    public Class getType() {
        return Double.class;
    }

    public boolean isConstantResult() {
        for (ExprNode child : getChildNodes()) {
            if (!child.isConstantResult()) {
                return false;
            }
        }
        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        int exprCtr = 0;
        String delimiter = "";
        if (hasYear) {
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" years");
            delimiter = " ";
        }
        if (hasMonth) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" months");
            delimiter = " ";
        }
        if (hasWeek) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" weeks");
            delimiter = " ";
        }
        if (hasDay) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" days");
            delimiter = " ";
        }
        if (hasHour) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" hours");
            delimiter = " ";
        }
        if (hasMinute) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" minutes");
            delimiter = " ";
        }
        if (hasSecond) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" seconds");
            delimiter = " ";
        }
        if (hasMillisecond) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" milliseconds");
            delimiter = " ";
        }
        if (hasMicrosecond) {
            writer.append(delimiter);
            getChildNodes()[exprCtr].toEPL(writer, getPrecedence());
            writer.append(" microseconds");
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprTimePeriodImpl)) {
            return false;
        }
        ExprTimePeriodImpl other = (ExprTimePeriodImpl) node;

        if (hasYear != other.hasYear) {
            return false;
        }
        if (hasMonth != other.hasMonth) {
            return false;
        }
        if (hasWeek != other.hasWeek) {
            return false;
        }
        if (hasDay != other.hasDay) {
            return false;
        }
        if (hasHour != other.hasHour) {
            return false;
        }
        if (hasMinute != other.hasMinute) {
            return false;
        }
        if (hasSecond != other.hasSecond) {
            return false;
        }
        if (hasMillisecond != other.hasMillisecond) {
            return false;
        }
        return hasMicrosecond == other.hasMicrosecond;
    }
}
