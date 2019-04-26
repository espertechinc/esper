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

public interface InstrumentationCommon {
    String RUNTIME_PACKAGE_NAME = "com.espertech.esper.runtime.internal.metrics.instrumentation";
    String RUNTIME_DEFAULT_CLASS = RUNTIME_PACKAGE_NAME + ".InstrumentationDefault";
    String RUNTIME_HELPER_CLASS = RUNTIME_PACKAGE_NAME + ".InstrumentationHelper";

    boolean activated();

    void qNamedWindowDispatch(String runtimeURI);

    void aNamedWindowDispatch();

    void qNamedWindowCPSingle(String runtimeURI, int numConsumers, EventBean[] newData, EventBean[] oldData, EPStatementAgentInstanceHandle handle, long time);

    void aNamedWindowCPSingle();

    void qNamedWindowCPMulti(String runtimeURI, Map<NamedWindowConsumerView, NamedWindowDeltaData> deltaPerConsumer, EPStatementAgentInstanceHandle handle, long time);

    void aNamedWindowCPMulti();

    void qRegEx(EventBean newEvent, RowRecogPartitionState partitionState);

    void aRegEx(RowRecogPartitionState partitionState, List<RowRecogNFAStateEntry> endStates, List<RowRecogNFAStateEntry> terminationStates);

    void qRegExState(RowRecogNFAStateEntry currentState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable);

    void aRegExState(List<RowRecogNFAStateEntry> next, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable);

    void qRegExStateStart(RowRecogNFAState startState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable);

    void aRegExStateStart(List<RowRecogNFAStateEntry> nextStates, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable);

    void qRegExPartition(EventBean theEvent);

    void aRegExPartition(boolean exists, Object partitionKey, RowRecogPartitionState state);

    void qRegIntervalValue();

    void aRegIntervalValue(long result);

    void qRegIntervalState(RowRecogNFAStateEntry endState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable, long runtimeTime);

    void aRegIntervalState(boolean scheduled);

    void qRegOut(EventBean[] outBeans);

    void aRegOut();

    void qRegMeasure(RowRecogNFAStateEntry endState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable);

    void aRegMeasure(EventBean outBean);

    void qRegExScheduledEval();

    void aRegExScheduledEval();

    void qRegFilter(String text, EventBean[] eventsPerStream);

    void aRegFilter(Boolean result);

    void qFilterActivationStream(String eventTypeName, int streamNumber, AgentInstanceContext agentInstanceContext, boolean subselect, int subselectNumber);

    void aFilterActivationStream(AgentInstanceContext agentInstanceContext, boolean subselect, int subselectNumber);

    void qIndexSubordLookup(SubordTableLookupStrategy subordTableLookupStrategy, EventTable optionalEventIndex, int[] keyStreamNums);

    void aIndexSubordLookup(Collection<EventBean> events, Object keys);

    void qViewProcessIRStream(ViewFactory viewFactory, EventBean[] newData, EventBean[] oldData);

    void aViewProcessIRStream();

    void qViewIndicate(ViewFactory viewFactory, EventBean[] newData, EventBean[] oldData);

    void aViewIndicate();

    void qViewScheduledEval(ViewFactory viewFactory);

    void aViewScheduledEval();

    void qPatternFilterMatch(EvalFilterFactoryNode filterNode, EventBean theEvent);

    void aPatternFilterMatch(boolean quitted);

    void qPatternNotEvaluateTrue(EvalNotFactoryNode evalNotNode, MatchedEventMapMinimal matchEvent);

    void aPatternNotEvaluateTrue(boolean quitted);

    void qPatternObserverQuit(EvalObserverFactoryNode evalObserverNode);

    void aPatternObserverQuit();

    void qPatternAndEvaluateFalse(EvalAndFactoryNode evalAndNode);

    void aPatternAndEvaluateFalse();

    void qPatternRootEvalFalse();

    void aPatternRootEvalFalse();

    void qPatternObserverScheduledEval();

    void aPatternObserverScheduledEval();

