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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameSubqueryAgg;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRefWSerde;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.*;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.context.util.ContextPropertyRegistry;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceForgeDesc;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeGroupKey;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredNoTraverseVisitor;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTableFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.index.inkeyword.PropertyHashedArrayFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.index.unindexed.UnindexedEventTableFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.hint.IndexHint;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionUtil;
import com.espertech.esper.common.internal.epl.join.queryplan.IndexNameAndDescPair;
import com.espertech.esper.common.internal.epl.join.queryplanbuild.QueryPlanIndexBuilder;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescSubquery;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexHook;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexHookUtil;
import com.espertech.esper.common.internal.epl.lookup.SubordFullTableScanLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.lookupplan.*;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.epl.util.ViewResourceVerifyHelper;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateDesc;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateExpr;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewProcessingException;
import com.espertech.esper.common.internal.view.prior.PriorEventViewForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

public class SubSelectHelperForgePlanner {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);
    private final static String MSG_SUBQUERY_REQUIRES_WINDOW = "Subqueries require one or more views to limit the stream, consider declaring a length or time window (applies to correlated or non-fully-aggregated subqueries)";

    public static SubSelectHelperForgePlan planSubSelect(StatementBaseInfo statement,
                                                         Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation,
                                                         String[] outerStreamNames,
                                                         EventType[] outerEventTypesSelect,
                                                         String[] outerEventTypeNamees,
                                                         StatementCompileTimeServices compileTimeServices)
        throws ExprValidationException, ViewProcessingException {

        ExprDeclaredNode[] declaredExpressions = statement.getStatementSpec().getDeclaredExpressions();
        Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges = new LinkedHashMap<>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        Map<ExprDeclaredNode, List<ExprDeclaredNode>> declaredExpressionCallHierarchy = null;
        if (declaredExpressions.length > 0) {
            declaredExpressionCallHierarchy = ExprNodeUtilityQuery.getDeclaredExpressionCallHierarchy(declaredExpressions);
        }

        for (Map.Entry<ExprSubselectNode, SubSelectActivationPlan> entry : subselectActivation.entrySet()) {
            ExprSubselectNode subselect = entry.getKey();
            SubSelectActivationPlan subSelectActivation = entry.getValue();

            try {
                SubSelectFactoryForgeDesc forgeDesc = planSubSelectInternal(
                    subselect, subSelectActivation, outerStreamNames, outerEventTypesSelect, outerEventTypeNamees, declaredExpressions, statement.getContextPropertyRegistry(), declaredExpressionCallHierarchy, statement, compileTimeServices);
                subselectForges.put(entry.getKey(), forgeDesc.getSubSelectFactoryForge());
                additionalForgeables.addAll(forgeDesc.getAdditionalForgeables());
            } catch (Exception ex) {
                throw new ExprValidationException("Failed to plan " + ExprNodeUtilityMake.getSubqueryInfoText(subselect) + ": " + ex.getMessage(), ex);
            }
        }

        return new SubSelectHelperForgePlan(subselectForges, additionalForgeables);
    }

    private static SubSelectFactoryForgeDesc planSubSelectInternal(ExprSubselectNode subselect,
                                                                   SubSelectActivationPlan subselectActivation,
                                                                   String[] outerStreamNames,
                                                                   EventType[] outerEventTypesSelect,
                                                                   String[] outerEventTypeNamees,
                                                                   ExprDeclaredNode[] declaredExpressions,
                                                                   ContextPropertyRegistry contextPropertyRegistry,
                                                                   Map<ExprDeclaredNode, List<ExprDeclaredNode>> declaredExpressionCallHierarchy,
                                                                   StatementBaseInfo statement,
                                                                   StatementCompileTimeServices services)
        throws ExprValidationException {
        boolean queryPlanLogging = services.getConfiguration().getCommon().getLogging().isEnableQueryPlan();
        if (queryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
            QUERY_PLAN_LOG.info("For statement '" + statement.getStatementNumber() + "' subquery " + subselect.getSubselectNumber());
        }

        Annotation[] annotations = statement.getStatementSpec().getAnnotations();
        IndexHint indexHint = IndexHint.getIndexHint(annotations);
        StatementSpecCompiled subselectSpec = subselect.getStatementSpecCompiled();
        StreamSpecCompiled filterStreamSpec = subselectSpec.getStreamSpecs()[0];
        int subqueryNum = subselect.getSubselectNumber();

        String subselecteventTypeName = null;
        if (filterStreamSpec instanceof FilterStreamSpecCompiled) {
            subselecteventTypeName = ((FilterStreamSpecCompiled) filterStreamSpec).getFilterSpecCompiled().getFilterForEventTypeName();
        } else if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec) {
            subselecteventTypeName = ((NamedWindowConsumerStreamSpec) filterStreamSpec).getNamedWindow().getEventType().getName();
        } else if (filterStreamSpec instanceof TableQueryStreamSpec) {
            subselecteventTypeName = ((TableQueryStreamSpec) filterStreamSpec).getTable().getTableName();
        }

        List<ViewFactoryForge> viewForges = subselectActivation.getViewForges();
        EventType eventType = viewForges.isEmpty() ? subselectActivation.getViewableType() : viewForges.get(viewForges.size() - 1).getEventType();

        // determine a stream name unless one was supplied
        String subexpressionStreamName = SubselectUtil.getStreamName(filterStreamSpec.getOptionalStreamName(), subselect.getSubselectNumber());
        String[] allStreamNames = new String[outerStreamNames.length + 1];
        System.arraycopy(outerStreamNames, 0, allStreamNames, 1, outerStreamNames.length);
        allStreamNames[0] = subexpressionStreamName;

        // Named windows don't allow data views
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec || filterStreamSpec instanceof TableQueryStreamSpec) {
            EPStatementStartMethodHelperValidate.validateNoDataWindowOnNamedWindow(viewForges);
        }

        // Expression declarations are copies of a predefined expression body with their own stream context.
        // Should only be invoked if the subselect belongs to that instance.
        StreamTypeService subselectTypeService = null;
        EventType[] outerEventTypes = null;

        // determine subselect type information from the enclosing declared expression, if possibly enclosed
        if (declaredExpressions != null && declaredExpressions.length > 0) {
            subselectTypeService = getDeclaredExprTypeService(declaredExpressions, declaredExpressionCallHierarchy, outerStreamNames, outerEventTypesSelect, subselect, subexpressionStreamName, eventType);
            if (subselectTypeService != null) {
                outerEventTypes = new EventType[subselectTypeService.getEventTypes().length - 1];
                System.arraycopy(subselectTypeService.getEventTypes(), 1, outerEventTypes, 0, subselectTypeService.getEventTypes().length - 1);
            }
        }

        // Use the override provided by the subselect if present
        if (subselectTypeService == null) {
            if (subselect.getFilterSubqueryStreamTypes() != null) {
                subselectTypeService = subselect.getFilterSubqueryStreamTypes();
                outerEventTypes = new EventType[subselectTypeService.getEventTypes().length - 1];
                System.arraycopy(subselectTypeService.getEventTypes(), 1, outerEventTypes, 0, subselectTypeService.getEventTypes().length - 1);
            } else {
                // Streams event types are the original stream types with the stream zero the subselect stream
                LinkedHashMap<String, Pair<EventType, String>> namesAndTypes = new LinkedHashMap<String, Pair<EventType, String>>();
                namesAndTypes.put(subexpressionStreamName, new Pair<EventType, String>(eventType, subselecteventTypeName));
                for (int i = 0; i < outerEventTypesSelect.length; i++) {
                    Pair<EventType, String> pair = new Pair<EventType, String>(outerEventTypesSelect[i], outerEventTypeNamees[i]);
                    namesAndTypes.put(outerStreamNames[i], pair);
                }
                subselectTypeService = new StreamTypeServiceImpl(namesAndTypes, true, true);
                outerEventTypes = outerEventTypesSelect;
            }
        }

        // Validate select expression
        ViewResourceDelegateExpr viewResourceDelegateSubselect = new ViewResourceDelegateExpr();
        SelectClauseSpecCompiled selectClauseSpec = subselect.getStatementSpecCompiled().getSelectClauseCompiled();
        List<ExprNode> selectExpressions = new ArrayList<ExprNode>();
        List<String> assignedNames = new ArrayList<String>();
        boolean isWildcard = false;
        boolean isStreamWildcard = false;
        boolean hasNonAggregatedProperties;

        ExprValidationContext validationContext = new ExprValidationContextBuilder(subselectTypeService, statement.getStatementRawInfo(), services)
            .withViewResourceDelegate(viewResourceDelegateSubselect).withAllowBindingConsumption(true)
            .withMemberName(new ExprValidationMemberNameQualifiedSubquery(subqueryNum)).build();
        List<ExprAggregateNode> aggExprNodesSelect = new ArrayList<>(2);

        for (int i = 0; i < selectClauseSpec.getSelectExprList().length; i++) {
            SelectClauseElementCompiled element = selectClauseSpec.getSelectExprList()[i];

            if (element instanceof SelectClauseExprCompiledSpec) {
                // validate
                SelectClauseExprCompiledSpec compiled = (SelectClauseExprCompiledSpec) element;
                ExprNode selectExpression = compiled.getSelectExpression();
                selectExpression = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SELECT, selectExpression, validationContext);

                selectExpressions.add(selectExpression);
                if (compiled.getAssignedName() == null) {
                    assignedNames.add(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(selectExpression));
                } else {
                    assignedNames.add(compiled.getAssignedName());
                }

                // handle aggregation
                ExprAggregateNodeUtil.getAggregatesBottomUp(selectExpression, aggExprNodesSelect);

                // This stream (stream 0) properties must either all be under aggregation, or all not be.
                if (aggExprNodesSelect.size() > 0) {
                    List<Pair<Integer, String>> propertiesNotAggregated = ExprNodeUtilityQuery.getExpressionProperties(selectExpression, false);
                    for (Pair<Integer, String> pair : propertiesNotAggregated) {
                        if (pair.getFirst() == 0) {
                            throw new ExprValidationException("Subselect properties must all be within aggregation functions");
                        }
                    }
                }
            } else if (element instanceof SelectClauseElementWildcard) {
                isWildcard = true;
            } else if (element instanceof SelectClauseStreamCompiledSpec) {
                isStreamWildcard = true;
            }
        }   // end of for loop

        // validate having-clause and collect aggregations
        List<ExprAggregateNode> aggExpressionNodesHaving = Collections.emptyList();
        if (subselectSpec.getRaw().getHavingClause() != null) {
            ExprNode validatedHavingClause = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.HAVING, subselectSpec.getRaw().getHavingClause(), validationContext);
            if (JavaClassHelper.getBoxedType(validatedHavingClause.getForge().getEvaluationType()) != Boolean.class) {
                throw new ExprValidationException("Subselect having-clause expression must return a boolean value");
            }
            aggExpressionNodesHaving = new ArrayList<>();
            ExprAggregateNodeUtil.getAggregatesBottomUp(validatedHavingClause, aggExpressionNodesHaving);
            validateAggregationPropsAndLocalGroup(aggExpressionNodesHaving);

            // if the having-clause does not have aggregations, it becomes part of the filter
            if (aggExpressionNodesHaving.isEmpty()) {
                ExprNode filter = subselectSpec.getRaw().getWhereClause();
                if (filter == null) {
                    subselectSpec.getRaw().setWhereClause(subselectSpec.getRaw().getHavingClause());
                } else {
                    subselectSpec.getRaw().setWhereClause(ExprNodeUtilityMake.connectExpressionsByLogicalAnd(Arrays.asList(subselectSpec.getRaw().getWhereClause(), subselectSpec.getRaw().getHavingClause())));
                }
                subselectSpec.getRaw().setHavingClause(null);
            } else {
                subselect.setHavingExpr(validatedHavingClause.getForge());
                ExprNodePropOrStreamSet nonAggregatedPropsHaving = ExprNodeUtilityAggregation.getNonAggregatedProps(validationContext.getStreamTypeService().getEventTypes(), Collections.singletonList(validatedHavingClause), contextPropertyRegistry);
                for (ExprNodePropOrStreamPropDesc prop : nonAggregatedPropsHaving.getProperties()) {
                    if (prop.getStreamNum() == 0) {
                        throw new ExprValidationException("Subselect having-clause requires that all properties are under aggregation, consider using the 'first' aggregation function instead");
                    }
                }
            }
        }

        // Figure out all non-aggregated event properties in the select clause (props not under a sum/avg/max aggregation node)
        ExprNodePropOrStreamSet nonAggregatedPropsSelect = ExprNodeUtilityAggregation.getNonAggregatedProps(validationContext.getStreamTypeService().getEventTypes(), selectExpressions, contextPropertyRegistry);
        hasNonAggregatedProperties = !nonAggregatedPropsSelect.isEmpty();

        // Validate and set select-clause names and expressions
        if (!selectExpressions.isEmpty()) {
            if (isWildcard || isStreamWildcard) {
                throw new ExprValidationException("Subquery multi-column select does not allow wildcard or stream wildcard when selecting multiple columns.");
            }
            if (selectExpressions.size() > 1 && !subselect.isAllowMultiColumnSelect()) {
                throw new ExprValidationException("Subquery multi-column select is not allowed in this context.");
            }
            if (subselectSpec.getGroupByExpressions() == null && selectExpressions.size() > 1 &&
                aggExprNodesSelect.size() > 0 && hasNonAggregatedProperties) {
                throw new ExprValidationException("Subquery with multi-column select requires that either all or none of the selected columns are under aggregation, unless a group-by clause is also specified");
            }
            subselect.setSelectClause(selectExpressions.toArray(new ExprNode[selectExpressions.size()]));
            subselect.setSelectAsNames(assignedNames.toArray(new String[assignedNames.size()]));
        }

        // Handle aggregation
        ExprNodePropOrStreamSet propertiesGroupBy = null;
        AggregationServiceForgeDesc aggregationServiceForgeDesc = null;
        ExprNode[] groupByNodes = null;
        MultiKeyPlan groupByMultikeyPlan = null;
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        if (aggExprNodesSelect.size() > 0 || aggExpressionNodesHaving.size() > 0) {
            GroupByClauseExpressions groupBy = subselectSpec.getGroupByExpressions();
            if (groupBy != null && groupBy.getGroupByRollupLevels() != null) {
                throw new ExprValidationException("Group-by expressions in a subselect may not have rollups");
            }
            groupByNodes = groupBy == null ? null : groupBy.getGroupByNodes();
            boolean hasGroupBy = groupByNodes != null && groupByNodes.length > 0;
            if (hasGroupBy) {

                // validate group-by
                for (int i = 0; i < groupByNodes.length; i++) {
                    groupByNodes[i] = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.GROUPBY, groupByNodes[i], validationContext);
                    String minimal = ExprNodeUtilityValidate.isMinimalExpression(groupByNodes[i]);
                    if (minimal != null) {
                        throw new ExprValidationException("Group-by expressions in a subselect may not have " + minimal);
                    }
                }

                // Get a list of event properties being aggregated in the select clause, if any
                propertiesGroupBy = ExprNodeUtilityAggregation.getGroupByPropertiesValidateHasOne(groupByNodes);

                // Validated all group-by properties come from stream itself
                ExprNodePropOrStreamDesc firstNonZeroGroupBy = propertiesGroupBy.getFirstWithStreamNumNotZero();
                if (firstNonZeroGroupBy != null) {
                    throw new ExprValidationException("Subselect with group-by requires that group-by properties are provided by the subselect stream only (" + firstNonZeroGroupBy.getTextual() + " is not)");
                }

                // Validate that this is a grouped full-aggregated case
                String reasonMessage = propertiesGroupBy.notContainsAll(nonAggregatedPropsSelect);
                boolean allInGroupBy = reasonMessage == null;
                if (!allInGroupBy) {
                    throw new ExprValidationException("Subselect with group-by requires non-aggregated properties in the select-clause to also appear in the group-by clause");
                }

                // Plan multikey
                groupByMultikeyPlan = MultiKeyPlanner.planMultiKey(groupByNodes, false, statement.getStatementRawInfo(), services.getSerdeResolver());
                additionalForgeables.addAll(groupByMultikeyPlan.getMultiKeyForgeables());
            }

            // Other stream properties, if there is aggregation, cannot be under aggregation.
            validateAggregationPropsAndLocalGroup(aggExprNodesSelect);

            // determine whether select-clause has grouped-by expressions
            List<ExprAggregateNodeGroupKey> groupKeyExpressions = null;
            ExprNode[] groupByExpressions = ExprNodeUtilityQuery.EMPTY_EXPR_ARRAY;
            if (hasGroupBy) {
                groupByExpressions = subselectSpec.getGroupByExpressions().getGroupByNodes();
                for (int i = 0; i < selectExpressions.size(); i++) {
                    ExprNode selectExpression = selectExpressions.get(i);
                    boolean revalidate = false;
                    for (int j = 0; j < groupByExpressions.length; j++) {
                        List<Pair<ExprNode, ExprNode>> foundPairs = ExprNodeUtilityQuery.findExpression(selectExpression, groupByExpressions[j]);
                        for (Pair<ExprNode, ExprNode> pair : foundPairs) {
                            CodegenFieldName aggName = new CodegenFieldNameSubqueryAgg(subqueryNum);
                            ExprAggregateNodeGroupKey replacement = new ExprAggregateNodeGroupKey(groupByExpressions.length, j, groupByExpressions[j].getForge().getEvaluationType(), aggName);
                            if (pair.getFirst() == null) {
                                selectExpressions.set(i, replacement);
                            } else {
                                ExprNodeUtilityModify.replaceChildNode(pair.getFirst(), pair.getSecond(), replacement);
                                revalidate = true;
                            }
                            if (groupKeyExpressions == null) {
                                groupKeyExpressions = new ArrayList<ExprAggregateNodeGroupKey>();
                            }
                            groupKeyExpressions.add(replacement);
                        }
                    }

                    // if the select-clause expression changed, revalidate it
                    if (revalidate) {
                        selectExpression = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SELECT, selectExpression, validationContext);
                        selectExpressions.set(i, selectExpression);
                    }
                }   // end of for loop
            }

            aggregationServiceForgeDesc = AggregationServiceFactoryFactory.getService(aggExprNodesSelect, Collections.emptyMap(),
                Collections.emptyList(), groupByExpressions, groupByMultikeyPlan == null ? null : groupByMultikeyPlan.getClassRef(), aggExpressionNodesHaving, Collections.emptyList(),
                groupKeyExpressions, hasGroupBy, annotations, services.getVariableCompileTimeResolver(), true, subselectSpec.getRaw().getWhereClause(), subselectSpec.getRaw().getHavingClause(),
                subselectTypeService.getEventTypes(), null, subselectSpec.getRaw().getOptionalContextName(), null, null, false, false, false,
                services.getClasspathImportServiceCompileTime(), statement.getStatementRawInfo(), services.getSerdeResolver());
            additionalForgeables.addAll(aggregationServiceForgeDesc.getAdditionalForgeables());

            // assign select-clause
            if (!selectExpressions.isEmpty()) {
                subselect.setSelectClause(selectExpressions.toArray(new ExprNode[selectExpressions.size()]));
                subselect.setSelectAsNames(assignedNames.toArray(new String[assignedNames.size()]));
            }
        }

        // no aggregation functions allowed in filter
        if (subselectSpec.getRaw().getWhereClause() != null) {
            List<ExprAggregateNode> aggExprNodesFilter = new LinkedList<ExprAggregateNode>();
            ExprAggregateNodeUtil.getAggregatesBottomUp(subselectSpec.getRaw().getWhereClause(), aggExprNodesFilter);
            if (aggExprNodesFilter.size() > 0) {
                throw new ExprValidationException("Aggregation functions are not supported within subquery filters, consider using a having-clause or insert-into instead");
            }
        }

        // validate filter expression, if there is one
        ExprNode filterExpr = subselectSpec.getRaw().getWhereClause();

        // add the table filter for tables
        if (filterStreamSpec instanceof TableQueryStreamSpec) {
            TableQueryStreamSpec table = (TableQueryStreamSpec) filterStreamSpec;
            filterExpr = ExprNodeUtilityMake.connectExpressionsByLogicalAnd(table.getFilterExpressions(), filterExpr);
        }

        // determine correlated
        boolean correlatedSubquery = false;
        if (filterExpr != null) {
            filterExpr = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.FILTER, filterExpr, validationContext);
            if (JavaClassHelper.getBoxedType(filterExpr.getForge().getEvaluationType()) != Boolean.class) {
                throw new ExprValidationException("Subselect filter expression must return a boolean value");
            }

            // check the presence of a correlated filter, not allowed with aggregation
            ExprNodeIdentifierVisitor visitor = new ExprNodeIdentifierVisitor(true);
            filterExpr.accept(visitor);
            List<Pair<Integer, String>> propertiesNodes = visitor.getExprProperties();
            for (Pair<Integer, String> pair : propertiesNodes) {
                if (pair.getFirst() != 0) {
                    correlatedSubquery = true;
                    break;
                }
            }
        }

        ViewResourceDelegateDesc viewResourceDelegateDesc = ViewResourceVerifyHelper.verifyPreviousAndPriorRequirements(new List[]{viewForges}, viewResourceDelegateSubselect)[0];
        if (ViewResourceDelegateDesc.hasPrior(new ViewResourceDelegateDesc[]{viewResourceDelegateDesc})) {
            if (!viewResourceDelegateDesc.getPriorRequests().isEmpty()) {
                viewForges.add(new PriorEventViewForge(viewForges.isEmpty(), viewForges.isEmpty() ? eventType : viewForges.get(viewForges.size() - 1).getEventType()));
            }
        }

        // Set the aggregated flag
        // This must occur here as some analysis of return type depends on aggregated or not.
        if (aggregationServiceForgeDesc == null) {
            subselect.setSubselectAggregationType(ExprSubselectNode.SubqueryAggregationType.NONE);
        } else {
            subselect.setSubselectAggregationType(hasNonAggregatedProperties ? ExprSubselectNode.SubqueryAggregationType.FULLY_AGGREGATED_WPROPS : ExprSubselectNode.SubqueryAggregationType.FULLY_AGGREGATED_NOPROPS);
        }

        // Set the filter.
        ExprForge filterExprEval = (filterExpr == null) ? null : filterExpr.getForge();
        ExprForge assignedFilterExpr = aggregationServiceForgeDesc != null ? null : filterExprEval;
        subselect.setFilterExpr(assignedFilterExpr);

        // validation for correlated subqueries against named windows contained-event syntax
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec && correlatedSubquery) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) filterStreamSpec;
            if (namedSpec.getOptPropertyEvaluator() != null) {
                throw new ExprValidationException("Failed to validate named window use in subquery, contained-event is only allowed for named windows when not correlated");
            }
        }

        // Validate presence of a data window
        validateSubqueryDataWindow(subselect, correlatedSubquery, hasNonAggregatedProperties, propertiesGroupBy, nonAggregatedPropsSelect);

        // Determine strategy factories
        //

        // handle named window index share first
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) filterStreamSpec;
            if (namedSpec.getFilterExpressions().isEmpty()) {
                NamedWindowMetaData namedWindow = namedSpec.getNamedWindow();
                boolean disableIndexShare = HintEnum.DISABLE_WINDOW_SUBQUERY_INDEXSHARE.getHint(annotations) != null;
                if (disableIndexShare && namedWindow.isVirtualDataWindow()) {
                    disableIndexShare = false;
                }

                if ((!disableIndexShare && namedWindow.isEnableIndexShare()) || (services.isFireAndForget())) {
                    validateContextAssociation(statement.getContextName(), namedWindow.getContextName(), "named window '" + namedWindow.getEventType().getName() + "'");
                    if (queryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
                        QUERY_PLAN_LOG.info("prefering shared index");
                    }
                    boolean fullTableScan = HintEnum.SET_NOINDEX.getHint(annotations) != null;
                    ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(allStreamNames, statement.getStatementRawInfo(), services);
                    SubordPropPlan joinedPropPlan = QueryPlanIndexBuilder.getJoinProps(filterExpr, outerEventTypes.length, subselectTypeService.getEventTypes(), excludePlanHint);
                    SubSelectStrategyFactoryIndexShareForge strategyForge = new SubSelectStrategyFactoryIndexShareForge(subqueryNum, subselectActivation, outerEventTypesSelect, namedWindow, null,
                        fullTableScan, indexHint, joinedPropPlan, filterExprEval, groupByNodes, aggregationServiceForgeDesc, statement, services);
                    additionalForgeables.addAll(strategyForge.getAdditionalForgeables());
                    SubSelectFactoryForge forge = new SubSelectFactoryForge(subqueryNum, subselectActivation.getActivator(), strategyForge);
                    return new SubSelectFactoryForgeDesc(forge, additionalForgeables);
                }
            } else if (services.isFireAndForget()) {
                throw new ExprValidationException("Subqueries in fire-and-forget queries do not allow filter expressions");
            }
        }

        // handle table-subselect
        if (filterStreamSpec instanceof TableQueryStreamSpec) {
            TableQueryStreamSpec tableSpec = (TableQueryStreamSpec) filterStreamSpec;
            validateContextAssociation(statement.getStatementRawInfo().getContextName(), tableSpec.getTable().getOptionalContextName(), "table '" + tableSpec.getTable().getTableName() + "'");
            boolean fullTableScan = HintEnum.SET_NOINDEX.getHint(annotations) != null;
            ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(allStreamNames, statement.getStatementRawInfo(), services);
            SubordPropPlan joinedPropPlan = QueryPlanIndexBuilder.getJoinProps(filterExpr, outerEventTypes.length, subselectTypeService.getEventTypes(), excludePlanHint);
            SubSelectStrategyFactoryIndexShareForge strategyForge = new SubSelectStrategyFactoryIndexShareForge(subqueryNum, subselectActivation, outerEventTypesSelect, null, tableSpec.getTable(),
                fullTableScan, indexHint, joinedPropPlan, filterExprEval, groupByNodes, aggregationServiceForgeDesc, statement, services);
            additionalForgeables.addAll(strategyForge.getAdditionalForgeables());
            SubSelectFactoryForge forge = new SubSelectFactoryForge(subqueryNum, subselectActivation.getActivator(), strategyForge);
            return new SubSelectFactoryForgeDesc(forge, additionalForgeables);
        }

        // determine unique keys, if any
        Set<String> optionalUniqueProps = StreamJoinAnalysisResultCompileTime.getUniqueCandidateProperties(viewForges, annotations);
        NamedWindowMetaData namedWindow = null;
        ExprNode namedWindowFilterExpr = null;
        QueryGraphForge namedWindowFilterQueryGraph = null;
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) filterStreamSpec;
            namedWindow = namedSpec.getNamedWindow();
            optionalUniqueProps = namedWindow.getUniquenessAsSet();
            if (namedSpec.getFilterExpressions() != null && !namedSpec.getFilterExpressions().isEmpty()) {
                StreamTypeServiceImpl types = new StreamTypeServiceImpl(namedWindow.getEventType(), namedWindow.getEventType().getName(), false);
                namedWindowFilterExpr = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(namedSpec.getFilterExpressions());
                namedWindowFilterQueryGraph = EPLValidationUtil.validateFilterGetQueryGraphSafe(namedWindowFilterExpr, types, statement.getStatementRawInfo(), services);
            }
        }

        // handle local stream + named-window-stream
        boolean fullTableScan = HintEnum.SET_NOINDEX.getHint(annotations) != null;
        SubqueryIndexForgeDesc indexDesc = determineSubqueryIndexFactory(filterExpr, eventType,
            outerEventTypes, subselectTypeService, fullTableScan, queryPlanLogging, optionalUniqueProps, statement, subselect, services);
        additionalForgeables.addAll(indexDesc.getAdditionalForgeables());
        Pair<EventTableFactoryFactoryForge, SubordTableLookupStrategyFactoryForge> indexPair = new Pair<>(indexDesc.getTableForge(), indexDesc.getLookupForge());

        SubSelectStrategyFactoryForge strategyForge = new SubSelectStrategyFactoryLocalViewPreloadedForge(viewForges, viewResourceDelegateDesc, indexPair,
            filterExpr, correlatedSubquery, aggregationServiceForgeDesc,  /* viewResourceDelegateVerified */ subqueryNum, groupByNodes, namedWindow,
            namedWindowFilterExpr, namedWindowFilterQueryGraph, groupByMultikeyPlan == null ? null : groupByMultikeyPlan.getClassRef());

        SubSelectFactoryForge forge = new SubSelectFactoryForge(subqueryNum, subselectActivation.getActivator(), strategyForge);
        return new SubSelectFactoryForgeDesc(forge, additionalForgeables);
    }

    private static void validateSubqueryDataWindow(ExprSubselectNode subselectNode, boolean correlatedSubquery, boolean hasNonAggregatedProperties, ExprNodePropOrStreamSet propertiesGroupBy, ExprNodePropOrStreamSet nonAggregatedPropsSelect)
        throws ExprValidationException {
        // validation applies only to type+filter subqueries that have no data window
        StreamSpecCompiled streamSpec = subselectNode.getStatementSpecCompiled().getStreamSpecs()[0];
        if (!(streamSpec instanceof FilterStreamSpecCompiled) || streamSpec.getViewSpecs().length > 0) {
            return;
        }

        if (correlatedSubquery) {
            throw new ExprValidationException(MSG_SUBQUERY_REQUIRES_WINDOW);
        }

        // we have non-aggregated properties
        if (hasNonAggregatedProperties) {
            if (propertiesGroupBy == null) {
                throw new ExprValidationException(MSG_SUBQUERY_REQUIRES_WINDOW);
            }

            String reason = nonAggregatedPropsSelect.notContainsAll(propertiesGroupBy);
            if (reason != null) {
                throw new ExprValidationException(MSG_SUBQUERY_REQUIRES_WINDOW);
            }
        }
    }

    private static void validateAggregationPropsAndLocalGroup(List<ExprAggregateNode> aggregateNodes) throws ExprValidationException {
        for (ExprAggregateNode aggNode : aggregateNodes) {
            List<Pair<Integer, String>> propertiesNodesAggregated = ExprNodeUtilityQuery.getExpressionProperties(aggNode, true);
            for (Pair<Integer, String> pair : propertiesNodesAggregated) {
                if (pair.getFirst() != 0) {
                    throw new ExprValidationException("Subselect aggregation functions cannot aggregate across correlated properties");
                }
            }

            if (aggNode.getOptionalLocalGroupBy() != null) {
                throw new ExprValidationException("Subselect aggregations functions cannot specify a group-by");
            }
        }
    }

    private static SubqueryIndexForgeDesc determineSubqueryIndexFactory(ExprNode filterExpr,
                                                                        EventType viewableEventType,
                                                                        EventType[] outerEventTypes,
                                                                        StreamTypeService subselectTypeService,
                                                                        boolean fullTableScan,
                                                                        boolean queryPlanLogging,
                                                                        Set<String> optionalUniqueProps,
                                                                        StatementBaseInfo statement,
                                                                        ExprSubselectNode subselect,
                                                                        StatementCompileTimeServices services)
        throws ExprValidationException {
        SubqueryIndexForgeDesc desc = determineSubqueryIndexInternalFactory(filterExpr, viewableEventType, outerEventTypes, subselectTypeService, fullTableScan, optionalUniqueProps, statement, subselect, services);

        QueryPlanIndexHook hook = QueryPlanIndexHookUtil.getHook(statement.getStatementSpec().getAnnotations(), services.getClasspathImportServiceCompileTime());
        if (queryPlanLogging && (QUERY_PLAN_LOG.isInfoEnabled() || hook != null)) {
            QUERY_PLAN_LOG.info("local index");
            QUERY_PLAN_LOG.info("strategy " + desc.getLookupForge().toQueryPlan());
            QUERY_PLAN_LOG.info("table " + desc.getTableForge().toQueryPlan());
            if (hook != null) {
                String strategyName = desc.getLookupForge().getClass().getSimpleName();
                hook.subquery(new QueryPlanIndexDescSubquery(
                    new IndexNameAndDescPair[]{
                        new IndexNameAndDescPair(null, desc.getTableForge().getEventTableClass().getSimpleName())
                    }, subselect.getSubselectNumber(), strategyName));
            }
        }

        return desc;
    }

    private static String validateContextAssociation(String optionalProvidedContextName, String entityDeclaredContextName, String entityDesc)
        throws ExprValidationException {
        if (entityDeclaredContextName != null) {
            if (optionalProvidedContextName == null || !optionalProvidedContextName.equals(entityDeclaredContextName)) {
                throw new ExprValidationException("Mismatch in context specification, the context for the " + entityDesc + " is '" +
                    entityDeclaredContextName + "' and the query specifies " +
                    (optionalProvidedContextName == null ? "no context " :
                        "context '" + optionalProvidedContextName + "'"));
            }
        }
        return null;
    }

    private static SubqueryIndexForgeDesc determineSubqueryIndexInternalFactory(ExprNode filterExpr,
                                                                                EventType viewableEventType,
                                                                                EventType[] outerEventTypes,
                                                                                StreamTypeService subselectTypeService,
                                                                                boolean fullTableScan,
                                                                                Set<String> optionalUniqueProps,
                                                                                StatementBaseInfo statement,
                                                                                ExprSubselectNode subselectNode,
                                                                                StatementCompileTimeServices services)
        throws ExprValidationException {
        int subqueryNumber = subselectNode.getSubselectNumber();

        // No filter expression means full table scan
        if ((filterExpr == null) || fullTableScan) {
            UnindexedEventTableFactoryFactoryForge tableForge = new UnindexedEventTableFactoryFactoryForge(0, subqueryNumber, false);
            SubordFullTableScanLookupStrategyFactoryForge strategy = new SubordFullTableScanLookupStrategyFactoryForge();
            return new SubqueryIndexForgeDesc(tableForge, strategy, Collections.emptyList());
        }

        // Build a list of streams and indexes
        ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(subselectTypeService.getStreamNames(), statement.getStatementRawInfo(), services);
        SubordPropPlan joinPropDesc = QueryPlanIndexBuilder.getJoinProps(filterExpr, outerEventTypes.length, subselectTypeService.getEventTypes(), excludePlanHint);
        Map<String, SubordPropHashKeyForge> hashKeys = joinPropDesc.getHashProps();
        Map<String, SubordPropRangeKeyForge> rangeKeys = joinPropDesc.getRangeProps();
        List<SubordPropHashKeyForge> hashKeyList = new ArrayList<SubordPropHashKeyForge>(hashKeys.values());
        List<SubordPropRangeKeyForge> rangeKeyList = new ArrayList<SubordPropRangeKeyForge>(rangeKeys.values());
        boolean unique = false;
        ExprNode[] inKeywordSingleIdxKeys = null;
        ExprNode inKeywordMultiIdxKey = null;

        // If this is a unique-view and there are unique criteria, use these
        if (optionalUniqueProps != null && !optionalUniqueProps.isEmpty()) {
            boolean found = true;
            for (String uniqueProp : optionalUniqueProps) {
                if (!hashKeys.containsKey(uniqueProp)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                String[] hashKeysArray = hashKeys.keySet().toArray(new String[hashKeys.keySet().size()]);
                for (String hashKey : hashKeysArray) {
                    if (!optionalUniqueProps.contains(hashKey)) {
                        hashKeys.remove(hashKey);
                    }
                }
                hashKeyList = new ArrayList<SubordPropHashKeyForge>(hashKeys.values());
                unique = true;
                rangeKeyList.clear();
                rangeKeys.clear();
            }
        }

        // build table (local table)
        EventTableFactoryFactoryForge eventTableFactory;
        CoercionDesc hashCoercionDesc;
        CoercionDesc rangeCoercionDesc;
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        MultiKeyClassRef hashMultikeyClasses = null;
        if (hashKeys.size() != 0 && rangeKeys.isEmpty()) {
            String[] indexedProps = hashKeys.keySet().toArray(new String[hashKeys.keySet().size()]);
            hashCoercionDesc = CoercionUtil.getCoercionTypesHash(viewableEventType, indexedProps, hashKeyList);
            rangeCoercionDesc = new CoercionDesc(false, null);
            MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(hashCoercionDesc.getCoercionTypes(), false, statement.getStatementRawInfo(), services.getSerdeResolver());
            additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());
            hashMultikeyClasses = multiKeyPlan.getClassRef();
            eventTableFactory = new PropertyHashedFactoryFactoryForge(0, subqueryNumber, false, indexedProps, viewableEventType, unique, hashCoercionDesc, multiKeyPlan.getClassRef());
        } else if (hashKeys.isEmpty() && rangeKeys.isEmpty()) {
            rangeCoercionDesc = new CoercionDesc(false, null);
            if (joinPropDesc.getInKeywordSingleIndex() != null) {
                String prop = joinPropDesc.getInKeywordSingleIndex().getIndexedProp();
                Class[] propTypes = new Class[]{viewableEventType.getPropertyType(prop)};
                hashCoercionDesc = new CoercionDesc(false, propTypes);
                DataInputOutputSerdeForge serdeForge = services.getSerdeResolver().serdeForIndexHashNonArray(propTypes[0], statement.getStatementRawInfo());
                hashMultikeyClasses = new MultiKeyClassRefWSerde(serdeForge, propTypes);
                eventTableFactory = new PropertyHashedFactoryFactoryForge(0, subqueryNumber, false, new String[]{prop}, viewableEventType, unique, hashCoercionDesc, hashMultikeyClasses);
                inKeywordSingleIdxKeys = joinPropDesc.getInKeywordSingleIndex().getExpressions();
            } else if (joinPropDesc.getInKeywordMultiIndex() != null) {
                String[] props = joinPropDesc.getInKeywordMultiIndex().getIndexedProp();
                hashCoercionDesc = new CoercionDesc(false, EventTypeUtility.getPropertyTypes(viewableEventType, props));
                DataInputOutputSerdeForge[] serdes = new DataInputOutputSerdeForge[hashCoercionDesc.getCoercionTypes().length];
                for (int i = 0; i < hashCoercionDesc.getCoercionTypes().length; i++) {
                    serdes[i] = services.getSerdeResolver().serdeForIndexHashNonArray(hashCoercionDesc.getCoercionTypes()[i], statement.getStatementRawInfo());
                }
                eventTableFactory = new PropertyHashedArrayFactoryFactoryForge(0, viewableEventType, props, hashCoercionDesc.getCoercionTypes(), serdes, unique, false);
                inKeywordMultiIdxKey = joinPropDesc.getInKeywordMultiIndex().getExpression();
            } else {
                hashCoercionDesc = new CoercionDesc(false, null);
                eventTableFactory = new UnindexedEventTableFactoryFactoryForge(0, subqueryNumber, false);
            }
        } else if (hashKeys.isEmpty() && rangeKeys.size() == 1) {
            String indexedProp = rangeKeys.keySet().iterator().next();
            CoercionDesc coercionRangeTypes = CoercionUtil.getCoercionTypesRange(viewableEventType, rangeKeys, outerEventTypes);
            DataInputOutputSerdeForge serde = services.getSerdeResolver().serdeForIndexBtree(coercionRangeTypes.getCoercionTypes()[0], statement.getStatementRawInfo());
            eventTableFactory = new PropertySortedFactoryFactoryForge(0, subqueryNumber, false, indexedProp, viewableEventType, coercionRangeTypes, serde);
            hashCoercionDesc = new CoercionDesc(false, null);
            rangeCoercionDesc = coercionRangeTypes;
        } else {
            String[] indexedKeyProps = hashKeys.keySet().toArray(new String[hashKeys.keySet().size()]);
            Class[] coercionKeyTypes = SubordPropUtil.getCoercionTypes(hashKeys.values());
            MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(coercionKeyTypes, false, statement.getStatementRawInfo(), services.getSerdeResolver());
            additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());
            hashMultikeyClasses = multiKeyPlan.getClassRef();
            String[] indexedRangeProps = rangeKeys.keySet().toArray(new String[rangeKeys.keySet().size()]);
            CoercionDesc coercionRangeTypes = CoercionUtil.getCoercionTypesRange(viewableEventType, rangeKeys, outerEventTypes);
            DataInputOutputSerdeForge[] rangeSerdes = new DataInputOutputSerdeForge[coercionRangeTypes.getCoercionTypes().length];
            for (int i = 0; i < coercionRangeTypes.getCoercionTypes().length; i++) {
                rangeSerdes[i] = services.getSerdeResolver().serdeForIndexBtree(coercionRangeTypes.getCoercionTypes()[i], statement.getStatementRawInfo());
            }
            eventTableFactory = new PropertyCompositeEventTableFactoryFactoryForge(0, subqueryNumber, false, indexedKeyProps, coercionKeyTypes, hashMultikeyClasses, indexedRangeProps, coercionRangeTypes.getCoercionTypes(), rangeSerdes, viewableEventType);
            hashCoercionDesc = CoercionUtil.getCoercionTypesHash(viewableEventType, indexedKeyProps, hashKeyList);
            rangeCoercionDesc = coercionRangeTypes;
        }

        SubordTableLookupStrategyFactoryForge subqTableLookupStrategyFactory = SubordinateTableLookupStrategyUtil.getLookupStrategy(outerEventTypes,
            hashKeyList, hashCoercionDesc, hashMultikeyClasses, rangeKeyList, rangeCoercionDesc, inKeywordSingleIdxKeys, inKeywordMultiIdxKey, false);

        return new SubqueryIndexForgeDesc(eventTableFactory, subqTableLookupStrategyFactory, additionalForgeables);
    }

    private static StreamTypeService getDeclaredExprTypeService(ExprDeclaredNode[] declaredExpressions,
                                                                Map<ExprDeclaredNode, List<ExprDeclaredNode>> declaredExpressionCallHierarchy,
                                                                String[] outerStreamNames,
                                                                EventType[] outerEventTypesSelect,
                                                                ExprSubselectNode subselect,
                                                                String subexpressionStreamName,
                                                                EventType eventType)
        throws ExprValidationException {
        // Find that subselect within that any of the expression declarations
        for (ExprDeclaredNode declaration : declaredExpressions) {
            ExprNodeSubselectDeclaredNoTraverseVisitor visitor = new ExprNodeSubselectDeclaredNoTraverseVisitor(declaration);
            visitor.reset();
            declaration.acceptNoVisitParams(visitor);
            if (!visitor.getSubselects().contains(subselect)) {
                continue;
            }

            // no type service for "alias"
            if (declaration.getPrototype().isAlias()) {
                return null;
            }

            // subselect found - compute outer stream names
            // initialize from the outermost provided stream names
            Map<String, Integer> outerStreamNamesMap = new LinkedHashMap<String, Integer>();
            int count = 0;
            for (String outerStreamName : outerStreamNames) {
                outerStreamNamesMap.put(outerStreamName, count++);
            }

            // give each declared expression a chance to change the names (unless alias expression)
            Map<String, Integer> outerStreamNamesForSubselect = outerStreamNamesMap;
            List<ExprDeclaredNode> callers = declaredExpressionCallHierarchy.get(declaration);
            for (ExprDeclaredNode caller : callers) {
                outerStreamNamesForSubselect = caller.getOuterStreamNames(outerStreamNamesForSubselect);
            }
            outerStreamNamesForSubselect = declaration.getOuterStreamNames(outerStreamNamesForSubselect);

            // compile a new StreamTypeService for use in validating that particular subselect
            EventType[] eventTypes = new EventType[outerStreamNamesForSubselect.size() + 1];
            String[] streamNames = new String[outerStreamNamesForSubselect.size() + 1];
            eventTypes[0] = eventType;
            streamNames[0] = subexpressionStreamName;
            count = 0;
            for (Map.Entry<String, Integer> entry : outerStreamNamesForSubselect.entrySet()) {
                eventTypes[count + 1] = outerEventTypesSelect[entry.getValue()];
                streamNames[count + 1] = entry.getKey();
                count++;
            }

            StreamTypeServiceImpl availableTypes = new StreamTypeServiceImpl(eventTypes, streamNames, new boolean[eventTypes.length], false, false);
            availableTypes.setRequireStreamNames(true);
            return availableTypes;
        }
        return null;
    }
}
