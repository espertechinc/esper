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
package com.espertech.esper.epl.agg.service.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceUngrouped;
import com.espertech.esper.epl.table.strategy.ExprTableEvalLockUtil;
import com.espertech.esper.epl.table.strategy.ExprTableEvalStrategyUtil;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.epl.agg.access.AggregationAgentCodegenSymbols.NAME_AGENTSTATE;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.REF_COLUMN;
import static com.espertech.esper.epl.agg.service.table.AggSvcGroupByWTableCodegenUtil.REF_TABLESTATEINSTANCE;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Implementation for handling aggregation without any grouping (no group-by).
 */
public class AggSvcGroupAllWTableImpl implements AggregationService {
    private final TableStateInstanceUngrouped tableStateInstance;
    private final TableColumnMethodPair[] methodPairs;
    private final AggregationAccessorSlotPair[] accessors;
    private final int[] targetStates;
    private final ExprNode[] accessStateExpr;
    private final AggregationAgent[] agents;

    public AggSvcGroupAllWTableImpl(TableStateInstanceUngrouped tableStateInstance, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents) {
        this.tableStateInstance = tableStateInstance;
        this.methodPairs = methodPairs;
        this.accessors = accessors;
        this.targetStates = targetStates;
        this.accessStateExpr = accessStateExpr;
        this.agents = agents;
    }

