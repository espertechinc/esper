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
package com.espertech.esper.view;

import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.*;
import com.espertech.esper.util.JavaClassHelper;

public class ViewFactoryTimePeriodHelper {
    public static ExprTimePeriodEvalDeltaConstFactory validateAndEvaluateTimeDeltaFactory(String viewName,
                                                                                          StatementContext statementContext,
                                                                                          ExprNode expression,
                                                                                          String expectedMessage,
                                                                                          int expressionNumber)
            throws ViewParameterException {
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(statementContext.getEngineURI(), false);
        ExprTimePeriodEvalDeltaConstFactory factory;
        if (expression instanceof ExprTimePeriod) {
            ExprTimePeriod validated = (ExprTimePeriod) ViewFactorySupport.validateExpr(viewName, statementContext, expression, streamTypeService, expressionNumber);
            factory = validated.constEvaluator(new ExprEvaluatorContextStatement(statementContext, false));
        } else {
            ExprNode validated = ViewFactorySupport.validateExpr(viewName, statementContext, expression, streamTypeService, expressionNumber);
            ExprEvaluator secondsEvaluator = validated.getForge().getExprEvaluator();
            Class returnType = JavaClassHelper.getBoxedType(validated.getForge().getEvaluationType());
            if (!JavaClassHelper.isNumeric(returnType)) {
                throw new ViewParameterException(expectedMessage);
            }
            if (validated.isConstantResult()) {
                Number time = (Number) ViewFactorySupport.evaluate(secondsEvaluator, 0, viewName, statementContext);
                if (!ExprTimePeriodUtil.validateTime(time, statementContext.getTimeAbacus())) {
                    throw new ViewParameterException(ExprTimePeriodUtil.getTimeInvalidMsg(viewName, "view", time));
                }
                long msec = statementContext.getTimeAbacus().deltaForSecondsNumber(time);
                factory = new ExprTimePeriodEvalDeltaConstGivenDelta(msec);
            } else {
                factory = new ExprTimePeriodEvalDeltaConstFactoryMsec(secondsEvaluator, statementContext.getTimeAbacus());
            }
        }
        return factory;
    }
}
