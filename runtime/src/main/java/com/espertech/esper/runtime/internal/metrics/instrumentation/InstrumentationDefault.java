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
package com.espertech.esper.runtime.internal.metrics.instrumentation;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.MultiKey;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerView;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowDeltaData;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAState;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEntry;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogPartitionState;
import com.espertech.esper.common.internal.filterspec.ExprNodeAdapterBase;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommonDefault;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.type.BitWiseOpEnum;
import com.espertech.esper.runtime.internal.filtersvcimpl.EventEvaluator;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterParamIndexBase;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterParamIndexBooleanExpr;

import java.util.*;

public class InstrumentationDefault extends InstrumentationCommonDefault implements Instrumentation {
    public final static InstrumentationDefault INSTANCE = new InstrumentationDefault();

    private InstrumentationDefault() {
    }

    public boolean activated() {
        return false;
    }

    public void qFilterActivationStream(String eventTypeName, int streamNumber, AgentInstanceContext agentInstanceContext, boolean subselect, int subselectNumber) {
    }

    public void aFilterActivationStream(AgentInstanceContext agentInstanceContext, boolean subselect, int subselectNumber) {
    }

    public void qStimulantEvent(EventBean eventBean, String runtimeURI) {
    }

    public void aStimulantEvent() {
    }

    public void qStimulantTime(long currentTime, long target, long ultimateTarget, boolean span, Long resolution, String runtimeURI) {
    }

    public void aStimulantTime() {
    }

    public void qEvent(EventBean eventBean, String runtimeURI, boolean providedBySendEvent) {
    }

    public void aEvent() {
    }

    public void qEventCP(EventBean theEvent, EPStatementAgentInstanceHandle handle, long runtimeTime) {
    }

    public void aEventCP() {
    }

    public void qTime(long runtimeTime, String runtimeURI) {
    }

    public void aTime() {
    }

    public void qTimeCP(EPStatementAgentInstanceHandle handle, long runtimeTime) {
    }

