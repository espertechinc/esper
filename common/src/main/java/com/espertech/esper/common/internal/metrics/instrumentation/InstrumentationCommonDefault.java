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
package com.espertech.esper.common.internal.metrics.instrumentation;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.ContextRuntimeDescriptor;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.context.util.InternalEventRouterEntry;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerView;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowDeltaData;
import com.espertech.esper.common.internal.epl.pattern.and.EvalAndFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.every.EvalEveryFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.everydistinct.EvalEveryDistinctFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.followedby.EvalFollowedByFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.guard.EvalGuardFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.matchuntil.EvalMatchUntilFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.not.EvalNotFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EvalObserverFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.or.EvalOrFactoryNode;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAState;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEntry;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogPartitionState;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapMinimal;
import com.espertech.esper.common.internal.view.core.ViewFactory;

import java.util.*;

public class InstrumentationCommonDefault implements InstrumentationCommon {
    public final static InstrumentationCommonDefault INSTANCE = new InstrumentationCommonDefault();

    public InstrumentationCommonDefault() {
    }

    public boolean activated() {
        return false;
    }

    public void qNamedWindowDispatch(String runtimeURI) {

    }

    public void aNamedWindowDispatch() {

    }

    public void qNamedWindowCPSingle(String runtimeURI, int numConsumers, EventBean[] newData, EventBean[] oldData, EPStatementAgentInstanceHandle handle, long time) {

    }

    public void aNamedWindowCPSingle() {

    }

    public void qNamedWindowCPMulti(String runtimeURI, Map<NamedWindowConsumerView, NamedWindowDeltaData> deltaPerConsumer, EPStatementAgentInstanceHandle handle, long time) {

    }

    public void aNamedWindowCPMulti() {

    }

    public void qRegEx(EventBean newEvent, RowRecogPartitionState partitionState) {

    }

    public void aRegEx(RowRecogPartitionState partitionState, List<RowRecogNFAStateEntry> endStates, List<RowRecogNFAStateEntry> terminationStates) {

    }

