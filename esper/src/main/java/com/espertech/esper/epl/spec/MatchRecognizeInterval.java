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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.MetaDefItem;

import java.io.Serializable;

/**
 * Interval specification within match_recognize.
 */
public class MatchRecognizeInterval implements MetaDefItem, Serializable {
    private ExprTimePeriod timePeriodExpr;
    private boolean orTerminated;
    private ExprTimePeriodEvalDeltaConst timeDeltaComputation;
    private static final long serialVersionUID = 9015877742992218244L;

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

    /**
     * Returns the number of milliseconds.
     *
     * @param fromTime             from-time
     * @param agentInstanceContext context
     * @return msec
     */
    public long getScheduleForwardDelta(long fromTime, AgentInstanceContext agentInstanceContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qRegIntervalValue(timePeriodExpr);
        }
        if (timeDeltaComputation == null) {
            timeDeltaComputation = timePeriodExpr.constEvaluator(new ExprEvaluatorContextStatement(agentInstanceContext.getStatementContext(), false));
        }
        long result = timeDeltaComputation.deltaAdd(fromTime);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aRegIntervalValue(result);
        }
        return result;
    }

    /**
     * Returns the number of milliseconds.
     *
     * @param fromTime             from-time
     * @param agentInstanceContext context
     * @return msec
     */
    public long getScheduleBackwardDelta(long fromTime, AgentInstanceContext agentInstanceContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qRegIntervalValue(timePeriodExpr);
        }
        if (timeDeltaComputation == null) {
            timeDeltaComputation = timePeriodExpr.constEvaluator(new ExprEvaluatorContextStatement(agentInstanceContext.getStatementContext(), false));
        }
        long result = timeDeltaComputation.deltaSubtract(fromTime);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aRegIntervalValue(result);
        }
        return result;
    }

    public boolean isOrTerminated() {
        return orTerminated;
    }

    public void validate(ExprValidationContext validationContext) throws ExprValidationException {
        timePeriodExpr = (ExprTimePeriod) ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGINTERVAL, timePeriodExpr, validationContext);
    }
}