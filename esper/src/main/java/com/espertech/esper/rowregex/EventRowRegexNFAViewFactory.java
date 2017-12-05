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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.service.common.AggregationServiceAggExpressionDesc;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryFactory;
import com.espertech.esper.epl.agg.service.common.AggregationServiceMatchRecognize;
import com.espertech.esper.epl.agg.service.common.AggregationServiceMatchRecognizeFactoryDesc;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.prev.ExprPreviousMatchRecognizeNode;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodePreviousVisitorWParent;
import com.espertech.esper.epl.expression.visitor.ExprNodeStreamRequiredVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeStreamUseCollectVisitor;
import com.espertech.esper.epl.spec.MatchRecognizeDefineItem;
import com.espertech.esper.epl.spec.MatchRecognizeMeasureItem;
import com.espertech.esper.epl.spec.MatchRecognizeSpec;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * View factory for match-recognize view.
 */
public class EventRowRegexNFAViewFactory extends ViewFactorySupport {
    private static final Logger log = LoggerFactory.getLogger(EventRowRegexNFAViewFactory.class);

    protected final MatchRecognizeSpec matchRecognizeSpec;
    protected final LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams;
    protected final HashMap<String, Pair<ExprNode, ExprEvaluator>> variableDefinitions;
    protected final Map<Integer, String> streamVariables;
    protected final Set<String> variablesSingle;
    protected final ObjectArrayEventType compositeEventType;
    protected final EventType rowEventType;
    protected final AggregationServiceMatchRecognize aggregationService;
    protected final List<AggregationServiceAggExpressionDesc> aggregationExpressions;
    protected final TreeMap<Integer, List<ExprPreviousMatchRecognizeNode>> callbacksPerIndex = new TreeMap<Integer, List<ExprPreviousMatchRecognizeNode>>();
    protected final boolean isUnbound;
    protected final boolean isIterateOnly;
    protected final boolean isCollectMultimatches;
    protected final boolean isDefineAsksMultimatches;
    protected final ObjectArrayBackedEventBean defineMultimatchEventBean;
    protected final boolean[] isExprRequiresMultimatchState;
    protected final RowRegexExprNode expandedPatternNode;
    protected final ConfigurationEngineDefaults.MatchRecognize matchRecognizeConfig;
    protected final ExprEvaluator[] columnEvaluators;
    protected final String[] columnNames;
    protected final ExprEvaluator[] partitionByEvals;
    protected final RegexNFAState[] startStates;
    protected final RegexNFAState[] allStates;
    protected final String[] multimatchVariablesArray;
    protected final int[] multimatchStreamNumToVariable;
    protected final int[] multimatchVariableToStreamNum;
    protected final int numEventsEventsPerStreamDefine;
    protected final boolean isOrTerminated;
    protected final boolean isTrackMaxStates;

    /**
     * Ctor.
     *
     * @param viewChain            views
     * @param matchRecognizeSpec   specification
     * @param isUnbound            true for unbound stream
     * @param annotations          annotations
     * @param matchRecognizeConfig config
     * @param agentInstanceContext context
     * @throws ExprValidationException if validation fails
     */
    public EventRowRegexNFAViewFactory(ViewFactoryChain viewChain, MatchRecognizeSpec matchRecognizeSpec, AgentInstanceContext agentInstanceContext, boolean isUnbound, Annotation[] annotations, ConfigurationEngineDefaults.MatchRecognize matchRecognizeConfig)
            throws ExprValidationException {
        EventType parentViewType = viewChain.getEventType();
        this.matchRecognizeSpec = matchRecognizeSpec;
        this.isUnbound = isUnbound;
        this.isIterateOnly = HintEnum.ITERATE_ONLY.getHint(annotations) != null;
        this.matchRecognizeConfig = matchRecognizeConfig;
        StatementContext statementContext = agentInstanceContext.getStatementContext();

        // Expand repeats and permutations
        expandedPatternNode = RegexPatternExpandUtil.expand(matchRecognizeSpec.getPattern());

        // Determine single-row and multiple-row variables
        variablesSingle = new LinkedHashSet<String>();
        Set<String> variablesMultiple = new LinkedHashSet<String>();
        EventRowRegexHelper.recursiveInspectVariables(expandedPatternNode, false, variablesSingle, variablesMultiple);

        // each variable gets associated with a stream number (multiple-row variables as well to hold the current event for the expression).
        int streamNum = 0;
        variableStreams = new LinkedHashMap<String, Pair<Integer, Boolean>>();
        for (String variableSingle : variablesSingle) {
            variableStreams.put(variableSingle, new Pair<Integer, Boolean>(streamNum, false));
            streamNum++;
        }
        for (String variableMultiple : variablesMultiple) {
            variableStreams.put(variableMultiple, new Pair<Integer, Boolean>(streamNum, true));
            streamNum++;
        }

        // mapping of stream to variable
        streamVariables = new TreeMap<Integer, String>();
        for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
            streamVariables.put(entry.getValue().getFirst(), entry.getKey());
        }

