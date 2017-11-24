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
package com.espertech.esper.epl.core.resultset.handthru;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenInstanceAux;
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.TransformEventIterator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputHelperVisitor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.view.OutputProcessViewConditionLastAllUnord;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil.METHOD_GETSELECTEVENTSNOHAVING;
import static com.espertech.esper.event.EventBeanUtility.METHOD_FLATTENBATCHJOIN;
import static com.espertech.esper.event.EventBeanUtility.METHOD_FLATTENBATCHSTREAM;
import static com.espertech.esper.util.CollectionUtil.METHOD_TOARRAYEVENTS;
import static com.espertech.esper.util.CollectionUtil.METHOD_TOARRAYOBJECTS;

/**
 * Result set processor for the simplest case: no aggregation functions used in the select clause, and no group-by.
 * <p>
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 */
public class ResultSetProcessorSimpleImpl implements ResultSetProcessorSimple {
    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";

    protected final ResultSetProcessorSimpleFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private final OrderByProcessor orderByProcessor;
    protected ExprEvaluatorContext exprEvaluatorContext;
    private ResultSetProcessorSimpleOutputLastHelper outputLastHelper;
    private ResultSetProcessorSimpleOutputAllHelper outputAllHelper;

    ResultSetProcessorSimpleImpl(ResultSetProcessorSimpleFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.exprEvaluatorContext = agentInstanceContext;
        if (prototype.isOutputLast()) { // output-last always uses this mechanism
            outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSSimpleOutputLast(this, agentInstanceContext, prototype.getNumStreams());
        } else if (prototype.isOutputAll() && prototype.getOutputConditionType() == ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED) {
            outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSSimpleOutputAll(this, agentInstanceContext, prototype.getNumStreams());
        }
    }

