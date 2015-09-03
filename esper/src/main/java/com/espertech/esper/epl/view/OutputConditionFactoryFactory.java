/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.view;

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.spec.OutputLimitRateType;
import com.espertech.esper.epl.spec.OutputLimitSpec;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableServiceUtil;
import com.espertech.esper.util.JavaClassHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;

/**
 * Factory for output condition instances.
 */
public class OutputConditionFactoryFactory
{
	private static final Log log = LogFactory.getLog(OutputConditionFactoryFactory.class);

    /**
     * Creates an output condition instance.
     * @param outputLimitSpec specifies what kind of condition to create
     * @return instance for performing output
     */
	public static OutputConditionFactory createCondition(OutputLimitSpec outputLimitSpec,
										 	  	  StatementContext statementContext,
                                                  boolean isGrouped,
                                                  boolean isWithHavingClause,
                                                  boolean isStartConditionOnCreation)
            throws ExprValidationException
    {
		if(outputLimitSpec == null)
		{
			return new OutputConditionNullFactory();
		}

        // Check if a variable is present
        VariableMetaData variableMetaData = null;
        if (outputLimitSpec.getVariableName() != null)
        {
            variableMetaData = statementContext.getVariableService().getVariableMetaData(outputLimitSpec.getVariableName());
            if (variableMetaData == null) {
                throw new ExprValidationException("Variable named '" + outputLimitSpec.getVariableName() + "' has not been declared");
            }
            String message = VariableServiceUtil.checkVariableContextName(statementContext.getContextDescriptor(), variableMetaData);
            if (message != null) {
                throw new ExprValidationException(message);
            }
        }

        if (outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.FIRST && isGrouped) {
            return new OutputConditionNullFactory();
        }

        if(outputLimitSpec.getRateType() == OutputLimitRateType.CRONTAB)
        {
            return new OutputConditionCrontabFactory(outputLimitSpec.getCrontabAtSchedule(), statementContext, isStartConditionOnCreation);
        }
        else if(outputLimitSpec.getRateType() == OutputLimitRateType.WHEN_EXPRESSION)
        {
            return new OutputConditionExpressionFactory(outputLimitSpec.getWhenExpressionNode(), outputLimitSpec.getThenExpressions(), statementContext, outputLimitSpec.getAndAfterTerminateExpr(), outputLimitSpec.getAndAfterTerminateThenExpressions(), isStartConditionOnCreation);
        }
        else if(outputLimitSpec.getRateType() == OutputLimitRateType.EVENTS)
		{
            if (log.isDebugEnabled())
            {
			    log.debug(".createCondition creating OutputConditionCount with event rate " + outputLimitSpec);
            }

            if ((variableMetaData != null) && (!JavaClassHelper.isNumericNonFP(variableMetaData.getType())))
            {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' must be type integer, long or short");
            }

            int rate = -1;
            if (outputLimitSpec.getRate() != null)
            {
                rate = outputLimitSpec.getRate().intValue();
            }
            return new OutputConditionCountFactory(rate, variableMetaData);
		}
		else if (outputLimitSpec.getRateType() == OutputLimitRateType.TERM)
		{
            if (outputLimitSpec.getAndAfterTerminateExpr() == null && (outputLimitSpec.getAndAfterTerminateThenExpressions() == null || outputLimitSpec.getAndAfterTerminateThenExpressions().isEmpty())) {
                return new OutputConditionTermFactory();
            }
            else {
                return new OutputConditionExpressionFactory(new ExprConstantNodeImpl(false), Collections.<OnTriggerSetAssignment>emptyList(), statementContext, outputLimitSpec.getAndAfterTerminateExpr(), outputLimitSpec.getAndAfterTerminateThenExpressions(), isStartConditionOnCreation);
            }
		}
        else {
            if (log.isDebugEnabled())
            {
                log.debug(".createCondition creating OutputConditionTime with interval length " + outputLimitSpec.getRate());
            }
            if ((variableMetaData != null) && (!JavaClassHelper.isNumeric(variableMetaData.getType())))
            {
                throw new IllegalArgumentException("Variable named '" + outputLimitSpec.getVariableName() + "' must be of numeric type");
            }

            return new OutputConditionTimeFactory(outputLimitSpec.getTimePeriodExpr(), isStartConditionOnCreation);
        }
	}
}
