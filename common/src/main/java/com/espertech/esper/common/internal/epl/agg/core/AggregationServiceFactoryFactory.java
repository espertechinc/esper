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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.Hint;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.stage1.spec.IntoTableSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.agg.groupall.AggregationServiceGroupAllForge;
import com.espertech.esper.common.internal.epl.agg.groupby.*;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.*;
import com.espertech.esper.common.internal.epl.agg.rollup.AggSvcGroupByRollupForge;
import com.espertech.esper.common.internal.epl.agg.table.AggregationServiceFactoryForgeTable;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateLocalGroupByDesc;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeGroupKey;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableNodeUtil;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodePreviousVisitorWParent;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;
import com.espertech.esper.common.internal.epl.table.core.TableColumnMethodPairForge;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Factory for aggregation service instances.
 * <p>
 * Consolidates aggregation nodes such that result futures point to a single instance and
 * no re-evaluation of the same result occurs.
 */
public class AggregationServiceFactoryFactory {

    public static AggregationServiceForgeDesc getService(List<ExprAggregateNode> selectAggregateExprNodes,
                                                         Map<ExprNode, String> selectClauseNamedNodes,
                                                         List<ExprDeclaredNode> declaredExpressions,
                                                         ExprNode[] groupByNodes,
                                                         MultiKeyClassRef groupByMultiKey,
                                                         List<ExprAggregateNode> havingAggregateExprNodes,
                                                         List<ExprAggregateNode> orderByAggregateExprNodes,
                                                         List<ExprAggregateNodeGroupKey> groupKeyExpressions,
                                                         boolean hasGroupByClause,
                                                         Annotation[] annotations,
                                                         VariableCompileTimeResolver variableCompileTimeResolver,
                                                         boolean isDisallowNoReclaim,
                                                         ExprNode whereClause,
                                                         ExprNode havingClause,
                                                         EventType[] typesPerStream,
                                                         AggregationGroupByRollupDescForge groupByRollupDesc,
                                                         String optionalContextName,
                                                         IntoTableSpec intoTableSpec,
                                                         TableCompileTimeResolver tableCompileTimeResolver,
                                                         boolean isUnidirectional,
                                                         boolean isFireAndForget,
                                                         boolean isOnSelect,
                                                         ClasspathImportServiceCompileTime classpathImportService,
                                                         StatementRawInfo raw,
                                                         SerdeCompileTimeResolver serdeResolver)
            throws ExprValidationException {
        // No aggregates used, we do not need this service
        if ((selectAggregateExprNodes.isEmpty()) && (havingAggregateExprNodes.isEmpty())) {
            if (intoTableSpec != null) {
                throw new ExprValidationException("Into-table requires at least one aggregation function");
            }
            return new AggregationServiceForgeDesc(AggregationServiceNullFactory.INSTANCE, Collections.<AggregationServiceAggExpressionDesc>emptyList(), Collections.<ExprAggregateNodeGroupKey>emptyList(), Collections.emptyList());
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
        boolean intoTableNonRollup = groupByRollupDesc == null && intoTableSpec != null;
        for (ExprAggregateNode selectAggNode : selectAggregateExprNodes) {
            addEquivalent(selectAggNode, aggregations, intoTableNonRollup);
        }
        for (ExprAggregateNode havingAggNode : havingAggregateExprNodes) {
            addEquivalent(havingAggNode, aggregations, intoTableNonRollup);
        }
        for (ExprAggregateNode orderByAggNode : orderByAggregateExprNodes) {
            addEquivalent(orderByAggNode, aggregations, intoTableNonRollup);
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
            TableMetaData metadata = tableCompileTimeResolver.resolve(intoTableSpec.getName());
            if (metadata == null) {
                throw new ExprValidationException("Invalid into-table clause: Failed to find table by name '" + intoTableSpec.getName() + "'");
            }

            EPLValidationUtil.validateContextName(true, intoTableSpec.getName(), metadata.getOptionalContextName(), optionalContextName, false);

            // validate group keys
            Class[] groupByTypes = ExprNodeUtilityQuery.getExprResultTypes(groupByNodes);
            Class[] keyTypes = metadata.isKeyed() ? metadata.getKeyTypes() : new Class[0];
            ExprTableNodeUtil.validateExpressions(intoTableSpec.getName(), groupByTypes, "group-by", groupByNodes, keyTypes, "group-by");

            // determine how this binds to existing aggregations, assign column numbers
            BindingMatchResult bindingMatchResult = matchBindingsAssignColumnNumbers(intoTableSpec, metadata, aggregations, selectClauseNamedNodes, methodAggForgesList, declaredExpressions, classpathImportService, raw.getStatementName());

            // return factory
            AggregationServiceFactoryForge serviceForge = new AggregationServiceFactoryForgeTable(metadata, bindingMatchResult.getMethodPairs(), bindingMatchResult.getTargetStates(), bindingMatchResult.getAgents(), groupByRollupDesc);
            return new AggregationServiceForgeDesc(serviceForge, aggregations, groupKeyExpressions, Collections.emptyList());
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
        AggregationForgeFactory[] methodAggFactories = new AggregationForgeFactory[methodAggForges.length];
        int count = 0;
        for (AggregationServiceAggExpressionDesc aggregation : aggregations) {
            ExprAggregateNode aggregateNode = aggregation.getAggregationNode();
            if (!aggregateNode.getFactory().isAccessAggregation()) {
                methodAggFactories[count] = aggregateNode.getFactory();
                count++;
            }
        }

        // handle access aggregations
        AggregationMultiFunctionAnalysisResult multiFunctionAggPlan = AggregationMultiFunctionAnalysisHelper.analyzeAccessAggregations(aggregations, classpathImportService, isFireAndForget, raw.getStatementName(), groupByNodes);
        AggregationAccessorSlotPairForge[] accessorPairsForge = multiFunctionAggPlan.getAccessorPairsForge();
        AggregationStateFactoryForge[] accessFactories = multiFunctionAggPlan.getStateFactoryForges();
        boolean hasAccessAgg = accessorPairsForge.length > 0;
        boolean hasMethodAgg = methodAggFactories.length > 0;

        AggregationServiceFactoryForge serviceForge;
        AggregationUseFlags useFlags = new AggregationUseFlags(isUnidirectional, isFireAndForget, isOnSelect);
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // analyze local group by
        AggregationLocalGroupByPlanForge localGroupByPlan = null;
        if (localGroupDesc != null) {
            AggregationLocalGroupByPlanDesc plan = AggregationGroupByLocalGroupByAnalyzer.analyze(methodAggForges, methodAggFactories, accessFactories, localGroupDesc, groupByNodes, groupByMultiKey, accessorPairsForge, raw, serdeResolver);
            localGroupByPlan = plan.getForge();
            additionalForgeables.addAll(plan.getAdditionalForgeables());
            try {
                AggregationLocalLevelHook hook = (AggregationLocalLevelHook) ClasspathImportUtil.getAnnotationHook(annotations, HookType.INTERNAL_AGGLOCALLEVEL, AggregationLocalLevelHook.class, classpathImportService);
                if (hook != null) {
                    hook.planned(localGroupDesc, localGroupByPlan);
                }
            } catch (ExprValidationException e) {
                throw new EPException("Failed to obtain hook for " + HookType.INTERNAL_AGGLOCALLEVEL);
            }
        }

        // Handle without a group-by clause: we group all into the same pot
        AggregationRowStateForgeDesc rowStateDesc = new AggregationRowStateForgeDesc(
                hasMethodAgg ? methodAggFactories : null, hasMethodAgg ? methodAggForges : null,
                hasAccessAgg ? accessFactories : null, hasAccessAgg ? accessorPairsForge : null, useFlags);
        if (!hasGroupByClause) {
            if (localGroupByPlan != null) {
                serviceForge = new AggSvcLocalGroupByForge(false, localGroupByPlan, useFlags);
            } else {
                serviceForge = new AggregationServiceGroupAllForge(rowStateDesc);
            }
        } else {
            AggGroupByDesc groupDesc = new AggGroupByDesc(rowStateDesc, isUnidirectional, isFireAndForget, isOnSelect, groupByNodes, groupByMultiKey);
            boolean hasNoReclaim = HintEnum.DISABLE_RECLAIM_GROUP.getHint(annotations) != null;
            Hint reclaimGroupAged = HintEnum.RECLAIM_GROUP_AGED.getHint(annotations);
            Hint reclaimGroupFrequency = HintEnum.RECLAIM_GROUP_AGED.getHint(annotations);
            if (localGroupByPlan != null) {
                serviceForge = new AggSvcLocalGroupByForge(true, localGroupByPlan, useFlags);
            } else {
                if (!isDisallowNoReclaim && hasNoReclaim) {
                    if (groupByRollupDesc != null) {
                        throw getRollupReclaimEx();
                    }
                    serviceForge = new AggregationServiceGroupByForge(groupDesc, classpathImportService.getTimeAbacus());
                } else if (!isDisallowNoReclaim && reclaimGroupAged != null) {
                    if (groupByRollupDesc != null) {
                        throw getRollupReclaimEx();
                    }
                    compileReclaim(groupDesc, reclaimGroupAged, reclaimGroupFrequency, variableCompileTimeResolver, optionalContextName);
                    serviceForge = new AggregationServiceGroupByForge(groupDesc, classpathImportService.getTimeAbacus());
                } else if (groupByRollupDesc != null) {
                    serviceForge = new AggSvcGroupByRollupForge(rowStateDesc, groupByRollupDesc, groupByNodes);
                } else {
                    groupDesc.setRefcounted(true);
                    serviceForge = new AggregationServiceGroupByForge(groupDesc, classpathImportService.getTimeAbacus());
                }
            }
        }

        return new AggregationServiceForgeDesc(serviceForge, aggregations, groupKeyExpressions, additionalForgeables);
    }

    private static void addEquivalent(ExprAggregateNode aggNodeToAdd, List<AggregationServiceAggExpressionDesc> equivalencyList, boolean intoTableNonRollup) {
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
            if (!ExprNodeUtilityCompare.deepEquals(aggNode.getPositionalParams(), aggNodeToAdd.getPositionalParams(), false)) {
                continue;
            }
            if (!ExprNodeUtilityCompare.deepEqualsNullChecked(aggNode.getOptionalFilter(), aggNodeToAdd.getOptionalFilter(), false)) {
                continue;
            }
            if (aggNode.getOptionalLocalGroupBy() != null || aggNodeToAdd.getOptionalLocalGroupBy() != null) {
                if ((aggNode.getOptionalLocalGroupBy() == null && aggNodeToAdd.getOptionalLocalGroupBy() != null) ||
                        (aggNode.getOptionalLocalGroupBy() != null && aggNodeToAdd.getOptionalLocalGroupBy() == null)) {
                    continue;
                }
                if (!ExprNodeUtilityCompare.deepEqualsIgnoreDupAndOrder(aggNode.getOptionalLocalGroupBy().getPartitionExpressions(), aggNodeToAdd.getOptionalLocalGroupBy().getPartitionExpressions())) {
                    continue;
                }
            }

            existing.addEquivalent(aggNodeToAdd);
            foundEquivalent = true;
            break;
        }

        if (!foundEquivalent || intoTableNonRollup) {
            equivalencyList.add(new AggregationServiceAggExpressionDesc(aggNodeToAdd, aggNodeToAdd.getFactory()));
        }
    }

    private static void compileReclaim(AggGroupByDesc groupDesc, Hint reclaimGroupAged, Hint reclaimGroupFrequency, VariableCompileTimeResolver variableCompileTimeResolver, String optionalContextName) throws ExprValidationException {
        String hintValueMaxAge = HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(reclaimGroupAged);
        if (hintValueMaxAge == null) {
            throw new ExprValidationException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_AGED + "' has not been provided");
        }
        AggSvcGroupByReclaimAgedEvalFuncFactoryForge evaluationFunctionMaxAge = getEvaluationFunction(variableCompileTimeResolver, hintValueMaxAge, optionalContextName);
        groupDesc.setReclaimAged(true);
        groupDesc.setReclaimEvaluationFunctionMaxAge(evaluationFunctionMaxAge);

        String hintValueFrequency = HintEnum.RECLAIM_GROUP_FREQ.getHintAssignedValue(reclaimGroupAged);
        AggSvcGroupByReclaimAgedEvalFuncFactoryForge evaluationFunctionFrequency;
        if ((reclaimGroupFrequency == null) || (hintValueFrequency == null)) {
            evaluationFunctionFrequency = evaluationFunctionMaxAge;
        } else {
            evaluationFunctionFrequency = getEvaluationFunction(variableCompileTimeResolver, hintValueFrequency, optionalContextName);
        }
        groupDesc.setReclaimEvaluationFunctionFrequency(evaluationFunctionFrequency);
    }

