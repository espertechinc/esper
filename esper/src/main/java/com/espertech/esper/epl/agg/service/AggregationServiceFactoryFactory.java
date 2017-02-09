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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.client.annotation.HookType;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.util.*;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateLocalGroupByDesc;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeGroupKey;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.epl.expression.table.ExprTableNodeUtil;
import com.espertech.esper.epl.expression.visitor.ExprNodePreviousVisitorWParent;
import com.espertech.esper.epl.spec.IntoTableSpec;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Factory for aggregation service instances.
 * <p>
 * Consolidates aggregation nodes such that result futures point to a single instance and
 * no re-evaluation of the same result occurs.
 */
public class AggregationServiceFactoryFactory {
    /**
     * Produces an aggregation service for use with match-recognice.
     *
     * @param numStreams                number of streams
     * @param measureExprNodesPerStream measure nodes
     * @param typesPerStream            type information
     * @return service
     * @throws ExprValidationException for validation errors
     */
    public static AggregationServiceMatchRecognizeFactoryDesc getServiceMatchRecognize(int numStreams,
                                                                                       Map<Integer, List<ExprAggregateNode>> measureExprNodesPerStream,
                                                                                       EventType[] typesPerStream)
            throws ExprValidationException {
        Map<Integer, List<AggregationServiceAggExpressionDesc>> equivalencyListPerStream = new TreeMap<Integer, List<AggregationServiceAggExpressionDesc>>();

        for (Map.Entry<Integer, List<ExprAggregateNode>> entry : measureExprNodesPerStream.entrySet()) {
            List<AggregationServiceAggExpressionDesc> equivalencyList = new ArrayList<AggregationServiceAggExpressionDesc>();
            equivalencyListPerStream.put(entry.getKey(), equivalencyList);
            for (ExprAggregateNode selectAggNode : entry.getValue()) {
                addEquivalent(selectAggNode, equivalencyList);
            }
        }

        LinkedHashMap<Integer, AggregationMethodFactory[]> aggregatorsPerStream = new LinkedHashMap<Integer, AggregationMethodFactory[]>();
        Map<Integer, ExprEvaluator[]> evaluatorsPerStream = new HashMap<Integer, ExprEvaluator[]>();

        for (Map.Entry<Integer, List<AggregationServiceAggExpressionDesc>> equivalencyPerStream : equivalencyListPerStream.entrySet()) {
            int index = 0;
            int stream = equivalencyPerStream.getKey();

            AggregationMethodFactory[] aggregators = new AggregationMethodFactory[equivalencyPerStream.getValue().size()];
            aggregatorsPerStream.put(stream, aggregators);

            ExprEvaluator[] evaluators = new ExprEvaluator[equivalencyPerStream.getValue().size()];
            evaluatorsPerStream.put(stream, evaluators);

            for (AggregationServiceAggExpressionDesc aggregation : equivalencyPerStream.getValue()) {
                ExprAggregateNode aggregateNode = aggregation.getAggregationNode();
                if (aggregateNode.getChildNodes().length > 1) {
                    evaluators[index] = ExprMethodAggUtil.getMultiNodeEvaluator(aggregateNode.getChildNodes(), typesPerStream.length > 1, typesPerStream);
                } else if (aggregateNode.getChildNodes().length > 0) {
                    // Use the evaluation node under the aggregation node to obtain the aggregation value
                    evaluators[index] = aggregateNode.getChildNodes()[0].getExprEvaluator();
                } else {
                    // For aggregation that doesn't evaluate any particular sub-expression, return null on evaluation
                    evaluators[index] = new ExprEvaluator() {
                        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                            return null;
                        }

                        public Class getType() {
                            return null;
                        }
                    };
                }

                aggregators[index] = aggregateNode.getFactory();
                index++;
            }
        }

        // Assign a column number to each aggregation node. The regular aggregation goes first followed by access-aggregation.
        int columnNumber = 0;
        List<AggregationServiceAggExpressionDesc> allExpressions = new ArrayList<AggregationServiceAggExpressionDesc>();
        for (Map.Entry<Integer, List<AggregationServiceAggExpressionDesc>> equivalencyPerStream : equivalencyListPerStream.entrySet()) {
            for (AggregationServiceAggExpressionDesc entry : equivalencyPerStream.getValue()) {
                entry.setColumnNum(columnNumber++);
            }
            allExpressions.addAll(equivalencyPerStream.getValue());
        }

