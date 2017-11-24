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
package com.espertech.esper.epl.core.resultset.rowpergrouprollup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenInstanceAux;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupKey;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputHelperVisitor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroupImpl;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.view.OutputConditionPolled;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil.METHOD_ITERATORTODEQUE;
import static com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil.METHOD_TOPAIRNULLIFALLNULL;
import static com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedUtil.*;
import static com.espertech.esper.epl.core.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupUtil.*;
import static com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames.REF_EPS;
import static com.espertech.esper.util.CollectionUtil.*;

public class ResultSetProcessorRowPerGroupRollupImpl implements ResultSetProcessorRowPerGroupRollup {
    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";
    private final static String NAME_OUTPUTFIRSTHELPERS = "outputFirstHelpers";
    private final static String NAME_EVENTPERGROUPBUFJOIN = "eventPerGroupBufJoin";
    private final static String NAME_EVENTPERGROUPBUFVIEW = "eventPerGroupBufView";
    private final static String NAME_GROUPREPSPERLEVELBUF = "groupRepsPerLevelBuf";
    private final static String NAME_RSTREAMEVENTSORTARRAYBUF = "rstreamEventSortArrayBuf";

    protected final ResultSetProcessorRowPerGroupRollupFactory prototype;
    protected final OrderByProcessor orderByProcessor;
    protected final AggregationService aggregationService;
    protected AgentInstanceContext agentInstanceContext;

    private final Map<Object, EventBean[]>[] groupRepsPerLevelBuf;
    private final Map<Object, EventBean>[] eventPerGroupBufView;
    private final Map<Object, EventBean[]>[] eventPerGroupBufJoin;
    private final EventArrayAndSortKeyArray rstreamEventSortArrayBuf;

    private final ResultSetProcessorRowPerGroupRollupOutputLastHelper outputLastHelper;
    private final ResultSetProcessorRowPerGroupRollupOutputAllHelper outputAllHelper;
    private final ResultSetProcessorGroupedOutputFirstHelper[] outputFirstHelpers;

    ResultSetProcessorRowPerGroupRollupImpl(ResultSetProcessorRowPerGroupRollupFactory prototype, OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.orderByProcessor = orderByProcessor;
        this.aggregationService = aggregationService;
        this.agentInstanceContext = agentInstanceContext;
        aggregationService.setRemovedCallback(this);

        int levelCount = prototype.getGroupByRollupDesc().getLevels().length;

        if (prototype.isJoin()) {
            eventPerGroupBufJoin = (LinkedHashMap<Object, EventBean[]>[]) new LinkedHashMap[levelCount];
            for (int i = 0; i < levelCount; i++) {
                eventPerGroupBufJoin[i] = new LinkedHashMap<>();
            }
            eventPerGroupBufView = null;
        } else {
            eventPerGroupBufView = (LinkedHashMap<Object, EventBean>[]) new LinkedHashMap[levelCount];
            for (int i = 0; i < levelCount; i++) {
                eventPerGroupBufView[i] = new LinkedHashMap<>();
            }
            eventPerGroupBufJoin = null;
        }

        if (prototype.getOutputLimitSpec() != null) {
            groupRepsPerLevelBuf = makeGroupRepsPerLevelBuf(levelCount);

            if (prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
                outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupRollupLast(agentInstanceContext, this, prototype.getGroupKeyTypes(), prototype.getNumStreams());
                outputAllHelper = null;
            } else if (prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
                outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSRowPerGroupRollupAll(agentInstanceContext, this, prototype.getGroupKeyTypes(), prototype.getNumStreams());
                outputLastHelper = null;
            } else {
                outputLastHelper = null;
                outputAllHelper = null;
            }
        } else {
            groupRepsPerLevelBuf = null;
            outputLastHelper = null;
            outputAllHelper = null;
        }

        // Allocate output state for output-first
        if (prototype.getOutputLimitSpec() != null && prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.FIRST) {
            outputFirstHelpers = ResultSetProcessorRowPerGroupRollupUtil.initializeOutputFirstHelpers(prototype.getResultSetProcessorHelperFactory(), agentInstanceContext, prototype.getGroupKeyTypes(), prototype.getGroupByRollupDesc(), prototype.getOptionalOutputFirstConditionFactory());
        } else {
            outputFirstHelpers = null;
        }

        if (prototype.getOutputLimitSpec() != null && (prototype.isSelectRStream() || prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.FIRST)) {
            rstreamEventSortArrayBuf = makeRStreamSortedArrayBuf(prototype.getGroupByRollupDesc().getLevels().length, orderByProcessor != null);
        } else {
            rstreamEventSortArrayBuf = null;
        }
    }

    public void setAgentInstanceContext(AgentInstanceContext agentInstanceContext) {
        this.agentInstanceContext = agentInstanceContext;
    }

    public AggregationService getAggregationService() {
        return aggregationService;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();
        }

        if (prototype.isUnidirectional()) {
            this.clear();
        }

