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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.annotation.IterableUnbound;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.util.ContextPropertyRegistry;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDescForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupLevelForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceForgeDesc;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupPerLevelForge;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupPlanDesc;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupPlanHook;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeGroupKey;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.common.internal.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.expression.visitor.*;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactoryFactory;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.agggrouped.ResultSetProcessorAggregateGroupedForge;
import com.espertech.esper.common.internal.epl.resultset.handthru.ResultSetProcessorHandThroughFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.order.OrderByElementForge;
import com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.rowforall.ResultSetProcessorRowForAllForge;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEventForge;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroupForge;
import com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.*;
import com.espertech.esper.common.internal.epl.resultset.simple.ResultSetProcessorSimpleForge;
import com.espertech.esper.common.internal.epl.streamtype.PropertyResolutionDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypesException;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.event.core.NativeEventType;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Factory for output processors. Output processors process the result set of a join or of a view
 * and apply aggregation/grouping, having and some output limiting logic.
 * <p>
 * The instance produced by the factory depends on the presence of aggregation functions in the select list,
 * the presence and nature of the group-by clause.
 * <p>
 * In case (1) and (2) there are no aggregation functions in the select clause.
 * <p>
 * Case (3) is without group-by and with aggregation functions and without non-aggregated properties
 * in the select list: <pre>select sum(volume) </pre>.
 * Always produces one row for new and old data, aggregates without grouping.
 * <p>
 * Case (4) is without group-by and with aggregation functions but with non-aggregated properties
 * in the select list: <pre>select price, sum(volume) </pre>.
 * Produces a row for each event, aggregates without grouping.
 * <p>
 * Case (5) is with group-by and with aggregation functions and all selected properties are grouped-by.
 * in the select list: <pre>select customerId, sum(volume) group by customerId</pre>.
 * Produces a old and new data row for each group changed, aggregates with grouping.
 * <p>
 * Case (6) is with group-by and with aggregation functions and only some selected properties are grouped-by.
 * in the select list: <pre>select customerId, supplierId, sum(volume) group by customerId</pre>.
 * Produces row for each event, aggregates with grouping.
 */
public class ResultSetProcessorFactoryFactory {
    private static final Logger log = LoggerFactory.getLogger(ResultSetProcessorFactoryFactory.class);

    public static ResultSetProcessorDesc getProcessorPrototype(ResultSetSpec spec,
                                                               StreamTypeService typeService,
                                                               ViewResourceDelegateExpr viewResourceDelegate,
                                                               boolean[] isUnidirectionalStream,
                                                               boolean allowAggregation,
                                                               ContextPropertyRegistry contextPropertyRegistry,
                                                               boolean isFireAndForget,
                                                               boolean isOnSelect,
                                                               StatementRawInfo statementRawInfo,
                                                               StatementCompileTimeServices services)
        throws ExprValidationException {
        List<OrderByItem> orderByListUnexpanded = spec.getOrderByList();
        SelectClauseSpecCompiled selectClauseSpec = spec.getSelectClauseSpec();
        InsertIntoDesc insertIntoDesc = spec.getInsertIntoDesc();
        ExprNode optionalHavingNode = spec.getHavingClause();
        OutputLimitSpec outputLimitSpec = spec.getOptionalOutputLimitSpec();
        GroupByClauseExpressions groupByClauseExpressions = spec.getGroupByClauseExpressions();
        List<ExprDeclaredNode> declaredNodes = new ArrayList<>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // validate output limit spec
        validateOutputLimit(outputLimitSpec, statementRawInfo, services);

        // determine unidirectional
        boolean isUnidirectional = false;
        for (int i = 0; i < isUnidirectionalStream.length; i++) {
            isUnidirectional |= isUnidirectionalStream[i];
        }

        // determine single-stream historical
        boolean isHistoricalOnly = false;
        if (spec.getStreamSpecs().length == 1) {
            StreamSpecCompiled streamSpec = spec.getStreamSpecs()[0];
            if (streamSpec instanceof DBStatementStreamSpec || streamSpec instanceof MethodStreamSpec || streamSpec instanceof TableQueryStreamSpec) {
                isHistoricalOnly = true;
            }
        }

        // determine join or number of streams
        int numStreams = typeService.getEventTypes().length;
        boolean join = numStreams > 1;

        // Expand any instances of select-clause names in the
        // order-by clause with the full expression
        List<OrderByItem> orderByList = expandColumnNames(selectClauseSpec.getSelectExprList(), orderByListUnexpanded);

        // Validate selection expressions, if any (could be wildcard i.e. empty list)
        List<SelectClauseExprCompiledSpec> namedSelectionList = new LinkedList<SelectClauseExprCompiledSpec>();
        boolean allowRollup = groupByClauseExpressions != null && groupByClauseExpressions.getGroupByRollupLevels() != null;
        boolean resettableAggs = isUnidirectional || statementRawInfo.getStatementType().isOnTriggerInfra();
        String intoTableName = spec.getIntoTableSpec() == null ? null : spec.getIntoTableSpec().getName();
        ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, statementRawInfo, services)
            .withViewResourceDelegate(viewResourceDelegate).withAllowRollupFunctions(allowRollup).withAllowBindingConsumption(true)
            .withIsResettingAggregations(resettableAggs).withIntoTableName(intoTableName).build();

