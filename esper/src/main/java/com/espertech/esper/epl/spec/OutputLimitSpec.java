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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;

import java.io.Serializable;
import java.util.List;

/**
 * Spec for defining an output rate
 */
public class OutputLimitSpec implements Serializable {
    private final OutputLimitLimitType displayLimit;
    private final OutputLimitRateType rateType;
    private final Double rate;
    private final String variableName;
    private ExprNode whenExpressionNode;
    private final List<OnTriggerSetAssignment> thenExpressions;
    private final List<ExprNode> crontabAtSchedule;
    private ExprTimePeriod timePeriodExpr;
    private ExprTimePeriod afterTimePeriodExpr;
    private final Integer afterNumberOfEvents;
    private final boolean andAfterTerminate;
    private ExprNode andAfterTerminateExpr;
    private List<OnTriggerSetAssignment> andAfterTerminateThenExpressions;
    private static final long serialVersionUID = 7314871194757342071L;

    public OutputLimitSpec(Double rate, String variableForRate, OutputLimitRateType rateType, OutputLimitLimitType displayLimit, ExprNode whenExpressionNode, List<OnTriggerSetAssignment> thenExpressions, List<ExprNode> crontabAtSchedule, ExprTimePeriod timePeriodExpr, ExprTimePeriod afterTimePeriodExpr, Integer afterNumberOfEvents, boolean andAfterTerminate, ExprNode andAfterTerminateExpr, List<OnTriggerSetAssignment> andAfterTerminateSetExpressions) {
        this.rate = rate;
        this.displayLimit = displayLimit;
        this.variableName = variableForRate;
        this.rateType = rateType;
        this.crontabAtSchedule = crontabAtSchedule;
        this.whenExpressionNode = whenExpressionNode;
        this.thenExpressions = thenExpressions;
        this.timePeriodExpr = timePeriodExpr;
        this.afterTimePeriodExpr = afterTimePeriodExpr;
        this.afterNumberOfEvents = afterNumberOfEvents;
        this.andAfterTerminate = andAfterTerminate;
        this.andAfterTerminateExpr = andAfterTerminateExpr;
        this.andAfterTerminateThenExpressions = andAfterTerminateSetExpressions;
    }

    public OutputLimitSpec(OutputLimitLimitType displayLimit, OutputLimitRateType rateType) {
        this(null, null, rateType, displayLimit, null, null, null, null, null, null, false, null, null);
    }

    /**
     * Returns the type of output limit.
     *
     * @return limit
     */
    public OutputLimitLimitType getDisplayLimit() {
        return displayLimit;
    }

    /**
     * Returns the type of rate.
     *
     * @return rate type
     */
    public OutputLimitRateType getRateType() {
        return rateType;
    }

    /**
     * Returns the rate, or null or -1 if a variable is used instead
     *
     * @return rate if set
     */
    public Double getRate() {
        return rate;
    }

    /**
     * Returns the variable name if set, or null if a fixed rate
     *
     * @return variable name
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Returns the when-keyword trigger expression, or null if not using when.
     *
     * @return expression
     */
    public ExprNode getWhenExpressionNode() {
        return whenExpressionNode;
    }

    /**
     * Returns crontab parameters, or null if not using crontab-at output.
     *
     * @return schedule parameters
     */
    public List<ExprNode> getCrontabAtSchedule() {
        return crontabAtSchedule;
    }

    /**
     * Sets a new when-keyword trigger expression.
     *
     * @param whenExpressionNode to set
     */
    public void setWhenExpressionNode(ExprNode whenExpressionNode) {
        this.whenExpressionNode = whenExpressionNode;
    }

    /**
     * Returns a list of variable assignments, or null if none made.
     *
     * @return variable assignments
     */
    public List<OnTriggerSetAssignment> getThenExpressions() {
        return thenExpressions;
    }

    /**
     * Returns time period expression or null if none used.
     *
     * @return time period
     */
    public ExprTimePeriod getTimePeriodExpr() {
        return timePeriodExpr;
    }

    /**
     * Returns the after-keyword time period.
     *
     * @return after-keyword time period
     */
    public ExprTimePeriod getAfterTimePeriodExpr() {
        return afterTimePeriodExpr;
    }

    /**
     * Returns the after-keyword number of events.
     *
     * @return after-keyword number of events
     */
    public Integer getAfterNumberOfEvents() {
        return afterNumberOfEvents;
    }

    public ExprNode getAndAfterTerminateExpr() {
        return andAfterTerminateExpr;
    }

    public void setAndAfterTerminateExpr(ExprNode andAfterTerminateExpr) {
        this.andAfterTerminateExpr = andAfterTerminateExpr;
    }

    public boolean isAndAfterTerminate() {
        return andAfterTerminate;
    }

    public List<OnTriggerSetAssignment> getAndAfterTerminateThenExpressions() {
        return andAfterTerminateThenExpressions;
    }

    public void setAndAfterTerminateThenExpressions(List<OnTriggerSetAssignment> andAfterTerminateThenExpressions) {
        this.andAfterTerminateThenExpressions = andAfterTerminateThenExpressions;
    }

    public void setAfterTimePeriodExpr(ExprTimePeriod afterTimePeriodExpr) {
        this.afterTimePeriodExpr = afterTimePeriodExpr;
    }

    public void setTimePeriodExpr(ExprTimePeriod timePeriodExpr) {
        this.timePeriodExpr = timePeriodExpr;
    }
}
