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

import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitRateType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.fabric.FabricCharge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Factory for output condition instances.
 */
public class OutputConditionFactoryFactory {
    private static final Logger log = LoggerFactory.getLogger(OutputConditionFactoryFactory.class);

    public static OutputConditionFactoryForgeResult createCondition(OutputLimitSpec outputLimitSpec,
                                                                    boolean isGrouped,
                                                                    boolean isStartConditionOnCreation,
                                                                    StatementRawInfo statementRawInfo,
                                                                    StatementCompileTimeServices services)
            throws ExprValidationException {
        FabricCharge fabricCharge = services.getStateMgmtSettingsProvider().newCharge();
        if (outputLimitSpec == null) {
            return new OutputConditionFactoryForgeResult(OutputConditionNullFactoryForge.INSTANCE, fabricCharge);
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
            return new OutputConditionFactoryForgeResult(OutputConditionNullFactoryForge.INSTANCE, fabricCharge);
        }

        if (outputLimitSpec.getRateType() == OutputLimitRateType.CRONTAB) {
            OutputConditionCrontabForge forge = new OutputConditionCrontabForge(outputLimitSpec.getCrontabAtSchedule(), isStartConditionOnCreation, statementRawInfo, services);
            return new OutputConditionFactoryForgeResult(forge, fabricCharge);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.WHEN_EXPRESSION) {
            StateMgmtSetting settings = services.getStateMgmtSettingsProvider().resultSet().outputExpression(fabricCharge);
            OutputConditionExpressionForge forge = new OutputConditionExpressionForge(outputLimitSpec.getWhenExpressionNode(), outputLimitSpec.getThenExpressions(), outputLimitSpec.getAndAfterTerminateExpr(), outputLimitSpec.getAndAfterTerminateThenExpressions(), isStartConditionOnCreation, settings, statementRawInfo, services);
            return new OutputConditionFactoryForgeResult(forge, fabricCharge);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.EVENTS) {
            if ((variableMetaData != null) && (!JavaClassHelper.isNumericNonFP(variableMetaData.getType()))) {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' must be type integer, long or short");
            }
            int rate = -1;
            if (outputLimitSpec.getRate() != null) {
                rate = outputLimitSpec.getRate().intValue();
            }
            StateMgmtSetting setting = services.getStateMgmtSettingsProvider().resultSet().outputCount(fabricCharge);
            OutputConditionCountForge forge = new OutputConditionCountForge(rate, variableMetaData, setting);
            return new OutputConditionFactoryForgeResult(forge, fabricCharge);
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.TERM) {
            OutputConditionFactoryForge forge;
            if (outputLimitSpec.getAndAfterTerminateExpr() == null && (outputLimitSpec.getAndAfterTerminateThenExpressions() == null || outputLimitSpec.getAndAfterTerminateThenExpressions().isEmpty())) {
                forge = new OutputConditionTermFactoryForge();
            } else {
                StateMgmtSetting setting = services.getStateMgmtSettingsProvider().resultSet().outputExpression(fabricCharge);
                forge = new OutputConditionExpressionForge(new ExprConstantNodeImpl(false), Collections.emptyList(), outputLimitSpec.getAndAfterTerminateExpr(), outputLimitSpec.getAndAfterTerminateThenExpressions(), isStartConditionOnCreation, setting, statementRawInfo, services);
            }
            return new OutputConditionFactoryForgeResult(forge, fabricCharge);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(".createCondition creating OutputConditionTime with interval length " + outputLimitSpec.getRate());
            }

            if ((variableMetaData != null) && (!JavaClassHelper.isNumeric(variableMetaData.getType()))) {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' must be of numeric type");
            }

            StateMgmtSetting setting = services.getStateMgmtSettingsProvider().resultSet().outputTime(fabricCharge);
            OutputConditionTimeForge forge = new OutputConditionTimeForge(outputLimitSpec.getTimePeriodExpr(), isStartConditionOnCreation, setting);
            return new OutputConditionFactoryForgeResult(forge, fabricCharge);
        }
    }
}
