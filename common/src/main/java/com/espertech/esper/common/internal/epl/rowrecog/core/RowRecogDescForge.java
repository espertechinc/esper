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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameMatchRecognizeAgg;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.compile.stage1.spec.MatchRecognizeSkipEnum;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateBase;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class RowRecogDescForge {
    private final EventType parentEventType;
    private final EventType rowEventType;
    private final EventType compositeEventType;
    private final EventType multimatchEventType;
    private final int[] multimatchStreamNumToVariable;
    private final int[] multimatchVariableToStreamNum;
    private final ExprNode[] partitionBy;
    private final MultiKeyClassRef partitionByMultiKey;
    private final LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams;
    private final boolean hasInterval;
    private final boolean iterateOnly;
    private final boolean unbound;
    private final boolean orTerminated;
    private final boolean collectMultimatches;
    private final boolean defineAsksMultimatches;
    private final int numEventsEventsPerStreamDefine;
    private final String[] multimatchVariablesArray;
    private final RowRecogNFAStateForge[] startStates;
    private final RowRecogNFAStateForge[] allStates;
    private final boolean allMatches;
    private final MatchRecognizeSkipEnum skip;
    private final ExprNode[] columnEvaluators;
    private final String[] columnNames;
    private final TimePeriodComputeForge intervalCompute;
    private final int[] previousRandomAccessIndexes;
    private final AggregationServiceForgeDesc[] aggregationServices;

    public RowRecogDescForge(EventType parentEventType, EventType rowEventType, EventType compositeEventType, EventType multimatchEventType, int[] multimatchStreamNumToVariable, int[] multimatchVariableToStreamNum, ExprNode[] partitionBy, MultiKeyClassRef partitionByMultiKey, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, boolean hasInterval, boolean iterateOnly, boolean unbound, boolean orTerminated, boolean collectMultimatches, boolean defineAsksMultimatches, int numEventsEventsPerStreamDefine, String[] multimatchVariablesArray, RowRecogNFAStateForge[] startStates, RowRecogNFAStateForge[] allStates, boolean allMatches, MatchRecognizeSkipEnum skip, ExprNode[] columnEvaluators, String[] columnNames, TimePeriodComputeForge intervalCompute, int[] previousRandomAccessIndexes, AggregationServiceForgeDesc[] aggregationServices) {
        this.parentEventType = parentEventType;
        this.rowEventType = rowEventType;
        this.compositeEventType = compositeEventType;
        this.multimatchEventType = multimatchEventType;
        this.multimatchStreamNumToVariable = multimatchStreamNumToVariable;
        this.multimatchVariableToStreamNum = multimatchVariableToStreamNum;
        this.partitionBy = partitionBy;
        this.partitionByMultiKey = partitionByMultiKey;
        this.variableStreams = variableStreams;
        this.hasInterval = hasInterval;
        this.iterateOnly = iterateOnly;
        this.unbound = unbound;
        this.orTerminated = orTerminated;
        this.collectMultimatches = collectMultimatches;
        this.defineAsksMultimatches = defineAsksMultimatches;
        this.numEventsEventsPerStreamDefine = numEventsEventsPerStreamDefine;
        this.multimatchVariablesArray = multimatchVariablesArray;
        this.startStates = startStates;
        this.allStates = allStates;
        this.allMatches = allMatches;
        this.skip = skip;
        this.columnEvaluators = columnEvaluators;
        this.columnNames = columnNames;
        this.intervalCompute = intervalCompute;
        this.previousRandomAccessIndexes = previousRandomAccessIndexes;
        this.aggregationServices = aggregationServices;
    }

    public EventType getRowEventType() {
        return rowEventType;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(RowRecogDesc.class, this.getClass(), classScope);
        CodegenExpressionRef desc = ref("desc");
        CodegenExpression init = symbols.getAddInitSvc(method);

        int[] startStateNums = new int[startStates.length];
        for (int i = 0; i < startStates.length; i++) {
            startStateNums[i] = startStates[i].getNodeNumFlat();
        }

        CodegenExpression aggregationServiceFactories = constantNull();
        if (aggregationServices != null) {
            CodegenExpression[] initAggsSvcs = new CodegenExpression[aggregationServices.length];
            for (int i = 0; i < aggregationServices.length; i++) {
                initAggsSvcs[i] = constantNull();
                if (aggregationServices[i] != null) {
                    AggregationServiceForgeDesc aggSvc = aggregationServices[i];
                    AggregationClassNames aggregationClassNames = new AggregationClassNames("_mra" + i);
                    AggregationServiceFactoryMakeResult result = AggregationServiceFactoryCompiler.makeInnerClassesAndInit(false, aggSvc.getAggregationServiceFactoryForge(), method, classScope, classScope.getOutermostClassName(), aggregationClassNames);
                    classScope.addInnerClasses(result.getInnerClasses());
                    initAggsSvcs[i] = localMethod(result.getInitMethod(), symbols.getAddInitSvc(parent));
                }
            }
            aggregationServiceFactories = newArrayWithInit(AggregationServiceFactory.class, initAggsSvcs);
        }

        method.getBlock()
            .declareVar(RowRecogDesc.class, desc.getRef(), newInstance(RowRecogDesc.class))
            .exprDotMethod(desc, "setParentEventType", EventTypeUtility.resolveTypeCodegen(parentEventType, init))
            .exprDotMethod(desc, "setRowEventType", EventTypeUtility.resolveTypeCodegen(rowEventType, init))
            .exprDotMethod(desc, "setCompositeEventType", EventTypeUtility.resolveTypeCodegen(compositeEventType, init))
            .exprDotMethod(desc, "setMultimatchEventType", multimatchEventType == null ? constantNull() : EventTypeUtility.resolveTypeCodegen(multimatchEventType, init))
            .exprDotMethod(desc, "setMultimatchStreamNumToVariable", constant(multimatchStreamNumToVariable))
            .exprDotMethod(desc, "setMultimatchVariableToStreamNum", constant(multimatchVariableToStreamNum))
            .exprDotMethod(desc, "setPartitionEvalMayNull", MultiKeyCodegen.codegenExprEvaluatorMayMultikey(partitionBy, null, partitionByMultiKey, method, classScope))
            .exprDotMethod(desc, "setPartitionEvalTypes", partitionBy == null ? constantNull() : constant(ExprNodeUtilityQuery.getExprResultTypes(partitionBy)))
            .exprDotMethod(desc, "setPartitionEvalSerde", partitionBy == null ? constantNull() : partitionByMultiKey.getExprMKSerde(method, classScope))
            .exprDotMethod(desc, "setVariableStreams", makeVariableStreams(method, symbols, classScope))
            .exprDotMethod(desc, "setHasInterval", constant(hasInterval))
            .exprDotMethod(desc, "setIterateOnly", constant(iterateOnly))
            .exprDotMethod(desc, "setUnbound", constant(unbound))
            .exprDotMethod(desc, "setOrTerminated", constant(orTerminated))
            .exprDotMethod(desc, "setCollectMultimatches", constant(collectMultimatches))
            .exprDotMethod(desc, "setDefineAsksMultimatches", constant(defineAsksMultimatches))
            .exprDotMethod(desc, "setNumEventsEventsPerStreamDefine", constant(numEventsEventsPerStreamDefine))
            .exprDotMethod(desc, "setMultimatchVariablesArray", constant(multimatchVariablesArray))
            .exprDotMethod(desc, "setStatesOrdered", makeStates(method, symbols, classScope))
            .exprDotMethod(desc, "setNextStatesPerState", makeNextStates(method, classScope))
            .exprDotMethod(desc, "setStartStates", constant(startStateNums))
            .exprDotMethod(desc, "setAllMatches", constant(allMatches))
            .exprDotMethod(desc, "setSkip", constant(skip))
            .exprDotMethod(desc, "setColumnEvaluators", ExprNodeUtilityCodegen.codegenEvaluators(columnEvaluators, method, this.getClass(), classScope))
            .exprDotMethod(desc, "setColumnNames", constant(columnNames))
            .exprDotMethod(desc, "setIntervalCompute", intervalCompute == null ? constantNull() : intervalCompute.makeEvaluator(method, classScope))
            .exprDotMethod(desc, "setPreviousRandomAccessIndexes", constant(previousRandomAccessIndexes))
            .exprDotMethod(desc, "setAggregationServiceFactories", aggregationServiceFactories)
            .exprDotMethod(desc, "setAggregationResultFutureAssignables", aggregationServices == null ? constantNull() : makeAggAssignables(method, classScope))
            .methodReturn(desc);
        return localMethod(method);
    }

    private CodegenExpression makeAggAssignables(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationResultFutureAssignable[].class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(AggregationResultFutureAssignable[].class, "assignables", newArrayByLength(AggregationResultFutureAssignable.class, constant(aggregationServices.length)));

        for (int i = 0; i < aggregationServices.length; i++) {
            if (aggregationServices[i] != null) {
                CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), AggregationResultFutureAssignable.class);
                CodegenMethod assign = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(AggregationResultFuture.class, "future");
                anonymousClass.addMethod("assign", assign);

                CodegenExpression field = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameMatchRecognizeAgg(i), AggregationResultFuture.class);
                assign.getBlock().assignRef(field, ref("future"));

                method.getBlock().assignArrayElement(ref("assignables"), constant(i), anonymousClass);
            }
        }

        method.getBlock().methodReturn(ref("assignables"));
        return localMethod(method);
    }

    private CodegenExpression makeStates(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(RowRecogNFAStateBase[].class, this.getClass(), classScope);
        method.getBlock().declareVar(RowRecogNFAStateBase[].class, "states", newArrayByLength(RowRecogNFAStateBase.class, constant(allStates.length)));
        for (int i = 0; i < allStates.length; i++) {
            method.getBlock().assignArrayElement("states", constant(i), allStates[i].make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("states"));
        return localMethod(method);
    }

    private CodegenExpression makeNextStates(CodegenMethodScope parent, CodegenClassScope classScope) {
        List<Pair<Integer, int[]>> nextStates = new ArrayList<>();
        for (RowRecogNFAStateForge state : allStates) {
            int[] next = new int[state.getNextStates().size()];
            for (int i = 0; i < next.length; i++) {
                next[i] = state.getNextStates().get(i).getNodeNumFlat();
            }
            nextStates.add(new Pair<>(state.getNodeNumFlat(), next));
        }

        CodegenMethod method = parent.makeChild(List.class, this.getClass(), classScope);
        method.getBlock().declareVar(List.class, "next", newInstance(ArrayList.class, constant(nextStates.size())));
        for (Pair<Integer, int[]> pair : nextStates) {
            method.getBlock().exprDotMethod(ref("next"), "add", newInstance(Pair.class, constant(pair.getFirst()), constant(pair.getSecond())));
        }
        method.getBlock().methodReturn(ref("next"));
        return localMethod(method);
    }

    private CodegenExpression makeVariableStreams(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(LinkedHashMap.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(LinkedHashMap.class, "vars", newInstance(LinkedHashMap.class, constant(CollectionUtil.capacityHashMap(variableStreams.size()))));
        for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
            method.getBlock().exprDotMethod(ref("vars"), "put", constant(entry.getKey()),
                newInstance(Pair.class, constant(entry.getValue().getFirst()), constant(entry.getValue().getSecond())));
        }
        method.getBlock().methodReturn(ref("vars"));
        return localMethod(method);
    }
}
