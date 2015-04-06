/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.view;

import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.core.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstMsec;
import com.espertech.esper.util.JavaClassHelper;

public class ViewFactoryTimePeriodHelper
{
    public static ExprTimePeriodEvalDeltaConst validateAndEvaluateTimeDelta(String viewName,
                                                                           StatementContext statementContext,
                                                                           ExprNode expression,
                                                                           String expectedMessage,
                                                                           int expressionNumber)
            throws ViewParameterException
    {
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(statementContext.getEngineURI(), false);
        ExprTimePeriodEvalDeltaConst timeDelta;
        if (expression instanceof ExprTimePeriod) {
            ExprTimePeriod validated = (ExprTimePeriod) ViewFactorySupport.validateExpr(viewName, statementContext, expression, streamTypeService, expressionNumber);
            timeDelta = validated.constEvaluator(new ExprEvaluatorContextStatement(statementContext, false));
        }
        else {
            Object result = ViewFactorySupport.validateAndEvaluateExpr(viewName, statementContext, expression, streamTypeService, expressionNumber);
            if (!(result instanceof Number)) {
                throw new ViewParameterException(expectedMessage);
            }
            Number param = (Number) result;
            long millisecondsBeforeExpiry;
            if (JavaClassHelper.isFloatingPointNumber(param)) {
                millisecondsBeforeExpiry = Math.round(1000d * param.doubleValue());
            }
            else {
                millisecondsBeforeExpiry = 1000 * param.longValue();
            }
            timeDelta = new ExprTimePeriodEvalDeltaConstMsec(millisecondsBeforeExpiry);
        }
        if (timeDelta.deltaMillisecondsAdd(0) < 1) {
            throw new ViewParameterException(viewName + " view requires a size of at least 1 msec");
        }
        return timeDelta;
    }
}
