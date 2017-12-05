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
package com.espertech.esper.epl.core.resultset.core;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.HookType;
import com.espertech.esper.client.annotation.IterableUnbound;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.ContextPropertyRegistry;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.agg.rollup.GroupByRollupPerLevelForge;
import com.espertech.esper.epl.agg.rollup.GroupByRollupPlanDesc;
import com.espertech.esper.epl.agg.rollup.GroupByRollupPlanHook;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryFactory;
import com.espertech.esper.epl.agg.service.common.AggregationServiceForgeDesc;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportUtil;
import com.espertech.esper.epl.core.orderby.OrderByElementForge;
import com.espertech.esper.epl.core.orderby.OrderByProcessorFactoryFactory;
import com.espertech.esper.epl.core.orderby.OrderByProcessorFactoryForge;
import com.espertech.esper.epl.core.resultset.agggrouped.ResultSetProcessorAggregateGroupedForge;
import com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorFactoryCompiler;
import com.espertech.esper.epl.core.resultset.handthru.ResultSetProcessorHandThroughFactoryForge;
import com.espertech.esper.epl.core.resultset.handthru.ResultSetProcessorSimpleForge;
import com.espertech.esper.epl.core.resultset.rowforall.ResultSetProcessorRowForAllForge;
import com.espertech.esper.epl.core.resultset.rowperevent.ResultSetProcessorRowPerEventForge;
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroup;
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroupForge;
import com.espertech.esper.epl.core.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupForge;
import com.espertech.esper.epl.core.select.SelectExprEventTypeRegistry;
import com.espertech.esper.epl.core.select.SelectExprProcessorDeliveryCallback;
import com.espertech.esper.epl.core.select.SelectExprProcessorFactory;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.core.streamtype.PropertyResolutionDescriptor;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.core.streamtype.StreamTypesException;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateUnverified;
import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeGroupKey;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.visitor.*;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;
import com.espertech.esper.epl.view.OutputConditionPolledFactoryFactory;
import com.espertech.esper.event.NativeEventType;
import com.espertech.esper.util.CollectionUtil;
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
 * Produces a old and new data row for each group changed, aggregates with grouping, see
 * {@link ResultSetProcessorRowPerGroup}
 * <p>
 * Case (6) is with group-by and with aggregation functions and only some selected properties are grouped-by.
 * in the select list: <pre>select customerId, supplierId, sum(volume) group by customerId</pre>.
 * Produces row for each event, aggregates with grouping.
 */
