/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.view;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.core.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.schedule.ScheduleComputeHelper;
import com.espertech.esper.schedule.ScheduleParameterException;
import com.espertech.esper.schedule.ScheduleSpec;
import com.espertech.esper.schedule.ScheduleSpecUtil;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Output condition handling crontab-at schedule output.
 */
public final class OutputConditionPolledCrontab implements OutputConditionPolled
{
    private Long currentReferencePoint;
    private AgentInstanceContext agentInstanceContext;
    private ScheduleSpec scheduleSpec;
    private long nextScheduledTime;

    /**
     * Constructor.
     * @param agentInstanceContext is the view context for time scheduling
     * @param scheduleSpecExpressionList list of schedule parameters
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException if the crontab expression failed to validate
     */
    public OutputConditionPolledCrontab(List<ExprNode> scheduleSpecExpressionList,
                                   AgentInstanceContext agentInstanceContext)
            throws ExprValidationException
    {
        if (agentInstanceContext == null)
        {
            String message = "OutputConditionTime requires a non-null view context";
            throw new NullPointerException(message);
        }

        this.agentInstanceContext = agentInstanceContext;

        // Validate the expression
        ExprEvaluator[] expressions = new ExprEvaluator[scheduleSpecExpressionList.size()];
        int count = 0;
        ExprValidationContext validationContext = new ExprValidationContext(new StreamTypeServiceImpl(agentInstanceContext.getStatementContext().getEngineURI(), false), agentInstanceContext.getStatementContext().getMethodResolutionService(), null, agentInstanceContext.getStatementContext().getSchedulingService(), agentInstanceContext.getStatementContext().getVariableService(), agentInstanceContext.getStatementContext().getTableService(), agentInstanceContext, agentInstanceContext.getStatementContext().getEventAdapterService(), agentInstanceContext.getStatementContext().getStatementName(), agentInstanceContext.getStatementContext().getStatementId(), agentInstanceContext.getStatementContext().getAnnotations(), agentInstanceContext.getStatementContext().getContextDescriptor(), false, false, false, false, null, false);
        for (ExprNode parameters : scheduleSpecExpressionList)
        {
            ExprNode node = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, parameters, validationContext);
            expressions[count++] = node.getExprEvaluator();
        }

        try
        {
            Object[] scheduleSpecParameterList = evaluate(expressions, agentInstanceContext);
            scheduleSpec = ScheduleSpecUtil.computeValues(scheduleSpecParameterList);
        }
        catch (ScheduleParameterException e)
        {
            throw new IllegalArgumentException("Invalid schedule specification : " + e.getMessage(), e);
        }
    }

    public final boolean updateOutputCondition(int newEventsCount, int oldEventsCount)
    {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()))
        {
        	log.debug(".updateOutputCondition, " +
        			"  newEventsCount==" + newEventsCount +
        			"  oldEventsCount==" + oldEventsCount);
        }

        boolean output = false;
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        if (currentReferencePoint == null)
        {
        	currentReferencePoint = currentTime;
            nextScheduledTime = ScheduleComputeHelper.computeNextOccurance(scheduleSpec, currentTime);
            output = true;
        }

        if (nextScheduledTime <= currentTime)
        {
            nextScheduledTime = ScheduleComputeHelper.computeNextOccurance(scheduleSpec, currentTime);
            output = true;
        }

        return output;
    }

    private static Object[] evaluate(ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext)
    {
        Object[] results = new Object[parameters.length];
        int count = 0;
        for (ExprEvaluator expr : parameters)
        {
            try
            {
                results[count] = expr.evaluate(null, true, exprEvaluatorContext);
                count++;
            }
            catch (RuntimeException ex)
            {
                String message = "Failed expression evaluation in crontab timer-at for parameter " + count + ": " + ex.getMessage();
                log.error(message, ex);
                throw new IllegalArgumentException(message);
            }
        }
        return results;
    }

    private static final Log log = LogFactory.getLog(OutputConditionPolledCrontab.class);
}
