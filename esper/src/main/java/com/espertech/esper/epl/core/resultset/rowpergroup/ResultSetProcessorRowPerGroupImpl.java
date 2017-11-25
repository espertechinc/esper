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
package com.espertech.esper.epl.core.resultset.rowpergroup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenInstanceAux;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputHelperVisitor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedOutputAllGroupReps;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.view.OutputConditionPolled;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.Viewable;

import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil.*;
import static com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedUtil.*;
import static com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames.REF_EPS;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.NAME_EPS;

/**
 * Result set processor for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 * <p>
 * Produces one row for each group that changed (and not one row per event). Computes MultiKey group-by keys for
 * each event and uses a set of the group-by keys to generate the result rows, using the first (old or new, anyone) event
 * for each distinct group-by key.
 */
public class ResultSetProcessorRowPerGroupImpl implements ResultSetProcessorRowPerGroup {
    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";
    private final static String NAME_OUTPUTFIRSTHELPER = "outputFirstHelper";
    private final static String NAME_OUTPUTALLGROUPREPS = "outputAllGroupReps";

    protected final ResultSetProcessorRowPerGroupFactory prototype;
    protected final SelectExprProcessor selectExprProcessor;
    protected final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected AgentInstanceContext agentInstanceContext;

    // For output rate limiting, keep a representative event for each group for
    // representing each group in an output limit clause
    private ResultSetProcessorGroupedOutputAllGroupReps outputAllGroupReps;

    private ResultSetProcessorGroupedOutputFirstHelper outputFirstHelper;
    private ResultSetProcessorRowPerGroupOutputLastHelper outputLastHelper;
    private ResultSetProcessorRowPerGroupOutputAllHelper outputAllHelper;

    ResultSetProcessorRowPerGroupImpl(ResultSetProcessorRowPerGroupFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.agentInstanceContext = agentInstanceContext;

        aggregationService.setRemovedCallback(this);

        if (prototype.isOutputLast()) {
            outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupOutputLastOpt(agentInstanceContext, this, prototype.getGroupKeyTypes(), prototype.getNumStreams());
        } else if (prototype.isOutputAll()) {
            if (prototype.getOutputConditionType() != ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED) {
                outputAllGroupReps = prototype.getResultSetProcessorHelperFactory().makeRSGroupedOutputAllNoOpt(agentInstanceContext, prototype.getGroupKeyTypes(), prototype.getNumStreams());
            } else {
                outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupOutputAllOpt(agentInstanceContext, this, prototype.getGroupKeyTypes(), prototype.getNumStreams());
            }
        } else if (prototype.isOutputFirst()) {
            outputFirstHelper = prototype.getResultSetProcessorHelperFactory().makeRSGroupedOutputFirst(agentInstanceContext, prototype.getGroupKeyTypes(), prototype.getOptionalOutputFirstConditionFactory(), null, -1);
        }
    }

    public void setAgentInstanceContext(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            // apply new data to aggregates
            for (EventBean aNewData : newData) {
                eventsPerStream[0] = aNewData;
                Object mk = generateGroupKeySingle(eventsPerStream, true);
                aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            for (EventBean anOldData : oldData) {
                eventsPerStream[0] = anOldData;
                Object mk = generateGroupKeySingle(eventsPerStream, false);
                aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
            }
        }
    }