public class ResultSetProcessorFactoryFactory {
    public static ResultSetProcessorFactoryDesc getProcessorPrototype(StatementSpecCompiled statementSpec,
                                                                      StatementContext stmtContext,
                                                                      StreamTypeService typeService,
                                                                      ViewResourceDelegateUnverified viewResourceDelegate,
                                                                      boolean[] isUnidirectionalStream,
                                                                      boolean allowAggregation,
                                                                      ContextPropertyRegistry contextPropertyRegistry,
                                                                      SelectExprProcessorDeliveryCallback selectExprProcessorCallback,
                                                                      ConfigurationInformation configurationInformation,
                                                                      ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                                                                      boolean isFireAndForget,
                                                                      boolean isOnSelect
    )
            throws ExprValidationException {
        OrderByItem[] orderByListUnexpanded = statementSpec.getOrderByList();
        SelectClauseSpecCompiled selectClauseSpec = statementSpec.getSelectClauseSpec();
        InsertIntoDesc insertIntoDesc = statementSpec.getInsertIntoDesc();
        ExprNode optionalHavingNode = statementSpec.getHavingExprRootNode();
        OutputLimitSpec outputLimitSpec = statementSpec.getOutputLimitSpec();
        List<ExprDeclaredNode> declaredNodes = new ArrayList<ExprDeclaredNode>();

        // validate output limit spec
        validateOutputLimit(outputLimitSpec, stmtContext);

        // determine unidirectional
        boolean isUnidirectional = false;
        for (int i = 0; i < isUnidirectionalStream.length; i++) {
            isUnidirectional |= isUnidirectionalStream[i];
        }

        // determine single-stream historical
        boolean isHistoricalOnly = false;
        if (statementSpec.getStreamSpecs().length == 1) {
            StreamSpecCompiled spec = statementSpec.getStreamSpecs()[0];
            if (spec instanceof DBStatementStreamSpec || spec instanceof MethodStreamSpec || spec instanceof TableQueryStreamSpec) {
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
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(stmtContext, false);
        boolean allowRollup = statementSpec.getGroupByExpressions() != null && statementSpec.getGroupByExpressions().getGroupByRollupLevels() != null;
        boolean resettableAggs = isUnidirectional || statementSpec.getOnTriggerDesc() != null;
        String intoTableName = statementSpec.getIntoTableSpec() == null ? null : statementSpec.getIntoTableSpec().getName();
        ExprValidationContext validationContext = new ExprValidationContext(typeService, stmtContext.getEngineImportService(), stmtContext.getStatementExtensionServicesContext(), viewResourceDelegate, stmtContext.getSchedulingService(), stmtContext.getVariableService(), stmtContext.getTableService(), evaluatorContextStmt, stmtContext.getEventAdapterService(), stmtContext.getStatementName(), stmtContext.getStatementId(), stmtContext.getAnnotations(), stmtContext.getContextDescriptor(), false, allowRollup, true, resettableAggs, intoTableName, false);

        validateSelectAssignColNames(selectClauseSpec, namedSelectionList, validationContext);
        if (statementSpec.getGroupByExpressions() != null && statementSpec.getGroupByExpressions().getSelectClausePerLevel() != null) {
            ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.GROUPBY, statementSpec.getGroupByExpressions().getSelectClausePerLevel(), validationContext);
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
                TableMetadata tableMetadata = stmtContext.getTableService().getTableMetadataFromEventType(typeService.getEventTypes()[streamNum]);
                streamSelectSpec.setTableMetadata(tableMetadata);
            }
        }

        // Validate having clause, if present
        if (optionalHavingNode != null) {
            optionalHavingNode = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.HAVING, optionalHavingNode, validationContext);
            if (statementSpec.getGroupByExpressions() != null) {
                ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.GROUPBY, statementSpec.getGroupByExpressions().getOptHavingNodePerLevel(), validationContext);
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
            OrderByItem validatedOrderBy = new OrderByItem(ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.ORDERBY, orderByNode, validationContext), isDescending);
            orderByList.set(i, validatedOrderBy);