    void qPatternObserverEvaluateTrue(EvalObserverFactoryNode evalObserverNode, MatchedEventMap matchEvent);

    void aPatternObserverEvaluateTrue();

    void qPatternFollowedByEvaluateTrue(EvalFollowedByFactoryNode evalFollowedByNode, MatchedEventMap matchEvent, Integer index);

    void aPatternFollowedByEvaluateTrue(boolean quitted);

    void qPatternGuardStart(EvalGuardFactoryNode evalGuardNode, MatchedEventMap beginState);

    void aPatternGuardStart();

    void qPatternAndStart(EvalAndFactoryNode evalAndNode, MatchedEventMap beginState);

    void aPatternAndStart();

    void qPatternFilterStart(EvalFilterFactoryNode evalFilterNode, MatchedEventMap beginState);

    void aPatternFilterStart();

    void qPatternNotStart(EvalNotFactoryNode evalNotNode, MatchedEventMap beginState);

    void aPatternNotStart();

    void qPatternAndEvaluateTrue(EvalAndFactoryNode evalAndNode, MatchedEventMap passUp);

    void aPatternAndEvaluateTrue(boolean quitted);

    void qPatternGuardScheduledEval();

    void aPatternGuardScheduledEval();

    void qPatternGuardGuardQuit(EvalGuardFactoryNode evalGuardNode);

    void aPatternGuardGuardQuit();

    void qPatternAndQuit(EvalAndFactoryNode evalAndNode);

    void aPatternAndQuit();

    void qPatternFilterQuit(EvalFilterFactoryNode evalFilterNode, MatchedEventMap beginState);

    void aPatternFilterQuit();

    void qPatternNotQuit(EvalNotFactoryNode evalNotNode);

    void aPatternNotQuit();

    void qPatternNotEvalFalse(EvalNotFactoryNode evalNotNode);

    void aPatternNotEvalFalse();

    void qPatternRootEvaluateTrue(MatchedEventMap matchEvent);

    void aPatternRootEvaluateTrue(boolean quitted);

    void qPatternObserverStart(EvalObserverFactoryNode evalObserverNode, MatchedEventMap beginState);

    void aPatternObserverStart();

    void qPatternMatchUntilEvaluateTrue(EvalMatchUntilFactoryNode evalMatchUntilNode, MatchedEventMap matchEvent, boolean matchFromUntil);

    void aPatternMatchUntilEvaluateTrue(boolean quitted);

    void qPatternMatchUntilStart(EvalMatchUntilFactoryNode evalMatchUntilNode, MatchedEventMap beginState);

    void aPatternMatchUntilStart();

    void qPatternMatchUntilQuit(EvalMatchUntilFactoryNode evalMatchUntilNode);

    void aPatternMatchUntilQuit();

    void qPatternMatchUntilEvalFalse(EvalMatchUntilFactoryNode evalMatchUntilNode, boolean matchFromUntil);

    void aPatternMatchUntilEvalFalse();

    void qPatternGuardEvaluateTrue(EvalGuardFactoryNode evalGuardNode, MatchedEventMap matchEvent);

    void aPatternGuardEvaluateTrue(boolean quitted);

    void qPatternGuardQuit(EvalGuardFactoryNode evalGuardNode);

    void aPatternGuardQuit();

    void qPatternEveryDistinctEvaluateTrue(EvalEveryDistinctFactoryNode everyDistinctNode, MatchedEventMap matchEvent);

    void aPatternEveryDistinctEvaluateTrue(Set<Object> keysFromNodeNoExpire, LinkedHashMap<Object, Long> keysFromNodeExpire, Object matchEventKey, boolean haveSeenThis);

    void qPatternEveryDistinctStart(EvalEveryDistinctFactoryNode everyNode, MatchedEventMap beginState);

    void aPatternEveryDistinctStart();

    void qPatternEveryDistinctQuit(EvalEveryDistinctFactoryNode everyNode);

    void aPatternEveryDistinctQuit();

    void qPatternFollowedByEvalFalse(EvalFollowedByFactoryNode evalFollowedByNode);

