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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.pattern.EvalFactoryNode;
import com.espertech.esper.schedule.ScheduleSpec;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.ViewProcessingException;
import com.espertech.esper.view.ZeroDepthStreamNoIterate;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class EPStatementStartMethodCreateContext extends EPStatementStartMethodBase {
    public EPStatementStartMethodCreateContext(StatementSpecCompiled statementSpec) {
        super(statementSpec);
    }

    public EPStatementStartResult startInternal(final EPServicesContext services, StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient) throws ExprValidationException, ViewProcessingException {

        if (statementSpec.getOptionalContextName() != null) {
            throw new ExprValidationException("A create-context statement cannot itself be associated to a context, please declare a nested context instead");
        }
        final CreateContextDesc context = statementSpec.getContextDesc();
        final AgentInstanceContext agentInstanceContext = getDefaultAgentInstanceContext(statementContext);

        // compile filter specs, if any
        Set<String> eventTypesReferenced = new HashSet<String>();
        validateContextDetail(services, statementContext, eventTypesReferenced, context.getContextDetail(), agentInstanceContext);
        services.getStatementEventTypeRefService().addReferences(statementContext.getStatementName(), CollectionUtil.toArray(eventTypesReferenced));

        // define output event type
        String typeName = "EventType_Context_" + context.getContextName();
        EventType statementResultEventType = services.getEventAdapterService().createAnonymousMapType(typeName, Collections.<String, Object>emptyMap(), true);

        // add context - does not activate that context
        services.getContextManagementService().addContextSpec(services, agentInstanceContext, context, isRecoveringResilient, statementResultEventType);

        EPStatementStopMethod stopMethod = new EPStatementStopMethod() {
            public void stop() {
                // no action
            }
        };

        EPStatementDestroyMethod destroyMethod = new EPStatementDestroyMethod() {
            public void destroy() {
                services.getContextManagementService().destroyedContext(context.getContextName());
            }
        };
        return new EPStatementStartResult(new ZeroDepthStreamNoIterate(statementResultEventType), stopMethod, destroyMethod);
    }

    private void validateContextDetail(EPServicesContext servicesContext, StatementContext statementContext, Set<String> eventTypesReferenced, ContextDetail contextDetail, AgentInstanceContext agentInstanceContext) throws ExprValidationException {
        if (contextDetail instanceof ContextDetailPartitioned) {
            ContextDetailPartitioned segmented = (ContextDetailPartitioned) contextDetail;
            for (ContextDetailPartitionItem partition : segmented.getItems()) {
                validateNotTable(servicesContext, partition.getFilterSpecRaw().getEventTypeName());
                FilterStreamSpecRaw raw = new FilterStreamSpecRaw(partition.getFilterSpecRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
                StreamSpecCompiled compiled = raw.compile(statementContext, eventTypesReferenced, false, Collections.<Integer>emptyList(), false, true, false, null);
                if (!(compiled instanceof FilterStreamSpecCompiled)) {
                    throw new ExprValidationException("Partition criteria may not include named windows");
                }
                FilterStreamSpecCompiled result = (FilterStreamSpecCompiled) compiled;
                partition.setFilterSpecCompiled(result.getFilterSpec());
            }
        } else if (contextDetail instanceof ContextDetailCategory) {

            // compile filter
            ContextDetailCategory category = (ContextDetailCategory) contextDetail;
            validateNotTable(servicesContext, category.getFilterSpecRaw().getEventTypeName());
            FilterStreamSpecRaw raw = new FilterStreamSpecRaw(category.getFilterSpecRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
            FilterStreamSpecCompiled result = (FilterStreamSpecCompiled) raw.compile(statementContext, eventTypesReferenced, false, Collections.<Integer>emptyList(), false, true, false, null);
            category.setFilterSpecCompiled(result.getFilterSpec());
            servicesContext.getStatementEventTypeRefService().addReferences(statementContext.getStatementName(), CollectionUtil.toArray(eventTypesReferenced));

            // compile expressions
            for (ContextDetailCategoryItem item : category.getItems()) {
                validateNotTable(servicesContext, category.getFilterSpecRaw().getEventTypeName());
                FilterSpecRaw filterSpecRaw = new FilterSpecRaw(category.getFilterSpecRaw().getEventTypeName(), Collections.singletonList(item.getExpression()), null);
                FilterStreamSpecRaw rawExpr = new FilterStreamSpecRaw(filterSpecRaw, ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
                FilterStreamSpecCompiled compiled = (FilterStreamSpecCompiled) rawExpr.compile(statementContext, eventTypesReferenced, false, Collections.<Integer>emptyList(), false, true, false, null);
                item.setCompiledFilter(compiled.getFilterSpec(), agentInstanceContext);
            }
        } else if (contextDetail instanceof ContextDetailHash) {
            ContextDetailHash hashed = (ContextDetailHash) contextDetail;
            for (ContextDetailHashItem hashItem : hashed.getItems()) {
                FilterStreamSpecRaw raw = new FilterStreamSpecRaw(hashItem.getFilterSpecRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
                validateNotTable(servicesContext, hashItem.getFilterSpecRaw().getEventTypeName());
                FilterStreamSpecCompiled result = (FilterStreamSpecCompiled) raw.compile(statementContext, eventTypesReferenced, false, Collections.<Integer>emptyList(), false, true, false, null);
                hashItem.setFilterSpecCompiled(result.getFilterSpec());

                // validate parameters
                StreamTypeServiceImpl streamTypes = new StreamTypeServiceImpl(result.getFilterSpec().getFilterForEventType(), null, true, statementContext.getEngineURI());
                ExprValidationContext validationContext = new ExprValidationContext(streamTypes, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), getDefaultAgentInstanceContext(statementContext), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
                ExprNodeUtility.validate(ExprNodeOrigin.CONTEXT, Collections.singletonList(hashItem.getFunction()), validationContext);
            }
        } else if (contextDetail instanceof ContextDetailInitiatedTerminated) {
            ContextDetailInitiatedTerminated def = (ContextDetailInitiatedTerminated) contextDetail;
            ContextDetailMatchPair startCondition = validateRewriteContextCondition(servicesContext, statementContext, def.getStart(), eventTypesReferenced, new MatchEventSpec(), new LinkedHashSet<String>());
            ContextDetailMatchPair endCondition = validateRewriteContextCondition(servicesContext, statementContext, def.getEnd(), eventTypesReferenced, startCondition.getMatches(), startCondition.getAllTags());
            def.setStart(startCondition.getCondition());
            def.setEnd(endCondition.getCondition());

            if (def.getDistinctExpressions() != null) {
                if (!(startCondition.getCondition() instanceof ContextDetailConditionFilter)) {
                    throw new ExprValidationException("Distinct-expressions require a stream as the initiated-by condition");
                }
                ExprNode[] distinctExpressions = def.getDistinctExpressions();
                if (distinctExpressions.length == 0) {
                    throw new ExprValidationException("Distinct-expressions have not been provided");
                }
                ContextDetailConditionFilter filter = (ContextDetailConditionFilter) startCondition.getCondition();
                if (filter.getOptionalFilterAsName() == null) {
                    throw new ExprValidationException("Distinct-expressions require that a stream name is assigned to the stream using 'as'");
                }
                StreamTypeServiceImpl types = new StreamTypeServiceImpl(filter.getFilterSpecCompiled().getFilterForEventType(), filter.getOptionalFilterAsName(), true, servicesContext.getEngineURI());
                ExprValidationContext validationContext = new ExprValidationContext(types, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), getDefaultAgentInstanceContext(statementContext), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);
                for (int i = 0; i < distinctExpressions.length; i++) {
                    ExprNodeUtility.validatePlainExpression(ExprNodeOrigin.CONTEXTDISTINCT, ExprNodeUtility.toExpressionStringMinPrecedenceSafe(distinctExpressions[i]), distinctExpressions[i]);
                    distinctExpressions[i] = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.CONTEXTDISTINCT, distinctExpressions[i], validationContext);
                }
            }
        } else if (contextDetail instanceof ContextDetailNested) {
            ContextDetailNested nested = (ContextDetailNested) contextDetail;
            for (CreateContextDesc nestedContext : nested.getContexts()) {
                validateContextDetail(servicesContext, statementContext, eventTypesReferenced, nestedContext.getContextDetail(), agentInstanceContext);
            }
        } else {
            throw new IllegalStateException("Unrecognized context detail " + contextDetail);
        }
    }

    private void validateNotTable(EPServicesContext servicesContext, String eventTypeName) throws ExprValidationException {
        if (servicesContext.getTableService().getTableMetadata(eventTypeName) != null) {
            throw new ExprValidationException("Tables cannot be used in a context declaration");
        }
    }

    private ContextDetailMatchPair validateRewriteContextCondition(EPServicesContext servicesContext, StatementContext statementContext, ContextDetailCondition endpoint, Set<String> eventTypesReferenced, MatchEventSpec priorMatches, Set<String> priorAllTags) throws ExprValidationException {
        if (endpoint instanceof ContextDetailConditionCrontab) {
            ContextDetailConditionCrontab crontab = (ContextDetailConditionCrontab) endpoint;
            ExprEvaluator[] scheduleSpecEvaluators = ExprNodeUtility.crontabScheduleValidate(ExprNodeOrigin.CONTEXTCONDITION, crontab.getCrontab(), statementContext, false);
            ScheduleSpec schedule = ExprNodeUtility.crontabScheduleBuild(scheduleSpecEvaluators, new ExprEvaluatorContextStatement(statementContext, false));
            crontab.setSchedule(schedule);
            return new ContextDetailMatchPair(crontab, new MatchEventSpec(), new LinkedHashSet<String>());
        }

        if (endpoint instanceof ContextDetailConditionTimePeriod) {
            ContextDetailConditionTimePeriod timePeriod = (ContextDetailConditionTimePeriod) endpoint;
            ExprValidationContext validationContext = new ExprValidationContext(new StreamTypeServiceImpl(servicesContext.getEngineURI(), false), statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), getDefaultAgentInstanceContext(statementContext), statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, null, false);
            ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.CONTEXTCONDITION, timePeriod.getTimePeriod(), validationContext);
            if (timePeriod.getTimePeriod().isConstantResult()) {
                if (timePeriod.getTimePeriod().evaluateAsSeconds(null, true, null) < 0) {
                    throw new ExprValidationException("Invalid negative time period expression '" + ExprNodeUtility.toExpressionStringMinPrecedenceSafe(timePeriod.getTimePeriod()) + "'");
                }
            }
            return new ContextDetailMatchPair(timePeriod, new MatchEventSpec(), new LinkedHashSet<String>());
        }

        if (endpoint instanceof ContextDetailConditionPattern) {
            ContextDetailConditionPattern pattern = (ContextDetailConditionPattern) endpoint;
            Pair<MatchEventSpec, Set<String>> matches = validatePatternContextConditionPattern(statementContext, pattern, eventTypesReferenced, priorMatches, priorAllTags);
            return new ContextDetailMatchPair(pattern, matches.getFirst(), matches.getSecond());
        }

        if (endpoint instanceof ContextDetailConditionFilter) {
            ContextDetailConditionFilter filter = (ContextDetailConditionFilter) endpoint;
            validateNotTable(servicesContext, filter.getFilterSpecRaw().getEventTypeName());

            // compile as filter if there are no prior match to consider
            if (priorMatches == null || (priorMatches.getArrayEventTypes().isEmpty() && priorMatches.getTaggedEventTypes().isEmpty())) {
                FilterStreamSpecRaw rawExpr = new FilterStreamSpecRaw(filter.getFilterSpecRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT);
                FilterStreamSpecCompiled compiled = (FilterStreamSpecCompiled) rawExpr.compile(statementContext, eventTypesReferenced, false, Collections.<Integer>emptyList(), false, true, false, filter.getOptionalFilterAsName());
                filter.setFilterSpecCompiled(compiled.getFilterSpec());
                MatchEventSpec matchEventSpec = new MatchEventSpec();
                EventType filterForType = compiled.getFilterSpec().getFilterForEventType();
                LinkedHashSet<String> allTags = new LinkedHashSet<String>();
                if (filter.getOptionalFilterAsName() != null) {
                    matchEventSpec.getTaggedEventTypes().put(filter.getOptionalFilterAsName(), new Pair<EventType, String>(filterForType, rawExpr.getRawFilterSpec().getEventTypeName()));
                    allTags.add(filter.getOptionalFilterAsName());
                }
                return new ContextDetailMatchPair(filter, matchEventSpec, allTags);
            }

            // compile as pattern if there are prior matches to consider, since this is a type of followed-by relationship
            EvalFactoryNode factoryNode = servicesContext.getPatternNodeFactory().makeFilterNode(filter.getFilterSpecRaw(), filter.getOptionalFilterAsName(), 0);
            ContextDetailConditionPattern pattern = new ContextDetailConditionPattern(factoryNode, true, false);
            Pair<MatchEventSpec, Set<String>> matches = validatePatternContextConditionPattern(statementContext, pattern, eventTypesReferenced, priorMatches, priorAllTags);
            return new ContextDetailMatchPair(pattern, matches.getFirst(), matches.getSecond());
        } else if (endpoint instanceof ContextDetailConditionImmediate || endpoint instanceof ContextDetailConditionNever) {
            return new ContextDetailMatchPair(endpoint, new MatchEventSpec(), new LinkedHashSet<String>());
        } else {
            throw new IllegalStateException("Unrecognized endpoint type " + endpoint);
        }
    }

    private Pair<MatchEventSpec, Set<String>> validatePatternContextConditionPattern(StatementContext statementContext, ContextDetailConditionPattern pattern, Set<String> eventTypesReferenced, MatchEventSpec priorMatches, Set<String> priorAllTags)
            throws ExprValidationException {
        PatternStreamSpecRaw raw = new PatternStreamSpecRaw(pattern.getPatternRaw(), ViewSpec.EMPTY_VIEWSPEC_ARRAY, null, StreamSpecOptions.DEFAULT, false, false);
        PatternStreamSpecCompiled compiled = raw.compile(statementContext, eventTypesReferenced, false, Collections.<Integer>emptyList(), priorMatches, priorAllTags, false, true, false);
        pattern.setPatternCompiled(compiled);
        return new Pair<MatchEventSpec, Set<String>>(new MatchEventSpec(compiled.getTaggedEventTypes(), compiled.getArrayEventTypes()), compiled.getAllTags());
    }

    private static class ContextDetailMatchPair {
        private final ContextDetailCondition condition;
        private final MatchEventSpec matches;
        private final Set<String> allTags;

        private ContextDetailMatchPair(ContextDetailCondition condition, MatchEventSpec matches, Set<String> allTags) {
            this.condition = condition;
            this.matches = matches;
            this.allTags = allTags;
        }

        public ContextDetailCondition getCondition() {
            return condition;
        }

        public MatchEventSpec getMatches() {
            return matches;
        }

        public Set<String> getAllTags() {
            return allTags;
        }
    }
}