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
package com.espertech.esper.metrics.instrumentation;

import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.InternalEventRouterEntry;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.core.orderby.OrderByElementEval;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.expression.funcs.*;
import com.espertech.esper.epl.expression.ops.*;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodImpl;
import com.espertech.esper.epl.expression.time.ExprTimestampNode;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategyType;
import com.espertech.esper.epl.named.NamedWindowConsumerView;
import com.espertech.esper.epl.named.NamedWindowDeltaData;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.epl.spec.OnTriggerType;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateItem;
import com.espertech.esper.filter.*;
import com.espertech.esper.filterspec.ExprNodeAdapterBase;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.*;
import com.espertech.esper.rowregex.RegexNFAState;
import com.espertech.esper.rowregex.RegexNFAStateEntry;
import com.espertech.esper.rowregex.RegexPartitionState;
import com.espertech.esper.schedule.ScheduleHandle;
import com.espertech.esper.type.BitWiseOpEnum;
import com.espertech.esper.view.View;

import java.lang.reflect.Method;
import java.util.*;

public class InstrumentationDefault implements Instrumentation {
    public void qStimulantEvent(EventBean eventBean, String engineURI) {

    }

    public void aStimulantEvent() {

    }

    public void qStimulantTime(long currentTime, String engineURI) {

    }

    public void aStimulantTime() {

    }

    public void qEvent(EventBean eventBean, String engineURI, boolean providedBySendEvent) {

    }

    public void aEvent() {

    }

    public void qEventCP(EventBean theEvent, EPStatementAgentInstanceHandle handle, long engineTime) {

    }

    public void aEventCP() {

    }

    public void qTime(long engineTime, String engineURI) {

    }

    public void aTime() {

    }

    public void qTimeCP(EPStatementAgentInstanceHandle handle, long engineTime) {

    }

    public void aTimeCP() {

    }

    public void qNamedWindowDispatch(String engineURI) {

    }

    public void aNamedWindowDispatch() {

    }

    public void qNamedWindowCPSingle(String engineURI, List<NamedWindowConsumerView> value, EventBean[] newData, EventBean[] oldData, EPStatementAgentInstanceHandle handle, long time) {

    }

    public void aNamedWindowCPSingle() {

    }

    public void qNamedWindowCPMulti(String engineURI, Map<NamedWindowConsumerView, NamedWindowDeltaData> deltaPerConsumer, EPStatementAgentInstanceHandle handle, long time) {

    }

    public void aNamedWindowCPMulti() {

    }

    public void qRegEx(EventBean newEvent, RegexPartitionState partitionState) {

    }

    public void aRegEx(RegexPartitionState partitionState, List<RegexNFAStateEntry> endStates, List<RegexNFAStateEntry> terminationStates) {

    }