    public void setAgentInstanceContext(AgentInstanceContext context) {
        exprEvaluatorContext = context;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessSimple();
        }

        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        if (prototype.getOptionalHavingNode() == null) {
            if (prototype.isSelectRStream()) {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, oldEvents, false, isSynthesize, exprEvaluatorContext);
                } else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, oldEvents, false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, newEvents, true, isSynthesize, exprEvaluatorContext);
            } else {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, newEvents, true, isSynthesize, exprEvaluatorContext);
            }
        } else {
            if (prototype.isSelectRStream()) {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, oldEvents, prototype.getOptionalHavingNode(), false, isSynthesize, exprEvaluatorContext);
                } else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, oldEvents, prototype.getOptionalHavingNode(), false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, newEvents, prototype.getOptionalHavingNode(), true, isSynthesize, exprEvaluatorContext);
            } else {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, newEvents, prototype.getOptionalHavingNode(), true, isSynthesize, exprEvaluatorContext);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessSimple(selectNewEvents, selectOldEvents);
        }
        return new UniformPair<>(selectNewEvents, selectOldEvents); // we return a pair even if both are null
    }

    public static void processJoinResultCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", constantNull())
                .declareVarNoInit(EventBean[].class, "selectNewEvents");
        ResultSetProcessorUtil.processJoinResultCodegen(method, classScope, instance, forge.getOptionalHavingNode() != null, forge.isSelectRStream(), forge.isSorting(), false);
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessSimple();
        }

        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;
        if (prototype.getOptionalHavingNode() == null) {
            if (prototype.isSelectRStream()) {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, oldData, false, isSynthesize, exprEvaluatorContext);
                } else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsNoHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, oldData, false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, newData, true, isSynthesize, exprEvaluatorContext);
            } else {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, newData, true, isSynthesize, exprEvaluatorContext);
            }
        } else {
            if (prototype.isSelectRStream()) {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, oldData, prototype.getOptionalHavingNode(), false, isSynthesize, exprEvaluatorContext);
                } else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, oldData, prototype.getOptionalHavingNode(), false, isSynthesize, exprEvaluatorContext);
                }
            }
            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingNode(), true, isSynthesize, exprEvaluatorContext);
            } else {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, newData, prototype.getOptionalHavingNode(), true, isSynthesize, exprEvaluatorContext);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessSimple(selectNewEvents, selectOldEvents);
        }
        return new UniformPair<>(selectNewEvents, selectOldEvents);  // we return a pair even if both are null
    }

    public static void processViewResultCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", constantNull())
                .declareVarNoInit(EventBean[].class, "selectNewEvents");
        ResultSetProcessorUtil.processViewResultCodegen(method, classScope, instance, forge.getOptionalHavingNode() != null, forge.isSelectRStream(), forge.isSorting(), false);
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        if (orderByProcessor == null) {
            // Return an iterator that gives row-by-row a result
            return new TransformEventIterator(parent.iterator(), new ResultSetProcessorHandtruTransform(this));
        }

        // Pull all events, generate order keys
        EventBean[] eventsPerStream = new EventBean[1];
        List<EventBean> events = new ArrayList<>();
        List<Object> orderKeys = new ArrayList<>();
        Iterator parentIterator = parent.iterator();
        if (parentIterator == null) {
            return CollectionUtil.NULL_EVENT_ITERATOR;
        }
        for (EventBean aParent : parent) {
            eventsPerStream[0] = aParent;
            Object orderKey = orderByProcessor.getSortKey(eventsPerStream, true, exprEvaluatorContext);

            EventBean[] result;
            if (prototype.getOptionalHavingNode() == null) {
                // ignore orderByProcessor
                result = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, eventsPerStream, true, true, exprEvaluatorContext);
            } else {
                // ignore orderByProcessor
                result = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, eventsPerStream, prototype.getOptionalHavingNode(), true, true, exprEvaluatorContext);
            }
            if (result != null && result.length != 0) {
                events.add(result[0]);
                orderKeys.add(orderKey);
            }
        }

        // sort
        EventBean[] outgoingEvents = CollectionUtil.toArrayEvents(events);
        Object[] orderKeysArr = CollectionUtil.toArrayObjects(orderKeys);
        EventBean[] orderedEvents = orderByProcessor.sortWOrderKeys(outgoingEvents, orderKeysArr, exprEvaluatorContext);

        return new ArrayEventIterator(orderedEvents);
    }

    public static void getIteratorViewCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (!forge.isSorting()) {
            // Return an iterator that gives row-by-row a result
            method.getBlock().methodReturn(newInstance(TransformEventIterator.class, exprDotMethod(REF_VIEWABLE, "iterator"), newInstance(ResultSetProcessorHandtruTransform.class, ref("this"))));
            return;
        }

        // Pull all events, generate order keys
        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(List.class, "events", newInstance(ArrayList.class))
                .declareVar(List.class, "orderKeys", newInstance(ArrayList.class))
                .declareVar(Iterator.class, "parentIterator", exprDotMethod(REF_VIEWABLE, "iterator"))
                .ifCondition(equalsNull(ref("parentIterator"))).blockReturn(publicConstValue(CollectionUtil.class, "NULL_EVENT_ITERATOR"));

        {
            CodegenBlock loop = method.getBlock().forEach(EventBean.class, "aParent", REF_VIEWABLE);
            loop.assignArrayElement("eventsPerStream", constant(0), ref("aParent"))
                    .declareVar(Object.class, "orderKey", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), REF_AGENTINSTANCECONTEXT));

            if (forge.getOptionalHavingNode() == null) {
                loop.declareVar(EventBean[].class, "result", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTEVENTSNOHAVING, REF_SELECTEXPRPROCESSOR, ref("eventsPerStream"), constantTrue(), constantTrue(), REF_AGENTINSTANCECONTEXT));
            } else {
                CodegenMethodNode select = ResultSetProcessorUtil.getSelectEventsHavingCodegen(classScope, instance);
                loop.declareVar(EventBean[].class, "result", localMethod(select, REF_SELECTEXPRNONMEMBER, ref("eventsPerStream"), constantTrue(), constantTrue(), REF_AGENTINSTANCECONTEXT));
            }

            loop.ifCondition(and(notEqualsNull(ref("result")), not(equalsIdentity(arrayLength(ref("result")), constant(0)))))
                    .exprDotMethod(ref("events"), "add", arrayAtIndex(ref("result"), constant(0)))
                    .exprDotMethod(ref("orderKeys"), "add", ref("orderKey"));
        }

        method.getBlock().declareVar(EventBean[].class, "outgoingEvents", staticMethod(CollectionUtil.class, METHOD_TOARRAYEVENTS, ref("events")))
                .declareVar(Object[].class, "orderKeysArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYOBJECTS, ref("orderKeys")))
                .declareVar(EventBean[].class, "orderedEvents", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("outgoingEvents"), ref("orderKeysArr"), REF_AGENTINSTANCECONTEXT))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("orderedEvents")));
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        // Process join results set as a regular join, includes sorting and having-clause filter
        UniformPair<EventBean[]> result = processJoinResult(joinSet, Collections.emptySet(), true);
        if (result == null) {
            return Collections.emptyIterator();
        }
        return new ArrayEventIterator(result.getFirst());
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(UniformPair.class, "result", exprDotMethod(ref("this"), "processJoinResult", REF_JOINSET, staticMethod(Collections.class, "emptySet"), constantTrue()))
                .ifRefNull("result")
                .blockReturn(staticMethod(Collections.class, "emptyIterator"))
                .methodReturn(newInstance(ArrayEventIterator.class, cast(EventBean[].class, exprDotMethod(ref("result"), "getFirst"))));
    }

    public void clear() {
        // No need to clear state, there is no state held
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (prototype.isOutputAll()) {
            outputAllHelper.processView(newData, oldData);
        } else {
            outputLastHelper.processView(newData, oldData);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorSimpleForge forge, String methodName, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMember factory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());

        if (forge.isOutputAll()) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorSimpleOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSSimpleOutputAll", ref("this"), REF_AGENTINSTANCECONTEXT, constant(forge.getNumStreams())));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorSimpleOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSSimpleOutputLast", ref("this"), REF_AGENTINSTANCECONTEXT, constant(forge.getNumStreams())));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA);
        }
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        if (prototype.isOutputAll()) {
            outputAllHelper.processJoin(newEvents, oldEvents);
        } else {
            outputLastHelper.processJoin(newEvents, oldEvents);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize) {
        if (prototype.isOutputAll()) {
            return outputAllHelper.outputView(isSynthesize);
        }
        return outputLastHelper.outputView(isSynthesize);
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTALLHELPER), "outputView", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "outputView", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize) {
        if (prototype.isOutputAll()) {
            return outputAllHelper.outputJoin(isSynthesize);
        }
        return outputLastHelper.outputJoin(isSynthesize);
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public void stop() {
        if (outputLastHelper != null) {
            outputLastHelper.destroy();
        }
        if (outputAllHelper != null) {
            outputAllHelper.destroy();
        }
    }

    public static void stopMethodCodegen(CodegenMethodNode method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), "destroy");
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

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        if (!prototype.isOutputLast()) {
            UniformPair<Set<MultiKey<EventBean>>> flattened = EventBeanUtility.flattenBatchJoin(joinEventsSet);
            return processJoinResult(flattened.getFirst(), flattened.getSecond(), generateSynthetic);
        }

        throw new IllegalStateException("Output last is provided by " + OutputProcessViewConditionLastAllUnord.class.getSimpleName());
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorSimpleForge forge, CodegenMethodNode method) {
        if (!forge.isOutputLast()) {
            method.getBlock().declareVar(UniformPair.class, "pair", staticMethod(EventBeanUtility.class, METHOD_FLATTENBATCHJOIN, REF_JOINEVENTSSET))
                    .methodReturn(exprDotMethod(ref("this"), "processJoinResult",
                            cast(Set.class, exprDotMethod(ref("pair"), "getFirst")), cast(Set.class, exprDotMethod(ref("pair"), "getSecond")), REF_ISSYNTHESIZE));
            return;
        }
        method.getBlock().methodThrowUnsupported();
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        if (!prototype.isOutputLast()) {
            UniformPair<EventBean[]> pair = EventBeanUtility.flattenBatchStream(viewEventsList);
            return processViewResult(pair.getFirst(), pair.getSecond(), generateSynthetic);
        }

        throw new IllegalStateException("Output last is provided by " + OutputProcessViewConditionLastAllUnord.class.getSimpleName());
    }

    public static void processOutputLimitedViewCodegen(ResultSetProcessorSimpleForge forge, CodegenMethodNode method) {
        if (!forge.isOutputLast()) {
            method.getBlock().declareVar(UniformPair.class, "pair", staticMethod(EventBeanUtility.class, METHOD_FLATTENBATCHSTREAM, REF_VIEWEVENTSLIST))
                    .methodReturn(exprDotMethod(ref("this"), "processViewResult",
                            cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")), cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")), REF_ISSYNTHESIZE));
            return;
        }
        method.getBlock().methodThrowUnsupported();
    }

    public boolean hasHavingClause() {
        return prototype.getOptionalHavingNode() != null;
    }

    public boolean evaluateHavingClause(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public ExprEvaluatorContext getAgentInstanceContext() {
        return exprEvaluatorContext;
    }
}
