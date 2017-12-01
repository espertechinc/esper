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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.client.annotation.HookType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.service.groupby.*;
import com.espertech.esper.epl.agg.util.*;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.engineimport.EngineImportUtil;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateLocalGroupByDesc;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeGroupKey;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.epl.expression.table.ExprTableNodeUtil;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.expression.visitor.ExprNodePreviousVisitorWParent;
import com.espertech.esper.epl.spec.IntoTableSpec;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceUtil;
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
     * @param statementName             statement name
     * @param engineImportService       engine imports
     * @return service
     * @throws ExprValidationException for validation errors
     */
    public static AggregationServiceMatchRecognizeFactoryDesc getServiceMatchRecognize(int numStreams,
                                                                                       Map<Integer, List<ExprAggregateNode>> measureExprNodesPerStream,
                                                                                       EventType[] typesPerStream,
                                                                                       EngineImportService engineImportService,
                                                                                       String statementName)
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
                    evaluators[index] = ExprNodeCompiler.allocateEvaluator(aggregateNode.getChildNodes()[0].getForge(), engineImportService, AggregationServiceFactoryFactory.class, false, statementName);
                } else {
                    // For aggregation that doesn't evaluate any particular sub-expression, return null on evaluation
                    evaluators[index] = new ExprEvaluator() {
                        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
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

    public static AggregationServiceForgeDesc getService(List<ExprAggregateNode> selectAggregateExprNodes,
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
                                                         EngineImportService engineImportService,
                                                         String statementName,
                                                         TimeAbacus timeAbacus)
            throws ExprValidationException {
        // No aggregates used, we do not need this service
        if ((selectAggregateExprNodes.isEmpty()) && (havingAggregateExprNodes.isEmpty())) {
            if (intoTableSpec != null) {
                throw new ExprValidationException("Into-table requires at least one aggregation function");
            }
            return new AggregationServiceForgeDesc(factoryService.getNullAggregationService(), Collections.<AggregationServiceAggExpressionDesc>emptyList(), Collections.<ExprAggregateNodeGroupKey>emptyList());
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
        List<ExprForge[]> methodAggForgesList = new ArrayList<>();
        for (AggregationServiceAggExpressionDesc aggregation : aggregations) {
            ExprAggregateNode aggregateNode = aggregation.getAggregationNode();
            if (!aggregateNode.getFactory().isAccessAggregation()) {
                ExprForge[] forges = aggregateNode.getFactory().getMethodAggregationForge(typesPerStream.length > 1, typesPerStream);
                methodAggForgesList.add(forges);
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
            Class[] groupByTypes = ExprNodeUtilityCore.getExprResultTypes(groupByNodes);
            ExprTableNodeUtil.validateExpressions(intoTableSpec.getName(), groupByTypes, "group-by", groupByNodes,
                    metadata.getKeyTypes(), "group-by");

            // determine how this binds to existing aggregations, assign column numbers
            BindingMatchResult bindingMatchResult = matchBindingsAssignColumnNumbers(intoTableSpec, metadata, aggregations, selectClauseNamedNodes, methodAggForgesList, declaredExpressions, engineImportService, statementName, isFireAndForget);

            // return factory
            AggregationAccessorSlotPair[] accessorPairs = AggregatorUtil.getAccessorsForForges(bindingMatchResult.getAccessors(), engineImportService, isFireAndForget, statementName);
            AggregationAgent[] agents = AggregatorUtil.getAgentForges(bindingMatchResult.getAgents(), engineImportService, isFireAndForget, statementName);
            AggregationServiceFactoryForge serviceForge = factoryService.getTable(tableService, metadata, bindingMatchResult.getMethodPairs(), bindingMatchResult.getAccessors(), accessorPairs, isJoin, bindingMatchResult.getTargetStates(), bindingMatchResult.getAccessStateExpr(), bindingMatchResult.getAgents(), agents, groupByRollupDesc, hasGroupByClause);
            return new AggregationServiceForgeDesc(serviceForge, aggregations, groupKeyExpressions);
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
        ExprForge[][] methodAggForges = methodAggForgesList.toArray(new ExprForge[methodAggForgesList.size()][]);
        AggregationMethodFactory[] methodAggFactories = new AggregationMethodFactory[methodAggForges.length];
        int count = 0;
        for (AggregationServiceAggExpressionDesc aggregation : aggregations) {
            ExprAggregateNode aggregateNode = aggregation.getAggregationNode();
            if (!aggregateNode.getFactory().isAccessAggregation()) {
                methodAggFactories[count] = aggregateNode.getFactory();
                count++;
            }
        }

        // handle access aggregations
        AggregationMultiFunctionAnalysisResult multiFunctionAggPlan = AggregationMultiFunctionAnalysisHelper.analyzeAccessAggregations(aggregations, engineImportService, isFireAndForget, statementName, groupByNodes);
        AggregationAccessorSlotPairForge[] accessorPairsForge = multiFunctionAggPlan.getAccessorPairsForge();
        AggregationStateFactoryForge[] accessFactories = multiFunctionAggPlan.getStateFactoryForges();
        boolean hasAccessAgg = accessorPairsForge.length > 0;
        boolean hasMethodAgg = methodAggFactories.length > 0;

        AggregationServiceFactoryForge serviceForge;

        // analyze local group by
        AggregationLocalGroupByPlanForge localGroupByPlan = null;
        if (localGroupDesc != null) {
            localGroupByPlan = AggregationGroupByLocalGroupByAnalyzer.analyze(methodAggForges, methodAggFactories, accessFactories, localGroupDesc, groupByNodes, accessorPairsForge, engineImportService, isFireAndForget, statementName);
            try {
                AggregationLocalLevelHook hook = (AggregationLocalLevelHook) EngineImportUtil.getAnnotationHook(annotations, HookType.INTERNAL_AGGLOCALLEVEL, AggregationLocalLevelHook.class, engineImportService);
                if (hook != null) {
                    hook.planned(localGroupDesc, localGroupByPlan);
                }
            } catch (ExprValidationException e) {
                throw new EPException("Failed to obtain hook for " + HookType.INTERNAL_AGGLOCALLEVEL);
            }
        }

        // Handle without a group-by clause: we group all into the same pot
        AggregationRowStateForgeDesc rowStateDesc = new AggregationRowStateForgeDesc(hasMethodAgg ? methodAggForges : null, hasMethodAgg ? methodAggFactories : null, hasAccessAgg ? accessorPairsForge : null, hasAccessAgg ? accessFactories : null);
        if (!hasGroupByClause) {
            if (localGroupByPlan != null) {
                serviceForge = factoryService.getGroupLocalGroupBy(false, isJoin, localGroupByPlan, isUnidirectional, isFireAndForget, isOnSelect);
            } else {
                serviceForge = factoryService.getNoGroup(rowStateDesc, isJoin, isUnidirectional, isFireAndForget, isOnSelect);
            }
        } else {
            AggGroupByDesc groupDesc = new AggGroupByDesc(rowStateDesc, isJoin, isUnidirectional, isFireAndForget, isOnSelect, groupByNodes);
            boolean hasNoReclaim = HintEnum.DISABLE_RECLAIM_GROUP.getHint(annotations) != null;
            Hint reclaimGroupAged = HintEnum.RECLAIM_GROUP_AGED.getHint(annotations);
            Hint reclaimGroupFrequency = HintEnum.RECLAIM_GROUP_AGED.getHint(annotations);
            if (localGroupByPlan != null) {
                serviceForge = factoryService.getGroupLocalGroupBy(true, isJoin, localGroupByPlan, isUnidirectional, isFireAndForget, isOnSelect);
            } else {
                if (!isDisallowNoReclaim && hasNoReclaim) {
                    if (groupByRollupDesc != null) {
                        throw getRollupReclaimEx();
                    }
                    serviceForge = factoryService.getGroupBy(groupDesc, timeAbacus, isUnidirectional, isFireAndForget, isOnSelect);
                } else if (!isDisallowNoReclaim && reclaimGroupAged != null) {
                    if (groupByRollupDesc != null) {
                        throw getRollupReclaimEx();
                    }
                    compileReclaim(groupDesc, reclaimGroupAged, reclaimGroupFrequency, variableService, optionalContextName);
                    serviceForge = factoryService.getGroupBy(groupDesc, timeAbacus, isUnidirectional, isFireAndForget, isOnSelect);
                } else if (groupByRollupDesc != null) {
                    serviceForge = factoryService.getRollup(groupByNodes, groupByRollupDesc, rowStateDesc, isJoin, groupByRollupDesc, isUnidirectional, isFireAndForget, isOnSelect);
                } else {
                    groupDesc.setRefcounted(true);
                    serviceForge = factoryService.getGroupBy(groupDesc, timeAbacus, isUnidirectional, isFireAndForget, isOnSelect);
                }
            }
        }

        return new AggregationServiceForgeDesc(serviceForge, aggregations, groupKeyExpressions);
    }

    private static void compileReclaim(AggGroupByDesc groupDesc, Hint reclaimGroupAged, Hint reclaimGroupFrequency, VariableService variableService, String optionalContextName) throws ExprValidationException {
        String hintValueMaxAge = HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(reclaimGroupAged);
        if (hintValueMaxAge == null) {
            throw new ExprValidationException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_AGED + "' has not been provided");
        }
        AggSvcGroupByReclaimAgedEvalFuncFactory evaluationFunctionMaxAge = getEvaluationFunction(variableService, hintValueMaxAge, optionalContextName);
        groupDesc.setReclaimAged(true);
        groupDesc.setReclaimEvaluationFunctionMaxAge(evaluationFunctionMaxAge);

        String hintValueFrequency = HintEnum.RECLAIM_GROUP_FREQ.getHintAssignedValue(reclaimGroupAged);
        AggSvcGroupByReclaimAgedEvalFuncFactory evaluationFunctionFrequency;
        if ((reclaimGroupFrequency == null) || (hintValueFrequency == null)) {
            evaluationFunctionFrequency = evaluationFunctionMaxAge;
        } else {
            evaluationFunctionFrequency = getEvaluationFunction(variableService, hintValueFrequency, optionalContextName);
        }
        groupDesc.setReclaimEvaluationFunctionFrequency(evaluationFunctionFrequency);
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
        if (partitions.size() == 1 && ExprNodeUtilityCore.deepEqualsIgnoreDupAndOrder(partitions.get(0).getPartitionExpr(), groupByNodes)) {
            return null;
        }
        return new AggregationGroupByLocalGroupDesc(aggregations.size(), partitions.toArray(new AggregationGroupByLocalGroupLevel[partitions.size()]));
    }

    private static List<AggregationServiceAggExpressionDesc> findPartition(List<AggregationGroupByLocalGroupLevel> partitions, ExprNode[] partitionExpressions) {
        for (AggregationGroupByLocalGroupLevel level : partitions) {
            if (ExprNodeUtilityCore.deepEqualsIgnoreDupAndOrder(level.getPartitionExpr(), partitionExpressions)) {
                return level.getExpressions();
            }
        }
        return null;
    }

    private static BindingMatchResult matchBindingsAssignColumnNumbers(IntoTableSpec bindings,
                                                                       TableMetadata metadata,
                                                                       List<AggregationServiceAggExpressionDesc> aggregations,
                                                                       Map<ExprNode, String> selectClauseNamedNodes,
                                                                       List<ExprForge[]> methodAggForgesList,
                                                                       List<ExprDeclaredNode> declaredExpressions,
                                                                       EngineImportService engineImportService,
                                                                       String statementName,
                                                                       boolean isFireAndForget)
            throws ExprValidationException {
        Map<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> methodAggs = new LinkedHashMap<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation>();
        Map<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> accessAggs = new LinkedHashMap<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation>();
        for (AggregationServiceAggExpressionDesc aggDesc : aggregations) {

            // determine assigned name
            String columnName = findColumnNameForAggregation(selectClauseNamedNodes, declaredExpressions, aggDesc.getAggregationNode());
            if (columnName == null) {
                throw new ExprValidationException("Failed to find an expression among the select-clause expressions for expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(aggDesc.getAggregationNode()) + "'");
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
        TableColumnMethodPair[] methodPairs = new TableColumnMethodPair[methodAggForgesList.size()];
        int methodIndex = -1;
        for (Map.Entry<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> methodEntry : methodAggs.entrySet()) {
            methodIndex++;
            int targetIndex = methodEntry.getValue().getMethodOffset();
            ExprForge[] forges = methodAggForgesList.get(methodIndex);
            ExprEvaluator evaluator = ExprNodeUtilityRich.getEvaluatorMayCompileWMultiValue(forges, engineImportService, AggregationServiceFactoryFactory.class, isFireAndForget, statementName);
            methodPairs[methodIndex] = new TableColumnMethodPair(evaluator, forges, targetIndex, methodEntry.getKey().getAggregationNode());
            methodEntry.getKey().setColumnNum(targetIndex);
        }

        // handle access-aggs
        Map<Integer, ExprNode> accessSlots = new LinkedHashMap<Integer, ExprNode>();
        List<AggregationAccessorSlotPairForge> accessReadPairs = new ArrayList<>();
        int accessIndex = -1;
        List<AggregationAgentForge> agents = new ArrayList<>();
        for (Map.Entry<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> accessEntry : accessAggs.entrySet()) {
            accessIndex++;
            int slot = accessEntry.getValue().getAccessAccessorSlotPair().getSlot();
            AggregationMethodFactory aggregationMethodFactory = accessEntry.getKey().getFactory();
            AggregationAccessorForge accessorForge = aggregationMethodFactory.getAccessorForge();
            accessSlots.put(slot, accessEntry.getKey().getAggregationNode());
            accessReadPairs.add(new AggregationAccessorSlotPairForge(slot, accessorForge));
            accessEntry.getKey().setColumnNum(metadata.getNumberMethodAggregations() + accessIndex);
            agents.add(aggregationMethodFactory.getAggregationStateAgent(engineImportService, statementName));
        }
        AggregationAgentForge[] agentArr = agents.toArray(new AggregationAgentForge[agents.size()]);
        AggregationAccessorSlotPairForge[] accessReads = accessReadPairs.toArray(new AggregationAccessorSlotPairForge[accessReadPairs.size()]);

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
                ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(aggregationRequired) +
                "' and received '" +
                ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(aggregationProvided) +
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
            if (!aggNode.equalsNode(aggNodeToAdd, false)) {
                continue;
            }
            if (!ExprNodeUtilityCore.deepEquals(aggNode.getPositionalParams(), aggNodeToAdd.getPositionalParams(), false)) {
                continue;
            }
            if (!ExprNodeUtilityCore.deepEqualsNullChecked(aggNode.getOptionalFilter(), aggNodeToAdd.getOptionalFilter(), false)) {
                continue;
            }
            if (aggNode.getOptionalLocalGroupBy() != null || aggNodeToAdd.getOptionalLocalGroupBy() != null) {
                if ((aggNode.getOptionalLocalGroupBy() == null && aggNodeToAdd.getOptionalLocalGroupBy() != null) ||
                        (aggNode.getOptionalLocalGroupBy() != null && aggNodeToAdd.getOptionalLocalGroupBy() == null)) {
                    continue;
                }
                if (!ExprNodeUtilityCore.deepEqualsIgnoreDupAndOrder(aggNode.getOptionalLocalGroupBy().getPartitionExpressions(), aggNodeToAdd.getOptionalLocalGroupBy().getPartitionExpressions())) {
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

    private static AggSvcGroupByReclaimAgedEvalFuncFactory getEvaluationFunction(final VariableService variableService, String hintValue, String optionalContextName)
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
                valueDouble = Double.parseDouble(hintValue);
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

    public static ExprValidationException getRollupReclaimEx() {
        return new ExprValidationException("Reclaim hints are not available with rollup");
    }

    private static class BindingMatchResult {
        private final TableColumnMethodPair[] methodPairs;
        private final AggregationAccessorSlotPairForge[] accessors;
        private final int[] targetStates;
        private final ExprNode[] accessStateExpr;
        private final AggregationAgentForge[] agents;

        private BindingMatchResult(TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPairForge[] accessors, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgentForge[] agents) {
            this.methodPairs = methodPairs;
            this.accessors = accessors;
            this.targetStates = targetStates;
            this.accessStateExpr = accessStateExpr;
            this.agents = agents;
        }

        public TableColumnMethodPair[] getMethodPairs() {
            return methodPairs;
        }

        public AggregationAccessorSlotPairForge[] getAccessors() {
            return accessors;
        }

        public int[] getTargetStates() {
            return targetStates;
        }

        public AggregationAgentForge[] getAgents() {
            return agents;
        }

        public ExprNode[] getAccessStateExpr() {
            return accessStateExpr;
        }
    }
}
