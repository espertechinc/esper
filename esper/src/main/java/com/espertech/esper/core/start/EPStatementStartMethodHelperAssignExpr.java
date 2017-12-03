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
package com.espertech.esper.core.start;

import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.subselect.SubSelectStrategyFactoryDesc;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationResultFuture;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.agg.service.common.AggregationServiceAggExpressionDesc;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeGroupKey;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;
import com.espertech.esper.epl.expression.prev.ExprPreviousMatchRecognizeNode;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.rowregex.RegexExprPreviousEvalStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EPStatementStartMethodHelperAssignExpr {
    public static void assignExpressionStrategies(EPStatementStartMethodSelectDesc selectDesc, AggregationService aggregationService, Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategyInstances, Map<ExprPriorNode, ExprPriorEvalStrategy> priorStrategyInstances, Map<ExprPreviousNode, ExprPreviousEvalStrategy> previousStrategyInstances, Set<ExprPreviousMatchRecognizeNode> matchRecognizeNodes, RegexExprPreviousEvalStrategy matchRecognizePrevEvalStrategy, Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> tableAccessStrategyInstances) {
        // initialize aggregation expression nodes
        if (selectDesc.getResultSetProcessorPrototypeDesc().getAggregationServiceFactoryDesc() != null && aggregationService != null) {
            EPStatementStartMethodHelperAssignExpr.assignAggregations(aggregationService, selectDesc.getResultSetProcessorPrototypeDesc().getAggregationServiceFactoryDesc().getExpressions());
        }

        // assign subquery nodes
        assignSubqueryStrategies(selectDesc.getSubSelectStrategyCollection(), subselectStrategyInstances);

        // assign prior nodes
        assignPriorStrategies(priorStrategyInstances);

        // assign previous nodes
        assignPreviousStrategies(previousStrategyInstances);

        // assign match-recognize previous nodes
        assignMatchRecognizePreviousStrategies(matchRecognizeNodes, matchRecognizePrevEvalStrategy);

        // assign table access nodes
        assignTableAccessStrategies(tableAccessStrategyInstances);
    }

    public static void assignTableAccessStrategies(Map<ExprTableAccessNode, ExprTableAccessEvalStrategy> tableAccessStrategies) {
        for (Map.Entry<ExprTableAccessNode, ExprTableAccessEvalStrategy> pair : tableAccessStrategies.entrySet()) {
            pair.getKey().setStrategy(pair.getValue());
        }
    }

    public static void assignMatchRecognizePreviousStrategies(Set<ExprPreviousMatchRecognizeNode> matchRecognizeNodes, RegexExprPreviousEvalStrategy strategy) {
        if (matchRecognizeNodes != null && strategy != null) {
            for (ExprPreviousMatchRecognizeNode node : matchRecognizeNodes) {
                node.setStrategy(strategy);
            }
        }
    }

    public static void unassignMatchRecognizePreviousStrategies(Collection<ExprPreviousMatchRecognizeNode> matchRecognizeNodes) {
        if (matchRecognizeNodes != null) {
            for (ExprPreviousMatchRecognizeNode node : matchRecognizeNodes) {
                node.setStrategy(null);
            }
        }
    }

    public static void assignAggregations(AggregationResultFuture aggregationService, List<AggregationServiceAggExpressionDesc> aggregationExpressions) {
        for (AggregationServiceAggExpressionDesc aggregation : aggregationExpressions) {
            aggregation.assignFuture(aggregationService);
        }
    }

    public static void unassignAggregations(List<AggregationServiceAggExpressionDesc> aggregationExpressions) {
        for (AggregationServiceAggExpressionDesc aggregation : aggregationExpressions) {
            aggregation.assignFuture(null);
        }
    }

    public static void assignPreviousStrategies(Map<ExprPreviousNode, ExprPreviousEvalStrategy> previousStrategyInstances) {
        for (Map.Entry<ExprPreviousNode, ExprPreviousEvalStrategy> pair : previousStrategyInstances.entrySet()) {
            pair.getKey().setEvaluator(pair.getValue());
        }
    }

    public static void unassignPreviousStrategies(Collection<ExprPreviousNode> nodes) {
        for (ExprPreviousNode node : nodes) {
            node.setEvaluator(null);
        }
    }

    public static void assignPriorStrategies(Map<ExprPriorNode, ExprPriorEvalStrategy> priorStrategyInstances) {
        for (Map.Entry<ExprPriorNode, ExprPriorEvalStrategy> pair : priorStrategyInstances.entrySet()) {
            pair.getKey().setPriorStrategy(pair.getValue());
        }
    }

    public static void unassignPriorStrategies(Collection<ExprPriorNode> priorStrategyInstances) {
        for (ExprPriorNode node : priorStrategyInstances) {
            node.setPriorStrategy(null);
        }
    }

    public static ResultSetProcessor getAssignResultSetProcessor(AgentInstanceContext agentInstanceContext, ResultSetProcessorFactoryDesc resultSetProcessorPrototype, boolean isSubquery, Integer subqueryNumber, boolean isFireAndForget) {
        AggregationService aggregationService = null;
        if (resultSetProcessorPrototype.getAggregationServiceFactoryDesc() != null) {
            aggregationService = resultSetProcessorPrototype.getAggregationServiceFactoryDesc().getAggregationServiceFactory().makeService(agentInstanceContext, agentInstanceContext.getEngineImportService(), isSubquery, subqueryNumber);
        }

        OrderByProcessor orderByProcessor = null;
        if (resultSetProcessorPrototype.getOrderByProcessorFactory() != null) {
            orderByProcessor = resultSetProcessorPrototype.getOrderByProcessorFactory().instantiate(agentInstanceContext);
        }

        ResultSetProcessor processor = resultSetProcessorPrototype.getResultSetProcessorFactory().instantiate(orderByProcessor, aggregationService, agentInstanceContext);

        // initialize aggregation expression nodes
        if (resultSetProcessorPrototype.getAggregationServiceFactoryDesc() != null) {
            for (AggregationServiceAggExpressionDesc aggregation : resultSetProcessorPrototype.getAggregationServiceFactoryDesc().getExpressions()) {
                aggregation.assignFuture(aggregationService);
            }
        }

        return processor;
    }

    public static void unassignSubqueryStrategies(Collection<ExprSubselectNode> subselects) {
        for (ExprSubselectNode subselectNode : subselects) {
            subselectNode.setStrategy(null);
            subselectNode.setSubselectAggregationService(null);
        }
    }

    public static void assignSubqueryStrategies(SubSelectStrategyCollection subSelectStrategyCollection, Map<ExprSubselectNode, SubSelectStrategyHolder> subselectStrategyInstances) {
        // initialize subselects expression nodes (strategy assignment)
        for (Map.Entry<ExprSubselectNode, SubSelectStrategyHolder> subselectEntry : subselectStrategyInstances.entrySet()) {

            ExprSubselectNode subselectNode = subselectEntry.getKey();
            SubSelectStrategyHolder strategyInstance = subselectEntry.getValue();

            subselectNode.setStrategy(strategyInstance.getStategy());
            subselectNode.setSubselectAggregationService(strategyInstance.getSubselectAggregationService());

            // initialize aggregations in the subselect
            SubSelectStrategyFactoryDesc factoryDesc = subSelectStrategyCollection.getSubqueries().get(subselectNode);
            if (factoryDesc.getAggregationServiceFactoryDesc() != null) {
                for (AggregationServiceAggExpressionDesc aggExpressionDesc : factoryDesc.getAggregationServiceFactoryDesc().getExpressions()) {
                    aggExpressionDesc.assignFuture(subselectEntry.getValue().getSubselectAggregationService());
                }
                if (factoryDesc.getAggregationServiceFactoryDesc().getGroupKeyExpressions() != null) {
                    for (ExprAggregateNodeGroupKey groupKeyExpr : factoryDesc.getAggregationServiceFactoryDesc().getGroupKeyExpressions()) {
                        groupKeyExpr.assignFuture(subselectEntry.getValue().getSubselectAggregationService());
                    }
                }
            }

            // initialize "prior" nodes in the subselect
            if (strategyInstance.getPriorStrategies() != null) {
                for (Map.Entry<ExprPriorNode, ExprPriorEvalStrategy> entry : strategyInstance.getPriorStrategies().entrySet()) {
                    entry.getKey().setPriorStrategy(entry.getValue());
                }
            }

            // initialize "prev" nodes in the subselect
            if (strategyInstance.getPreviousNodeStrategies() != null) {
                for (Map.Entry<ExprPreviousNode, ExprPreviousEvalStrategy> entry : strategyInstance.getPreviousNodeStrategies().entrySet()) {
                    entry.getKey().setEvaluator(entry.getValue());
                }
            }
        }
    }
}