    void aPatternFollowedByEvalFalse();

    void qPatternEveryDistinctEvalFalse(EvalEveryDistinctFactoryNode everyNode);

    void aPatternEveryDistinctEvalFalse();

    void qPatternEveryEvaluateTrue(EvalEveryFactoryNode evalEveryNode, MatchedEventMap matchEvent);

    void aPatternEveryEvaluateTrue();

    void qPatternEveryStart(EvalEveryFactoryNode evalEveryNode, MatchedEventMap beginState);

    void aPatternEveryStart();

    void qPatternEveryQuit(EvalEveryFactoryNode evalEveryNode);

    void aPatternEveryQuit();

    void qPatternEveryEvalFalse(EvalEveryFactoryNode evalEveryNode);

    void aPatternEveryEvalFalse();

    void qPatternOrEvaluateTrue(EvalOrFactoryNode evalOrNode, MatchedEventMap matchEvent);

    void aPatternOrEvaluateTrue(boolean quitted);

    void qPatternOrStart(EvalOrFactoryNode evalOrNode, MatchedEventMap beginState);

    void aPatternOrStart();

    void qPatternOrQuit(EvalOrFactoryNode evalOrNode);

    void aPatternOrQuit();

    void qPatternOrEvalFalse(EvalOrFactoryNode evalOrNode);

    void aPatternOrEvalFalse();

    void qPatternFollowedByStart(EvalFollowedByFactoryNode evalFollowedByNode, MatchedEventMap beginState);

    void aPatternFollowedByStart();

    void qPatternFollowedByQuit(EvalFollowedByFactoryNode evalFollowedByNode);

    void aPatternFollowedByQuit();

    void qPatternGuardEvalFalse(EvalGuardFactoryNode evalGuardNode);

    void aPatternGuardEvalFalse();

    void qContextScheduledEval(ContextRuntimeDescriptor contextDescriptor);

    void aContextScheduledEval();

    void qContextPartitionAllocate(AgentInstanceContext agentInstanceContext);

    void aContextPartitionAllocate();

    void qContextPartitionDestroy(AgentInstanceContext agentInstanceContext);

    void aContextPartitionDestroy();

    void qPatternRootStart(MatchedEventMap root);

    void aPatternRootStart();

    void qPatternRootQuit();

    void aPatternRootQuit();

    void qInfraOnAction(OnTriggerType triggerType, EventBean[] triggerEvents, EventBean[] matchingEvents);

    void aInfraOnAction();

    void qTableUpdatedEvent(EventBean theEvent);

    void aTableUpdatedEvent();

    void qInfraMergeWhenThens(boolean matched, EventBean triggerEvent, int numWhenThens);

    void aInfraMergeWhenThens(boolean matched);

    void qInfraMergeWhenThenItem(boolean matched, int count);

    void aInfraMergeWhenThenItem(boolean matched, boolean actionsApplied);

    void qInfraMergeWhenThenActions(int numActions);

    void aInfraMergeWhenThenActions();

    void qInfraMergeWhenThenActionItem(int count, String actionName);

    void aInfraMergeWhenThenActionItem(boolean applies);

    void qInfraTriggeredLookup(String lookupStrategy);

    void aInfraTriggeredLookup(EventBean[] result);

    void qIndexJoinLookup(JoinExecTableLookupStrategy strategy, EventTable index);

    void aIndexJoinLookup(Set<EventBean> result, Object keys);

    void qJoinDispatch(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream);

    void aJoinDispatch();

    void qJoinExecStrategy();

    void aJoinExecStrategy(UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> joinSet);

    void qJoinCompositionStreamToWin();

    void aJoinCompositionStreamToWin(Set<MultiKeyArrayOfKeys<EventBean>> newResults);

    void qJoinCompositionStepUpdIndex(int stream, EventBean[] added, EventBean[] removed);

    void aJoinCompositionStepUpdIndex();

    void qIndexAddRemove(EventTable eventTable, EventBean[] newData, EventBean[] oldData);

    void aIndexAddRemove();

