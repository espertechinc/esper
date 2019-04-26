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
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.MatchRecognizeSkipEnum;
import com.espertech.esper.common.internal.epl.agg.core.AggregationResultFutureAssignable;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateBase;

import java.util.LinkedHashMap;
import java.util.List;

public class RowRecogDesc {
    private EventType parentEventType;
    private EventType rowEventType;
    private EventType compositeEventType;
    private EventType multimatchEventType;
    private int[] multimatchStreamNumToVariable;
    private int[] multimatchVariableToStreamNum;
    private ExprEvaluator partitionEvalMayNull;
    private Class[] partitionEvalTypes;
    private DataInputOutputSerde<Object> partitionEvalSerde;
    private LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams;
    private boolean hasInterval;
    private boolean iterateOnly;
    private boolean unbound;
    private boolean orTerminated;
    private boolean collectMultimatches;
    private boolean defineAsksMultimatches;
    private int numEventsEventsPerStreamDefine;
    private String[] multimatchVariablesArray;
    private RowRecogNFAStateBase[] statesOrdered;
    private List<Pair<Integer, int[]>> nextStatesPerState;
    private int[] startStates;
    private boolean allMatches;
    private MatchRecognizeSkipEnum skip;
    private ExprEvaluator[] columnEvaluators;
    private String[] columnNames;
    private TimePeriodCompute intervalCompute;
    private int[] previousRandomAccessIndexes;
    private AggregationServiceFactory[] aggregationServiceFactories;
    private AggregationResultFutureAssignable[] aggregationResultFutureAssignables;

    public EventType getParentEventType() {
        return parentEventType;
    }

    public void setParentEventType(EventType parentEventType) {
        this.parentEventType = parentEventType;
    }

    public EventType getMultimatchEventType() {
        return multimatchEventType;
    }

    public void setMultimatchEventType(EventType multimatchEventType) {
        this.multimatchEventType = multimatchEventType;
    }

    public void setRowEventType(EventType rowEventType) {
        this.rowEventType = rowEventType;
    }

    public EventType getRowEventType() {
        return rowEventType;
    }

    public EventType getCompositeEventType() {
        return compositeEventType;
    }

    public void setCompositeEventType(EventType compositeEventType) {
        this.compositeEventType = compositeEventType;
    }

    public void setMultimatchStreamNumToVariable(int[] multimatchStreamNumToVariable) {
        this.multimatchStreamNumToVariable = multimatchStreamNumToVariable;
    }

    public void setPartitionEvalMayNull(ExprEvaluator partitionEvalMayNull) {
        this.partitionEvalMayNull = partitionEvalMayNull;
    }

    public Class[] getPartitionEvalTypes() {
        return partitionEvalTypes;
    }

    public void setPartitionEvalTypes(Class[] partitionEvalTypes) {
        this.partitionEvalTypes = partitionEvalTypes;
    }

    public DataInputOutputSerde<Object> getPartitionEvalSerde() {
        return partitionEvalSerde;
    }

    public void setPartitionEvalSerde(DataInputOutputSerde<Object> partitionEvalSerde) {
        this.partitionEvalSerde = partitionEvalSerde;
    }

    public int[] getMultimatchStreamNumToVariable() {
        return multimatchStreamNumToVariable;
    }

    public ExprEvaluator getPartitionEvalMayNull() {
        return partitionEvalMayNull;
    }

    public LinkedHashMap<String, Pair<Integer, Boolean>> getVariableStreams() {
        return variableStreams;
    }

    public void setVariableStreams(LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams) {
        this.variableStreams = variableStreams;
    }

    public boolean isHasInterval() {
        return hasInterval;
    }

    public void setHasInterval(boolean hasInterval) {
        this.hasInterval = hasInterval;
    }

    public boolean isIterateOnly() {
        return iterateOnly;
    }

    public void setIterateOnly(boolean iterateOnly) {
        this.iterateOnly = iterateOnly;
    }

