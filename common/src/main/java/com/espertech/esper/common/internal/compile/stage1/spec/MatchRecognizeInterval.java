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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriodEvalDeltaConst;

/**
 * Interval specification within match_recognize.
 */
public class MatchRecognizeInterval {
    private ExprTimePeriod timePeriodExpr;
    private boolean orTerminated;
    private ExprTimePeriodEvalDeltaConst timeDeltaComputation;

    /**
     * Ctor.
     *
     * @param timePeriodExpr time period
     * @param orTerminated   or-terminated indicator
     */
    public MatchRecognizeInterval(ExprTimePeriod timePeriodExpr, boolean orTerminated) {
        this.timePeriodExpr = timePeriodExpr;
        this.orTerminated = orTerminated;
    }

    /**
     * Returns the time period.
     *
     * @return time period
     */
    public ExprTimePeriod getTimePeriodExpr() {
        return timePeriodExpr;
    }

    public void setTimePeriodExpr(ExprTimePeriod timePeriodExpr) {
        this.timePeriodExpr = timePeriodExpr;
    }

    public void setTimeDeltaComputation(ExprTimePeriodEvalDeltaConst timeDeltaComputation) {
        this.timeDeltaComputation = timeDeltaComputation;
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    public boolean isOrTerminated() {
        return orTerminated;
    }
}