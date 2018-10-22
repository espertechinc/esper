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
package com.espertech.esper.common.internal.context.airegistry;

import com.espertech.esper.common.internal.collection.ArrayWrap;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.util.HashMap;
import java.util.Map;

public class AIRegistryUtil {

    public static StatementAIResourceRegistry allocateRegistries(AIRegistryRequirements registryRequirements, AIRegistryFactory factory) {
        AIRegistryPriorEvalStrategy[] priorEvalStrategies = null;
        if (registryRequirements.getPriorFlagsPerStream() != null) {
            boolean[] priorFlagPerStream = registryRequirements.getPriorFlagsPerStream();
            priorEvalStrategies = new AIRegistryPriorEvalStrategy[priorFlagPerStream.length];
            for (int i = 0; i < priorEvalStrategies.length; i++) {
                if (priorFlagPerStream[i]) {
                    priorEvalStrategies[i] = factory.makePrior();
                }
            }
        }

        AIRegistryPreviousGetterStrategy[] previousGetterStrategies = null;
        if (registryRequirements.getPreviousFlagsPerStream() != null) {
            boolean[] previousFlagPerStream = registryRequirements.getPreviousFlagsPerStream();
            previousGetterStrategies = new AIRegistryPreviousGetterStrategy[previousFlagPerStream.length];
            for (int i = 0; i < previousGetterStrategies.length; i++) {
                if (previousFlagPerStream[i]) {
                    previousGetterStrategies[i] = factory.makePrevious();
                }
            }
        }

        Map<Integer, AIRegistrySubqueryEntry> subselects = null;
        if (registryRequirements.getSubqueries() != null) {
            AIRegistryRequirementSubquery[] requirements = registryRequirements.getSubqueries();
            subselects = new HashMap<>();
            for (int i = 0; i < requirements.length; i++) {
                AIRegistrySubselectLookup lookup = factory.makeSubqueryLookup(requirements[i].getLookupStrategyDesc());
                AIRegistryAggregation aggregation = requirements[i].isHasAggregation() ? factory.makeAggregation() : null;
                AIRegistryPriorEvalStrategy prior = requirements[i].isHasPrior() ? factory.makePrior() : null;
                AIRegistryPreviousGetterStrategy prev = requirements[i].isHasPrev() ? factory.makePrevious() : null;
                subselects.put(i, new AIRegistrySubqueryEntry(lookup, aggregation, prior, prev));
            }
        }

        Map<Integer, AIRegistryTableAccess> tableAccesses = null;
        if (registryRequirements.getTableAccessCount() > 0) {
            tableAccesses = new HashMap<>();
            for (int i = 0; i < registryRequirements.getTableAccessCount(); i++) {
                AIRegistryTableAccess strategy = factory.makeTableAccess();
                tableAccesses.put(i, strategy);
            }
        }

        AIRegistryRowRecogPreviousStrategy rowRecogPreviousStrategy = null;
        if (registryRequirements.isRowRecogWithPrevious()) {
            rowRecogPreviousStrategy = factory.makeRowRecogPreviousStrategy();
        }

        return new StatementAIResourceRegistry(factory.makeAggregation(), priorEvalStrategies, subselects, tableAccesses, previousGetterStrategies, rowRecogPreviousStrategy);
    }

    public static void assignFutures(StatementAIResourceRegistry aiResourceRegistry,
                                     int agentInstanceId,
                                     AggregationService optionalAggegationService,
                                     PriorEvalStrategy[] optionalPriorStrategies,
                                     PreviousGetterStrategy[] optionalPreviousGetters,
                                     Map<Integer, SubSelectFactoryResult> subselects,
                                     Map<Integer, ExprTableEvalStrategy> tableAccessStrategies,
                                     RowRecogPreviousStrategy rowRecogPreviousStrategy) {
        // assign aggregation service
        if (optionalAggegationService != null) {
            aiResourceRegistry.getAgentInstanceAggregationService().assignService(agentInstanceId, optionalAggegationService);
        }

        // assign prior-strategies
        if (optionalPriorStrategies != null) {
            for (int i = 0; i < optionalPriorStrategies.length; i++) {
                if (optionalPriorStrategies[i] != null) {
                    aiResourceRegistry.getAgentInstancePriorEvalStrategies()[i].assignService(agentInstanceId, optionalPriorStrategies[i]);
                }
            }
        }

        // assign prior-strategies
        if (optionalPreviousGetters != null) {
            for (int i = 0; i < optionalPreviousGetters.length; i++) {
                if (optionalPreviousGetters[i] != null) {
                    aiResourceRegistry.getAgentInstancePreviousGetterStrategies()[i].assignService(agentInstanceId, optionalPreviousGetters[i]);
                }
            }
        }

        // assign subqueries
        for (Map.Entry<Integer, SubSelectFactoryResult> subselectEntry : subselects.entrySet()) {
            AIRegistrySubqueryEntry registryEntry = aiResourceRegistry.getAgentInstanceSubselects().get(subselectEntry.getKey());
            SubSelectFactoryResult subq = subselectEntry.getValue();
            registryEntry.assign(agentInstanceId, subq.getLookupStrategy(), subq.getAggregationService(), subq.getPriorStrategy(), subq.getPreviousStrategy());
        }

        // assign table access strategies
        for (Map.Entry<Integer, ExprTableEvalStrategy> tableEntry : tableAccessStrategies.entrySet()) {
            AIRegistryTableAccess registryEntry = aiResourceRegistry.getAgentInstanceTableAccesses().get(tableEntry.getKey());
            ExprTableEvalStrategy evalStrategy = tableEntry.getValue();
            registryEntry.assignService(agentInstanceId, evalStrategy);
        }

        // assign match-recognize previous strategy
        if (rowRecogPreviousStrategy != null) {
            aiResourceRegistry.getAgentInstanceRowRecogPreviousStrategy().assignService(agentInstanceId, rowRecogPreviousStrategy);
        }
    }

    public static void checkExpand(int serviceId, ArrayWrap services) {
        if (serviceId > services.getArray().length - 1) {
            int delta = serviceId - services.getArray().length + 1;
            services.expand(delta);
        }
    }
}
