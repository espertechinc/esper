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
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.filterspec.ExprNodeAdapterBase;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.type.BitWiseOpEnum;
import com.espertech.esper.runtime.internal.filtersvcimpl.EventEvaluator;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterParamIndexBase;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterParamIndexBooleanExpr;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Instrumentation extends InstrumentationCommon {
    void qStimulantEvent(EventBean eventBean, String runtimeURI);

    void aStimulantEvent();

    void qStimulantTime(long currentTime, long target, long ultimateTarget, boolean span, Long resolution, String runtimeURI);

    void aStimulantTime();

    void qEvent(EventBean eventBean, String runtimeURI, boolean providedBySendEvent);

    void aEvent();

    void qEventCP(EventBean theEvent, EPStatementAgentInstanceHandle handle, long runtimeTime);

    void aEventCP();

    void qTime(long runtimeTime, String runtimeURI);

    void aTime();

    void qTimeCP(EPStatementAgentInstanceHandle handle, long runtimeTime);

    void aTimeCP();

    void qExprEquals(String text);

    void aExprEquals(Boolean result);

    void qOutputProcessNonBuffered(EventBean[] newData, EventBean[] oldData);

    void aOutputProcessNonBuffered();

    void qOutputProcessNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents);

    void aOutputProcessNonBufferedJoin();

    void qSelectClause(EventBean[] eventsPerStream, boolean newData, boolean synthesize, ExprEvaluatorContext exprEvaluatorContext);

    void aSelectClause(boolean newData, EventBean event, Object[] subscriberParameters);

    void qExprBitwise(String text, BitWiseOpEnum bitWiseOpEnum);

    void aExprBitwise(Object result);

    void qExprIdent(String fullUnresolvedName);

    void aExprIdent(Object result);

    void qExprMath(String text, String op);

    void aExprMath(Object result);

    void qExprRegexp(String text);

    void aExprRegexp(Boolean result);

    void qExprTypeof(String text);

    void aExprTypeof(String typeName);

    void qExprOr(String text);

    void aExprOr(Boolean result);

    void qExprIn(String text);

    void aExprIn(Boolean result);

    void qExprConcat(String text);

    void aExprConcat(String result);

    void qExprCoalesce(String text);

    void aExprCoalesce(Object value);

    void qExprBetween(String text);

    void aExprBetween(Boolean result);

    void qExprCast(String text);

    void aExprCast(Object result);

    void qExprCase(String text);

    void aExprCase(Object result);

    void qExprArray(String text);

    void aExprArray(Object result);

    void qExprEqualsAnyOrAll(String text);

    void aExprEqualsAnyOrAll(Boolean result);

    void qExprMinMaxRow(String text);

    void aExprMinMaxRow(Object result);

    void qExprNew(String text);

    void aExprNew(Map<String, Object> props);

    void qExprNot(String text);

    void aExprNot(Boolean result);

    void qExprIStream(String text);

    void aExprIStream(boolean newData);

    void qExprConst();

    void aExprConst(Object value);

    void qExprPropExists(String text);

    void aExprPropExists(boolean exists);

    void qExprRelOpAnyOrAll(String text, String op);

    void aExprRelOpAnyOrAll(Boolean result);

    void qExprRelOp(String text, String op);

    void aExprRelOp(Boolean result);

    void qExprStreamUndSelectClause(String text);

    void aExprStreamUndSelectClause(EventBean event);

    void qExprIs(String text);

    void aExprIs(boolean result);

    void qExprVariable(String text);

    void aExprVariable(Object value);

    void qExprInstanceof(String text);

    void aExprInstanceof(Boolean result);

    void qExprTimestamp(String text);

    void aExprTimestamp(long value);

    void qExprContextProp(String text);

    void aExprContextProp(Object result);

    void qExprPlugInSingleRow(String text, String declaringClass, String methodName, String returnTypeName, String[] parameterTypes);

    void aExprPlugInSingleRow(Object result);

    void qExprDotChain(EPType targetTypeInfo, Object target, int numUnpacking);

    void aExprDotChain();

    void qExprDot(String text);

    void aExprDot(Object result);

    void qExprStreamUndMethod(String text);

    void aExprStreamUndMethod(Object result);

    void qExprDotChainElement(int num, String methodType, String methodName);

    void aExprDotChainElement(EPType typeInfo, Object result);

    void qExprPrev(String text, boolean newData);

    void aExprPrev(Object result);

    void qExprPrior(String text);

    void aExprPrior(Object result);

    void qScheduleAdd(long currentTime, long afterMSec, ScheduleHandle handle, long slot);

    void aScheduleAdd();

    void qScheduleRemove(ScheduleHandle handle, long slot);

    void aScheduleRemove();

    void qFilterRemove(FilterHandle filterCallback, EventType eventType, FilterValueSetParam[][] parameters);

    void aFilterRemove();

    void qFilterAdd(EventType eventType, FilterValueSetParam[][] parameters, FilterHandle filterCallback);

    void aFilterAdd();

    void qExprAnd(String text);

    void aExprAnd(Boolean result);

    void qExprLike(String text);

    void aExprLike(Boolean result);

    void qResultSetProcessUngroupedFullyAgg();

    void aResultSetProcessUngroupedFullyAgg(UniformPair<EventBean[]> pair);

    void qAggregationUngroupedApplyEnterLeave(boolean enter, int numAggregators, int numAccessStates);

    void aAggregationUngroupedApplyEnterLeave(boolean enter);

    void qExprAggValue(String text);

    void aExprAggValue(Object value);

    void qResultSetProcessGroupedRowPerGroup();

    void aResultSetProcessGroupedRowPerGroup(UniformPair<EventBean[]> pair);

    void qResultSetProcessComputeGroupKeys(boolean enter, String[] groupKeyNodeExpressions, EventBean[] eventsPerStream);

    void aResultSetProcessComputeGroupKeys(boolean enter, Object groupKeysPerEvent);

    void qResultSetProcessUngroupedNonfullyAgg();

    void aResultSetProcessUngroupedNonfullyAgg(UniformPair<EventBean[]> pair);

    void qResultSetProcessGroupedRowPerEvent();

    void aResultSetProcessGroupedRowPerEvent(UniformPair<EventBean[]> pair);

    void qResultSetProcessSimple();

    void aResultSetProcessSimple(UniformPair<EventBean[]> pair);

    void qFilter(EventBean theEvent);

    void aFilter(Collection<FilterHandle> matches);

    void qFilterHandleSetIndexes(List<FilterParamIndexBase> indizes);

    void aFilterHandleSetIndexes();

    void qFilterReverseIndex(FilterParamIndexBase filterParamIndex, Object propertyValue);

    void aFilterReverseIndex(Boolean match);

    void qFilterBoolean(FilterParamIndexBooleanExpr filterParamIndexBooleanExpr);

    void aFilterBoolean();

    void qFilterBooleanExpr(int num, Map.Entry<ExprNodeAdapterBase, EventEvaluator> evals);

    void aFilterBooleanExpr(boolean result);

    void qExprDeclared(String text, String name, String expressionText, String[] parameterNames);

    void aExprDeclared(Object value);

    void qInfraUpdate(EventBean beforeUpdate, EventBean[] eventsPerStream, int length, boolean copy);

    void aInfraUpdate(EventBean afterUpdate);

    void qInfraUpdateRHSExpr(int index);

    void aInfraUpdateRHSExpr(Object result);

    void qRouteBetweenStmt(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront);

    void aRouteBetweenStmt();

    void qScheduleEval(long currentTime);

    void aScheduleEval(Collection<ScheduleHandle> handles);

    void qStatementResultExecute(UniformPair<EventBean[]> events, String deploymentId, int statementId, String statementName, long threadId);

    void aStatementResultExecute();

    void qOrderBy(EventBean[] events, String[] expressions, boolean[] descending);

    void aOrderBy(Object values);

    void qHavingClause(EventBean[] eventsPerStream);

    void aHavingClause(Boolean pass);

    void qExprSubselect(String text);

    void aExprSubselect(Object result);

    void qExprTableSubpropAccessor(String text, String tableName, String subpropName, String aggregationExpression);

    void aExprTableSubpropAccessor(Object result);

    void qExprTableSubproperty(String text, String tableName, String subpropName);

    void aExprTableSubproperty(Object result);

    void qExprTableTop(String text, String tableName);

    void aExprTableTop(Object result);

    void qaRuntimeManagementStmtStarted(String runtimeURI, String deploymentId, int statementId, String statementName, String epl, long runtimeTime);

    void qaRuntimeManagementStmtStop(String runtimeURI, String deploymentId, int statementId, String statementName, String epl, long runtimeTime);

    void qExprStreamUnd(String text);

    void aExprStreamUnd(Object result);

    void qaFilterHandleSetCallbacks(Set<FilterHandle> callbackSet);
}