    public void aTimeCP() {
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

    public void qRegFilter(EventBean[] eventsPerStream) {
    }

    public void aRegFilter(Boolean result) {
    }

    public void qExprEquals(String text) {
    }

    public void aExprEquals(Boolean result) {
    }

    public void qOutputProcessNonBuffered(EventBean[] newData, EventBean[] oldData) {
    }

    public void aOutputProcessNonBuffered() {
    }

    public void qSelectClause(EventBean[] eventsPerStream, boolean newData, boolean synthesize, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public void aSelectClause(boolean newData, EventBean event, Object[] subscriberParameters) {
    }

    public void qExprBitwise(String text, BitWiseOpEnum bitWiseOpEnum) {
    }

    public void aExprBitwise(Object result) {
    }

    public void qExprIdent(String fullUnresolvedName) {
    }

    public void aExprIdent(Object result) {
    }

    public void qExprMath(String text, String op) {
    }

    public void aExprMath(Object result) {
    }

    public void qExprRegexp(String text) {
    }

    public void aExprRegexp(Boolean result) {
    }

    public void qExprTypeof(String text) {
    }

    public void aExprTypeof(String typeName) {
    }

    public void qExprOr(String text) {
    }

    public void aExprOr(Boolean result) {
    }

    public void qExprIn(String text) {
    }

    public void aExprIn(Boolean result) {
    }

    public void qExprConcat(String text) {
    }

    public void aExprConcat(String result) {
    }

    public void qExprCoalesce(String text) {
    }

    public void aExprCoalesce(Object value) {
    }

    public void qExprBetween(String text) {
    }

    public void aExprBetween(Boolean result) {
    }

    public void qExprCast(String text) {
    }

    public void aExprCast(Object result) {

    }

    public void qExprCase(String text) {

    }

    public void aExprCase(Object result) {

    }

    public void qExprArray(String text) {

    }

    public void aExprArray(Object result) {

    }

    public void qExprEqualsAnyOrAll(String text) {

    }

    public void aExprEqualsAnyOrAll(Boolean result) {

    }

    public void qExprPropExists(String text) {

    }

    public void aExprPropExists(boolean exists) {

    }

    public void qExprRelOpAnyOrAll(String text, String op) {

    }

    public void aExprRelOpAnyOrAll(Boolean result) {

    }

    public void qExprIs(String text) {

    }

    public void aExprIs(boolean result) {

    }

    public void qExprTimestamp(String text) {

    }

    public void aExprTimestamp(long value) {

    }

    public void qExprInstanceof(String text) {

    }

    public void aExprInstanceof(Boolean result) {

    }

    public void qExprVariable(String text) {

    }

    public void aExprVariable(Object value) {

    }

    public void qExprStreamUndSelectClause(String text) {

    }

    public void aExprStreamUndSelectClause(EventBean event) {

    }

    public void qExprRelOp(String text, String op) {

    }

    public void aExprRelOp(Boolean result) {

    }

    public void qExprMinMaxRow(String text) {

    }

    public void aExprMinMaxRow(Object result) {

    }

    public void qExprNew(String text) {

    }

    public void aExprNew(Map<String, Object> props) {

    }

    public void qExprNot(String text) {

    }

    public void aExprNot(Boolean result) {

    }

    public void qExprIStream(String text) {

    }

    public void aExprIStream(boolean newData) {

    }

    public void qExprConst() {

    }

    public void aExprConst(Object value) {

    }

    public void qExprPlugInSingleRow(String text, String declaringClass, String methodName, String returnTypeName, String[] parameterTypes) {

    }

    public void aExprPlugInSingleRow(Object result) {

    }

    public void qExprContextProp(String text) {

    }

    public void aExprContextProp(Object result) {

    }

    public void qExprDotChain(EPType targetTypeInfo, Object target, int numUnpacking) {

    }

    public void aExprDotChain() {

    }

    public void qExprDot(String text) {

    }

    public void aExprDot(Object result) {

    }

    public void qExprStreamUndMethod(String text) {

    }

    public void aExprStreamUndMethod(Object result) {

    }

    public void qExprDotChainElement(int num, String methodType, String methodName) {
    }

    public void aExprDotChainElement(EPType typeInfo, Object result) {

    }

    public void qExprPrev(String text, boolean newData) {

    }

    public void aExprPrev(Object result) {

    }

    public void qExprPrior(String text) {

    }

    public void aExprPrior(Object result) {

    }

    public void qScheduleAdd(long currentTime, long afterMSec, ScheduleHandle handle, long slot) {

    }

    public void aScheduleAdd() {

    }

    public void qScheduleRemove(ScheduleHandle handle, long slot) {

    }

    public void aScheduleRemove() {

    }

    public void qFilterRemove(FilterHandle filterCallback, EventType eventType, FilterValueSetParam[][] parameters) {

    }

    public void aFilterRemove() {

    }

    public void qFilterAdd(EventType eventType, FilterValueSetParam[][] parameters, FilterHandle filterCallback) {

    }

    public void aFilterAdd() {

    }

    public void qExprTimePeriod(String text) {

    }

    public void aExprTimePeriod(Object result) {

    }

    public void qExprAnd(String text) {

    }

    public void aExprAnd(Boolean result) {

    }

    public void qExprLike(String text) {

    }

    public void aExprLike(Boolean result) {

    }

    public void qResultSetProcessUngroupedFullyAgg() {

    }

    public void aResultSetProcessUngroupedFullyAgg(UniformPair<EventBean[]> pair) {

    }

    public void qAggregationUngroupedApplyEnterLeave(boolean enter, int numAggregators, int numAccessStates) {

    }

    public void aAggregationUngroupedApplyEnterLeave(boolean enter) {

    }

    public void qExprAggValue(String text) {

    }

    public void aExprAggValue(Object value) {

    }

    public void qOutputProcessNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {

    }

    public void aOutputProcessNonBufferedJoin() {

    }

    public void qResultSetProcessGroupedRowPerGroup() {

    }

    public void aResultSetProcessGroupedRowPerGroup(UniformPair<EventBean[]> pair) {

    }

    public void qResultSetProcessComputeGroupKeys(boolean enter, String[] groupKeyNodeExpressions, EventBean[] eventsPerStream) {

    }

    public void aResultSetProcessComputeGroupKeys(boolean enter, Object groupKeysPerEvent) {

    }

    public void qResultSetProcessUngroupedNonfullyAgg() {

    }

    public void aResultSetProcessUngroupedNonfullyAgg(UniformPair<EventBean[]> pair) {

    }

    public void qResultSetProcessGroupedRowPerEvent() {

    }

    public void aResultSetProcessGroupedRowPerEvent(UniformPair<EventBean[]> pair) {

    }

    public void qResultSetProcessSimple() {

    }

    public void aResultSetProcessSimple(UniformPair<EventBean[]> pair) {

    }

    public void qFilter(EventBean theEvent) {

    }

    public void aFilter(Collection<FilterHandle> matches) {

    }

    public void qFilterHandleSetIndexes(List<FilterParamIndexBase> indizes) {

    }

    public void aFilterHandleSetIndexes() {

    }

    public void qFilterReverseIndex(FilterParamIndexBase filterParamIndex, Object propertyValue) {

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

    public void qExprDeclared(String text, String name, String expressionText, String[] parameterNames) {

    }

    public void aExprDeclared(Object value) {

    }

    public void qInfraUpdate(EventBean beforeUpdate, EventBean[] eventsPerStream, int length, boolean copy) {

    }

    public void aInfraUpdate(EventBean afterUpdate) {

    }

    public void qInfraUpdateRHSExpr(int index) {

    }

    public void aInfraUpdateRHSExpr(Object result) {

    }

    public void qRouteBetweenStmt(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront) {

    }

    public void aRouteBetweenStmt() {

    }

    public void qScheduleEval(long currentTime) {

    }

    public void aScheduleEval(Collection<ScheduleHandle> handles) {

    }

    public void qStatementResultExecute(UniformPair<EventBean[]> events, String deploymentId, int statementId, String statementName, long threadId) {

    }

    public void aStatementResultExecute() {

    }

    public void qOrderBy(EventBean[] events, String[] expressions, boolean[] descending) {

    }

    public void aOrderBy(Object values) {

    }

    public void qHavingClause(EventBean[] eventsPerStream) {

    }

    public void aHavingClause(Boolean pass) {

    }

    public void qExprSubselect(String text) {

    }

    public void aExprSubselect(Object result) {

    }

    public void qExprTableSubpropAccessor(String text, String tableName, String subpropName, String aggregationExpression) {

    }

    public void aExprTableSubpropAccessor(Object result) {

    }

    public void qExprTableSubproperty(String text, String tableName, String subpropName) {

    }

    public void aExprTableSubproperty(Object result) {

    }

    public void qExprTableTop(String text, String tableName) {

    }

    public void aExprTableTop(Object result) {

    }

    public void qaRuntimeManagementStmtStarted(String runtimeURI, String deploymentId, int statementId, String statementName, String epl, long runtimeTime) {

    }

    public void qaRuntimeManagementStmtStop(String runtimeURI, String deploymentId, int statementId, String statementName, String epl, long runtimeTime) {

    }

    public void qExprStreamUnd(String text) {

    }

    public void aExprStreamUnd(Object result) {

    }

    public void qaFilterHandleSetCallbacks(Set<FilterHandle> callbackSet) {

    }
}
