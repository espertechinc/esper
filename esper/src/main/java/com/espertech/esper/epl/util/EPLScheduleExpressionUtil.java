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
package com.espertech.esper.epl.util;

import com.espertech.esper.client.EPException;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.schedule.ScheduleParameterException;
import com.espertech.esper.schedule.ScheduleSpec;
import com.espertech.esper.schedule.ScheduleSpecUtil;

import java.util.List;

import static com.espertech.esper.epl.expression.core.ExprNodeUtilityCore.evaluateExpressions;

public class EPLScheduleExpressionUtil {
    public static ExprEvaluator[] crontabScheduleValidate(ExprNodeOrigin origin, List<ExprNode> scheduleSpecExpressionList, StatementContext context, boolean allowBindingConsumption)
            throws ExprValidationException {

        // Validate the expressions
        ExprEvaluator[] expressions = new ExprEvaluator[scheduleSpecExpressionList.size()];
        int count = 0;
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(context, false);
        for (ExprNode parameters : scheduleSpecExpressionList) {
            ExprValidationContext validationContext = new ExprValidationContext(new StreamTypeServiceImpl(context.getEngineURI(), false), context.getEngineImportService(), context.getStatementExtensionServicesContext(), null, context.getSchedulingService(), context.getVariableService(), context.getTableService(), evaluatorContextStmt, context.getEventAdapterService(), context.getStatementName(), context.getStatementId(), context.getAnnotations(), context.getContextDescriptor(), false, false, allowBindingConsumption, false, null, false);
            ExprNode node = ExprNodeUtilityRich.getValidatedSubtree(origin, parameters, validationContext);
            expressions[count++] = ExprNodeCompiler.allocateEvaluator(node.getForge(), context.getEngineImportService(), ExprNodeUtilityCore.class, false, context.getStatementName());
        }

        if (expressions.length <= 4 || expressions.length >= 8) {
            throw new ExprValidationException("Invalid schedule specification: " + ScheduleSpecUtil.getExpressionCountException(expressions.length));
        }

        return expressions;
    }

    public static ScheduleSpec crontabScheduleBuild(ExprEvaluator[] scheduleSpecEvaluators, ExprEvaluatorContext context) {

        // Build a schedule
        try {
            Object[] scheduleSpecParameterList = evaluateExpressions(scheduleSpecEvaluators, context);
            return ScheduleSpecUtil.computeValues(scheduleSpecParameterList);
        } catch (ScheduleParameterException e) {
            throw new EPException("Invalid schedule specification: " + e.getMessage(), e);
        }
    }
}
