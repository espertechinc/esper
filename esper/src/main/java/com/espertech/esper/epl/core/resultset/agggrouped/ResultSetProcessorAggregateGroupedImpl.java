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
package com.espertech.esper.epl.core.resultset.agggrouped;

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
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroupImpl;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.view.OutputConditionPolled;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil.*;
import static com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedUtil.*;
import static com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames.REF_EPS;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.NAME_EPS;
import static com.espertech.esper.util.CollectionUtil.*;

/**
 * Result-set processor for the aggregate-grouped case:
 * there is a group-by and one or more non-aggregation event properties in the select clause are not listed in the group by,
 * and there are aggregation functions.
 * <p>
 * This processor does perform grouping by computing MultiKey group-by keys for each row.
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 * <p>
 * Aggregation state is a table of rows held by {@link AggregationService} where the row key is the group-by MultiKey.
 */
public class ResultSetProcessorAggregateGroupedImpl implements ResultSetProcessorAggregateGrouped {

    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";
    private final static String NAME_OUTPUTFIRSTHELPER = "outputFirstHelper";
    private final static String NAME_OUTPUTALLGROUPREPS = "outputAllGroupReps";

    protected final ResultSetProcessorAggregateGroupedFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected AgentInstanceContext agentInstanceContext;

    // For output limiting, keep a representative of each group-by group
    private ResultSetProcessorGroupedOutputAllGroupReps outputAllGroupReps;

    private ResultSetProcessorAggregateGroupedOutputLastHelper outputLastHelper;
    private ResultSetProcessorAggregateGroupedOutputAllHelper outputAllHelper;
    private ResultSetProcessorGroupedOutputFirstHelper outputFirstHelper;