        resetEventPerGroupBufJoin();
        Object[][] newDataMultiKey = generateGroupKeysJoin(newEvents, eventPerGroupBufJoin, true);
        Object[][] oldDataMultiKey = generateGroupKeysJoin(oldEvents, eventPerGroupBufJoin, false);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsJoin(eventPerGroupBufJoin, false, isSynthesize);
        }

        ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newEvents, newDataMultiKey, oldEvents, oldDataMultiKey);

        // generate new events using select expressions
        EventBean[] selectNewEvents = generateOutputEventsJoin(eventPerGroupBufJoin, true, isSynthesize);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);
        }
        return ResultSetProcessorUtil.toPairNullIfAllNull(selectNewEvents, selectOldEvents);
    }

    static void processJoinResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        addEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFJOIN, forge, instance, method, classScope);
        CodegenMethodNode resetEventPerGroupJoinBuf = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFJOIN, classScope, instance);
        CodegenMethodNode generateGroupKeysJoin = generateGroupKeysJoinCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }

        method.getBlock().localMethod(resetEventPerGroupJoinBuf)
                .declareVar(Object[][].class, "newDataMultiKey", localMethod(generateGroupKeysJoin, REF_NEWDATA, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue()))
                .declareVar(Object[][].class, "oldDataMultiKey", localMethod(generateGroupKeysJoin, REF_OLDDATA, ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse()))
                .declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessGroupedRowPerGroup();
        }

        resetEventPerGroupBufView();
        Object[][] newDataMultiKey = generateGroupKeysView(newData, eventPerGroupBufView, true);
        Object[][] oldDataMultiKey = generateGroupKeysView(oldData, eventPerGroupBufView, false);

        EventBean[] selectOldEvents = null;
        if (prototype.isSelectRStream()) {
            selectOldEvents = generateOutputEventsView(eventPerGroupBufView, false, isSynthesize);
        }

        EventBean[] eventsPerStream = new EventBean[1];
        ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey, eventsPerStream);

        EventBean[] selectNewEvents = generateOutputEventsView(eventPerGroupBufView, true, isSynthesize);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessGroupedRowPerGroup(selectNewEvents, selectOldEvents);
        }
        return ResultSetProcessorUtil.toPairNullIfAllNull(selectNewEvents, selectOldEvents);
    }

    static void processViewResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        addEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, forge, instance, method, classScope);
        CodegenMethodNode resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);
        CodegenMethodNode generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        method.getBlock().localMethod(resetEventPerGroupBufView)
                .declareVar(Object[][].class, "newDataMultiKey", localMethod(generateGroupKeysView, REF_NEWDATA, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue()))
                .declareVar(Object[][].class, "oldDataMultiKey", localMethod(generateGroupKeysView, REF_OLDDATA, ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse()))
                .declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"), ref("eventsPerStream"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    private static void addEventPerGroupBufCodegen(String memberName, ResultSetProcessorRowPerGroupRollupForge forge, CodegenInstanceAux instance, CodegenMethodNode method, CodegenClassScope classScope) {
        if (!instance.hasMember(memberName)) {
            CodegenMethodNode init = method.makeChild(LinkedHashMap[].class, ResultSetProcessorRowPerGroupRollupImpl.class, classScope);
            instance.addMember(memberName, LinkedHashMap[].class);
            instance.getServiceCtor().getBlock().assignRef(memberName, localMethod(init));
            int levelCount = forge.getGroupByRollupDesc().getLevels().length;
            init.getBlock().declareVar(LinkedHashMap[].class, memberName, newArrayByLength(LinkedHashMap.class, constant(levelCount)))
                    .forLoopIntSimple("i", constant(levelCount))
                    .assignArrayElement(memberName, ref("i"), newInstance(LinkedHashMap.class))
                    .blockEnd()
                    .methodReturn(ref(memberName));
        }
    }

    EventBean[] generateOutputEventsView(Map<Object, EventBean>[] keysAndEvents, boolean isNewData, boolean isSynthesize) {
        EventBean[] eventsPerStream = new EventBean[1];
        ArrayList<EventBean> events = new ArrayList<>(1);
        List<GroupByRollupKey> currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new ArrayList<>(1);
        }

        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        SelectExprProcessor[] selectExprProcessors = prototype.getPerLevelExpression().getSelectExprProcessor();
        ExprEvaluator[] optionalHavingClauses = prototype.getPerLevelExpression().getOptionalHavingNodes();
        for (AggregationGroupByRollupLevel level : levels) {
            for (Map.Entry<Object, EventBean> entry : keysAndEvents[level.getLevelNumber()].entrySet()) {
                Object groupKey = entry.getKey();

                // Set the current row of aggregation states
                aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                eventsPerStream[0] = entry.getValue();

                // Filter the having clause
                if (optionalHavingClauses != null) {
                    boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(optionalHavingClauses[level.getLevelNumber()], eventsPerStream, isNewData, agentInstanceContext);
                    if (!passesHaving) {
                        continue;
                    }
                }
                events.add(selectExprProcessors[level.getLevelNumber()].process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

                if (prototype.isSorting()) {
                    EventBean[] currentEventsPerStream = new EventBean[]{entry.getValue()};
                    currentGenerators.add(new GroupByRollupKey(currentEventsPerStream, level, groupKey));
                }
            }
        }

        if (events.isEmpty()) {
            return null;
        }
        EventBean[] outgoing = CollectionUtil.toArrayEvents(events);
        if (outgoing.length > 1 && prototype.isSorting()) {
            return orderByProcessor.sortRollup(outgoing, currentGenerators, isNewData, agentInstanceContext, aggregationService);
        }
        return outgoing;
    }

    static CodegenMethodNode generateOutputEventsViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {

            methodNode.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                    .declareVar(ArrayList.class, "events", newInstance(ArrayList.class, constant(1)))
                    .declareVar(List.class, "currentGenerators", forge.isSorting() ? newInstance(ArrayList.class, constant(1)) : constantNull())
                    .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            {
                CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", ref("levels"));
                {
                    CodegenBlock forEvents = forLevels.forEach(Map.Entry.class, "entry", exprDotMethod(arrayAtIndex(ref("keysAndEvents"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                    forEvents.declareVar(Object.class, "groupKey", exprDotMethod(ref("entry"), "getKey"))
                            .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                            .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.class, exprDotMethod(ref("entry"), "getValue")));

                    if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                        CodegenExpression having = arrayAtIndex(REF_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
                        forEvents.ifCondition(not(exprDotMethod(having, "evaluateHaving", REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                    }

                    forEvents.exprDotMethod(ref("events"), "add", exprDotMethod(arrayAtIndex(REF_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

                    if (forge.isSorting()) {
                        forEvents.declareVar(EventBean[].class, "currentEventsPerStream", newArrayWithInit(EventBean.class, cast(EventBean.class, exprDotMethod(ref("entry"), "getValue"))))
                                .exprDotMethod(ref("currentGenerators"), "add", newInstance(GroupByRollupKey.class, ref("currentEventsPerStream"), ref("level"), ref("groupKey")));
                    }
                }
            }

            methodNode.getBlock().ifCondition(exprDotMethod(ref("events"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(EventBean[].class, "outgoing", staticMethod(CollectionUtil.class, METHOD_TOARRAYEVENTS, ref("events")));
            if (forge.isSorting()) {
                methodNode.getBlock().ifCondition(relational(arrayLength(ref("outgoing")), GT, constant(1)))
                        .blockReturn(exprDotMethod(REF_ORDERBYPROCESSOR, "sortRollup", ref("outgoing"), ref("currentGenerators"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT, REF_AGGREGATIONSVC));
            }
            methodNode.getBlock().methodReturn(ref("outgoing"));
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsView",
                CodegenNamedParam.from(Map[].class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private EventBean[] generateOutputEventsJoin(Map<Object, EventBean[]>[] eventPairs, boolean isNewData, boolean synthesize) {
        ArrayList<EventBean> events = new ArrayList<EventBean>(1);
        List<GroupByRollupKey> currentGenerators = null;
        if (prototype.isSorting()) {
            currentGenerators = new ArrayList<>(1);
        }

        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        SelectExprProcessor[] selectExprProcessors = prototype.getPerLevelExpression().getSelectExprProcessor();
        ExprEvaluator[] optionalHavingClauses = prototype.getPerLevelExpression().getOptionalHavingNodes();
        for (AggregationGroupByRollupLevel level : levels) {
            for (Map.Entry<Object, EventBean[]> entry : eventPairs[level.getLevelNumber()].entrySet()) {
                Object groupKey = entry.getKey();

                // Set the current row of aggregation states
                aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);

                // Filter the having clause
                if (optionalHavingClauses != null) {
                    boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(optionalHavingClauses[level.getLevelNumber()], entry.getValue(), isNewData, agentInstanceContext);
                    if (!passesHaving) {
                        continue;
                    }
                }
                events.add(selectExprProcessors[level.getLevelNumber()].process(entry.getValue(), isNewData, synthesize, agentInstanceContext));

                if (prototype.isSorting()) {
                    currentGenerators.add(new GroupByRollupKey(entry.getValue(), level, groupKey));
                }
            }
        }

        if (events.isEmpty()) {
            return null;
        }
        EventBean[] outgoing = events.toArray(new EventBean[events.size()]);
        if (outgoing.length > 1 && prototype.isSorting()) {
            return orderByProcessor.sortRollup(outgoing, currentGenerators, isNewData, agentInstanceContext, aggregationService);
        }
        return outgoing;
    }

    private static CodegenMethodNode generateOutputEventsJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {

            methodNode.getBlock().declareVar(ArrayList.class, "events", newInstance(ArrayList.class, constant(1)))
                    .declareVar(List.class, "currentGenerators", forge.isSorting() ? newInstance(ArrayList.class, constant(1)) : constantNull())
                    .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            {
                CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", ref("levels"));
                {
                    CodegenBlock forEvents = forLevels.forEach(Map.Entry.class, "entry", exprDotMethod(arrayAtIndex(ref("eventPairs"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                    forEvents.declareVar(Object.class, "groupKey", exprDotMethod(ref("entry"), "getKey"))
                            .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")));

                    if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                        CodegenExpression having = arrayAtIndex(REF_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
                        forEvents.ifCondition(not(exprDotMethod(having, "evaluateHaving", REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockContinue();
                    }

                    forEvents.exprDotMethod(ref("events"), "add", exprDotMethod(arrayAtIndex(REF_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

                    if (forge.isSorting()) {
                        forEvents.exprDotMethod(ref("currentGenerators"), "add", newInstance(GroupByRollupKey.class, ref("eventsPerStream"), ref("level"), ref("groupKey")));
                    }
                }
            }

            methodNode.getBlock().ifCondition(exprDotMethod(ref("events"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(EventBean[].class, "outgoing", staticMethod(CollectionUtil.class, METHOD_TOARRAYEVENTS, ref("events")));
            if (forge.isSorting()) {
                methodNode.getBlock().ifCondition(relational(arrayLength(ref("outgoing")), GT, constant(1)))
                        .blockReturn(exprDotMethod(REF_ORDERBYPROCESSOR, "sortRollup", ref("outgoing"), ref("currentGenerators"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT, REF_AGGREGATIONSVC));
            }
            methodNode.getBlock().methodReturn(ref("outgoing"));
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsJoin",
                CodegenNamedParam.from(Map[].class, "eventPairs", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        if (!prototype.isHistoricalOnly()) {
            return obtainIterator(parent);
        }

        aggregationService.clearResults(agentInstanceContext);
        Iterator<EventBean> it = parent.iterator();
        EventBean[] eventsPerStream = new EventBean[1];
        Object[] groupKeys = new Object[prototype.getGroupByRollupDesc().getLevels().length];
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        while (it.hasNext()) {
            eventsPerStream[0] = it.next();
            Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
            for (int j = 0; j < levels.length; j++) {
                Object subkey = levels[j].computeSubkey(groupKeyComplete);
                groupKeys[j] = subkey;
            }
            aggregationService.applyEnter(eventsPerStream, groupKeys, agentInstanceContext);
        }

        ArrayDeque<EventBean> deque = ResultSetProcessorUtil.iteratorToDeque(obtainIterator(parent));
        aggregationService.clearResults(agentInstanceContext);
        return deque.iterator();
    }

    static void getIteratorViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE));
            return;
        }

        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        method.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT)
                .declareVar(Iterator.class, "it", exprDotMethod(REF_VIEWABLE, "iterator"))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(Object[].class, "groupKeys", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)))
                .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("it"), "hasNext"))
                    .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.class, exprDotMethod(ref("it"), "next")))
                    .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()))
                    .forLoopIntSimple("j", arrayLength(ref("levels")))
                    .declareVar(Object.class, "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                    .assignArrayElement("groupKeys", ref("j"), ref("subkey"))
                    .blockEnd()
                    .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeys"), REF_AGENTINSTANCECONTEXT)
                    .blockEnd();
        }

        method.getBlock().declareVar(ArrayDeque.class, "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE)))
                .exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private Iterator<EventBean> obtainIterator(Viewable parent) {
        resetEventPerGroupBufView();
        EventBean[] events = CollectionUtil.iteratorToArrayEvents(parent.iterator());
        generateGroupKeysView(events, eventPerGroupBufView, true);
        EventBean[] output = generateOutputEventsView(eventPerGroupBufView, true, true);
        return new ArrayEventIterator(output);
    }

    private static CodegenMethodNode obtainIteratorCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode parent, CodegenInstanceAux instance) {
        CodegenMethodNode resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);
        CodegenMethodNode generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        CodegenMethodNode iterator = parent.makeChild(Iterator.class, ResultSetProcessorRowPerGroupRollupImpl.class, classScope).addParam(Viewable.class, NAME_VIEWABLE);
        iterator.getBlock().localMethod(resetEventPerGroupBufView)
                .declareVar(EventBean[].class, "events", staticMethod(CollectionUtil.class, METHOD_ITERATORTOARRAYEVENTS, exprDotMethod(REF_VIEWABLE, "iterator")))
                .localMethod(generateGroupKeysView, ref("events"), ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue())
                .declareVar(EventBean[].class, "output", localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("output")));
        return iterator;
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        resetEventPerGroupBufJoin();
        generateGroupKeysJoin(joinSet, eventPerGroupBufJoin, true);
        EventBean[] output = generateOutputEventsJoin(eventPerGroupBufJoin, true, true);
        return new ArrayEventIterator(output);
    }

    static void getIteratorJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeysJoin = generateGroupKeysJoinCodegen(forge, classScope, instance);
        CodegenMethodNode generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);
        CodegenMethodNode resetEventPerGroupBuf = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFJOIN, classScope, instance);
        method.getBlock().localMethod(resetEventPerGroupBuf)
                .localMethod(generateGroupKeysJoin, REF_JOINSET, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue())
                .declareVar(EventBean[].class, "output", localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("output")));
    }

    public void clear() {
        aggregationService.clearResults(agentInstanceContext);
    }

    static void clearMethodCodegen(CodegenMethodNode method) {
        method.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "clearResults", REF_AGENTINSTANCECONTEXT);
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        OutputLimitLimitType outputLimitLimitType = prototype.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return handleOutputLimitDefaultJoin(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return handleOutputLimitAllJoin(joinEventsSet, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return handleOutputLimitFirstJoin(joinEventsSet, generateSynthetic);
        }
        return handleOutputLimitLastJoin(joinEventsSet, generateSynthetic);
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        OutputLimitLimitType outputLimitLimitType = forge.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            handleOutputLimitDefaultJoinCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            handleOutputLimitAllJoinCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            handleOutputLimitFirstJoinCodegen(forge, classScope, method, instance);
            return;
        }
        handleOutputLimitLastJoinCodegen(forge, classScope, method, instance);
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        OutputLimitLimitType outputLimitLimitType = prototype.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            return handleOutputLimitDefaultView(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            return handleOutputLimitAllView(viewEventsList, generateSynthetic);
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            return handleOutputLimitFirstView(viewEventsList, generateSynthetic);
        }
        return handleOutputLimitLastView(viewEventsList, generateSynthetic);
    }

    static void processOutputLimitedViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        OutputLimitLimitType outputLimitLimitType = forge.getOutputLimitSpec().getDisplayLimit();
        if (outputLimitLimitType == OutputLimitLimitType.DEFAULT) {
            handleOutputLimitDefaultViewCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.ALL) {
            handleOutputLimitAllViewCodegen(forge, classScope, method, instance);
            return;
        } else if (outputLimitLimitType == OutputLimitLimitType.FIRST) {
            handleOutputLimitFirstViewCodegen(forge, classScope, method, instance);
            return;
        }
        handleOutputLimitLastViewCodegen(forge, classScope, method, instance);
    }

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        if (outputLastHelper != null) {
            visitor.visit(outputLastHelper);
        }
        if (outputAllHelper != null) {
            visitor.visit(outputAllHelper);
        }
        if (outputFirstHelpers != null) {
            for (ResultSetProcessorGroupedOutputFirstHelper helper : outputFirstHelpers) {
                visitor.visit(helper);
            }
        }
    }

    static void acceptHelperVisitorCodegen(CodegenMethodNode method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTLASTHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", ref(NAME_OUTPUTALLHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPERS)) {
            method.getBlock().forEach(ResultSetProcessorGroupedOutputFirstHelper.class, "helper", ref(NAME_OUTPUTFIRSTHELPERS))
                    .exprDotMethod(REF_RESULTSETVISITOR, "visit", ref("helper"));
        }
    }

    private UniformPair<EventBean[]> handleOutputLimitFirstView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {

        for (Map<Object, EventBean[]> aGroupRepsView : groupRepsPerLevelBuf) {
            aGroupRepsView.clear();
        }

        rstreamEventSortArrayBuf.reset();

        int count;
        if (prototype.getPerLevelExpression().getOptionalHavingNodes() == null) {
            count = handleOutputLimitFirstViewNoHaving(viewEventsList, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
        } else {
            count = handleOutputLimitFirstViewHaving(viewEventsList, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, count);
    }

    private static void handleOutputLimitFirstViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenMethodNode generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().forEach(Map.class, "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");

        method.getBlock().declareVarNoInit(int.class, "count");
        if (forge.getPerLevelForges().getOptionalHavingForges() == null) {
            CodegenMethodNode handleOutputLimitFirstViewNoHaving = handleOutputLimitFirstViewNoHavingCodegen(forge, classScope, instance);
            method.getBlock().assignRef("count", localMethod(handleOutputLimitFirstViewNoHaving, REF_VIEWEVENTSLIST, REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel")));
        } else {
            CodegenMethodNode handleOutputLimitFirstViewHaving = handleOutputLimitFirstViewHavingCodegen(forge, classScope, instance);
            method.getBlock().assignRef("count", localMethod(handleOutputLimitFirstViewHaving, REF_VIEWEVENTSLIST, REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel")));
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private UniformPair<EventBean[]> handleOutputLimitFirstJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {

        for (Map<Object, EventBean[]> aGroupRepsView : groupRepsPerLevelBuf) {
            aGroupRepsView.clear();
        }

        rstreamEventSortArrayBuf.reset();

        int count;
        if (prototype.getPerLevelExpression().getOptionalHavingNodes() == null) {
            count = handleOutputLimitFirstJoinNoHaving(joinEventsSet, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
        } else {
            count = handleOutputLimitFirstJoinHaving(joinEventsSet, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, count);
    }

    private static void handleOutputLimitFirstJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenMethodNode generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().forEach(Map.class, "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");

        method.getBlock().declareVarNoInit(int.class, "count");
        if (forge.getPerLevelForges().getOptionalHavingForges() == null) {
            CodegenMethodNode handleOutputLimitFirstJoinNoHaving = handleOutputLimitFirstJoinNoHavingCodegen(forge, classScope, instance);
            method.getBlock().assignRef("count", localMethod(handleOutputLimitFirstJoinNoHaving, REF_JOINEVENTSSET, REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel")));
        } else {
            CodegenMethodNode handleOutputLimitFirstJoinHaving = handleOutputLimitFirstJoinHavingCodegen(forge, classScope, instance);
            method.getBlock().assignRef("count", localMethod(handleOutputLimitFirstJoinHaving, REF_JOINEVENTSSET, REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel")));
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private int handleOutputLimitFirstViewHaving(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {
        int count = 0;

        ExprEvaluator[] havingPerLevel = prototype.getPerLevelExpression().getOptionalHavingNodes();

        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            EventBean[] eventsPerStream;

            if (newData != null) {
                for (EventBean aNewData : newData) {
                    eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }

            if (newData != null) {
                for (EventBean aNewData : newData) {
                    eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);

                        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(havingPerLevel[level.getLevelNumber()], eventsPerStream, true, agentInstanceContext);
                        if (!passesHaving) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, true, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);

                        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(havingPerLevel[level.getLevelNumber()], eventsPerStream, false, agentInstanceContext);
                        if (!passesHaving) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, false, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    private static CodegenMethodNode handleOutputLimitFirstViewHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethodNode generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);
        CodegenMember outputFactory = classScope.makeAddMember(OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory());
        initOutputFirstHelpers(instance, forge, classScope, outputFactory);

        Consumer<CodegenMethodNode> code = methodNode -> {

            methodNode.getBlock().declareVar(int.class, "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "groupKeysPerLevel", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)))
                        .declareVarNoInit(EventBean[].class, "eventsPerStream");

                {
                    CodegenBlock ifNewApplyAgg = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNew = ifNewApplyAgg.forEach(EventBean.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("aNewData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                        {
                            forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(EventBean.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                        {
                            forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifNewFirst = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNewFirst = ifNewFirst.forEach(EventBean.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("aNewData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forNewFirst.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(REF_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantTrue(), REF_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(ref(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                            CodegenBlock passBlock = eachlvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .increment("count");
                            }
                        }
                    }

                    CodegenBlock ifOldFirst = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOldFirst = ifOldFirst.forEach(EventBean.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forOldFirst.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(REF_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantFalse(), REF_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(ref(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                            CodegenBlock passBlock = eachlvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .increment("count");
                            }
                        }
                    }
                }
            }

            methodNode.getBlock().methodReturn(ref("count"));
        };

        return instance.getMethods().addMethod(int.class, "handleOutputLimitFirstViewHaving",
                CodegenNamedParam.from(List.class, "viewEventsList", boolean.class, NAME_ISSYNTHESIZE, List[].class, "oldEventsPerLevel", List[].class, "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private int handleOutputLimitFirstJoinNoHaving(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventSet, boolean generateSynthetic, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {

        int oldEventCount = 0;
        EventBean[] eventsPerStream;

        // outer loop is the events
        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    eventsPerStream = aNewData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedGivenArray(true, groupKey, level, eventsPerStream, true, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    eventsPerStream = anOldData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedGivenArray(true, groupKey, level, eventsPerStream, false, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    oldEventCount++;
                                }
                            }
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }
        return oldEventCount;
    }

    private static CodegenMethodNode handleOutputLimitFirstJoinNoHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethodNode generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenMember outputFactory = classScope.makeAddMember(OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory());
        initOutputFirstHelpers(instance, forge, classScope, outputFactory);

        Consumer<CodegenMethodNode> code = methodNode -> {

            methodNode.getBlock().declareVar(int.class, "count", constant(0))
                    .declareVarNoInit(EventBean[].class, "eventsPerStream");

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "groupKeysPerLevel", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)));

                {
                    CodegenBlock ifNewApplyAgg = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNew = ifNewApplyAgg.forEach(MultiKey.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock forLvl = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(ref(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                            CodegenBlock passBlock = forLvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .increment("count");
                            }
                        }
                        forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(MultiKey.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                        {
                            CodegenBlock forLvl = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(ref(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                            CodegenBlock passBlock = forLvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .increment("count");
                            }
                        }
                        forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                    }
                }
            }

            methodNode.getBlock().methodReturn(ref("count"));
        };

        return instance.getMethods().addMethod(int.class, "handleOutputLimitFirstJoinNoHaving",
                CodegenNamedParam.from(List.class, NAME_JOINEVENTSSET, boolean.class, NAME_ISSYNTHESIZE, List[].class, "oldEventsPerLevel", List[].class, "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private int handleOutputLimitFirstJoinHaving(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventSet, boolean generateSynthetic, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {
        int count = 0;
        ExprEvaluator[] havingPerLevel = prototype.getPerLevelExpression().getOptionalHavingNodes();
        EventBean[] eventsPerStream;

        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    eventsPerStream = aNewData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    eventsPerStream = anOldData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }

            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    eventsPerStream = aNewData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);

                        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(havingPerLevel[level.getLevelNumber()], eventsPerStream, true, agentInstanceContext);
                        if (!passesHaving) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, true, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    eventsPerStream = anOldData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);

                        aggregationService.setCurrentAccess(groupKey, agentInstanceContext.getAgentInstanceId(), level);
                        boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(havingPerLevel[level.getLevelNumber()], eventsPerStream, false, agentInstanceContext);
                        if (!passesHaving) {
                            continue;
                        }

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, false, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    private static CodegenMethodNode handleOutputLimitFirstJoinHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethodNode generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenMember outputFactory = classScope.makeAddMember(OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory());
        initOutputFirstHelpers(instance, forge, classScope, outputFactory);

        Consumer<CodegenMethodNode> code = methodNode -> {

            methodNode.getBlock().declareVar(int.class, "count", constant(0))
                    .declareVarNoInit(EventBean[].class, "eventsPerStream");

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "groupKeysPerLevel", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)));
                {
                    CodegenBlock ifNewApplyAgg = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNew = ifNewApplyAgg.forEach(MultiKey.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                        {
                            forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(MultiKey.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                        {
                            forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifNewFirst = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNewFirst = ifNewFirst.forEach(MultiKey.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forNewFirst.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(REF_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantTrue(), REF_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(ref(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                            CodegenBlock passBlock = eachlvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .increment("count");
                            }
                        }
                    }

                    CodegenBlock ifOldFirst = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOldFirst = ifOldFirst.forEach(MultiKey.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forOldFirst.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(REF_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantFalse(), REF_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(ref(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                            CodegenBlock passBlock = eachlvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .increment("count");
                            }
                        }
                    }
                }
            }

            methodNode.getBlock().methodReturn(ref("count"));
        };

        return instance.getMethods().addMethod(int.class, "handleOutputLimitFirstJoinHaving",
                CodegenNamedParam.from(List.class, NAME_JOINEVENTSSET, boolean.class, NAME_ISSYNTHESIZE, List[].class, "oldEventsPerLevel", List[].class, "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private int handleOutputLimitFirstViewNoHaving(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, List<EventBean>[] oldEventsPerLevel, List<Object>[] oldEventsSortKeyPerLevel) {

        int count = 0;

        // outer loop is the events
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            EventBean[] eventsPerStream;
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(1, 0);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, true, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    count++;
                                }
                            }
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;

                        OutputConditionPolled outputStateGroup = outputFirstHelpers[level.getLevelNumber()].getOrAllocate(groupKey, agentInstanceContext, prototype.getOptionalOutputFirstConditionFactory());
                        boolean pass = outputStateGroup.updateOutputCondition(0, 1);
                        if (pass) {
                            if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                                if (prototype.isSelectRStream()) {
                                    generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, false, generateSynthetic, oldEventsPerLevel, oldEventsSortKeyPerLevel);
                                    count++;
                                }
                            }
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }
        return count;
    }

    private static CodegenMethodNode handleOutputLimitFirstViewNoHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethodNode generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenMember outputFactory = classScope.makeAddMember(OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory());
        initOutputFirstHelpers(instance, forge, classScope, outputFactory);

        Consumer<CodegenMethodNode> code = methodNode -> {

            methodNode.getBlock().declareVar(int.class, "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "groupKeysPerLevel", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)))
                        .declareVarNoInit(EventBean[].class, "eventsPerStream");

                {
                    CodegenBlock ifNewApplyAgg = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNew = ifNewApplyAgg.forEach(EventBean.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("aNewData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock forLvl = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(ref(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                            CodegenBlock passBlock = forLvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .increment("count");
                            }
                        }
                        forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(EventBean.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                        {
                            CodegenBlock forLvl = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(ref(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), REF_AGENTINSTANCECONTEXT, member(outputFactory.getMemberId())))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                            CodegenBlock passBlock = forLvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .increment("count");
                            }
                        }
                        forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                    }
                }
            }

            methodNode.getBlock().methodReturn(ref("count"));
        };

        return instance.getMethods().addMethod(int.class, "handleOutputLimitFirstNoViewHaving",
                CodegenNamedParam.from(List.class, "viewEventsList", boolean.class, NAME_ISSYNTHESIZE, List[].class, "oldEventsPerLevel", List[].class, "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private UniformPair<EventBean[]> handleOutputLimitDefaultView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {

        List<EventBean> newEvents = new ArrayList<>();
        List<Object> newEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new ArrayList<>();
        }

        List<EventBean> oldEvents = null;
        List<Object> oldEventsSortKey = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new ArrayList<>();
            if (orderByProcessor != null) {
                oldEventsSortKey = new ArrayList<>();
            }
        }

        EventBean[] eventsPerStream = new EventBean[1];
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            resetEventPerGroupBufView();
            Object[][] newDataMultiKey = generateGroupKeysView(newData, eventPerGroupBufView, true);
            Object[][] oldDataMultiKey = generateGroupKeysView(oldData, eventPerGroupBufView, false);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedCollectView(eventPerGroupBufView, false, generateSynthetic, oldEvents, oldEventsSortKey, eventsPerStream);
            }

            ResultSetProcessorGroupedUtil.applyAggViewResultKeyedView(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey, eventsPerStream);

            generateOutputBatchedCollectView(eventPerGroupBufView, true, generateSynthetic, newEvents, newEventsSortKey, eventsPerStream);
        }

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void handleOutputLimitDefaultViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatchedCollectView = generateOutputBatchedCollectViewCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethodNode resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .localMethod(resetEventPerGroupBufView)
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeysView, ref("newData"), ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeysView, ref("oldData"), ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse()));

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedCollectView, ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), ref("eventsPerStream"));
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"))
                    .localMethod(generateOutputBatchedCollectView, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), ref("eventsPerStream"));
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private UniformPair<EventBean[]> handleOutputLimitDefaultJoin(List<UniformPair<Set<MultiKey<EventBean>>>> viewEventsList, boolean generateSynthetic) {

        List<EventBean> newEvents = new ArrayList<>();
        List<Object> newEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new ArrayList<>();
        }

        List<EventBean> oldEvents = null;
        List<Object> oldEventsSortKey = null;
        if (prototype.isSelectRStream()) {
            oldEvents = new ArrayList<>();
            if (orderByProcessor != null) {
                oldEventsSortKey = new ArrayList<>();
            }
        }

        for (UniformPair<Set<MultiKey<EventBean>>> pair : viewEventsList) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();

            resetEventPerGroupBufJoin();
            Object[][] newDataMultiKey = generateGroupKeysJoin(newData, eventPerGroupBufJoin, true);
            Object[][] oldDataMultiKey = generateGroupKeysJoin(oldData, eventPerGroupBufJoin, false);

            if (prototype.isSelectRStream()) {
                generateOutputBatchedCollectJoin(eventPerGroupBufJoin, false, generateSynthetic, oldEvents, oldEventsSortKey);
            }

            ResultSetProcessorGroupedUtil.applyAggJoinResultKeyedJoin(aggregationService, agentInstanceContext, newData, newDataMultiKey, oldData, oldDataMultiKey);

            generateOutputBatchedCollectJoin(eventPerGroupBufJoin, true, generateSynthetic, newEvents, newEventsSortKey);
        }

        return ResultSetProcessorUtil.finalizeOutputMaySortMayRStream(newEvents, newEventsSortKey, oldEvents, oldEventsSortKey, prototype.isSelectRStream(), orderByProcessor, agentInstanceContext);
    }

    private static void handleOutputLimitDefaultJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatchedCollectJoin = generateOutputBatchedCollectJoinCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeysJoin = generateGroupKeysJoinCodegen(forge, classScope, instance);
        CodegenMethodNode resetEventPerGroupBufJoin = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFJOIN, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .localMethod(resetEventPerGroupBufJoin)
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeysJoin, ref("newData"), ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeysJoin, ref("oldData"), ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse()));

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedCollectJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"))
                    .localMethod(generateOutputBatchedCollectJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    public void removedAggregationGroupKey(Object key) {
        throw new UnsupportedOperationException();
    }

    static void removedAggregationGroupKeyCodegen(CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = method -> method.getBlock().methodThrowUnsupported();
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

    private void generateOutputBatchedGivenArray(boolean join, Object mk, AggregationGroupByRollupLevel level, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean>[] resultEvents, List<Object>[] optSortKeys) {
        List<EventBean> resultList = resultEvents[level.getLevelNumber()];
        List<Object> sortKeys = optSortKeys == null ? null : optSortKeys[level.getLevelNumber()];
        generateOutputBatched(mk, level, eventsPerStream, isNewData, isSynthesize, resultList, sortKeys);
    }

    private static CodegenMethodNode generateOutputBatchedGivenArrayCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);
        Consumer<CodegenMethodNode> code = methodNode -> methodNode.getBlock().declareVar(List.class, "resultList", arrayAtIndex(ref("resultEvents"), exprDotMethod(ref("level"), "getLevelNumber")))
                .declareVarNoInit(List.class, "sortKeys")
                .ifCondition(equalsNull(ref("optSortKeys")))
                .assignRef("sortKeys", constantNull())
                .ifElse()
                .assignRef("sortKeys", arrayAtIndex(ref("optSortKeys"), exprDotMethod(ref("level"), "getLevelNumber")))
                .blockEnd()
                .localMethod(generateOutputBatched, ref("mk"), ref("level"), ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("resultList"), ref("sortKeys"));
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedGivenArrayCodegen",
                CodegenNamedParam.from(boolean.class, "join", Object.class, "mk", AggregationGroupByRollupLevel.class, "level", EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List[].class, "resultEvents", List[].class, "optSortKeys"),
                ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    public void generateOutputBatched(Object mk, AggregationGroupByRollupLevel level, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys) {
        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), level);

        if (prototype.getPerLevelExpression().getOptionalHavingNodes() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getPerLevelExpression().getOptionalHavingNodes()[level.getLevelNumber()], eventsPerStream, isNewData, agentInstanceContext);
            if (!passesHaving) {
                return;
            }
        }

        resultEvents.add(prototype.getPerLevelExpression().getSelectExprProcessor()[level.getLevelNumber()].process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));

        if (prototype.isSorting()) {
            optSortKeys.add(orderByProcessor.getSortKeyRollup(eventsPerStream, isNewData, agentInstanceContext, level));
        }
    }

    static CodegenMethodNode generateOutputBatchedCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenInstanceAux instance, CodegenClassScope classScope) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"));

            if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                methodNode.getBlock().ifCondition(not(exprDotMethod(arrayAtIndex(REF_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockReturnNoValue();
            }

            CodegenExpression selectExprProcessor = arrayAtIndex(REF_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
            methodNode.getBlock().exprDotMethod(ref("resultEvents"), "add", exprDotMethod(selectExprProcessor, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));

            if (forge.isSorting()) {
                methodNode.getBlock().exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(REF_ORDERBYPROCESSOR, "getSortKeyRollup", ref("eventsPerStream"), REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT, ref("level")));
            }
        };
        return instance.getMethods().addMethod(void.class, "generateOutputBatched",
                CodegenNamedParam.from(Object.class, "mk", AggregationGroupByRollupLevel.class, "level", EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"),
                ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    public void generateOutputBatchedMapUnsorted(boolean join, Object mk, AggregationGroupByRollupLevel level, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, Map<Object, EventBean> resultEvents) {
        aggregationService.setCurrentAccess(mk, agentInstanceContext.getAgentInstanceId(), level);

        if (prototype.getPerLevelExpression().getOptionalHavingNodes() != null) {
            boolean passesHaving = ResultSetProcessorUtil.evaluateHavingClause(prototype.getPerLevelExpression().getOptionalHavingNodes()[level.getLevelNumber()], eventsPerStream, isNewData, agentInstanceContext);
            if (!passesHaving) {
                return;
            }
        }

        resultEvents.put(mk, prototype.getPerLevelExpression().getSelectExprProcessor()[level.getLevelNumber()].process(eventsPerStream, isNewData, isSynthesize, agentInstanceContext));
    }

    static void generateOutputBatchedMapUnsortedCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenInstanceAux instance, CodegenClassScope classScope) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(REF_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"));

            if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                CodegenExpression having = arrayAtIndex(REF_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
                methodNode.getBlock().ifCondition(not(exprDotMethod(having, "evaluateHaving", REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT))).blockReturnNoValue();
            }

            CodegenExpression selectExprProcessor = arrayAtIndex(REF_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
            methodNode.getBlock().exprDotMethod(ref("resultEvents"), "put", ref("mk"), exprDotMethod(selectExprProcessor, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_AGENTINSTANCECONTEXT));
        };

        instance.getMethods().addMethod(void.class, "generateOutputBatchedMapUnsorted",
                CodegenNamedParam.from(boolean.class, "join", Object.class, "mk", AggregationGroupByRollupLevel.class, "level", EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Map.class, "resultEvents"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private UniformPair<EventBean[]> handleOutputLimitLastView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        int count = 0;
        if (prototype.isSelectRStream()) {
            rstreamEventSortArrayBuf.reset();
        }

        for (Map<Object, EventBean[]> aGroupRepsView : groupRepsPerLevelBuf) {
            aGroupRepsView.clear();
        }

        // outer loop is the events
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            EventBean[] eventsPerStream;
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                            if (prototype.isSelectRStream()) {
                                generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                                count++;
                            }
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                            if (prototype.isSelectRStream()) {
                                generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                                count++;
                            }
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, count);
    }

    private static void handleOutputLimitLastViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (forge.isSelectRStream()) {
            initRStreamEventsSortArrayBufCodegen(instance, forge);
        }
        initGroupRepsPerLevelBufCodegen(instance, forge);

        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenMethodNode generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        CodegenMethodNode generateAndSort = generateAndSortCodegen(forge, classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");

        method.getBlock().declareVar(int.class, "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
        }

        method.getBlock().forEach(Map.class, "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "groupKeysPerLevel", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)))
                    .declareVarNoInit(EventBean[].class, "eventsPerStream");

            {
                CodegenBlock ifNew = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNew.forEach(EventBean.class, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("aNewData")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .increment("count");
                        }
                    }
                    forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                }

                CodegenBlock ifOld = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOld.forEach(EventBean.class, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .increment("count");
                        }
                    }
                    forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private UniformPair<EventBean[]> handleOutputLimitLastJoin(List<UniformPair<Set<MultiKey<EventBean>>>> viewEventsList, boolean generateSynthetic) {
        int count = 0;
        if (prototype.isSelectRStream()) {
            rstreamEventSortArrayBuf.reset();
        }

        for (Map<Object, EventBean[]> aGroupRepsView : groupRepsPerLevelBuf) {
            aGroupRepsView.clear();
        }

        // outer loop is the events
        for (UniformPair<Set<MultiKey<EventBean>>> pair : viewEventsList) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();
            EventBean[] eventsPerStream;

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    eventsPerStream = aNewData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                            if (prototype.isSelectRStream()) {
                                generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                                count++;
                            }
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    eventsPerStream = anOldData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        if (groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream) == null) {
                            if (prototype.isSelectRStream()) {
                                generateOutputBatchedGivenArray(true, groupKey, level, eventsPerStream, true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                                count++;
                            }
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, count);
    }

    private static void handleOutputLimitLastJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (forge.isSelectRStream()) {
            initRStreamEventsSortArrayBufCodegen(instance, forge);
        }
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethodNode generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        CodegenMethodNode generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().declareVar(int.class, "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
        }

        method.getBlock().forEach(Map.class, "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "groupKeysPerLevel", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)))
                    .declareVarNoInit(EventBean[].class, "eventsPerStream");

            {
                CodegenBlock ifNew = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNew.forEach(MultiKey.class, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .increment("count");
                        }
                    }
                    forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                }

                CodegenBlock ifOld = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOld.forEach(MultiKey.class, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .increment("count");
                        }
                    }
                    forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private UniformPair<EventBean[]> handleOutputLimitAllView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {

        int count = 0;
        if (prototype.isSelectRStream()) {
            rstreamEventSortArrayBuf.reset();

            for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                Map<Object, EventBean[]> groupGenerators = groupRepsPerLevelBuf[level.getLevelNumber()];
                for (Map.Entry<Object, EventBean[]> entry : groupGenerators.entrySet()) {
                    generateOutputBatchedGivenArray(false, entry.getKey(), level, entry.getValue(), false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                    count++;
                }
            }
        }

        // outer loop is the events
        for (UniformPair<EventBean[]> pair : viewEventsList) {
            EventBean[] newData = pair.getFirst();
            EventBean[] oldData = pair.getSecond();

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    EventBean[] eventsPerStream = new EventBean[]{aNewData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        Object existing = groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream);

                        if (existing == null && prototype.isSelectRStream()) {
                            generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                            count++;
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (EventBean anOldData : oldData) {
                    EventBean[] eventsPerStream = new EventBean[]{anOldData};
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        Object existing = groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream);

                        if (existing == null && prototype.isSelectRStream()) {
                            generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                            count++;
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, count);
    }

    private static void handleOutputLimitAllViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenMethodNode generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        if (forge.isSelectRStream()) {
            initRStreamEventsSortArrayBufCodegen(instance, forge);
        }
        CodegenMethodNode generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().declareVar(int.class, "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
            method.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(Map.class, "groupGenerators", arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), exprDotMethod(ref("level"), "getLevelNumber")))
                    .forEach(Map.Entry.class, "entry", exprDotMethod(ref("groupGenerators"), "entrySet"))
                    .localMethod(generateOutputBatchedGivenArray, constantFalse(), exprDotMethod(ref("entry"), "getKey"), ref("level"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                    .increment("count");
        }

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "groupKeysPerLevel", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)))
                    .declareVarNoInit(EventBean[].class, "eventsPerStream");

            {
                CodegenBlock ifNew = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNew.forEach(EventBean.class, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("aNewData")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .increment("count");
                        }
                    }
                    forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                }

                CodegenBlock ifOld = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOld.forEach(EventBean.class, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .increment("count");
                        }
                    }
                    forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private UniformPair<EventBean[]> handleOutputLimitAllJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic) {

        int count = 0;
        if (prototype.isSelectRStream()) {
            rstreamEventSortArrayBuf.reset();

            for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                Map<Object, EventBean[]> groupGenerators = groupRepsPerLevelBuf[level.getLevelNumber()];
                for (Map.Entry<Object, EventBean[]> entry : groupGenerators.entrySet()) {
                    generateOutputBatchedGivenArray(false, entry.getKey(), level, entry.getValue(), false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                    count++;
                }
            }
        }

        // outer loop is the events
        for (UniformPair<Set<MultiKey<EventBean>>> pair : joinEventsSet) {
            Set<MultiKey<EventBean>> newData = pair.getFirst();
            Set<MultiKey<EventBean>> oldData = pair.getSecond();
            EventBean[] eventsPerStream;

            // apply to aggregates
            Object[] groupKeysPerLevel = new Object[prototype.getGroupByRollupDesc().getLevels().length];
            if (newData != null) {
                for (MultiKey<EventBean> aNewData : newData) {
                    eventsPerStream = aNewData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, true);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        Object existing = groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream);

                        if (existing == null && prototype.isSelectRStream()) {
                            generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, true, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                            count++;
                        }
                    }
                    aggregationService.applyEnter(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
            if (oldData != null) {
                for (MultiKey<EventBean> anOldData : oldData) {
                    eventsPerStream = anOldData.getArray();
                    Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, false);
                    for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
                        Object groupKey = level.computeSubkey(groupKeyComplete);
                        groupKeysPerLevel[level.getLevelNumber()] = groupKey;
                        Object existing = groupRepsPerLevelBuf[level.getLevelNumber()].put(groupKey, eventsPerStream);

                        if (existing == null && prototype.isSelectRStream()) {
                            generateOutputBatchedGivenArray(false, groupKey, level, eventsPerStream, false, generateSynthetic, rstreamEventSortArrayBuf.getEventsPerLevel(), rstreamEventSortArrayBuf.getSortKeyPerLevel());
                            count++;
                        }
                    }
                    aggregationService.applyLeave(eventsPerStream, groupKeysPerLevel, agentInstanceContext);
                }
            }
        }

        return generateAndSort(groupRepsPerLevelBuf, generateSynthetic, count);
    }

    private static void handleOutputLimitAllJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        initGroupRepsPerLevelBufCodegen(instance, forge);
        if (forge.isSelectRStream()) {
            initRStreamEventsSortArrayBufCodegen(instance, forge);
        }
        CodegenMethodNode generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().declareVar(int.class, "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
            method.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(Map.class, "groupGenerators", arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), exprDotMethod(ref("level"), "getLevelNumber")))
                    .forEach(Map.Entry.class, "entry", exprDotMethod(ref("groupGenerators"), "entrySet"))
                    .localMethod(generateOutputBatchedGivenArray, constantFalse(), exprDotMethod(ref("entry"), "getKey"), ref("level"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                    .increment("count");
        }

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "groupKeysPerLevel", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)))
                    .declareVarNoInit(EventBean[].class, "eventsPerStream");

            {
                CodegenBlock ifNew = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNew.forEach(MultiKey.class, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .increment("count");
                        }
                    }
                    forNew.exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                }

                CodegenBlock ifOld = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOld.forEach(MultiKey.class, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .increment("count");
                        }
                    }
                    forOld.exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), REF_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private void generateOutputBatchedCollectView(Map<Object, EventBean>[] eventPairs, boolean isNewData, boolean generateSynthetic, List<EventBean> events, List<Object> sortKey, EventBean[] eventsPerStream) {
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();

        for (AggregationGroupByRollupLevel level : levels) {
            Map<Object, EventBean> eventsForLevel = eventPairs[level.getLevelNumber()];
            for (Map.Entry<Object, EventBean> pair : eventsForLevel.entrySet()) {
                eventsPerStream[0] = pair.getValue();
                generateOutputBatched(pair.getKey(), level, eventsPerStream, isNewData, generateSynthetic, events, sortKey);
            }
        }
    }

    private static CodegenMethodNode generateOutputBatchedCollectViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        CodegenMethodNode generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", ref("levels"));
            {
                CodegenBlock forEvents = forLevels.forEach(Map.Entry.class, "pair", exprDotMethod(arrayAtIndex(ref("eventPairs"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                forEvents.assignArrayElement("eventsPerStream", constant(0), cast(EventBean.class, exprDotMethod(ref("pair"), "getValue")))
                        .localMethod(generateOutputBatched, exprDotMethod(ref("pair"), "getKey"), ref("level"), ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("events"), ref("sortKey"));
            }
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedCollectView",
                CodegenNamedParam.from(Map[].class, "eventPairs", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "events", List.class, "sortKey", EventBean[].class, "eventsPerStream"),
                ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private void generateOutputBatchedCollectJoin(Map<Object, EventBean[]>[] eventPairs, boolean isNewData, boolean generateSynthetic, List<EventBean> events, List<Object> sortKey) {
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();

        for (AggregationGroupByRollupLevel level : levels) {
            Map<Object, EventBean[]> eventsForLevel = eventPairs[level.getLevelNumber()];
            for (Map.Entry<Object, EventBean[]> pair : eventsForLevel.entrySet()) {
                generateOutputBatched(pair.getKey(), level, pair.getValue(), isNewData, generateSynthetic, events, sortKey);
            }
        }
    }

    private static CodegenMethodNode generateOutputBatchedCollectJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        CodegenMethodNode generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);

        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", ref("levels"));
            {
                CodegenBlock forEvents = forLevels.forEach(Map.Entry.class, "pair", exprDotMethod(arrayAtIndex(ref("eventPairs"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                forEvents.localMethod(generateOutputBatched, exprDotMethod(ref("pair"), "getKey"), ref("level"), cast(EventBean[].class, exprDotMethod(ref("pair"), "getValue")), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("events"), ref("sortKey"));
            }
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedCollectJoin",
                CodegenNamedParam.from(Map[].class, "eventPairs", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "events", List.class, "sortKey"),
                ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private void resetEventPerGroupBufView() {
        for (Map<Object, EventBean> anEventPerGroupBuf : eventPerGroupBufView) {
            anEventPerGroupBuf.clear();
        }
    }

    private static CodegenMethodNode resetEventPerGroupBufCodegen(String memberName, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> methodNode.getBlock().forEach(LinkedHashMap.class, "anEventPerGroupBuf", ref(memberName))
                .exprDotMethod(ref("anEventPerGroupBuf"), "clear");

        return instance.getMethods().addMethod(void.class, "resetEventPerGroupBuf", Collections.emptyList(), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private void resetEventPerGroupBufJoin() {
        for (Map<Object, EventBean[]> anEventPerGroupBuf : eventPerGroupBufJoin) {
            anEventPerGroupBuf.clear();
        }
    }

    Object[][] generateGroupKeysView(EventBean[] events, Map<Object, EventBean>[] eventPerKey, boolean isNewData) {
        if (events == null) {
            return null;
        }

        Object[][] result = new Object[events.length][];
        EventBean[] eventsPerStream = new EventBean[1];
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();

        for (int i = 0; i < events.length; i++) {
            eventsPerStream[0] = events[i];
            Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, isNewData);
            result[i] = new Object[levels.length];
            for (int j = 0; j < levels.length; j++) {
                Object subkey = levels[j].computeSubkey(groupKeyComplete);
                result[i][j] = subkey;
                eventPerKey[levels[j].getLevelNumber()].put(subkey, events[i]);
            }
        }

        return result;
    }

    static CodegenMethodNode generateGroupKeysViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifRefNullReturnNull("events")
                    .declareVar(Object[][].class, "result", newArrayByLength(Object[].class, arrayLength(ref("events"))))
                    .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                    .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));
            {
                CodegenBlock forLoop = methodNode.getBlock().forLoopIntSimple("i", arrayLength(ref("events")));
                forLoop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("events"), ref("i")))
                        .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), REF_ISNEWDATA))
                        .assignArrayElement("result", ref("i"), newArrayByLength(Object.class, arrayLength(ref("levels"))));
                {
                    forLoop.forLoopIntSimple("j", arrayLength(ref("levels")))
                            .declareVar(Object.class, "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                            .assignArrayElement2Dim("result", ref("i"), ref("j"), ref("subkey"))
                            .exprDotMethod(arrayAtIndex(ref("eventPerKey"), exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "getLevelNumber")), "put", ref("subkey"), arrayAtIndex(ref("events"), ref("i")));
                }
            }

            methodNode.getBlock().methodReturn(ref("result"));
        };

        return instance.getMethods().addMethod(Object[][].class, "generateGroupKeysView",
                CodegenNamedParam.from(EventBean[].class, "events", Map[].class, "eventPerKey", boolean.class, NAME_ISNEWDATA), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private Object[][] generateGroupKeysJoin(Set<MultiKey<EventBean>> events, Map<Object, EventBean[]>[] eventPerKey, boolean isNewData) {
        if (events == null || events.isEmpty()) {
            return null;
        }

        Object[][] result = new Object[events.size()][];
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        int count = -1;

        for (MultiKey<EventBean> eventrow : events) {
            count++;
            EventBean[] eventsPerStream = eventrow.getArray();
            Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, isNewData);
            result[count] = new Object[levels.length];
            for (int j = 0; j < levels.length; j++) {
                Object subkey = levels[j].computeSubkey(groupKeyComplete);
                result[count][j] = subkey;
                eventPerKey[levels[j].getLevelNumber()].put(subkey, eventsPerStream);
            }
        }

        return result;
    }

    private static CodegenMethodNode generateGroupKeysJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);
        Consumer<CodegenMethodNode> code = methodNode -> {
            methodNode.getBlock().ifCondition(or(equalsNull(ref("events")), exprDotMethod(ref("events"), "isEmpty"))).blockReturn(constantNull())
                    .declareVar(Object[][].class, "result", newArrayByLength(Object[].class, exprDotMethod(ref("events"), "size")))
                    .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(int.class, "count", constant(-1));
            {
                CodegenBlock forLoop = methodNode.getBlock().forEach(MultiKey.class, "eventrow", ref("events"));
                forLoop.increment("count")
                        .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("eventrow"), "getArray")))
                        .declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), REF_ISNEWDATA))
                        .assignArrayElement("result", ref("count"), newArrayByLength(Object.class, arrayLength(ref("levels"))));
                {
                    forLoop.forLoopIntSimple("j", arrayLength(ref("levels")))
                            .declareVar(Object.class, "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                            .assignArrayElement2Dim("result", ref("count"), ref("j"), ref("subkey"))
                            .exprDotMethod(arrayAtIndex(ref("eventPerKey"), exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "getLevelNumber")), "put", ref("subkey"), ref("eventsPerStream"));
                }
            }

            methodNode.getBlock().methodReturn(ref("result"));
        };

        return instance.getMethods().addMethod(Object[][].class, "generateGroupKeysJoin",
                CodegenNamedParam.from(Set.class, "events", Map[].class, "eventPerKey", boolean.class, NAME_ISNEWDATA), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private UniformPair<EventBean[]> generateAndSort(Map<Object, EventBean[]>[] outputLimitGroupRepsPerLevel, boolean generateSynthetic, int oldEventCount) {
        // generate old events: ordered by level by default
        EventBean[] oldEventsArr = null;
        Object[] oldEventSortKeys = null;
        if (prototype.isSelectRStream() && oldEventCount > 0) {
            EventsAndSortKeysPair pair = getOldEventsSortKeys(oldEventCount, rstreamEventSortArrayBuf, orderByProcessor, prototype.getGroupByRollupDesc());
            oldEventsArr = pair.getEvents();
            oldEventSortKeys = pair.getSortKeys();
        }

        List<EventBean> newEvents = new ArrayList<>();
        List<Object> newEventsSortKey = null;
        if (orderByProcessor != null) {
            newEventsSortKey = new ArrayList<>();
        }

        for (AggregationGroupByRollupLevel level : prototype.getGroupByRollupDesc().getLevels()) {
            Map<Object, EventBean[]> groupGenerators = outputLimitGroupRepsPerLevel[level.getLevelNumber()];
            for (Map.Entry<Object, EventBean[]> entry : groupGenerators.entrySet()) {
                generateOutputBatched(entry.getKey(), level, entry.getValue(), true, generateSynthetic, newEvents, newEventsSortKey);
            }
        }

        EventBean[] newEventsArr = CollectionUtil.toArrayNullForEmptyEvents(newEvents);
        if (orderByProcessor != null) {
            Object[] sortKeysNew = CollectionUtil.toArrayNullForEmptyObjects(newEventsSortKey);
            newEventsArr = orderByProcessor.sortWOrderKeys(newEventsArr, sortKeysNew, agentInstanceContext);
            if (prototype.isSelectRStream()) {
                oldEventsArr = orderByProcessor.sortWOrderKeys(oldEventsArr, oldEventSortKeys, agentInstanceContext);
            }
        }

        return ResultSetProcessorUtil.toPairNullIfAllNull(newEventsArr, oldEventsArr);
    }

    private static CodegenMethodNode generateAndSortCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);

        Consumer<CodegenMethodNode> code = methodNode -> {

            methodNode.getBlock().declareVar(EventBean[].class, "oldEventsArr", constantNull())
                    .declareVar(Object[].class, "oldEventSortKeys", constantNull());

            if (forge.isSelectRStream()) {
                methodNode.getBlock().ifCondition(relational(ref("oldEventCount"), GT, constant(0)))
                        .declareVar(EventsAndSortKeysPair.class, "pair", staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, METHOD_GETOLDEVENTSSORTKEYS, ref("oldEventCount"), ref(NAME_RSTREAMEVENTSORTARRAYBUF), REF_ORDERBYPROCESSOR, exprDotMethod(ref("this"), "getGroupByRollupDesc")))
                        .assignRef("oldEventsArr", exprDotMethod(ref("pair"), "getEvents"))
                        .assignRef("oldEventSortKeys", exprDotMethod(ref("pair"), "getSortKeys"));
            }

            methodNode.getBlock().declareVar(List.class, "newEvents", newInstance(ArrayList.class))
                    .declareVar(List.class, "newEventsSortKey", forge.isSorting() ? newInstance(ArrayList.class) : constantNull());

            methodNode.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(Map.class, "groupGenerators", arrayAtIndex(ref("outputLimitGroupRepsPerLevel"), exprDotMethod(ref("level"), "getLevelNumber")))
                    .forEach(Map.Entry.class, "entry", exprDotMethod(ref("groupGenerators"), "entrySet"))
                    .localMethod(generateOutputBatched, exprDotMethod(ref("entry"), "getKey"), ref("level"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

            methodNode.getBlock().declareVar(EventBean[].class, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYEVENTS, ref("newEvents")));
            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(Object[].class, "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYOBJECTS, ref("newEventsSortKey")))
                        .assignRef("newEventsArr", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), REF_AGENTINSTANCECONTEXT));
                if (forge.isSelectRStream()) {
                    methodNode.getBlock().assignRef("oldEventsArr", exprDotMethod(REF_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("oldEventSortKeys"), REF_AGENTINSTANCECONTEXT));
                }
            }

            methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
        };

        return instance.getMethods().addMethod(UniformPair.class, "generateAndSort",
                CodegenNamedParam.from(Map[].class, "outputLimitGroupRepsPerLevel", boolean.class, NAME_ISSYNTHESIZE, int.class, "oldEventCount"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null) {
            for (EventBean aNewData : newData) {
                eventsPerStream[0] = aNewData;
                Object[] keys = generateGroupKeysRow(eventsPerStream, true);
                aggregationService.applyEnter(eventsPerStream, keys, agentInstanceContext);
            }
        }
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                eventsPerStream[0] = anOldData;
                Object[] keys = generateGroupKeysRow(eventsPerStream, false);
                aggregationService.applyLeave(eventsPerStream, keys, agentInstanceContext);
            }
        }
    }

    static void applyViewResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeysRow = generateGroupKeysRowCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
        {
            CodegenBlock ifNew = method.getBlock().ifCondition(notEqualsNull(REF_NEWDATA));
            {
                ifNew.forEach(EventBean.class, "aNewData", REF_NEWDATA)
                        .assignArrayElement("eventsPerStream", constant(0), ref("aNewData"))
                        .declareVar(Object[].class, "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantTrue()))
                        .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("keys"), REF_AGENTINSTANCECONTEXT);
            }
        }
        {
            CodegenBlock ifOld = method.getBlock().ifCondition(notEqualsNull(REF_OLDDATA));
            {
                ifOld.forEach(EventBean.class, "anOldData", REF_OLDDATA)
                        .assignArrayElement("eventsPerStream", constant(0), ref("anOldData"))
                        .declareVar(Object[].class, "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantFalse()))
                        .exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("keys"), REF_AGENTINSTANCECONTEXT);
            }
        }
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        EventBean[] eventsPerStream;
        if (newEvents != null) {
            for (MultiKey<EventBean> mk : newEvents) {
                eventsPerStream = mk.getArray();
                Object[] keys = generateGroupKeysRow(eventsPerStream, true);
                aggregationService.applyEnter(eventsPerStream, keys, agentInstanceContext);
            }
        }
        if (oldEvents != null) {
            for (MultiKey<EventBean> mk : oldEvents) {
                eventsPerStream = mk.getArray();
                Object[] keys = generateGroupKeysRow(eventsPerStream, false);
                aggregationService.applyLeave(eventsPerStream, keys, agentInstanceContext);
            }
        }
    }

    public static void applyJoinResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeysRow = generateGroupKeysRowCodegen(forge, classScope, instance);

        method.getBlock().declareVarNoInit(EventBean[].class, "eventsPerStream");
        {
            CodegenBlock ifNew = method.getBlock().ifCondition(notEqualsNull(REF_NEWDATA));
            {
                ifNew.forEach(MultiKey.class, "mk", REF_NEWDATA)
                        .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("mk"), "getArray")))
                        .declareVar(Object[].class, "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantTrue()))
                        .exprDotMethod(REF_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("keys"), REF_AGENTINSTANCECONTEXT);
            }
        }
        {
            CodegenBlock ifOld = method.getBlock().ifCondition(notEqualsNull(REF_OLDDATA));
            {
                ifOld.forEach(MultiKey.class, "mk", REF_OLDDATA)
                        .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("mk"), "getArray")))
                        .declareVar(Object[].class, "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantFalse()))
                        .exprDotMethod(REF_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("keys"), REF_AGENTINSTANCECONTEXT);
            }
        }
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        if (prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            outputAllHelper.processView(newData, oldData, isGenerateSynthetic);
        } else {
            outputLastHelper.processView(newData, oldData, isGenerateSynthetic);
        }
    }

    static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorRowPerGroupRollupForge forge, String methodName, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        CodegenMember factory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
        CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());

        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorRowPerGroupRollupOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSRowPerGroupRollupAll", REF_AGENTINSTANCECONTEXT, ref("this"), member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorRowPerGroupRollupOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(member(factory.getMemberId()), "makeRSRowPerGroupRollupLast", REF_AGENTINSTANCECONTEXT, ref("this"), member(groupKeyTypes.getMemberId()), constant(forge.getNumStreams())));
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        }
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        if (prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            outputAllHelper.processJoin(newEvents, oldEvents, isGenerateSynthetic);
        } else {
            outputLastHelper.processJoin(newEvents, oldEvents, isGenerateSynthetic);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize) {
        if (prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            return outputAllHelper.outputView(isSynthesize);
        }
        return outputLastHelper.outputView(isSynthesize);
    }

    static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenMethodNode method) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTALLHELPER), "outputView", REF_ISSYNTHESIZE));
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "outputView", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize) {
        if (prototype.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            return outputAllHelper.outputJoin(isSynthesize);
        }
        return outputLastHelper.outputJoin(isSynthesize);
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenMethodNode method) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            method.getBlock().methodReturn(exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public void stop() {
        if (outputLastHelper != null) {
            outputLastHelper.destroy();
        }
        if (outputFirstHelpers != null) {
            for (ResultSetProcessorGroupedOutputFirstHelper helper : outputFirstHelpers) {
                helper.destroy();
            }
        }
        if (outputAllHelper != null) {
            outputAllHelper.destroy();
        }
    }

    static void stopMethodCodegenBound(CodegenMethodNode method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTLASTHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPERS)) {
            method.getBlock().forEach(ResultSetProcessorGroupedOutputFirstHelper.class, "helper", ref(NAME_OUTPUTFIRSTHELPERS))
                    .exprDotMethod(ref("helper"), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(ref(NAME_OUTPUTALLHELPER), "destroy");
        }
    }

    private Object[] generateGroupKeysRow(EventBean[] eventsPerStream, boolean isNewData) {
        Object groupKeyComplete = generateGroupKeySingle(eventsPerStream, isNewData);
        AggregationGroupByRollupLevel[] levels = prototype.getGroupByRollupDesc().getLevels();
        Object[] result = new Object[levels.length];
        for (int j = 0; j < levels.length; j++) {
            Object subkey = levels[j].computeSubkey(groupKeyComplete);
            result[j] = subkey;
        }
        return result;
    }

    private static CodegenMethodNode generateGroupKeysRowCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(forge.getGroupKeyNodeExpressions(), classScope, instance);

        Consumer<CodegenMethodNode> code = methodNode -> methodNode.getBlock().declareVar(Object.class, "groupKeyComplete", localMethod(generateGroupKeySingle, ref("eventsPerStream"), REF_ISNEWDATA))
                .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                .declareVar(Object[].class, "result", newArrayByLength(Object.class, arrayLength(ref("levels"))))
                .forLoopIntSimple("j", arrayLength(ref("levels")))
                .declareVar(Object.class, "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                .assignArrayElement("result", ref("j"), ref("subkey"))
                .blockEnd()
                .methodReturn(ref("result"));

        return instance.getMethods().addMethod(Object[].class, "generateGroupKeysRow", CodegenNamedParam.from(EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA), ResultSetProcessorUtil.class, classScope, code);
    }

    public ExprEvaluatorContext getAgentInstanceContext() {
        return agentInstanceContext;
    }

    public boolean isSelectRStream() {
        return prototype.isSelectRStream();
    }

    public AggregationGroupByRollupDesc getGroupByRollupDesc() {
        return prototype.getGroupByRollupDesc();
    }

    private static void initGroupRepsPerLevelBufCodegen(CodegenInstanceAux instance, ResultSetProcessorRowPerGroupRollupForge forge) {
        if (!instance.hasMember(NAME_GROUPREPSPERLEVELBUF)) {
            instance.addMember(NAME_GROUPREPSPERLEVELBUF, Map[].class);
            instance.getServiceCtor().getBlock().assignRef(NAME_GROUPREPSPERLEVELBUF, staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, METHOD_MAKEGROUPREPSPERLEVELBUF, constant(forge.getGroupByRollupDesc().getLevels().length)));
        }
    }

    private static void initRStreamEventsSortArrayBufCodegen(CodegenInstanceAux instance, ResultSetProcessorRowPerGroupRollupForge forge) {
        if (!instance.hasMember(NAME_RSTREAMEVENTSORTARRAYBUF)) {
            instance.addMember(NAME_RSTREAMEVENTSORTARRAYBUF, EventArrayAndSortKeyArray.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_RSTREAMEVENTSORTARRAYBUF, staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, METHOD_MAKERSTREAMSORTEDARRAYBUF, constant(forge.getGroupByRollupDesc().getLevels().length), constant(forge.isSorting())));
        }
    }

    private static void initOutputFirstHelpers(CodegenInstanceAux instance, ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMember outputFactory) {
        if (!instance.hasMember(NAME_OUTPUTFIRSTHELPERS)) {
            CodegenMember factory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, forge.getResultSetProcessorHelperFactory());
            CodegenMember groupKeyTypes = classScope.makeAddMember(Class[].class, forge.getGroupKeyTypes());
            instance.addMember(NAME_OUTPUTFIRSTHELPERS, ResultSetProcessorGroupedOutputFirstHelper[].class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPERS, staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, "initializeOutputFirstHelpers", member(factory.getMemberId()),
                    REF_AGENTINSTANCECONTEXT, member(groupKeyTypes.getMemberId()), exprDotMethod(ref("this"), "getGroupByRollupDesc"), member(outputFactory.getMemberId())));
        }
    }
}
