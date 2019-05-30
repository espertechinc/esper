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
package com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupLevel;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupKey;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolled;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactoryField;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroupImpl;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil.METHOD_ITERATORTODEQUE;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil.METHOD_TOPAIRNULLIFALLNULL;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.METHOD_APPLYAGGJOINRESULTKEYEDJOIN;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.METHOD_APPLYAGGVIEWRESULTKEYEDVIEW;
import static com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupUtil.*;
import static com.espertech.esper.common.internal.util.CollectionUtil.*;

public class ResultSetProcessorRowPerGroupRollupImpl {
    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";
    private final static String NAME_OUTPUTFIRSTHELPERS = "outputFirstHelpers";
    private final static String NAME_EVENTPERGROUPBUFJOIN = "eventPerGroupBufJoin";
    private final static String NAME_EVENTPERGROUPBUFVIEW = "eventPerGroupBufView";
    private final static String NAME_GROUPREPSPERLEVELBUF = "groupRepsPerLevelBuf";
    private final static String NAME_RSTREAMEVENTSORTARRAYBUF = "rstreamEventSortArrayBuf";

    static void processJoinResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        addEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFJOIN, forge, instance, method, classScope);
        CodegenMethod resetEventPerGroupJoinBuf = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFJOIN, classScope, instance);
        CodegenMethod generateGroupKeysJoin = generateGroupKeysJoinCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }

        method.getBlock().localMethod(resetEventPerGroupJoinBuf)
                .declareVar(Object[][].class, "newDataMultiKey", localMethod(generateGroupKeysJoin, REF_NEWDATA, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue()))
                .declareVar(Object[][].class, "oldDataMultiKey", localMethod(generateGroupKeysJoin, REF_OLDDATA, ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse()))
                .declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    static void processViewResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        addEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, forge, instance, method, classScope);
        CodegenMethod resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);
        CodegenMethod generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        method.getBlock().localMethod(resetEventPerGroupBufView)
                .declareVar(Object[][].class, "newDataMultiKey", localMethod(generateGroupKeysView, REF_NEWDATA, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue()))
                .declareVar(Object[][].class, "oldDataMultiKey", localMethod(generateGroupKeysView, REF_OLDDATA, ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse()))
                .declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"), ref("eventsPerStream"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    private static void addEventPerGroupBufCodegen(String memberName, ResultSetProcessorRowPerGroupRollupForge forge, CodegenInstanceAux instance, CodegenMethod method, CodegenClassScope classScope) {
        if (!instance.hasMember(memberName)) {
            CodegenMethod init = method.makeChild(LinkedHashMap[].class, ResultSetProcessorRowPerGroupRollupImpl.class, classScope);
            instance.addMember(memberName, LinkedHashMap[].class);
            instance.getServiceCtor().getBlock().assignMember(memberName, localMethod(init));
            int levelCount = forge.getGroupByRollupDesc().getLevels().length;
            init.getBlock().declareVar(LinkedHashMap[].class, memberName, newArrayByLength(LinkedHashMap.class, constant(levelCount)))
                    .forLoopIntSimple("i", constant(levelCount))
                    .assignArrayElement(memberName, ref("i"), newInstance(LinkedHashMap.class))
                    .blockEnd()
                    .methodReturn(ref(memberName));
        }
    }

    static CodegenMethod generateOutputEventsViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                    .declareVar(ArrayList.class, "events", newInstance(ArrayList.class, constant(1)))
                    .declareVar(List.class, "currentGenerators", forge.isSorting() ? newInstance(ArrayList.class, constant(1)) : constantNull())
                    .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            {
                CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", ref("levels"));
                {
                    CodegenBlock forEvents = forLevels.forEach(Map.Entry.class, "entry", exprDotMethod(arrayAtIndex(ref("keysAndEvents"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                    forEvents.declareVar(Object.class, "groupKey", exprDotMethod(ref("entry"), "getKey"))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                            .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.class, exprDotMethod(ref("entry"), "getValue")));

                    if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                        CodegenExpression having = arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
                        forEvents.ifCondition(not(exprDotMethod(having, "evaluateHaving", REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                    }

                    forEvents.exprDotMethod(ref("events"), "add", exprDotMethod(arrayAtIndex(MEMBER_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

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
                        .blockReturn(exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortRollup", ref("outgoing"), ref("currentGenerators"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT, MEMBER_AGGREGATIONSVC));
            }
            methodNode.getBlock().methodReturn(ref("outgoing"));
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsView",
                CodegenNamedParam.from(Map[].class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputEventsJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(ArrayList.class, "events", newInstance(ArrayList.class, constant(1)))
                    .declareVar(List.class, "currentGenerators", forge.isSorting() ? newInstance(ArrayList.class, constant(1)) : constantNull())
                    .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            {
                CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", ref("levels"));
                {
                    CodegenBlock forEvents = forLevels.forEach(Map.Entry.class, "entry", exprDotMethod(arrayAtIndex(ref("eventPairs"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                    forEvents.declareVar(Object.class, "groupKey", exprDotMethod(ref("entry"), "getKey"))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")));

                    if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                        CodegenExpression having = arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
                        forEvents.ifCondition(not(exprDotMethod(having, "evaluateHaving", REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                    }

                    forEvents.exprDotMethod(ref("events"), "add", exprDotMethod(arrayAtIndex(MEMBER_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                    if (forge.isSorting()) {
                        forEvents.exprDotMethod(ref("currentGenerators"), "add", newInstance(GroupByRollupKey.class, ref("eventsPerStream"), ref("level"), ref("groupKey")));
                    }
                }
            }

            methodNode.getBlock().ifCondition(exprDotMethod(ref("events"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(EventBean[].class, "outgoing", staticMethod(CollectionUtil.class, METHOD_TOARRAYEVENTS, ref("events")));
            if (forge.isSorting()) {
                methodNode.getBlock().ifCondition(relational(arrayLength(ref("outgoing")), GT, constant(1)))
                        .blockReturn(exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortRollup", ref("outgoing"), ref("currentGenerators"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT, MEMBER_AGGREGATIONSVC));
            }
            methodNode.getBlock().methodReturn(ref("outgoing"));
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsJoin",
                CodegenNamedParam.from(Map[].class, "eventPairs", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    static void getIteratorViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE));
            return;
        }

        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
                .declareVar(Iterator.class, "it", exprDotMethod(REF_VIEWABLE, "iterator"))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(Object[].class, "groupKeys", newArrayByLength(Object.class, constant(forge.getGroupByRollupDesc().getLevels().length)))
                .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("it"), "hasNext"))
                    .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.class, exprDotMethod(ref("it"), "next")))
                    .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                    .forLoopIntSimple("j", arrayLength(ref("levels")))
                    .declareVar(Object.class, "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                    .assignArrayElement("groupKeys", ref("j"), ref("subkey"))
                    .blockEnd()
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeys"), MEMBER_AGENTINSTANCECONTEXT)
                    .blockEnd();
        }

        method.getBlock().declareVar(ArrayDeque.class, "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE)))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod parent, CodegenInstanceAux instance) {
        CodegenMethod resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);
        CodegenMethod generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        CodegenMethod iterator = parent.makeChild(Iterator.class, ResultSetProcessorRowPerGroupRollupImpl.class, classScope).addParam(Viewable.class, NAME_VIEWABLE);
        iterator.getBlock().localMethod(resetEventPerGroupBufView)
                .declareVar(EventBean[].class, "events", staticMethod(CollectionUtil.class, METHOD_ITERATORTOARRAYEVENTS, exprDotMethod(REF_VIEWABLE, "iterator")))
                .localMethod(generateGroupKeysView, ref("events"), ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue())
                .declareVar(EventBean[].class, "output", localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("output")));
        return iterator;
    }

    static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    static void getIteratorJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeysJoin = generateGroupKeysJoinCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);
        CodegenMethod resetEventPerGroupBuf = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFJOIN, classScope, instance);
        method.getBlock().localMethod(resetEventPerGroupBuf)
                .localMethod(generateGroupKeysJoin, REF_JOINSET, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue())
                .declareVar(EventBean[].class, "output", localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("output")));
    }

    static void clearMethodCodegen(CodegenMethod method) {
        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT);
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
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

    static void processOutputLimitedViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
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

    static void acceptHelperVisitorCodegen(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTLASTHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTALLHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPERS)) {
            method.getBlock().forEach(ResultSetProcessorGroupedOutputFirstHelper.class, "helper", member(NAME_OUTPUTFIRSTHELPERS))
                    .exprDotMethod(REF_RESULTSETVISITOR, "visit", ref("helper"));
        }
    }

    private static void handleOutputLimitFirstViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenMethod generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().forEach(Map.class, "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");

        method.getBlock().declareVarNoInit(int.class, "count");
        if (forge.getPerLevelForges().getOptionalHavingForges() == null) {
            CodegenMethod handleOutputLimitFirstViewNoHaving = handleOutputLimitFirstViewNoHavingCodegen(forge, classScope, instance);
            method.getBlock().assignRef("count", localMethod(handleOutputLimitFirstViewNoHaving, REF_VIEWEVENTSLIST, REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel")));
        } else {
            CodegenMethod handleOutputLimitFirstViewHaving = handleOutputLimitFirstViewHavingCodegen(forge, classScope, instance);
            method.getBlock().assignRef("count", localMethod(handleOutputLimitFirstViewHaving, REF_VIEWEVENTSLIST, REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel")));
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private static void handleOutputLimitFirstJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenMethod generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().forEach(Map.class, "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");

        method.getBlock().declareVarNoInit(int.class, "count");
        if (forge.getPerLevelForges().getOptionalHavingForges() == null) {
            CodegenMethod handleOutputLimitFirstJoinNoHaving = handleOutputLimitFirstJoinNoHavingCodegen(forge, classScope, instance);
            method.getBlock().assignRef("count", localMethod(handleOutputLimitFirstJoinNoHaving, REF_JOINEVENTSSET, REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel")));
        } else {
            CodegenMethod handleOutputLimitFirstJoinHaving = handleOutputLimitFirstJoinHavingCodegen(forge, classScope, instance);
            method.getBlock().assignRef("count", localMethod(handleOutputLimitFirstJoinHaving, REF_JOINEVENTSSET, REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel")));
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private static CodegenMethod handleOutputLimitFirstViewHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        initOutputFirstHelpers(outputFactory, instance, forge, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

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
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(EventBean.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                        {
                            forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifNewFirst = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNewFirst = ifNewFirst.forEach(EventBean.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("aNewData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forNewFirst.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                            CodegenBlock passBlock = eachlvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .incrementRef("count");
                            }
                        }
                    }

                    CodegenBlock ifOldFirst = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOldFirst = ifOldFirst.forEach(EventBean.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forOldFirst.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                            CodegenBlock passBlock = eachlvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .incrementRef("count");
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

    private static CodegenMethod handleOutputLimitFirstJoinNoHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        initOutputFirstHelpers(outputFactory, instance, forge, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

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
                        CodegenBlock forNew = ifNewApplyAgg.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock forLvl = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                            CodegenBlock passBlock = forLvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .incrementRef("count");
                            }
                        }
                        forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                        {
                            CodegenBlock forLvl = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                            CodegenBlock passBlock = forLvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .incrementRef("count");
                            }
                        }
                        forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }
            }

            methodNode.getBlock().methodReturn(ref("count"));
        };

        return instance.getMethods().addMethod(int.class, "handleOutputLimitFirstJoinNoHaving",
                CodegenNamedParam.from(List.class, NAME_JOINEVENTSSET, boolean.class, NAME_ISSYNTHESIZE, List[].class, "oldEventsPerLevel", List[].class, "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private static CodegenMethod handleOutputLimitFirstJoinHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        initOutputFirstHelpers(outputFactory, instance, forge, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

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
                        CodegenBlock forNew = ifNewApplyAgg.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                        {
                            forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifNewFirst = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNewFirst = ifNewFirst.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forNewFirst.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                            CodegenBlock passBlock = eachlvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .incrementRef("count");
                            }
                        }
                    }

                    CodegenBlock ifOldFirst = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOldFirst = ifOldFirst.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forOldFirst.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                            CodegenBlock passBlock = eachlvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .incrementRef("count");
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

    private static CodegenMethod handleOutputLimitFirstViewNoHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        initOutputFirstHelpers(outputFactory, instance, forge, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

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
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock forLvl = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                            CodegenBlock passBlock = forLvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .incrementRef("count");
                            }
                        }
                        forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(EventBean.class, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                                .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                        {
                            CodegenBlock forLvl = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                            CodegenBlock passBlock = forLvl.ifCondition(ref("pass"));
                            CodegenBlock putBlock = passBlock.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                            if (forge.isSelectRStream()) {
                                putBlock.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEventsPerLevel"), ref("oldEventsSortKeyPerLevel"))
                                        .incrementRef("count");
                            }
                        }
                        forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }
            }

            methodNode.getBlock().methodReturn(ref("count"));
        };

        return instance.getMethods().addMethod(int.class, "handleOutputLimitFirstNoViewHaving",
                CodegenNamedParam.from(List.class, "viewEventsList", boolean.class, NAME_ISSYNTHESIZE, List[].class, "oldEventsPerLevel", List[].class, "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private static void handleOutputLimitDefaultViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedCollectView = generateOutputBatchedCollectViewCodegen(forge, classScope, instance);
        CodegenMethod generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethod resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);

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

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"))
                    .localMethod(generateOutputBatchedCollectView, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), ref("eventsPerStream"));
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void handleOutputLimitDefaultJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedCollectJoin = generateOutputBatchedCollectJoinCodegen(forge, classScope, instance);
        CodegenMethod generateGroupKeysJoin = generateGroupKeysJoinCodegen(forge, classScope, instance);
        CodegenMethod resetEventPerGroupBufJoin = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFJOIN, classScope, instance);

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

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"))
                    .localMethod(generateOutputBatchedCollectJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    static void removedAggregationGroupKeyCodegen(CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> method.getBlock().methodThrowUnsupported();
        instance.getMethods().addMethod(void.class, "removedAggregationGroupKey", CodegenNamedParam.from(Object.class, "key"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputBatchedGivenArrayCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);
        Consumer<CodegenMethod> code = methodNode -> methodNode.getBlock().declareVar(List.class, "resultList", arrayAtIndex(ref("resultEvents"), exprDotMethod(ref("level"), "getLevelNumber")))
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

    static CodegenMethod generateOutputBatchedCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenInstanceAux instance, CodegenClassScope classScope) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"));

            if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                methodNode.getBlock().ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockReturnNoValue();
            }

            CodegenExpression selectExprProcessor = arrayAtIndex(MEMBER_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
            methodNode.getBlock().exprDotMethod(ref("resultEvents"), "add", exprDotMethod(selectExprProcessor, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

            if (forge.isSorting()) {
                methodNode.getBlock().exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKeyRollup", ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT, ref("level")));
            }
        };
        return instance.getMethods().addMethod(void.class, "generateOutputBatched",
                CodegenNamedParam.from(Object.class, "mk", AggregationGroupByRollupLevel.class, "level", EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"),
                ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    static void generateOutputBatchedMapUnsortedCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenInstanceAux instance, CodegenClassScope classScope) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"));

            if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                CodegenExpression having = arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
                methodNode.getBlock().ifCondition(not(exprDotMethod(having, "evaluateHaving", REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockReturnNoValue();
            }

            CodegenExpression selectExprProcessor = arrayAtIndex(MEMBER_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
            methodNode.getBlock().exprDotMethod(ref("resultEvents"), "put", ref("mk"), exprDotMethod(selectExprProcessor, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
        };

        instance.getMethods().addMethod(void.class, "generateOutputBatchedMapUnsorted",
                CodegenNamedParam.from(boolean.class, "join", Object.class, "mk", AggregationGroupByRollupLevel.class, "level", EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Map.class, "resultEvents"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private static void handleOutputLimitLastViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.isSelectRStream()) {
            initRStreamEventsSortArrayBufCodegen(instance, forge);
        }
        initGroupRepsPerLevelBufCodegen(instance, forge);

        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        CodegenMethod generateAndSort = generateAndSortCodegen(forge, classScope, instance);
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
                            .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .incrementRef("count");
                        }
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                }

                CodegenBlock ifOld = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOld.forEach(EventBean.class, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .incrementRef("count");
                        }
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private static void handleOutputLimitLastJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.isSelectRStream()) {
            initRStreamEventsSortArrayBufCodegen(instance, forge);
        }
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        CodegenMethod generateAndSort = generateAndSortCodegen(forge, classScope, instance);

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
                    CodegenBlock forNew = ifNew.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .incrementRef("count");
                        }
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                }

                CodegenBlock ifOld = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOld.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .incrementRef("count");
                        }
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private static void handleOutputLimitAllViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        if (forge.isSelectRStream()) {
            initRStreamEventsSortArrayBufCodegen(instance, forge);
        }
        CodegenMethod generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().declareVar(int.class, "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
            method.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(Map.class, "groupGenerators", arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), exprDotMethod(ref("level"), "getLevelNumber")))
                    .forEach(Map.Entry.class, "entry", exprDotMethod(ref("groupGenerators"), "entrySet"))
                    .localMethod(generateOutputBatchedGivenArray, constantFalse(), exprDotMethod(ref("entry"), "getKey"), ref("level"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                    .incrementRef("count");
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
                            .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .incrementRef("count");
                        }
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                }

                CodegenBlock ifOld = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOld.forEach(EventBean.class, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .incrementRef("count");
                        }
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private static void handleOutputLimitAllJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        initGroupRepsPerLevelBufCodegen(instance, forge);
        if (forge.isSelectRStream()) {
            initRStreamEventsSortArrayBufCodegen(instance, forge);
        }
        CodegenMethod generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().declareVar(int.class, "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
            method.getBlock().forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(Map.class, "groupGenerators", arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), exprDotMethod(ref("level"), "getLevelNumber")))
                    .forEach(Map.Entry.class, "entry", exprDotMethod(ref("groupGenerators"), "entrySet"))
                    .localMethod(generateOutputBatchedGivenArray, constantFalse(), exprDotMethod(ref("entry"), "getKey"), ref("level"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                    .incrementRef("count");
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
                    CodegenBlock forNew = ifNew.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .incrementRef("count");
                        }
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                }

                CodegenBlock ifOld = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOld.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.class, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(Object.class, "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        CodegenBlock ifNullPut = forLevel.ifCondition(equalsNull(exprDotMethod(arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), levelNumber), "put", ref("groupKey"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifNullPut.localMethod(generateOutputBatchedGivenArray, constantFalse(), ref("groupKey"), ref("level"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                                    .incrementRef("count");
                        }
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().methodReturn(localMethod(generateAndSort, ref(NAME_GROUPREPSPERLEVELBUF), REF_ISSYNTHESIZE, ref("count")));
    }

    private static CodegenMethod generateOutputBatchedCollectViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        CodegenMethod generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);

        Consumer<CodegenMethod> code = methodNode -> {
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

    private static CodegenMethod generateOutputBatchedCollectJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        CodegenMethod generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);

        Consumer<CodegenMethod> code = methodNode -> {
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

    private static CodegenMethod resetEventPerGroupBufCodegen(String memberName, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> methodNode.getBlock().forEach(LinkedHashMap.class, "anEventPerGroupBuf", ref(memberName))
                .exprDotMethod(ref("anEventPerGroupBuf"), "clear");

        return instance.getMethods().addMethod(void.class, "resetEventPerGroupBuf", Collections.emptyList(), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    static CodegenMethod generateGroupKeysViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifRefNullReturnNull("events")
                    .declareVar(Object[][].class, "result", newArrayByLength(Object[].class, arrayLength(ref("events"))))
                    .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                    .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));
            {
                CodegenBlock forLoop = methodNode.getBlock().forLoopIntSimple("i", arrayLength(ref("events")));
                forLoop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("events"), ref("i")))
                        .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), REF_ISNEWDATA))
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

    private static CodegenMethod generateGroupKeysJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(or(equalsNull(ref("events")), exprDotMethod(ref("events"), "isEmpty"))).blockReturn(constantNull())
                    .declareVar(Object[][].class, "result", newArrayByLength(Object[].class, exprDotMethod(ref("events"), "size")))
                    .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(int.class, "count", constant(-1));
            {
                CodegenBlock forLoop = methodNode.getBlock().forEach(MultiKeyArrayOfKeys.class, "eventrow", ref("events"));
                forLoop.incrementRef("count")
                        .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("eventrow"), "getArray")))
                        .declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), REF_ISNEWDATA))
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

    private static CodegenMethod generateAndSortCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EventBean[].class, "oldEventsArr", constantNull())
                    .declareVar(Object[].class, "oldEventSortKeys", constantNull());

            if (forge.isSelectRStream()) {
                methodNode.getBlock().ifCondition(relational(ref("oldEventCount"), GT, constant(0)))
                        .declareVar(EventsAndSortKeysPair.class, "pair", staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, METHOD_GETOLDEVENTSSORTKEYS, ref("oldEventCount"), ref(NAME_RSTREAMEVENTSORTARRAYBUF), MEMBER_ORDERBYPROCESSOR, exprDotMethod(ref("this"), "getGroupByRollupDesc")))
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
                        .assignRef("newEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), MEMBER_AGENTINSTANCECONTEXT));
                if (forge.isSelectRStream()) {
                    methodNode.getBlock().assignRef("oldEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("oldEventSortKeys"), MEMBER_AGENTINSTANCECONTEXT));
                }
            }

            methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
        };

        return instance.getMethods().addMethod(UniformPair.class, "generateAndSort",
                CodegenNamedParam.from(Map[].class, "outputLimitGroupRepsPerLevel", boolean.class, NAME_ISSYNTHESIZE, int.class, "oldEventCount"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    static void applyViewResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeysRow = generateGroupKeysRowCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
        {
            CodegenBlock ifNew = method.getBlock().ifCondition(notEqualsNull(REF_NEWDATA));
            {
                ifNew.forEach(EventBean.class, "aNewData", REF_NEWDATA)
                        .assignArrayElement("eventsPerStream", constant(0), ref("aNewData"))
                        .declareVar(Object[].class, "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantTrue()))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("keys"), MEMBER_AGENTINSTANCECONTEXT);
            }
        }
        {
            CodegenBlock ifOld = method.getBlock().ifCondition(notEqualsNull(REF_OLDDATA));
            {
                ifOld.forEach(EventBean.class, "anOldData", REF_OLDDATA)
                        .assignArrayElement("eventsPerStream", constant(0), ref("anOldData"))
                        .declareVar(Object[].class, "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantFalse()))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("keys"), MEMBER_AGENTINSTANCECONTEXT);
            }
        }
    }

    public static void applyJoinResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeysRow = generateGroupKeysRowCodegen(forge, classScope, instance);

        method.getBlock().declareVarNoInit(EventBean[].class, "eventsPerStream");
        {
            CodegenBlock ifNew = method.getBlock().ifCondition(notEqualsNull(REF_NEWDATA));
            {
                ifNew.forEach(MultiKeyArrayOfKeys.class, "mk", REF_NEWDATA)
                        .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("mk"), "getArray")))
                        .declareVar(Object[].class, "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantTrue()))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("keys"), MEMBER_AGENTINSTANCECONTEXT);
            }
        }
        {
            CodegenBlock ifOld = method.getBlock().ifCondition(notEqualsNull(REF_OLDDATA));
            {
                ifOld.forEach(MultiKeyArrayOfKeys.class, "mk", REF_OLDDATA)
                        .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("mk"), "getArray")))
                        .declareVar(Object[].class, "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantFalse()))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("keys"), MEMBER_AGENTINSTANCECONTEXT);
            }
        }
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorRowPerGroupRollupForge forge, String methodName, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));

        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorRowPerGroupRollupOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(factory, "makeRSRowPerGroupRollupAll", MEMBER_AGENTINSTANCECONTEXT, ref("this"), constant(forge.getGroupKeyTypes()), eventTypes));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorRowPerGroupRollupOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(factory, "makeRSRowPerGroupRollupLast", MEMBER_AGENTINSTANCECONTEXT, ref("this"), constant(forge.getGroupKeyTypes()), eventTypes));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenMethod method) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputView", REF_ISSYNTHESIZE));
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputView", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenMethod method) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    static void stopMethodCodegenBound(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPERS)) {
            method.getBlock().forEach(ResultSetProcessorGroupedOutputFirstHelper.class, "helper", member(NAME_OUTPUTFIRSTHELPERS))
                    .exprDotMethod(ref("helper"), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), "destroy");
        }
    }

    private static CodegenMethod generateGroupKeysRowCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> methodNode.getBlock().declareVar(Object.class, "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), REF_ISNEWDATA))
                .declareVar(AggregationGroupByRollupLevel[].class, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                .declareVar(Object[].class, "result", newArrayByLength(Object.class, arrayLength(ref("levels"))))
                .forLoopIntSimple("j", arrayLength(ref("levels")))
                .declareVar(Object.class, "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                .assignArrayElement("result", ref("j"), ref("subkey"))
                .blockEnd()
                .methodReturn(ref("result"));

        return instance.getMethods().addMethod(Object[].class, "generateGroupKeysRow", CodegenNamedParam.from(EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA), ResultSetProcessorUtil.class, classScope, code);
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

    private static void initOutputFirstHelpers(CodegenExpressionField outputConditionFactory, CodegenInstanceAux instance, ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope) {
        if (!instance.hasMember(NAME_OUTPUTFIRSTHELPERS)) {
            CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
            instance.addMember(NAME_OUTPUTFIRSTHELPERS, ResultSetProcessorGroupedOutputFirstHelper[].class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPERS, staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, "initializeOutputFirstHelpers", factory,
                MEMBER_AGENTINSTANCECONTEXT, constant(forge.getGroupKeyTypes()), exprDotMethod(ref("this"), "getGroupByRollupDesc"), outputConditionFactory));
        }
    }
}
