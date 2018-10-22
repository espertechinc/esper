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
package com.espertech.esper.common.internal.epl.output.polled;

import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitRateType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.util.JavaClassHelper;

/**
 * Factory for output condition instances that are polled/queried only.
 */
public class OutputConditionPolledFactoryFactory {
    public static OutputConditionPolledFactoryForge createConditionFactory(OutputLimitSpec outputLimitSpec, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices)
            throws ExprValidationException {
        if (outputLimitSpec == null) {
            throw new NullPointerException("Output condition requires a non-null callback");
        }

        // check variable use
        VariableMetaData variableMetaData = null;
        if (outputLimitSpec.getVariableName() != null) {
            variableMetaData = compileTimeServices.getVariableCompileTimeResolver().resolve(outputLimitSpec.getVariableName());
            if (variableMetaData == null) {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' has not been declared");
            }
        }

        if (outputLimitSpec.getRateType() == OutputLimitRateType.CRONTAB) {
            return new OutputConditionPolledCrontabFactoryForge(outputLimitSpec.getCrontabAtSchedule(), statementRawInfo, compileTimeServices);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.WHEN_EXPRESSION) {
            return new OutputConditionPolledExpressionFactoryForge(outputLimitSpec.getWhenExpressionNode(), outputLimitSpec.getThenExpressions(), compileTimeServices);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.EVENTS) {
            int rate = -1;
            if (outputLimitSpec.getRate() != null) {
                rate = outputLimitSpec.getRate().intValue();
            }
            return new OutputConditionPolledCountFactoryForge(rate, variableMetaData);
        } else {
            if (variableMetaData != null && (!JavaClassHelper.isNumeric(variableMetaData.getType()))) {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' must be of numeric type");
            }
            return new OutputConditionPolledTimeFactoryForge(outputLimitSpec.getTimePeriodExpr());
        }
    }
}