    public void qRegExState(RowRecogNFAStateEntry currentState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void aRegExState(List<RowRecogNFAStateEntry> next, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void qRegExStateStart(RowRecogNFAState startState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void aRegExStateStart(List<RowRecogNFAStateEntry> nextStates, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void qRegExPartition(EventBean theEvent) {

    }

    public void aRegExPartition(boolean exists, Object partitionKey, RowRecogPartitionState state) {

    }

    public void qRegIntervalValue() {

    }

    public void aRegIntervalValue(long result) {

    }

    public void qRegIntervalState(RowRecogNFAStateEntry endState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable, long runtimeTime) {

    }

    public void aRegIntervalState(boolean scheduled) {

    }

    public void qRegOut(EventBean[] outBeans) {

    }

    public void aRegOut() {

    }

    public void qRegMeasure(RowRecogNFAStateEntry endState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void aRegMeasure(EventBean outBean) {

    }

    public void qRegExScheduledEval() {

    }

    public void aRegExScheduledEval() {

    }

    public void qRegFilter(String text, EventBean[] eventsPerStream) {

    }

    public void aRegFilter(Boolean result) {

    }

    public void qFilterActivationStream(String eventTypeName, int streamNumber, AgentInstanceContext agentInstanceContext, boolean subselect, int subselectNumber) {

    }

    public void aFilterActivationStream(AgentInstanceContext agentInstanceContext, boolean subselect, int subselectNumber) {

    }

    public void qIndexSubordLookup(SubordTableLookupStrategy subordTableLookupStrategy, EventTable optionalEventIndex, int[] keyStreamNums) {

    }

    public void aIndexSubordLookup(Collection<EventBean> events, Object keys) {

    }

    public void qViewProcessIRStream(ViewFactory viewFactory, EventBean[] newData, EventBean[] oldData) {

    }

    public void aViewProcessIRStream() {

    }

    public void qViewIndicate(ViewFactory viewFactory, EventBean[] newData, EventBean[] oldData) {

    }

    public void aViewIndicate() {

    }

    public void qViewScheduledEval(ViewFactory viewFactory) {

    }

    public void aViewScheduledEval() {

    }

    public void qPatternFilterMatch(EvalFilterFactoryNode filterNode, EventBean theEvent) {

    }

    public void aPatternFilterMatch(boolean quitted) {

    }

    public void qPatternNotEvaluateTrue(EvalNotFactoryNode evalNotNode, MatchedEventMapMinimal matchEvent) {

    }

    public void aPatternNotEvaluateTrue(boolean quitted) {

    }

    public void qPatternObserverQuit(EvalObserverFactoryNode evalObserverNode) {

    }

    public void aPatternObserverQuit() {

    }

    public void qPatternAndEvaluateFalse(EvalAndFactoryNode evalAndNode) {

    }

    public void aPatternAndEvaluateFalse() {

    }

    public void qPatternRootEvalFalse() {

    }

    public void aPatternRootEvalFalse() {

    }

    public void qPatternObserverScheduledEval() {

    }

    public void aPatternObserverScheduledEval() {

    }

    public void qPatternObserverScheduledEval(EvalObserverFactoryNode node) {

    }

    public void qPatternObserverEvaluateTrue(EvalObserverFactoryNode evalObserverNode, MatchedEventMap matchEvent) {

    }

    public void aPatternObserverEvaluateTrue() {

    }

    public void qPatternFollowedByEvaluateTrue(EvalFollowedByFactoryNode evalFollowedByNode, MatchedEventMap matchEvent, Integer index) {

    }

    public void aPatternFollowedByEvaluateTrue(boolean quitted) {

    }

    public void qPatternGuardStart(EvalGuardFactoryNode evalGuardNode, MatchedEventMap beginState) {

    }

    public void aPatternGuardStart() {

    }

    public void qPatternAndStart(EvalAndFactoryNode evalAndNode, MatchedEventMap beginState) {

    }

    public void aPatternAndStart() {

    }

    public void qPatternFilterStart(EvalFilterFactoryNode evalFilterNode, MatchedEventMap beginState) {

    }

    public void aPatternFilterStart() {

    }

    public void qPatternNotStart(EvalNotFactoryNode evalNotNode, MatchedEventMap beginState) {

    }

    public void aPatternNotStart() {

    }

    public void qPatternAndEvaluateTrue(EvalAndFactoryNode evalAndNode, MatchedEventMap passUp) {

    }

    public void aPatternAndEvaluateTrue(boolean quitted) {

    }

    public void qPatternGuardScheduledEval() {

    }

    public void aPatternGuardScheduledEval() {

    }

    public void qPatternGuardGuardQuit(EvalGuardFactoryNode evalGuardNode) {

    }

    public void aPatternGuardGuardQuit() {

    }

    public void qPatternAndQuit(EvalAndFactoryNode evalAndNode) {

    }

    public void aPatternAndQuit() {

    }

    public void qPatternFilterQuit(EvalFilterFactoryNode evalFilterNode, MatchedEventMap beginState) {

    }

    public void aPatternFilterQuit() {

    }

    public void qPatternNotQuit(EvalNotFactoryNode evalNotNode) {

    }

    public void aPatternNotQuit() {

    }

    public void qPatternNotEvalFalse(EvalNotFactoryNode evalNotNode) {

    }

    public void aPatternNotEvalFalse() {

    }

    public void qPatternRootEvaluateTrue(MatchedEventMap matchEvent) {

    }

    public void aPatternRootEvaluateTrue(boolean quitted) {

    }

    public void qPatternObserverStart(EvalObserverFactoryNode evalObserverNode, MatchedEventMap beginState) {

    }

    public void aPatternObserverStart() {

    }

    public void qPatternMatchUntilEvaluateTrue(EvalMatchUntilFactoryNode evalMatchUntilNode, MatchedEventMap matchEvent, boolean matchFromUntil) {

    }

    public void aPatternMatchUntilEvaluateTrue(boolean quitted) {

    }

    public void qPatternMatchUntilStart(EvalMatchUntilFactoryNode evalMatchUntilNode, MatchedEventMap beginState) {

    }

    public void aPatternMatchUntilStart() {

    }

    public void qPatternMatchUntilQuit(EvalMatchUntilFactoryNode evalMatchUntilNode) {

    }

    public void aPatternMatchUntilQuit() {

    }

    public void qPatternMatchUntilEvalFalse(EvalMatchUntilFactoryNode evalMatchUntilNode, boolean matchFromUntil) {

    }

    public void aPatternMatchUntilEvalFalse() {

    }

    public void qPatternGuardEvaluateTrue(EvalGuardFactoryNode evalGuardNode, MatchedEventMap matchEvent) {

    }

    public void aPatternGuardEvaluateTrue(boolean quitted) {

    }

    public void qPatternGuardQuit(EvalGuardFactoryNode evalGuardNode) {

    }

    public void aPatternGuardQuit() {

    }

    public void qPatternEveryDistinctEvaluateTrue(EvalEveryDistinctFactoryNode everyDistinctNode, MatchedEventMap matchEvent) {

    }

    public void aPatternEveryDistinctEvaluateTrue(Set<Object> keysFromNodeNoExpire, LinkedHashMap<Object, Long> keysFromNodeExpire, Object matchEventKey, boolean haveSeenThis) {

    }

    public void qPatternEveryDistinctStart(EvalEveryDistinctFactoryNode everyNode, MatchedEventMap beginState) {

    }

    public void aPatternEveryDistinctStart() {

    }

    public void qPatternEveryDistinctQuit(EvalEveryDistinctFactoryNode everyNode) {

    }

    public void aPatternEveryDistinctQuit() {

    }

    public void qPatternFollowedByEvalFalse(EvalFollowedByFactoryNode evalFollowedByNode) {

    }

    public void aPatternFollowedByEvalFalse() {

    }

    public void qPatternEveryDistinctEvalFalse(EvalEveryDistinctFactoryNode everyNode) {

    }

    public void aPatternEveryDistinctEvalFalse() {

    }

    public void qPatternEveryEvaluateTrue(EvalEveryFactoryNode evalEveryNode, MatchedEventMap matchEvent) {

    }

    public void aPatternEveryEvaluateTrue() {

    }

    public void qPatternEveryStart(EvalEveryFactoryNode evalEveryNode, MatchedEventMap beginState) {

    }

    public void aPatternEveryStart() {

    }

    public void qPatternEveryQuit(EvalEveryFactoryNode evalEveryNode) {

    }

    public void aPatternEveryQuit() {

    }

    public void qPatternEveryEvalFalse(EvalEveryFactoryNode evalEveryNode) {

    }

    public void aPatternEveryEvalFalse() {

    }

    public void qPatternOrEvaluateTrue(EvalOrFactoryNode evalOrNode, MatchedEventMap matchEvent) {

    }

    public void aPatternOrEvaluateTrue(boolean quitted) {

    }

    public void qPatternOrStart(EvalOrFactoryNode evalOrNode, MatchedEventMap beginState) {

    }

    public void aPatternOrStart() {

    }

    public void qPatternOrQuit(EvalOrFactoryNode evalOrNode) {

    }

    public void aPatternOrQuit() {

    }

    public void qPatternOrEvalFalse(EvalOrFactoryNode evalOrNode) {

    }

    public void aPatternOrEvalFalse() {

    }

    public void qPatternFollowedByStart(EvalFollowedByFactoryNode evalFollowedByNode, MatchedEventMap beginState) {

    }

    public void aPatternFollowedByStart() {

    }

    public void qPatternFollowedByQuit(EvalFollowedByFactoryNode evalFollowedByNode) {

    }

    public void aPatternFollowedByQuit() {

    }

    public void qPatternGuardEvalFalse(EvalGuardFactoryNode evalGuardNode) {

    }

    public void aPatternGuardEvalFalse() {

    }

    public void qContextScheduledEval(ContextRuntimeDescriptor contextDescriptor) {

    }

    public void aContextScheduledEval() {

    }

    public void qContextPartitionAllocate(AgentInstanceContext agentInstanceContext) {

    }

    public void aContextPartitionAllocate() {

    }

    public void qPatternRootStart(MatchedEventMap root) {

    }

    public void aPatternRootStart() {

    }

    public void qPatternRootQuit() {

    }

    public void aPatternRootQuit() {

    }

    public void qContextPartitionDestroy(AgentInstanceContext agentInstanceContext) {

    }

    public void aContextPartitionDestroy() {

    }

    public void qInfraOnAction(OnTriggerType triggerType, EventBean[] triggerEvents, EventBean[] matchingEvents) {

    }

    public void aInfraOnAction() {

    }

    public void qTableUpdatedEvent(EventBean theEvent) {

    }

    public void aTableUpdatedEvent() {

    }

    public void qInfraMergeWhenThens(boolean matched, EventBean triggerEvent, int numWhenThens) {

    }

    public void aInfraMergeWhenThens(boolean matched) {

    }

    public void qInfraMergeWhenThenItem(boolean matched, int count) {

    }

    public void aInfraMergeWhenThenItem(boolean matched, boolean actionsApplied) {

    }

    public void qInfraMergeWhenThenActions(int numActions) {

    }

    public void aInfraMergeWhenThenActions() {

    }

    public void qInfraMergeWhenThenActionItem(int count, String actionName) {

    }

    public void aInfraMergeWhenThenActionItem(boolean applies) {

    }

    public void qInfraTriggeredLookup(String lookupStrategy) {

    }

    public void aInfraTriggeredLookup(EventBean[] result) {

    }

    public void qIndexJoinLookup(JoinExecTableLookupStrategy strategy, EventTable index) {

    }

    public void aIndexJoinLookup(Set<EventBean> result, Object keys) {

    }

    public void qJoinDispatch(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream) {

    }

    public void aJoinDispatch() {

    }

    public void qJoinExecStrategy() {

    }

    public void aJoinExecStrategy(UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> joinSet) {

    }

    public void qJoinCompositionStreamToWin() {

    }

    public void aJoinCompositionStreamToWin(Set<MultiKeyArrayOfKeys<EventBean>> newResults) {

    }

    public void qJoinCompositionStepUpdIndex(int stream, EventBean[] added, EventBean[] removed) {

    }

    public void aJoinCompositionStepUpdIndex() {

    }

    public void qIndexAddRemove(EventTable eventTable, EventBean[] newData, EventBean[] oldData) {

    }

    public void aIndexAddRemove() {

    }

    public void qIndexAdd(EventTable eventTable, EventBean[] addEvents) {

    }

    public void aIndexAdd() {

    }

    public void qIndexRemove(EventTable eventTable, EventBean[] removeEvents) {

    }

    public void aIndexRemove() {

    }

    public void qJoinCompositionQueryStrategy(boolean insert, int streamNum, EventBean[] events) {

    }

    public void aJoinCompositionQueryStrategy() {

    }

    public void qJoinExecProcess(UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> joinSet) {

    }

    public void aJoinExecProcess() {

    }

    public void qJoinCompositionWinToWin() {

    }

    public void aJoinCompositionWinToWin(Set<MultiKeyArrayOfKeys<EventBean>> newResults, Set<MultiKeyArrayOfKeys<EventBean>> oldResults) {

    }

    public void qOutputProcessWCondition(EventBean[] newData, EventBean[] oldData) {

    }

    public void aOutputProcessWCondition(boolean buffered) {

    }

    public void qOutputRateConditionUpdate(int newDataLength, int oldDataLength) {

    }

    public void aOutputRateConditionUpdate() {

    }

    public void qOutputRateConditionOutputNow() {

    }

    public void aOutputRateConditionOutputNow(boolean generate) {

    }

    public void qOutputProcessWConditionJoin(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents) {

    }

    public void aOutputProcessWConditionJoin(boolean buffered) {

    }

    public void qWhereClauseFilter(String text, EventBean[] newData, EventBean[] oldData) {

    }

    public void aWhereClauseFilter(EventBean[] filteredNewData, EventBean[] filteredOldData) {

    }

    public void qWhereClauseFilterEval(int num, EventBean event, boolean newData) {

    }

    public void aWhereClauseFilterEval(Boolean pass) {

    }

    public void qWhereClauseIR(EventBean[] filteredNewData, EventBean[] filteredOldData) {

    }

    public void aWhereClauseIR() {

    }

    public void qSplitStream(boolean all, EventBean theEvent, int numWhereClauses) {

    }

    public void aSplitStream(boolean all, boolean handled) {

    }

    public void qSplitStreamWhere(int index) {

    }

    public void aSplitStreamWhere(Boolean pass) {

    }

    public void qSplitStreamRoute(int index) {

    }

    public void aSplitStreamRoute() {

    }

    public void qSubselectAggregation() {
    }

    public void aSubselectAggregation() {
    }

    public void qTableAddEvent(EventBean theEvent) {

    }

    public void aTableAddEvent() {

    }

    public void qaTableUpdatedEventWKeyBefore(EventBean theEvent) {

    }

    public void qaTableUpdatedEventWKeyAfter(EventBean theEvent) {

    }

    public void qTableDeleteEvent(EventBean theEvent) {

    }

    public void aTableDeleteEvent() {

    }

    public void qAggregationGroupedApplyEnterLeave(boolean enter, int numAggregators, int numAccessStates, Object groupKey) {

    }

    public void aAggregationGroupedApplyEnterLeave(boolean enter) {

    }

    public void qAggNoAccessEnterLeave(boolean enter, int index, Object currentValue, String aggExpression) {

    }

    public void aAggNoAccessEnterLeave(boolean enter, int index, Object newValue) {

    }

    public void qAggAccessEnterLeave(boolean enter, int index, String aggExpr) {

    }

    public void aAggAccessEnterLeave(boolean enter, int index) {

    }

    public void qUpdateIStream(InternalEventRouterEntry[] entries) {

    }

    public void aUpdateIStream(EventBean finalEvent, boolean haveCloned) {

    }

    public void qUpdateIStreamApply(int index, InternalEventRouterEntry entry) {

    }

    public void aUpdateIStreamApply(EventBean updated, boolean applied) {

    }

    public void qUpdateIStreamApplyWhere() {

    }

    public void aUpdateIStreamApplyWhere(Boolean result) {

    }

    public void qUpdateIStreamApplyAssignments(InternalEventRouterEntry entry) {

    }

    public void aUpdateIStreamApplyAssignments(Object[] values) {

    }

    public void qUpdateIStreamApplyAssignmentItem(int index) {

    }

    public void aUpdateIStreamApplyAssignmentItem(Object value) {

    }

    public void qOutputRateConditionScheduledEval() {

    }

    public void aOutputRateConditionScheduledEval() {

    }

    public void qJoinExecFilter() {

    }

    public void aJoinExecFilter(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents) {

    }

    public void qJoinCompositionHistorical() {

    }

    public void aJoinCompositionHistorical(Set<MultiKeyArrayOfKeys<EventBean>> newResults, Set<MultiKeyArrayOfKeys<EventBean>> oldResults) {

    }

    public void qHistoricalScheduledEval() {

    }

    public void aHistoricalScheduledEval() {

    }
}