            if (statementSpec.getGroupByExpressions() != null && statementSpec.getGroupByExpressions().getOptOrderByPerLevel() != null) {
                ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.GROUPBY, statementSpec.getGroupByExpressions().getOptOrderByPerLevel(), validationContext);
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
        if (statementSpec.getGroupByExpressions() != null) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(statementSpec.getGroupByExpressions().getSelectClausePerLevel(), selectAggregateExprNodes);
        }
        if (!allowAggregation && !selectAggregateExprNodes.isEmpty()) {
            throw new ExprValidationException("Aggregation functions are not allowed in this context");
        }

        // Determine if we have a having clause with aggregation
        List<ExprAggregateNode> havingAggregateExprNodes = new LinkedList<ExprAggregateNode>();
        ExprNodePropOrStreamSet propertiesAggregatedHaving = new ExprNodePropOrStreamSet();
        if (optionalHavingNode != null) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(optionalHavingNode, havingAggregateExprNodes);
            if (statementSpec.getGroupByExpressions() != null) {
                ExprAggregateNodeUtil.getAggregatesBottomUp(statementSpec.getGroupByExpressions().getOptHavingNodePerLevel(), havingAggregateExprNodes);
            }
            propertiesAggregatedHaving = ExprNodeUtilityRich.getAggregatedProperties(havingAggregateExprNodes);
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
            if (statementSpec.getGroupByExpressions() != null) {
                ExprAggregateNodeUtil.getAggregatesBottomUp(statementSpec.getGroupByExpressions().getOptOrderByPerLevel(), orderByAggregateExprNodes);
            }
            if (!allowAggregation && !orderByAggregateExprNodes.isEmpty()) {
                throw new ExprValidationException("Aggregation functions are not allowed in this context");
            }
        }

        // Analyze rollup
        GroupByRollupInfo groupByRollupInfo = analyzeValidateGroupBy(statementSpec.getGroupByExpressions(), validationContext);
        ExprNode[] groupByNodesValidated = groupByRollupInfo == null ? ExprNodeUtilityCore.EMPTY_EXPR_ARRAY : groupByRollupInfo.getExprNodes();
        AggregationGroupByRollupDesc groupByRollupDesc = groupByRollupInfo == null ? null : groupByRollupInfo.getRollupDesc();

        // Construct the appropriate aggregation service
        boolean hasGroupBy = groupByNodesValidated.length > 0;
        AggregationServiceForgeDesc aggregationServiceForgeDesc = AggregationServiceFactoryFactory.getService(
                selectAggregateExprNodes, selectAggregationNodesNamed, declaredNodes, groupByNodesValidated, havingAggregateExprNodes, orderByAggregateExprNodes, Collections.<ExprAggregateNodeGroupKey>emptyList(), hasGroupBy, statementSpec.getAnnotations(), stmtContext.getVariableService(), typeService.getEventTypes().length > 1, false,
                statementSpec.getFilterRootNode(), statementSpec.getHavingExprRootNode(),
                stmtContext.getAggregationServiceFactoryService(), typeService.getEventTypes(), groupByRollupDesc,
                statementSpec.getOptionalContextName(), statementSpec.getIntoTableSpec(), stmtContext.getTableService(), isUnidirectional, isFireAndForget, isOnSelect, stmtContext.getEngineImportService(), stmtContext.getStatementName(), stmtContext.getTimeAbacus());

        // Compare local-aggregation versus group-by
        boolean localGroupByMatchesGroupBy = analyzeLocalGroupBy(groupByNodesValidated, selectAggregateExprNodes, havingAggregateExprNodes, orderByAggregateExprNodes);

        boolean useCollatorSort = false;
        if (stmtContext.getConfigSnapshot() != null) {
            useCollatorSort = stmtContext.getConfigSnapshot().getEngineDefaults().getLanguage().isSortUsingCollator();
        }

        // Construct the processor for evaluating the select clause
        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry(stmtContext.getStatementName(), stmtContext.getStatementEventTypeRef());
        SelectExprProcessorForge selectExprProcessorForge = SelectExprProcessorFactory.getProcessor(Collections.<Integer>emptyList(), selectClauseSpec.getSelectExprList(), isUsingWildcard, insertIntoDesc, null, statementSpec.getForClauseSpec(), typeService, stmtContext.getEventAdapterService(), stmtContext.getStatementResultService(), stmtContext.getValueAddEventService(), selectExprEventTypeRegistry, stmtContext.getEngineImportService(), evaluatorContextStmt,
                stmtContext.getVariableService(), stmtContext.getTableService(), stmtContext.getTimeProvider(), stmtContext.getEngineURI(), stmtContext.getStatementId(), stmtContext.getStatementName(), stmtContext.getAnnotations(), stmtContext.getContextDescriptor(), stmtContext.getConfigSnapshot(), selectExprProcessorCallback, stmtContext.getNamedWindowMgmtService(), statementSpec.getIntoTableSpec(), groupByRollupInfo, stmtContext.getStatementExtensionServicesContext());
        EventType resultEventType = selectExprProcessorForge.getResultEventType();

        // compute rollup if applicable
        GroupByRollupPerLevelForge rollupPerLevelForges = null;
        if (groupByRollupDesc != null) {
            rollupPerLevelForges = getRollUpPerLevelExpressions(statementSpec, groupByNodesValidated, groupByRollupDesc, stmtContext, selectExprEventTypeRegistry, evaluatorContextStmt, insertIntoDesc, typeService, validationContext, groupByRollupInfo);
        }

        // Construct the processor for sorting output events
        OrderByProcessorFactoryForge orderByProcessorFactory = OrderByProcessorFactoryFactory.getProcessor(namedSelectionList,
                orderByList, statementSpec.getRowLimitSpec(), stmtContext.getVariableService(), useCollatorSort, statementSpec.getOptionalContextName(), rollupPerLevelForges == null ? null : rollupPerLevelForges.getOptionalOrderByElements());
        boolean hasOrderBy = orderByProcessorFactory != null;

        // Get a list of event properties being aggregated in the select clause, if any
        ExprNodePropOrStreamSet propertiesGroupBy = ExprNodeUtilityRich.getGroupByPropertiesValidateHasOne(groupByNodesValidated);
        // Figure out all non-aggregated event properties in the select clause (props not under a sum/avg/max aggregation node)
        ExprNodePropOrStreamSet nonAggregatedPropsSelect = ExprNodeUtilityRich.getNonAggregatedProps(typeService.getEventTypes(), selectNodes, contextPropertyRegistry);
        if (optionalHavingNode != null) {
            ExprNodeUtilityRich.addNonAggregatedProps(optionalHavingNode, nonAggregatedPropsSelect, typeService.getEventTypes(), contextPropertyRegistry);
        }

        // Validate the having-clause (selected aggregate nodes and all in group-by are allowed)
        boolean isAggregated = (!selectAggregateExprNodes.isEmpty()) || (!havingAggregateExprNodes.isEmpty()) || (!orderByAggregateExprNodes.isEmpty()) || (!propertiesAggregatedHaving.isEmpty());
        if (optionalHavingNode != null && isAggregated) {
            validateHaving(propertiesGroupBy, optionalHavingNode);
        }

        // We only generate Remove-Stream events if they are explicitly selected, or the insert-into requires them
        boolean isSelectRStream = statementSpec.getSelectStreamSelectorEnum() == SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH
                || statementSpec.getSelectStreamSelectorEnum() == SelectClauseStreamSelectorEnum.RSTREAM_ONLY;
        if ((statementSpec.getInsertIntoDesc() != null) && (statementSpec.getInsertIntoDesc().getStreamSelector().isSelectsRStream())) {
            isSelectRStream = true;
        }

        ExprForge optionalHavingForge = optionalHavingNode == null ? null : optionalHavingNode.getForge();
        boolean hasOutputLimitOpt = ResultSetProcessorOutputConditionType.getOutputLimitOpt(statementSpec.getAnnotations(), configurationInformation, hasOrderBy);
        boolean hasOutputLimitSnapshot = outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT;
        boolean isGrouped = groupByNodesValidated.length > 0 || groupByRollupDesc != null;
        ResultSetProcessorOutputConditionType outputConditionType = outputLimitSpec != null ? ResultSetProcessorOutputConditionType.getConditionType(outputLimitSpec.getDisplayLimit(), isAggregated, hasOrderBy, hasOutputLimitOpt, isGrouped) : null;

        // Determine output-first condition factory
        OutputConditionPolledFactory optionalOutputFirstConditionFactory = null;
        if (outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.FIRST) {
            optionalOutputFirstConditionFactory = OutputConditionPolledFactoryFactory.createConditionFactory(outputLimitSpec, stmtContext);
        }
        boolean hasOutputLimit = outputLimitSpec != null;

        // (1)
        // There is no group-by clause and no aggregate functions with event properties in the select clause and having clause (simplest case)
        if ((groupByNodesValidated.length == 0) && (selectAggregateExprNodes.isEmpty()) && (havingAggregateExprNodes.isEmpty())) {
            // Determine if any output rate limiting must be performed early while processing results
            // Snapshot output does not count in terms of limiting output for grouping/aggregation purposes
            boolean isOutputLimitingNoSnapshot = (outputLimitSpec != null) && (outputLimitSpec.getDisplayLimit() != OutputLimitLimitType.SNAPSHOT);

            // (1a)
            // There is no need to perform select expression processing, the single view itself (no join) generates
            // events in the desired format, therefore there is no output processor. There are no order-by expressions.
            if (orderByNodes.isEmpty() && optionalHavingNode == null && !isOutputLimitingNoSnapshot && statementSpec.getRowLimitSpec() == null) {
                log.debug(".getProcessor Using no result processor");
                ResultSetProcessorHandThroughFactoryForge forge = new ResultSetProcessorHandThroughFactoryForge(resultEventType, selectExprProcessorForge, isSelectRStream);
                return ResultSetProcessorFactoryCompiler.allocate(forge, ResultSetProcessorType.HANDTHROUGH, resultEventType, stmtContext, isFireAndForget, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, new SelectExprProcessorForge[]{selectExprProcessorForge}, false, aggregationServiceForgeDesc, orderByProcessorFactory);
            }

            // (1b)
            // We need to process the select expression in a simple fashion, with each event (old and new)
            // directly generating one row, and no need to update aggregate state since there is no aggregate function.
            // There might be some order-by expressions.
            log.debug(".getProcessor Using ResultSetProcessorSimple");
            ResultSetProcessorSimpleForge forge = new ResultSetProcessorSimpleForge(resultEventType, selectExprProcessorForge, optionalHavingForge, isSelectRStream, outputLimitSpec, outputConditionType, resultSetProcessorHelperFactory, hasOrderBy, numStreams);
            return ResultSetProcessorFactoryCompiler.allocate(forge, ResultSetProcessorType.UNAGGREGATED_UNGROUPED, resultEventType, stmtContext, isFireAndForget, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, new SelectExprProcessorForge[]{selectExprProcessorForge}, false, aggregationServiceForgeDesc, orderByProcessorFactory);
        }

        // (2)
        // A wildcard select-clause has been specified and the group-by is ignored since no aggregation functions are used, and no having clause
        boolean isLast = statementSpec.getOutputLimitSpec() != null && statementSpec.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST;
        boolean isFirst = statementSpec.getOutputLimitSpec() != null && statementSpec.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.FIRST;
        if ((namedSelectionList.isEmpty()) && (propertiesAggregatedHaving.isEmpty()) && (havingAggregateExprNodes.isEmpty()) && !isLast && !isFirst) {
            log.debug(".getProcessor Using ResultSetProcessorSimple");
            ResultSetProcessorSimpleForge forge = new ResultSetProcessorSimpleForge(resultEventType, selectExprProcessorForge, optionalHavingForge, isSelectRStream, outputLimitSpec, outputConditionType, resultSetProcessorHelperFactory, hasOrderBy, numStreams);
            return ResultSetProcessorFactoryCompiler.allocate(forge, ResultSetProcessorType.UNAGGREGATED_UNGROUPED, resultEventType, stmtContext, isFireAndForget, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, new SelectExprProcessorForge[]{selectExprProcessorForge}, false, aggregationServiceForgeDesc, orderByProcessorFactory);
        }

        if ((groupByNodesValidated.length == 0) && isAggregated) {
            // (3)
            // There is no group-by clause and there are aggregate functions with event properties in the select clause (aggregation case)
            // or having class, and all event properties are aggregated (all properties are under aggregation functions).
            boolean hasStreamSelect = ExprNodeUtilityRich.hasStreamSelect(selectNodes);
            if ((nonAggregatedPropsSelect.isEmpty()) && !hasStreamSelect && !isUsingWildcard && !isUsingStreamSelect && localGroupByMatchesGroupBy && (viewResourceDelegate == null || viewResourceDelegate.getPreviousRequests().isEmpty())) {
                log.debug(".getProcessor Using ResultSetProcessorRowForAll");
                ResultSetProcessorRowForAllForge forge = new ResultSetProcessorRowForAllForge(resultEventType, selectExprProcessorForge, optionalHavingForge, isSelectRStream, isUnidirectional, isHistoricalOnly, outputLimitSpec, resultSetProcessorHelperFactory, hasOrderBy, outputConditionType);
                return ResultSetProcessorFactoryCompiler.allocate(forge, ResultSetProcessorType.FULLYAGGREGATED_UNGROUPED, resultEventType, stmtContext, isFireAndForget, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, new SelectExprProcessorForge[]{selectExprProcessorForge}, false, aggregationServiceForgeDesc, orderByProcessorFactory);
            }

            // (4)
            // There is no group-by clause but there are aggregate functions with event properties in the select clause (aggregation case)
            // or having clause and not all event properties are aggregated (some properties are not under aggregation functions).
            log.debug(".getProcessor Using ResultSetProcessorRowPerEventImpl");
            ResultSetProcessorRowPerEventForge forge = new ResultSetProcessorRowPerEventForge(selectExprProcessorForge.getResultEventType(), selectExprProcessorForge, optionalHavingForge, isSelectRStream, isUnidirectional, isHistoricalOnly, outputLimitSpec, outputConditionType, resultSetProcessorHelperFactory, hasOrderBy);
            return ResultSetProcessorFactoryCompiler.allocate(forge, ResultSetProcessorType.AGGREGATED_UNGROUPED, resultEventType, stmtContext, isFireAndForget, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, new SelectExprProcessorForge[]{selectExprProcessorForge}, false, aggregationServiceForgeDesc, orderByProcessorFactory);
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
        ExprNodePropOrStreamSet nonAggregatedPropsOrderBy = ExprNodeUtilityRich.getNonAggregatedProps(typeService.getEventTypes(), orderByNodes, contextPropertyRegistry);

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
            boolean iterableUnboundConfig = configurationInformation.getEngineDefaults().getViewResources().isIterableUnbound();
            boolean iterateUnbounded = noDataWindowSingleStream && (iterableUnboundConfig || AnnotationUtil.findAnnotation(statementSpec.getAnnotations(), IterableUnbound.class) != null);

            log.debug(".getProcessor Using ResultSetProcessorRowPerGroup");
            ResultSetProcessorFactoryForge forge;
            ResultSetProcessorType type;
            SelectExprProcessorForge[] selectExprProcessorForges;
            boolean rollup;
            if (groupByRollupDesc != null) {
                GroupByRollupPerLevelForge perLevelForges = getRollUpPerLevelExpressions(statementSpec, groupByNodesValidated, groupByRollupDesc, stmtContext, selectExprEventTypeRegistry, evaluatorContextStmt, insertIntoDesc, typeService, validationContext, groupByRollupInfo);
                forge = new ResultSetProcessorRowPerGroupRollupForge(resultEventType, perLevelForges, groupByNodesValidated, isSelectRStream, isUnidirectional, outputLimitSpec, orderByProcessorFactory != null, noDataWindowSingleStream, groupByRollupDesc, typeService.getEventTypes().length > 1, isHistoricalOnly, iterateUnbounded, optionalOutputFirstConditionFactory, resultSetProcessorHelperFactory, outputConditionType, numStreams);
                type = ResultSetProcessorType.FULLYAGGREGATED_GROUPED_ROLLUP;
                selectExprProcessorForges = perLevelForges.getSelectExprProcessorForges();
                rollup = true;
            } else {
                forge = new ResultSetProcessorRowPerGroupForge(resultEventType, selectExprProcessorForge, groupByNodesValidated, optionalHavingForge, isSelectRStream, isUnidirectional, outputLimitSpec, orderByProcessorFactory != null, noDataWindowSingleStream, isHistoricalOnly, iterateUnbounded, resultSetProcessorHelperFactory, outputConditionType, numStreams, optionalOutputFirstConditionFactory);
                type = ResultSetProcessorType.FULLYAGGREGATED_GROUPED;
                selectExprProcessorForges = new SelectExprProcessorForge[]{selectExprProcessorForge};
                rollup = false;
            }
            return ResultSetProcessorFactoryCompiler.allocate(forge, type, resultEventType, stmtContext, isFireAndForget, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, selectExprProcessorForges, rollup, aggregationServiceForgeDesc, orderByProcessorFactory);
        }

        if (groupByRollupDesc != null) {
            throw new ExprValidationException("Group-by with rollup requires a fully-aggregated query, the query is not full-aggregated because of " + notInGroupByReason);
        }

        // (6)
        // There is a group-by clause, and one or more event properties in the select clause that are not under an aggregation
        // function are not listed in the group-by clause (output one row per event, not one row per group)
        log.debug(".getProcessor Using ResultSetProcessorAggregateGrouped");
        ResultSetProcessorAggregateGroupedForge forge = new ResultSetProcessorAggregateGroupedForge(resultEventType, selectExprProcessorForge, groupByNodesValidated, optionalHavingForge, isSelectRStream, isUnidirectional, outputLimitSpec, orderByProcessorFactory != null, isHistoricalOnly, resultSetProcessorHelperFactory, optionalOutputFirstConditionFactory, outputConditionType, numStreams);
        return ResultSetProcessorFactoryCompiler.allocate(forge, ResultSetProcessorType.AGGREGATED_GROUPED, resultEventType, stmtContext, isFireAndForget, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, new SelectExprProcessorForge[]{selectExprProcessorForge}, false, aggregationServiceForgeDesc, orderByProcessorFactory);
    }

    private static void validateOutputLimit(OutputLimitSpec outputLimitSpec, StatementContext statementContext) throws ExprValidationException {
        if (outputLimitSpec == null) {
            return;
        }
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
        ExprValidationContext validationContext = new ExprValidationContext(new StreamTypeServiceImpl(statementContext.getEngineURI(), false), statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getTimeProvider(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
        if (outputLimitSpec.getAfterTimePeriodExpr() != null) {
            ExprTimePeriod timePeriodExpr = (ExprTimePeriod) ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, outputLimitSpec.getAfterTimePeriodExpr(), validationContext);
            outputLimitSpec.setAfterTimePeriodExpr(timePeriodExpr);
        }
        if (outputLimitSpec.getTimePeriodExpr() != null) {
            ExprTimePeriod timePeriodExpr = (ExprTimePeriod) ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, outputLimitSpec.getTimePeriodExpr(), validationContext);
            outputLimitSpec.setTimePeriodExpr(timePeriodExpr);
            if (timePeriodExpr.isConstantResult() && timePeriodExpr.evaluateAsSeconds(null, true, new ExprEvaluatorContextStatement(statementContext, false)) <= 0) {
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
                if (!ExprNodeUtilityCore.deepEqualsIsSubset(agg.getOptionalLocalGroupBy().getPartitionExpressions(), groupByNodesValidated)) {
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
        ExprNodeUtilityRich.validateNoSpecialsGroupByExpressions(groupBy.getGroupByNodes());

        // validate each expression
        ExprNode[] validated = new ExprNode[groupBy.getGroupByNodes().length];
        for (int i = 0; i < validated.length; i++) {
            validated[i] = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.GROUPBY, groupBy.getGroupByNodes()[i], validationContext);
        }

        if (groupBy.getGroupByRollupLevels() == null) {
            return new GroupByRollupInfo(validated, null);
        }

        AggregationGroupByRollupDesc rollup = AggregationGroupByRollupDesc.make(groupBy.getGroupByRollupLevels());

        // callback when hook reporting enabled
        try {
            GroupByRollupPlanHook hook = (GroupByRollupPlanHook) EngineImportUtil.getAnnotationHook(validationContext.getAnnotations(), HookType.INTERNAL_GROUPROLLUP_PLAN, GroupByRollupPlanHook.class, validationContext.getEngineImportService());
            if (hook != null) {
                hook.query(new GroupByRollupPlanDesc(validated, rollup));
            }
        } catch (ExprValidationException e) {
            throw new EPException("Failed to obtain hook for " + HookType.INTERNAL_QUERY_PLAN);
        }

        return new GroupByRollupInfo(validated, rollup);
    }

    private static GroupByRollupPerLevelForge getRollUpPerLevelExpressions(StatementSpecCompiled statementSpec, ExprNode[] groupByNodesValidated, AggregationGroupByRollupDesc groupByRollupDesc, StatementContext stmtContext, SelectExprEventTypeRegistry selectExprEventTypeRegistry, ExprEvaluatorContextStatement evaluatorContextStmt, InsertIntoDesc insertIntoDesc, StreamTypeService typeService, ExprValidationContext validationContext, GroupByRollupInfo groupByRollupInfo)
            throws ExprValidationException {
        int numLevels = groupByRollupDesc.getLevels().length;
        GroupByClauseExpressions groupByExpressions = statementSpec.getGroupByExpressions();

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
            propsPerGroupByExpr[i] = ExprNodeUtilityRich.getGroupByPropertiesValidateHasOne(new ExprNode[]{groupByNodesValidated[i]});
        }

        // for each level obtain a separate select expression processor
        for (int i = 0; i < numLevels; i++) {
            AggregationGroupByRollupLevel level = groupByRollupDesc.getLevels()[i];

            // determine properties rolled up for this level
            ExprNodePropOrStreamSet rolledupProps = getRollupProperties(level, propsPerGroupByExpr);

            ExprNode[] selectClauseLevel = groupByExpressions.getSelectClausePerLevel()[i];
            SelectClauseElementCompiled[] selectClause = getRollUpSelectClause(statementSpec.getSelectClauseSpec(), selectClauseLevel, level, rolledupProps, groupByNodesValidated, validationContext);
            SelectExprProcessorForge forge = SelectExprProcessorFactory.getProcessor(Collections.<Integer>emptyList(), selectClause, false, insertIntoDesc, null, statementSpec.getForClauseSpec(), typeService, stmtContext.getEventAdapterService(), stmtContext.getStatementResultService(), stmtContext.getValueAddEventService(), selectExprEventTypeRegistry, stmtContext.getEngineImportService(), evaluatorContextStmt,
                    stmtContext.getVariableService(), stmtContext.getTableService(), stmtContext.getTimeProvider(), stmtContext.getEngineURI(), stmtContext.getStatementId(), stmtContext.getStatementName(), stmtContext.getAnnotations(), stmtContext.getContextDescriptor(), stmtContext.getConfigSnapshot(), null, stmtContext.getNamedWindowMgmtService(), statementSpec.getIntoTableSpec(), groupByRollupInfo, stmtContext.getStatementExtensionServicesContext());
            processors[i] = forge;

            if (havingClauses != null) {
                ExprNode havingNode = rewriteRollupValidateExpression(ExprNodeOrigin.HAVING, groupByExpressions.getOptHavingNodePerLevel()[i], validationContext, rolledupProps, groupByNodesValidated, level);
                havingClauses[i] = havingNode.getForge();
            }

            if (orderByElements != null) {
                orderByElements[i] = rewriteRollupOrderBy(statementSpec.getOrderByList(), groupByExpressions.getOptOrderByPerLevel()[i], validationContext, rolledupProps, groupByNodesValidated, level);
            }
        }

        return new GroupByRollupPerLevelForge(processors, havingClauses, orderByElements);
    }

    private static OrderByElementForge[] rewriteRollupOrderBy(OrderByItem[] items, ExprNode[] orderByList, ExprValidationContext validationContext, ExprNodePropOrStreamSet rolledupProps, ExprNode[] groupByNodes, AggregationGroupByRollupLevel level)
            throws ExprValidationException {
        OrderByElementForge[] elements = new OrderByElementForge[orderByList.length];
        for (int i = 0; i < orderByList.length; i++) {
            ExprNode validated = rewriteRollupValidateExpression(ExprNodeOrigin.ORDERBY, orderByList[i], validationContext, rolledupProps, groupByNodes, level);
            elements[i] = new OrderByElementForge(validated, items[i].isDescending());
        }
        return elements;
    }

    private static ExprNodePropOrStreamSet getRollupProperties(AggregationGroupByRollupLevel level, ExprNodePropOrStreamSet[] propsPerGroupByExpr) {
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

    private static SelectClauseElementCompiled[] getRollUpSelectClause(SelectClauseSpecCompiled selectClauseSpec, ExprNode[] selectClauseLevel, AggregationGroupByRollupLevel level, ExprNodePropOrStreamSet rolledupProps, ExprNode[] groupByNodesValidated, ExprValidationContext validationContext)
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
                                                            AggregationGroupByRollupLevel level)
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
                ExprNodeUtilityCore.replaceChildNode(groupingNodePair.getFirst(), groupingNodePair.getSecond(), constant);
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
                ExprNodeUtilityCore.replaceChildNode(groupingIdNodePair.getFirst(), groupingIdNodePair.getSecond(), constant);
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
                ExprNodeUtilityCore.replaceChildNode(node.getFirst(), node.getSecond(), constant);
            } else {
                exprNode = constant;
            }
        }

        return ExprNodeUtilityRich.getValidatedSubtree(exprNodeOrigin, exprNode, validationContext);
    }

    private static int[] getGroupExprCombination(ExprNode[] groupByNodes, ExprNode[] childNodes)
            throws ExprValidationException {
        Set<Integer> indexes = new TreeSet<Integer>();
        for (ExprNode child : childNodes) {
            boolean found = false;

            for (int i = 0; i < groupByNodes.length; i++) {
                if (ExprNodeUtilityCore.deepEquals(child, groupByNodes[i], false)) {
                    if (indexes.contains(i)) {
                        throw new ExprValidationException("Duplicate expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(child) + "' among grouping function parameters");
                    }
                    indexes.add(i);
                    found = true;
                }
            }

            if (!found) {
                throw new ExprValidationException("Failed to find expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(child) + "' among group-by expressions");
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
                ExprNode validatedExpression = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, expr.getSelectExpression(), validationContext);

                // determine an element name if none assigned
                String asName = expr.getAssignedName();
                if (asName == null) {
                    asName = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(validatedExpression);
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
            ExprNodePropOrStreamSet aggPropertiesHaving = ExprNodeUtilityRich.getAggregatedProperties(aggregateNodesHaving);

            aggPropertiesHaving.removeFromList(allPropertiesHaving);
            propertiesGroupedBy.removeFromList(allPropertiesHaving);

            if (!allPropertiesHaving.isEmpty()) {
                ExprNodePropOrStreamDesc desc = allPropertiesHaving.iterator().next();
                throw new ExprValidationException("Non-aggregated " + desc.getTextual() + " in the HAVING clause must occur in the group-by clause");
            }
        }
    }

    private static List<OrderByItem> expandColumnNames(SelectClauseElementCompiled[] selectionList, OrderByItem[] orderByUnexpanded) {
        if (orderByUnexpanded.length == 0) {
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

    private static final Logger log = LoggerFactory.getLogger(ResultSetProcessorFactoryFactory.class);
}
