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
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceUtil;
import com.espertech.esper.type.DoubleValue;
import com.espertech.esper.util.JavaClassHelper;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByReclaimAgedFactory extends AggregationServiceFactoryBase {
    protected final AggregationAccessorSlotPair[] accessors;
    protected final AggregationStateFactory[] accessAggregations;
    protected final boolean isJoin;

    protected final AggSvcGroupByReclaimAgedEvalFuncFactory evaluationFunctionMaxAge;
    protected final AggSvcGroupByReclaimAgedEvalFuncFactory evaluationFunctionFrequency;

    /**
     * Ctor.
     *
     * @param evaluators            - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param prototypes            - collect the aggregation state that evaluators evaluate to, act as prototypes for new aggregations
     *                              aggregation states for each group
     * @param reclaimGroupAged      hint to reclaim
     * @param reclaimGroupFrequency hint to reclaim
     * @param variableService       variables
     * @param accessors             accessor definitions
     * @param accessAggregations    access aggs
     * @param isJoin                true for join, false for single-stream
     * @param optionalContextName   context name
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException when validation fails
     */
    public AggSvcGroupByReclaimAgedFactory(ExprEvaluator[] evaluators,
                                           AggregationMethodFactory[] prototypes,
                                           Hint reclaimGroupAged,
                                           Hint reclaimGroupFrequency,
                                           final VariableService variableService,
                                           AggregationAccessorSlotPair[] accessors,
                                           AggregationStateFactory[] accessAggregations,
                                           boolean isJoin,
                                           String optionalContextName)
            throws ExprValidationException {
        super(evaluators, prototypes);
        this.accessors = accessors;
        this.accessAggregations = accessAggregations;
        this.isJoin = isJoin;

        String hintValueMaxAge = HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(reclaimGroupAged);
        if (hintValueMaxAge == null) {
            throw new ExprValidationException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_AGED + "' has not been provided");
        }
        evaluationFunctionMaxAge = getEvaluationFunction(variableService, hintValueMaxAge, optionalContextName);

        String hintValueFrequency = HintEnum.RECLAIM_GROUP_FREQ.getHintAssignedValue(reclaimGroupAged);
        if ((reclaimGroupFrequency == null) || (hintValueFrequency == null)) {
            evaluationFunctionFrequency = evaluationFunctionMaxAge;
        } else {
            evaluationFunctionFrequency = getEvaluationFunction(variableService, hintValueFrequency, optionalContextName);
        }
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, EngineImportService engineImportService, boolean isSubquery, Integer subqueryNumber) {
        AggSvcGroupByReclaimAgedEvalFunc max = evaluationFunctionMaxAge.make(agentInstanceContext);
        AggSvcGroupByReclaimAgedEvalFunc freq = evaluationFunctionFrequency.make(agentInstanceContext);
        return new AggSvcGroupByReclaimAgedImpl(evaluators, aggregators, accessors, accessAggregations, isJoin, max, freq, agentInstanceContext.getStatementContext().getTimeAbacus());
    }

    private AggSvcGroupByReclaimAgedEvalFuncFactory getEvaluationFunction(final VariableService variableService, String hintValue, String optionalContextName)
            throws ExprValidationException {
        final VariableMetaData variableMetaData = variableService.getVariableMetaData(hintValue);
        if (variableMetaData != null) {
            if (!JavaClassHelper.isNumeric(variableMetaData.getType())) {
                throw new ExprValidationException("Variable type of variable '" + variableMetaData.getVariableName() + "' is not numeric");
            }
            String message = VariableServiceUtil.checkVariableContextName(optionalContextName, variableMetaData);
            if (message != null) {
                throw new ExprValidationException(message);
            }
            return new AggSvcGroupByReclaimAgedEvalFuncFactory() {
                public AggSvcGroupByReclaimAgedEvalFunc make(AgentInstanceContext agentInstanceContext) {
                    VariableReader reader = variableService.getReader(variableMetaData.getVariableName(), agentInstanceContext.getAgentInstanceId());
                    return new AggSvcGroupByReclaimAgedEvalFuncVariable(reader);
                }
            };
        } else {
            final Double valueDouble;
            try {
                valueDouble = DoubleValue.parseString(hintValue);
            } catch (RuntimeException ex) {
                throw new ExprValidationException("Failed to parse hint parameter value '" + hintValue + "' as a double-typed seconds value or variable name");
            }
            if (valueDouble <= 0) {
                throw new ExprValidationException("Hint parameter value '" + hintValue + "' is an invalid value, expecting a double-typed seconds value or variable name");
            }
            return new AggSvcGroupByReclaimAgedEvalFuncFactory() {
                public AggSvcGroupByReclaimAgedEvalFunc make(AgentInstanceContext agentInstanceContext) {
                    return new AggSvcGroupByReclaimAgedEvalFuncConstant(valueDouble);
                }
            };
        }
    }
}
