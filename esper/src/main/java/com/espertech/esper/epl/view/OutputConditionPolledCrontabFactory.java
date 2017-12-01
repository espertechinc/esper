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
package com.espertech.esper.epl.view;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.schedule.ScheduleParameterException;
import com.espertech.esper.schedule.ScheduleSpec;
import com.espertech.esper.schedule.ScheduleSpecUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Output condition handling crontab-at schedule output.
 */
public final class OutputConditionPolledCrontabFactory implements OutputConditionPolledFactory {
    private final ExprEvaluator[] expressions;

    public OutputConditionPolledCrontabFactory(List<ExprNode> scheduleSpecExpressionList,
                                               StatementContext statementContext)
            throws ExprValidationException {

        ExprValidationContext validationContext = new ExprValidationContext(new StreamTypeServiceImpl(statementContext.getEngineURI(), false), statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), new ExprEvaluatorContextStatement(statementContext, false), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
        expressions = new ExprEvaluator[scheduleSpecExpressionList.size()];
        int count = 0;
        for (ExprNode parameters : scheduleSpecExpressionList) {
            ExprNode node = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, parameters, validationContext);
            expressions[count++] = ExprNodeCompiler.allocateEvaluator(node.getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
        }
    }

    public OutputConditionPolled makeNew(AgentInstanceContext agentInstanceContext) {
        ScheduleSpec scheduleSpec;
        try {
            Object[] scheduleSpecParameterList = evaluate(expressions, agentInstanceContext);
            scheduleSpec = ScheduleSpecUtil.computeValues(scheduleSpecParameterList);
        } catch (ScheduleParameterException e) {
            throw new IllegalArgumentException("Invalid schedule specification : " + e.getMessage(), e);
        }
        OutputConditionPolledCrontabState state = new OutputConditionPolledCrontabState(scheduleSpec, null, 0);
        return new OutputConditionPolledCrontab(agentInstanceContext, state);
    }

    public OutputConditionPolled makeFromState(AgentInstanceContext agentInstanceContext, OutputConditionPolledState state) {
        return new OutputConditionPolledCrontab(agentInstanceContext, (OutputConditionPolledCrontabState) state);
    }

    private static Object[] evaluate(ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] results = new Object[parameters.length];
        int count = 0;
        for (ExprEvaluator expr : parameters) {
            try {
                results[count] = expr.evaluate(null, true, exprEvaluatorContext);
                count++;
            } catch (RuntimeException ex) {
                String message = "Failed expression evaluation in crontab timer-at for parameter " + count + ": " + ex.getMessage();
                log.error(message, ex);
                throw new IllegalArgumentException(message);
            }
        }
        return results;
    }

    private static final Logger log = LoggerFactory.getLogger(OutputConditionPolledCrontabFactory.class);
}