        // determine visibility rules
        Map<String, Set<String>> visibility = EventRowRegexHelper.determineVisibility(expandedPatternNode);

        // assemble all single-row variables for expression validation
        String[] allStreamNames = new String[variableStreams.size()];
        EventType[] allTypes = new EventType[variableStreams.size()];

        streamNum = 0;
        for (String variableSingle : variablesSingle) {
            allStreamNames[streamNum] = variableSingle;
            allTypes[streamNum] = parentViewType;
            streamNum++;
        }
        for (String variableMultiple : variablesMultiple) {
            allStreamNames[streamNum] = variableMultiple;
            allTypes[streamNum] = parentViewType;
            streamNum++;
        }

        // determine type service for use with DEFINE
        // validate each DEFINE clause expression
        Set<String> definedVariables = new HashSet<String>();
        List<ExprAggregateNode> aggregateNodes = new ArrayList<ExprAggregateNode>();
        ExprEvaluatorContextStatement exprEvaluatorContext = new ExprEvaluatorContextStatement(statementContext, false);
        this.isExprRequiresMultimatchState = new boolean[variableStreams.size()];

        for (int defineIndex = 0; defineIndex < matchRecognizeSpec.getDefines().size(); defineIndex++) {
            MatchRecognizeDefineItem defineItem = matchRecognizeSpec.getDefines().get(defineIndex);
            if (definedVariables.contains(defineItem.getIdentifier())) {
                throw new ExprValidationException("Variable '" + defineItem.getIdentifier() + "' has already been defined");
            }
            definedVariables.add(defineItem.getIdentifier());

            // stream-type visibilities handled here
            StreamTypeService typeServiceDefines = EventRowRegexNFAViewFactoryHelper.buildDefineStreamTypeServiceDefine(statementContext, variableStreams, defineItem, visibility, parentViewType);

            ExprNode exprNodeResult = handlePreviousFunctions(defineItem.getExpression());
            ExprValidationContext validationContext = new ExprValidationContext(typeServiceDefines, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), exprEvaluatorContext, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), true, false, true, false, null, false);

            ExprNode validated;
            try {
                // validate
                validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGDEFINE, exprNodeResult, validationContext);

                // check aggregates
                defineItem.setExpression(validated);
                ExprAggregateNodeUtil.getAggregatesBottomUp(validated, aggregateNodes);
                if (!aggregateNodes.isEmpty()) {
                    throw new ExprValidationException("An aggregate function may not appear in a DEFINE clause");
                }
            } catch (ExprValidationException ex) {
                throw new ExprValidationException("Failed to validate condition expression for variable '" + defineItem.getIdentifier() + "': " + ex.getMessage(), ex);
            }

            // determine access to event properties from multi-matches
            ExprNodeStreamRequiredVisitor visitor = new ExprNodeStreamRequiredVisitor();
            validated.accept(visitor);
            Set<Integer> streamsRequired = visitor.getStreamsRequired();
            for (int streamRequired : streamsRequired) {
                if (streamRequired >= variableStreams.size()) {
                    int streamNumIdent = variableStreams.get(defineItem.getIdentifier()).getFirst();
                    isExprRequiresMultimatchState[streamNumIdent] = true;
                    break;
                }
            }
        }
        isDefineAsksMultimatches = CollectionUtil.isAnySet(isExprRequiresMultimatchState);
        defineMultimatchEventBean = isDefineAsksMultimatches ? EventRowRegexNFAViewFactoryHelper.getDefineMultimatchBean(statementContext, variableStreams, parentViewType) : null;

        // assign "prev" node indexes
        // Since an expression such as "prior(2, price), prior(8, price)" translates into {2, 8} the relative index is {0, 1}.
        // Map the expression-supplied index to a relative index
        int countPrev = 0;
        for (Map.Entry<Integer, List<ExprPreviousMatchRecognizeNode>> entry : callbacksPerIndex.entrySet()) {
            for (ExprPreviousMatchRecognizeNode callback : entry.getValue()) {
                callback.setAssignedIndex(countPrev);
            }
            countPrev++;
        }

        // determine type service for use with MEASURE
        Map<String, Object> measureTypeDef = new LinkedHashMap<String, Object>();
        for (String variableSingle : variablesSingle) {
            measureTypeDef.put(variableSingle, parentViewType);
        }
        for (String variableMultiple : variablesMultiple) {
            measureTypeDef.put(variableMultiple, new EventType[]{parentViewType});
        }
        String outputEventTypeName = statementContext.getStatementId() + "_rowrecog";
        compositeEventType = (ObjectArrayEventType) statementContext.getEventAdapterService().createAnonymousObjectArrayType(outputEventTypeName, measureTypeDef);
        StreamTypeService typeServiceMeasure = new StreamTypeServiceImpl(compositeEventType, "MATCH_RECOGNIZE", true, statementContext.getEngineURI());

        // find MEASURE clause aggregations
        boolean measureReferencesMultivar = false;
        List<ExprAggregateNode> measureAggregateExprNodes = new ArrayList<ExprAggregateNode>();
        for (MatchRecognizeMeasureItem measureItem : matchRecognizeSpec.getMeasures()) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(measureItem.getExpr(), measureAggregateExprNodes);
        }
        if (!measureAggregateExprNodes.isEmpty()) {
            boolean[] isIStreamOnly = new boolean[allStreamNames.length];
            Arrays.fill(isIStreamOnly, true);
            StreamTypeServiceImpl typeServiceAggregateMeasure = new StreamTypeServiceImpl(allTypes, allStreamNames, isIStreamOnly, statementContext.getEngineURI(), false, true);
            Map<Integer, List<ExprAggregateNode>> measureExprAggNodesPerStream = new HashMap<Integer, List<ExprAggregateNode>>();

            for (ExprAggregateNode aggregateNode : measureAggregateExprNodes) {
                // validate absence of group-by
                aggregateNode.validatePositionals();
                if (aggregateNode.getOptionalLocalGroupBy() != null) {
                    throw new ExprValidationException("Match-recognize does not allow aggregation functions to specify a group-by");
                }

                // validate node and params
                int count = 0;
                ExprNodeIdentifierVisitor visitor = new ExprNodeIdentifierVisitor(true);

                ExprValidationContext validationContext = new ExprValidationContext(typeServiceAggregateMeasure, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), exprEvaluatorContext, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);
                for (ExprNode child : aggregateNode.getChildNodes()) {
                    ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGMEASURE, child, validationContext);
                    validated.accept(visitor);
                    aggregateNode.setChildNode(count++, new ExprNodeValidated(validated));
                }
                validationContext = new ExprValidationContext(typeServiceMeasure, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), exprEvaluatorContext, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);
                aggregateNode.validate(validationContext);

                // verify properties used within the aggregation
                Set<Integer> aggregatedStreams = new HashSet<Integer>();
                for (Pair<Integer, String> pair : visitor.getExprProperties()) {
                    aggregatedStreams.add(pair.getFirst());
                }

                Integer multipleVarStream = null;
                for (int streamNumAggregated : aggregatedStreams) {
                    String variable = streamVariables.get(streamNumAggregated);
                    if (variablesMultiple.contains(variable)) {
                        measureReferencesMultivar = true;
                        if (multipleVarStream == null) {
                            multipleVarStream = streamNumAggregated;
                            continue;
                        }
                        throw new ExprValidationException("Aggregation functions in the measure-clause must only refer to properties of exactly one group variable returning multiple events");
                    }
                }

                if (multipleVarStream == null) {
                    throw new ExprValidationException("Aggregation functions in the measure-clause must refer to one or more properties of exactly one group variable returning multiple events");
                }

                List<ExprAggregateNode> aggNodesForStream = measureExprAggNodesPerStream.get(multipleVarStream);
                if (aggNodesForStream == null) {
                    aggNodesForStream = new ArrayList<ExprAggregateNode>();
                    measureExprAggNodesPerStream.put(multipleVarStream, aggNodesForStream);
                }
                aggNodesForStream.add(aggregateNode);
            }

            AggregationServiceMatchRecognizeFactoryDesc factoryDesc = AggregationServiceFactoryFactory.getServiceMatchRecognize(streamVariables.size(), measureExprAggNodesPerStream, typeServiceAggregateMeasure.getEventTypes(), agentInstanceContext.getEngineImportService(), agentInstanceContext.getStatementName());
            aggregationService = factoryDesc.getAggregationServiceFactory().makeService(agentInstanceContext);
            aggregationExpressions = factoryDesc.getExpressions();
        } else {
            aggregationService = null;
            aggregationExpressions = Collections.emptyList();
        }

        // validate each MEASURE clause expression
        Map<String, Object> rowTypeDef = new LinkedHashMap<String, Object>();
        ExprNodeStreamUseCollectVisitor streamRefVisitor = new ExprNodeStreamUseCollectVisitor();
        for (MatchRecognizeMeasureItem measureItem : matchRecognizeSpec.getMeasures()) {
            if (measureItem.getName() == null) {
                throw new ExprValidationException("The measures clause requires that each expression utilizes the AS keyword to assign a column name");
            }
            ExprNode validated = validateMeasureClause(measureItem.getExpr(), typeServiceMeasure, variablesMultiple, variablesSingle, statementContext);
            measureItem.setExpr(validated);
            rowTypeDef.put(measureItem.getName(), validated.getForge().getEvaluationType());
            validated.accept(streamRefVisitor);
        }

        // Determine if any of the multi-var streams are referenced in the measures (non-aggregated only)
        for (ExprStreamRefNode ref : streamRefVisitor.getReferenced()) {
            String rootPropName = ref.getRootPropertyNameIfAny();
            if (rootPropName != null) {
                if (variablesMultiple.contains(rootPropName)) {
                    measureReferencesMultivar = true;
                    break;
                }
            }

            Integer streamRequired = ref.getStreamReferencedIfAny();
            if (streamRequired != null) {
                String streamVariable = streamVariables.get(streamRequired);
                if (streamVariable != null) {
                    Pair<Integer, Boolean> def = variableStreams.get(streamVariable);
                    if (def != null && def.getSecond()) {
                        measureReferencesMultivar = true;
                        break;
                    }
                }
            }
        }
        isCollectMultimatches = measureReferencesMultivar || isDefineAsksMultimatches;

        // create rowevent type
        String rowEventTypeName = statementContext.getStatementId() + "_rowrecogrow";
        rowEventType = statementContext.getEventAdapterService().createAnonymousMapType(rowEventTypeName, rowTypeDef, true);

        // validate partition-by expressions, if any
        if (!matchRecognizeSpec.getPartitionByExpressions().isEmpty()) {
            StreamTypeService typeServicePartition = new StreamTypeServiceImpl(parentViewType, "MATCH_RECOGNIZE_PARTITION", true, statementContext.getEngineURI());
            List<ExprNode> validated = new ArrayList<ExprNode>();
            ExprValidationContext validationContext = new ExprValidationContext(typeServicePartition, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), exprEvaluatorContext, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);
            for (ExprNode partitionExpr : matchRecognizeSpec.getPartitionByExpressions()) {
                validated.add(ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGPARTITION, partitionExpr, validationContext));
            }
            matchRecognizeSpec.setPartitionByExpressions(validated);
            partitionByEvals = ExprNodeUtilityRich.getEvaluatorsMayCompile(validated, statementContext.getEngineImportService(), EventRowRegexNFAViewFactory.class, false, statementContext.getStatementName());
        } else {
            partitionByEvals = null;
        }

        // validate interval if present
        if (matchRecognizeSpec.getInterval() != null) {
            ExprValidationContext validationContext = new ExprValidationContext(new StreamTypeServiceImpl(statementContext.getEngineURI(), false), statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), exprEvaluatorContext, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);
            ExprTimePeriod validated = (ExprTimePeriod) ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGINTERVAL, matchRecognizeSpec.getInterval().getTimePeriodExpr(), validationContext);
            matchRecognizeSpec.getInterval().setTimePeriodExpr(validated);
        }

        // compile variable definition expressions
        variableDefinitions = new HashMap<>();
        for (MatchRecognizeDefineItem defineItem : matchRecognizeSpec.getDefines()) {
            ExprEvaluator evaluator = ExprNodeCompiler.allocateEvaluator(defineItem.getExpression().getForge(), agentInstanceContext.getEngineImportService(), EventRowRegexNFAViewFactory.class, false, statementContext.getStatementName());
            variableDefinitions.put(defineItem.getIdentifier(), new Pair<>(defineItem.getExpression(), evaluator));
        }

        // create evaluators
        columnNames = new String[matchRecognizeSpec.getMeasures().size()];
        columnEvaluators = new ExprEvaluator[matchRecognizeSpec.getMeasures().size()];
        int count = 0;
        for (MatchRecognizeMeasureItem measureItem : matchRecognizeSpec.getMeasures()) {
            columnNames[count] = measureItem.getName();
            columnEvaluators[count] = ExprNodeCompiler.allocateEvaluator(measureItem.getExpr().getForge(), agentInstanceContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
            count++;
        }

        // build states
        RegexNFAStrandResult strand = EventRowRegexHelper.recursiveBuildStartStates(expandedPatternNode, variableDefinitions, variableStreams, isExprRequiresMultimatchState);
        startStates = strand.getStartStates().toArray(new RegexNFAState[strand.getStartStates().size()]);
        allStates = strand.getAllStates().toArray(new RegexNFAState[strand.getAllStates().size()]);

        if (log.isInfoEnabled()) {
            log.info("NFA tree:\n" + EventRowRegexNFAViewUtil.print(startStates));
        }

        // determine names of multimatching variables
        if (variablesSingle.size() == variableStreams.size()) {
            multimatchVariablesArray = new String[0];
            multimatchStreamNumToVariable = new int[0];
            multimatchVariableToStreamNum = new int[0];
        } else {
            multimatchVariablesArray = new String[variableStreams.size() - variablesSingle.size()];
            multimatchVariableToStreamNum = new int[multimatchVariablesArray.length];
            multimatchStreamNumToVariable = new int[variableStreams.size()];
            Arrays.fill(multimatchStreamNumToVariable, -1);
            count = 0;
            for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
                if (entry.getValue().getSecond()) {
                    int index = count;
                    multimatchVariablesArray[index] = entry.getKey();
                    multimatchVariableToStreamNum[index] = entry.getValue().getFirst();
                    multimatchStreamNumToVariable[entry.getValue().getFirst()] = index;
                    count++;
                }
            }
        }
        this.numEventsEventsPerStreamDefine = isDefineAsksMultimatches ? variableStreams.size() + 1 : variableStreams.size();

        // determine interval-or-terminated
        if (matchRecognizeSpec.getInterval() != null) {
            isOrTerminated = matchRecognizeSpec.getInterval().isOrTerminated();
        } else {
            isOrTerminated = false;
        }

        // flag to track max states
        this.isTrackMaxStates = matchRecognizeConfig != null && matchRecognizeConfig.getMaxStates() != null;
    }

    private ExprNode validateMeasureClause(ExprNode measureNode, StreamTypeService typeServiceMeasure, Set<String> variablesMultiple, Set<String> variablesSingle, StatementContext statementContext)
            throws ExprValidationException {
        try {
            ExprEvaluatorContextStatement exprEvaluatorContext = new ExprEvaluatorContextStatement(statementContext, false);
            ExprValidationContext validationContext = new ExprValidationContext(typeServiceMeasure, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), exprEvaluatorContext, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), true, false, true, false, null, false);
            return ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGMEASURE, measureNode, validationContext);
        } catch (ExprValidationPropertyException e) {
            String grouped = CollectionUtil.toString(variablesMultiple);
            String single = CollectionUtil.toString(variablesSingle);
            String message = e.getMessage();
            if (!variablesMultiple.isEmpty()) {
                message += ", ensure that grouped variables (variables " + grouped + ") are accessed via index (i.e. variable[0].property) or appear within an aggregation";
            }
            if (!variablesSingle.isEmpty()) {
                message += ", ensure that singleton variables (variables " + single + ") are not accessed via index";
            }
            throw new ExprValidationPropertyException(message, e);
        }
    }

    private ExprNode handlePreviousFunctions(ExprNode defineItemExpression) throws ExprValidationException {
        ExprNodePreviousVisitorWParent previousVisitor = new ExprNodePreviousVisitorWParent();
        defineItemExpression.accept(previousVisitor);

        if (previousVisitor.getPrevious() == null) {
            return defineItemExpression;
        }

        for (Pair<ExprNode, ExprPreviousNode> previousNodePair : previousVisitor.getPrevious()) {
            ExprPreviousNode previousNode = previousNodePair.getSecond();
            ExprPreviousMatchRecognizeNode matchRecogPrevNode = new ExprPreviousMatchRecognizeNode();

            if (previousNodePair.getSecond().getChildNodes().length == 1) {
                matchRecogPrevNode.addChildNode(previousNode.getChildNodes()[0]);
                matchRecogPrevNode.addChildNode(new ExprConstantNodeImpl(1));
            } else if (previousNodePair.getSecond().getChildNodes().length == 2) {
                ExprNode first = previousNode.getChildNodes()[0];
                ExprNode second = previousNode.getChildNodes()[1];
                if ((first.isConstantResult()) && (!second.isConstantResult())) {
                    matchRecogPrevNode.addChildNode(second);
                    matchRecogPrevNode.addChildNode(first);
                } else if ((!first.isConstantResult()) && (second.isConstantResult())) {
                    matchRecogPrevNode.addChildNode(first);
                    matchRecogPrevNode.addChildNode(second);
                } else {
                    throw new ExprValidationException("PREV operator requires a constant index");
                }
            }

            if (previousNodePair.getFirst() == null) {
                defineItemExpression = matchRecogPrevNode;
            } else {
                ExprNodeUtilityCore.replaceChildNode(previousNodePair.getFirst(), previousNodePair.getSecond(), matchRecogPrevNode);
            }

            // store in a list per index such that we can consolidate this into a single buffer
            int index = matchRecogPrevNode.getConstantIndexNumber();
            List<ExprPreviousMatchRecognizeNode> callbackList = callbacksPerIndex.get(index);
            if (callbackList == null) {
                callbackList = new ArrayList<ExprPreviousMatchRecognizeNode>();
                callbacksPerIndex.put(index, callbackList);
            }
            callbackList.add(matchRecogPrevNode);
        }

        return defineItemExpression;
    }

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParameters) throws ViewParameterException {
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {

        EventRowRegexNFAViewScheduler scheduler = null;
        if (matchRecognizeSpec.getInterval() != null) {
            scheduler = new EventRowRegexNFAViewSchedulerImpl();
        }

        EventRowRegexNFAView view = new EventRowRegexNFAView(this,
                agentInstanceViewFactoryContext.getAgentInstanceContext(),
                scheduler
        );

        if (scheduler != null) {
            scheduler.setScheduleCallback(agentInstanceViewFactoryContext.getAgentInstanceContext(), view);
        }

        return view;
    }

    public EventType getEventType() {
        return rowEventType;
    }

    public List<AggregationServiceAggExpressionDesc> getAggregationExpressions() {
        return aggregationExpressions;
    }

    public AggregationServiceMatchRecognize getAggregationService() {
        return aggregationService;
    }

    public Set<ExprPreviousMatchRecognizeNode> getPreviousExprNodes() {
        if (callbacksPerIndex.isEmpty()) {
            return Collections.emptySet();
        }
        Set<ExprPreviousMatchRecognizeNode> nodes = new HashSet<ExprPreviousMatchRecognizeNode>();
        for (List<ExprPreviousMatchRecognizeNode> list : callbacksPerIndex.values()) {
            for (ExprPreviousMatchRecognizeNode node : list) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public String getViewName() {
        return "Match-recognize";
    }

    public RegexNFAState[] getAllStates() {
        return allStates;
    }

    public int getNumEventsEventsPerStreamDefine() {
        return numEventsEventsPerStreamDefine;
    }
}