    public boolean isUnbound() {
        return unbound;
    }

    public void setUnbound(boolean unbound) {
        this.unbound = unbound;
    }

    public boolean isOrTerminated() {
        return orTerminated;
    }

    public void setOrTerminated(boolean orTerminated) {
        this.orTerminated = orTerminated;
    }

    public boolean isCollectMultimatches() {
        return collectMultimatches;
    }

    public void setCollectMultimatches(boolean collectMultimatches) {
        this.collectMultimatches = collectMultimatches;
    }

    public boolean isDefineAsksMultimatches() {
        return defineAsksMultimatches;
    }

    public void setDefineAsksMultimatches(boolean defineAsksMultimatches) {
        this.defineAsksMultimatches = defineAsksMultimatches;
    }

    public int getNumEventsEventsPerStreamDefine() {
        return numEventsEventsPerStreamDefine;
    }

    public void setNumEventsEventsPerStreamDefine(int numEventsEventsPerStreamDefine) {
        this.numEventsEventsPerStreamDefine = numEventsEventsPerStreamDefine;
    }

    public String[] getMultimatchVariablesArray() {
        return multimatchVariablesArray;
    }

    public void setMultimatchVariablesArray(String[] multimatchVariablesArray) {
        this.multimatchVariablesArray = multimatchVariablesArray;
    }

    public RowRecogNFAStateBase[] getStatesOrdered() {
        return statesOrdered;
    }

    public void setStatesOrdered(RowRecogNFAStateBase[] statesOrdered) {
        this.statesOrdered = statesOrdered;
    }

    public List<Pair<Integer, int[]>> getNextStatesPerState() {
        return nextStatesPerState;
    }

    public void setNextStatesPerState(List<Pair<Integer, int[]>> nextStatesPerState) {
        this.nextStatesPerState = nextStatesPerState;
    }

    public int[] getStartStates() {
        return startStates;
    }

    public void setStartStates(int[] startStates) {
        this.startStates = startStates;
    }

    public boolean isAllMatches() {
        return allMatches;
    }

    public void setAllMatches(boolean allMatches) {
        this.allMatches = allMatches;
    }

    public MatchRecognizeSkipEnum getSkip() {
        return skip;
    }

    public void setSkip(MatchRecognizeSkipEnum skip) {
        this.skip = skip;
    }

    public ExprEvaluator[] getColumnEvaluators() {
        return columnEvaluators;
    }

    public void setColumnEvaluators(ExprEvaluator[] columnEvaluators) {
        this.columnEvaluators = columnEvaluators;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public int[] getMultimatchVariableToStreamNum() {
        return multimatchVariableToStreamNum;
    }

    public void setMultimatchVariableToStreamNum(int[] multimatchVariableToStreamNum) {
        this.multimatchVariableToStreamNum = multimatchVariableToStreamNum;
    }

    public TimePeriodCompute getIntervalCompute() {
        return intervalCompute;
    }

    public void setIntervalCompute(TimePeriodCompute intervalCompute) {
        this.intervalCompute = intervalCompute;
    }

    public int[] getPreviousRandomAccessIndexes() {
        return previousRandomAccessIndexes;
    }

    public void setPreviousRandomAccessIndexes(int[] previousRandomAccessIndexes) {
        this.previousRandomAccessIndexes = previousRandomAccessIndexes;
    }

    public AggregationServiceFactory[] getAggregationServiceFactories() {
        return aggregationServiceFactories;
    }

    public void setAggregationServiceFactories(AggregationServiceFactory[] aggregationServiceFactories) {
        this.aggregationServiceFactories = aggregationServiceFactories;
    }

    public AggregationResultFutureAssignable[] getAggregationResultFutureAssignables() {
        return aggregationResultFutureAssignables;
    }

    public void setAggregationResultFutureAssignables(AggregationResultFutureAssignable[] aggregationResultFutureAssignables) {
        this.aggregationResultFutureAssignables = aggregationResultFutureAssignables;
    }
}
