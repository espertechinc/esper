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
package com.espertech.esper.epl.core.resultset.rowforall;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenInstanceAux;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputHelperVisitor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil.*;
import static com.espertech.esper.util.CollectionUtil.METHOD_TOARRAYMAYNULL;

/**
 * Result set processor for the case: aggregation functions used in the select clause, and no group-by,
 * and all properties in the select clause are under an aggregation function.
 * <p>
 * This processor does not perform grouping, every event entering and leaving is in the same group.
 * Produces one old event and one new event row every time either at least one old or new event is received.
 * Aggregation state is simply one row holding all the state.
 */
public class ResultSetProcessorRowForAllImpl implements ResultSetProcessorRowForAll {
    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";

    protected final ResultSetProcessorRowForAllFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected ExprEvaluatorContext exprEvaluatorContext;
    private ResultSetProcessorRowForAllOutputLastHelper outputLastHelper;
    private ResultSetProcessorRowForAllOutputAllHelper outputAllHelper;

    ResultSetProcessorRowForAllImpl(ResultSetProcessorRowForAllFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.exprEvaluatorContext = agentInstanceContext;
        if (prototype.getOutputConditionType() == ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED) {
            if (prototype.isOutputLast()) {
                outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowForAllOutputLast(this, agentInstanceContext);
            } else if (prototype.isOutputAll()) {
                outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowForAllOutputAll(this, agentInstanceContext);
            }
        }
    }

    public boolean isSelectRStream() {
        return prototype.isSelectRStream();
    }

    public ExprEvaluatorContext getExprEvaluatorContext() {
        return exprEvaluatorContext;
    }

    public void setAgentInstanceContext(AgentInstanceContext context) {
        this.exprEvaluatorContext = context;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessUngroupedFullyAgg();
        }

