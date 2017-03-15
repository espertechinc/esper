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
package com.espertech.esper.client.soda;

import java.io.StringWriter;

/**
 * Represent an expression
 */
public class TimePeriodExpression extends ExpressionBase {
    private boolean hasYears;
    private boolean hasMonths;
    private boolean hasWeeks;
    private boolean hasDays;
    private boolean hasHours;
    private boolean hasMinutes;
    private boolean hasSeconds;
    private boolean hasMilliseconds;
    private boolean hasMicroseconds;
    private static final long serialVersionUID = 1386645838943804276L;

    /**
     * Ctor.
     */
    public TimePeriodExpression() {
    }

    /**
     * Ctor.
     *
     * @param hasYears        flag to indicate that a year-part expression exists
     * @param hasMonths       flag to indicate that a month-part expression exists
     * @param hasWeeks        flag to indicate that a week-part expression exists
     * @param hasDays         flag to indicate that a day-part expression exists
     * @param hasHours        flag to indicate that a hour-part expression exists
     * @param hasMinutes      flag to indicate that a minute-part expression exists
     * @param hasSeconds      flag to indicate that a seconds-part expression exists
     * @param hasMilliseconds flag to indicate that a millisec-part expression exists
     * @param hasMicroseconds flag to indicate that a microsecond-part expression exists
     */
    public TimePeriodExpression(boolean hasYears, boolean hasMonths, boolean hasWeeks, boolean hasDays, boolean hasHours, boolean hasMinutes, boolean hasSeconds, boolean hasMilliseconds, boolean hasMicroseconds) {
        this.hasYears = hasYears;
        this.hasMonths = hasMonths;
        this.hasWeeks = hasWeeks;
        this.hasDays = hasDays;
        this.hasHours = hasHours;
        this.hasMinutes = hasMinutes;
        this.hasSeconds = hasSeconds;
        this.hasMilliseconds = hasMilliseconds;
        this.hasMicroseconds = hasMicroseconds;
    }

    /**
     * Ctor.
     *
     * @param yearsExpr        expression returning years value, or null if no such part
     * @param monthsExpr       expression returning months value, or null if no such part
     * @param weeksExpr        expression returning weeks value, or null if no such part
     * @param daysExpr         expression returning days value, or null if no such part
     * @param hoursExpr        expression returning hours value, or null if no such part
     * @param minutesExpr      expression returning minutes value, or null if no such part
     * @param secondsExpr      expression returning seconds value, or null if no such part
     * @param millisecondsExpr expression returning millisec value, or null if no such part
     * @param microsecondsExpr expression returning microsecond value, or null if no such part
     */
    public TimePeriodExpression(Expression yearsExpr, Expression monthsExpr, Expression weeksExpr, Expression daysExpr, Expression hoursExpr, Expression minutesExpr, Expression secondsExpr, Expression millisecondsExpr, Expression microsecondsExpr) {
        addExpr(yearsExpr, monthsExpr, weeksExpr, daysExpr, hoursExpr, minutesExpr, secondsExpr, millisecondsExpr, microsecondsExpr);
    }

    /**
     * Ctor.
     *
     * @param hasYears        flag to indicate that a year-part expression exists
     * @param hasMonths       flag to indicate that a month-part expression exists
     * @param hasWeeks        flag to indicate that a week-part expression exists
     * @param hasDays         flag to indicate that a day-part expression exists
     * @param hasHours        flag to indicate that a hour-part expression exists
     * @param hasMinutes      flag to indicate that a minute-part expression exists
     * @param hasSeconds      flag to indicate that a seconds-part expression exists
     * @param hasMilliseconds flag to indicate that a millisec-part expression exists
     */
    public TimePeriodExpression(boolean hasYears, boolean hasMonths, boolean hasWeeks, boolean hasDays, boolean hasHours, boolean hasMinutes, boolean hasSeconds, boolean hasMilliseconds) {
        this(hasYears, hasMonths, hasWeeks, hasDays, hasHours, hasMinutes, hasSeconds, hasMilliseconds, false);
    }