    private static AggregationGroupByLocalGroupDesc analyzeLocalGroupBy(List<AggregationServiceAggExpressionDesc> aggregations, ExprNode[] groupByNodes, AggregationGroupByRollupDescForge groupByRollupDesc, IntoTableSpec intoTableSpec) throws ExprValidationException {

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
        if (partitions.size() == 1 && ExprNodeUtilityCompare.deepEqualsIgnoreDupAndOrder(partitions.get(0).getPartitionExpr(), groupByNodes)) {
            return null;
        }
        return new AggregationGroupByLocalGroupDesc(aggregations.size(), partitions.toArray(new AggregationGroupByLocalGroupLevel[partitions.size()]));
    }

    private static List<AggregationServiceAggExpressionDesc> findPartition(List<AggregationGroupByLocalGroupLevel> partitions, ExprNode[] partitionExpressions) {
        for (AggregationGroupByLocalGroupLevel level : partitions) {
            if (ExprNodeUtilityCompare.deepEqualsIgnoreDupAndOrder(level.getPartitionExpr(), partitionExpressions)) {
                return level.getExpressions();
            }
        }
        return null;
    }

    private static BindingMatchResult matchBindingsAssignColumnNumbers(IntoTableSpec bindings,
                                                                       TableMetaData metadata,
                                                                       List<AggregationServiceAggExpressionDesc> aggregations,
                                                                       Map<ExprNode, String> selectClauseNamedNodes,
                                                                       List<ExprForge[]> methodAggForgesList,
                                                                       List<ExprDeclaredNode> declaredExpressions,
                                                                       ClasspathImportService classpathImportService,
                                                                       String statementName)
            throws ExprValidationException {
        Map<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> methodAggs = new LinkedHashMap<>();
        Map<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> accessAggs = new LinkedHashMap<>();
        for (AggregationServiceAggExpressionDesc aggDesc : aggregations) {

            // determine assigned name
            String columnName = findColumnNameForAggregation(selectClauseNamedNodes, declaredExpressions, aggDesc.getAggregationNode());
            if (columnName == null) {
                throw new ExprValidationException("Failed to find an expression among the select-clause expressions for expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(aggDesc.getAggregationNode()) + "'");
            }

            // determine binding metadata
            TableMetadataColumnAggregation columnMetadata = (TableMetadataColumnAggregation) metadata.getColumns().get(columnName);
            if (columnMetadata == null) {
                throw new ExprValidationException("Failed to find name '" + columnName + "' among the columns for table '" + bindings.getName() + "'");
            }

            // validate compatible
            validateIntoTableCompatible(bindings.getName(), columnName, columnMetadata, aggDesc);

            if (columnMetadata.isMethodAgg()) {
                methodAggs.put(aggDesc, columnMetadata);
            } else {
                accessAggs.put(aggDesc, columnMetadata);
            }
        }

        // handle method-aggs
        TableColumnMethodPairForge[] methodPairs = new TableColumnMethodPairForge[methodAggForgesList.size()];
        int methodIndex = -1;
        for (Map.Entry<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> methodEntry : methodAggs.entrySet()) {
            methodIndex++;
            int column = methodEntry.getValue().getColumn();
            ExprForge[] forges = methodAggForgesList.get(methodIndex);
            methodPairs[methodIndex] = new TableColumnMethodPairForge(forges, column, methodEntry.getKey().getAggregationNode());
            methodEntry.getKey().setColumnNum(column);
        }

        // handle access-aggs
        Map<Integer, ExprNode> accessSlots = new LinkedHashMap<>();
        List<AggregationAccessorSlotPairForge> accessReadPairs = new ArrayList<>();
        int accessIndex = -1;
        List<AggregationAgentForge> agents = new ArrayList<>();
        for (Map.Entry<AggregationServiceAggExpressionDesc, TableMetadataColumnAggregation> accessEntry : accessAggs.entrySet()) {
            accessIndex++;
            int column = accessEntry.getValue().getColumn(); // Slot is zero-based as we enter with zero-offset
            AggregationForgeFactory aggregationMethodFactory = accessEntry.getKey().getFactory();
            AggregationAccessorForge accessorForge = aggregationMethodFactory.getAccessorForge();
            accessSlots.put(column, accessEntry.getKey().getAggregationNode());
            accessReadPairs.add(new AggregationAccessorSlotPairForge(column, accessorForge));
            accessEntry.getKey().setColumnNum(column);
            agents.add(aggregationMethodFactory.getAggregationStateAgent(classpathImportService, statementName));
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
        AggregationPortableValidation factoryProvided = aggDesc.getFactory().getAggregationPortableValidation();
        AggregationPortableValidation factoryRequired = columnMetadata.getAggregationPortableValidation();

        try {
            factoryRequired.validateIntoTableCompatible(columnMetadata.getAggregationExpression(), factoryProvided, ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(aggDesc.getAggregationNode()), aggDesc.getFactory());
        } catch (ExprValidationException ex) {
            String text = getMessage(tableName, columnName, columnMetadata.getAggregationExpression(), aggDesc.getFactory().getAggregationExpression());
            throw new ExprValidationException(text + ": " + ex.getMessage(), ex);
        }
    }

    private static String getMessage(String tableName, String columnName, String aggregationRequired, ExprAggregateNodeBase aggregationProvided) {
        return "Incompatible aggregation function for table '" +
                tableName +
                "' column '" +
                columnName + "', expecting '" +
                aggregationRequired +
                "' and received '" +
                ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(aggregationProvided) +
                "'";
    }

    private static AggSvcGroupByReclaimAgedEvalFuncFactoryForge getEvaluationFunction(VariableCompileTimeResolver variableCompileTimeResolver, String hintValue, String optionalContextName)
            throws ExprValidationException {
        VariableMetaData variableMetaData = variableCompileTimeResolver.resolve(hintValue);
        if (variableMetaData != null) {
            if (!JavaClassHelper.isNumeric(variableMetaData.getType())) {
                throw new ExprValidationException("Variable type of variable '" + variableMetaData.getVariableName() + "' is not numeric");
            }
            String message = VariableUtil.checkVariableContextName(optionalContextName, variableMetaData);
            if (message != null) {
                throw new ExprValidationException(message);
            }
            return new AggSvcGroupByReclaimAgedEvalFuncFactoryVariableForge(variableMetaData);
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
            return new AggSvcGroupByReclaimAgedEvalFuncFactoryConstForge(valueDouble);
        }
    }

    private static class BindingMatchResult {
        private final TableColumnMethodPairForge[] methodPairs;
        private final AggregationAccessorSlotPairForge[] accessors;
        private final int[] targetStates;
        private final ExprNode[] accessStateExpr;
        private final AggregationAgentForge[] agents;

        private BindingMatchResult(TableColumnMethodPairForge[] methodPairs, AggregationAccessorSlotPairForge[] accessors, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgentForge[] agents) {
            this.methodPairs = methodPairs;
            this.accessors = accessors;
            this.targetStates = targetStates;
            this.accessStateExpr = accessStateExpr;
            this.agents = agents;
        }

        public TableColumnMethodPairForge[] getMethodPairs() {
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

    public static ExprValidationException getRollupReclaimEx() {
        return new ExprValidationException("Reclaim hints are not available with rollup");
    }
}
