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
package com.espertech.esper.common.internal.epl.rowrecog.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.stage1.spec.MatchRecognizeDefineItem;
import com.espertech.esper.common.internal.compile.stage1.spec.MatchRecognizeMeasureItem;
import com.espertech.esper.common.internal.compile.stage1.spec.MatchRecognizeSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceForgeDesc;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.prev.ExprPreviousMatchRecognizeNode;
import com.espertech.esper.common.internal.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodePreviousVisitorWParent;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeStreamRequiredVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeStreamUseCollectVisitor;
import com.espertech.esper.common.internal.epl.rowrecog.expr.RowRecogExprNode;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateForge;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Plan match-recognize.
 */
public class RowRecogNFAViewPlanUtil {

    private static final Logger log = LoggerFactory.getLogger(RowRecogNFAViewFactoryForge.class);

    public static RowRecogPlan validateAndPlan(EventType parentEventType,
                                                    boolean unbound,
                                                    StatementBaseInfo base,
                                                    StatementCompileTimeServices services)
            throws ExprValidationException {

        StatementRawInfo statementRawInfo = base.getStatementRawInfo();
        MatchRecognizeSpec matchRecognizeSpec = base.getStatementSpec().getRaw().getMatchRecognizeSpec();
        Annotation[] annotations = statementRawInfo.getAnnotations();
        boolean iterateOnly = HintEnum.ITERATE_ONLY.getHint(annotations) != null;
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // Expanded pattern already there
        RowRecogExprNode expandedPatternNode = matchRecognizeSpec.getPattern();

        // Determine single-row and multiple-row variables
        LinkedHashSet<String> variablesSingle = new LinkedHashSet<>();
        Set<String> variablesMultiple = new LinkedHashSet<>();
        RowRecogHelper.recursiveInspectVariables(expandedPatternNode, false, variablesSingle, variablesMultiple);

        // each variable gets associated with a stream number (multiple-row variables as well to hold the current event for the expression).
        int streamNum = 0;
        LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams = new LinkedHashMap<>();
        for (String variableSingle : variablesSingle) {
            variableStreams.put(variableSingle, new Pair<>(streamNum, false));
            streamNum++;
        }
        for (String variableMultiple : variablesMultiple) {
            variableStreams.put(variableMultiple, new Pair<>(streamNum, true));
            streamNum++;
        }

        // mapping of stream to variable
        TreeMap<Integer, String> streamVariables = new TreeMap<>();
        for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
            streamVariables.put(entry.getValue().getFirst(), entry.getKey());
        }

        // determine visibility rules
        Map<String, Set<String>> visibility = RowRecogHelper.determineVisibility(expandedPatternNode);

        // assemble all single-row variables for expression validation
        String[] allStreamNames = new String[variableStreams.size()];
        EventType[] allTypes = new EventType[variableStreams.size()];

        streamNum = 0;
        for (String variableSingle : variablesSingle) {
            allStreamNames[streamNum] = variableSingle;
            allTypes[streamNum] = parentEventType;
            streamNum++;
        }
        for (String variableMultiple : variablesMultiple) {
            allStreamNames[streamNum] = variableMultiple;
            allTypes[streamNum] = parentEventType;
            streamNum++;
        }

        // determine type service for use with DEFINE
        // validate each DEFINE clause expression
        Set<String> definedVariables = new HashSet<>();
        List<ExprAggregateNode> aggregateNodes = new ArrayList<>();
        boolean[] isExprRequiresMultimatchState = new boolean[variableStreams.size()];
        TreeMap<Integer, List<ExprPreviousMatchRecognizeNode>> previousNodes = new TreeMap<>();