    /**
     * Ctor.
     *
     * @param hasDays         flag to indicate that a day-part expression exists
     * @param hasHours        flag to indicate that a hour-part expression exists
     * @param hasMinutes      flag to indicate that a minute-part expression exists
     * @param hasSeconds      flag to indicate that a seconds-part expression exists
     * @param hasMilliseconds flag to indicate that a millisec-part expression exists
     */
    public TimePeriodExpression(boolean hasDays, boolean hasHours, boolean hasMinutes, boolean hasSeconds, boolean hasMilliseconds) {
        this(false, false, false, hasDays, hasHours, hasMinutes, hasSeconds, hasMilliseconds, false);
    }

    /**
     * Ctor.
     *
     * @param yearsExpr        expression returning years value, or null if no such part
     * @param monthsExpr       expression returning months value, or null if no such part
     * @param weeksExpr        expression returning weeks value, or null if no such part
     * @param daysExpr         expression returning days value, or null if no such part
     * @param hoursExpr        expression returning hours value, or null if no such part
     * @param minutesExpr      expression returning minutes value, or null if no such part
     * @param secondsExpr      expression returning seconds value, or null if no such part
     * @param millisecondsExpr expression returning millisec value, or null if no such part
     */
    public TimePeriodExpression(Expression yearsExpr, Expression monthsExpr, Expression weeksExpr, Expression daysExpr, Expression hoursExpr, Expression minutesExpr, Expression secondsExpr, Expression millisecondsExpr) {
        this(yearsExpr, monthsExpr, weeksExpr, daysExpr, hoursExpr, minutesExpr, secondsExpr, millisecondsExpr, null);
    }

    /**
     * Ctor.
     *
     * @param daysExpr         expression returning days value, or null if no such part
     * @param hoursExpr        expression returning hours value, or null if no such part
     * @param minutesExpr      expression returning minutes value, or null if no such part
     * @param secondsExpr      expression returning seconds value, or null if no such part
     * @param millisecondsExpr expression returning millisec value, or null if no such part
     */
    public TimePeriodExpression(Expression daysExpr, Expression hoursExpr, Expression minutesExpr, Expression secondsExpr, Expression millisecondsExpr) {
        this(null, null, null, daysExpr, hoursExpr, minutesExpr, secondsExpr, millisecondsExpr, null);
    }

    private void addExpr(Expression yearsExpr, Expression monthExpr, Expression weeksExpr, Expression daysExpr, Expression hoursExpr, Expression minutesExpr, Expression secondsExpr, Expression millisecondsExpr, Expression microsecondsExpr) {
        if (yearsExpr != null) {
            hasYears = true;
            this.addChild(yearsExpr);
        }
        if (monthExpr != null) {
            hasMonths = true;
            this.addChild(monthExpr);
        }
        if (weeksExpr != null) {
            hasWeeks = true;
            this.addChild(weeksExpr);
        }
        if (daysExpr != null) {
            hasDays = true;
            this.addChild(daysExpr);
        }
        if (hoursExpr != null) {
            hasHours = true;
            this.addChild(hoursExpr);
        }
        if (minutesExpr != null) {
            hasMinutes = true;
            this.addChild(minutesExpr);
        }
        if (secondsExpr != null) {
            hasSeconds = true;
            this.addChild(secondsExpr);
        }
        if (millisecondsExpr != null) {
            hasMilliseconds = true;
            this.addChild(millisecondsExpr);
        }
        if (microsecondsExpr != null) {
            hasMicroseconds = true;
            this.addChild(microsecondsExpr);
        }
    }

    /**
     * Returns true if a subexpression exists that is a day-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasDays() {
        return hasDays;
    }

    /**
     * Set to true if a subexpression exists that is a day-part.
     *
     * @param hasDays for presence of part
     */
    public void setHasDays(boolean hasDays) {
        this.hasDays = hasDays;
    }