        validateSelectAssignColNames(selectClauseSpec, namedSelectionList, validationContext);
        if (spec.getGroupByClauseExpressions() != null && spec.getGroupByClauseExpressions().getSelectClausePerLevel() != null) {
            ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.GROUPBY, spec.getGroupByClauseExpressions().getSelectClausePerLevel(), validationContext);
        }
        boolean isUsingWildcard = selectClauseSpec.isUsingWildcard();

        // Validate stream selections, if any (such as stream.*)
        boolean isUsingStreamSelect = false;
        for (SelectClauseElementCompiled compiled : selectClauseSpec.getSelectExprList()) {
            if (!(compiled instanceof SelectClauseStreamCompiledSpec)) {
                continue;
            }
            SelectClauseStreamCompiledSpec streamSelectSpec = (SelectClauseStreamCompiledSpec) compiled;
            int streamNum = Integer.MIN_VALUE;
            boolean isFragmentEvent = false;
            boolean isProperty = false;
            Class propertyType = null;
            isUsingStreamSelect = true;
            for (int i = 0; i < typeService.getStreamNames().length; i++) {
                String streamName = streamSelectSpec.getStreamName();
                if (typeService.getStreamNames()[i].equals(streamName)) {
                    streamNum = i;
                    break;
                }

                // see if the stream name is known as a nested event type
                EventType candidateProviderOfFragments = typeService.getEventTypes()[i];
                // for the native event type we don't need to fragment, we simply use the property itself since all wrappers understand Java objects
                if (!(candidateProviderOfFragments instanceof NativeEventType) && (candidateProviderOfFragments.getFragmentType(streamName) != null)) {
                    streamNum = i;
                    isFragmentEvent = true;
                    break;
                }
            }

            // stream name not found
            if (streamNum == Integer.MIN_VALUE) {
                // see if the stream name specified resolves as a property
                PropertyResolutionDescriptor desc = null;
                try {
                    desc = typeService.resolveByPropertyName(streamSelectSpec.getStreamName(), false);
                } catch (StreamTypesException e) {
                    // not handled
                }

                if (desc == null) {
                    throw new ExprValidationException("Stream selector '" + streamSelectSpec.getStreamName() + ".*' does not match any stream name in the from clause");
                }
                isProperty = true;
                propertyType = desc.getPropertyType();
                streamNum = desc.getStreamNum();
            }

            streamSelectSpec.setStreamNumber(streamNum);
            streamSelectSpec.setFragmentEvent(isFragmentEvent);
            streamSelectSpec.setProperty(isProperty, propertyType);

            if (streamNum >= 0) {
                TableMetaData table = services.getTableCompileTimeResolver().resolveTableFromEventType(typeService.getEventTypes()[streamNum]);
                streamSelectSpec.setTableMetadata(table);
            }
        }

        // Validate having clause, if present
        if (optionalHavingNode != null) {
            optionalHavingNode = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.HAVING, optionalHavingNode, validationContext);
            if (spec.getGroupByClauseExpressions() != null) {
                ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.GROUPBY, spec.getGroupByClauseExpressions().getOptHavingNodePerLevel(), validationContext);
            }
        }

        // Validate order-by expressions, if any (could be empty list for no order-by)
        for (int i = 0; i < orderByList.size(); i++) {
            ExprNode orderByNode = orderByList.get(i).getExprNode();

            // Ensure there is no subselects
            ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
            orderByNode.accept(visitor);
            if (visitor.getSubselects().size() > 0) {
                throw new ExprValidationException("Subselects not allowed within order-by clause");
            }

            Boolean isDescending = orderByList.get(i).isDescending();
            OrderByItem validatedOrderBy = new OrderByItem(ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.ORDERBY, orderByNode, validationContext), isDescending);
            orderByList.set(i, validatedOrderBy);

            if (spec.getGroupByClauseExpressions() != null && spec.getGroupByClauseExpressions().getOptOrderByPerLevel() != null) {
                ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.GROUPBY, spec.getGroupByClauseExpressions().getOptOrderByPerLevel(), validationContext);
            }
        }

        // Get the select expression nodes
        List<ExprNode> selectNodes = new ArrayList<ExprNode>();
        for (SelectClauseExprCompiledSpec element : namedSelectionList) {
            selectNodes.add(element.getSelectExpression());
        }

        // Get the order-by expression nodes
        List<ExprNode> orderByNodes = new ArrayList<ExprNode>();
        for (OrderByItem element : orderByList) {
            orderByNodes.add(element.getExprNode());
        }

        // Determine aggregate functions used in select, if any
        List<ExprAggregateNode> selectAggregateExprNodes = new LinkedList<ExprAggregateNode>();
        Map<ExprNode, String> selectAggregationNodesNamed = new HashMap<ExprNode, String>();
        ExprNodeDeclaredVisitor declaredNodeVisitor = new ExprNodeDeclaredVisitor();
        for (SelectClauseExprCompiledSpec element : namedSelectionList) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(element.getSelectExpression(), selectAggregateExprNodes);
            if (element.getProvidedName() != null) {
                selectAggregationNodesNamed.put(element.getSelectExpression(), element.getProvidedName());
            }
            element.getSelectExpression().accept(declaredNodeVisitor);
            declaredNodes.addAll(declaredNodeVisitor.getDeclaredExpressions());
            declaredNodeVisitor.clear();
        }
        if (spec.getGroupByClauseExpressions() != null) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(spec.getGroupByClauseExpressions().getSelectClausePerLevel(), selectAggregateExprNodes);
        }
        if (!allowAggregation && !selectAggregateExprNodes.isEmpty()) {
            throw new ExprValidationException("Aggregation functions are not allowed in this context");
        }

        // Determine if we have a having clause with aggregation
        List<ExprAggregateNode> havingAggregateExprNodes = new LinkedList<ExprAggregateNode>();
        ExprNodePropOrStreamSet propertiesAggregatedHaving = new ExprNodePropOrStreamSet();
        if (optionalHavingNode != null) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(optionalHavingNode, havingAggregateExprNodes);
            if (groupByClauseExpressions != null) {
                ExprAggregateNodeUtil.getAggregatesBottomUp(groupByClauseExpressions.getOptHavingNodePerLevel(), havingAggregateExprNodes);
            }
            propertiesAggregatedHaving = ExprNodeUtilityAggregation.getAggregatedProperties(havingAggregateExprNodes);
        }
        if (!allowAggregation && !havingAggregateExprNodes.isEmpty()) {
            throw new ExprValidationException("Aggregation functions are not allowed in this context");
        }

        // Determine if we have a order-by clause with aggregation
        List<ExprAggregateNode> orderByAggregateExprNodes = new LinkedList<ExprAggregateNode>();
        if (orderByNodes != null && !orderByNodes.isEmpty()) {
            for (ExprNode orderByNode : orderByNodes) {
                ExprAggregateNodeUtil.getAggregatesBottomUp(orderByNode, orderByAggregateExprNodes);
            }
            if (groupByClauseExpressions != null) {
                ExprAggregateNodeUtil.getAggregatesBottomUp(groupByClauseExpressions.getOptOrderByPerLevel(), orderByAggregateExprNodes);
            }
            if (!allowAggregation && !orderByAggregateExprNodes.isEmpty()) {
                throw new ExprValidationException("Aggregation functions are not allowed in this context");
            }
        }

        // Analyze rollup
        GroupByRollupInfo groupByRollupInfo = analyzeValidateGroupBy(groupByClauseExpressions, validationContext);

        ExprNode[] groupByNodesValidated;
        AggregationGroupByRollupDescForge groupByRollupDesc;
        MultiKeyClassRef groupByMultiKey;
        if (groupByRollupInfo == null) {
            groupByNodesValidated = ExprNodeUtilityQuery.EMPTY_EXPR_ARRAY;
            groupByRollupDesc = null;
            groupByMultiKey = null;
        } else {
            groupByNodesValidated = groupByRollupInfo.getExprNodes();
            groupByRollupDesc = groupByRollupInfo.getRollupDesc();
            groupByMultiKey = groupByRollupInfo.getOptionalMultiKey();
            additionalForgeables.addAll(groupByRollupInfo.getAdditionalForgeables());
        }

        // Construct the appropriate aggregation service
        boolean hasGroupBy = groupByNodesValidated.length > 0;
        AggregationServiceForgeDesc aggregationServiceForgeDesc = AggregationServiceFactoryFactory.getService(
            selectAggregateExprNodes, selectAggregationNodesNamed, declaredNodes, groupByNodesValidated, groupByMultiKey,
            havingAggregateExprNodes, orderByAggregateExprNodes, Collections.<ExprAggregateNodeGroupKey>emptyList(), hasGroupBy,
            statementRawInfo.getAnnotations(), services.getVariableCompileTimeResolver(), false,
            spec.getWhereClause(), spec.getHavingClause(),
            typeService.getEventTypes(), groupByRollupDesc,
            spec.getContextName(), spec.getIntoTableSpec(), services.getTableCompileTimeResolver(),
            isUnidirectional, isFireAndForget, isOnSelect,
            services.getClasspathImportServiceCompileTime(), statementRawInfo, services.getSerdeResolver());
        additionalForgeables.addAll(aggregationServiceForgeDesc.getAdditionalForgeables());

        // Compare local-aggregation versus group-by
        boolean localGroupByMatchesGroupBy = analyzeLocalGroupBy(groupByNodesValidated, selectAggregateExprNodes, havingAggregateExprNodes, orderByAggregateExprNodes);

        // Construct the processor for evaluating the select clause
        SelectProcessorArgs args = new SelectProcessorArgs(selectClauseSpec.getSelectExprList(), groupByRollupInfo, isUsingWildcard, null, spec.getForClauseSpec(), typeService,
            null, isFireAndForget, spec.getAnnotations(), statementRawInfo, services);
        SelectExprProcessorDescriptor selectExprProcessorDesc = SelectExprProcessorFactory.getProcessor(args, insertIntoDesc, true);
        SelectExprProcessorForge selectExprProcessorForge = selectExprProcessorDesc.getForge();
        additionalForgeables.addAll(selectExprProcessorDesc.getAdditionalForgeables());
        SelectSubscriberDescriptor selectSubscriberDescriptor = selectExprProcessorDesc.getSubscriberDescriptor();
        EventType resultEventType = selectExprProcessorForge.getResultEventType();

        // compute rollup if applicable
        GroupByRollupPerLevelForge rollupPerLevelForges = null;
        if (groupByRollupDesc != null) {
            rollupPerLevelForges = getRollUpPerLevelExpressions(spec, groupByNodesValidated, groupByRollupDesc, groupByRollupInfo, insertIntoDesc, typeService, validationContext, isFireAndForget, statementRawInfo, services);
        }

        // Construct the processor for sorting output events
        OrderByProcessorFactoryForge orderByProcessorFactory = OrderByProcessorFactoryFactory.getProcessor(namedSelectionList,
            orderByList, spec.getRowLimitSpec(), services.getVariableCompileTimeResolver(), services.getConfiguration().getCompiler().getLanguage().isSortUsingCollator(),
            spec.getContextName(), rollupPerLevelForges == null ? null : rollupPerLevelForges.getOptionalOrderByElements());
        boolean hasOrderBy = orderByProcessorFactory != null;

        // Get a list of event properties being aggregated in the select clause, if any
        ExprNodePropOrStreamSet propertiesGroupBy = ExprNodeUtilityAggregation.getGroupByPropertiesValidateHasOne(groupByNodesValidated);
        // Figure out all non-aggregated event properties in the select clause (props not under a sum/avg/max aggregation node)
        ExprNodePropOrStreamSet nonAggregatedPropsSelect = ExprNodeUtilityAggregation.getNonAggregatedProps(typeService.getEventTypes(), selectNodes, contextPropertyRegistry);
        if (optionalHavingNode != null) {
            ExprNodeUtilityAggregation.addNonAggregatedProps(optionalHavingNode, nonAggregatedPropsSelect, typeService.getEventTypes(), contextPropertyRegistry);
        }

        // Validate the having-clause (selected aggregate nodes and all in group-by are allowed)
        boolean isAggregated = (!selectAggregateExprNodes.isEmpty()) || (!havingAggregateExprNodes.isEmpty()) || (!orderByAggregateExprNodes.isEmpty()) || (!propertiesAggregatedHaving.isEmpty());
        if (optionalHavingNode != null && isAggregated) {
            validateHaving(propertiesGroupBy, optionalHavingNode);
        }

        // We only generate Remove-Stream events if they are explicitly selected, or the insert-into requires them
        boolean isSelectRStream = spec.getSelectClauseStreamSelector() == SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH || spec.getSelectClauseStreamSelector() == SelectClauseStreamSelectorEnum.RSTREAM_ONLY;
        if ((spec.getInsertIntoDesc() != null) && (spec.getInsertIntoDesc().getStreamSelector().isSelectsRStream())) {
            isSelectRStream = true;
        }

        ExprForge optionalHavingForge = optionalHavingNode == null ? null : optionalHavingNode.getForge();
        boolean hasOutputLimitOpt = ResultSetProcessorOutputConditionType.getOutputLimitOpt(statementRawInfo.getAnnotations(), services.getConfiguration(), hasOrderBy);
        boolean hasOutputLimitSnapshot = outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT;
        boolean isGrouped = groupByNodesValidated.length > 0 || groupByRollupDesc != null;
        ResultSetProcessorOutputConditionType outputConditionType = outputLimitSpec != null ? ResultSetProcessorOutputConditionType.getConditionType(outputLimitSpec.getDisplayLimit(), isAggregated, hasOrderBy, hasOutputLimitOpt, isGrouped) : null;

        // Determine output-first condition factory
        OutputConditionPolledFactoryForge optionalOutputFirstConditionFactoryForge = null;
        if (outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.FIRST) {
            optionalOutputFirstConditionFactoryForge = OutputConditionPolledFactoryFactory.createConditionFactory(outputLimitSpec, statementRawInfo, services);
        }
        boolean hasOutputLimit = outputLimitSpec != null;

        if (hasOutputLimitOpt && hasOutputLimit) {
            planSerdes(selectExprProcessorForge.getResultEventType(), additionalForgeables, statementRawInfo, services);
        }

        // (1)
        // There is no group-by clause and no aggregate functions with event properties in the select clause and having clause (simplest case)
        if ((groupByNodesValidated.length == 0) && (selectAggregateExprNodes.isEmpty()) && (havingAggregateExprNodes.isEmpty())) {
            // Determine if any output rate limiting must be performed early while processing results
            // Snapshot output does not count in terms of limiting output for grouping/aggregation purposes
            boolean isOutputLimitingNoSnapshot = (outputLimitSpec != null) && (outputLimitSpec.getDisplayLimit() != OutputLimitLimitType.SNAPSHOT);

            // (1a)
            // There is no need to perform select expression processing, the single view itself (no join) generates
            // events in the desired format, therefore there is no output processor. There are no order-by expressions.
            if (orderByNodes.isEmpty() && optionalHavingNode == null && !isOutputLimitingNoSnapshot && spec.getRowLimitSpec() == null) {
                log.debug(".getProcessor Using no result processor");
                ResultSetProcessorHandThroughFactoryForge forge = new ResultSetProcessorHandThroughFactoryForge(resultEventType, selectExprProcessorForge, isSelectRStream);
                return new ResultSetProcessorDesc(forge, ResultSetProcessorType.HANDTHROUGH, new SelectExprProcessorForge[]{selectExprProcessorForge},
                    join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType, false, aggregationServiceForgeDesc, orderByProcessorFactory, selectSubscriberDescriptor, additionalForgeables);
            }

            // (1b)
            // We need to process the select expression in a simple fashion, with each event (old and new)
            // directly generating one row, and no need to update aggregate state since there is no aggregate function.
            // There might be some order-by expressions.
            ResultSetProcessorSimpleForge forge = new ResultSetProcessorSimpleForge(resultEventType, selectExprProcessorForge, optionalHavingForge, isSelectRStream, outputLimitSpec, outputConditionType, hasOrderBy, typeService.getEventTypes());
            return new ResultSetProcessorDesc(forge, ResultSetProcessorType.UNAGGREGATED_UNGROUPED, new SelectExprProcessorForge[]{selectExprProcessorForge},
                join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType, false, aggregationServiceForgeDesc, orderByProcessorFactory, selectSubscriberDescriptor, additionalForgeables);
        }

        // (2)
        // A wildcard select-clause has been specified and the group-by is ignored since no aggregation functions are used, and no having clause
        boolean isLast = outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.LAST;
        boolean isFirst = outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.FIRST;
        if ((namedSelectionList.isEmpty()) && (propertiesAggregatedHaving.isEmpty()) && (havingAggregateExprNodes.isEmpty()) && !isLast && !isFirst) {
            ResultSetProcessorSimpleForge forge = new ResultSetProcessorSimpleForge(resultEventType, selectExprProcessorForge, optionalHavingForge, isSelectRStream, outputLimitSpec, outputConditionType, hasOrderBy, typeService.getEventTypes());
            return new ResultSetProcessorDesc(forge, ResultSetProcessorType.UNAGGREGATED_UNGROUPED, new SelectExprProcessorForge[]{selectExprProcessorForge},
                join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType, false, aggregationServiceForgeDesc, orderByProcessorFactory, selectSubscriberDescriptor, additionalForgeables);
        }

        if ((groupByNodesValidated.length == 0) && isAggregated) {

            // (3)
            // There is no group-by clause and there are aggregate functions with event properties in the select clause (aggregation case)
            // or having class, and all event properties are aggregated (all properties are under aggregation functions).
            boolean hasStreamSelect = ExprNodeUtilityQuery.hasStreamSelect(selectNodes);
            if ((nonAggregatedPropsSelect.isEmpty()) && !hasStreamSelect && !isUsingWildcard && !isUsingStreamSelect && localGroupByMatchesGroupBy && (viewResourceDelegate == null || viewResourceDelegate.getPreviousRequests().isEmpty())) {
                log.debug(".getProcessor Using ResultSetProcessorRowForAll");
                ResultSetProcessorRowForAllForge forge = new ResultSetProcessorRowForAllForge(resultEventType, selectExprProcessorForge, optionalHavingForge, isSelectRStream, isUnidirectional, isHistoricalOnly, outputLimitSpec, hasOrderBy, outputConditionType);
                return new ResultSetProcessorDesc(forge, ResultSetProcessorType.FULLYAGGREGATED_UNGROUPED, new SelectExprProcessorForge[]{selectExprProcessorForge},
                    join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType, false, aggregationServiceForgeDesc, orderByProcessorFactory, selectSubscriberDescriptor, additionalForgeables);
            }

            // (4)
            // There is no group-by clause but there are aggregate functions with event properties in the select clause (aggregation case)
            // or having clause and not all event properties are aggregated (some properties are not under aggregation functions).
            log.debug(".getProcessor Using ResultSetProcessorRowPerEventImpl");
            ResultSetProcessorRowPerEventForge forge = new ResultSetProcessorRowPerEventForge(selectExprProcessorForge.getResultEventType(), selectExprProcessorForge, optionalHavingForge, isSelectRStream, isUnidirectional, isHistoricalOnly, outputLimitSpec, outputConditionType, hasOrderBy);
            return new ResultSetProcessorDesc(forge, ResultSetProcessorType.AGGREGATED_UNGROUPED, new SelectExprProcessorForge[]{selectExprProcessorForge},
                join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType, false, aggregationServiceForgeDesc, orderByProcessorFactory, selectSubscriberDescriptor, additionalForgeables);
        }

        // Handle group-by cases
        if (groupByNodesValidated.length == 0) {
            throw new IllegalStateException("Unexpected empty group-by expression list");
        }

        // Figure out if all non-aggregated event properties in the select clause are listed in the group by
        boolean allInGroupBy = true;
        String notInGroupByReason = null;
        if (isUsingStreamSelect) {
            allInGroupBy = false;
            notInGroupByReason = "stream select";
        }

        String reasonMessage = propertiesGroupBy.notContainsAll(nonAggregatedPropsSelect);
        if (reasonMessage != null) {
            notInGroupByReason = reasonMessage;
            allInGroupBy = false;
        }

        // Wildcard select-clause means we do not have all selected properties in the group
        if (isUsingWildcard) {
            allInGroupBy = false;
            notInGroupByReason = "wildcard select";
        }

        // Figure out if all non-aggregated event properties in the order-by clause are listed in the select expression
        ExprNodePropOrStreamSet nonAggregatedPropsOrderBy = ExprNodeUtilityAggregation.getNonAggregatedProps(typeService.getEventTypes(), orderByNodes, contextPropertyRegistry);

        reasonMessage = nonAggregatedPropsSelect.notContainsAll(nonAggregatedPropsOrderBy);
        boolean allInSelect = reasonMessage == null;

        // Wildcard select-clause means that all order-by props in the select expression
        if (isUsingWildcard) {
            allInSelect = true;
        }

        // (4)
        // There is a group-by clause, and all event properties in the select clause that are not under an aggregation
        // function are listed in the group-by clause, and if there is an order-by clause, all non-aggregated properties
        // referred to in the order-by clause also appear in the select (output one row per group, not one row per event)
        if (allInGroupBy && allInSelect && localGroupByMatchesGroupBy) {
            boolean noDataWindowSingleStream = typeService.getIStreamOnly()[0] && typeService.getEventTypes().length < 2;
            boolean iterableUnboundConfig = services.getConfiguration().getCompiler().getViewResources().isIterableUnbound();
            boolean iterateUnbounded = noDataWindowSingleStream && (iterableUnboundConfig || AnnotationUtil.hasAnnotation(statementRawInfo.getAnnotations(), IterableUnbound.class));

            log.debug(".getProcessor Using ResultSetProcessorRowPerGroup");
            ResultSetProcessorFactoryForge forge;
            ResultSetProcessorType type;
            SelectExprProcessorForge[] selectExprProcessorForges;
            boolean rollup;

            if (groupByRollupDesc != null) {
                if (outputLimitSpec != null) {
                    planSerdes(typeService, additionalForgeables, statementRawInfo, services);
                }

                forge = new ResultSetProcessorRowPerGroupRollupForge(resultEventType, rollupPerLevelForges, groupByNodesValidated, isSelectRStream, isUnidirectional, outputLimitSpec, orderByProcessorFactory != null, noDataWindowSingleStream, groupByRollupDesc, typeService.getEventTypes().length > 1, isHistoricalOnly, iterateUnbounded, outputConditionType, optionalOutputFirstConditionFactoryForge, typeService.getEventTypes(), groupByMultiKey);
                type = ResultSetProcessorType.FULLYAGGREGATED_GROUPED_ROLLUP;
                selectExprProcessorForges = rollupPerLevelForges.getSelectExprProcessorForges();
                rollup = true;
            } else {
                boolean noDataWindowSingleSnapshot = iterateUnbounded || (outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT && noDataWindowSingleStream);
                boolean unboundedProcessor = noDataWindowSingleSnapshot && !isHistoricalOnly;
                if (unboundedProcessor) {
                    planSerdes(typeService, additionalForgeables, statementRawInfo, services);
                }
                forge = new ResultSetProcessorRowPerGroupForge(resultEventType, typeService.getEventTypes(), groupByNodesValidated, optionalHavingForge, isSelectRStream, isUnidirectional, outputLimitSpec, hasOrderBy, isHistoricalOnly, outputConditionType, typeService.getEventTypes(), optionalOutputFirstConditionFactoryForge, groupByMultiKey, unboundedProcessor);
                type = ResultSetProcessorType.FULLYAGGREGATED_GROUPED;
                selectExprProcessorForges = new SelectExprProcessorForge[]{selectExprProcessorForge};
                rollup = false;
            }
            return new ResultSetProcessorDesc(forge, type, selectExprProcessorForges,
                join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType, rollup, aggregationServiceForgeDesc, orderByProcessorFactory, selectSubscriberDescriptor, additionalForgeables);
        }

        if (groupByRollupDesc != null) {
            throw new ExprValidationException("Group-by with rollup requires a fully-aggregated query, the query is not full-aggregated because of " + notInGroupByReason);
        }

        // (6)
        // There is a group-by clause, and one or more event properties in the select clause that are not under an aggregation
        // function are not listed in the group-by clause (output one row per event, not one row per group)
        ResultSetProcessorAggregateGroupedForge forge = new ResultSetProcessorAggregateGroupedForge(resultEventType, groupByNodesValidated, optionalHavingForge, isSelectRStream, isUnidirectional, outputLimitSpec, hasOrderBy, isHistoricalOnly, outputConditionType, optionalOutputFirstConditionFactoryForge, typeService.getEventTypes(), groupByMultiKey);
        return new ResultSetProcessorDesc(forge, ResultSetProcessorType.AGGREGATED_GROUPED, new SelectExprProcessorForge[]{selectExprProcessorForge},
            join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType, false, aggregationServiceForgeDesc, orderByProcessorFactory, selectSubscriberDescriptor, additionalForgeables);
    }

    private static void planSerdes(StreamTypeService typeService, List<StmtClassForgeableFactory> additionalForgeables, StatementRawInfo raw, StatementCompileTimeServices services) {
        for (EventType eventType : typeService.getEventTypes()) {
            List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(eventType, raw, services.getSerdeEventTypeRegistry(), services.getSerdeResolver());
            additionalForgeables.addAll(serdeForgeables);
        }
    }

    private static void planSerdes(EventType eventType, List<StmtClassForgeableFactory> additionalForgeables, StatementRawInfo raw, StatementCompileTimeServices services) {
        List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(eventType, raw, services.getSerdeEventTypeRegistry(), services.getSerdeResolver());
        additionalForgeables.addAll(serdeForgeables);
    }

    private static void validateOutputLimit(OutputLimitSpec outputLimitSpec, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        if (outputLimitSpec == null) {
            return;
        }
        ExprValidationContext validationContext = new ExprValidationContextBuilder(new StreamTypeServiceImpl(false), statementRawInfo, services).build();
        if (outputLimitSpec.getAfterTimePeriodExpr() != null) {
            ExprTimePeriod timePeriodExpr = (ExprTimePeriod) ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, outputLimitSpec.getAfterTimePeriodExpr(), validationContext);
            outputLimitSpec.setAfterTimePeriodExpr(timePeriodExpr);
        }
        if (outputLimitSpec.getTimePeriodExpr() != null) {
            ExprTimePeriod timePeriodExpr = (ExprTimePeriod) ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, outputLimitSpec.getTimePeriodExpr(), validationContext);
            outputLimitSpec.setTimePeriodExpr(timePeriodExpr);
            if (timePeriodExpr.isConstantResult() && timePeriodExpr.evaluateAsSeconds(null, true, null) <= 0) {
                throw new ExprValidationException("Invalid time period expression returns a zero or negative time interval");
            }
        }
    }

    private static boolean analyzeLocalGroupBy(ExprNode[] groupByNodesValidated, List<ExprAggregateNode> selectAggregateExprNodes, List<ExprAggregateNode> havingAggregateExprNodes, List<ExprAggregateNode> orderByAggregateExprNodes) {
        boolean localGroupByMatchesGroupBy = analyzeLocalGroupBy(groupByNodesValidated, selectAggregateExprNodes);
        localGroupByMatchesGroupBy = localGroupByMatchesGroupBy && analyzeLocalGroupBy(groupByNodesValidated, havingAggregateExprNodes);
        localGroupByMatchesGroupBy = localGroupByMatchesGroupBy && analyzeLocalGroupBy(groupByNodesValidated, orderByAggregateExprNodes);
        return localGroupByMatchesGroupBy;
    }

    private static boolean analyzeLocalGroupBy(ExprNode[] groupByNodesValidated, List<ExprAggregateNode> aggNodes) {
        for (ExprAggregateNode agg : aggNodes) {
            if (agg.getOptionalLocalGroupBy() != null) {
                if (!ExprNodeUtilityCompare.deepEqualsIsSubset(agg.getOptionalLocalGroupBy().getPartitionExpressions(), groupByNodesValidated)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static GroupByRollupInfo analyzeValidateGroupBy(GroupByClauseExpressions groupBy, ExprValidationContext validationContext)
        throws ExprValidationException {
        if (groupBy == null) {
            return null;
        }

        // validate that group-by expressions are somewhat-plain expressions
        ExprNodeUtilityValidate.validateNoSpecialsGroupByExpressions(groupBy.getGroupByNodes());

        // validate each expression
        ExprNode[] validated = new ExprNode[groupBy.getGroupByNodes().length];
        for (int i = 0; i < validated.length; i++) {
            validated[i] = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.GROUPBY, groupBy.getGroupByNodes()[i], validationContext);
        }

        MultiKeyPlan groupByMKPLan = MultiKeyPlanner.planMultiKey(validated, false, validationContext.getStatementRawInfo(), validationContext.getSerdeResolver());
        if (groupBy.getGroupByRollupLevels() == null) {
            return new GroupByRollupInfo(validated, null, groupByMKPLan.getMultiKeyForgeables(), groupByMKPLan.getClassRef());
        }

        // make rollup levels
        List<AggregationGroupByRollupLevelForge> levels = new ArrayList<>();
        int countOffset = 0;
        int countNumber = -1;
        Class[] allGroupKeyTypes = ExprNodeUtilityQuery.getExprResultTypes(validated);
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(groupByMKPLan.getMultiKeyForgeables());
        for (int[] mki : groupBy.getGroupByRollupLevels()) {
            countNumber++;

            if (mki.length == 0) {
                levels.add(new AggregationGroupByRollupLevelForge(countNumber, -1, null, allGroupKeyTypes, groupByMKPLan.getClassRef(), null));
            } else {
                ExprNode[] levelExpressions = new ExprNode[mki.length];
                for (int i = 0; i < levelExpressions.length; i++) {
                    levelExpressions[i] = validated[mki[i]];
                }

                MultiKeyPlan levelMKPLan;
                if (sameExpressions(levelExpressions, validated)) {
                    levelMKPLan = groupByMKPLan;
                } else {
                    levelMKPLan = MultiKeyPlanner.planMultiKey(levelExpressions, false, validationContext.getStatementRawInfo(), validationContext.getSerdeResolver());
                    additionalForgeables.addAll(levelMKPLan.getMultiKeyForgeables());
                }
                levels.add(new AggregationGroupByRollupLevelForge(countNumber, countOffset, mki, allGroupKeyTypes, groupByMKPLan.getClassRef(), levelMKPLan.getClassRef()));
                countOffset++;
            }
        }
        AggregationGroupByRollupLevelForge[] levelsarr = levels.toArray(new AggregationGroupByRollupLevelForge[levels.size()]);
        AggregationGroupByRollupDescForge rollup = new AggregationGroupByRollupDescForge(levelsarr);

        // callback when hook reporting enabled
        try {
            GroupByRollupPlanHook hook = (GroupByRollupPlanHook) ClasspathImportUtil.getAnnotationHook(validationContext.getAnnotations(), HookType.INTERNAL_GROUPROLLUP_PLAN, GroupByRollupPlanHook.class, validationContext.getClasspathImportService());
            if (hook != null) {
                hook.query(new GroupByRollupPlanDesc(validated, rollup));
            }
        } catch (ExprValidationException e) {
            throw new EPException("Failed to obtain hook for " + HookType.INTERNAL_QUERY_PLAN);
        }

        return new GroupByRollupInfo(validated, rollup, additionalForgeables, groupByMKPLan.getClassRef());
    }

    private static GroupByRollupPerLevelForge getRollUpPerLevelExpressions(ResultSetSpec spec, ExprNode[] groupByNodesValidated, AggregationGroupByRollupDescForge groupByRollupDesc,
                                                                           GroupByRollupInfo groupByRollupInfo,
                                                                           InsertIntoDesc insertIntoDesc, StreamTypeService typeService, ExprValidationContext validationContext,
                                                                           boolean isFireAndForget, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices)
        throws ExprValidationException {
        int numLevels = groupByRollupDesc.getLevels().length;
        GroupByClauseExpressions groupByExpressions = spec.getGroupByClauseExpressions();

        // allocate
        SelectExprProcessorForge[] processors = new SelectExprProcessorForge[numLevels];
        ExprForge[] havingClauses = null;
        if (groupByExpressions.getOptHavingNodePerLevel() != null) {
            havingClauses = new ExprForge[numLevels];
        }
        OrderByElementForge[][] orderByElements = null;
        if (groupByExpressions.getOptOrderByPerLevel() != null) {
            orderByElements = new OrderByElementForge[numLevels][];
        }

        // for each expression in the group-by clause determine which properties it refers to
        ExprNodePropOrStreamSet[] propsPerGroupByExpr = new ExprNodePropOrStreamSet[groupByNodesValidated.length];
        for (int i = 0; i < groupByNodesValidated.length; i++) {
            propsPerGroupByExpr[i] = ExprNodeUtilityAggregation.getGroupByPropertiesValidateHasOne(new ExprNode[]{groupByNodesValidated[i]});
        }

        // for each level obtain a separate select expression processor
        for (int i = 0; i < numLevels; i++) {
            AggregationGroupByRollupLevelForge level = groupByRollupDesc.getLevels()[i];

            // determine properties rolled up for this level
            ExprNodePropOrStreamSet rolledupProps = getRollupProperties(level, propsPerGroupByExpr);

            ExprNode[] selectClauseLevel = groupByExpressions.getSelectClausePerLevel()[i];
            SelectClauseElementCompiled[] selectClause = getRollUpSelectClause(spec.getSelectClauseSpec(), selectClauseLevel, level, rolledupProps, groupByNodesValidated, validationContext);
            SelectProcessorArgs args = new SelectProcessorArgs(selectClause, groupByRollupInfo, false, null, spec.getForClauseSpec(), typeService,
                statementRawInfo.getOptionalContextDescriptor(), isFireAndForget, spec.getAnnotations(), statementRawInfo, compileTimeServices);
            SelectExprProcessorForge forge = SelectExprProcessorFactory.getProcessor(args, insertIntoDesc, false).getForge();
            processors[i] = forge;

            if (havingClauses != null) {
                ExprNode havingNode = rewriteRollupValidateExpression(ExprNodeOrigin.HAVING, groupByExpressions.getOptHavingNodePerLevel()[i], validationContext, rolledupProps, groupByNodesValidated, level);
                havingClauses[i] = havingNode.getForge();
            }

            if (orderByElements != null) {
                orderByElements[i] = rewriteRollupOrderBy(spec.getOrderByList(), groupByExpressions.getOptOrderByPerLevel()[i], validationContext, rolledupProps, groupByNodesValidated, level);
            }
        }

        return new GroupByRollupPerLevelForge(processors, havingClauses, orderByElements);
    }

    private static OrderByElementForge[] rewriteRollupOrderBy(List<OrderByItem> items, ExprNode[] orderByList, ExprValidationContext validationContext, ExprNodePropOrStreamSet rolledupProps, ExprNode[] groupByNodes, AggregationGroupByRollupLevelForge level)
        throws ExprValidationException {
        OrderByElementForge[] elements = new OrderByElementForge[orderByList.length];
        for (int i = 0; i < orderByList.length; i++) {
            ExprNode validated = rewriteRollupValidateExpression(ExprNodeOrigin.ORDERBY, orderByList[i], validationContext, rolledupProps, groupByNodes, level);
            elements[i] = new OrderByElementForge(validated, items.get(i).isDescending());
        }
        return elements;
    }

    private static ExprNodePropOrStreamSet getRollupProperties(AggregationGroupByRollupLevelForge level, ExprNodePropOrStreamSet[] propsPerGroupByExpr) {
        // determine properties rolled up for this level
        ExprNodePropOrStreamSet rolledupProps = new ExprNodePropOrStreamSet();
        for (int i = 0; i < propsPerGroupByExpr.length; i++) {
            if (level.isAggregationTop()) {
                rolledupProps.addAll(propsPerGroupByExpr[i]);
            } else {
                boolean rollupContainsGroupExpr = false;
                for (int num : level.getRollupKeys()) {
                    if (num == i) {
                        rollupContainsGroupExpr = true;
                        break;
                    }
                }
                if (!rollupContainsGroupExpr) {
                    rolledupProps.addAll(propsPerGroupByExpr[i]);
                }
            }
        }
        return rolledupProps;
    }

    private static SelectClauseElementCompiled[] getRollUpSelectClause(SelectClauseSpecCompiled selectClauseSpec, ExprNode[] selectClauseLevel, AggregationGroupByRollupLevelForge level, ExprNodePropOrStreamSet rolledupProps, ExprNode[] groupByNodesValidated, ExprValidationContext validationContext)
        throws ExprValidationException {
        SelectClauseElementCompiled[] rewritten = new SelectClauseElementCompiled[selectClauseSpec.getSelectExprList().length];
        for (int i = 0; i < rewritten.length; i++) {
            SelectClauseElementCompiled spec = selectClauseSpec.getSelectExprList()[i];
            if (!(spec instanceof SelectClauseExprCompiledSpec)) {
                throw new ExprValidationException("Group-by clause with roll-up does not allow wildcard");
            }

            SelectClauseExprCompiledSpec exprSpec = (SelectClauseExprCompiledSpec) spec;
            ExprNode validated = rewriteRollupValidateExpression(ExprNodeOrigin.SELECT, selectClauseLevel[i], validationContext, rolledupProps, groupByNodesValidated, level);
            rewritten[i] = new SelectClauseExprCompiledSpec(validated, exprSpec.getAssignedName(), exprSpec.getProvidedName(), exprSpec.isEvents());
        }
        return rewritten;
    }

    private static ExprNode rewriteRollupValidateExpression(ExprNodeOrigin exprNodeOrigin,
                                                            ExprNode exprNode,
                                                            ExprValidationContext validationContext,
                                                            ExprNodePropOrStreamSet rolledupProps,
                                                            ExprNode[] groupByNodes,
                                                            AggregationGroupByRollupLevelForge level)
        throws ExprValidationException {
        // rewrite grouping expressions
        ExprNodeGroupingVisitorWParent groupingVisitor = new ExprNodeGroupingVisitorWParent();
        exprNode.accept(groupingVisitor);
        for (Pair<ExprNode, ExprGroupingNode> groupingNodePair : groupingVisitor.getGroupingNodes()) {
            // obtain combination - always a single one as grouping nodes cannot have
            int[] combination = getGroupExprCombination(groupByNodes, groupingNodePair.getSecond().getChildNodes());

            boolean found = false;
            int[] rollupIndexes = level.isAggregationTop() ? new int[0] : level.getRollupKeys();
            for (int index : rollupIndexes) {
                if (index == combination[0]) {
                    found = true;
                    break;
                }
            }

            int result = found ? 0 : 1;
            ExprConstantNodeImpl constant = new ExprConstantNodeImpl(result, Integer.class);
            if (groupingNodePair.getFirst() != null) {
                ExprNodeUtilityModify.replaceChildNode(groupingNodePair.getFirst(), groupingNodePair.getSecond(), constant);
            } else {
                exprNode = constant;
            }
        }

        // rewrite grouping id expressions
        for (Pair<ExprNode, ExprGroupingIdNode> groupingIdNodePair : groupingVisitor.getGroupingIdNodes()) {
            int[] combination = getGroupExprCombination(groupByNodes, groupingIdNodePair.getSecond().getChildNodes());

            int result = 0;
            for (int i = 0; i < combination.length; i++) {
                int index = combination[i];

                boolean found = false;
                int[] rollupIndexes = level.isAggregationTop() ? new int[0] : level.getRollupKeys();
                for (int rollupIndex : rollupIndexes) {
                    if (index == rollupIndex) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    result = result + pow2(combination.length - i - 1);
                }
            }

            ExprConstantNodeImpl constant = new ExprConstantNodeImpl(result, Integer.class);
            if (groupingIdNodePair.getFirst() != null) {
                ExprNodeUtilityModify.replaceChildNode(groupingIdNodePair.getFirst(), groupingIdNodePair.getSecond(), constant);
            } else {
                exprNode = constant;
            }
        }

        // rewrite properties
        ExprNodeIdentifierCollectVisitorWContainer identVisitor = new ExprNodeIdentifierCollectVisitorWContainer();
        exprNode.accept(identVisitor);
        for (Pair<ExprNode, ExprIdentNode> node : identVisitor.getExprProperties()) {
            boolean rewrite = false;

            ExprNodePropOrStreamExprDesc firstRollupNonPropExpr = rolledupProps.getFirstExpression();
            if (firstRollupNonPropExpr != null) {
                throw new ExprValidationException("Invalid rollup expression " + firstRollupNonPropExpr.getTextual());
            }

            for (ExprNodePropOrStreamDesc rolledupProp : rolledupProps.getProperties()) {
                ExprNodePropOrStreamPropDesc prop = (ExprNodePropOrStreamPropDesc) rolledupProp;
                if (rolledupProp.getStreamNum() == node.getSecond().getStreamId() && prop.getPropertyName().equals(node.getSecond().getResolvedPropertyName())) {
                    rewrite = true;
                    break;
                }
            }
            if (node.getFirst() != null && (node.getFirst() instanceof ExprPreviousNode || node.getFirst() instanceof ExprPriorNode)) {
                rewrite = false;
            }
            if (!rewrite) {
                continue;
            }

            ExprConstantNodeImpl constant = new ExprConstantNodeImpl(null, node.getSecond().getForge().getEvaluationType());
            if (node.getFirst() != null) {
                ExprNodeUtilityModify.replaceChildNode(node.getFirst(), node.getSecond(), constant);
            } else {
                exprNode = constant;
            }
        }

        return ExprNodeUtilityValidate.getValidatedSubtree(exprNodeOrigin, exprNode, validationContext);
    }

    private static int[] getGroupExprCombination(ExprNode[] groupByNodes, ExprNode[] childNodes)
        throws ExprValidationException {
        Set<Integer> indexes = new TreeSet<Integer>();
        for (ExprNode child : childNodes) {
            boolean found = false;

            for (int i = 0; i < groupByNodes.length; i++) {
                if (ExprNodeUtilityCompare.deepEquals(child, groupByNodes[i], false)) {
                    if (indexes.contains(i)) {
                        throw new ExprValidationException("Duplicate expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(child) + "' among grouping function parameters");
                    }
                    indexes.add(i);
                    found = true;
                }
            }

            if (!found) {
                throw new ExprValidationException("Failed to find expression '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(child) + "' among group-by expressions");
            }
        }
        return CollectionUtil.intArray(indexes);
    }

    private static void validateSelectAssignColNames(SelectClauseSpecCompiled selectClauseSpec, List<SelectClauseExprCompiledSpec> namedSelectionList, ExprValidationContext validationContext)
        throws ExprValidationException {
        for (int i = 0; i < selectClauseSpec.getSelectExprList().length; i++) {
            // validate element
            SelectClauseElementCompiled element = selectClauseSpec.getSelectExprList()[i];
            if (element instanceof SelectClauseExprCompiledSpec) {
                SelectClauseExprCompiledSpec expr = (SelectClauseExprCompiledSpec) element;
                ExprNode validatedExpression = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SELECT, expr.getSelectExpression(), validationContext);

                // determine an element name if none assigned
                String asName = expr.getAssignedName();
                if (asName == null) {
                    asName = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(validatedExpression);
                }

                expr.setAssignedName(asName);
                expr.setSelectExpression(validatedExpression);
                namedSelectionList.add(expr);
            }
        }
    }

    private static void validateHaving(ExprNodePropOrStreamSet propertiesGroupedBy,
                                       ExprNode havingNode)
        throws ExprValidationException {
        List<ExprAggregateNode> aggregateNodesHaving = new LinkedList<ExprAggregateNode>();
        ExprAggregateNodeUtil.getAggregatesBottomUp(havingNode, aggregateNodesHaving);

        // Any non-aggregated properties must occur in the group-by clause (if there is one)
        if (!propertiesGroupedBy.isEmpty()) {
            ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(true);
            havingNode.accept(visitor);
            List<ExprNodePropOrStreamDesc> allPropertiesHaving = visitor.getRefs();
            ExprNodePropOrStreamSet aggPropertiesHaving = ExprNodeUtilityAggregation.getAggregatedProperties(aggregateNodesHaving);

            aggPropertiesHaving.removeFromList(allPropertiesHaving);
            propertiesGroupedBy.removeFromList(allPropertiesHaving);

            if (!allPropertiesHaving.isEmpty()) {
                ExprNodePropOrStreamDesc desc = allPropertiesHaving.iterator().next();
                throw new ExprValidationException("Non-aggregated " + desc.getTextual() + " in the HAVING clause must occur in the group-by clause");
            }
        }
    }

    private static int pow2(int exponent) {
        if (exponent == 0) {
            return 1;
        }
        int result = 2;
        for (int i = 0; i < exponent - 1; i++) {
            result = 2 * result;
        }
        return result;
    }

    private static List<OrderByItem> expandColumnNames(SelectClauseElementCompiled[] selectionList, List<OrderByItem> orderByUnexpanded) {
        if (orderByUnexpanded == null || orderByUnexpanded.isEmpty()) {
            return Collections.emptyList();
        }

        // copy list to modify
        List<OrderByItem> expanded = new ArrayList<OrderByItem>();
        for (OrderByItem item : orderByUnexpanded) {
            expanded.add(item.copy());
        }

        // expand
        for (SelectClauseElementCompiled selectElement : selectionList) {
            // process only expressions
            if (!(selectElement instanceof SelectClauseExprCompiledSpec)) {
                continue;
            }
            SelectClauseExprCompiledSpec selectExpr = (SelectClauseExprCompiledSpec) selectElement;

            String name = selectExpr.getAssignedName();
            if (name != null) {
                ExprNode fullExpr = selectExpr.getSelectExpression();
                for (ListIterator<OrderByItem> iterator = expanded.listIterator(); iterator.hasNext(); ) {
                    OrderByItem orderByElement = iterator.next();
                    ExprNode swapped = ColumnNamedNodeSwapper.swap(orderByElement.getExprNode(), name, fullExpr);
                    OrderByItem newOrderByElement = new OrderByItem(swapped, orderByElement.isDescending());
                    iterator.set(newOrderByElement);
                }
            }
        }

        return expanded;
    }

    private static boolean sameExpressions(ExprNode[] levelExpressions, ExprNode[] validated) {
        if (levelExpressions.length != validated.length) {
            return false;
        }
        for (int i = 0; i < levelExpressions.length; i++) {
            if (validated[i] != levelExpressions[i]) {
                return false;
            }
        }
        return true;
    }
}