    void qIndexAdd(EventTable eventTable, EventBean[] addEvents);

    void aIndexAdd();

    void qIndexRemove(EventTable eventTable, EventBean[] removeEvents);

    void aIndexRemove();

    void qJoinCompositionQueryStrategy(boolean insert, int streamNum, EventBean[] events);

    void aJoinCompositionQueryStrategy();

    void qJoinExecProcess(UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> joinSet);

    void aJoinExecProcess();

    void qJoinCompositionWinToWin();

    void aJoinCompositionWinToWin(Set<MultiKeyArrayOfKeys<EventBean>> newResults, Set<MultiKeyArrayOfKeys<EventBean>> oldResults);

    void qOutputProcessWCondition(EventBean[] newData, EventBean[] oldData);

    void aOutputProcessWCondition(boolean buffered);

    void qOutputRateConditionUpdate(int newDataLength, int oldDataLength);

    void aOutputRateConditionUpdate();

    void qOutputRateConditionOutputNow();

    void aOutputRateConditionOutputNow(boolean generate);

    void qOutputProcessWConditionJoin(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents);

    void aOutputProcessWConditionJoin(boolean buffered);

    void qWhereClauseFilter(String text, EventBean[] newData, EventBean[] oldData);

    void aWhereClauseFilter(EventBean[] filteredNewData, EventBean[] filteredOldData);

    void qWhereClauseFilterEval(int num, EventBean event, boolean newData);

    void aWhereClauseFilterEval(Boolean pass);

    void qWhereClauseIR(EventBean[] filteredNewData, EventBean[] filteredOldData);

    void aWhereClauseIR();

    void qSplitStream(boolean all, EventBean theEvent, int numWhereClauses);

    void aSplitStream(boolean all, boolean handled);

    void qSplitStreamWhere(int index);

    void aSplitStreamWhere(Boolean pass);

    void qSplitStreamRoute(int index);

    void aSplitStreamRoute();

    void qSubselectAggregation();

    void aSubselectAggregation();

    void qTableAddEvent(EventBean theEvent);

    void aTableAddEvent();

    void qaTableUpdatedEventWKeyBefore(EventBean theEvent);

    void qaTableUpdatedEventWKeyAfter(EventBean theEvent);

    void qTableDeleteEvent(EventBean theEvent);

    void aTableDeleteEvent();

    void qAggregationGroupedApplyEnterLeave(boolean enter, int numAggregators, int numAccessStates, Object groupKey);

    void aAggregationGroupedApplyEnterLeave(boolean enter);

    void qAggNoAccessEnterLeave(boolean enter, int index, Object currentValue, String aggExpression);

    void aAggNoAccessEnterLeave(boolean enter, int index, Object newValue);

    void qAggAccessEnterLeave(boolean enter, int index, String aggExpr);

    void aAggAccessEnterLeave(boolean enter, int index);

    void qUpdateIStream(InternalEventRouterEntry[] entries);

    void aUpdateIStream(EventBean finalEvent, boolean haveCloned);

    void qUpdateIStreamApply(int index, InternalEventRouterEntry entry);

    void aUpdateIStreamApply(EventBean updated, boolean applied);

    void qUpdateIStreamApplyWhere();

    void aUpdateIStreamApplyWhere(Boolean result);

    void qUpdateIStreamApplyAssignments(InternalEventRouterEntry entry);

    void aUpdateIStreamApplyAssignments(Object[] values);

    void qUpdateIStreamApplyAssignmentItem(int index);

    void aUpdateIStreamApplyAssignmentItem(Object value);

    void qOutputRateConditionScheduledEval();

    void aOutputRateConditionScheduledEval();

    void qHistoricalScheduledEval();

    void aHistoricalScheduledEval();

    void qJoinExecFilter();

    void aJoinExecFilter(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents);

    void qJoinCompositionHistorical();

    void aJoinCompositionHistorical(Set<MultiKeyArrayOfKeys<EventBean>> newResults, Set<MultiKeyArrayOfKeys<EventBean>> oldResults);
}

