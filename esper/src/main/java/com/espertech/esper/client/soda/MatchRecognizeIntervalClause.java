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

/**
 * Interval used within match recognize.
 */
public class MatchRecognizeIntervalClause implements Serializable {
    private static final long serialVersionUID = 3883389636579120071L;
    private Expression expression;
    private boolean orTerminated;

    /**
     * Ctor.
     */
    public MatchRecognizeIntervalClause() {
    }

    /**
     * Ctor.
     *
     * @param expression   interval expression
     * @param orTerminated indicator whether or-terminated
     */
    public MatchRecognizeIntervalClause(TimePeriodExpression expression, boolean orTerminated) {
        this.expression = expression;
        this.orTerminated = orTerminated;
    }

    /**
     * Returns the interval expression.
     *
     * @return expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets the interval expression.
     *
     * @param expression to set
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * Returns indicator whether or-terminated is set
     *
     * @return indicator
     */
    public boolean isOrTerminated() {
        return orTerminated;
    }

    /**
     * Sets indicator whether or-terminated is set
     *
     * @param orTerminated indicator
     */
    public void setOrTerminated(boolean orTerminated) {
        this.orTerminated = orTerminated;
    }
}