        for (int defineIndex = 0; defineIndex < matchRecognizeSpec.getDefines().size(); defineIndex++) {
            MatchRecognizeDefineItem defineItem = matchRecognizeSpec.getDefines().get(defineIndex);
            if (definedVariables.contains(defineItem.getIdentifier())) {
                throw new ExprValidationException("Variable '" + defineItem.getIdentifier() + "' has already been defined");
            }
            definedVariables.add(defineItem.getIdentifier());

            // stream-type visibilities handled here
            StreamTypeService typeServiceDefines = buildDefineStreamTypeServiceDefine(defineIndex, variableStreams, defineItem, visibility, parentEventType, statementRawInfo, services);

            ExprNode exprNodeResult = handlePreviousFunctions(defineItem.getExpression(), previousNodes);
            ExprValidationContext validationContext = new ExprValidationContextBuilder(typeServiceDefines, statementRawInfo, services)
                    .withAllowBindingConsumption(true)
                    .withDisablePropertyExpressionEventCollCache(true)
                    .build();

            ExprNode validated;
            try {
                // validate
                validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGDEFINE, exprNodeResult, validationContext);

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
        boolean defineAsksMultimatches = CollectionUtil.isAnySet(isExprRequiresMultimatchState);

        // determine type service for use with MEASURE
        Map<String, Object> measureTypeDef = new LinkedHashMap<>();
        for (String variableSingle : variablesSingle) {
            measureTypeDef.put(variableSingle, parentEventType);
        }
        for (String variableMultiple : variablesMultiple) {
            measureTypeDef.put(variableMultiple, new EventType[]{parentEventType});
        }
        String compositeTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousRowrecogCompositeName();
        EventTypeMetadata compositeTypeMetadata = new EventTypeMetadata(compositeTypeName, base.getModuleName(), EventTypeTypeClass.MATCHRECOGDERIVED, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        ObjectArrayEventType compositeEventType = BaseNestableEventUtil.makeOATypeCompileTime(compositeTypeMetadata, measureTypeDef, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(compositeEventType);
        StreamTypeService compositeTypeServiceMeasure = new StreamTypeServiceImpl(compositeEventType, "MATCH_RECOGNIZE", true);

        // find MEASURE clause aggregations
        boolean measureReferencesMultivar = false;
        List<ExprAggregateNode> measureAggregateExprNodes = new ArrayList<>();
        for (MatchRecognizeMeasureItem measureItem : matchRecognizeSpec.getMeasures()) {
            ExprAggregateNodeUtil.getAggregatesBottomUp(measureItem.getExpr(), measureAggregateExprNodes);
        }

        AggregationServiceForgeDesc[] aggregationServices = null;
        if (!measureAggregateExprNodes.isEmpty()) {
            aggregationServices = planAggregations(measureAggregateExprNodes, compositeTypeServiceMeasure, allStreamNames, allTypes, streamVariables, variablesMultiple, base, services);
            for (AggregationServiceForgeDesc svc : aggregationServices) {
                if (svc != null) {
                    additionalForgeables.addAll(svc.getAdditionalForgeables());
                }
            }
        }

        // validate each MEASURE clause expression
        Map<String, Object> rowTypeDef = new LinkedHashMap<>();
        ExprNodeStreamUseCollectVisitor streamRefVisitor = new ExprNodeStreamUseCollectVisitor();
        for (MatchRecognizeMeasureItem measureItem : matchRecognizeSpec.getMeasures()) {
            if (measureItem.getName() == null) {
                throw new ExprValidationException("The measures clause requires that each expression utilizes the AS keyword to assign a column name");
            }
            ExprNode validated = validateMeasureClause(measureItem.getExpr(), compositeTypeServiceMeasure, variablesMultiple, variablesSingle, statementRawInfo, services);
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
        boolean collectMultimatches = measureReferencesMultivar || defineAsksMultimatches;

        // create rowevent type
        String rowTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousRowrecogRowName();
        EventTypeMetadata rowTypeMetadata = new EventTypeMetadata(rowTypeName, base.getModuleName(), EventTypeTypeClass.MATCHRECOGDERIVED, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        MapEventType rowEventType = BaseNestableEventUtil.makeMapTypeCompileTime(rowTypeMetadata, rowTypeDef, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(rowEventType);

        // validate partition-by expressions, if any
        ExprNode[] partitionBy;
        MultiKeyClassRef partitionMultiKey;
        if (!matchRecognizeSpec.getPartitionByExpressions().isEmpty()) {
            StreamTypeService typeServicePartition = new StreamTypeServiceImpl(parentEventType, "MATCH_RECOGNIZE_PARTITION", true);
            List<ExprNode> validated = new ArrayList<>();
            ExprValidationContext validationContext = new ExprValidationContextBuilder(typeServicePartition, statementRawInfo, services).withAllowBindingConsumption(true).build();
            for (ExprNode partitionExpr : matchRecognizeSpec.getPartitionByExpressions()) {
                validated.add(ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGPARTITION, partitionExpr, validationContext));
            }
            matchRecognizeSpec.setPartitionByExpressions(validated);
            partitionBy = ExprNodeUtilityQuery.toArray(validated);
            MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(partitionBy, false, base.getStatementRawInfo(), services.getSerdeResolver());
            partitionMultiKey = multiKeyPlan.getClassRef();
            additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());
        } else {
            partitionBy = null;
            partitionMultiKey = null;
        }

        // validate interval if present
        if (matchRecognizeSpec.getInterval() != null) {
            ExprValidationContext validationContext = new ExprValidationContextBuilder(new StreamTypeServiceImpl(false), statementRawInfo, services).withAllowBindingConsumption(true).build();
            ExprTimePeriod validated = (ExprTimePeriod) ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGINTERVAL, matchRecognizeSpec.getInterval().getTimePeriodExpr(), validationContext);
            matchRecognizeSpec.getInterval().setTimePeriodExpr(validated);
        }

        // compile variable definition expressions
        Map<String, ExprNode> variableDefinitions = new HashMap<>();
        for (MatchRecognizeDefineItem defineItem : matchRecognizeSpec.getDefines()) {
            variableDefinitions.put(defineItem.getIdentifier(), defineItem.getExpression());
        }

        // create evaluators
        String[] columnNames = new String[matchRecognizeSpec.getMeasures().size()];
        ExprNode[] columnForges = new ExprNode[matchRecognizeSpec.getMeasures().size()];
        int count = 0;
        for (MatchRecognizeMeasureItem measureItem : matchRecognizeSpec.getMeasures()) {
            columnNames[count] = measureItem.getName();
            columnForges[count] = measureItem.getExpr();
            count++;
        }

        // build states
        RowRecogNFAStrandResult strand = RowRecogHelper.buildStartStates(expandedPatternNode, variableDefinitions, variableStreams, isExprRequiresMultimatchState);
        RowRecogNFAStateForge[] startStates = strand.getStartStates().toArray(new RowRecogNFAStateForge[strand.getStartStates().size()]);
        RowRecogNFAStateForge[] allStates = strand.getAllStates().toArray(new RowRecogNFAStateForge[strand.getAllStates().size()]);

        if (log.isDebugEnabled()) {
            log.debug("NFA tree:\n" + RowRecogNFAViewUtil.print(startStates));
        }

        // determine names of multimatching variables
        String[] multimatchVariablesArray;
        int[] multimatchStreamNumToVariable;
        int[] multimatchVariableToStreamNum;
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
        int numEventsEventsPerStreamDefine = defineAsksMultimatches ? variableStreams.size() + 1 : variableStreams.size();

        // determine interval-or-terminated
        boolean orTerminated = matchRecognizeSpec.getInterval() != null && matchRecognizeSpec.getInterval().isOrTerminated();
        TimePeriodComputeForge intervalCompute = null;
        if (matchRecognizeSpec.getInterval() != null) {
            intervalCompute = matchRecognizeSpec.getInterval().getTimePeriodExpr().getTimePeriodComputeForge();
        }

        EventType multimatchEventType = null;
        if (defineAsksMultimatches) {
            multimatchEventType = getDefineMultimatchEventType(variableStreams, parentEventType, base, services);
        }

        // determine previous-access indexes and assign "prev" node indexes
        // Since an expression such as "prior(2, price), prior(8, price)" translates into {2, 8} the relative index is {0, 1}.
        // Map the expression-supplied index to a relative index
        int[] previousRandomAccessIndexes = null;
        if (!previousNodes.isEmpty()) {
            previousRandomAccessIndexes = new int[previousNodes.size()];
            int countPrev = 0;
            for (Map.Entry<Integer, List<ExprPreviousMatchRecognizeNode>> entry : previousNodes.entrySet()) {
                previousRandomAccessIndexes[countPrev] = entry.getKey();
                for (ExprPreviousMatchRecognizeNode callback : entry.getValue()) {
                    callback.setAssignedIndex(countPrev);
                }
                countPrev++;
            }
        }

        RowRecogDescForge forge = new RowRecogDescForge(parentEventType, rowEventType, compositeEventType, multimatchEventType,
                multimatchStreamNumToVariable, multimatchVariableToStreamNum,
                partitionBy, partitionMultiKey, variableStreams,
                matchRecognizeSpec.getInterval() != null, iterateOnly, unbound,
                orTerminated, collectMultimatches, defineAsksMultimatches,
                numEventsEventsPerStreamDefine, multimatchVariablesArray,
                startStates, allStates,
                matchRecognizeSpec.isAllMatches(), matchRecognizeSpec.getSkip().getSkip(),
                columnForges, columnNames,
                intervalCompute, previousRandomAccessIndexes, aggregationServices);
        return new RowRecogPlan(forge, additionalForgeables);
    }

    private static AggregationServiceForgeDesc[] planAggregations(List<ExprAggregateNode> measureAggregateExprNodes,
                                                                  StreamTypeService compositeTypeServiceMeasure, String[] allStreamNames,
                                                                  EventType[] allTypes,
                                                                  TreeMap<Integer, String> streamVariables,
                                                                  Set<String> variablesMultiple,
                                                                  StatementBaseInfo base,
                                                                  StatementCompileTimeServices services)
            throws ExprValidationException {
        Map<Integer, List<ExprAggregateNode>> measureExprAggNodesPerStream = new HashMap<>();

        for (ExprAggregateNode aggregateNode : measureAggregateExprNodes) {
            // validate node and params
            int count = 0;
            ExprNodeIdentifierVisitor visitor = new ExprNodeIdentifierVisitor(true);
            boolean[] isIStreamOnly = new boolean[allStreamNames.length];
            StreamTypeServiceImpl typeServiceAggregateMeasure = new StreamTypeServiceImpl(allTypes, allStreamNames, isIStreamOnly, false, true);

            ExprValidationContext validationContext = new ExprValidationContextBuilder(typeServiceAggregateMeasure, base.getStatementRawInfo(), services).withAllowBindingConsumption(true).build();
            aggregateNode.validatePositionals(validationContext);

            if (aggregateNode.getOptionalLocalGroupBy() != null) {
                throw new ExprValidationException("Match-recognize does not allow aggregation functions to specify a group-by");
            }

            for (ExprNode child : aggregateNode.getChildNodes()) {
                ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGMEASURE, child, validationContext);
                validated.accept(visitor);
                aggregateNode.setChildNode(count++, new ExprNodeValidated(validated));
            }

            // verify properties used within the aggregation
            Set<Integer> aggregatedStreams = new HashSet<>();
            for (Pair<Integer, String> pair : visitor.getExprProperties()) {
                aggregatedStreams.add(pair.getFirst());
            }

            Integer multipleVarStream = null;
            for (int streamNumAggregated : aggregatedStreams) {
                String variable = streamVariables.get(streamNumAggregated);
                if (variablesMultiple.contains(variable)) {
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
                aggNodesForStream = new ArrayList<>();
                measureExprAggNodesPerStream.put(multipleVarStream, aggNodesForStream);
            }
            aggNodesForStream.add(aggregateNode);
        }

        // validate aggregation itself
        for (Map.Entry<Integer, List<ExprAggregateNode>> entry : measureExprAggNodesPerStream.entrySet()) {
            for (ExprAggregateNode aggregateNode : entry.getValue()) {
                ExprValidationContext validationContext = new ExprValidationContextBuilder(compositeTypeServiceMeasure, base.getStatementRawInfo(), services)
                        .withAllowBindingConsumption(true)
                        .withMemberName(new ExprValidationMemberNameQualifiedRowRecogAgg(entry.getKey()))
                        .build();
                aggregateNode.validate(validationContext);
            }
        }

        // get aggregation service per variable
        AggregationServiceForgeDesc[] aggServices = new AggregationServiceForgeDesc[allStreamNames.length];
        List<ExprDeclaredNode> declareds = Arrays.asList(base.getStatementSpec().getDeclaredExpressions());
        for (Map.Entry<Integer, List<ExprAggregateNode>> entry : measureExprAggNodesPerStream.entrySet()) {

            EventType[] typesPerStream = new EventType[]{allTypes[entry.getKey()]};
            AggregationServiceForgeDesc desc = AggregationServiceFactoryFactory.getService(
                    entry.getValue(), Collections.emptyMap(), declareds,
                    new ExprNode[0], null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    false, base.getStatementRawInfo().getAnnotations(),
                    services.getVariableCompileTimeResolver(), true, null, null,
                    typesPerStream, null, base.getContextName(), null, services.getTableCompileTimeResolver(),
                    false, true, false, services.getClasspathImportServiceCompileTime(), base.getStatementRawInfo(), services.getSerdeResolver());
            aggServices[entry.getKey()] = desc;
        }

        return aggServices;
    }

    private static EventType getDefineMultimatchEventType(LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, EventType parentEventType, StatementBaseInfo base, StatementCompileTimeServices services) {
        Map<String, Object> multievent = new LinkedHashMap<>();
        for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
            if (entry.getValue().getSecond()) {
                multievent.put(entry.getKey(), new EventType[]{parentEventType});
            }
        }

        String multimatchAllTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousRowrecogMultimatchAllName();
        EventTypeMetadata multimatchAllTypeMetadata = new EventTypeMetadata(multimatchAllTypeName, base.getModuleName(), EventTypeTypeClass.MATCHRECOGDERIVED, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        ObjectArrayEventType multimatchAllEventType = BaseNestableEventUtil.makeOATypeCompileTime(multimatchAllTypeMetadata, multievent, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(multimatchAllEventType);
        return multimatchAllEventType;
    }

    private static ExprNode validateMeasureClause(ExprNode measureNode, StreamTypeService typeServiceMeasure, Set<String> variablesMultiple, Set<String> variablesSingle, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {
        try {
            ExprValidationContext validationContext = new ExprValidationContextBuilder(typeServiceMeasure, statementRawInfo, services).withAllowBindingConsumption(true)
                    .withDisablePropertyExpressionEventCollCache(true).withAggregationFutureNameAlreadySet(true).build();
            return ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.MATCHRECOGMEASURE, measureNode, validationContext);
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

    private static ExprNode handlePreviousFunctions(ExprNode defineItemExpression, TreeMap<Integer, List<ExprPreviousMatchRecognizeNode>> previousNodes) throws ExprValidationException {
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
                if ((first instanceof ExprConstantNode) && (!(second instanceof ExprConstantNode))) {
                    matchRecogPrevNode.addChildNode(second);
                    matchRecogPrevNode.addChildNode(first);
                } else if ((!(first instanceof ExprConstantNode)) && (second instanceof ExprConstantNode)) {
                    matchRecogPrevNode.addChildNode(first);
                    matchRecogPrevNode.addChildNode(second);
                } else {
                    throw new ExprValidationException("PREV operator requires a constant index");
                }
            }

            if (previousNodePair.getFirst() == null) {
                defineItemExpression = matchRecogPrevNode;
            } else {
                ExprNodeUtilityModify.replaceChildNode(previousNodePair.getFirst(), previousNodePair.getSecond(), matchRecogPrevNode);
            }

            // store in a list per index such that we can consolidate this into a single buffer
            int index = matchRecogPrevNode.getConstantIndexNumber();
            List<ExprPreviousMatchRecognizeNode> callbackList = previousNodes.get(index);
            if (callbackList == null) {
                callbackList = new ArrayList<>();
                previousNodes.put(index, callbackList);
            }
            callbackList.add(matchRecogPrevNode);
        }

        return defineItemExpression;
    }

    private static StreamTypeService buildDefineStreamTypeServiceDefine(int defineNum,
                                                                        LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams,
                                                                        MatchRecognizeDefineItem defineItem,
                                                                        Map<String, Set<String>> visibilityByIdentifier,
                                                                        EventType parentViewType,
                                                                        StatementRawInfo statementRawInfo,
                                                                        StatementCompileTimeServices services)
            throws ExprValidationException {
        if (!variableStreams.containsKey(defineItem.getIdentifier())) {
            throw new ExprValidationException("Variable '" + defineItem.getIdentifier() + "' does not occur in pattern");
        }

        String[] streamNamesDefine = new String[variableStreams.size() + 1];
        EventType[] typesDefine = new EventType[variableStreams.size() + 1];
        boolean[] isIStreamOnly = new boolean[variableStreams.size() + 1];
        Arrays.fill(isIStreamOnly, true);

        int streamNumDefine = variableStreams.get(defineItem.getIdentifier()).getFirst();
        streamNamesDefine[streamNumDefine] = defineItem.getIdentifier();
        typesDefine[streamNumDefine] = parentViewType;

        // add visible single-value
        Set<String> visibles = visibilityByIdentifier.get(defineItem.getIdentifier());
        boolean hasVisibleMultimatch = false;
        if (visibles != null) {
            for (String visible : visibles) {
                Pair<Integer, Boolean> def = variableStreams.get(visible);
                if (!def.getSecond()) {
                    streamNamesDefine[def.getFirst()] = visible;
                    typesDefine[def.getFirst()] = parentViewType;
                } else {
                    hasVisibleMultimatch = true;
                }
            }
        }

        // compile multi-matching event type (in last position), if any are used
        if (hasVisibleMultimatch) {
            Map<String, Object> multievent = new LinkedHashMap<>();
            for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
                String identifier = entry.getKey();
                if (entry.getValue().getSecond()) {
                    if (visibles.contains(identifier)) {
                        multievent.put(identifier, new EventType[]{parentViewType});
                    } else {
                        multievent.put("esper_matchrecog_internal", null);
                    }
                }
            }

            String multimatchTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousRowrecogMultimatchDefineName(defineNum);
            EventTypeMetadata multimatchTypeMetadata = new EventTypeMetadata(multimatchTypeName, statementRawInfo.getModuleName(), EventTypeTypeClass.MATCHRECOGDERIVED, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
            ObjectArrayEventType multimatchEventType = BaseNestableEventUtil.makeOATypeCompileTime(multimatchTypeMetadata, multievent, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());

            typesDefine[typesDefine.length - 1] = multimatchEventType;
            streamNamesDefine[streamNamesDefine.length - 1] = multimatchEventType.getName();
        }

        return new StreamTypeServiceImpl(typesDefine, streamNamesDefine, isIStreamOnly, false, true);
    }
}