    ResultSetProcessorAggregateGroupedImpl(ResultSetProcessorAggregateGroupedFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.agentInstanceContext = agentInstanceContext;

        aggregationService.setRemovedCallback(this);

        if (prototype.isOutputLast() && prototype.getOutputConditionType() == ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED) {
            outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSAggregateGroupedOutputLastOpt(agentInstanceContext, this, prototype.getGroupKeyTypes(), prototype.getNumStreams());
        } else if (prototype.isOutputAll()) {
            if (prototype.getOutputConditionType() != ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED) {
                outputAllGroupReps = prototype.getResultSetProcessorHelperFactory().makeRSGroupedOutputAllNoOpt(agentInstanceContext, prototype.getGroupKeyTypes(), prototype.getNumStreams());
            } else {
                outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSAggregateGroupedOutputAll(agentInstanceContext, this, prototype.getGroupKeyTypes(), prototype.getNumStreams());
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

    public AggregationService getAggregationService() {
        return aggregationService;
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

    public static void applyViewResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
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
            for (MultiKey<EventBean> eventsPerStream : newEvents) {
                Object mk = generateGroupKeySingle(eventsPerStream.getArray(), true);
                aggregationService.applyEnter(eventsPerStream.getArray(), mk, agentInstanceContext);
            }
        }
        if (oldEvents != null && !oldEvents.isEmpty()) {
            // apply old data to aggregates
            for (MultiKey<EventBean> eventsPerStream : oldEvents) {
                Object mk = generateGroupKeySingle(eventsPerStream.getArray(), false);
                aggregationService.applyLeave(eventsPerStream.getArray(), mk, agentInstanceContext);
            }
        }
    }

    public static void applyJoinResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
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
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerEvent();
        }
        // Generate group-by keys for all events
        Object[] newDataGroupByKeys = generateGroupKeyArrayJoin(newEvents, true);
        Object[] oldDataGroupByKeys = generateGroupKeyArrayJoin(oldEvents, false);

        // generate old events
        if (prototype.isUnidirectional()) {
            this.clear();
        }

        ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newEvents, newDataGroupByKeys, oldEvents, oldDataGroupByKeys);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsJoin(oldEvents, oldDataGroupByKeys, false, isSynthesize);
        }
        EventBean[] selectNewEvents = generateOutputEventsJoin(newEvents, newDataGroupByKeys, true, isSynthesize);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerEvent(selectNewEvents, selectOldEvents);
        }
        return ResultSetProcessorUtil.toPairNullIfAllNull(selectNewEvents, selectOldEvents);
    }

    public static void processJoinResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        method.getBlock().declareVar(Object[].class, "newDataGroupByKeys", localMethod(generateGroupKeyArrayJoin, REF_NEWDATA, constantTrue()))
                .declareVar(Object[].class, "oldDataGroupByKeys", localMethod(generateGroupKeyArrayJoin, REF_OLDDATA, constantFalse()));

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }

        method.getBlock().staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataGroupByKeys"), REF_OLDDATA, ref("oldDataGroupByKeys"));

        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, REF_OLDDATA, ref("oldDataGroupByKeys"), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsJoin, REF_NEWDATA, ref("newDataGroupByKeys"), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerEvent();
        }

        if (newData != null && newData.length == 1) {
            if (oldData == null || oldData.length == 0) {
                UniformPair<EventBean[]> pair = processViewResultNewDepthOne(newData, isSynthesize);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aResultSetProcessGroupedRowPerEvent(pair);
                }
                return pair;
            }
            if (oldData.length == 1) {
                UniformPair<EventBean[]> pair = processViewResultPairDepthOne(newData, oldData, isSynthesize);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aResultSetProcessGroupedRowPerEvent(pair);
                }
                return pair;
            }
        }

        // Generate group-by keys for all events
        Object[] newDataGroupByKeys = generateGroupKeyArrayView(newData, true);
        Object[] oldDataGroupByKeys = generateGroupKeyArrayView(oldData, false);

        // update aggregates
        EventBean[] eventsPerStream = new EventBean[1];
        ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataGroupByKeys, oldData, oldDataGroupByKeys, eventsPerStream);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsView(oldData, oldDataGroupByKeys, false, isSynthesize, eventsPerStream);
        }
        EventBean[] selectNewEvents = generateOutputEventsView(newData, newDataGroupByKeys, true, isSynthesize, eventsPerStream);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerEvent(selectNewEvents, selectOldEvents);
        }
        return ResultSetProcessorUtil.toPairNullIfAllNull(selectNewEvents, selectOldEvents);
    }

    public static void processViewResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayView = ResultSetProcessorGroupedUtil.generateGroupKeyArrayViewCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        CodegenMethodNode processViewResultNewDepthOne = processViewResultNewDepthOneCodegen(forge, classScope, instance);
        CodegenMethodNode processViewResultPairDepthOneNoRStream = processViewResultPairDepthOneCodegen(forge, classScope, instance);

        CodegenBlock ifShortcut = method.getBlock().ifCondition(and(notEqualsNull(REF_NEWDATA), equalsIdentity(arrayLength(REF_NEWDATA), constant(1))));
        ifShortcut.ifCondition(or(equalsNull(REF_OLDDATA), equalsIdentity(arrayLength(REF_OLDDATA), constant(0))))
                .blockReturn(localMethod(processViewResultNewDepthOne, REF_NEWDATA, REF_ISSYNTHESIZE))
                .ifCondition(equalsIdentity(arrayLength(REF_OLDDATA), constant(1)))
                .blockReturn(localMethod(processViewResultPairDepthOneNoRStream, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE));

        method.getBlock().declareVar(Object[].class, "newDataGroupByKeys", localMethod(generateGroupKeyArrayView, REF_NEWDATA, constantTrue()))
                .declareVar(Object[].class, "oldDataGroupByKeys", localMethod(generateGroupKeyArrayView, REF_OLDDATA, constantFalse()))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataGroupByKeys"), REF_OLDDATA, ref("oldDataGroupByKeys"), ref("eventsPerStream"));

        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, REF_OLDDATA, ref("oldDataGroupByKeys"), constantFalse(), REF_ISSYNTHESIZE, ref("eventsPerStream")) : constantNull())
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsView, REF_NEWDATA, ref("newDataGroupByKeys"), constantTrue(), REF_ISSYNTHESIZE, ref("eventsPerStream")))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    private EventBean[] generateOutputEventsView(EventBean[] outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, EventBean[] eventsPerStream) {
        if (outputEvents == null) {
            return null;
        }

        EventBean[] events = new EventBean[outputEvents.length];
        Object[] keys = new Object[outputEvents.length];
        EventBean[][] currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new EventBean[outputEvents.length][];
        }

        int countOutputRows = 0;
        for (int countInputRows = 0; countInputRows < outputEvents.length; countInputRows++) {
            aggregationService.setCurrentAccess(groupByKeys[countInputRows], agentInstanceContext.getAgentInstanceId(), null);
            eventsPerStream[0] = outputEvents[countInputRows];

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }

            events[countOutputRows] = selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            keys[countOutputRows] = groupByKeys[countInputRows];
            if (prototype.isSorting()) {
                EventBean[] currentEventsPerStream = new EventBean[]{outputEvents[countInputRows]};
                currentGenerators[countOutputRows] = currentEventsPerStream;
            }

            countOutputRows++;
        }

        return ResultSetProcessorUtil.outputFromCountMaySort(countOutputRows, events, keys, currentGenerators, isNewData, orderByProcessor, agentInstanceContext, aggregationService);
    }

    private static CodegenMethodNode generateOutputEventsViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifRefNullReturnNull(ref("outputEvents"))
                    .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, arrayLength(ref("outputEvents"))))
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, arrayLength(ref("outputEvents"))));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean[][].class, "currentGenerators", newArrayByLength(EventBean[].class, arrayLength(ref("outputEvents"))));
            }

            methodNode.getBlock().declareVar(int.class, "countOutputRows", constant(0))
                    .declareVar(int.class, "cpid", exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forLoop = methodNode.getBlock().forLoopIntSimple("countInputRows", arrayLength(ref("outputEvents")));
                forLoop.exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("countInputRows")), ref("cpid"), constantNull())
                        .assignArrayElement(REF_EPS, constant(0), arrayAtIndex(ref("outputEvents"), ref("countInputRows")));

                if (forge.getOptionalHavingNode() != null) {
                    forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forLoop.assignArrayElement("events", ref("countOutputRows"), exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT))
                        .assignArrayElement("keys", ref("countOutputRows"), arrayAtIndex(ref("groupByKeys"), ref("countInputRows")));

                if (forge.isSorting()) {
                    forLoop.assignArrayElement("currentGenerators", ref("countOutputRows"), newArrayWithInit(EventBean.class, arrayAtIndex(ref("outputEvents"), ref("countInputRows"))));
                }

                forLoop.increment("countOutputRows")
                        .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("countOutputRows"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsView",
                CodegenNamedParam.from(EventBean[].class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, EventBean[].class, NAME_EPS),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    public Object[] generateGroupKeyArrayJoin(Set<MultiKey<EventBean>> resultSet, boolean isNewData) {
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

    public Object[] generateGroupKeyArrayView(EventBean[] events, boolean isNewData) {
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

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        if (outputAllGroupReps != null) {
            visitor.visit(outputAllGroupReps);
        }
        if (outputLastHelper != null) {
            visitor.visit(outputLastHelper);
        }
        if (outputAllHelper != null) {
            visitor.visit(outputAllHelper);
        }
        if (outputFirstHelper != null) {
            visitor.visit(outputFirstHelper);
        }
    }

    public static void acceptHelperVisitorCodegen(CodegenMethodNode method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTALLGROUPREPS));
        }
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTLASTHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTALLHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTFIRSTHELPER));
        }
    }

    /**
     * Generates the group-by key for the row
     *
     * @param eventsPerStream is the row of events
     * @param isNewData       is true for new data
     * @return grouping keys
     */
    public Object generateGroupKeySingle(EventBean[] eventsPerStream, boolean isNewData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessComputeGroupKeys(isNewData, prototype.getGroupKeyNodeExpressions(), eventsPerStream);

            Object keyObject;
            if (prototype.getGroupKeyNode() != null) {
                keyObject = prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
            } else {
                Object[] keys = new Object[prototype.getGroupKeyNodes().length];
                int count = 0;
                for (ExprEvaluator exprNode : prototype.getGroupKeyNodes()) {
                    keys[count] = exprNode.evaluate(eventsPerStream, isNewData, agentInstanceContext);
                    count++;
                }
                keyObject = new MultiKeyUntyped(keys);
            }
            InstrumentationHelper.get().aResultSetProcessComputeGroupKeys(isNewData, keyObject);
            return keyObject;
        }

        if (prototype.getGroupKeyNode() != null) {
            return prototype.getGroupKeyNode().evaluate(eventsPerStream, isNewData, agentInstanceContext);
        }

        Object[] keys = new Object[prototype.getGroupKeyNodes().length];
        int count = 0;
        for (ExprEvaluator exprNode : prototype.getGroupKeyNodes()) {
            keys[count] = exprNode.evaluate(eventsPerStream, isNewData, agentInstanceContext);
            count++;
        }
        return new MultiKeyUntyped(keys);
    }

    private EventBean[] generateOutputEventsJoin(Set<MultiKey<EventBean>> resultSet, Object[] groupByKeys, boolean isNewData, boolean isSynthesize) {
        if (resultSet.isEmpty()) {
            return null;
        }

        EventBean[] events = new EventBean[resultSet.size()];
        Object[] keys = new Object[resultSet.size()];
        EventBean[][] currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new EventBean[resultSet.size()][];
        }

        int countOutputRows = 0;
        int countInputRows = -1;
        for (MultiKey<EventBean> row : resultSet) {
            countInputRows++;
            EventBean[] eventsPerStream = row.getArray();

            aggregationService.setCurrentAccess(groupByKeys[countInputRows], agentInstanceContext.getAgentInstanceId(), null);

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }

            events[countOutputRows] = selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
            keys[countOutputRows] = groupByKeys[countInputRows];
            if (prototype.isSorting()) {
                currentGenerators[countOutputRows] = eventsPerStream;
            }

            countOutputRows++;
        }

        return ResultSetProcessorUtil.outputFromCountMaySort(countOutputRows, events, keys, currentGenerators, isNewData, orderByProcessor, agentInstanceContext, aggregationService);
    }

    private static CodegenMethodNode generateOutputEventsJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifCondition(exprDotMethod(ref("resultSet"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, exprDotMethod(ref("resultSet"), "size")))
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("resultSet"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean[][].class, "currentGenerators", newArrayByLength(EventBean[].class, exprDotMethod(ref("resultSet"), "size")));
            }

            methodNode.getBlock().declareVar(int.class, "countOutputRows", constant(0))
                    .declareVar(int.class, "countInputRows", constant(-1))
                    .declareVar(int.class, "cpid", exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forLoop = methodNode.getBlock().forEach(MultiKey.class, "row", ref("resultSet"));
                forLoop.increment("countInputRows")
                        .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("row"), "getArray")))
                        .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("countInputRows")), ref("cpid"), constantNull());

                if (forge.getOptionalHavingNode() != null) {
                    forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forLoop.assignArrayElement("events", ref("countOutputRows"), exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT))
                        .assignArrayElement("keys", ref("countOutputRows"), arrayAtIndex(ref("groupByKeys"), ref("countInputRows")));

                if (forge.isSorting()) {
                    forLoop.assignArrayElement("currentGenerators", ref("countOutputRows"), ref("eventsPerStream"));
                }

                forLoop.increment("countOutputRows")
                        .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("countOutputRows"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsJoin",
                CodegenNamedParam.from(Set.class, "resultSet", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
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

    public static void getIteratorViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, method, classScope, instance), REF_VIEWABLE));
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

        method.getBlock().declareVar(ArrayDeque.class, "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, method, classScope, instance), REF_VIEWABLE)))
                .exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private Iterator<EventBean> obtainIterator(Viewable parent) {
        if (orderByProcessor == null) {
            return new ResultSetProcessorAggregateGroupedIterator(parent.iterator(), this, aggregationService, agentInstanceContext);
        }

        // Pull all parent events, generate order keys
        EventBean[] eventsPerStream = new EventBean[1];
        List<EventBean> outgoingEvents = new ArrayList<>();
        List<Object> orderKeys = new ArrayList<>();

        for (EventBean candidate : parent) {
            eventsPerStream[0] = candidate;

            Object groupKey = generateGroupKeySingle(eventsPerStream, true);
            aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);

            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, true, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }

            outgoingEvents.add(selectExprProcessor.process(eventsPerStream, true, true, agentInstanceContext));

            Object orderKey = orderByProcessor.getSortKey(eventsPerStream, true, agentInstanceContext);
            orderKeys.add(orderKey);
        }

        return ResultSetProcessorUtil.orderOutgoingGetIterator(outgoingEvents, orderKeys, orderByProcessor, agentInstanceContext);
    }

    private static CodegenMethodNode obtainIteratorCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenMethodNode parent, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode iterator = parent.makeChild(Iterator.class, ResultSetProcessorAggregateGroupedImpl.class, classScope).addParam(Viewable.class, NAME_VIEWABLE);
        if (!forge.isSorting()) {
            iterator.getBlock().methodReturn(newInstance(ResultSetProcessorAggregateGroupedIterator.class, exprDotMethod(REF_VIEWABLE, "iterator"), ref("this"), REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT));
            return iterator;
        }

        CodegenMethodNode generateGroupKeySingle = ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        // Pull all parent events, generate order keys
        iterator.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(List.class, "outgoingEvents", newInstance(ArrayList.class))
                .declareVar(List.class, "orderKeys", newInstance(ArrayList.class));

        {
            CodegenBlock forLoop = iterator.getBlock().forEach(EventBean.class, "candidate", REF_VIEWABLE);
            forLoop.assignArrayElement(ref("eventsPerStream"), constant(0), ref("candidate"))
                    .declareVar(Object.class, "groupKey", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()))
                    .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, constantTrue(), REF_AGENTINSTANCECONTEXT))).blockContinue();
            }

            forLoop.exprDotMethod(ref("outgoingEvents"), "add", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), constantTrue(), constantTrue(), REF_AGENTINSTANCECONTEXT))
                    .exprDotMethod(ref("orderKeys"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), REF_AGENTINSTANCECONTEXT));
        }

        iterator.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_ORDEROUTGOINGGETITERATOR, ref("outgoingEvents"), ref("orderKeys"), REF_ORDERBYPROCESSOR, REF_AGENTINSTANCECONTEXT));
        return iterator;
    }

    /**
     * Returns the select expression processor
     *
     * @return select processor.
     */
    public SelectExprProcessor getSelectExprProcessor() {
        return selectExprProcessor;
    }

    /**
     * Returns the having node.
     *
     * @return having expression
     */
    public ExprEvaluator getOptionalHavingNode() {
        return prototype.getOptionalHavingNode();
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        // Generate group-by keys for all events
        Object[] groupByKeys = generateGroupKeyArrayJoin(joinSet, true);
        EventBean[] result = generateOutputEventsJoin(joinSet, groupByKeys, true, true);
        return new ArrayEventIterator(result);
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        method.getBlock().declareVar(Object[].class, "groupByKeys", localMethod(generateGroupKeyArrayJoin, REF_JOINSET, constantTrue()))
                .declareVar(EventBean[].class, "result", localMethod(generateOutputEventsJoin, REF_JOINSET, ref("groupByKeys"), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("result")));
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
        } else {
            throw new IllegalStateException("Unrecognized output limit " + outputLimitLimitType);
        }
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        OutputLimitLimitType outputLimitLimitType = forge.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            processOutputLimitedJoinDefaultCodegen(forge, classScope, method, instance);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            processOutputLimitedJoinAllCodegen(forge, classScope, method, instance);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            processOutputLimitedJoinFirstCodegen(forge, classScope, method, instance);
        } else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            processOutputLimitedJoinLastCodegen(forge, classScope, method, instance);
        } else {
            throw new IllegalStateException("Unrecognized output limit " + outputLimitLimitType);
        }
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
        } else {
            throw new IllegalStateException("Unrecognized output limited type " + outputLimitLimitType);
        }
    }

    public static void processOutputLimitedViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        OutputLimitLimitType outputLimitLimitType = forge.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            processOutputLimitedViewDefaultCodegen(forge, classScope, method, instance);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            processOutputLimitedViewAllCodegen(forge, classScope, method, instance);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            processOutputLimitedViewFirstCodegen(forge, classScope, method, instance);
        } else if (outputLimitLimitType == OutputLimitLimitType.LAST) {
            processOutputLimitedViewLastCodegen(forge, classScope, method, instance);
        } else {
            throw new IllegalStateException("Unrecognized output limited type " + outputLimitLimitType);
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

    public static void stopMethodCodegen(CodegenMethodNode method, CodegenInstanceAux instance) {
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

    public void generateOutputBatchedJoinUnkeyed(Set<MultiKey<EventBean>> outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Collection<EventBean> resultEvents, List<Object> optSortKeys) {
        if (outputEvents == null) {
            return;
        }

        EventBean[] eventsPerStream;
        int count = 0;

        for (MultiKey<EventBean> row : outputEvents) {
            aggregationService.setCurrentAccess(groupByKeys[count], agentInstanceContext.getAgentInstanceId(), null);
            eventsPerStream = row.getArray();

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
                if (!passesHaving) {
                    count++;
                    continue;
                }
            }

            resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));
            if (prototype.isSorting()) {
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }

            count++;
        }
    }

    static CodegenMethodNode generateOutputBatchedJoinUnkeyedCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(int.class, "count", constant(0))
                    .declareVarNoInit(EventBean[].class, "eventsPerStream");

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(MultiKey.class, "row", ref("outputEvents"));
                forEach.exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("count")), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("row"), "getArray")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT)))
                            .increment("count")
                            .blockContinue();
                }

                forEach.exprDotMethod(ref("resultEvents"), "add", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
                }

                forEach.increment("count");
            }
        };
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedJoinUnkeyed",
                CodegenNamedParam.from(Set.class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Collection.class, "resultEvents", List.class, "optSortKeys"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }

    public EventBean generateOutputBatchedSingle(Object groupByKey, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize) {
        aggregationService.setCurrentAccess(groupByKey, agentInstanceContext.getAgentInstanceId(), null);

        // Filter the having clause
        if (prototype.getOptionalHavingNode() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
            if (!passesHaving) {
                return null;
            }
        }

        return selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
    }

    static void generateOutputBatchedSingleCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupByKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockReturn(constantNull());
            }

            methodNode.getBlock().methodReturn(exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));
        };
        instance.getMethods().addMethod(EventBean.class, "generateOutputBatchedSingle", CodegenNamedParam.from(Object.class, "groupByKey", EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorUtil.class, classScope, code);
    }

    public void generateOutputBatchedViewPerKey(EventBean[] outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Map<Object, EventBean> resultEvents, Map<Object, Object> optSortKeys, EventBean[] eventsPerStream) {
        if (outputEvents == null) {
            return;
        }

        int count = 0;
        for (EventBean outputEvent : outputEvents) {
            Object groupKey = groupByKeys[count];
            aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);
            eventsPerStream[0] = outputEvents[count];

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }

            resultEvents.put(groupKey, selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));
            if (prototype.isSorting()) {
                optSortKeys.put(groupKey, orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }

            count++;
        }
    }

    static CodegenMethodNode generateOutputBatchedViewPerKeyCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(int.class, "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(EventBean.class, "outputEvent", ref("outputEvents"));
                forEach.declareVar(Object.class, "groupKey", arrayAtIndex(ref("groupByKeys"), ref("count")))
                        .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .assignArrayElement(ref("eventsPerStream"), constant(0), arrayAtIndex(ref("outputEvents"), ref("count")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forEach.exprDotMethod(ref("resultEvents"), "put", ref("groupKey"), exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("optSortKeys"), "put", ref("groupKey"), exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
                }

                forEach.increment("count");
            }
        };
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedViewPerKey",
                CodegenNamedParam.from(EventBean[].class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Map.class, "resultEvents", Map.class, "optSortKeys", EventBean[].class, "eventsPerStream"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }


    public void generateOutputBatchedJoinPerKey(Set<MultiKey<EventBean>> outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Map<Object, EventBean> resultEvents, Map<Object, Object> optSortKeys) {
        if (outputEvents == null) {
            return;
        }

        int count = 0;
        for (MultiKey<EventBean> row : outputEvents) {
            Object groupKey = groupByKeys[count];
            aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), row.getArray(), isNewData, agentInstanceContext);
                if (!passesHaving) {
                    continue;
                }
            }

            resultEvents.put(groupKey, selectExprProcessor.process(row.getArray(), isNewData, isSynthesize, agentInstanceContext));
            if (prototype.isSorting()) {
                optSortKeys.put(groupKey, orderByProcessor.getSortKey(row.getArray(), isNewData, agentInstanceContext));
            }

            count++;
        }
    }

    static CodegenMethodNode generateOutputBatchedJoinPerKeyCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(int.class, "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(MultiKey.class, "row", ref("outputEvents"));
                forEach.declareVar(Object.class, "groupKey", arrayAtIndex(ref("groupByKeys"), ref("count")))
                        .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("row"), "getArray")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forEach.exprDotMethod(ref("resultEvents"), "put", ref("groupKey"), exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("optSortKeys"), "put", ref("groupKey"), exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
                }

                forEach.increment("count");
            }
        };
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedJoinPerKey",
                CodegenNamedParam.from(Set.class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Map.class, "resultEvents", Map.class, "optSortKeys"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }

    public void removedAggregationGroupKey(Object key) {
        if (outputAllGroupReps != null) {
            outputAllGroupReps.remove(key);
        }
        if (outputAllHelper != null) {
            outputAllHelper.remove(key);
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
            if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
                method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), "remove", ref("key"));
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

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (prototype.isOutputAll()) {
            outputAllHelper.processView(newData, oldData, isGenerateSynthetic);
        } else {
            outputLastHelper.processView(newData, oldData, isGenerateSynthetic);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorAggregateGroupedForge forge, String methodName, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMember factory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());

        if (forge.isOutputAll()) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorAggregateGroupedOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSAggregateGroupedOutputAll", REF_AGENTINSTANCECONTEXT, ref("this"), member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorAggregateGroupedOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSAggregateGroupedOutputLastOpt", REF_AGENTINSTANCECONTEXT, ref("this"), member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));
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

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize) {
        if (prototype.isOutputAll()) {
            return outputAllHelper.outputView(isSynthesize);
        }
        return outputLastHelper.outputView(isSynthesize);
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenMethodNode method) {
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

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenMethodNode method) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinLast(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        Map<Object, EventBean> lastPerGroupNew = new LinkedHashMap<>();
        Map<Object, EventBean> lastPerGroupOld = null;
        if (prototype.isSelectRStream()) {
            lastPerGroupOld = new LinkedHashMap<>();
        }

        Map<Object, Object> newEventsSortKey = null; // group key to sort key
        Map<Object, Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedHashMap<>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedHashMap<>();
            }
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeyArrayJoin(newData, true);
            Object[] oldDataMultiKey = generateGroupKeyArrayJoin(oldData, false);

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedJoinPerKey(oldData, oldDataMultiKey, false, generateSynthetic, lastPerGroupOld, oldEventsSortKey);
            }
            generateOutputBatchedJoinPerKey(newData, newDataMultiKey, true, generateSynthetic, lastPerGroupNew, newEventsSortKey);
        }

        EventBean[] newEventsArr = CollectionUtil.toArrayNullForEmptyValueEvents(lastPerGroupNew);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = CollectionUtil.toArrayNullForEmptyValueEvents(lastPerGroupOld);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = CollectionUtil.toArrayNullForEmptyValueValues(newEventsSortKey);
            newEventsArr = orderByProcessor.sortWOrderKeys(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = CollectionUtil.toArrayNullForEmptyValueValues(oldEventsSortKey);
                oldEventsArr = orderByProcessor.sortWOrderKeys(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        return ResultSetProcessorUtil.toPairNullIfAllNull(newEventsArr, oldEventsArr);
    }

    private static void processOutputLimitedJoinLastCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedJoinPerKey = generateOutputBatchedJoinPerKeyCodegen(forge, classScope, instance);

        method.getBlock().declareVar(Map.class, "lastPerGroupNew", newInstance(LinkedHashMap.class))
                .declareVar(Map.class, "lastPerGroupOld", forge.isSelectRStream() ? newInstance(LinkedHashMap.class) : constantNull());

        method.getBlock().declareVar(Map.class, "newEventsSortKey", constantNull())
                .declareVar(Map.class, "oldEventsSortKey", constantNull());
        if (forge.isSorting()) {
            method.getBlock().assignRef("newEventsSortKey", newInstance(LinkedHashMap.class))
                    .assignRef("oldEventsSortKey", forge.isSelectRStream() ? newInstance(LinkedHashMap.class) : constantNull());
        }

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("oldData"), constantFalse()));

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedJoinPerKey, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("lastPerGroupOld"), ref("oldEventsSortKey"));
            }

            forEach.localMethod(generateOutputBatchedJoinPerKey, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("lastPerGroupNew"), ref("newEventsSortKey"));
        }

        method.getBlock().declareVar(EventBean[].class, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS, ref("lastPerGroupNew")))
                .declareVar(EventBean[].class, "oldEventsArr", forge.isSelectRStream() ? staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS, ref("lastPerGroupOld")) : constantNull());

        if (forge.isSorting()) {
            method.getBlock().declareVar(Object[].class, "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("newEventsSortKey")))
                    .assignRef("newEventsArr", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), REF_AGENTINSTANCECONTEXT));
            if (forge.isSelectRStream()) {
                method.getBlock().declareVar(Object[].class, "sortKeysOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("oldEventsSortKey")))
                        .assignRef("oldEventsArr", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("sortKeysOld"), REF_AGENTINSTANCECONTEXT));
            }
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private UniformPair<EventBean[]> processOutputLimitedJoinFirst(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        List<EventBean> newEvents = new ArrayList<>();
        List<Object> newEventsSortKey = null;

        if (orderByProcessor != null) {
            newEventsSortKey = new ArrayList<>();
        }

        Map<Object, EventBean[]> workCollection = new LinkedHashMap<>();

        if (prototype.getOptionalHavingNode() == null) {
            for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
                Set<MultiKey<EventBean>> newData = pair.getFirst();
                Set<MultiKey<EventBean>> oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeyArrayJoin(newData, true);
                Object[] oldDataMultiKey = generateGroupKeyArrayJoin(oldData, false);

                if (newData != null) {
                    // apply new data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> aNewData : newData) {
                        Object mk = newDataMultiKey[count];
                        EventBean[] eventsPerStream = aNewData.getArray();
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            workCollection.put(mk, eventsPerStream);
                        }
                        aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                        count++;
                    }
                }

                if (oldData != null) {
                    // apply new data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> aOldData : oldData) {
                        Object mk = oldDataMultiKey[count];
                        EventBean[] eventsPerStream = aOldData.getArray();
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            workCollection.put(mk, eventsPerStream);
                        }
                        aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                        count++;
                    }
                }

                // there is no remove stream currently for output first
                generateOutputBatchedAddToList(workCollection, false, generateSynthetic, newEvents, newEventsSortKey);
            }
        } else {
            // there is a having-clause, apply after aggregations
            for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
                Set<MultiKey<EventBean>> newData = pair.getFirst();
                Set<MultiKey<EventBean>> oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeyArrayJoin(newData, true);
                Object[] oldDataMultiKey = generateGroupKeyArrayJoin(oldData, false);

                ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey);

                if (newData != null) {
                    // check having clause and first-condition
                    int count = 0;
                    for (MultiKey<EventBean> aNewData : newData) {
                        Object mk = newDataMultiKey[count];
                        EventBean[] eventsPerStream = aNewData.getArray();
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, true, agentInstanceContext);
                        if (!passesHaving) {
                            count++;
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            workCollection.put(mk, eventsPerStream);
                        }
                        count++;
                    }
                }

                if (oldData != null) {
                    // apply new data to aggregates
                    int count = 0;
                    for (MultiKey<EventBean> aOldData : oldData) {
                        Object mk = oldDataMultiKey[count];
                        EventBean[] eventsPerStream = aOldData.getArray();
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, true, agentInstanceContext);
                        if (!passesHaving) {
                            count++;
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            workCollection.put(mk, eventsPerStream);
                        }
                    }
                }

                // there is no remove stream currently for output first
                generateOutputBatchedAddToList(workCollection, false, generateSynthetic, newEvents, newEventsSortKey);
            }
        }

        EventBean[] newEventsArr = null;
        if (!newEvents.isEmpty()) {
            newEventsArr = CollectionUtil.toArrayEvents(newEvents);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = CollectionUtil.toArrayNullForEmptyObjects(newEventsSortKey);
            newEventsArr = orderByProcessor.sortWOrderKeys(newEventsArr, sortKeysNew, agentInstanceContext);
        }

        return ResultSetProcessorUtil.toPairNullIfAllNull(newEventsArr, null);
    }

    private static void processOutputLimitedJoinFirstCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatchedAddToList = generateOutputBatchedAddToListCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        CodegenMember helperFactory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember outputFactory = classScope.makeAddMember(OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(member(helperFactory.getMemberId()), "makeRSGroupedOutputFirst", REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), member(outputFactory.getMemberId()), constantNull(), constant(-1)));

        method.getBlock().declareVar(List.class, "newEvents", newInstance(LinkedList.class));
        method.getBlock().declareVar(List.class, "newEventsSortKey", constantNull());
        if (forge.isSorting()) {
            method.getBlock().assignRef("newEventsSortKey", newInstance(LinkedList.class));
        }

        method.getBlock().declareVar(Map.class, "workCollection", newInstance(LinkedHashMap.class));

        if (forge.getOptionalHavingNode() == null) {
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("oldData"), constantFalse()));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKey.class, "aNewData", ref("newData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                        forloop.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                                .increment("count");
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKey.class, "aOldData", ref("oldData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aOldData"), "getArray")))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                        forloop.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                                .increment("count");
                    }
                }

                forEach.localMethod(generateOutputBatchedAddToList, ref("workCollection"), constantFalse(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
            }
        } else {
            // having clause present, having clause evaluates at the level of individual posts
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("oldData"), constantFalse()))
                        .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKey.class, "aNewData", ref("newData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), REF_AGENTINSTANCECONTEXT)))
                                .increment("count")
                                .blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                    }
                }

                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKey.class, "aOldData", ref("oldData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aOldData"), "getArray")))
                                .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), REF_AGENTINSTANCECONTEXT)))
                                .increment("count")
                                .blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                    }
                }

                forEach.localMethod(generateOutputBatchedAddToList, ref("workCollection"), constantFalse(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
            }
        }

        method.getBlock().declareVar(EventBean[].class, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYEVENTS, ref("newEvents")));

        if (forge.isSorting()) {
            method.getBlock().declareVar(Object[].class, "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYOBJECTS, ref("newEventsSortKey")))
                    .assignRef("newEventsArr", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), REF_AGENTINSTANCECONTEXT));
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), constantNull()));
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

        Map<Object, EventBean[]> workCollection = new LinkedHashMap<>();

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeyArrayJoin(newData, true);
            Object[] oldDataMultiKey = generateGroupKeyArrayJoin(oldData, false);

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            if (newData != null) {
                // apply new data to aggregates
                int count = 0;
                for (MultiKey<EventBean> aNewData : newData) {
                    Object mk = newDataMultiKey[count];
                    aggregationService.applyEnter(aNewData.getArray(), mk, agentInstanceContext);
                    count++;

                    // keep the new event as a representative for the group
                    workCollection.put(mk, aNewData.getArray());
                    outputAllGroupReps.put(mk, aNewData.getArray());
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                int count = 0;
                for (MultiKey<EventBean> anOldData : oldData) {
                    aggregationService.applyLeave(anOldData.getArray(), oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            if (prototype.isSelectRStream()) {
                generateOutputBatchedJoinUnkeyed(oldData, oldDataMultiKey, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }
            generateOutputBatchedJoinUnkeyed(newData, newDataMultiKey, true, generateSynthetic, newEvents, newEventsSortKey);
        }

        // For any group representatives not in the work collection, generate a row
        Iterator<Map.Entry<Object, EventBean[]>> entryIterator = outputAllGroupReps.entryIterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, EventBean[]> entry = entryIterator.next();
            if (!workCollection.containsKey(entry.getKey())) {
                generateOutputBatchedAddToListSingle(entry.getKey(), entry.getValue(), true, generateSynthetic, newEvents, newEventsSortKey);
            }
        }

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedJoinAllCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedJoinUnkeyed = generateOutputBatchedJoinUnkeyedCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputBatchedAddToListSingle = generateOutputBatchedAddToListSingleCodegen(forge, classScope, instance);

        CodegenMember helperFactory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(member(helperFactory.getMemberId()), "makeRSGroupedOutputAllNoOpt", REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "workCollection", newInstance(LinkedHashMap.class));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("oldData"), constantFalse()));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                        .declareVar(int.class, "count", constant(0));

                {
                    ifNewData.forEach(MultiKey.class, "aNewData", ref("newData"))
                            .declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                            .increment("count")
                            .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"))
                            .exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"));
                }

                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                        .declareVar(int.class, "count", constant(0));
                {
                    ifOldData.forEach(MultiKey.class, "anOldData", ref("oldData"))
                            .declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                            .increment("count");
                }
            }

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
            }
            forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
        }

        method.getBlock().declareVar(Iterator.class, "entryIterator", exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "entryIterator"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("entryIterator"), "hasNext"))
                    .declareVar(Map.Entry.class, "entry", cast(Map.Entry.class, exprDotMethod(ref("entryIterator"), "next")))
                    .ifCondition(not(exprDotMethod(ref("workCollection"), "containsKey", exprDotMethod(ref("entry"), "getKey"))))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        }

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

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeyArrayJoin(newData, true);
            Object[] oldDataMultiKey = generateGroupKeyArrayJoin(oldData, false);

            if (prototype.isUnidirectional()) {
                this.clear();
            }

            ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedJoinUnkeyed(oldData, oldDataMultiKey, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }
            generateOutputBatchedJoinUnkeyed(newData, newDataMultiKey, true, generateSynthetic, newEvents, newEventsSortKey);
        }

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedJoinDefaultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayJoin = generateGroupKeyArrayJoinCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedJoinUnkeyed = generateOutputBatchedJoinUnkeyedCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, ref("oldData"), constantFalse()));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
            }

            forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> processOutputLimitedViewLast(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        Map<Object, EventBean> lastPerGroupNew = new LinkedHashMap<>();
        Map<Object, EventBean> lastPerGroupOld = null;
        if (prototype.isSelectRStream()) {
            lastPerGroupOld = new LinkedHashMap<>();
        }

        Map<Object, Object> newEventsSortKey = null; // group key to sort key
        Map<Object, Object> oldEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new LinkedHashMap<>();
            if (prototype.isSelectRStream()) {
                oldEventsSortKey = new LinkedHashMap<>();
            }
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeyArrayView(newData, true);
            Object[] oldDataMultiKey = generateGroupKeyArrayView(oldData, false);

            ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey, eventsPerStream);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedViewPerKey(oldData, oldDataMultiKey, false, generateSynthetic, lastPerGroupOld, oldEventsSortKey, eventsPerStream);
            }
            generateOutputBatchedViewPerKey(newData, newDataMultiKey, true, generateSynthetic, lastPerGroupNew, newEventsSortKey, eventsPerStream);
        }

        EventBean[] newEventsArr = CollectionUtil.toArrayNullForEmptyValueEvents(lastPerGroupNew);
        EventBean[] oldEventsArr = null;
        if (prototype.isSelectRStream()) {
            oldEventsArr = CollectionUtil.toArrayNullForEmptyValueEvents(lastPerGroupOld);
        }

        if (orderByProcessor != null) {
            Object[] sortKeysNew = CollectionUtil.toArrayNullForEmptyValueValues(newEventsSortKey);
            newEventsArr = orderByProcessor.sortWOrderKeys(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                Object[] sortKeysOld = CollectionUtil.toArrayNullForEmptyValueValues(oldEventsSortKey);
                oldEventsArr = orderByProcessor.sortWOrderKeys(oldEventsArr, sortKeysOld, agentInstanceContext);
            }
        }

        return ResultSetProcessorUtil.toPairNullIfAllNull(newEventsArr, oldEventsArr);
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayView = generateGroupKeyArrayViewCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedViewPerKey = generateOutputBatchedViewPerKeyCodegen(forge, classScope, instance);

        method.getBlock().declareVar(Map.class, "lastPerGroupNew", newInstance(LinkedHashMap.class))
                .declareVar(Map.class, "lastPerGroupOld", forge.isSelectRStream() ? newInstance(LinkedHashMap.class) : constantNull());

        method.getBlock().declareVar(Map.class, "newEventsSortKey", constantNull())
                .declareVar(Map.class, "oldEventsSortKey", constantNull());
        if (forge.isSorting()) {
            method.getBlock().assignRef("newEventsSortKey", newInstance(LinkedHashMap.class))
                    .assignRef("oldEventsSortKey", forge.isSelectRStream() ? newInstance(LinkedHashMap.class) : constantNull());
        }

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayView, ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayView, ref("oldData"), constantFalse()));

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedViewPerKey, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("lastPerGroupOld"), ref("oldEventsSortKey"), ref("eventsPerStream"));
            }

            forEach.localMethod(generateOutputBatchedViewPerKey, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("lastPerGroupNew"), ref("newEventsSortKey"), ref("eventsPerStream"));
        }

        method.getBlock().declareVar(EventBean[].class, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS, ref("lastPerGroupNew")))
                .declareVar(EventBean[].class, "oldEventsArr", forge.isSelectRStream() ? staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS, ref("lastPerGroupOld")) : constantNull());

        if (forge.isSorting()) {
            method.getBlock().declareVar(Object[].class, "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("newEventsSortKey")))
                    .assignRef("newEventsArr", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), REF_AGENTINSTANCECONTEXT));
            if (forge.isSelectRStream()) {
                method.getBlock().declareVar(Object[].class, "sortKeysOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("oldEventsSortKey")))
                        .assignRef("oldEventsArr", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("sortKeysOld"), REF_AGENTINSTANCECONTEXT));
            }
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private UniformPair<EventBean[]> processOutputLimitedViewFirst(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        List<EventBean> newEvents = new ArrayList<>();
        List<Object> newEventsSortKey = null;

        if (orderByProcessor != null) {
            newEventsSortKey = new ArrayList<>();
        }

        Map<Object, EventBean[]> workCollection = new LinkedHashMap<>();
        EventBean[] eventsPerStream = new EventBean[1];

        if (prototype.getOptionalHavingNode() == null) {
            for (UniformPair<EventBean[]> pair : viewEventsList) {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeyArrayView(newData, true);
                Object[] oldDataMultiKey = generateGroupKeyArrayView(oldData, false);

                if (newData != null) {
                    // apply new data to aggregates
                    for (int i = 0; i < newData.length; i++) {
                        eventsPerStream[0] = newData[i];
                        Object mk = newDataMultiKey[i];
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            workCollection.put(mk, new EventBean[]{newData[i]});
                        }
                        aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                    }
                }

                if (oldData != null) {
                    // apply new data to aggregates
                    for (int i = 0; i < oldData.length; i++) {
                        eventsPerStream[0] = oldData[i];
                        Object mk = oldDataMultiKey[i];
                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            workCollection.put(mk, new EventBean[]{oldData[i]});
                        }
                        aggregationService.applyLeave(eventsPerStream, mk, agentInstanceContext);
                    }
                }

                // there is no remove stream currently for output first
                generateOutputBatchedAddToList(workCollection, false, generateSynthetic, newEvents, newEventsSortKey);
            }
        } else {  // has a having-clause
            for (UniformPair<EventBean[]> pair : viewEventsList) {
                EventBean[] newData = pair.getFirst();
                EventBean[] oldData = pair.getSecond();

                Object[] newDataMultiKey = generateGroupKeyArrayView(newData, true);
                Object[] oldDataMultiKey = generateGroupKeyArrayView(oldData, false);

                ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey, eventsPerStream);

                if (newData != null) {
                    // check having clause and first-condition
                    for (int i = 0; i < newData.length; i++) {
                        eventsPerStream[0] = newData[i];
                        Object mk = newDataMultiKey[i];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, true, agentInstanceContext);
                        if (!passesHaving) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            workCollection.put(mk, new EventBean[]{newData[i]});
                        }
                    }
                }

                if (oldData != null) {
                    // apply new data to aggregates
                    for (int i = 0; i < oldData.length; i++) {
                        eventsPerStream[0] = oldData[i];
                        Object mk = oldDataMultiKey[i];
                        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), null);

                        // Filter the having clause
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, true, agentInstanceContext);
                        if (!passesHaving) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelper.getOrAllocate(mk, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            workCollection.put(mk, new EventBean[]{oldData[i]});
                        }
                    }
                }

                // there is no remove stream currently for output first
                generateOutputBatchedAddToList(workCollection, false, generateSynthetic, newEvents, newEventsSortKey);
            }
        }

        EventBean[] newEventsArr = CollectionUtil.toArrayNullForEmptyEvents(newEvents);

        if (orderByProcessor != null) {
            Object[] sortKeysNew = CollectionUtil.toArrayNullForEmptyObjects(newEventsSortKey);
            newEventsArr = orderByProcessor.sortWOrderKeys(newEventsArr, sortKeysNew, agentInstanceContext);
        }

        return ResultSetProcessorUtil.toPairNullIfAllNull(newEventsArr, null);
    }

    private static void processOutputLimitedViewFirstCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatchedAddToList = generateOutputBatchedAddToListCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeyArrayView = generateGroupKeyArrayViewCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        CodegenMember helperFactory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember outputFactory = classScope.makeAddMember(OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(member(helperFactory.getMemberId()), "makeRSGroupedOutputFirst", REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), member(outputFactory.getMemberId()), constantNull(), constant(-1)));

        method.getBlock().declareVar(List.class, "newEvents", newInstance(LinkedList.class));
        method.getBlock().declareVar(List.class, "newEventsSortKey", constantNull());
        if (forge.isSorting()) {
            method.getBlock().assignRef("newEventsSortKey", newInstance(LinkedList.class));
        }

        method.getBlock().declareVar(Map.class, "workCollection", newInstance(LinkedHashMap.class))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        if (forge.getOptionalHavingNode() == null) {
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayView, ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayView, ref("oldData"), constantFalse()));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                                .declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.class, arrayAtIndex(ref("newData"), ref("i"))));
                        forloop.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forLoopIntSimple("i", arrayLength(ref("oldData")));
                        forloop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("oldData"), ref("i")))
                                .declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("i")))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.class, arrayAtIndex(ref("oldData"), ref("i"))));
                        forloop.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT);
                    }
                }

                forEach.localMethod(generateOutputBatchedAddToList, ref("workCollection"), constantFalse(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
            }
        } else {
            // having clause present, having clause evaluates at the level of individual posts
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayView, ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayView, ref("oldData"), constantFalse()))
                        .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                                .assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                                .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), REF_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.class, arrayAtIndex(ref("newData"), ref("i"))));
                    }
                }

                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forLoopIntSimple("i", arrayLength(ref("oldData")));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("i")))
                                .assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("oldData"), ref("i")))
                                .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), REF_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(ref(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.class, arrayAtIndex(ref("oldData"), ref("i"))));
                    }
                }

                forEach.localMethod(generateOutputBatchedAddToList, ref("workCollection"), constantFalse(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
            }
        }

        method.getBlock().declareVar(EventBean[].class, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYEVENTS, ref("newEvents")));

        if (forge.isSorting()) {
            method.getBlock().declareVar(Object[].class, "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYOBJECTS, ref("newEventsSortKey")))
                    .assignRef("newEventsArr", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), REF_AGENTINSTANCECONTEXT));
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), constantNull()));
    }

    private UniformPair<EventBean[]> processOutputLimitedViewAll(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
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

        Map<Object, EventBean[]> workCollection = new LinkedHashMap<>();
        EventBean[] eventsPerStream = new EventBean[1];

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeyArrayView(newData, true);
            Object[] oldDataMultiKey = generateGroupKeyArrayView(oldData, false);

            if (newData != null) {
                // apply new data to aggregates
                int count = 0;
                for (EventBean aNewData : newData) {
                    Object mk = newDataMultiKey[count];
                    eventsPerStream[0] = aNewData;
                    aggregationService.applyEnter(eventsPerStream, mk, agentInstanceContext);
                    count++;

                    // keep the new event as a representative for the group
                    workCollection.put(mk, eventsPerStream);
                    outputAllGroupReps.put(mk, new EventBean[]{aNewData});
                }
            }
            if (oldData != null) {
                // apply old data to aggregates
                int count = 0;
                for (EventBean anOldData : oldData) {
                    eventsPerStream[0] = anOldData;
                    aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[count], agentInstanceContext);
                    count++;
                }
            }

            if (prototype.isSelectRStream()) {
                generateOutputBatchedViewUnkeyed(oldData, oldDataMultiKey, false, generateSynthetic, oldEvents, oldEventsSortKey, eventsPerStream);
            }
            generateOutputBatchedViewUnkeyed(newData, newDataMultiKey, true, generateSynthetic, newEvents, newEventsSortKey, eventsPerStream);
        }

        // For any group representatives not in the work collection, generate a row
        Iterator<Map.Entry<Object, EventBean[]>> entryIterator = outputAllGroupReps.entryIterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, EventBean[]> entry = entryIterator.next();
            if (!workCollection.containsKey(entry.getKey())) {
                generateOutputBatchedAddToListSingle(entry.getKey(), entry.getValue(), true, generateSynthetic, newEvents, newEventsSortKey);
            }
        }

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedViewAllCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayView = generateGroupKeyArrayViewCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedViewUnkeyed = generateOutputBatchedViewUnkeyedCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputBatchedAddToListSingle = generateOutputBatchedAddToListSingleCodegen(forge, classScope, instance);

        CodegenMember helperFactory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(member(helperFactory.getMemberId()), "makeRSGroupedOutputAllNoOpt", REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "workCollection", newInstance(LinkedHashMap.class))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayView, ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayView, ref("oldData"), constantFalse()));

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                        .declareVar(int.class, "count", constant(0));

                {
                    ifNewData.forEach(EventBean.class, "aNewData", ref("newData"))
                            .declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                            .assignArrayElement(ref("eventsPerStream"), constant(0), ref("aNewData"))
                            .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                            .increment("count")
                            .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"))
                            .exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.class, ref("aNewData")));
                }

                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                        .declareVar(int.class, "count", constant(0));
                {
                    ifOldData.forEach(EventBean.class, "anOldData", ref("oldData"))
                            .declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                            .assignArrayElement(ref("eventsPerStream"), constant(0), ref("anOldData"))
                            .exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), REF_AGENTINSTANCECONTEXT)
                            .increment("count");
                }
            }

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedViewUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), ref("eventsPerStream"));
            }
            forEach.localMethod(generateOutputBatchedViewUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), ref("eventsPerStream"));
        }

        method.getBlock().declareVar(Iterator.class, "entryIterator", exprDotMethod(ref(NAME_OUTPUTALLGROUPREPS), "entryIterator"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("entryIterator"), "hasNext"))
                    .declareVar(Map.Entry.class, "entry", cast(Map.Entry.class, exprDotMethod(ref("entryIterator"), "next")))
                    .ifCondition(not(exprDotMethod(ref("workCollection"), "containsKey", exprDotMethod(ref("entry"), "getKey"))))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        }

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

        EventBean[] eventsPerStream = new EventBean[1];
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            Object[] newDataMultiKey = generateGroupKeyArrayView(newData, true);
            Object[] oldDataMultiKey = generateGroupKeyArrayView(oldData, false);

            ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey, eventsPerStream);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedViewUnkeyed(oldData, oldDataMultiKey, false, generateSynthetic, oldEvents, oldEventsSortKey, eventsPerStream);
            }
            generateOutputBatchedViewUnkeyed(newData, newDataMultiKey, true, generateSynthetic, newEvents, newEventsSortKey, eventsPerStream);
        }

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeyArrayView = generateGroupKeyArrayViewCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedViewUnkeyed = generateOutputBatchedViewUnkeyedCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayView, ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayView, ref("oldData"), constantFalse()));

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedViewUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), ref("eventsPerStream"));
            }

            forEach.localMethod(generateOutputBatchedViewUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), ref("eventsPerStream"));
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private void generateOutputBatchedAddToList(Map<Object, EventBean[]> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys) {
        for (Map.Entry<Object, EventBean[]> entry : keysAndEvents.entrySet()) {
            generateOutputBatchedAddToListSingle(entry.getKey(), entry.getValue(), isNewData, isSynthesize, resultEvents, optSortKeys);
        }
    }

    private static CodegenMethodNode generateOutputBatchedAddToListCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatchedAddToListSingle = generateOutputBatchedAddToListSingleCodegen(forge, classScope, instance);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("resultEvents"), ref("optSortKeys"));
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedAddToList",
                CodegenNamedParam.from(Map.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    private void generateOutputBatchedAddToListSingle(Object key, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys) {
        // Set the current row of aggregation states
        aggregationService.setCurrentAccess(key, agentInstanceContext.getAgentInstanceId(), null);

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

    private static CodegenMethodNode generateOutputBatchedAddToListSingleCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            {
                methodNode.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("key"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

                if (forge.getOptionalHavingNode() != null) {
                    methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockReturnNoValue();
                }

                methodNode.getBlock().exprDotMethod(ref("resultEvents"), "add", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    methodNode.getBlock().exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
                }
            }
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedAddToListSingle",
                CodegenNamedParam.from(Object.class, "key", EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    public void generateOutputBatchedViewUnkeyed(EventBean[] outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Collection<EventBean> resultEvents, List<Object> optSortKeys, EventBean[] eventsPerStream) {
        if (outputEvents == null) {
            return;
        }

        int count = 0;
        for (EventBean outputEvent : outputEvents) {
            aggregationService.setCurrentAccess(groupByKeys[count], agentInstanceContext.getAgentInstanceId(), null);
            eventsPerStream[0] = outputEvents[count];

            // Filter the having clause
            if (prototype.getOptionalHavingNode() != null) {
                boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
                if (!passesHaving) {
                    count++;
                    continue;
                }
            }

            resultEvents.add(selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));
            if (prototype.isSorting()) {
                optSortKeys.add(orderByProcessor.getSortKey(eventsPerStream, isNewData, agentInstanceContext));
            }

            count++;
        }
    }

    static CodegenMethodNode generateOutputBatchedViewUnkeyedCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(int.class, "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(EventBean.class, "outputEvent", ref("outputEvents"));
                forEach.exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("count")), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .assignArrayElement(ref("eventsPerStream"), constant(0), arrayAtIndex(ref("outputEvents"), ref("count")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT)))
                            .increment("count")
                            .blockContinue();
                }

                forEach.exprDotMethod(ref("resultEvents"), "add", exprDotMethod(REF_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
                }

                forEach.increment("count");
            }
        };
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedViewUnkeyed",
                CodegenNamedParam.from(EventBean[].class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Collection.class, "resultEvents", List.class, "optSortKeys", EventBean[].class, "eventsPerStream"), ResultSetProcessorAggregateGrouped.class, classScope, code);
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

    private UniformPair<EventBean[]> processViewResultPairDepthOne(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        Object newGroupKey = generateGroupKeySingle(newData, true);
        Object oldGroupKey = generateGroupKeySingle(oldData, false);
        aggregationService.applyEnter(newData, newGroupKey, agentInstanceContext);
        aggregationService.applyLeave(oldData, oldGroupKey, agentInstanceContext);
        EventBean istream = shortcutEvalGivenKey(newData, newGroupKey, true, isSynthesize);
        if (!prototype.isSelectRStream()) {
            return ResultSetProcessorUtil.toPairNullIfNullIStream(istream);
        }
        EventBean rstream = shortcutEvalGivenKey(oldData, oldGroupKey, false, isSynthesize);
        return ResultSetProcessorUtil.toPairNullIfAllNullSingle(istream, rstream);
    }

    private static CodegenMethodNode processViewResultPairDepthOneCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);
        CodegenMethodNode generateGroupKeySingle = ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().declareVar(Object.class, "newGroupKey", localMethod(generateGroupKeySingle, REF_NEWDATA, constantTrue()))
                    .declareVar(Object.class, "oldGroupKey", localMethod(generateGroupKeySingle, REF_OLDDATA, constantFalse()))
                    .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("newGroupKey"), REF_AGENTINSTANCECONTEXT)
                    .exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", REF_OLDDATA, ref("oldGroupKey"), REF_AGENTINSTANCECONTEXT)
                    .declareVar(EventBean.class, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("newGroupKey"), constantTrue(), REF_ISSYNTHESIZE));
            if (!forge.isSelectRStream()) {
                methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")));
            } else {
                methodNode.getBlock().declareVar(EventBean.class, "rstream", localMethod(shortcutEvalGivenKey, REF_OLDDATA, ref("oldGroupKey"), constantFalse(), REF_ISSYNTHESIZE))
                        .methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfAllNullSingle", ref("istream"), ref("rstream")));
            }
        };

        return instance.getMethods().addMethod(UniformPair.class, "processViewResultPairDepthOne", CodegenNamedParam.from(EventBean[].class, NAME_NEWDATA, EventBean[].class, NAME_OLDDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private UniformPair<EventBean[]> processViewResultNewDepthOne(EventBean[] newData, boolean isSynthesize) {
        Object groupKey = generateGroupKeySingle(newData, true);
        aggregationService.applyEnter(newData, groupKey, agentInstanceContext);
        EventBean istream = shortcutEvalGivenKey(newData, groupKey, true, isSynthesize);
        return ResultSetProcessorUtil.toPairNullIfNullIStream(istream);
    }

    private static CodegenMethodNode processViewResultNewDepthOneCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);
        CodegenMethodNode generateGroupKeySingle = ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock()
                    .declareVar(Object.class, "groupKey", localMethod(generateGroupKeySingle, REF_NEWDATA, constantTrue()))
                    .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("groupKey"), REF_AGENTINSTANCECONTEXT)
                    .declareVar(EventBean.class, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("groupKey"), constantTrue(), REF_ISSYNTHESIZE))
                    .methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")));
        };

        return instance.getMethods().addMethod(UniformPair.class, "processViewResultNewDepthOneCodegen", CodegenNamedParam.from(EventBean[].class, NAME_NEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private EventBean shortcutEvalGivenKey(EventBean[] eventsPerStream, Object groupKey, boolean isNewData, boolean isSynthesize) {
        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), null);

        if (prototype.getOptionalHavingNode() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getOptionalHavingNode(), eventsPerStream, isNewData, agentInstanceContext);
            if (!passesHaving) {
                return null;
            }
        }
        return selectExprProcessor.process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext);
    }

    private static CodegenMethodNode shortcutEvalGivenKeyCodegen(ExprForge optionalHavingNode, CodegenClassScope classScope, CodegenInstanceAux instance) {
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
}
