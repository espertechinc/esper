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
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceGrouped;
import com.espertech.esper.epl.table.strategy.ExprTableEvalLockUtil;
import com.espertech.esper.epl.table.strategy.ExprTableEvalStrategyUtil;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LT;
import static com.espertech.esper.epl.agg.access.AggregationAgentCodegenSymbols.NAME_AGENTSTATE;
import static com.espertech.esper.epl.agg.service.table.AggSvcGroupByWTableCodegenUtil.REF_TABLESTATEINSTANCE;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public abstract class AggSvcGroupByWTableBase implements AggregationService {
    protected final TableMetadata tableMetadata;
    protected final TableColumnMethodPair[] methodPairs;
    protected final AggregationAccessorSlotPair[] accessors;
    protected final boolean isJoin;
    protected final TableStateInstanceGrouped tableStateInstance;
    protected final int[] targetStates;
    protected final ExprNode[] accessStateExpr;
    private final AggregationAgent[] agents;

    // maintain a current row for random access into the aggregator state table
    // (row=groups, columns=expression nodes that have aggregation functions)
    protected AggregationMethod[] currentAggregatorMethods;
    protected AggregationState[] currentAggregatorStates;
    protected Object currentGroupKey;

    public AggSvcGroupByWTableBase(TableMetadata tableMetadata, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, boolean join, TableStateInstanceGrouped tableStateInstance, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents) {
        this.tableMetadata = tableMetadata;
        this.methodPairs = methodPairs;
        this.accessors = accessors;
        isJoin = join;
        this.tableStateInstance = tableStateInstance;
        this.targetStates = targetStates;
        this.accessStateExpr = accessStateExpr;
        this.agents = agents;
    }

    public static void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope) {
        explicitMembers.add(new CodegenTypedParam(AggregationMethod[].class, "currentAggregatorMethods"));
        explicitMembers.add(new CodegenTypedParam(AggregationState[].class, "currentAggregatorStates"));
        explicitMembers.add(new CodegenTypedParam(Object.class, "currentGroupKey"));
    }

    public abstract void applyEnterInternal(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext);

    public abstract void applyLeaveInternal(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext);

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);
        applyEnterInternal(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        // acquire table-level write lock
        ExprTableEvalLockUtil.obtainLockUnless(tableStateInstance.getTableLevelRWLock().writeLock(), exprEvaluatorContext);
        applyLeaveInternal(eventsPerStream, groupByKey, exprEvaluatorContext);
    }

    protected void applyEnterGroupKey(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = tableStateInstance.getCreateRowIntoTable(groupByKey, exprEvaluatorContext);
        AggregationRowPair row = (AggregationRowPair) bean.getProperties()[0];

        currentAggregatorMethods = row.getMethods();
        currentAggregatorStates = row.getStates();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, methodPairs.length, targetStates.length, groupByKey);
        }

        for (int j = 0; j < methodPairs.length; j++) {
            TableColumnMethodPair methodPair = methodPairs[j];
            AggregationMethod method = currentAggregatorMethods[methodPair.getTargetIndex()];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(true, j, method, methodPair.getAggregationNode());
            }
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, true, exprEvaluatorContext);
            method.enter(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(true, j, method);
            }
        }

        for (int i = 0; i < targetStates.length; i++) {
            AggregationState state = currentAggregatorStates[targetStates[i]];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(true, i, state, accessStateExpr[i]);
            }
            agents[i].applyEnter(eventsPerStream, exprEvaluatorContext, state);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(true, i, state);
            }
        }

        tableStateInstance.handleRowUpdated(bean);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
        }
    }

    public static CodegenMethodNode applyGroupKeyCodegen(boolean enter, CodegenMethodNode parent, CodegenClassScope classScope, TableColumnMethodPair[] methodPairs, AggregationAgentForge[] agentForges, AggregationAgent[] agents, int[] targetStates) {
        AggregationAgentCodegenSymbols symbols = new AggregationAgentCodegenSymbols(true, true);
        CodegenMethodNode method = parent.makeChildWithScope(void.class, AggSvcGroupByWTableImpl.class, symbols, classScope).addParam(EventBean[].class, NAME_EPS).addParam(Object.class, AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);

        method.getBlock().declareVar(ObjectArrayBackedEventBean.class, "event", exprDotMethod(REF_TABLESTATEINSTANCE, "getCreateRowIntoTable", AggregationServiceCodegenNames.REF_GROUPKEY, REF_EXPREVALCONTEXT))
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

    protected void applyLeaveGroupKey(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        ObjectArrayBackedEventBean bean = tableStateInstance.getCreateRowIntoTable(groupByKey, exprEvaluatorContext);
        AggregationRowPair row = (AggregationRowPair) bean.getProperties()[0];

        currentAggregatorMethods = row.getMethods();
        currentAggregatorStates = row.getStates();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, methodPairs.length, targetStates.length, groupByKey);
        }

        for (int j = 0; j < methodPairs.length; j++) {
            TableColumnMethodPair methodPair = methodPairs[j];
            AggregationMethod method = currentAggregatorMethods[methodPair.getTargetIndex()];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(false, j, method, methodPair.getAggregationNode());
            }
            Object columnResult = methodPair.getEvaluator().evaluate(eventsPerStream, false, exprEvaluatorContext);
            method.leave(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(false, j, method);
            }
        }

        for (int i = 0; i < targetStates.length; i++) {
            AggregationState state = currentAggregatorStates[targetStates[i]];
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggAccessEnterLeave(false, i, state, accessStateExpr[i]);
            }
            agents[i].applyLeave(eventsPerStream, exprEvaluatorContext, state);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggAccessEnterLeave(false, i, state);
            }
        }

        tableStateInstance.handleRowUpdated(bean);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
        }
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        ObjectArrayBackedEventBean bean = tableStateInstance.getRowForGroupKey(groupByKey);

        if (bean != null) {
            AggregationRowPair row = (AggregationRowPair) bean.getProperties()[0];
            currentAggregatorMethods = row.getMethods();
            currentAggregatorStates = row.getStates();
        } else {
            currentAggregatorMethods = null;
        }

        this.currentGroupKey = groupByKey;
    }

    public static void setCurrentAccessCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().declareVar(ObjectArrayBackedEventBean.class, "bean", exprDotMethod(REF_TABLESTATEINSTANCE, "getRowForGroupKey", AggregationServiceCodegenNames.REF_GROUPKEY))
                .ifCondition(notEqualsNull(ref("bean")))
                    .declareVar(AggregationRowPair.class, "row", cast(AggregationRowPair.class, arrayAtIndex(exprDotMethod(ref("bean"), "getProperties"), constant(0))))
                    .assignRef(ref("currentAggregatorMethods"), exprDotMethod(ref("row"), "getMethods"))
                    .assignRef(ref("currentAggregatorStates"), exprDotMethod(ref("row"), "getStates"))
                .ifElse()
                    .assignRef(ref("currentAggregatorMethods"), constantNull())
                .blockEnd()
                .assignRef("currentGroupKey", AggregationServiceCodegenNames.REF_GROUPKEY);
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (column < currentAggregatorMethods.length) {
            return currentAggregatorMethods[column].getValue();
        } else {
            AggregationAccessorSlotPair pair = accessors[column - currentAggregatorMethods.length];
            return pair.getAccessor().getValue(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public static void getGroupByValueCodegen(AggSvcTableGetterType getterType, CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, int numMethodAggs, AggregationAccessorSlotPair[] accessors) {
        CodegenMember accessorMember = classScope.makeAddMember(AggregationAccessorSlotPair[].class, accessors);
        method.getBlock().ifCondition(relational(AggregationServiceCodegenNames.REF_COLUMN, LT, arrayLength(ref("currentAggregatorMethods"))))
                    .blockReturn(getterType == AggSvcTableGetterType.GETVALUE ? exprDotMethod(arrayAtIndex(ref("currentAggregatorMethods"), AggregationServiceCodegenNames.REF_COLUMN), "getValue") : constantNull());
        method.getBlock().declareVar(int.class, "accessNum", op(AggregationServiceCodegenNames.REF_COLUMN, "-", constant(numMethodAggs)))
                .declareVar(AggregationAccessorSlotPair.class, "pair", arrayAtIndex(member(accessorMember.getMemberId()), op(AggregationServiceCodegenNames.REF_COLUMN, "-", arrayLength(ref("currentAggregatorMethods")))))
                .methodReturn(exprDotMethodChain(ref("pair")).add("getAccessor").add(getterType.getAccessorMethod(), arrayAtIndex(ref("currentAggregatorStates"), exprDotMethod(ref("pair"), "getSlot")), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < currentAggregatorMethods.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - currentAggregatorMethods.length];
            return pair.getAccessor().getEnumerableEvents(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < currentAggregatorMethods.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - currentAggregatorMethods.length];
            return pair.getAccessor().getEnumerableScalar(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (column < currentAggregatorMethods.length) {
            return null;
        } else {
            AggregationAccessorSlotPair pair = accessors[column - currentAggregatorMethods.length];
            return pair.getAccessor().getEnumerableEvent(currentAggregatorStates[pair.getSlot()], eventsPerStream, isNewData, context);
        }
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        // not applicable
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        // not applicable
    }

    public boolean isGrouped() {
        return true;
    }

    public Object getGroupKey(int agentInstanceId) {
        return currentGroupKey;
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        return tableStateInstance.getGroupKeys();
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        // clear not required
    }

    public void stop() {
    }

    public AggregationService getContextPartitionAggregationService(int agentInstanceId) {
        return this;
    }
}