        if (prototype.isUnidirectional()) {
            this.clear();
        }

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = getSelectListEventsAsArray(false, isSynthesize, true);
        }

        ResultSetProcessorUtil.applyAggJoinResult(aggregationService, exprEvaluatorContext, newEvents, oldEvents);

        EventBean[] selectNewEvents = getSelectListEventsAsArray(true, isSynthesize, true);

        if ((selectNewEvents == null) && (selectOldEvents == null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessUngroupedFullyAgg(null, null);
            }
            return null;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessUngroupedFullyAgg(selectNewEvents, selectOldEvents);
        }
        return new UniformPair<>(selectNewEvents, selectOldEvents);
    }

    public static void processJoinResultCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instanceMethods) {
        CodegenMethodNode selectList = getSelectListEventsAsArrayCodegen(forge, classScope, instanceMethods);

        if (forge.isUnidirectional()) {
            method.getBlock().expression(exprDotMethod(ref("this"), "clear"));
        }

        CodegenExpression selectOld;
        if (forge.isSelectRStream()) {
            selectOld = localMethod(selectList, constantFalse(), REF_ISSYNTHESIZE, constantTrue());
        } else {
            selectOld = constantNull();
        }
        method.getBlock()
                .declareVar(EventBean[].class, "selectOldEvents", selectOld)
                .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA)
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(selectList, constantTrue(), REF_ISSYNTHESIZE, constantTrue()))
                .ifCondition(and(equalsNull(ref("selectNewEvents")), equalsNull(ref("selectOldEvents"))))
                .blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessUngroupedFullyAgg();
        }
        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        if (prototype.isSelectRStream()) {
            selectOldEvents = getSelectListEventsAsArray(false, isSynthesize, false);
        }

        EventBean[] eventsPerStream = new EventBean[1];
        ResultSetProcessorUtil.applyAggViewResult(aggregationService, exprEvaluatorContext, newData, oldData, eventsPerStream);

        // generate new events using select expressions
        selectNewEvents = getSelectListEventsAsArray(true, isSynthesize, false);

        if ((selectNewEvents == null) && (selectOldEvents == null)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aResultSetProcessUngroupedFullyAgg(null, null);
            }
            return null;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessUngroupedFullyAgg(selectNewEvents, selectOldEvents);
        }
        return new UniformPair<>(selectNewEvents, selectOldEvents);
    }

    public static void processViewResultCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {

        CodegenMethodNode selectList = getSelectListEventsAsArrayCodegen(forge, classScope, instance);

        CodegenExpression selectOld;
        if (forge.isSelectRStream()) {
            selectOld = localMethod(selectList, constantFalse(), REF_ISSYNTHESIZE, constantFalse());
        } else {
            selectOld = constantNull();
        }
        method.getBlock()
                .declareVar(EventBean[].class, "selectOldEvents", selectOld)
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA, ref("eventsPerStream"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(selectList, constantTrue(), REF_ISSYNTHESIZE, constantFalse()))
                .ifCondition(and(equalsNull(ref("selectNewEvents")), equalsNull(ref("selectOldEvents"))))
                .blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        if (!prototype.isHistoricalOnly()) {
            return obtainIterator();
        }

        ResultSetProcessorUtil.clearAndAggregateUngrouped(exprEvaluatorContext, aggregationService, parent);
        Iterator<EventBean> iterator = obtainIterator();
        aggregationService.clearResults(exprEvaluatorContext);
        return iterator;
    }

    static void getIteratorViewCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode obtainMethod = obtainIteratorCodegen(forge, classScope, method, instance);
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainMethod));
            return;
        }

        method.getBlock()
                .staticMethod(ResultSetProcessorUtil.class, METHOD_CLEARANDAGGREGATEUNGROUPED, REF_AGENTINSTANCECONTEXT, REF_AGGREGATIONSVC, REF_VIEWABLE)
                .declareVar(Iterator.class, "iterator", localMethod(obtainMethod))
                .expression(exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT))
                .methodReturn(ref("iterator"));
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        EventBean[] result = getSelectListEventsAsArray(true, true, true);
        return new ArrayEventIterator(result);
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode select = getSelectListEventsAsArrayCodegen(forge, classScope, instance);
        method.getBlock()
                .declareVar(EventBean[].class, "result", localMethod(select, constant(true), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("result")));
    }

    public void clear() {
        aggregationService.clearResults(exprEvaluatorContext);
    }

    static void clearCodegen(CodegenMethodNode method) {
        method.getBlock().expression(exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT));
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        if (prototype.isOutputLast()) {
            return processOutputLimitedJoinLast(joinEventsSet, generateSynthetic);
        } else {
            return processOutputLimitedJoinDefault(joinEventsSet, generateSynthetic);
        }
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            processOutputLimitedJoinLastCodegen(forge, classScope, method, instance);
        } else {
            processOutputLimitedJoinDefaultCodegen(forge, classScope, method, instance);
        }
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean isSynthesize) {
        if (prototype.isOutputLast()) {
            return processOutputLimitedViewLast(viewEventsList, isSynthesize);
        } else {
            return processOutputLimitedViewDefault(viewEventsList, isSynthesize);
        }
    }

    public static void processOutputLimitedViewCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            processOutputLimitedViewLastCodegen(forge, classScope, method, instance);
        } else {
            processOutputLimitedViewDefaultCodegen(forge, classScope, method, instance);
        }
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        ResultSetProcessorUtil.applyAggViewResult(aggregationService, exprEvaluatorContext, newData, oldData, new EventBean[1]);
    }

    public static void applyViewResultCodegen(CodegenMethodNode method) {
        method.getBlock().staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA, newArrayByLength(EventBean.class, constant(1)));
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        ResultSetProcessorUtil.applyAggJoinResult(aggregationService, exprEvaluatorContext, newEvents, oldEvents);
    }

    public static void applyJoinResultCodegen(CodegenMethodNode method) {
        method.getBlock().staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA);
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public void stop() {
        if (outputLastHelper != null) {
            outputLastHelper.destroy();
        }
        if (outputAllHelper != null) {
            outputAllHelper.destroy();
        }
    }

    static void stopCodegen(CodegenMethodNode method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), "destroy");
        }
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (prototype.isOutputAll()) {
            outputAllHelper.processView(newData, oldData, isGenerateSynthetic);
        } else {
            outputLastHelper.processView(newData, oldData, isGenerateSynthetic);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen("processView", forge, classScope, method, instance);
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        if (prototype.isOutputAll()) {
            outputAllHelper.processJoin(newEvents, oldEvents, isGenerateSynthetic);
        } else {
            outputLastHelper.processJoin(newEvents, oldEvents, isGenerateSynthetic);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen("processJoin", forge, classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(String methodName, ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMember factory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());

        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorRowForAllOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSRowForAllOutputAll", ref("this"), REF_AGENTINSTANCECONTEXT));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorRowForAllOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSRowForAllOutputLast", ref("this"), REF_AGENTINSTANCECONTEXT));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        }
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize) {
        if (prototype.isOutputAll()) {
            return outputAllHelper.outputView(isSynthesize);
        }
        return outputLastHelper.outputView(isSynthesize);
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowForAllForge forge, CodegenMethodNode method) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTALLHELPER), "outputView", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "outputView", REF_ISSYNTHESIZE));
        }
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize) {
        if (prototype.isOutputAll()) {
            return outputAllHelper.outputJoin(isSynthesize);
        }
        return outputLastHelper.outputJoin(isSynthesize);
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowForAllForge forge, CodegenMethodNode method) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        }
    }

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        if (outputLastHelper != null) {
            visitor.visit(outputLastHelper);
        }
        if (outputAllHelper != null) {
            visitor.visit(outputAllHelper);
        }
    }

    public static void acceptHelperVisitorCodegen(CodegenMethodNode method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTLASTHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTALLHELPER));
        }
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinDefault(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean isSynthesize) {
        List<EventBean> newEvents = new LinkedList<>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new LinkedList<>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedList<>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedList<>();
            }
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            if (prototype.isUnidirectional()) {
                this.clear();
            }
            if (prototype.isSelectRStream()) {
                getSelectListEventAddToList(false, isSynthesize, oldEvents);
                if (orderByProcessor != null) {
                    oldEventsSortKey.add(orderByProcessor.getSortKey(null, false, exprEvaluatorContext));
                }
            }
            ResultSetProcessorUtil.applyAggJoinResult(aggregationService, exprEvaluatorContext, pair.getFirst(), pair.getSecond());
            getSelectListEventAddToList(true, isSynthesize, newEvents);
            if (orderByProcessor != null) {
                newEventsSortKey.add(orderByProcessor.getSortKey(null, true, exprEvaluatorContext));
            }
        }

        if (!joinEventsSet.isEmpty()) {
            return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, exprEvaluatorContext);
        }

        EventBean[] newEventsArr = getSelectListEventsAsArray(true, isSynthesize, true);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = getSelectListEventsAsArray(false, isSynthesize, true);
        }
        return ResultSetProcessorUtil.toPairNullIfAllNull(newEventsArr, oldEventsArr);
    }

    private static void processOutputLimitedJoinDefaultCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode getSelectListEventAddList = getSelectListEventsAddListCodegen(forge, classScope, instance);
        CodegenMethodNode getSelectListEventAsArray = getSelectListEventsAsArrayCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }
            if (forge.isSelectRStream()) {
                forEach.localMethod(getSelectListEventAddList, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"));
                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("oldEventsSortKey"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", constantNull(), constantFalse(), REF_AGENTINSTANCECONTEXT));
                }
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, cast(Set.class, exprDotMethod(ref("pair"), "getFirst")), cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));
            forEach.localMethod(getSelectListEventAddList, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"));
            if (forge.isSorting()) {
                forEach.exprDotMethod(ref("newEventsSortKey"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", constantNull(), constantTrue(), REF_AGENTINSTANCECONTEXT));
            }
            forEach.blockEnd();
        }

        CodegenBlock ifEmpty = method.getBlock().ifCondition(not(exprDotMethod(REF_JOINEVENTSSET, "isEmpty")));
        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(ifEmpty, ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());

        method.getBlock().declareVar(EventBean[].class, "newEventsArr", localMethod(getSelectListEventAsArray, constantTrue(), REF_ISSYNTHESIZE, constantFalse()))
                .declareVar(EventBean[].class, "oldEventsArr", forge.isSelectRStream() ? localMethod(getSelectListEventAsArray, constantFalse(), REF_ISSYNTHESIZE, constantFalse()) : constantNull())
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinLast(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        EventBean lastOldEvent = null;
        EventBean lastNewEvent = null;

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            if (prototype.isUnidirectional()) {
                this.clear();
            }
            if ((lastOldEvent == null) && (prototype.isSelectRStream())) {
                lastOldEvent = getSelectListEventSingle(false, generateSynthetic, true);
            }
            ResultSetProcessorUtil.applyAggJoinResult(aggregationService, exprEvaluatorContext, pair.getFirst(), pair.getSecond());
            lastNewEvent = getSelectListEventSingle(true, generateSynthetic, true);
        }

        // if empty (nothing to post)
        if (joinEventsSet.isEmpty()) {
            if (prototype.isSelectRStream()) {
                lastOldEvent = getSelectListEventSingle(false, generateSynthetic, true);
                lastNewEvent = lastOldEvent;
            } else {
                lastNewEvent = getSelectListEventSingle(false, generateSynthetic, true);
            }
        }

        EventBean[] lastNew = CollectionUtil.toArrayMayNull(lastNewEvent);
        EventBean[] lastOld = CollectionUtil.toArrayMayNull(lastOldEvent);
        if ((lastNew == null) && (lastOld == null)) {
            return null;
        }
        return new UniformPair<>(lastNew, lastOld);
    }

    private static void processOutputLimitedJoinLastCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode getSelectListEventSingle = getSelectListEventSingleCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EventBean.class, "lastOldEvent", constantNull())
                .declareVar(EventBean.class, "lastNewEvent", constantNull());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }
            if (forge.isSelectRStream()) {
                forEach.ifCondition(equalsNull(ref("lastOldEvent")))
                        .assignRef("lastOldEvent", localMethod(getSelectListEventSingle, constantFalse(), REF_ISSYNTHESIZE))
                        .blockEnd();
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, cast(Set.class, exprDotMethod(ref("pair"), "getFirst")), cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));
            forEach.assignRef("lastNewEvent", localMethod(getSelectListEventSingle, constantTrue(), REF_ISSYNTHESIZE));
        }

        {
            CodegenBlock ifEmpty = method.getBlock().ifCondition(exprDotMethod(REF_JOINEVENTSSET, "isEmpty"));
            if (forge.isSelectRStream()) {
                ifEmpty.assignRef("lastOldEvent", localMethod(getSelectListEventSingle, constantFalse(), REF_ISSYNTHESIZE))
                        .assignRef("lastNewEvent", ref("lastOldEvent"));
            } else {
                ifEmpty.assignRef("lastNewEvent", localMethod(getSelectListEventSingle, constantTrue(), REF_ISSYNTHESIZE));
            }
        }

        method.getBlock()
                .declareVar(EventBean[].class, "lastNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastNewEvent")))
                .declareVar(EventBean[].class, "lastOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastOldEvent")))
                .ifCondition(and(equalsNull(ref("lastNew")), equalsNull(ref("lastOld"))))
                .blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("lastNew"), ref("lastOld")));
    }

    private UniformPair<EventBean[]> processOutputLimitedViewDefault(List<UniformPair<EventBean[]>> viewEventsList, boolean isSynthesize) {
        List<EventBean> newEvents = new LinkedList<>();
        List<EventBean> oldEvents = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new LinkedList<>();
        }

        List<Object> newEventsSortKey = null;
        List<Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedList<>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedList<>();
            }
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            if (prototype.isSelectRStream()) {
                getSelectListEventAddToList(false, isSynthesize, oldEvents);
                if (orderByProcessor != null) {
                    oldEventsSortKey.add(orderByProcessor.getSortKey(null, false, exprEvaluatorContext));
                }
            }
            ResultSetProcessorUtil.applyAggViewResult(aggregationService, exprEvaluatorContext, pair.getFirst(), pair.getSecond(), eventsPerStream);
            getSelectListEventAddToList(true, isSynthesize, newEvents);
            if (orderByProcessor != null) {
                newEventsSortKey.add(orderByProcessor.getSortKey(null, true, exprEvaluatorContext));
            }
        }

        if (!viewEventsList.isEmpty()) {
            return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, exprEvaluatorContext);
        }

        EventBean[] newEventsArr = getSelectListEventsAsArray(true, isSynthesize, false);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = getSelectListEventsAsArray(false, isSynthesize, false);
        }
        return ResultSetProcessorUtil.toPairNullIfAllNull(newEventsArr, oldEventsArr);
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode getSelectListEventAddList = getSelectListEventsAddListCodegen(forge, classScope, instance);
        CodegenMethodNode getSelectListEventAsArray = getSelectListEventsAsArrayCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
        CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
        {
            if (forge.isSelectRStream()) {
                forEach.localMethod(getSelectListEventAddList, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"));
                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("oldEventsSortKey"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", constantNull(), constantFalse(), REF_AGENTINSTANCECONTEXT));
                }
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")), cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")), ref("eventsPerStream"));
            forEach.localMethod(getSelectListEventAddList, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"));
            if (forge.isSorting()) {
                forEach.exprDotMethod(ref("newEventsSortKey"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", constantNull(), constantTrue(), REF_AGENTINSTANCECONTEXT));
            }
            forEach.blockEnd();
        }

        CodegenBlock ifEmpty = method.getBlock().ifCondition(not(exprDotMethod(REF_VIEWEVENTSLIST, "isEmpty")));
        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(ifEmpty, ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());

        method.getBlock().declareVar(EventBean[].class, "newEventsArr", localMethod(getSelectListEventAsArray, constantTrue(), REF_ISSYNTHESIZE, constantFalse()))
                .declareVar(EventBean[].class, "oldEventsArr", forge.isSelectRStream() ? localMethod(getSelectListEventAsArray, constantFalse(), REF_ISSYNTHESIZE, constantFalse()) : constantNull())
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private UniformPair<EventBean[]> processOutputLimitedViewLast(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        // For last, if there are no events:
        //   As insert stream, return the current value, if matching the having clause
        //   As remove stream, return the current value, if matching the having clause
        // For last, if there are events in the batch:
        //   As insert stream, return the newest value that is matching the having clause
        //   As remove stream, return the oldest value that is matching the having clause

        EventBean lastOldEvent = null;
        EventBean lastNewEvent = null;
        EventBean[] eventsPerStream = new EventBean[1];

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            if ((lastOldEvent == null) && (prototype.isSelectRStream())) {
                lastOldEvent = getSelectListEventSingle(false, generateSynthetic, false);
            }
            ResultSetProcessorUtil.applyAggViewResult(aggregationService, exprEvaluatorContext, pair.getFirst(), pair.getSecond(), eventsPerStream);
            lastNewEvent = getSelectListEventSingle(true, generateSynthetic, false);
        }

        // if empty (nothing to post)
        if (viewEventsList.isEmpty()) {
            if (prototype.isSelectRStream()) {
                lastOldEvent = getSelectListEventSingle(false, generateSynthetic, false);
                lastNewEvent = lastOldEvent;
            } else {
                lastNewEvent = getSelectListEventSingle(true, generateSynthetic, false);
            }
        }

        EventBean[] lastNew = CollectionUtil.toArrayMayNull(lastNewEvent);
        EventBean[] lastOld = CollectionUtil.toArrayMayNull(lastOldEvent);
        if ((lastNew == null) && (lastOld == null)) {
            return null;
        }
        return new UniformPair<>(lastNew, lastOld);
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode getSelectListEventSingle = getSelectListEventSingleCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EventBean.class, "lastOldEvent", constantNull())
                .declareVar(EventBean.class, "lastNewEvent", constantNull())
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            if (forge.isSelectRStream()) {
                forEach.ifCondition(equalsNull(ref("lastOldEvent")))
                        .assignRef("lastOldEvent", localMethod(getSelectListEventSingle, constantFalse(), REF_ISSYNTHESIZE))
                        .blockEnd();
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")), cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")), ref("eventsPerStream"));
            forEach.assignRef("lastNewEvent", localMethod(getSelectListEventSingle, constantTrue(), REF_ISSYNTHESIZE));
        }

        {
            CodegenBlock ifEmpty = method.getBlock().ifCondition(exprDotMethod(REF_VIEWEVENTSLIST, "isEmpty"));
            if (forge.isSelectRStream()) {
                ifEmpty.assignRef("lastOldEvent", localMethod(getSelectListEventSingle, constantFalse(), REF_ISSYNTHESIZE))
                        .assignRef("lastNewEvent", ref("lastOldEvent"));
            } else {
                ifEmpty.assignRef("lastNewEvent", localMethod(getSelectListEventSingle, constantTrue(), REF_ISSYNTHESIZE));
            }
        }

        method.getBlock()
                .declareVar(EventBean[].class, "lastNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastNewEvent")))
                .declareVar(EventBean[].class, "lastOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastOldEvent")))
                .ifCondition(and(equalsNull(ref("lastNew")), equalsNull(ref("lastOld"))))
                .blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("lastNew"), ref("lastOld")));
    }

    private Iterator<EventBean> obtainIterator() {
        EventBean[] events = getSelectListEventsAsArray(true, true, false);
        if (events == null) {
            return CollectionUtil.NULL_EVENT_ITERATOR;
        }
        return new SingleEventIterator(events[0]);
    }

    private static CodegenMethodNode obtainIteratorCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethodNode parent, CodegenInstanceAux instance) {
        CodegenMethodNode selectList = getSelectListEventsAsArrayCodegen(forge, classScope, instance);
        CodegenMethodNode method = parent.makeChild(Iterator.class, ResultSetProcessorRowForAllImpl.class, classScope);
        method.getBlock().declareVar(EventBean[].class, "events", localMethod(selectList, constantTrue(), constantTrue(), constantFalse()))
                .ifRefNull("events")
                .blockReturn(enumValue(CollectionUtil.class, "NULL_EVENT_ITERATOR"))
                .methodReturn(newInstance(SingleEventIterator.class, arrayAtIndex(ref("events"), constant(0))));
        return method;
    }

    private EventBean getSelectListEventSingle(boolean isNewData, boolean isSynthesize, boolean join) {
        if (prototype.getOptionalHavingNode() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), null, isNewData, exprEvaluatorContext);
            if (!passesHaving) {
                return null;
            }
        }

        // Since we are dealing with strictly aggregation nodes, there are no events required for evaluating
        // The result is always a single row
        return selectExprProcessor.process(CollectionUtil.EVENTBEANARRAY_EMPTY, isNewData, isSynthesize, exprEvaluatorContext);
    }

    private static CodegenMethodNode getSelectListEventSingleCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = method -> {
            if (forge.getOptionalHavingNode() != null) {
                CodegenLegoMethodExpression.codegenBooleanExpressionReturnNullIfNullOrNotPass(forge.getOptionalHavingNode(), classScope, method, constantNull(), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT);
            }
            method.getBlock().methodReturn(exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", enumValue(CollectionUtil.class, "EVENTBEANARRAY_EMPTY"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));
        };
        return instance.getMethods().addMethod(EventBean.class, "getSelectListEventSingle", CodegenNamedParam.from(boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowForAllImpl.class, classScope, code);
    }

    private void getSelectListEventAddToList(boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents) {
        if (prototype.getOptionalHavingNode() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), null, isNewData, exprEvaluatorContext);
            if (!passesHaving) {
                return;
            }
        }

        // Since we are dealing with strictly aggregation nodes, there are no events required for evaluating
        EventBean theEvent = selectExprProcessor.process(CollectionUtil.EVENTBEANARRAY_EMPTY, isNewData, isSynthesize, exprEvaluatorContext);
        resultEvents.add(theEvent);
    }

    private static CodegenMethodNode getSelectListEventsAddListCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = method -> {
            if (forge.getOptionalHavingNode() != null) {
                CodegenLegoMethodExpression.codegenBooleanExpressionReturnIfNullOrNotPass(forge.getOptionalHavingNode(), classScope, method, constantNull(), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT);
            }
            method.getBlock().declareVar(EventBean.class, "theEvent", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", enumValue(CollectionUtil.class, "EVENTBEANARRAY_EMPTY"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT))
                    .expression(exprDotMethod(ref("resultEvents"), "add", ref("theEvent")));
        };
        return instance.getMethods().addMethod(void.class, "getSelectListEventsAddList", CodegenNamedParam.from(boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents"), ResultSetProcessorRowForAllImpl.class, classScope, code);
    }

    public EventBean[] getSelectListEventsAsArray(boolean isNewData, boolean isSynthesize, boolean join) {
        if (prototype.getOptionalHavingNode() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), null, isNewData, exprEvaluatorContext);
            if (!passesHaving) {
                return null;
            }
        }

        // Since we are dealing with strictly aggregation nodes, there are no events required for evaluating
        EventBean theEvent = selectExprProcessor.process(CollectionUtil.EVENTBEANARRAY_EMPTY, isNewData, isSynthesize, exprEvaluatorContext);

        // The result is always a single row
        return new EventBean[]{theEvent};
    }

    static CodegenMethodNode getSelectListEventsAsArrayCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = method -> {
            if (forge.getOptionalHavingNode() != null) {
                CodegenLegoMethodExpression.codegenBooleanExpressionReturnNullIfNullOrNotPass(forge.getOptionalHavingNode(), classScope, method, constantNull(), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT);
            }
            method.getBlock().declareVar(EventBean.class, "theEvent", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", enumValue(CollectionUtil.class, "EVENTBEANARRAY_EMPTY"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT))
                    .declareVar(EventBean[].class, "result", newArrayByLength(EventBean.class, constant(1)))
                    .assignArrayElement("result", constant(0), ref("theEvent"))
                    .methodReturn(ref("result"));
        };
        return instance.getMethods().addMethod(EventBean[].class, "getSelectListEventsAsArray", CodegenNamedParam.from(boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, boolean.class, "join"), ResultSetProcessorRowForAllImpl.class, classScope, code);
    }
}