        AggregationServiceMatchRecognizeFactory factory = new AggregationServiceMatchRecognizeFactoryImpl(numStreams, aggregatorsPerStream, evaluatorsPerStream);
        return new AggregationServiceMatchRecognizeFactoryDesc(factory, allExpressions);
    }

    public static AggregationServiceFactoryDesc getService(List<ExprAggregateNode> selectAggregateExprNodes,
                                                           Map<ExprNode, String> selectClauseNamedNodes,
                                                           List<ExprDeclaredNode> declaredExpressions,
                                                           ExprNode[] groupByNodes,
                                                           List<ExprAggregateNode> havingAggregateExprNodes,
                                                           List<ExprAggregateNode> orderByAggregateExprNodes,
                                                           List<ExprAggregateNodeGroupKey> groupKeyExpressions,
                                                           boolean hasGroupByClause,
                                                           Annotation[] annotations,
                                                           VariableService variableService,
                                                           boolean isJoin,
                                                           boolean isDisallowNoReclaim,
                                                           ExprNode whereClause,
                                                           ExprNode havingClause,
                                                           AggregationServiceFactoryService factoryService,
                                                           EventType[] typesPerStream,
                                                           AggregationGroupByRollupDesc groupByRollupDesc,
                                                           String optionalContextName,
                                                           IntoTableSpec intoTableSpec,
                                                           TableService tableService,
                                                           boolean isUnidirectional,
                                                           boolean isFireAndForget,
                                                           boolean isOnSelect,
                                                           EngineImportService engineImportService)
            throws ExprValidationException {
        // No aggregates used, we do not need this service
        if ((selectAggregateExprNodes.isEmpty()) && (havingAggregateExprNodes.isEmpty())) {
            if (intoTableSpec != null) {
                throw new ExprValidationException("Into-table requires at least one aggregation function");
            }
            return new AggregationServiceFactoryDesc(factoryService.getNullAggregationService(), Collections.<AggregationServiceAggExpressionDesc>emptyList(), Collections.<ExprAggregateNodeGroupKey>emptyList());
        }

        // Validate the absence of "prev" function in where-clause:
        // Since the "previous" function does not post remove stream results, disallow when used with aggregations.
        if ((whereClause != null) || (havingClause != null)) {
            ExprNodePreviousVisitorWParent visitor = new ExprNodePreviousVisitorWParent();
            if (whereClause != null) {
                whereClause.accept(visitor);
            }
            if (havingClause != null) {
                havingClause.accept(visitor);
            }
            if ((visitor.getPrevious() != null) && (!visitor.getPrevious().isEmpty())) {
                String funcname = visitor.getPrevious().get(0).getSecond().getPreviousType().toString().toLowerCase(Locale.ENGLISH);
                throw new ExprValidationException("The '" + funcname + "' function may not occur in the where-clause or having-clause of a statement with aggregations as 'previous' does not provide remove stream data; Use the 'first','last','window' or 'count' aggregation functions instead");
            }
        }

        // Compile a map of aggregation nodes and equivalent-to aggregation nodes.
        // Equivalent-to functions are for example "select sum(a*b), 5*sum(a*b)".
        // Reducing the total number of aggregation functions.
        List<AggregationServiceAggExpressionDesc> aggregations = new ArrayList<AggregationServiceAggExpressionDesc>();
        for (ExprAggregateNode selectAggNode : selectAggregateExprNodes) {
            addEquivalent(selectAggNode, aggregations);
        }
        for (ExprAggregateNode havingAggNode : havingAggregateExprNodes) {
            addEquivalent(havingAggNode, aggregations);
        }
        for (ExprAggregateNode orderByAggNode : orderByAggregateExprNodes) {
            addEquivalent(orderByAggNode, aggregations);
        }

        // Construct a list of evaluation node for the aggregation functions (regular agg).
        // For example "sum(2 * 3)" would make the sum an evaluation node.
        List<ExprEvaluator> methodAggEvaluatorsList = new ArrayList<ExprEvaluator>();
        for (AggregationServiceAggExpressionDesc aggregation : aggregations) {
            ExprAggregateNode aggregateNode = aggregation.getAggregationNode();
            if (!aggregateNode.getFactory().isAccessAggregation()) {
                ExprEvaluator evaluator = aggregateNode.getFactory().getMethodAggregationEvaluator(typesPerStream.length > 1, typesPerStream);
                methodAggEvaluatorsList.add(evaluator);
            }
        }

        // determine local group-by, report when hook provided
        AggregationGroupByLocalGroupDesc localGroupDesc = analyzeLocalGroupBy(aggregations, groupByNodes, groupByRollupDesc, intoTableSpec);

        // determine binding
        if (intoTableSpec != null) {

            // obtain metadata
            TableMetadata metadata = tableService.getTableMetadata(intoTableSpec.getName());
            if (metadata == null) {
                throw new ExprValidationException("Invalid into-table clause: Failed to find table by name '" + intoTableSpec.getName() + "'");
            }

            EPLValidationUtil.validateContextName(true, intoTableSpec.getName(), metadata.getContextName(), optionalContextName, false);

            // validate group keys
            Class[] groupByTypes = ExprNodeUtility.getExprResultTypes(groupByNodes);
            ExprTableNodeUtil.validateExpressions(intoTableSpec.getName(), groupByTypes, "group-by", groupByNodes,
                    metadata.getKeyTypes(), "group-by");

            // determine how this binds to existing aggregations, assign column numbers
            BindingMatchResult bindingMatchResult = matchBindingsAssignColumnNumbers(intoTableSpec, metadata, aggregations, selectClauseNamedNodes, methodAggEvaluatorsList, declaredExpressions);

            // return factory
            AggregationServiceFactory serviceFactory;
            if (!hasGroupByClause) {
                serviceFactory = factoryService.getNoGroupWBinding(bindingMatchResult.getAccessors(), isJoin, bindingMatchResult.getMethodPairs(), intoTableSpec.getName(), bindingMatchResult.getTargetStates(), bindingMatchResult.getAccessStateExpr(), bindingMatchResult.getAgents());
            } else {
                serviceFactory = factoryService.getGroupWBinding(metadata, bindingMatchResult.getMethodPairs(), bindingMatchResult.getAccessors(), isJoin, intoTableSpec, bindingMatchResult.getTargetStates(), bindingMatchResult.getAccessStateExpr(), bindingMatchResult.getAgents(), groupByRollupDesc);
            }
            return new AggregationServiceFactoryDesc(serviceFactory, aggregations, groupKeyExpressions);
        }

        // Assign a column number to each aggregation node. The regular aggregation goes first followed by access-aggregation.
        int columnNumber = 0;
        for (AggregationServiceAggExpressionDesc entry : aggregations) {
            if (!entry.getFactory().isAccessAggregation()) {
                entry.setColumnNum(columnNumber++);
            }
        }
        for (AggregationServiceAggExpressionDesc entry : aggregations) {
            if (entry.getFactory().isAccessAggregation()) {
                entry.setColumnNum(columnNumber++);
            }
        }

        // determine method aggregation factories and evaluators(non-access)
        ExprEvaluator[] methodAggEvaluators = methodAggEvaluatorsList.toArray(new ExprEvaluator[methodAggEvaluatorsList.size()]);
        AggregationMethodFactory[] methodAggFactories = new AggregationMethodFactory[methodAggEvaluators.length];
        int count = 0;
        for (AggregationServiceAggExpressionDesc aggregation : aggregations) {
            ExprAggregateNode aggregateNode = aggregation.getAggregationNode();
            if (!aggregateNode.getFactory().isAccessAggregation()) {
                methodAggFactories[count] = aggregateNode.getFactory();
                count++;
            }
        }

        // handle access aggregations
        AggregationMultiFunctionAnalysisResult multiFunctionAggPlan = AggregationMultiFunctionAnalysisHelper.analyzeAccessAggregations(aggregations);
        AggregationAccessorSlotPair[] accessorPairs = multiFunctionAggPlan.getAccessorPairs();
        AggregationStateFactory[] accessAggregations = multiFunctionAggPlan.getStateFactories();

        AggregationServiceFactory serviceFactory;

        // analyze local group by
        AggregationLocalGroupByPlan localGroupByPlan = null;
        if (localGroupDesc != null) {
            localGroupByPlan = AggregationGroupByLocalGroupByAnalyzer.analyze(methodAggEvaluators, methodAggFactories, accessAggregations, localGroupDesc, groupByNodes, accessorPairs);
            try {
                AggregationLocalLevelHook hook = (AggregationLocalLevelHook) JavaClassHelper.getAnnotationHook(annotations, HookType.INTERNAL_AGGLOCALLEVEL, AggregationLocalLevelHook.class, engineImportService);
                if (hook != null) {
                    hook.planned(localGroupDesc, localGroupByPlan);
                }
            } catch (ExprValidationException e) {
                throw new EPException("Failed to obtain hook for " + HookType.INTERNAL_AGGLOCALLEVEL);
            }
        }

        // Handle without a group-by clause: we group all into the same pot
        if (!hasGroupByClause) {
            if (localGroupByPlan != null) {
                serviceFactory = factoryService.getNoGroupLocalGroupBy(isJoin, localGroupByPlan, isUnidirectional, isFireAndForget, isOnSelect);
            } else if ((methodAggEvaluators.length > 0) && (accessorPairs.length == 0)) {
                serviceFactory = factoryService.getNoGroupNoAccess(methodAggEvaluators, methodAggFactories, isUnidirectional, isFireAndForget, isOnSelect);
            } else if ((methodAggEvaluators.length == 0) && (accessorPairs.length > 0)) {
                serviceFactory = factoryService.getNoGroupAccessOnly(accessorPairs, accessAggregations, isJoin, isUnidirectional, isFireAndForget, isOnSelect);
            } else {
                serviceFactory = factoryService.getNoGroupAccessMixed(methodAggEvaluators, methodAggFactories, accessorPairs, accessAggregations, isJoin, isUnidirectional, isFireAndForget, isOnSelect);
            }
        } else {
            boolean hasNoReclaim = HintEnum.DISABLE_RECLAIM_GROUP.getHint(annotations) != null;
            Hint reclaimGroupAged = HintEnum.RECLAIM_GROUP_AGED.getHint(annotations);
            Hint reclaimGroupFrequency = HintEnum.RECLAIM_GROUP_AGED.getHint(annotations);
            if (localGroupByPlan != null) {
                serviceFactory = factoryService.getGroupLocalGroupBy(isJoin, localGroupByPlan, isUnidirectional, isFireAndForget, isOnSelect);
            } else {
                if (!isDisallowNoReclaim && hasNoReclaim) {
                    if (groupByRollupDesc != null) {
                        throw getRollupReclaimEx();
                    }
                    if ((methodAggEvaluators.length > 0) && (accessorPairs.length == 0)) {
                        serviceFactory = factoryService.getGroupedNoReclaimNoAccess(groupByNodes, methodAggEvaluators, methodAggFactories, isUnidirectional, isFireAndForget, isOnSelect);
                    } else if ((methodAggEvaluators.length == 0) && (accessorPairs.length > 0)) {
                        serviceFactory = factoryService.getGroupNoReclaimAccessOnly(groupByNodes, accessorPairs, accessAggregations, isJoin, isUnidirectional, isFireAndForget, isOnSelect);
                    } else {
                        serviceFactory = factoryService.getGroupNoReclaimMixed(groupByNodes, methodAggEvaluators, methodAggFactories, accessorPairs, accessAggregations, isJoin, isUnidirectional, isFireAndForget, isOnSelect);
                    }
                } else if (!isDisallowNoReclaim && reclaimGroupAged != null) {
                    if (groupByRollupDesc != null) {
                        throw getRollupReclaimEx();
                    }
                    serviceFactory = factoryService.getGroupReclaimAged(groupByNodes, methodAggEvaluators, methodAggFactories, reclaimGroupAged, reclaimGroupFrequency, variableService, accessorPairs, accessAggregations, isJoin, optionalContextName, isUnidirectional, isFireAndForget, isOnSelect);
                } else if (groupByRollupDesc != null) {
                    serviceFactory = factoryService.getGroupReclaimMixableRollup(groupByNodes, groupByRollupDesc, methodAggEvaluators, methodAggFactories, accessorPairs, accessAggregations, isJoin, groupByRollupDesc, isUnidirectional, isFireAndForget, isOnSelect);
                } else {
                    if ((methodAggEvaluators.length > 0) && (accessorPairs.length == 0)) {
                        serviceFactory = factoryService.getGroupReclaimNoAccess(groupByNodes, methodAggEvaluators, methodAggFactories, accessorPairs, accessAggregations, isJoin, isUnidirectional, isFireAndForget, isOnSelect);
                    } else {
                        serviceFactory = factoryService.getGroupReclaimMixable(groupByNodes, methodAggEvaluators, methodAggFactories, accessorPairs, accessAggregations, isJoin, isUnidirectional, isFireAndForget, isOnSelect);
                    }
                }
            }
        }

        return new AggregationServiceFactoryDesc(serviceFactory, aggregations, groupKeyExpressions);
    }

    private static AggregationGroupByLocalGroupDesc analyzeLocalGroupBy(List<AggregationServiceAggExpressionDesc> aggregations, ExprNode[] groupByNodes, AggregationGroupByRollupDesc groupByRollupDesc, IntoTableSpec intoTableSpec) throws ExprValidationException {

        boolean hasOver = false;
        for (AggregationServiceAggExpressionDesc desc : aggregations) {
            if (desc.getAggregationNode().getOptionalLocalGroupBy() != null) {
                hasOver = true;
                break;
            }
        }
        if (!hasOver) {
            return null;
        }
        if (groupByRollupDesc != null) {
            throw new ExprValidationException("Roll-up and group-by parameters cannot be combined");
        }
        if (intoTableSpec != null) {
            throw new ExprValidationException("Into-table and group-by parameters cannot be combined");
        }

        List<AggregationGroupByLocalGroupLevel> partitions = new ArrayList<AggregationGroupByLocalGroupLevel>();
        for (AggregationServiceAggExpressionDesc desc : aggregations) {
            ExprAggregateLocalGroupByDesc localGroupBy = desc.getAggregationNode().getOptionalLocalGroupBy();

            ExprNode[] partitionExpressions = localGroupBy == null ? groupByNodes : localGroupBy.getPartitionExpressions();
            List<AggregationServiceAggExpressionDesc> found = findPartition(partitions, partitionExpressions);
            if (found == null) {
                found = new ArrayList<AggregationServiceAggExpressionDesc>();
                AggregationGroupByLocalGroupLevel level = new AggregationGroupByLocalGroupLevel(partitionExpressions, found);
                partitions.add(level);
            }
            found.add(desc);
        }

        // check single group-by partition and it matches the group-by clause
        if (partitions.size() == 1 && ExprNodeUtility.deepEqualsIgnoreDupAndOrder(partitions.get(0).getPartitionExpr(), groupByNodes)) {
            return null;
        }
        return new AggregationGroupByLocalGroupDesc(aggregations.size(), partitions.toArray(new AggregationGroupByLocalGroupLevel[partitions.size()]));
    }

    private static List<AggregationServiceAggExpressionDesc> findPartition(List<AggregationGroupByLocalGroupLevel> partitions, ExprNode[] partitionExpressions) {
        for (AggregationGroupByLocalGroupLevel level : partitions) {
            if (ExprNodeUtility.deepEqualsIgnoreDupAndOrder(level.getPartitionExpr(), partitionExpressions)) {
                return level.getExpressions();
            }
        }
        return null;
    }

    private static BindingMatchResult matchBindingsAssignColumnNumbers(IntoTableSpec bindings,
                                                                       TableMetadata metadata,
                                                                       List<AggregationServiceAggExpressionDesc> aggregations,
                                                                       Map<ExprNode, String> selectClauseNamedNodes,
                                                                       List<ExprEvaluator> methodAggEvaluatorsList, List<ExprDeclaredNode> declaredExpressions)
            throws ExprValidationException {
        Map<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> methodAggs = new LinkedHashMap<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation>();
        Map<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> accessAggs = new LinkedHashMap<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation>();
        for (AggregationServiceAggExpressionDesc aggDesc : aggregations) {

            // determine assigned name
            String columnName = findColumnNameForAggregation(selectClauseNamedNodes, declaredExpressions, aggDesc.getAggregationNode());
            if (columnName == null) {
                throw new ExprValidationException("Failed to find an expression among the select-clause expressions for expression '" + ExprNodeUtility.toExpressionStringMinPrecedenceSafe(aggDesc.getAggregationNode()) + "'");
            }

            // determine binding metadata
            TableMetadataColumnAggregation columnMetadata = (TableMetadataColumnAggregation) metadata.getTableColumns().get(columnName);
            if (columnMetadata == null) {
                throw new ExprValidationException("Failed to find name '" + columnName + "' among the columns for table '" + bindings.getName() + "'");
            }

            // validate compatible
            validateIntoTableCompatible(bindings.getName(), columnName, columnMetadata, aggDesc);

            if (!columnMetadata.getFactory().isAccessAggregation()) {
                methodAggs.put(aggDesc, columnMetadata);
            } else {
                accessAggs.put(aggDesc, columnMetadata);
            }
        }

        // handle method-aggs
        TableColumnMethodPair[] methodPairs = new TableColumnMethodPair[methodAggEvaluatorsList.size()];
        int methodIndex = -1;
        for (Map.Entry<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> methodEntry : methodAggs.entrySet()) {
            methodIndex++;
            int targetIndex = methodEntry.getValue().getMethodOffset();
            methodPairs[methodIndex] = new TableColumnMethodPair(methodAggEvaluatorsList.get(methodIndex), targetIndex, methodEntry.getKey().getAggregationNode());
            methodEntry.getKey().setColumnNum(targetIndex);
        }

        // handle access-aggs
        Map<Integer, ExprNode> accessSlots = new LinkedHashMap<Integer, ExprNode>();
        List<AggregationAccessorSlotPair> accessReadPairs = new ArrayList<AggregationAccessorSlotPair>();
        int accessIndex = -1;
        List<AggregationAgent> agents = new ArrayList<AggregationAgent>();
        for (Map.Entry<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> accessEntry : accessAggs.entrySet()) {
            accessIndex++;
            int slot = accessEntry.getValue().getAccessAccessorSlotPair().getSlot();
            AggregationMethodFactory aggregationMethodFactory = accessEntry.getKey().getFactory();
            AggregationAccessor accessor = aggregationMethodFactory.getAccessor();
            accessSlots.put(slot, accessEntry.getKey().getAggregationNode());
            accessReadPairs.add(new AggregationAccessorSlotPair(slot, accessor));
            accessEntry.getKey().setColumnNum(metadata.getNumberMethodAggregations() + accessIndex);
            agents.add(aggregationMethodFactory.getAggregationStateAgent());
        }
        AggregationAgent[] agentArr = agents.toArray(new AggregationAgent[agents.size()]);
        AggregationAccessorSlotPair[] accessReads = accessReadPairs.toArray(new AggregationAccessorSlotPair[accessReadPairs.size()]);

        int[] targetStates = new int[accessSlots.size()];
        ExprNode[] accessStateExpr = new ExprNode[accessSlots.size()];
        int count = 0;
        for (Map.Entry<Integer, ExprNode> entry : accessSlots.entrySet()) {
            targetStates[count] = entry.getKey();
            accessStateExpr[count] = entry.getValue();
            count++;
        }

        return new BindingMatchResult(methodPairs, accessReads, targetStates, accessStateExpr, agentArr);
    }

    private static String findColumnNameForAggregation(Map<ExprNode, String> selectClauseNamedNodes, List<ExprDeclaredNode> declaredExpressions, ExprAggregateNode aggregationNode) {
        if (selectClauseNamedNodes.containsKey(aggregationNode)) {
            return selectClauseNamedNodes.get(aggregationNode);
        }

        for (ExprDeclaredNode node : declaredExpressions) {
            if (node.getBody() == aggregationNode) {
                return node.getPrototype().getName();
            }
        }

        return null;
    }

    private static void validateIntoTableCompatible(String tableName, String columnName, TableMetadataColumnAggregation columnMetadata, AggregationServiceAggExpressionDesc aggDesc)
            throws ExprValidationException {
        AggregationMethodFactory factoryProvided = aggDesc.getFactory();
        AggregationMethodFactory factoryRequired = columnMetadata.getFactory();

        try {
            factoryRequired.validateIntoTableCompatible(factoryProvided);
        } catch (ExprValidationException ex) {
            String text = getMessage(tableName, columnName, factoryRequired.getAggregationExpression(), factoryProvided.getAggregationExpression());
            throw new ExprValidationException(text + ": " + ex.getMessage(), ex);
        }
    }

    private static String getMessage(String tableName, String columnName, ExprAggregateNodeBase aggregationRequired, ExprAggregateNodeBase aggregationProvided) {
        return "Incompatible aggregation function for table '" +
                tableName +
                "' column '" +
                columnName + "', expecting '" +
                ExprNodeUtility.toExpressionStringMinPrecedenceSafe(aggregationRequired) +
                "' and received '" +
                ExprNodeUtility.toExpressionStringMinPrecedenceSafe(aggregationProvided) +
                "'";
    }

    private static void addEquivalent(ExprAggregateNode aggNodeToAdd, List<AggregationServiceAggExpressionDesc> equivalencyList) {
        // Check any same aggregation nodes among all aggregation clauses
        boolean foundEquivalent = false;
        for (AggregationServiceAggExpressionDesc existing : equivalencyList) {
            ExprAggregateNode aggNode = existing.getAggregationNode();

            // we have equivalence when:
            // (a) equals on node returns true
            // (b) positional parameters are the same
            // (c) non-positional (group-by over, if present, are the same ignoring duplicates)
            if (!aggNode.equalsNode(aggNodeToAdd)) {
                continue;
            }
            if (!ExprNodeUtility.deepEquals(aggNode.getPositionalParams(), aggNodeToAdd.getPositionalParams())) {
                continue;
            }
            if (aggNode.getOptionalLocalGroupBy() != null || aggNodeToAdd.getOptionalLocalGroupBy() != null) {
                if ((aggNode.getOptionalLocalGroupBy() == null && aggNodeToAdd.getOptionalLocalGroupBy() != null) ||
                        (aggNode.getOptionalLocalGroupBy() != null && aggNodeToAdd.getOptionalLocalGroupBy() == null)) {
                    continue;
                }
                if (!ExprNodeUtility.deepEqualsIgnoreDupAndOrder(aggNode.getOptionalLocalGroupBy().getPartitionExpressions(), aggNodeToAdd.getOptionalLocalGroupBy().getPartitionExpressions())) {
                    continue;
                }
            }

            existing.addEquivalent(aggNodeToAdd);
            foundEquivalent = true;
            break;
        }

        if (!foundEquivalent) {
            equivalencyList.add(new AggregationServiceAggExpressionDesc(aggNodeToAdd, aggNodeToAdd.getFactory()));
        }
    }

    public static ExprValidationException getRollupReclaimEx() {
        return new ExprValidationException("Reclaim hints are not available with rollup");
    }

    private static class BindingMatchResult {
        private final TableColumnMethodPair[] methodPairs;
        private final AggregationAccessorSlotPair[] accessors;
        private final int[] targetStates;
        private final ExprNode[] accessStateExpr;
        private final AggregationAgent[] agents;

        private BindingMatchResult(TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents) {
            this.methodPairs = methodPairs;
            this.accessors = accessors;
            this.targetStates = targetStates;
            this.accessStateExpr = accessStateExpr;
            this.agents = agents;
        }

        public TableColumnMethodPair[] getMethodPairs() {
            return methodPairs;
        }

        public AggregationAccessorSlotPair[] getAccessors() {
            return accessors;
        }

        public int[] getTargetStates() {
            return targetStates;
        }

        public AggregationAgent[] getAgents() {
            return agents;
        }

        public ExprNode[] getAccessStateExpr() {
            return accessStateExpr;
        }
    }
}