    public void applyEnter(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableStateInstance.getCreateRowIntoTable(null, exprEvaluatorContext);
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationUngroupedApplyEnterLeave(true, row.getMethods().length, row.getStates().length);
        }
        for (int i = 0; i < methodPairs.length; i++) {
            TableColumnMethodPair methodPair = methodPairs[i];
            AggregationMethod method = row.getMethods()[methodPair.getTargetIndex()];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(true, i, method, methodPair.getAggregationNode());
            }
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
            method.enter(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(true, i, method);
            }
        }

        for (int i = 0; i < targetStates.length; i++) {
            AggregationState state = row.getStates()[targetStates[i]];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, state, accessStateExpr[i]);
            }
            agents[i].applyEnter(eventsPerStream, exprEvaluatorContext, state);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, state);
            }
        }

        tableStateInstance.handleRowUpdated(event);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationUngroupedApplyEnterLeave(true);
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, Object optionalGroupKeyPerRow, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableStateInstance.getCreateRowIntoTable(null, exprEvaluatorContext);
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationUngroupedApplyEnterLeave(false, row.getMethods().length, row.getStates().length);
        }

        for (int i = 0; i < methodPairs.length; i++) {
            TableColumnMethodPair methodPair = methodPairs[i];
            AggregationMethod method = row.getMethods()[methodPair.getTargetIndex()];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(false, i, method, methodPair.getAggregationNode());
            }
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, false, exprEvaluatorContext);
            method.leave(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(false, i, method);
            }
        }

        for (int i = 0; i < targetStates.length; i++) {
            AggregationState state = row.getStates()[targetStates[i]];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, state, accessStateExpr[i]);
            }
            agents[i].applyLeave(eventsPerStream, exprEvaluatorContext, state);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, state);
            }
        }

        tableStateInstance.handleRowUpdated(event);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationUngroupedApplyEnterLeave(false);
        }
    }

    public static CodegenMethodNode applyCodegen(boolean enter, CodegenMethodScope parent, CodegenClassScope classScope, TableColumnMethodPair[] methodPairs, AggregationAgentForge[] agentForges, AggregationAgent[] agents, int[] targetStates) {
        AggregationAgentCodegenSymbols symbols = new AggregationAgentCodegenSymbols(true, true);
        CodegenMethodNode method = parent.makeChildWithScope(void.class, AggSvcGroupAllWTableImpl.class, symbols, classScope).addParam(EventBean[].class, NAME_EPS).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);

        method.getBlock().declareVar(ObjectArrayBackedEventBean.class, "event", exprDotMethod(REF_TABLESTATEINSTANCE, "getCreateRowIntoTable", constantNull(), REF_EXPREVALCONTEXT))
                .declareVar(AggregationRowPair.class, "row", staticMethod(ExprTableEvalStrategyUtil.class, "getRow", ref("event")))
                .declareVarNoInit(Object.class, "columnResult")
                .declareVarNoInit(AggregationMethod.class, "method")
                .declareVarNoInit(AggregationState.class, NAME_AGENTSTATE);

        CodegenExpression[] methodEnterLeave = AggSvcGroupByWTableUtil.getMethodEnterLeave(methodPairs, method, symbols, classScope);
        CodegenExpression[] accessEnterLeave = AggSvcGroupByWTableUtil.getAccessEnterLeave(enter, agentForges, agents, method, symbols, classScope);

        symbols.derivedSymbolsCodegen(method, method.getBlock(), classScope);

        for (int i = 0; i < methodPairs.length; i++) {
            TableColumnMethodPair methodPair = methodPairs[i];
            method.getBlock().assignRef("method", arrayAtIndex(exprDotMethod(ref("row"), "getMethods"), constant(methodPair.getTargetIndex())))
                    .assignRef("columnResult", methodEnterLeave[i])
                    .exprDotMethod(ref("method"), enter ? "enter" : "leave", ref("columnResult"));
        }

        for (int i = 0; i < agentForges.length; i++) {
            method.getBlock().assignRef("state", arrayAtIndex(exprDotMethod(ref("row"), "getStates"), constant(targetStates[i])))
                    .expression(accessEnterLeave[i]);
        }

        method.getBlock().exprDotMethod(REF_TABLESTATEINSTANCE, "handleRowUpdated", ref("event"));

        return method;
    }

    public void setCurrentAccess(Object groupKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        // no action needed - this implementation does not group and the current row is the single group
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();
        if (column < aggregators.length) {
            return aggregators[column].getValue();
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getValue(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public static void getGroupAllValueCodegen(AggSvcTableGetterType getterType, CodegenMethodNode method, CodegenClassScope classScope, AggregationAccessorSlotPair[] accessors) {
        CodegenMember accessorMember = classScope.makeAddMember(AggregationAccessorSlotPair[].class, accessors);
        method.getBlock().declareVar(ObjectArrayBackedEventBean.class, "event", exprDotMethod(REF_TABLESTATEINSTANCE, "getEventUngrouped"))
                .ifRefNullReturnNull("event")
                .declareVar(AggregationRowPair.class, "row", staticMethod(ExprTableEvalStrategyUtil.class, "getRow", ref("event")))
                .declareVar(AggregationMethod[].class, "aggregators", exprDotMethod(ref("row"), "getMethods"))
                .ifCondition(relational(REF_COLUMN, LT, arrayLength(ref("aggregators"))))
                    .blockReturn(getterType == AggSvcTableGetterType.GETVALUE ? exprDotMethod(arrayAtIndex(ref("aggregators"), REF_COLUMN), "getValue") : constantNull())
                .declareVar(AggregationAccessorSlotPair.class, "pair", arrayAtIndex(member(accessorMember.getMemberId()), op(REF_COLUMN, "-", arrayLength(ref("aggregators")))))
                .methodReturn(exprDotMethodChain(ref("pair")).add("getAccessor").add(getterType.getAccessorMethod(), arrayAtIndex(exprDotMethod(ref("row"), "getStates"), exprDotMethod(ref("pair"), "getSlot")), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvents(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();
        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableScalar(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), context);

        ObjectArrayBackedEventBean event = tableStateInstance.getEventUngrouped();
        if (event == null) {
            return null;
        }
        AggregationRowPair row = ExprTableEvalStrategyUtil.getRow(event);
        AggregationMethod[] aggregators = row.getMethods();

        if (column < aggregators.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - aggregators.length];
            return pair.getAccessor().getEnumerableEvent(row.getStates()[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        // clear not required
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        // not applicable
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
    }

    public boolean isGrouped() {
        return false;
    }

    public Object getGroupKey(int agentInstanceId) {
        return null;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public void stop() {
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return this;
    }
}