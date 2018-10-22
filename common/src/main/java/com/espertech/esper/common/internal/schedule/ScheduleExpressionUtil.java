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
package com.espertech.esper.common.internal.schedule;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;

import java.util.List;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate.evaluateExpressions;

public class ScheduleExpressionUtil {
    public static ExprForge[] crontabScheduleValidate(ExprNodeOrigin origin, List<ExprNode> scheduleSpecExpressionList, boolean allowBindingConsumption, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {

        // Validate the expressions
        ExprForge[] expressions = new ExprForge[scheduleSpecExpressionList.size()];
        int count = 0;
        ExprValidationContext validationContext = new ExprValidationContextBuilder(new StreamTypeServiceImpl(false), statementRawInfo, services)
                .withAllowBindingConsumption(allowBindingConsumption).build();
        for (ExprNode parameters : scheduleSpecExpressionList) {
            ExprNode node = ExprNodeUtilityValidate.getValidatedSubtree(origin, parameters, validationContext);
            expressions[count++] = node.getForge();
        }

        if (expressions.length <= 4 || expressions.length >= 8) {
            throw new ExprValidationException("Invalid schedule specification: " + ScheduleSpecUtil.getExpressionCountException(expressions.length));
        }

        return expressions;
    }

    public static ScheduleSpec crontabScheduleBuild(ExprEvaluator[] scheduleSpecEvaluators, ExprEvaluatorContext context) {
        try {
            Object[] scheduleSpecParameterList = evaluateExpressions(scheduleSpecEvaluators, context);
            return ScheduleSpecUtil.computeValues(scheduleSpecParameterList);
        } catch (ScheduleParameterException e) {
            throw new EPException("Invalid schedule specification: " + e.getMessage(), e);
        }
    }
}