    public static void applyViewResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .ifCondition(notEqualsNull(REF_NEWDATA))
                .forEach(EventBean.class, "aNewData", REF_NEWDATA)
                .assignArrayElement("eventsPerStream", constant(0), ref("aNewData"))
                .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()))
                .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                .blockEnd()
                .blockEnd()
                .ifCondition(notEqualsNull(REF_OLDDATA))
                .forEach(EventBean.class, "anOldData", REF_OLDDATA)
                .assignArrayElement("eventsPerStream", constant(0), ref("anOldData"))
                .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()))
                .exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                .blockEnd()
                .blockEnd();
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        if (!newEvents.isEmpty()) {
            // apply old data to aggregates
            for (MultiKey<EventBean> aNewEvent : newEvents) {
                EventBean[] eventsPerStream = aNewEvent.getArray();
                Object mk = generateGroupKeySingle(eventsPerStream, true);
                aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
            }
        }
        if (oldEvents != null && !oldEvents.isEmpty()) {
            // apply old data to aggregates
            for (MultiKey<EventBean> anOldEvent : oldEvents) {
                EventBean[] eventsPerStream = anOldEvent.getArray();
                Object mk = generateGroupKeySingle(eventsPerStream, false);
                aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
            }
        }
    }

    public static void applyJoinResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        method.getBlock()
                .ifCondition(not(exprDotMethod(REF_NEWDATA, "isEmpty")))
                .forEach(MultiKey.class, "aNewEvent", REF_NEWDATA)
                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewEvent"), "getArray")))
                .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()))
                .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                .blockEnd()
                .blockEnd()
                .ifCondition(and(notEqualsNull(REF_OLDDATA), not(exprDotMethod(REF_OLDDATA, "isEmpty"))))
                .forEach(MultiKey.class, "anOldEvent", REF_OLDDATA)
                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldEvent"), "getArray")))
                .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()))
                .exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                .blockEnd()
                .blockEnd();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();
        }
        // Generate group-by keys for all events, collect all keys in a set for later event generation
        Map<Object, EventBean[]> keysAndEvents = new HashMap<>();
        Object[] newDataMultiKey = generateGroupKeyArrayJoinTakingMap(newEvents, keysAndEvents, true);
        Object[] oldDataMultiKey = generateGroupKeyArrayJoinTakingMap(oldEvents, keysAndEvents, false);

        if (prototype.isUnidirectional()) {
            this.clear();
        }

        // generate old events
        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsJoin(keysAndEvents, false, isSynthesize);
        }

        ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newEvents, newDataMultiKey, oldEvents, oldDataMultiKey);

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsJoin(keysAndEvents, true, isSynthesize);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);
        }
        return ResultSetProcessorUtil.toPairNullIfAllNull(selectNewEvents, selectOldEvents);
    }

    public static void processJoinResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinTakingMapCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        method.getBlock().declareVar(Map.class, "keysAndEvents", newInstance(HashMap.class))
                .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, REF_NEWDATA, ref("keysAndEvents"), constantTrue()))
                .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, REF_OLDDATA, ref("keysAndEvents"), constantFalse()));

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }

        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();
        }

        if (newData != null && newData.length == 1) {
            if (oldData == null || oldData.length == 0) {
                UniformPair<EventBean[]> pair = processViewResultNewDepthOne(newData, isSynthesize);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(pair);
                }
                return pair;
            }
            if (oldData.length == 1 && !prototype.isSelectRStream()) {
                UniformPair<EventBean[]> pair = processViewResultPairDepthOneNoRStream(newData, oldData, isSynthesize);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(pair);
                }
                return pair;
            }
        }

        Map<Object, EventBean> keysAndEvents = new HashMap<>();
        EventBean[] eventsPerStream = new EventBean[1];

        Object[] newDataMultiKey = generateGroupKeysKeepEvent(newData, keysAndEvents, true, eventsPerStream);
        Object[] oldDataMultiKey = generateGroupKeysKeepEvent(oldData, keysAndEvents, false, eventsPerStream);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsView(keysAndEvents, false, isSynthesize, eventsPerStream);
        }

        ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey, eventsPerStream);

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsView(keysAndEvents, true, isSynthesize, eventsPerStream);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);
        }

        return ResultSetProcessorUtil.toPairNullIfAllNull(selectNewEvents, selectOldEvents);
    }

    public static void processViewResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeysKeepEvent = generateGroupKeysKeepEventCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);
        CodegenMethodNode processViewResultNewDepthOne = processViewResultNewDepthOneCodegen(forge, classScope, instance);
        CodegenMethodNode processViewResultPairDepthOneNoRStream = processViewResultPairDepthOneNoRStreamCodegen(forge, classScope, instance);

        CodegenBlock ifShortcut = method.getBlock().ifCondition(and(notEqualsNull(REF_NEWDATA), equalsIdentity(arrayLength(REF_NEWDATA), constant(1))));
        ifShortcut.ifCondition(or(equalsNull(REF_OLDDATA), equalsIdentity(arrayLength(REF_OLDDATA), constant(0))))
                .blockReturn(localMethod(processViewResultNewDepthOne, REF_NEWDATA, REF_ISSYNTHESIZE));
        if (!forge.isSelectRStream()) {
            ifShortcut.ifCondition(equalsIdentity(arrayLength(REF_OLDDATA), constant(1)))
                    .blockReturn(localMethod(processViewResultPairDepthOneNoRStream, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE));
        }
        method.getBlock().declareVar(Map.class, "keysAndEvents", newInstance(HashMap.class))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeysKeepEvent, REF_NEWDATA, ref("keysAndEvents"), constantTrue(), ref("eventsPerStream")))
                .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeysKeepEvent, REF_OLDDATA, ref("keysAndEvents"), constantFalse(), ref("eventsPerStream")))
                .declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, ref("keysAndEvents"), constantFalse(), REF_ISSYNTHESIZE, ref("eventsPerStream")) : constantNull())
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"), ref("eventsPerStream"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsView, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE, ref("eventsPerStream")))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    EventBean[] generateOutputEventsView(Map<Object, EventBean> keysAndEvents, boolean isNewData, boolean isSynthesize, EventBean[] eventsPerStream) {
        EventBean[] events = new EventBean[keysAndEvents.size()];
        Object[] keys = new Object[keysAndEvents.size()];

        EventBean[][] currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new EventBean[keysAndEvents.size()][];
        }

        int count = 0;
        int cpid = agentInstanceContext.getAgentInstanceId();

        for (Map.Entry<Object, EventBean> entry : keysAndEvents.entrySet()) {
            // Set the current row of aggregation states
            aggregationService.setCurrentAccess(entry.getKey(), cpid, null);

            eventsPerStream[0] = entry.getValue();

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }

            events[count] = selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            keys[count] = entry.getKey();
            if (prototype.isSorting()) {
                currentGenerators[count] = new EventBean[]{entry.getValue()};
            }

            count++;
        }

        return ResultSetProcessorUtil.outputFromCountMaySort(count, events, keys, currentGenerators, isNewData, orderByProcessor, agentInstanceContext, aggregationService);
    }

    static CodegenMethodNode generateOutputEventsViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, exprDotMethod(ref("keysAndEvents"), "size")))
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("keysAndEvents"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean[][].class, "currentGenerators", newArrayByLength(EventBean[].class, exprDotMethod(ref("keysAndEvents"), "size")));
            }

            methodNode.getBlock().declareVar(int.class, "count", constant(0))
                    .declareVar(int.class, "cpid", exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forEach.exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), ref("cpid"), constantNull())
                        .assignArrayElement(REF_EPS, constant(0), cast(EventBean.class, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forEach.assignArrayElement("events", ref("count"), exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT))
                        .assignArrayElement("keys", ref("count"), exprDotMethod(ref("entry"), "getKey"));

                if (forge.isSorting()) {
                    forEach.assignArrayElement("currentGenerators", ref("count"), newArrayWithInit(EventBean.class, cast(EventBean.class, exprDotMethod(ref("entry"), "getValue"))));
                }

                forEach.increment("count")
                        .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("count"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsView",
                CodegenNamedParam.from(Map.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, EventBean[].class, NAME_EPS),
                ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private void generateOutputBatchedRowFromMap(Map<Object, EventBean> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys, AgentInstanceContext agentInstanceContext) {
        EventBean[] eventsPerStream = new EventBean[1];

        for (Map.Entry<Object, EventBean> entry : keysAndEvents.entrySet()) {
            // Set the current row of aggregation states
            aggregationService.setCurrentAccess(entry.getKey(), agentInstanceContext.getAgentInstanceId(), null);

            eventsPerStream[0] = entry.getValue();

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }

            resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

            if (prototype.isSorting()) {
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }
        }
    }

    static CodegenMethodNode generateOutputBatchedRowFromMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        Consumer<CodegenMethodNode> code = method -> {
            method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
            {
                CodegenBlock forLoop = method.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forLoop.exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .assignArrayElement("eventsPerStream", constant(0), cast(EventBean.class, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forLoop.exprDotMethod(ref("resultEvents"), "add", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forLoop.exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
                }
            }
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedRowFromMap",
                CodegenNamedParam.from(Map.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys", AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public void generateOutputBatchedArrFromIterator(boolean join, Iterator<Map.Entry<Object, EventBean[]>> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys) {
        while (keysAndEvents.hasNext()) {
            Map.Entry<Object, EventBean[]> entry = keysAndEvents.next();
            generateOutputBatchedRowAddToList(join, entry.getKey(), entry.getValue(), isNewData, isSynthesize, resultEvents, optSortKeys);
        }
    }

    static CodegenMethodNode generateOutputBatchedArrFromIteratorCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);

        Consumer<CodegenMethodNode> code = method -> method.getBlock().whileLoop(exprDotMethod(ref("keysAndEvents"), "hasNext"))
                .declareVar(Map.Entry.class, "entry", cast(Map.Entry.class, exprDotMethod(ref("keysAndEvents"), "next")))
                .localMethod(generateOutputBatchedRowAddToList, ref("join"), exprDotMethod(ref("entry"), "getKey"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("resultEvents"), ref("optSortKeys"));

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedArrFromIterator",
                CodegenNamedParam.from(boolean.class, "join", Iterator.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private void generateOutputBatchedRowAddToList(boolean join, Object mk, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys) {
        // Set the current row of aggregation states
        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

        // Filter the having clause
        if (prototype.getOptionalHavingNode() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
            if (!passesHaving) {
                return;
            }
        }

        resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

        if (prototype.isSorting()) {
            optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
        }
    }

    private static CodegenMethodNode generateOutputBatchedRowAddToListCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = method -> {
            method.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                method.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT)))
                        .blockReturnNoValue();
            }

            method.getBlock().exprDotMethod(ref("resultEvents"), "add", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

            if (forge.isSorting()) {
                method.getBlock().exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
            }
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedRowAddToList",
                CodegenNamedParam.from(boolean.class, "join", Object.class, "mk", EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public EventBean generateOutputBatchedNoSortWMap(boolean join, Object mk, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize) {
        // Set the current row of aggregation states
        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

        // Filter the having clause
        if (prototype.getOptionalHavingNode() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
            if (!passesHaving) {
                return null;
            }
        }

        return selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
    }

    static CodegenMethodNode generateOutputBatchedNoSortWMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT)))
                        .blockReturn(constantNull());
            }
            methodNode.getBlock().methodReturn(exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));
        };

        return instance.getMethods().addMethod(EventBean.class, "generateOutputBatchedNoSortWMap",
                CodegenNamedParam.from(boolean.class, "join", Object.class, "mk", EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE),
                ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }


    private EventBean[] generateOutputEventsJoin(Map<Object, EventBean[]> keysAndEvents, boolean isNewData, boolean isSynthesize) {
        EventBean[] events = new EventBean[keysAndEvents.size()];
        Object[] keys = new Object[keysAndEvents.size()];
        EventBean[][] currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new EventBean[keysAndEvents.size()][];
        }

        int count = 0;
        int cpid = agentInstanceContext.getAgentInstanceId();
        for (Map.Entry<Object, EventBean[]> entry : keysAndEvents.entrySet()) {
            aggregationService.setCurrentAccess(entry.getKey(), cpid, null);
            EventBean[] eventsPerStream = entry.getValue();

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }

            events[count] = selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            keys[count] = entry.getKey();
            if (prototype.isSorting()) {
                currentGenerators[count] = eventsPerStream;
            }

            count++;
        }

        return ResultSetProcessorUtil.outputFromCountMaySort(count, events, keys, currentGenerators, isNewData, orderByProcessor, agentInstanceContext, aggregationService);
    }

    private static CodegenMethodNode generateOutputEventsJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, exprDotMethod(ref("keysAndEvents"), "size")))
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("keysAndEvents"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean[][].class, "currentGenerators", newArrayByLength(EventBean[].class, exprDotMethod(ref("keysAndEvents"), "size")));
            }

            methodNode.getBlock().declareVar(int.class, "count", constant(0))
                    .declareVar(int.class, "cpid", exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forEach.exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), ref("cpid"), constantNull())
                        .declareVar(EventBean[].class, NAME_EPS, cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forEach.assignArrayElement("events", ref("count"), exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT))
                        .assignArrayElement("keys", ref("count"), exprDotMethod(ref("entry"), "getKey"));

                if (forge.isSorting()) {
                    forEach.assignArrayElement("currentGenerators", ref("count"), ref("eventsPerStream"));
                }

                forEach.increment("count")
                        .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("count"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsJoin",
                CodegenNamedParam.from(Map.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE),
                ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private Object[] generateGroupKeyArrayView(EventBean[] events, boolean isNewData) {
        if (events == null) {
            return null;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        Object[] keys = new Object[events.length];

        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];
            keys[i] = generateGroupKeySingle(eventsPerStream, isNewData);
        }

        return keys;
    }

    Object[] generateGroupKeysKeepEvent(EventBean[] events, Map<Object, EventBean> eventPerKey, boolean isNewData, EventBean[] eventsPerStream) {
        if (events == null) {
            return null;
        }

        Object[] keys = new Object[events.length];

        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];
            keys[i] = generateGroupKeySingle(eventsPerStream, isNewData);
            eventPerKey.put(keys[i], events[i]);
        }

        return keys;
    }

    static CodegenMethodNode generateGroupKeysKeepEventCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode key = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifRefNullReturnNull("events")
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, arrayLength(ref("events"))));
            {
                methodNode.getBlock().forLoopIntSimple("i", arrayLength(ref("events")))
                        .assignArrayElement(REF_EPS, constant(0), arrayAtIndex(ref("events"), ref("i")))
                        .assignArrayElement("keys", ref("i"), localMethod(key, REF_EPS, REF_ISNEWDATA))
                        .exprDotMethod(ref("eventPerKey"), "put", arrayAtIndex(ref("keys"), ref("i")), arrayAtIndex(ref("events"), ref("i")))
                        .blockEnd();
            }
            methodNode.getBlock().methodReturn(ref("keys"));
        };

        return instance.getMethods().addMethod(Object[].class, "generateGroupKeysKeepEvent",
                CodegenNamedParam.from(EventBean[].class, "events", Map.class, "eventPerKey", boolean.class, NAME_ISNEWDATA, EventBean[].class, NAME_EPS), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private Object[] generateGroupKeyArrayJoinTakingMap(Set<MultiKey<EventBean>> resultSet, Map<Object, EventBean[]> eventPerKey, boolean isNewData) {
        if (resultSet == null || resultSet.isEmpty()) {
            return null;
        }

        Object[] keys = new Object[resultSet.size()];

        int count = 0;
        for (MultiKey<EventBean> eventsPerStream : resultSet) {
            keys[count] = generateGroupKeySingle(eventsPerStream.getArray(), isNewData);
            eventPerKey.put(keys[count], eventsPerStream.getArray());

            count++;
        }

        return keys;
    }

    private static CodegenMethodNode generateGroupKeyArrayJoinTakingMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode key = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifCondition(or(equalsNull(ref("resultSet")), exprDotMethod(ref("resultSet"), "isEmpty"))).blockReturn(constantNull())
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("resultSet"), "size")))
                    .declareVar(int.class, "count", constant(0));
            {
                methodNode.getBlock().forEach(MultiKey.class, "eventsPerStream", ref("resultSet"))
                        .declareVar(EventBean[].class, "eps", cast(EventBean[].class, exprDotMethod(ref("eventsPerStream"), "getArray")))
                        .assignArrayElement("keys", ref("count"), localMethod(key, ref("eps"), REF_ISNEWDATA))
                        .exprDotMethod(ref("eventPerKey"), "put", arrayAtIndex(ref("keys"), ref("count")), ref("eps"))
                        .increment("count")
                        .blockEnd();
            }
            methodNode.getBlock().methodReturn(ref("keys"));
        };

        return instance.getMethods().addMethod(Object[].class, "generateGroupKeyArrayJoinTakingMapCodegen",
                CodegenNamedParam.from(Set.class, "resultSet", Map.class, "eventPerKey", boolean.class, NAME_ISNEWDATA), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    /**
     * Returns the optional having expression.
     *
     * @return having expression node
     */
    public ExprEvaluator getOptionalHavingNode() {
        return prototype.getOptionalHavingNode();
    }

    /**
     * Returns the select expression processor
     *
     * @return select processor.
     */
    public SelectExprProcessor getSelectExprProcessor() {
        return selectExprProcessor;
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        if (!prototype.isHistoricalOnly()) {
            return obtainIterator(parent);
        }

        aggregationService.clearResults(agentInstanceContext);
        Iterator<EventBean> it = parent.iterator();
        EventBean[] eventsPerStream = new EventBean[1];
        while (it.hasNext()) {
            eventsPerStream[0] = it.next();
            Object groupKey = generateGroupKeySingle(eventsPerStream, true);
            aggregationService.applyEnter(eventsPerStream, groupKey, agentInstanceContext);
        }

        ArrayDeque<EventBean> deque = ResultSetProcessorUtil.iteratorToDeque(obtainIterator(parent));
        aggregationService.clearResults(agentInstanceContext);
        return deque.iterator();
    }

    public static void getIteratorViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE));
            return;
        }

        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        method.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT)
                .declareVar(Iterator.class, "it", exprDotMethod(REF_VIEWABLE, "iterator"))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            method.getBlock().whileLoop(exprDotMethod(ref("it"), "hasNext"))
                    .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.class, exprDotMethod(ref("it"), "next")))
                    .declareVar(Object.class, "groupKey", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()))
                    .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKey"), REF_AGENTINSTANCECONTEXT)
                    .blockEnd();
        }

        method.getBlock().declareVar(ArrayDeque.class, "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE)))
                .exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    Iterator<EventBean> obtainIterator(Viewable parent) {
        if (orderByProcessor == null) {
            return new ResultSetProcessorRowPerGroupIterator(parent.iterator(), this, aggregationService, agentInstanceContext);
        }
        return getIteratorSorted(parent.iterator());
    }

    private static CodegenMethodNode obtainIteratorCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode parent, CodegenInstanceAux instance) {
        CodegenMethodNode iterator = parent.makeChild(Iterator.class, ResultSetProcessorRowPerGroupImpl.class, classScope).addParam(Viewable.class, NAME_VIEWABLE);
        if (!forge.isSorting()) {
            iterator.getBlock().methodReturn(newInstance(ResultSetProcessorRowPerGroupIterator.class, exprDotMethod(REF_VIEWABLE, "iterator"), ref("this"), REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT));
            return iterator;
        }

        CodegenMethodNode getIteratorSorted = getIteratorSortedCodegen(forge, classScope, instance);
        iterator.getBlock().methodReturn(localMethod(getIteratorSorted, exprDotMethod(REF_VIEWABLE, "iterator")));
        return iterator;
    }

    Iterator<EventBean> getIteratorSorted(Iterator<EventBean> parentIter) {

        // Pull all parent events, generate order keys
        EventBean[] eventsPerStream = new EventBean[1];
        List<EventBean> outgoingEvents = new ArrayList<>();
        List<Object> orderKeys = new ArrayList<>();
        Set<Object> priorSeenGroups = new HashSet<>();

        while (parentIter.hasNext()) {
            EventBean candidate = parentIter.next();
            eventsPerStream[0] = candidate;

            Object groupKey = generateGroupKeySingle(eventsPerStream, true);
            aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);

            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, true, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }
            if (priorSeenGroups.contains(groupKey)) {
                continue;
            }
            priorSeenGroups.add(groupKey);

            outgoingEvents.add(selectExprProcessor.process(eventsPerStream, true, true, agentInstanceContext));

            Object orderKey = orderByProcessor.getSortKey(eventsPerStream, true, agentInstanceContext);
            orderKeys.add(orderKey);
        }

        return ResultSetProcessorUtil.orderOutgoingGetIterator(outgoingEvents, orderKeys, orderByProcessor, agentInstanceContext);
    }

    static CodegenMethodNode getIteratorSortedCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        CodegenMethodNode generateGroupKeyViewSingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        Consumer<CodegenMethodNode> code = method -> {
            method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                    .declareVar(ArrayList.class, "outgoingEvents", newInstance(ArrayList.class))
                    .declareVar(ArrayList.class, "orderKeys", newInstance(ArrayList.class))
                    .declareVar(Set.class, "priorSeenGroups", newInstance(HashSet.class));

            {
                CodegenBlock whileLoop = method.getBlock().whileLoop(exprDotMethod(ref("parentIter"), "hasNext"));
                whileLoop.declareVar(EventBean.class, "candidate", cast(EventBean.class, exprDotMethod(ref("parentIter"), "next")))
                        .assignArrayElement("eventsPerStream", constant(0), ref("candidate"))
                        .declareVar(Object.class, "groupKey", localMethod(generateGroupKeyViewSingle, ref("eventsPerStream"), constantTrue()))
                        .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

                if (forge.getOptionalHavingNode() != null) {
                    whileLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, constantTrue(), REF_AGENTINSTANCECONTEXT))).blockContinue();
                }
                whileLoop.ifCondition(exprDotMethod(ref("priorSeenGroups"), "contains", ref("groupKey"))).blockContinue();

                whileLoop.exprDotMethod(ref("priorSeenGroups"), "add", ref("groupKey"))
                        .exprDotMethod(ref("outgoingEvents"), "add", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), constantTrue(), constantTrue(), REF_AGENTINSTANCECONTEXT))
                        .declareVar(Object.class, "orderKey", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), REF_AGENTINSTANCECONTEXT))
                        .exprDotMethod(ref("orderKeys"), "add", ref("orderKey"));
            }

            method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_ORDEROUTGOINGGETITERATOR, ref("outgoingEvents"), ref("orderKeys"), REF_ORDERBYPROCESSOR, REF_AGENTINSTANCECONTEXT));
        };
        return instance.getMethods().addMethod(Iterator.class, "getIteratorSorted", CodegenNamedParam.from(Iterator.class, "parentIter"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        Map<Object, EventBean[]> keysAndEvents = new HashMap<>();
        generateGroupKeyArrayJoinTakingMap(joinSet, keysAndEvents, true);
        EventBean[] selectNewEvents = generateOutputEventsJoin(keysAndEvents, true, true);
        return new ArrayEventIterator(selectNewEvents);
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinTakingMapCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);
        method.getBlock()
                .declareVar(Map.class, "keysAndEvents", newInstance(HashMap.class))
                .expression(localMethod(generateGroupKeyArrayJoin, REF_JOINSET, ref("keysAndEvents"), constantTrue()))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("selectNewEvents")));
    }

    public void clear() {
        aggregationService.clearResults(agentInstanceContext);
    }

    public static void clearMethodCodegen(CodegenMethodNode method) {
        method.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT);
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        OutputLimitLimitType outputLimitLimitType = prototype.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return processOutputLimitedJoinDefault(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return processOutputLimitedJoinAll(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return processOutputLimitedJoinFirst(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            return processOutputLimitedJoinLast(joinEventsSet, generateSynthetic);
        }
        throw new IllegalStateException("Unrecognized output limit type " + outputLimitLimitType);
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        OutputLimitLimitType outputLimitLimitType = forge.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            processOutputLimitedJoinDefaultCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            processOutputLimitedJoinAllCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            processOutputLimitedJoinFirstCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            processOutputLimitedJoinLastCodegen(forge, classScope, method, instance);
            return;
        }
        throw new IllegalStateException("Unrecognized output limit type " + outputLimitLimitType);
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        OutputLimitLimitType outputLimitLimitType = prototype.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return processOutputLimitedViewDefault(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return processOutputLimitedViewAll(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return processOutputLimitedViewFirst(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            return processOutputLimitedViewLast(viewEventsList, generateSynthetic);
        }
        throw new IllegalStateException("Unrecognized output limit type " + outputLimitLimitType);
    }

    public static void processOutputLimitedViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        OutputLimitLimitType outputLimitLimitType = forge.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            processOutputLimitedViewDefaultCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            processOutputLimitedViewAllCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            processOutputLimitedViewFirstCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            processOutputLimitedViewLastCodegen(forge, classScope, method, instance);
            return;
        }
        throw new IllegalStateException("Unrecognized output limit type " + outputLimitLimitType);
    }

    public void removedAggregationGroupKey(Object key) {
        if (outputAllGroupReps != null) {
            outputAllGroupReps.remove(key);
        }
        if (outputLastHelper != null) {
            outputLastHelper.remove(key);
        }
        if (outputFirstHelper != null) {
            outputFirstHelper.remove(key);
        }
    }

    static void removedAggregationGroupKeyCodegen(CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = method -> {
            if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
                method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "remove", ref("key"));
            }
            if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
                method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "remove", ref("key"));
            }
            if (instance.hasMember(NAME_OUTPUTFIRSTHELPER)) {
                method.getBlock().exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "remove", ref("key"));
            }
        };

        instance.getMethods().addMethod(void.class, "removedAggregationGroupKey", CodegenNamedParam.from(Object.class, "key"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public Object generateGroupKeySingle(EventBean[] eventsPerStream, boolean isNewData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessComputeGroupKeys(isNewData, prototype.getGroupKeyNodeExpressions(), eventsPerStream);
            Object keyObject;
            if (prototype.getGroupKeyNode() != null) {
                keyObject = prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
            } else {
                ExprEvaluator[] evals = prototype.getGroupKeyNodes();
                Object[] keys = new Object[evals.length];
                for (int i = 0; i < evals.length; i++) {
                    keys[i] = evals[i].evaluate(eventsPerStream, isNewData, agentInstanceContext);
                }
                keyObject = new MultiKeyUntyped(keys);
            }

            InstrumentationHelper.get().aResultSetProcessComputeGroupKeys(isNewData, keyObject);
            return keyObject;
        }

        if (prototype.getGroupKeyNode() != null) {
            return prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
        } else {
            ExprEvaluator[] evals = prototype.getGroupKeyNodes();
            Object[] keys = new Object[evals.length];
            for (int i = 0; i < evals.length; i++) {
                keys[i] = evals[i].evaluate(eventsPerStream, isNewData, agentInstanceContext);
            }
            return new MultiKeyUntyped(keys);
        }
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (prototype.isOutputAll()) {
            outputAllHelper.processView(newData, oldData, isGenerateSynthetic);
        } else {
            outputLastHelper.processView(newData, oldData, isGenerateSynthetic);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorRowPerGroupForge forge, String methodName, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMember factory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());

        if (forge.isOutputAll()) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorRowPerGroupOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSRowPerGroupOutputAllOpt", REF_AGENTINSTANCECONTEXT, ref("this"), member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorRowPerGroupOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSRowPerGroupOutputLastOpt", REF_AGENTINSTANCECONTEXT, ref("this"), member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        }
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newData, Set<MultiKey<EventBean>> oldData, boolean isGenerateSynthetic) {
        if (prototype.isOutputAll()) {
            outputAllHelper.processJoin(newData, oldData, isGenerateSynthetic);
        } else {
            outputLastHelper.processJoin(newData, oldData, isGenerateSynthetic);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize) {
        if (prototype.isOutputAll()) {
            return outputAllHelper.outputView(isSynthesize);
        }
        return outputLastHelper.outputView(isSynthesize);
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenMethodNode method) {
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

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenMethodNode method) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public void stop() {
        if (outputAllGroupReps != null) {
            outputAllGroupReps.destroy();
        }
        if (outputAllHelper != null) {
            outputAllHelper.destroy();
        }
        if (outputLastHelper != null) {
            outputLastHelper.destroy();
        }
        if (outputFirstHelper != null) {
            outputFirstHelper.destroy();
        }
    }

    public static void stopMethodCodegenBound(CodegenMethodNode method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "destroy");
        }
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        if (outputAllGroupReps != null) {
            visitor.visit(outputAllGroupReps);
        }
        if (outputFirstHelper != null) {
            visitor.visit(outputFirstHelper);
        }
        if (outputLastHelper != null) {
            visitor.visit(outputLastHelper);
        }
        if (outputAllHelper != null) {
            visitor.visit(outputAllHelper);
        }
    }

    public static void acceptHelperVisitorCodegen(CodegenMethodNode method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTALLGROUPREPS));
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTALLHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTLASTHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTFIRSTHELPER));
        }
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinLast(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
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

        Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<>();
        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            if (newData != null) {
                // apply new data to aggregates
                for (MultiKey<EventBean> aNewData : newData) {
                    EventBean[] eventsPerStream = aNewData.getArray();
                    Object mk = generateGroupKeySingle(eventsPerStream, true);

                    // if this is a newly encountered group, generate the remove stream event
                    if (groupRepsView.put(mk, eventsPerStream) == null) {
                        if (prototype.isSelectRStream()) {
                            generateOutputBatchedRowAddToList(true, mk, eventsPerStream, true, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                for (MultiKey<EventBean> anOldData : oldData) {
                    EventBean[] eventsPerStream = anOldData.getArray();
                    Object mk = generateGroupKeySingle(eventsPerStream, true);

                    if (groupRepsView.put(mk, eventsPerStream) == null) {
                        if (prototype.isSelectRStream()) {
                            generateOutputBatchedRowAddToList(true, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }

                    aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                }
            }
        }

        generateOutputBatchedArrFromIterator(true, groupRepsView.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedJoinLastCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "groupRepsView", newInstance(HashMap.class));
        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNewData.forEach(MultiKey.class, "aNewData", ref("newData"));
                    forNew.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(MultiKey.class, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethodChain(ref("groupRepsView")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinFirst(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
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

        Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<>();
        if (prototype.getOptionalHavingNode() == null) {
            for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
                Set<MultiKey<EventBean>> newData = pair.getFirst();
                Set<MultiKey<EventBean>> oldData = pair.getSecond();

                if (newData != null) {
                    // apply new data to aggregates
                    for (MultiKey<EventBean> aNewData : newData) {
                        Object mk = generateGroupKeySingle(aNewData.getArray(), true);
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            // if this is a newly encountered group, generate the remove stream event
                            if (groupRepsView.put(mk, aNewData.getArray()) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedRowAddToList(true, mk, aNewData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                        aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                    }
                }
                if (oldData != null) {
                    // apply old data to aggregates
                    for (MultiKey<EventBean> anOldData : oldData) {
                        Object mk = generateGroupKeySingle(anOldData.getArray(), true);
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsView.put(mk, anOldData.getArray()) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedRowAddToList(true, mk, anOldData.getArray(), false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }

                        aggregationService.applyLeave(anOldData.getArray(), mk, agentInstanceContext);
                    }
                }
            }
        } else {
            groupRepsView.clear();
            for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
                Set<MultiKey<EventBean>> newData = pair.getFirst();
                Set<MultiKey<EventBean>> oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeyArrayJoin(newData, true);
                Object[] oldDataMultiKey = generateGroupKeyArrayJoin(oldData, false);

                ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey);

                // evaluate having-clause
                if (newData != null) {
                    int count = 0;
                    for (MultiKey<EventBean> aNewData : newData) {
                        Object mk = newDataMultiKey[count];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);
                        EventBean[] eventsPerStream = aNewData.getArray();

                        // Filter the having clause
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, true, agentInstanceContext);
                        if (!passesHaving) {
                            count++;
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsView.put(mk, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedRowAddToList(true, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                        count++;
                    }
                }

                // evaluate having-clause
                if (oldData != null) {
                    int count = 0;
                    for (MultiKey<EventBean> anOldData : oldData) {
                        Object mk = oldDataMultiKey[count];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);
                        EventBean[] eventsPerStream = anOldData.getArray();

                        // Filter the having clause
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, false, agentInstanceContext);
                        if (!passesHaving) {
                            count++;
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsView.put(mk, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedRowAddToList(true, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                        count++;
                    }
                }
            }
        }

        generateOutputBatchedArrFromIterator(true, groupRepsView.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedJoinFirstCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

        CodegenMember helperFactory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember outputFactory = classScope.makeAddMember(OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(member(helperFactory.getMemberId()), "makeRSGroupedOutputFirst", REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), member(outputFactory.getMemberId()), constantNull(), constant(-1)));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "groupRepsView", newInstance(LinkedHashMap.class));

        if (forge.getOptionalHavingNode() == null) {
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKey.class, "aNewData", ref("newData"));
                        forloop.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKey.class, "anOldData", ref("oldData"));
                        forloop.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                    }
                }
            }
        } else {
            method.getBlock().exprDotMethod(ref("groupRepsView"), "clear");
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("oldData"), constantTrue()))
                        .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    ifNewData.declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKey.class, "aNewData", ref("newData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                                .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), REF_AGENTINSTANCECONTEXT)))
                                .increment("count")
                                .blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.increment("count");
                    }
                }

                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKey.class, "anOldData", ref("oldData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                                .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), REF_AGENTINSTANCECONTEXT)))
                                .increment("count")
                                .blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.increment("count");
                    }
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethodChain(ref("groupRepsView")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinAll(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
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

        if (prototype.isSelectRStream()) {
            generateOutputBatchedArrFromIterator(true, outputAllGroupReps.entryIterator(), false, generateSynthetic, oldEvents, oldEventsSortKey);
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            if (newData != null) {
                // apply new data to aggregates
                for (MultiKey<EventBean> aNewData : newData) {
                    EventBean[] eventsPerStream = aNewData.getArray();
                    Object mk = generateGroupKeySingle(eventsPerStream, true);

                    // if this is a newly encountered group, generate the remove stream event
                    if (outputAllGroupReps.put(mk, eventsPerStream) == null) {
                        if (prototype.isSelectRStream()) {
                            generateOutputBatchedRowAddToList(true, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                for (MultiKey<EventBean> anOldData : oldData) {
                    EventBean[] eventsPerStream = anOldData.getArray();
                    Object mk = generateGroupKeySingle(eventsPerStream, true);

                    if (outputAllGroupReps.put(mk, eventsPerStream) == null) {
                        if (prototype.isSelectRStream()) {
                            generateOutputBatchedRowAddToList(true, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }

                    aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                }
            }
        }

        generateOutputBatchedArrFromIterator(true, outputAllGroupReps.entryIterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedJoinAllCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);

        CodegenMember helperFactory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(member(helperFactory.getMemberId()), "makeRSGroupedOutputAllNoOpt", REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        if (forge.isSelectRStream()) {
            method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
        }

        {
            CodegenBlock forLoop = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forLoop.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));

            if (forge.isUnidirectional()) {
                forLoop.exprDotMethod(ref("this"), "clear");
            }

            {
                CodegenBlock ifNewData = forLoop.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNewData.forEach(MultiKey.class, "aNewData", ref("newData"));
                    forNew.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forLoop.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(MultiKey.class, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinDefault(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
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

        Map<Object, EventBean[]> keysAndEvents = new HashMap<>();

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            Object[] newDataMultiKey = generateGroupKeyArrayJoinTakingMap(newData, keysAndEvents, true);
            Object[] oldDataMultiKey = generateGroupKeyArrayJoinTakingMap(oldData, keysAndEvents, false);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedArrFromIterator(true, keysAndEvents.entrySet().iterator(), false, generateSynthetic, oldEvents, oldEventsSortKey);
            }

            ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey);

            generateOutputBatchedArrFromIterator(true, keysAndEvents.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

            keysAndEvents.clear();
        }

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedJoinDefaultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayJoinTakingMap = generateGroupKeyArrayJoinTakingMapCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "keysAndEvents", newInstance(HashMap.class));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            forEach.declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoinTakingMap, ref("newData"), ref("keysAndEvents"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoinTakingMap, ref("oldData"), ref("keysAndEvents"), constantFalse()));

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethodChain(ref("keysAndEvents")).add("entrySet").add("iterator"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"))
                    .localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethodChain(ref("keysAndEvents")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"))
                    .exprDotMethod(ref("keysAndEvents"), "clear");
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> processOutputLimitedViewLast(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
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

        Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<>();
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            if (newData != null) {
                // apply new data to aggregates
                for (EventBean aNewData : newData) {
                    EventBean[] eventsPerStream = new EventBean[]{aNewData};
                    Object mk = generateGroupKeySingle(eventsPerStream, true);

                    // if this is a newly encountered group, generate the remove stream event
                    if (groupRepsView.put(mk, eventsPerStream) == null) {
                        if (prototype.isSelectRStream()) {
                            generateOutputBatchedRowAddToList(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                for (EventBean anOldData : oldData) {
                    EventBean[] eventsPerStream = new EventBean[]{anOldData};
                    Object mk = generateGroupKeySingle(eventsPerStream, true);

                    if (groupRepsView.put(mk, eventsPerStream) == null) {
                        if (prototype.isSelectRStream()) {
                            generateOutputBatchedRowAddToList(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }

                    aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                }
            }
        }

        generateOutputBatchedArrFromIterator(false, groupRepsView.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "groupRepsView", newInstance(HashMap.class));
        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")));

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNewData.forEach(EventBean.class, "aNewData", ref("newData"));
                    forNew.declareVar(EventBean[].class, "eventsPerStream", newArrayWithInit(EventBean.class, ref("aNewData")))
                            .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(EventBean.class, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean[].class, "eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                            .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), newArrayWithInit(EventBean.class, ref("anOldData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethodChain(ref("groupRepsView")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> processOutputLimitedViewFirst(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
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

        Map<Object, EventBean[]> groupRepsView = new LinkedHashMap<>();
        if (prototype.getOptionalHavingNode() == null) {
            for (UniformPair<EventBean[]> pair : viewEventsList) {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                if (newData != null) {
                    // apply new data to aggregates
                    for (EventBean aNewData : newData) {
                        EventBean[] eventsPerStream = new EventBean[]{aNewData};
                        Object mk = generateGroupKeySingle(eventsPerStream, true);
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            // if this is a newly encountered group, generate the remove stream event
                            if (groupRepsView.put(mk, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedRowAddToList(false, mk, eventsPerStream, true, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                        aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                    }
                }
                if (oldData != null) {
                    // apply old data to aggregates
                    for (EventBean anOldData : oldData) {
                        EventBean[] eventsPerStream = new EventBean[]{anOldData};
                        Object mk = generateGroupKeySingle(eventsPerStream, false);
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsView.put(mk, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedRowAddToList(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }

                        aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                    }
                }
            }
        } else { // having clause present, having clause evaluates at the level of individual posts
            EventBean[] eventsPerStreamOneStream = new EventBean[1];
            for (UniformPair<EventBean[]> pair : viewEventsList) {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeyArrayView(newData, true);
                Object[] oldDataMultiKey = generateGroupKeyArrayView(oldData, false);

                ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey, eventsPerStreamOneStream);

                // evaluate having-clause
                if (newData != null) {
                    for (int i = 0; i < newData.length; i++) {
                        Object mk = newDataMultiKey[i];
                        eventsPerStreamOneStream[0] = newData[i];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStreamOneStream, true, agentInstanceContext);
                        if (!passesHaving) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            EventBean[] eventsPerStream = new EventBean[]{newData[i]};
                            if (groupRepsView.put(mk, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedRowAddToList(false, mk, eventsPerStream, true, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                    }
                }

                // evaluate having-clause
                if (oldData != null) {
                    for (int i = 0; i < oldData.length; i++) {
                        Object mk = oldDataMultiKey[i];
                        eventsPerStreamOneStream[0] = oldData[i];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStreamOneStream, false, agentInstanceContext);
                        if (!passesHaving) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            EventBean[] eventsPerStream = new EventBean[]{oldData[i]};
                            if (groupRepsView.put(mk, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedRowAddToList(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                                }
                            }
                        }
                    }
                }
            }
        }

        generateOutputBatchedArrFromIterator(false, groupRepsView.entrySet().iterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedViewFirstCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeyArrayView = generateGroupKeyArrayViewCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

        CodegenMember helperFactory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember outputFactory = classScope.makeAddMember(OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(member(helperFactory.getMemberId()), "makeRSGroupedOutputFirst", REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), member(outputFactory.getMemberId()), constantNull(), constant(-1)));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "groupRepsView", newInstance(LinkedHashMap.class));

        if (forge.getOptionalHavingNode() == null) {
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forEach(EventBean.class, "aNewData", ref("newData"));
                        forloop.declareVar(EventBean[].class, "eventsPerStream", newArrayWithInit(EventBean.class, ref("aNewData")))
                                .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forEach(EventBean.class, "anOldData", ref("oldData"));
                        forloop.declareVar(EventBean[].class, "eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                                .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                    }
                }
            }
        } else {
            // having clause present, having clause evaluates at the level of individual posts
            method.getBlock().declareVar(EventBean[].class, "eventsPerStreamOneStream", newArrayByLength(EventBean.class, constant(1)));

            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayView, ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayView, ref("oldData"), constantFalse()))
                        .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStreamOneStream"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                                .assignArrayElement("eventsPerStreamOneStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                                .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStreamOneStream"), constantTrue(), REF_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"))
                                .declareVar(EventBean[].class, "eventsPerStream", newArrayWithInit(EventBean.class, arrayAtIndex(ref("newData"), ref("i"))));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                    }
                }

                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forLoopIntSimple("i", arrayLength(ref("oldData")));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("i")))
                                .assignArrayElement("eventsPerStreamOneStream", constant(0), arrayAtIndex(ref("oldData"), ref("i")))
                                .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStreamOneStream"), constantFalse(), REF_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"))
                                .declareVar(EventBean[].class, "eventsPerStream", newArrayWithInit(EventBean.class, arrayAtIndex(ref("oldData"), ref("i"))));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                    }
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethodChain(ref("groupRepsView")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> processOutputLimitedViewAll(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        EventBean[] eventsPerStream = new EventBean[1];

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

        if (prototype.isSelectRStream()) {
            generateOutputBatchedArrFromIterator(false, outputAllGroupReps.entryIterator(), false, generateSynthetic, oldEvents, oldEventsSortKey);
        }

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            if (newData != null) {
                // apply new data to aggregates
                for (EventBean aNewData : newData) {
                    eventsPerStream[0] = aNewData;
                    Object mk = generateGroupKeySingle(eventsPerStream, true);

                    // if this is a newly encountered group, generate the remove stream event
                    if (outputAllGroupReps.put(mk, new EventBean[]{aNewData}) == null) {
                        if (prototype.isSelectRStream()) {
                            generateOutputBatchedRowAddToList(false, mk, eventsPerStream, true, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                for (EventBean anOldData : oldData) {
                    eventsPerStream[0] = anOldData;
                    Object mk = generateGroupKeySingle(eventsPerStream, false);

                    if (outputAllGroupReps.put(mk, new EventBean[]{anOldData}) == null) {
                        if (prototype.isSelectRStream()) {
                            generateOutputBatchedRowAddToList(false, mk, eventsPerStream, false, generateSynthetic, oldEvents, oldEventsSortKey);
                        }
                    }

                    aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                }
            }
        }

        generateOutputBatchedArrFromIterator(false, outputAllGroupReps.entryIterator(), true, generateSynthetic, newEvents, newEventsSortKey);

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedViewAllCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {

        CodegenMethodNode generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);

        CodegenMember helperFactory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(member(helperFactory.getMemberId()), "makeRSGroupedOutputAllNoOpt", REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        if (forge.isSelectRStream()) {
            method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
        }

        {
            CodegenBlock forLoop = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forLoop.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")));

            {
                CodegenBlock ifNewData = forLoop.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNewData.forEach(EventBean.class, "aNewData", ref("newData"));
                    forNew.assignArrayElement(ref("eventsPerStream"), constant(0), ref("aNewData"))
                            .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.class, ref("aNewData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forLoop.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(EventBean.class, "anOldData", ref("oldData"));
                    forOld.assignArrayElement(ref("eventsPerStream"), constant(0), ref("anOldData"))
                            .declareVar(Object.class, "mk", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.class, ref("anOldData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> processOutputLimitedViewDefault(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
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

        Map<Object, EventBean> keysAndEvents = new HashMap<>();
        EventBean[] eventsPerStream = new EventBean[1];

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeysKeepEvent(newData, keysAndEvents, true, eventsPerStream);
            Object[] oldDataMultiKey = generateGroupKeysKeepEvent(oldData, keysAndEvents, false, eventsPerStream);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedRowFromMap(keysAndEvents, false, generateSynthetic, oldEvents, oldEventsSortKey, agentInstanceContext);
            }

            ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey, eventsPerStream);
            generateOutputBatchedRowFromMap(keysAndEvents, true, generateSynthetic, newEvents, newEventsSortKey, agentInstanceContext);
            keysAndEvents.clear();
        }

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeysKeepEvent = generateGroupKeysKeepEventCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputBatchedRowFromMap = generateOutputBatchedRowFromMapCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "keysAndEvents", newInstance(HashMap.class))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeysKeepEvent, ref("newData"), ref("keysAndEvents"), constantTrue(), ref("eventsPerStream")))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeysKeepEvent, ref("oldData"), ref("keysAndEvents"), constantFalse(), ref("eventsPerStream")));

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedRowFromMap, ref("keysAndEvents"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), REF_AGENTINSTANCECONTEXT);
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"))
                    .localMethod(generateOutputBatchedRowFromMap, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), REF_AGENTINSTANCECONTEXT)
                    .exprDotMethod(ref("keysAndEvents"), "clear");
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    public boolean hasHavingClause() {
        return prototype.getOptionalHavingNode() != null;
    }

    public boolean evaluateHavingClause(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public boolean isSelectRStream() {
        return prototype.isSelectRStream();
    }

    public ExprEvaluatorContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    private Object[] generateGroupKeyArrayJoin(Set<MultiKey<EventBean>> resultSet, boolean isNewData) {
        if (resultSet.isEmpty()) {
            return null;
        }

        Object[] keys = new Object[resultSet.size()];

        int count = 0;
        for (MultiKey<EventBean> eventsPerStream : resultSet) {
            keys[count] = generateGroupKeySingle(eventsPerStream.getArray(), isNewData);
            count++;
        }

        return keys;
    }

    EventBean shortcutEvalGivenKey(EventBean[] eventsPerStream, Object groupKey, boolean isNewData, boolean isSynthesize) {
        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);

        if (prototype.getOptionalHavingNode() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, true, agentInstanceContext);
            if (!passesHaving) {
                return null;
            }
        }
        return selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
    }

    static CodegenMethodNode shortcutEvalGivenKeyCodegen(ExprForge optionalHavingNode, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());
            if (optionalHavingNode != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockReturn(constantNull());
            }
            methodNode.getBlock().methodReturn(exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));
        };

        return instance.getMethods().addMethod(EventBean.class, "shortcutEvalGivenKey",
                CodegenNamedParam.from(EventBean[].class, NAME_EPS, Object.class, "groupKey", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE),
                ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private UniformPair<EventBean[]> processViewResultPairDepthOneNoRStream(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        Object newGroupKey = generateGroupKeySingle(newData, true);
        Object oldGroupKey = generateGroupKeySingle(oldData, false);

        aggregationService.applyEnter(newData, newGroupKey, agentInstanceContext);
        aggregationService.applyLeave(oldData, oldGroupKey, agentInstanceContext);

        if (Objects.equals(newGroupKey, oldGroupKey)) {
            EventBean istream = shortcutEvalGivenKey(newData, newGroupKey, true, isSynthesize);
            return ResultSetProcessorUtil.toPairNullIfNullIStream(istream);
        }

        EventBean newKeyEvent = shortcutEvalGivenKey(newData, newGroupKey, true, isSynthesize);
        EventBean oldKeyEvent = shortcutEvalGivenKey(oldData, oldGroupKey, true, isSynthesize);
        if (prototype.isSorting()) {
            aggregationService.setCurrentAccess(newGroupKey, agentInstanceContext.getAgentInstanceId(), null);
            Object newSortKey = orderByProcessor.getSortKey(newData, true, agentInstanceContext);
            aggregationService.setCurrentAccess(oldGroupKey, agentInstanceContext.getAgentInstanceId(), null);
            Object oldSortKey = orderByProcessor.getSortKey(oldData, true, agentInstanceContext);
            EventBean[] sorted = orderByProcessor.sortTwoKeys(newKeyEvent, newSortKey, oldKeyEvent, oldSortKey);
            return new UniformPair<>(sorted, null);
        }
        return new UniformPair<>(new EventBean[]{newKeyEvent, oldKeyEvent}, null);
    }

    private static CodegenMethodNode processViewResultPairDepthOneNoRStreamCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);
        CodegenMethodNode generateGroupKeySingle = ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().declareVar(Object.class, "newGroupKey", localMethod(generateGroupKeySingle, REF_NEWDATA, constantTrue()))
                    .declareVar(Object.class, "oldGroupKey", localMethod(generateGroupKeySingle, REF_OLDDATA, constantFalse()))
                    .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("newGroupKey"), REF_AGENTINSTANCECONTEXT)
                    .exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", REF_OLDDATA, ref("oldGroupKey"), REF_AGENTINSTANCECONTEXT)
                    .ifCondition(staticMethod(Objects.class, "equals", ref("newGroupKey"), ref("oldGroupKey")))
                    .declareVar(EventBean.class, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("newGroupKey"), constantTrue(), REF_ISSYNTHESIZE))
                    .blockReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")))
                    .declareVar(EventBean.class, "newKeyEvent", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("newGroupKey"), constant(true), REF_ISSYNTHESIZE))
                    .declareVar(EventBean.class, "oldKeyEvent", localMethod(shortcutEvalGivenKey, REF_OLDDATA, ref("oldGroupKey"), constant(true), REF_ISSYNTHESIZE));

            if (forge.isSorting()) {
                methodNode.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("newGroupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .declareVar(Object.class, "newSortKey", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", REF_NEWDATA, constantTrue(), REF_AGENTINSTANCECONTEXT))
                        .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("newGroupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .declareVar(Object.class, "oldSortKey", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", REF_OLDDATA, constantTrue(), REF_AGENTINSTANCECONTEXT))
                        .declareVar(EventBean[].class, "sorted", exprDotMethod(REF_ORDERBYPROCESSOR, "sortTwoKeys", ref("newKeyEvent"), ref("newSortKey"), ref("oldKeyEvent"), ref("oldSortKey")))
                        .methodReturn(newInstance(UniformPair.class, ref("sorted"), constantNull()));
            } else {
                methodNode.getBlock().methodReturn(newInstance(UniformPair.class, newArrayWithInit(EventBean.class, ref("newKeyEvent"), ref("oldKeyEvent")), constantNull()));
            }
        };

        return instance.getMethods().addMethod(UniformPair.class, "processViewResultPairDepthOneNoRStream", CodegenNamedParam.from(EventBean[].class, NAME_NEWDATA, EventBean[].class, NAME_OLDDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private UniformPair<EventBean[]> processViewResultNewDepthOne(EventBean[] newData, boolean isSynthesize) {
        Object groupKey = generateGroupKeySingle(newData, true);
        EventBean rstream = !prototype.isSelectRStream() ? null : shortcutEvalGivenKey(newData, groupKey, false, isSynthesize);   // using newdata to compute is safe here
        aggregationService.applyEnter(newData, groupKey, agentInstanceContext);
        EventBean istream = shortcutEvalGivenKey(newData, groupKey, true, isSynthesize);
        return ResultSetProcessorUtil.toPairNullIfAllNullSingle(istream, rstream);
    }

    static CodegenMethodNode processViewResultNewDepthOneCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);
        CodegenMethodNode generateGroupKeySingle = ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().declareVar(Object.class, "groupKey", localMethod(generateGroupKeySingle, REF_NEWDATA, constantTrue()));
            if (forge.isSelectRStream()) {
                methodNode.getBlock().declareVar(EventBean.class, "rstream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("groupKey"), constantFalse(), REF_ISSYNTHESIZE));
            }
            methodNode.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("groupKey"), REF_AGENTINSTANCECONTEXT)
                    .declareVar(EventBean.class, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("groupKey"), constantTrue(), REF_ISSYNTHESIZE));
            if (forge.isSelectRStream()) {
                methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfAllNullSingle", ref("istream"), ref("rstream")));
            } else {
                methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")));
            }
        };

        return instance.getMethods().addMethod(UniformPair.class, "processViewResultNewDepthOneCodegen", CodegenNamedParam.from(EventBean[].class, NAME_NEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }
}