    /**
     * Returns true if a subexpression exists that is a hour-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasHours() {
        return hasHours;
    }

    /**
     * Set to true if a subexpression exists that is a hour-part.
     *
     * @param hasHours for presence of part
     */
    public void setHasHours(boolean hasHours) {
        this.hasHours = hasHours;
    }

    /**
     * Returns true if a subexpression exists that is a minutes-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasMinutes() {
        return hasMinutes;
    }

    /**
     * Set to true if a subexpression exists that is a minutes-part.
     *
     * @param hasMinutes for presence of part
     */
    public void setHasMinutes(boolean hasMinutes) {
        this.hasMinutes = hasMinutes;
    }

    /**
     * Returns true if a subexpression exists that is a seconds-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasSeconds() {
        return hasSeconds;
    }

    /**
     * Set to true if a subexpression exists that is a seconds-part.
     *
     * @param hasSeconds for presence of part
     */
    public void setHasSeconds(boolean hasSeconds) {
        this.hasSeconds = hasSeconds;
    }

    /**
     * Returns true if a subexpression exists that is a milliseconds-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasMilliseconds() {
        return hasMilliseconds;
    }

    /**
     * Set to true if a subexpression exists that is a msec-part.
     *
     * @param hasMilliseconds for presence of part
     */
    public void setHasMilliseconds(boolean hasMilliseconds) {
        this.hasMilliseconds = hasMilliseconds;
    }

    /**
     * Returns true if a subexpression exists that is a year-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasYears() {
        return hasYears;
    }

    /**
     * Set to true if a subexpression exists that is a year-part.
     *
     * @param hasYears for presence of part
     */
    public void setHasYears(boolean hasYears) {
        this.hasYears = hasYears;
    }

    /**
     * Returns true if a subexpression exists that is a month-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasMonths() {
        return hasMonths;
    }

    /**
     * Set to true if a subexpression exists that is a month-part.
     *
     * @param hasMonths for presence of part
     */
    public void setHasMonths(boolean hasMonths) {
        this.hasMonths = hasMonths;
    }

    /**
     * Returns true if a subexpression exists that is a weeks-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasWeeks() {
        return hasWeeks;
    }

    /**
     * Set to true if a subexpression exists that is a weeks-part.
     *
     * @param hasWeeks for presence of part
     */
    public void setHasWeeks(boolean hasWeeks) {
        this.hasWeeks = hasWeeks;
    }

    /**
     * Returns true if a subexpression exists that is a microsecond-part.
     *
     * @return indicator for presence of part
     */
    public boolean isHasMicroseconds() {
        return hasMicroseconds;
    }

    /**
     * Set to true if a subexpression exists that is a microsecond-part.
     *
     * @param hasMicroseconds indicator for presence of part
     */
    public void setHasMicroseconds(boolean hasMicroseconds) {
        this.hasMicroseconds = hasMicroseconds;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        int countExpr = 0;
        if (hasYears) {
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" years");
            delimiter = " ";
            countExpr++;
        }
        if (hasMonths) {
            writer.write(delimiter);
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" months");
            delimiter = " ";
            countExpr++;
        }
        if (hasWeeks) {
            writer.write(delimiter);
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" weeks");
            delimiter = " ";
            countExpr++;
        }
        if (hasDays) {
            writer.write(delimiter);
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" days");
            delimiter = " ";
            countExpr++;
        }
        if (hasHours) {
            writer.write(delimiter);
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" hours");
            delimiter = " ";
            countExpr++;
        }
        if (hasMinutes) {
            writer.write(delimiter);
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" minutes");
            delimiter = " ";
            countExpr++;
        }
        if (hasSeconds) {
            writer.write(delimiter);
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" seconds");
            delimiter = " ";
            countExpr++;
        }
        if (hasMilliseconds) {
            writer.write(delimiter);
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" milliseconds");
            delimiter = " ";
            countExpr++;
        }
        if (hasMicroseconds) {
            writer.write(delimiter);
            this.getChildren().get(countExpr).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.append(" microseconds");
        }
    }
}
