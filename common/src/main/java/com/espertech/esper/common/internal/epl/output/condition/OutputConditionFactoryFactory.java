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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitRateType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Factory for output condition instances.
 */
public class OutputConditionFactoryFactory {
    private static final Logger log = LoggerFactory.getLogger(OutputConditionFactoryFactory.class);

    public static OutputConditionFactoryForge createCondition(OutputLimitSpec outputLimitSpec,
                                                              boolean isGrouped,
                                                              boolean isWithHavingClause,
                                                              boolean isStartConditionOnCreation,
                                                              StatementRawInfo statementRawInfo,
                                                              StatementCompileTimeServices services)
            throws ExprValidationException {
        if (outputLimitSpec == null) {
            return OutputConditionNullFactoryForge.INSTANCE;
        }

        // Check if a variable is present
        VariableMetaData variableMetaData = null;
        if (outputLimitSpec.getVariableName() != null) {
            variableMetaData = services.getVariableCompileTimeResolver().resolve(outputLimitSpec.getVariableName());
            if (variableMetaData == null) {
                throw new ExprValidationException("Variable named '" + outputLimitSpec.getVariableName() + "' has not been declared");
            }
            String message = VariableUtil.checkVariableContextName(statementRawInfo.getContextName(), variableMetaData);
            if (message != null) {
                throw new ExprValidationException(message);
            }
        }

        if (outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.FIRST && isGrouped) {
            return OutputConditionNullFactoryForge.INSTANCE;
        }

        if (outputLimitSpec.getRateType() == OutputLimitRateType.CRONTAB) {
            return new OutputConditionCrontabForge(outputLimitSpec.getCrontabAtSchedule(), isStartConditionOnCreation, statementRawInfo, services);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.WHEN_EXPRESSION) {
            return new OutputConditionExpressionForge(outputLimitSpec.getWhenExpressionNode(), outputLimitSpec.getThenExpressions(), outputLimitSpec.getAndAfterTerminateExpr(), outputLimitSpec.getAndAfterTerminateThenExpressions(), isStartConditionOnCreation, services);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.EVENTS) {
            if ((variableMetaData != null) && (!JavaClassHelper.isNumericNonFP(variableMetaData.getType()))) {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' must be type integer, long or short");
            }
            int rate = -1;
            if (outputLimitSpec.getRate() != null) {
                rate = outputLimitSpec.getRate().intValue();
            }
            return new OutputConditionCountForge(rate, variableMetaData);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.TERM) {
            if (outputLimitSpec.getAndAfterTerminateExpr() == null && (outputLimitSpec.getAndAfterTerminateThenExpressions() == null || outputLimitSpec.getAndAfterTerminateThenExpressions().isEmpty())) {
                return new OutputConditionTermFactoryForge();
            } else {
                return new OutputConditionExpressionForge(new ExprConstantNodeImpl(false), Collections.<OnTriggerSetAssignment>emptyList(), outputLimitSpec.getAndAfterTerminateExpr(), outputLimitSpec.getAndAfterTerminateThenExpressions(), isStartConditionOnCreation, services);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(".createCondition creating OutputConditionTime with interval length " + outputLimitSpec.getRate());
            }

            if ((variableMetaData != null) && (!JavaClassHelper.isNumeric(variableMetaData.getType()))) {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' must be of numeric type");
            }

            return new OutputConditionTimeForge(outputLimitSpec.getTimePeriodExpr(), isStartConditionOnCreation);
        }
    }
}