    public void qRegExState(RegexNFAStateEntry currentState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void aRegExState(List<RegexNFAStateEntry> next, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void qRegExStateStart(RegexNFAState startState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void aRegExStateStart(List<RegexNFAStateEntry> nextStates, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void qRegExPartition(ExprNode[] partitionExpressionNodes) {

    }

    public void aRegExPartition(boolean exists, RegexPartitionState state) {

    }

    public void qRegIntervalValue(ExprNode exprNode) {

    }

    public void aRegIntervalValue(long result) {

    }

    public void qRegIntervalState(RegexNFAStateEntry endState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable, long engineTime) {

    }

    public void aRegIntervalState(boolean scheduled) {

    }

    public void qRegOut(EventBean[] outBeans) {

    }

    public void aRegOut() {

    }

    public void qRegMeasure(RegexNFAStateEntry endState, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {

    }

    public void aRegMeasure(EventBean outBean) {

    }

    public void qRegExScheduledEval() {

    }

    public void aRegExScheduledEval() {

    }

    public void qExprBool(ExprNode exprNode, EventBean[] eventsPerStream) {

    }

    public void aExprBool(Boolean result) {

    }

    public void qExprValue(ExprNode exprNode, EventBean[] eventsPerStream) {

    }

    public void aExprValue(Object result) {

    }

    public void qExprEquals(ExprNode exprNode) {

    }

    public void aExprEquals(Boolean result) {

    }

    public void qExprAnd(ExprNode exprNode) {

    }

    public void aExprAnd(Boolean result) {

    }

    public void qExprLike(ExprNode exprNode) {

    }

    public void aExprLike(Boolean result) {

    }

    public void qExprBitwise(ExprNode exprNode, BitWiseOpEnum bitWiseOpEnum) {

    }

    public void aExprBitwise(Object result) {

    }

    public void qExprMath(ExprMathNode exprMathNode, String op) {

    }

    public void aExprMath(Object result) {

    }

    public void qExprRegexp(ExprRegexpNode exprRegexpNode) {

    }

    public void aExprRegexp(Boolean result) {

    }

    public void qExprIdent(String fullUnresolvedName) {

    }

    public void aExprIdent(Object result) {

    }

    public void qExprTypeof() {

    }

    public void aExprTypeof(String typeName) {

    }

    public void qExprOr(ExprOrNode exprOrNode) {

    }

    public void aExprOr(Boolean result) {

    }

    public void qExprIn(ExprInNodeImpl exprInNode) {

    }

    public void aExprIn(Boolean result) {

    }

    public void qExprCoalesce(ExprCoalesceNode exprCoalesceNode) {

    }

    public void aExprCoalesce(Object value) {

    }

    public void qExprConcat(ExprConcatNode exprConcatNode) {

    }

    public void aExprConcat(String result) {

    }

    public void qaExprConst(Object result) {

    }

    public void qaExprTimestamp(ExprTimestampNode exprTimestampNode, long value) {

    }

    public void qExprBetween(ExprBetweenNodeImpl exprBetweenNode) {

    }

    public void aExprBetween(Boolean result) {

    }

    public void qExprCast(ExprCastNode exprCastNode) {

    }

    public void aExprCast(Object result) {

    }

    public void qExprCase(ExprCaseNode exprCaseNode) {

    }

    public void aExprCase(Object result) {

    }

    public void qExprArray(ExprArrayNode exprArrayNode) {

    }

    public void aExprArray(Object result) {

    }

    public void qExprEqualsAnyOrAll(ExprEqualsAllAnyNode exprEqualsAllAnyNode) {

    }

    public void aExprEqualsAnyOrAll(Boolean result) {

    }

    public void qExprMinMaxRow(ExprMinMaxRowNode exprMinMaxRowNode) {

    }

    public void aExprMinMaxRow(Object result) {

    }

    public void qExprNew(ExprNewStructNode exprNewNode) {

    }

    public void aExprNew(Map<String, Object> props) {

    }

    public void qExprNot(ExprNotNode exprNotNode) {

    }

    public void aExprNot(Boolean result) {

    }

    public void qExprPropExists(ExprPropertyExistsNode exprPropertyExistsNode) {

    }

    public void aExprPropExists(boolean exists) {

    }

    public void qExprRelOpAnyOrAll(ExprRelationalOpAllAnyNode exprRelationalOpAllAnyNode, String op) {

    }

    public void aExprRelOpAnyOrAll(Boolean result) {

    }

    public void qExprRelOp(ExprRelationalOpNodeImpl exprRelationalOpNode, String op) {

    }

    public void aExprRelOp(Boolean result) {

    }

    public void qExprStreamUnd(ExprStreamUnderlyingNodeImpl exprStreamUnderlyingNode) {

    }

    public void aExprStreamUnd(Object result) {

    }

    public void qExprStreamUndSelectClause(ExprStreamUnderlyingNode undNode) {

    }

    public void aExprStreamUndSelectClause(EventBean event) {

    }

    public void qExprIs(ExprEqualsNodeImpl exprNode) {

    }

    public void aExprIs(boolean result) {

    }

    public void qExprVariable(ExprVariableNode exprVariableNode) {

    }

    public void aExprVariable(Object value) {

    }

    public void qExprTimePeriod(ExprTimePeriodImpl exprTimePeriod) {

    }

    public void aExprTimePeriod(Object result) {

    }

    public void qExprInstanceof(ExprInstanceofNode exprInstanceofNode) {

    }

    public void aExprInstanceof(Boolean result) {

    }

    public void qExprContextProp(ExprContextPropertyNodeImpl exprContextPropertyNode) {

    }

    public void aExprContextProp(Object result) {

    }

    public void qExprPlugInSingleRow(Method method) {

    }

    public void aExprPlugInSingleRow(Object result) {

    }

    public void qaExprAggValue(ExprAggregateNodeBase exprAggregateNodeBase, Object value) {

    }

    public void qExprSubselect(ExprSubselectNode exprSubselectNode) {

    }

    public void aExprSubselect(Object result) {

    }

    public void qExprDot(ExprDotNode exprDotNode) {

    }

    public void aExprDot(Object result) {

    }

    public void qExprDotChain(EPType targetTypeInfo, Object target, ExprDotEval[] evalUnpacking) {

    }

    public void aExprDotChain() {

    }

    public void qExprDotChainElement(int num, ExprDotEval methodEval) {

    }

    public void aExprDotChainElement(EPType typeInfo, Object result) {

    }

    public void qaExprIStream(ExprIStreamNode exprIStreamNode, boolean newData) {

    }

    public void qExprDeclared(ExpressionDeclItem parent) {

    }

    public void aExprDeclared(Object value) {

    }

    public void qExprPrev(ExprPreviousNode exprPreviousNode, boolean newData) {

    }

    public void aExprPrev(Object result) {

    }

    public void qExprPrior(ExprPriorNode exprPriorNode) {

    }

    public void aExprPrior(Object result) {

    }

    public void qExprStreamUndMethod(ExprDotNode exprDotEvalStreamMethod) {

    }

    public void aExprStreamUndMethod(Object result) {

    }

    public void qExprStreamEventMethod(ExprDotNode exprDotNode) {

    }

    public void aExprStreamEventMethod(Object result) {

    }

    public void qScheduleAdd(long currentTime, long afterMSec, ScheduleHandle handle, long slot) {

    }

    public void aScheduleAdd() {

    }

    public void qScheduleRemove(ScheduleHandle handle, long slot) {

    }

    public void aScheduleRemove() {

    }

    public void qScheduleEval(long currentTime) {

    }

    public void aScheduleEval(Collection<ScheduleHandle> handles) {

    }

    public void qPatternAndEvaluateTrue(EvalAndNode evalAndNode, MatchedEventMap passUp) {

    }

    public void aPatternAndEvaluateTrue(boolean quitted) {

    }

    public void qPatternAndQuit(EvalAndNode evalAndNode) {

    }

    public void aPatternAndQuit() {

    }

    public void qPatternAndEvaluateFalse(EvalAndNode evalAndNode) {

    }

    public void aPatternAndEvaluateFalse() {

    }

    public void qPatternAndStart(EvalAndNode evalAndNode, MatchedEventMap beginState) {

    }

    public void aPatternAndStart() {

    }

    public void qPatternFollowedByEvaluateTrue(EvalFollowedByNode evalFollowedByNode, MatchedEventMap matchEvent, Integer index) {

    }

    public void aPatternFollowedByEvaluateTrue(boolean quitted) {

    }

    public void qPatternFollowedByQuit(EvalFollowedByNode evalFollowedByNode) {

    }

    public void aPatternFollowedByQuit() {

    }

    public void qPatternFollowedByEvalFalse(EvalFollowedByNode evalFollowedByNode) {

    }

    public void aPatternFollowedByEvalFalse() {

    }

    public void qPatternFollowedByStart(EvalFollowedByNode evalFollowedByNode, MatchedEventMap beginState) {

    }

    public void aPatternFollowedByStart() {

    }

    public void qPatternOrEvaluateTrue(EvalOrNode evalOrNode, MatchedEventMap matchEvent) {

    }

    public void aPatternOrEvaluateTrue(boolean quitted) {

    }

    public void qPatternOrEvalFalse(EvalOrNode evalOrNode) {

    }

    public void aPatternOrEvalFalse() {

    }

    public void qPatternOrQuit(EvalOrNode evalOrNode) {

    }

    public void aPatternOrQuit() {

    }

    public void aPatternOrStart() {

    }

    public void qPatternOrStart(EvalOrNode evalOrNode, MatchedEventMap beginState) {

    }

    public void qPatternFilterMatch(EvalFilterNode filterNode, EventBean theEvent) {

    }

    public void aPatternFilterMatch(boolean quitted) {

    }

    public void qPatternFilterStart(EvalFilterNode evalFilterNode, MatchedEventMap beginState) {

    }

    public void aPatternFilterStart() {

    }

    public void qPatternFilterQuit(EvalFilterNode evalFilterNode, MatchedEventMap beginState) {

    }

    public void aPatternFilterQuit() {

    }

    public void qPatternRootEvaluateTrue(MatchedEventMap matchEvent) {

    }

    public void aPatternRootEvaluateTrue(boolean quitted) {

    }

    public void qPatternRootStart(MatchedEventMap root) {

    }

    public void aPatternRootStart() {

    }

    public void qPatternRootQuit() {

    }

    public void aPatternRootQuit() {

    }

    public void qPatternRootEvalFalse() {

    }

    public void aPatternRootEvalFalse() {

    }

    public void qPatternEveryEvaluateTrue(EvalEveryNode evalEveryNode, MatchedEventMap matchEvent) {

    }

    public void aPatternEveryEvaluateTrue() {

    }

    public void qPatternEveryStart(EvalEveryNode evalEveryNode, MatchedEventMap beginState) {

    }

    public void aPatternEveryStart() {

    }

    public void qPatternEveryEvalFalse(EvalEveryNode evalEveryNode) {

    }

    public void aPatternEveryEvalFalse() {

    }

    public void qPatternEveryQuit(EvalEveryNode evalEveryNode) {

    }

    public void aPatternEveryQuit() {

    }

    public void qPatternEveryDistinctEvaluateTrue(EvalEveryDistinctNode everyDistinctNode, MatchedEventMap matchEvent) {

    }

    public void aPatternEveryDistinctEvaluateTrue(Set<Object> keysFromNodeNoExpire, LinkedHashMap<Object, Long> keysFromNodeExpire, Object matchEventKey, boolean haveSeenThis) {

    }

    public void qPatternEveryDistinctQuit(EvalEveryDistinctNode everyNode) {

    }

    public void aPatternEveryDistinctQuit() {

    }

    public void qPatternEveryDistinctEvalFalse(EvalEveryDistinctNode everyNode) {

    }

    public void aPatternEveryDistinctEvalFalse() {

    }

    public void qPatternEveryDistinctStart(EvalEveryDistinctNode everyNode, MatchedEventMap beginState) {

    }

    public void aPatternEveryDistinctStart() {

    }

    public void qPatternGuardEvaluateTrue(EvalGuardNode evalGuardNode, MatchedEventMap matchEvent) {

    }

    public void aPatternGuardEvaluateTrue(boolean quitted) {

    }

    public void qPatternGuardStart(EvalGuardNode evalGuardNode, MatchedEventMap beginState) {

    }

    public void aPatternGuardStart() {

    }

    public void qPatternGuardQuit(EvalGuardNode evalGuardNode) {

    }

    public void aPatternGuardQuit() {

    }

    public void qPatternGuardGuardQuit(EvalGuardNode evalGuardNode) {

    }

    public void aPatternGuardGuardQuit() {

    }

    public void qPatternGuardScheduledEval() {

    }

    public void aPatternGuardScheduledEval() {

    }

    public void qPatternMatchUntilEvaluateTrue(EvalMatchUntilNode evalMatchUntilNode, MatchedEventMap matchEvent, boolean matchFromUntil) {

    }

    public void aPatternMatchUntilEvaluateTrue(boolean quitted) {

    }

    public void qPatternMatchUntilStart(EvalMatchUntilNode evalMatchUntilNode, MatchedEventMap beginState) {

    }

    public void aPatternMatchUntilStart() {

    }

    public void qPatternMatchUntilEvalFalse(EvalMatchUntilNode evalMatchUntilNode, boolean matchFromUntil) {

    }

    public void aPatternMatchUntilEvalFalse() {

    }

    public void qPatternMatchUntilQuit(EvalMatchUntilNode evalMatchUntilNode) {

    }

    public void aPatternMatchUntilQuit() {

    }

    public void qPatternNotEvaluateTrue(EvalNotNode evalNotNode, MatchedEventMap matchEvent) {

    }

    public void aPatternNotEvaluateTrue(boolean quitted) {

    }

    public void aPatternNotQuit() {

    }

    public void qPatternNotQuit(EvalNotNode evalNotNode) {

    }

    public void qPatternNotStart(EvalNotNode evalNotNode, MatchedEventMap beginState) {

    }

    public void aPatternNotStart() {

    }

    public void qPatternNotEvalFalse(EvalNotNode evalNotNode) {

    }

    public void aPatternNotEvalFalse() {

    }

    public void qPatternObserverEvaluateTrue(EvalObserverNode evalObserverNode, MatchedEventMap matchEvent) {

    }

    public void aPatternObserverEvaluateTrue() {

    }

    public void qPatternObserverStart(EvalObserverNode evalObserverNode, MatchedEventMap beginState) {

    }

    public void aPatternObserverStart() {

    }

    public void qPatternObserverQuit(EvalObserverNode evalObserverNode) {

    }

    public void aPatternObserverQuit() {

    }

    public void qPatternObserverScheduledEval() {

    }

    public void aPatternObserverScheduledEval() {

    }

    public void qContextPartitionAllocate(AgentInstanceContext agentInstanceContext) {

    }

    public void aContextPartitionAllocate() {

    }

    public void qContextPartitionDestroy(AgentInstanceContext agentInstanceContext) {

    }

    public void aContextPartitionDestroy() {

    }

    public void qContextScheduledEval(ContextDescriptor contextDescriptor) {

    }

    public void aContextScheduledEval() {

    }

    public void qOutputProcessNonBuffered(EventBean[] newData, EventBean[] oldData) {

    }

    public void aOutputProcessNonBuffered() {

    }

    public void qOutputProcessNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {

    }

    public void aOutputProcessNonBufferedJoin() {

    }

    public void qOutputProcessWCondition(EventBean[] newData, EventBean[] oldData) {

    }

    public void aOutputProcessWCondition(boolean buffered) {

    }

    public void qOutputProcessWConditionJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {

    }

    public void aOutputProcessWConditionJoin(boolean buffered) {

    }

    public void qOutputRateConditionUpdate(int newDataLength, int oldDataLength) {

    }

    public void aOutputRateConditionUpdate() {

    }

    public void qOutputRateConditionOutputNow() {

    }

    public void aOutputRateConditionOutputNow(boolean generate) {

    }

    public void qOutputRateConditionScheduledEval() {

    }

    public void aOutputRateConditionScheduledEval() {

    }

    public void qResultSetProcessSimple() {

    }

    public void aResultSetProcessSimple(EventBean[] selectNewEvents, EventBean[] selectOldEvents) {

    }

    public void qResultSetProcessUngroupedFullyAgg() {

    }

    public void aResultSetProcessUngroupedFullyAgg(EventBean[] selectNewEvents, EventBean[] selectOldEvents) {

    }

    public void qResultSetProcessUngroupedNonfullyAgg() {

    }

    public void aResultSetProcessUngroupedNonfullyAgg(EventBean[] selectNewEvents, EventBean[] selectOldEvents) {

    }

    public void qResultSetProcessGroupedRowPerGroup() {

    }

    public void aResultSetProcessGroupedRowPerGroup(EventBean[] selectNewEvents, EventBean[] selectOldEvents) {

    }

    public void qResultSetProcessGroupedRowPerEvent() {

    }

    public void aResultSetProcessGroupedRowPerEvent(EventBean[] selectNewEvents, EventBean[] selectOldEvents) {

    }

    public void qResultSetProcessComputeGroupKeys(boolean enter, ExprNode[] groupKeyNodeExpressions, EventBean[] eventsPerStream) {

    }

    public void aResultSetProcessComputeGroupKeys(boolean enter, Object groupKeysPerEvent) {

    }

    public void qAggregationUngroupedApplyEnterLeave(boolean enter, int numAggregators, int numAccessStates) {

    }

    public void aAggregationUngroupedApplyEnterLeave(boolean enter) {

    }

    public void qAggregationGroupedApplyEnterLeave(boolean enter, int numAggregators, int numAccessStates, Object groupKey) {

    }

    public void aAggregationGroupedApplyEnterLeave(boolean enter) {

    }

    public void qAggNoAccessEnterLeave(boolean enter, int index, AggregationMethod aggregationMethod, ExprNode aggExpr) {

    }

    public void qAggAccessEnterLeave(boolean enter, int index, AggregationState state, ExprNode aggExpr) {

    }

    public void aAggNoAccessEnterLeave(boolean enter, int index, AggregationMethod aggregationMethod) {

    }

    public void aAggAccessEnterLeave(boolean enter, int index, AggregationState state) {

    }

    public void qSelectClause(EventBean[] eventsPerStream, boolean newData, boolean synthesize, ExprEvaluatorContext exprEvaluatorContext) {

    }

    public void aSelectClause(boolean newData, EventBean event, Object[] subscriberParameters) {

    }

    public void qViewProcessIRStream(View view, String viewName, EventBean[] newData, EventBean[] oldData) {

    }

    public void aViewProcessIRStream() {

    }

    public void qViewScheduledEval(View view, String viewName) {

    }

    public void aViewScheduledEval() {

    }

    public void qViewIndicate(View view, String viewName, EventBean[] newData, EventBean[] oldData) {

    }

    public void aViewIndicate() {

    }

    public void qSubselectAggregation(ExprNode optionalFilterExprNode) {

    }

    public void aSubselectAggregation() {

    }

    public void qFilterActivationSubselect(String eventTypeName, ExprSubselectNode subselectNode) {

    }

    public void aFilterActivationSubselect() {

    }

    public void qFilterActivationStream(String eventTypeName, int streamNumber) {

    }

    public void aFilterActivationStream() {

    }

    public void qFilterActivationNamedWindowInsert(String namedWindowName) {

    }

    public void aFilterActivationNamedWindowInsert() {

    }

    public void qFilterActivationOnTrigger(String eventTypeName) {

    }

    public void aFilterActivationOnTrigger() {

    }

    public void qRouteBetweenStmt(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront) {

    }

    public void aRouteBetweenStmt() {

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

    public void qIndexSubordLookup(SubordTableLookupStrategy subordTableLookupStrategy, EventTable optionalEventIndex, int[] keyStreamNums) {

    }

    public void aIndexSubordLookup(Collection<EventBean> events, Object keys) {

    }

    public void qIndexJoinLookup(JoinExecTableLookupStrategy strategy, EventTable index) {

    }

    public void aIndexJoinLookup(Set<EventBean> result, Object keys) {

    }

    public void qFilter(EventBean theEvent) {

    }

    public void aFilter(Collection<FilterHandle> matches) {

    }

    public void qFilterHandleSetIndexes(List<FilterParamIndexBase> indizes) {

    }

    public void aFilterHandleSetIndexes() {

    }

    public void qaFilterHandleSetCallbacks(Set<FilterHandle> callbackSet) {

    }

    public void qFilterReverseIndex(FilterParamIndexLookupableBase filterParamIndex, Object propertyValue) {

    }

    public void aFilterReverseIndex(Boolean match) {

    }

    public void qFilterBoolean(FilterParamIndexBooleanExpr filterParamIndexBooleanExpr) {

    }

    public void aFilterBoolean() {

    }

    public void qFilterBooleanExpr(int num, Map.Entry<ExprNodeAdapterBase, EventEvaluator> evals) {

    }

    public void aFilterBooleanExpr(boolean result) {

    }

    public void qFilterAdd(FilterValueSet filterValueSet, FilterHandle filterCallback) {

    }

    public void aFilterAdd() {

    }

    public void qFilterRemove(FilterHandle filterCallback, EventTypeIndexBuilderValueIndexesPair pair) {

    }

    public void aFilterRemove() {

    }

    public void qWhereClauseFilter(ExprNode exprNode, EventBean[] newData, EventBean[] oldData) {

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

    public void qHavingClause(EventBean[] eventsPerStream) {

    }

    public void aHavingClause(Boolean pass) {

    }

    public void qOrderBy(EventBean[] evalEventsPerStream, OrderByElementEval[] orderBy) {

    }

    public void aOrderBy(Object values) {

    }

    public void qJoinDispatch(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream) {

    }

    public void aJoinDispatch() {

    }

    public void qJoinExexStrategy() {

    }

    public void aJoinExecStrategy(UniformPair<Set<MultiKey<EventBean>>> joinSet) {

    }

    public void qJoinExecFilter() {

    }

    public void aJoinExecFilter(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {

    }

    public void qJoinExecProcess(UniformPair<Set<MultiKey<EventBean>>> joinSet) {

    }

    public void aJoinExecProcess() {

    }

    public void qJoinCompositionStreamToWin() {

    }

    public void aJoinCompositionStreamToWin(Set<MultiKey<EventBean>> newResults) {

    }

    public void qJoinCompositionWinToWin() {

    }

    public void aJoinCompositionWinToWin(Set<MultiKey<EventBean>> newResults, Set<MultiKey<EventBean>> oldResults) {

    }

    public void qJoinCompositionHistorical() {

    }

    public void aJoinCompositionHistorical(Set<MultiKey<EventBean>> newResults, Set<MultiKey<EventBean>> oldResults) {

    }

    public void qJoinCompositionStepUpdIndex(int stream, EventBean[] added, EventBean[] removed) {

    }

    public void aJoinCompositionStepUpdIndex() {

    }

    public void qJoinCompositionQueryStrategy(boolean insert, int streamNum, EventBean[] events) {

    }

    public void aJoinCompositionQueryStrategy() {

    }

    public void qInfraTriggeredLookup(SubordWMatchExprLookupStrategyType lookupStrategy) {

    }

    public void aInfraTriggeredLookup(EventBean[] result) {

    }

    public void qInfraOnAction(OnTriggerType triggerType, EventBean[] triggerEvents, EventBean[] matchingEvents) {

    }

    public void aInfraOnAction() {

    }

    public void qInfraUpdate(EventBean beforeUpdate, EventBean[] eventsPerStream, int length, boolean copy) {

    }

    public void aInfraUpdate(EventBean afterUpdate) {

    }

    public void qInfraUpdateRHSExpr(int index, EventBeanUpdateItem updateItem) {

    }

    public void aInfraUpdateRHSExpr(Object result) {

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

    public void qEngineManagementStmtCompileStart(String engineURI, int statementId, String statementName, String epl, long engineTime) {

    }

    public void aEngineManagementStmtCompileStart(boolean success, String message) {

    }

    public void qaEngineManagementStmtStarted(String engineURI, int statementId, String statementName, String epl, long engineTime) {

    }

    public void qEngineManagementStmtStop(EPStatementState targetState, String engineURI, int statementId, String statementName, String epl, long engineTime) {

    }

    public void aEngineManagementStmtStop() {

    }

    public void qaStatementResultExecute(UniformPair<EventBean[]> events, int statementId, String statementName, int agentInstanceId, long threadId) {

    }

    public void qSplitStream(boolean all, EventBean theEvent, ExprEvaluator[] whereClauses) {

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

    public void qHistoricalScheduledEval() {

    }

    public void aHistoricalScheduledEval() {

    }

    public void qAggregationGroupedRollupEvalParam(boolean enter, int length) {

    }

    public void aAggregationGroupedRollupEvalParam(Object result) {

    }

    public void qExprTableSubproperty(ExprNode exprNode, String tableName, String subpropName) {

    }

    public void aExprTableSubproperty(Object result) {

    }

    public void qExprTableTop(ExprNode exprNode, String tableName) {

    }

    public void aExprTableTop(Object result) {

    }

    public void qExprTableSubpropAccessor(ExprNode exprNode, String tableName, String subpropName, ExprAggregateNode aggregationExpression) {

    }

    public void aExprTableSubpropAccessor(Object result) {

    }

    public void qTableAddEvent(EventBean theEvent) {

    }

    public void aTableAddEvent() {

    }

    public void qTableDeleteEvent(EventBean theEvent) {

    }

    public void aTableDeleteEvent() {

    }

    public void qaTableUpdatedEvent(EventBean theEvent) {

    }

    public void qaTableUpdatedEventWKeyBefore(EventBean theEvent) {

    }

    public void qaTableUpdatedEventWKeyAfter(EventBean theEvent) {

    }

    public void aResultSetProcessGroupedRowPerGroup(UniformPair<EventBean[]> pair) {

    }

    public void aResultSetProcessGroupedRowPerEvent(UniformPair<EventBean[]> pair) {

    }
}
