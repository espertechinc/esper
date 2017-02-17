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

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * An output limit clause defines how to limit output of statements and consists of
 * a selector specifiying which events to select to output, a frequency and a unit.
 */
public class OutputLimitClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private OutputLimitSelector selector;
    private Double frequency;
    private String frequencyVariable;
    private OutputLimitUnit unit;
    private Expression whenExpression;
    private List<Assignment> thenAssignments;
    private Expression[] crontabAtParameters;
    private Expression timePeriodExpression;
    private Expression afterTimePeriodExpression;
    private Integer afterNumberOfEvents;
    private boolean andAfterTerminate;
    private Expression andAfterTerminateAndExpr;
    private List<Assignment> andAfterTerminateThenAssignments;

    /**
     * Ctor.
     */
    public OutputLimitClause() {
    }

    /**
     * Ctor.
     *
     * @param selector selector
     * @param unit     unit
     */
    public OutputLimitClause(OutputLimitSelector selector, OutputLimitUnit unit) {
        this.selector = selector;
        this.unit = unit;
    }

    /**
     * Creates an output limit clause.
     *
     * @param timePeriodExpression a frequency to output at
     * @return clause
     */
    public static OutputLimitClause create(TimePeriodExpression timePeriodExpression) {
        return new OutputLimitClause(OutputLimitSelector.DEFAULT, timePeriodExpression);
    }

    /**
     * Create with after-only time period.
     *
     * @param afterTimePeriodExpression time period
     * @return clause
     */
    public static OutputLimitClause createAfter(TimePeriodExpression afterTimePeriodExpression) {
        return new OutputLimitClause(OutputLimitSelector.DEFAULT, OutputLimitUnit.AFTER, afterTimePeriodExpression, null);
    }

    /**
     * Create with after-only and number of events.
     *
     * @param afterNumEvents num events
     * @return clause
     */
    public static OutputLimitClause createAfter(int afterNumEvents) {
        return new OutputLimitClause(OutputLimitSelector.DEFAULT, OutputLimitUnit.AFTER, null, afterNumEvents);
    }

    /**
     * Creates an output limit clause.
     *
     * @param selector             is the events to select
     * @param timePeriodExpression a frequency to output at
     * @return clause
     */
    public static OutputLimitClause create(OutputLimitSelector selector, TimePeriodExpression timePeriodExpression) {
        return new OutputLimitClause(selector, timePeriodExpression);
    }

    /**
     * Creates an output limit clause.
     *
     * @param selector  is the events to select
     * @param frequency a frequency to output at
     * @return clause
     */
    public static OutputLimitClause create(OutputLimitSelector selector, double frequency) {
        return new OutputLimitClause(selector, frequency);
    }

    /**
     * Creates an output limit clause.
     *
     * @param selector          is the events to select
     * @param frequencyVariable is the variable providing the output limit frequency
     * @return clause
     */
    public static OutputLimitClause create(OutputLimitSelector selector, String frequencyVariable) {
        return new OutputLimitClause(selector, frequencyVariable);
    }

    /**
     * Creates an output limit clause.
     *
     * @param frequency a frequency to output at
     * @return clause
     */
    public static OutputLimitClause create(double frequency) {
        return new OutputLimitClause(OutputLimitSelector.DEFAULT, frequency);
    }

    /**
     * Creates an output limit clause.
     *
     * @param frequencyVariable is the variable name providing output rate frequency values
     * @return clause
     */
    public static OutputLimitClause create(String frequencyVariable) {
        return new OutputLimitClause(OutputLimitSelector.DEFAULT, frequencyVariable);
    }

    /**
     * Creates an output limit clause with a when-expression and optional then-assignment expressions to be added.
     *
     * @param whenExpression the expression that returns true to trigger output
     * @return clause
     */
    public static OutputLimitClause create(Expression whenExpression) {
        return new OutputLimitClause(OutputLimitSelector.DEFAULT, whenExpression, new ArrayList<Assignment>());
    }

    /**
     * Creates an output limit clause with a crontab 'at' schedule parameters, see {@link com.espertech.esper.type.FrequencyParameter} and related.
     *
     * @param scheduleParameters the crontab schedule parameters
     * @return clause
     */
    public static OutputLimitClause createSchedule(Expression[] scheduleParameters) {
        return new OutputLimitClause(OutputLimitSelector.DEFAULT, scheduleParameters);
    }

    /**
     * Ctor.
     *
     * @param selector  is the events to select
     * @param frequency a frequency to output at
     */
    public OutputLimitClause(OutputLimitSelector selector, Double frequency) {
        this.selector = selector;
        this.frequency = frequency;
        this.unit = OutputLimitUnit.EVENTS;
    }

    /**
     * Ctor.
     *
     * @param selector             is the events to select
     * @param timePeriodExpression the unit for the frequency
     */
    public OutputLimitClause(OutputLimitSelector selector, TimePeriodExpression timePeriodExpression) {
        this.selector = selector;
        this.timePeriodExpression = timePeriodExpression;
        this.unit = OutputLimitUnit.TIME_PERIOD;
    }

    /**
     * Ctor.
     *
     * @param afterTimePeriodExpression timer period for after.
     */
    public OutputLimitClause(TimePeriodExpression afterTimePeriodExpression) {
        this.unit = OutputLimitUnit.AFTER;
        this.afterTimePeriodExpression = afterTimePeriodExpression;
    }

    /**
     * Ctor.
     *
     * @param selector          is the events to select
     * @param frequencyVariable is the variable name providing output rate frequency values
     */
    public OutputLimitClause(OutputLimitSelector selector, String frequencyVariable) {
        this.selector = selector;
        this.frequencyVariable = frequencyVariable;
        this.unit = OutputLimitUnit.EVENTS;
    }

    /**
     * Ctor.
     *
     * @param selector          is the events to select
     * @param frequency         a frequency to output at
     * @param unit              the unit for the frequency
     * @param frequencyVariable is the variable name providing output rate frequency values
     */
    public OutputLimitClause(OutputLimitSelector selector, Double frequency, String frequencyVariable, OutputLimitUnit unit) {
        this.selector = selector;
        this.frequency = frequency;
        this.frequencyVariable = frequencyVariable;
        this.unit = unit;
    }

    /**
     * Ctor.
     *
     * @param selector            is the events to select
     * @param unit                the unit of selection
     * @param afterTimePeriod     after-keyword time period
     * @param afterNumberOfEvents after-keyword number of events
     */
    public OutputLimitClause(OutputLimitSelector selector, OutputLimitUnit unit, TimePeriodExpression afterTimePeriod, Integer afterNumberOfEvents) {
        this.selector = selector;
        this.unit = unit;
        this.afterTimePeriodExpression = afterTimePeriod;
        this.afterNumberOfEvents = afterNumberOfEvents;
    }

    /**
     * Ctor.
     *
     * @param selector            is the events to select
     * @param crontabAtParameters the crontab schedule parameters
     */
    public OutputLimitClause(OutputLimitSelector selector, Expression[] crontabAtParameters) {
        this.selector = selector;
        this.crontabAtParameters = crontabAtParameters;
        this.unit = OutputLimitUnit.CRONTAB_EXPRESSION;
    }

    /**
     * Ctor.
     *
     * @param selector        is the events to select
     * @param whenExpression  the boolean expression to evaluate to control output
     * @param thenAssignments the variable assignments, optional or an empty list
     */
    public OutputLimitClause(OutputLimitSelector selector, Expression whenExpression, List<Assignment> thenAssignments) {
        this.selector = selector;
        this.whenExpression = whenExpression;
        this.thenAssignments = thenAssignments;
        this.unit = OutputLimitUnit.WHEN_EXPRESSION;
    }

    /**
     * Returns the selector indicating the events to output.
     *
     * @return selector
     */
    public OutputLimitSelector getSelector() {
        return selector;
    }

    /**
     * Sets the selector indicating the events to output.
     *
     * @param selector to set
     */
    public void setSelector(OutputLimitSelector selector) {
        this.selector = selector;
    }

    /**
     * Returns output frequency.
     *
     * @return frequency of output
     */
    public Double getFrequency() {
        return frequency;
    }

    /**
     * Returns the unit the frequency is in.
     *
     * @return unit for the frequency.
     */
    public OutputLimitUnit getUnit() {
        return unit;
    }

    /**
     * Sets the unit the frequency is in.
     *
     * @param unit is the unit for the frequency
     */
    public void setUnit(OutputLimitUnit unit) {
        this.unit = unit;
    }

    /**
     * Returns the variable name of the variable providing output rate frequency values, or null if the frequency is a fixed value.
     *
     * @return variable name or null if no variable is used
     */
    public String getFrequencyVariable() {
        return frequencyVariable;
    }

    /**
     * Sets the variable name of the variable providing output rate frequency values, or null if the frequency is a fixed value.
     *
     * @param frequencyVariable variable name or null if no variable is used
     */
    public void setFrequencyVariable(String frequencyVariable) {
        this.frequencyVariable = frequencyVariable;
    }

    /**
     * Returns the expression that controls output for use with the when-keyword.
     *
     * @return expression should be boolean result
     */
    public Expression getWhenExpression() {
        return whenExpression;
    }

    /**
     * Returns the time period, or null if none provided.
     *
     * @return time period
     */
    public Expression getTimePeriodExpression() {
        return timePeriodExpression;
    }

    /**
     * Returns the list of optional then-keyword variable assignments, if any
     *
     * @return list of variable assignments or null if none
     */
    public List<Assignment> getThenAssignments() {
        return thenAssignments;
    }

    /**
     * Adds a then-keyword variable assigment for use with the when-keyword.
     *
     * @param assignmentExpression expression to calculate new value
     * @return clause
     */
    public OutputLimitClause addThenAssignment(Expression assignmentExpression) {
        thenAssignments.add(new Assignment(assignmentExpression));
        return this;
    }

    /**
     * Returns the crontab parameters, or null if not using crontab-like schedule.
     *
     * @return parameters
     */
    public Expression[] getCrontabAtParameters() {
        return crontabAtParameters;
    }

    /**
     * Returns true for output upon termination of a context partition
     *
     * @return indicator
     */
    public boolean isAndAfterTerminate() {
        return andAfterTerminate;
    }

    /**
     * Set true for output upon termination of a context partition
     *
     * @param andAfterTerminate indicator
     */
    public void setAndAfterTerminate(boolean andAfterTerminate) {
        this.andAfterTerminate = andAfterTerminate;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        if (afterTimePeriodExpression != null) {
            writer.write("after ");
            afterTimePeriodExpression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.write(" ");
        } else if ((afterNumberOfEvents != null) && (afterNumberOfEvents != 0)) {
            writer.write("after ");
            writer.write(Integer.toString(afterNumberOfEvents));
            writer.write(" events ");
        }

        if (selector != OutputLimitSelector.DEFAULT) {
            writer.write(selector.getText());
            writer.write(" ");
        }
        if (unit == OutputLimitUnit.WHEN_EXPRESSION) {
            writer.write("when ");
            whenExpression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);

            if ((thenAssignments != null) && (thenAssignments.size() > 0)) {
                writeThenAssignments(writer, thenAssignments);
            }
        } else if (unit == OutputLimitUnit.CRONTAB_EXPRESSION) {
            writer.write("at (");
            String delimiter = "";
            for (int i = 0; i < crontabAtParameters.length; i++) {
                writer.write(delimiter);
                crontabAtParameters[i].toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                delimiter = ", ";
            }
            writer.write(")");
        } else if (unit == OutputLimitUnit.TIME_PERIOD && timePeriodExpression != null) {
            writer.write("every ");
            timePeriodExpression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        } else if (unit == OutputLimitUnit.AFTER) {
            // no action required
        } else if (unit == OutputLimitUnit.CONTEXT_PARTITION_TERM) {
            writer.write("when terminated");
            outputAndAfter(writer);
        } else {
            writer.write("every ");
            if (frequencyVariable == null) {
                writer.write(Integer.toString(frequency.intValue()));
            } else {
                writer.write(frequencyVariable);
            }
            writer.write(" events");
        }

        if (andAfterTerminate) {
            writer.write(" and when terminated");
            outputAndAfter(writer);
        }
    }

    /**
     * Returns the after-keyword time period.
     *
     * @return after-keyword time period
     */
    public Expression getAfterTimePeriodExpression() {
        return afterTimePeriodExpression;
    }

    /**
     * Sets the after-keyword time period.
     *
     * @param afterTimePeriodExpression after-keyword time period
     */
    public void setAfterTimePeriodExpression(Expression afterTimePeriodExpression) {
        this.afterTimePeriodExpression = afterTimePeriodExpression;
    }

    /**
     * Sets the after-keyword time period.
     *
     * @param afterTimePeriodExpression after-keyword time period
     * @return clause
     */
    public OutputLimitClause afterTimePeriodExpression(TimePeriodExpression afterTimePeriodExpression) {
        this.afterTimePeriodExpression = afterTimePeriodExpression;
        return this;
    }

    /**
     * Returns the after-keyword number of events, or null if undefined.
     *
     * @return num events for after-keyword
     */
    public Integer getAfterNumberOfEvents() {
        return afterNumberOfEvents;
    }

    /**
     * Set frequency.
     *
     * @param frequency to set
     */
    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }

    /**
     * Set when.
     *
     * @param whenExpression to set
     */
    public void setWhenExpression(Expression whenExpression) {
        this.whenExpression = whenExpression;
    }

    /**
     * Set then.
     *
     * @param thenAssignments to set
     */
    public void setThenAssignments(List<Assignment> thenAssignments) {
        this.thenAssignments = thenAssignments;
    }

    /**
     * Crontab.
     *
     * @param crontabAtParameters to set
     */
    public void setCrontabAtParameters(Expression[] crontabAtParameters) {
        this.crontabAtParameters = crontabAtParameters;
    }

    /**
     * Crontab
     *
     * @param timePeriodExpression to set
     */
    public void setTimePeriodExpression(Expression timePeriodExpression) {
        this.timePeriodExpression = timePeriodExpression;
    }

    /**
     * Sets the after-keyword number of events, or null if undefined.
     *
     * @param afterNumberOfEvents set num events for after-keyword
     */
    public void setAfterNumberOfEvents(Integer afterNumberOfEvents) {
        this.afterNumberOfEvents = afterNumberOfEvents;
    }

    /**
     * Sets the after-keyword number of events, or null if undefined.
     *
     * @param afterNumberOfEvents set num events for after-keyword
     * @return clause
     */
    public OutputLimitClause afterNumberOfEvents(Integer afterNumberOfEvents) {
        this.afterNumberOfEvents = afterNumberOfEvents;
        return this;
    }

    /**
     * Returns the optional expression evaluated when a context partition terminates before triggering output.
     *
     * @return expression
     */
    public Expression getAndAfterTerminateAndExpr() {
        return andAfterTerminateAndExpr;
    }

    /**
     * Sets an optional expression evaluated when a context partition terminates before triggering output.
     *
     * @param andAfterTerminateAndExpr expression
     */
    public void setAndAfterTerminateAndExpr(Expression andAfterTerminateAndExpr) {
        this.andAfterTerminateAndExpr = andAfterTerminateAndExpr;
    }

    /**
     * Returns the set-assignments to execute when a context partition terminates.
     *
     * @return set-assignments
     */
    public List<Assignment> getAndAfterTerminateThenAssignments() {
        return andAfterTerminateThenAssignments;
    }

    /**
     * Sets the set-assignments to execute when a context partition terminates.
     *
     * @param andAfterTerminateThenAssignments set-assignments
     */
    public void setAndAfterTerminateThenAssignments(List<Assignment> andAfterTerminateThenAssignments) {
        this.andAfterTerminateThenAssignments = andAfterTerminateThenAssignments;
    }

    private void writeThenAssignments(StringWriter writer, List<Assignment> thenAssignments) {
        writer.write(" then ");
        UpdateClause.renderEPLAssignments(writer, thenAssignments);
    }

    private void outputAndAfter(StringWriter writer) {
        if (andAfterTerminateAndExpr != null) {
            writer.write(" and ");
            andAfterTerminateAndExpr.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
        if (andAfterTerminateThenAssignments != null && andAfterTerminateThenAssignments.size() > 0) {
            writeThenAssignments(writer, andAfterTerminateThenAssignments);
        }
    }
}
