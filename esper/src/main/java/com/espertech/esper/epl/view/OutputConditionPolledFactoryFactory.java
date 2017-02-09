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

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.OutputLimitRateType;
import com.espertech.esper.epl.spec.OutputLimitSpec;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.util.JavaClassHelper;

/**
 * Factory for output condition instances that are polled/queried only.
 */
public class OutputConditionPolledFactoryFactory {
    public static OutputConditionPolledFactory createConditionFactory(OutputLimitSpec outputLimitSpec,
                                                                      StatementContext statementContext)
            throws ExprValidationException {
        if (outputLimitSpec == null) {
            throw new NullPointerException("Output condition by count requires a non-null callback");
        }

        // check variable use
        VariableMetaData variableMetaData = null;
        if (outputLimitSpec.getVariableName() != null) {
            variableMetaData = statementContext.getVariableService().getVariableMetaData(outputLimitSpec.getVariableName());
            if (variableMetaData == null) {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' has not been declared");
            }
        }

        if (outputLimitSpec.getRateType() == OutputLimitRateType.CRONTAB) {
            return new OutputConditionPolledCrontabFactory(outputLimitSpec.getCrontabAtSchedule(), statementContext);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.WHEN_EXPRESSION) {
            return new OutputConditionPolledExpressionFactory(outputLimitSpec.getWhenExpressionNode(), outputLimitSpec.getThenExpressions(), statementContext);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.EVENTS) {
            int rate = -1;
            if (outputLimitSpec.getRate() != null) {
                rate = outputLimitSpec.getRate().intValue();
            }
            return new OutputConditionPolledCountFactory(rate, statementContext, outputLimitSpec.getVariableName());
        } else {
            if (variableMetaData != null && (!JavaClassHelper.isNumeric(variableMetaData.getType()))) {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' must be of numeric type");
            }
            return new OutputConditionPolledTimeFactory(outputLimitSpec.getTimePeriodExpr(), statementContext);
        }
    }
}