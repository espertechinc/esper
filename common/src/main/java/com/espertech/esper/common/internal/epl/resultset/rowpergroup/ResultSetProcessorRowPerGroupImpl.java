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
package com.espertech.esper.common.internal.epl.resultset.rowpergroup;

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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolled;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactoryField;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputAllGroupReps;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.NAME_EPS;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil.*;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.METHOD_APPLYAGGJOINRESULTKEYEDJOIN;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.METHOD_APPLYAGGVIEWRESULTKEYEDVIEW;

/**
 * Result set processor for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 * <p>
 * Produces one row for each group that changed (and not one row per event). Computes MultiKey group-by keys for
 * each event and uses a set of the group-by keys to generate the result rows, using the first (old or new, anyone) event
 * for each distinct group-by key.
 */
public class ResultSetProcessorRowPerGroupImpl {
    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";
    private final static String NAME_OUTPUTFIRSTHELPER = "outputFirstHelper";
    private final static String NAME_OUTPUTALLGROUPREPS = "outputAllGroupReps";

    public static void applyViewResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
            .ifCondition(notEqualsNull(REF_NEWDATA))
            .forEach(EventBean.class, "aNewData", REF_NEWDATA)
            .assignArrayElement("eventsPerStream", constant(0), ref("aNewData"))
            .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
            .blockEnd()
            .blockEnd()
            .ifCondition(notEqualsNull(REF_OLDDATA))
            .forEach(EventBean.class, "anOldData", REF_OLDDATA)
            .assignArrayElement("eventsPerStream", constant(0), ref("anOldData"))
            .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
            .blockEnd()
            .blockEnd();
    }

    public static void applyJoinResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock()
            .ifCondition(not(exprDotMethod(REF_NEWDATA, "isEmpty")))
            .forEach(MultiKeyArrayOfKeys.class, "aNewEvent", REF_NEWDATA)
            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewEvent"), "getArray")))
            .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
            .blockEnd()
            .blockEnd()
            .ifCondition(and(notEqualsNull(REF_OLDDATA), not(exprDotMethod(REF_OLDDATA, "isEmpty"))))
            .forEach(MultiKeyArrayOfKeys.class, "anOldEvent", REF_OLDDATA)
            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldEvent"), "getArray")))
            .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
            .blockEnd()
            .blockEnd();
    }

    public static void processJoinResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeyArrayJoin = generateGroupKeyArrayJoinTakingMapCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        method.getBlock().declareVar(Map.class, "keysAndEvents", newInstance(HashMap.class))
            .declareVar(Object[].class, "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, REF_NEWDATA, ref("keysAndEvents"), constantTrue()))
            .declareVar(Object[].class, "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, REF_OLDDATA, ref("keysAndEvents"), constantFalse()));

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }

        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
            .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"))
            .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE))
            .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    public static void processViewResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeysKeepEvent = generateGroupKeysKeepEventCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);
        CodegenMethod processViewResultNewDepthOne = processViewResultNewDepthOneCodegen(forge, classScope, instance);
        CodegenMethod processViewResultPairDepthOneNoRStream = processViewResultPairDepthOneNoRStreamCodegen(forge, classScope, instance);

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
            .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"), ref("eventsPerStream"))
            .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsView, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE, ref("eventsPerStream")))
            .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    static CodegenMethod generateOutputEventsViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, exprDotMethod(ref("keysAndEvents"), "size")))
                .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("keysAndEvents"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean[][].class, "currentGenerators", newArrayByLength(EventBean[].class, exprDotMethod(ref("keysAndEvents"), "size")));
            }

            methodNode.getBlock().declareVar(int.class, "count", constant(0))
                .declareVar(int.class, "cpid", exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), ref("cpid"), constantNull())
                    .assignArrayElement(REF_EPS, constant(0), cast(EventBean.class, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forEach.assignArrayElement("events", ref("count"), exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT))
                    .assignArrayElement("keys", ref("count"), exprDotMethod(ref("entry"), "getKey"));

                if (forge.isSorting()) {
                    forEach.assignArrayElement("currentGenerators", ref("count"), newArrayWithInit(EventBean.class, cast(EventBean.class, exprDotMethod(ref("entry"), "getValue"))));
                }

                forEach.incrementRef("count")
                    .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("count"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsView",
            CodegenNamedParam.from(Map.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, EventBean[].class, NAME_EPS),
            ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedRowFromMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        Consumer<CodegenMethod> code = method -> {
            method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
            {
                CodegenBlock forLoop = method.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forLoop.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                    .assignArrayElement("eventsPerStream", constant(0), cast(EventBean.class, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forLoop.exprDotMethod(ref("resultEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forLoop.exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT));
                }
            }
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedRowFromMap",
            CodegenNamedParam.from(Map.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys", AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedArrFromIteratorCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);

        Consumer<CodegenMethod> code = method -> method.getBlock().whileLoop(exprDotMethod(ref("keysAndEvents"), "hasNext"))
            .declareVar(Map.Entry.class, "entry", cast(Map.Entry.class, exprDotMethod(ref("keysAndEvents"), "next")))
            .localMethod(generateOutputBatchedRowAddToList, ref("join"), exprDotMethod(ref("entry"), "getKey"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("resultEvents"), ref("optSortKeys"));

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedArrFromIterator",
            CodegenNamedParam.from(boolean.class, "join", Iterator.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputBatchedRowAddToListCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                method.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT)))
                    .blockReturnNoValue();
            }

            method.getBlock().exprDotMethod(ref("resultEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

            if (forge.isSorting()) {
                method.getBlock().exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT));
            }
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedRowAddToList",
            CodegenNamedParam.from(boolean.class, "join", Object.class, "mk", EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedNoSortWMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT)))
                    .blockReturn(constantNull());
            }
            methodNode.getBlock().methodReturn(exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
        };

        return instance.getMethods().addMethod(EventBean.class, "generateOutputBatchedNoSortWMap",
            CodegenNamedParam.from(boolean.class, "join", Object.class, "mk", EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE),
            ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputEventsJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, exprDotMethod(ref("keysAndEvents"), "size")))
                .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("keysAndEvents"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean[][].class, "currentGenerators", newArrayByLength(EventBean[].class, exprDotMethod(ref("keysAndEvents"), "size")));
            }

            methodNode.getBlock().declareVar(int.class, "count", constant(0))
                .declareVar(int.class, "cpid", exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), ref("cpid"), constantNull())
                    .declareVar(EventBean[].class, NAME_EPS, cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forEach.assignArrayElement("events", ref("count"), exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT))
                    .assignArrayElement("keys", ref("count"), exprDotMethod(ref("entry"), "getKey"));

                if (forge.isSorting()) {
                    forEach.assignArrayElement("currentGenerators", ref("count"), ref("eventsPerStream"));
                }

                forEach.incrementRef("count")
                    .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("count"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsJoin",
            CodegenNamedParam.from(Map.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE),
            ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod generateGroupKeysKeepEventCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifRefNullReturnNull("events")
                .declareVar(Object[].class, "keys", newArrayByLength(Object.class, arrayLength(ref("events"))));
            {
                methodNode.getBlock().forLoopIntSimple("i", arrayLength(ref("events")))
                    .assignArrayElement(REF_EPS, constant(0), arrayAtIndex(ref("events"), ref("i")))
                    .assignArrayElement("keys", ref("i"), localMethod(forge.getGenerateGroupKeySingle(), REF_EPS, REF_ISNEWDATA))
                    .exprDotMethod(ref("eventPerKey"), "put", arrayAtIndex(ref("keys"), ref("i")), arrayAtIndex(ref("events"), ref("i")))
                    .blockEnd();
            }
            methodNode.getBlock().methodReturn(ref("keys"));
        };

        return instance.getMethods().addMethod(Object[].class, "generateGroupKeysKeepEvent",
            CodegenNamedParam.from(EventBean[].class, "events", Map.class, "eventPerKey", boolean.class, NAME_ISNEWDATA, EventBean[].class, NAME_EPS), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod generateGroupKeyArrayJoinTakingMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(or(equalsNull(ref("resultSet")), exprDotMethod(ref("resultSet"), "isEmpty"))).blockReturn(constantNull())
                .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("resultSet"), "size")))
                .declareVar(int.class, "count", constant(0));
            {
                methodNode.getBlock().forEach(MultiKeyArrayOfKeys.class, "eventsPerStream", ref("resultSet"))
                    .declareVar(EventBean[].class, "eps", cast(EventBean[].class, exprDotMethod(ref("eventsPerStream"), "getArray")))
                    .assignArrayElement("keys", ref("count"), localMethod(forge.getGenerateGroupKeySingle(), ref("eps"), REF_ISNEWDATA))
                    .exprDotMethod(ref("eventPerKey"), "put", arrayAtIndex(ref("keys"), ref("count")), ref("eps"))
                    .incrementRef("count")
                    .blockEnd();
            }
            methodNode.getBlock().methodReturn(ref("keys"));
        };

        return instance.getMethods().addMethod(Object[].class, "generateGroupKeyArrayJoinTakingMapCodegen",
            CodegenNamedParam.from(Set.class, "resultSet", Map.class, "eventPerKey", boolean.class, NAME_ISNEWDATA), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public static void getIteratorViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE));
            return;
        }

        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
            .declareVar(Iterator.class, "it", exprDotMethod(REF_VIEWABLE, "iterator"))
            .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            method.getBlock().whileLoop(exprDotMethod(ref("it"), "hasNext"))
                .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.class, exprDotMethod(ref("it"), "next")))
                .declareVar(Object.class, "groupKey", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT)
                .blockEnd();
        }

        method.getBlock().declareVar(ArrayDeque.class, "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE)))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
            .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod parent, CodegenInstanceAux instance) {
        CodegenMethod iterator = parent.makeChild(Iterator.class, ResultSetProcessorRowPerGroupImpl.class, classScope).addParam(Viewable.class, NAME_VIEWABLE);
        if (!forge.isSorting()) {
            iterator.getBlock().methodReturn(newInstance(ResultSetProcessorRowPerGroupIterator.class, exprDotMethod(REF_VIEWABLE, "iterator"), ref("this"), MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT));
            return iterator;
        }

        CodegenMethod getIteratorSorted = getIteratorSortedCodegen(forge, classScope, instance);
        iterator.getBlock().methodReturn(localMethod(getIteratorSorted, exprDotMethod(REF_VIEWABLE, "iterator")));
        return iterator;
    }

    static CodegenMethod getIteratorSortedCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        Consumer<CodegenMethod> code = method -> {
            method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(ArrayList.class, "outgoingEvents", newInstance(ArrayList.class))
                .declareVar(ArrayList.class, "orderKeys", newInstance(ArrayList.class))
                .declareVar(Set.class, "priorSeenGroups", newInstance(HashSet.class));

            {
                CodegenBlock whileLoop = method.getBlock().whileLoop(exprDotMethod(ref("parentIter"), "hasNext"));
                whileLoop.declareVar(EventBean.class, "candidate", cast(EventBean.class, exprDotMethod(ref("parentIter"), "next")))
                    .assignArrayElement("eventsPerStream", constant(0), ref("candidate"))
                    .declareVar(Object.class, "groupKey", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

                if (forge.getOptionalHavingNode() != null) {
                    whileLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                }
                whileLoop.ifCondition(exprDotMethod(ref("priorSeenGroups"), "contains", ref("groupKey"))).blockContinue();

                whileLoop.exprDotMethod(ref("priorSeenGroups"), "add", ref("groupKey"))
                    .exprDotMethod(ref("outgoingEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))
                    .declareVar(Object.class, "orderKey", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))
                    .exprDotMethod(ref("orderKeys"), "add", ref("orderKey"));
            }

            method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_ORDEROUTGOINGGETITERATOR, ref("outgoingEvents"), ref("orderKeys"), MEMBER_ORDERBYPROCESSOR, MEMBER_AGENTINSTANCECONTEXT));
        };
        return instance.getMethods().addMethod(Iterator.class, "getIteratorSorted", CodegenNamedParam.from(Iterator.class, "parentIter"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeyArrayJoin = generateGroupKeyArrayJoinTakingMapCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);
        method.getBlock()
            .declareVar(Map.class, "keysAndEvents", newInstance(HashMap.class))
            .expression(localMethod(generateGroupKeyArrayJoin, REF_JOINSET, ref("keysAndEvents"), constantTrue()))
            .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantTrue(), constantTrue()))
            .methodReturn(newInstance(ArrayEventIterator.class, ref("selectNewEvents")));
    }

    public static void clearMethodCodegen(CodegenMethod method) {
        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT);
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
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

    public static void processOutputLimitedViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
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

    static void removedAggregationGroupKeyCodegen(CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
                method.getBlock().exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "remove", ref("key"));
            }
            if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
                method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), "remove", ref("key"));
            }
            if (instance.hasMember(NAME_OUTPUTFIRSTHELPER)) {
                method.getBlock().exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "remove", ref("key"));
            }
        };

        instance.getMethods().addMethod(void.class, "removedAggregationGroupKey", CodegenNamedParam.from(Object.class, "key"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorRowPerGroupForge forge, String methodName, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);

        if (forge.isOutputAll()) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorRowPerGroupOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(factory, "makeRSRowPerGroupOutputAllOpt", MEMBER_AGENTINSTANCECONTEXT, ref("this"), constant(forge.getGroupKeyTypes()), groupKeyMKSerde, eventTypes));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorRowPerGroupOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(factory, "makeRSRowPerGroupOutputLastOpt", MEMBER_AGENTINSTANCECONTEXT, ref("this"), constant(forge.getGroupKeyTypes()), groupKeyMKSerde, eventTypes));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenMethod method) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputView", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputView", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenMethod method) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public static void stopMethodCodegenBound(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "destroy");
        }
    }

    public static void acceptHelperVisitorCodegen(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTALLGROUPREPS));
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTALLHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTLASTHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTFIRSTHELPER));
        }
    }

    private static void processOutputLimitedJoinLastCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

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
                    CodegenBlock forNew = ifNewData.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"));
                    forNew.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                        .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                        .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethodChain(ref("groupRepsView")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedJoinFirstCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(helperFactory, "makeRSGroupedOutputFirst", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, outputFactory, constantNull(), constant(-1), groupKeyMKSerde));

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
                        CodegenBlock forloop = ifNewData.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"));
                        forloop.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                            .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                            .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"));
                        forloop.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
                            .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                            .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }
            }
        } else {
            method.getBlock().exprDotMethod(ref("groupRepsView"), "clear");
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantTrue()))
                    .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    ifNewData.declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT)))
                            .incrementRef("count")
                            .blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                            .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.incrementRef("count");
                    }
                }

                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                        .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT)))
                            .incrementRef("count")
                            .blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                            .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.incrementRef("count");
                    }
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethodChain(ref("groupRepsView")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedJoinAllCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(helperFactory, "makeRSGroupedOutputAllNoOpt", MEMBER_AGENTINSTANCECONTEXT, constant(forge.getGroupKeyTypes()), groupKeyMKSerde, eventTypes));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        if (forge.isSelectRStream()) {
            method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
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
                    CodegenBlock forNew = ifNewData.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"));
                    forNew.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                        .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forLoop.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                        .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedJoinDefaultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeyArrayJoinTakingMap = generateGroupKeyArrayJoinTakingMapCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

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

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"))
                .localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethodChain(ref("keysAndEvents")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"))
                .exprDotMethod(ref("keysAndEvents"), "clear");
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

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
                        .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(EventBean.class, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean[].class, "eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                        .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), newArrayWithInit(EventBean.class, ref("anOldData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethodChain(ref("groupRepsView")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewFirstCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(helperFactory, "makeRSGroupedOutputFirst", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, outputFactory, constantNull(), constant(-1), groupKeyMKSerde));

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
                            .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                            .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                            .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forEach(EventBean.class, "anOldData", ref("oldData"));
                        forloop.declareVar(EventBean[].class, "eventsPerStream", newArrayWithInit(EventBean.class, ref("anOldData")))
                            .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
                            .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                            .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
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
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()))
                    .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStreamOneStream"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                            .assignArrayElement("eventsPerStreamOneStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                            .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStreamOneStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
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
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                            .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStreamOneStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
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

    private static void processOutputLimitedViewAllCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {

        CodegenMethod generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(helperFactory, "makeRSGroupedOutputAllNoOpt", MEMBER_AGENTINSTANCECONTEXT, constant(forge.getGroupKeyTypes()), groupKeyMKSerde, eventTypes));

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        if (forge.isSelectRStream()) {
            method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
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
                        .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.class, ref("aNewData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forLoop.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(EventBean.class, "anOldData", ref("oldData"));
                    forOld.assignArrayElement(ref("eventsPerStream"), constant(0), ref("anOldData"))
                        .declareVar(Object.class, "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.class, ref("anOldData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeysKeepEvent = generateGroupKeysKeepEventCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedRowFromMap = generateOutputBatchedRowFromMapCodegen(forge, classScope, instance);

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
                forEach.localMethod(generateOutputBatchedRowFromMap, ref("keysAndEvents"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT);
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"))
                .localMethod(generateOutputBatchedRowFromMap, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT)
                .exprDotMethod(ref("keysAndEvents"), "clear");
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    static CodegenMethod shortcutEvalGivenKeyCodegen(ExprForge optionalHavingNode, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());
            if (optionalHavingNode != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockReturn(constantNull());
            }
            methodNode.getBlock().methodReturn(exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
        };

        return instance.getMethods().addMethod(EventBean.class, "shortcutEvalGivenKey",
            CodegenNamedParam.from(EventBean[].class, NAME_EPS, Object.class, "groupKey", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE),
            ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod processViewResultPairDepthOneNoRStreamCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(Object.class, "newGroupKey", localMethod(forge.getGenerateGroupKeySingle(), REF_NEWDATA, constantTrue()))
                .declareVar(Object.class, "oldGroupKey", localMethod(forge.getGenerateGroupKeySingle(), REF_OLDDATA, constantFalse()))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("newGroupKey"), MEMBER_AGENTINSTANCECONTEXT)
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", REF_OLDDATA, ref("oldGroupKey"), MEMBER_AGENTINSTANCECONTEXT)
                .ifCondition(staticMethod(Objects.class, "equals", ref("newGroupKey"), ref("oldGroupKey")))
                .declareVar(EventBean.class, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("newGroupKey"), constantTrue(), REF_ISSYNTHESIZE))
                .blockReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")))
                .declareVar(EventBean.class, "newKeyEvent", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("newGroupKey"), constant(true), REF_ISSYNTHESIZE))
                .declareVar(EventBean.class, "oldKeyEvent", localMethod(shortcutEvalGivenKey, REF_OLDDATA, ref("oldGroupKey"), constant(true), REF_ISSYNTHESIZE));

            if (forge.isSorting()) {
                methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("newGroupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                    .declareVar(Object.class, "newSortKey", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", REF_NEWDATA, constantTrue(), MEMBER_AGENTINSTANCECONTEXT))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("newGroupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                    .declareVar(Object.class, "oldSortKey", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", REF_OLDDATA, constantTrue(), MEMBER_AGENTINSTANCECONTEXT))
                    .declareVar(EventBean[].class, "sorted", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortTwoKeys", ref("newKeyEvent"), ref("newSortKey"), ref("oldKeyEvent"), ref("oldSortKey")))
                    .methodReturn(newInstance(UniformPair.class, ref("sorted"), constantNull()));
            } else {
                methodNode.getBlock()
                    .ifCondition(and(notEqualsNull(ref("newKeyEvent")), notEqualsNull(ref("oldKeyEvent"))))
                    .blockReturn(newInstance(UniformPair.class, newArrayWithInit(EventBean.class, ref("newKeyEvent"), ref("oldKeyEvent")), constantNull()))
                    .ifCondition(notEqualsNull(ref("newKeyEvent")))
                    .blockReturn(newInstance(UniformPair.class, newArrayWithInit(EventBean.class, ref("newKeyEvent")), constantNull()))
                    .ifCondition(notEqualsNull(ref("oldKeyEvent")))
                    .blockReturn(newInstance(UniformPair.class, newArrayWithInit(EventBean.class, ref("oldKeyEvent")), constantNull()))
                    .methodReturn(constantNull());
            }
        };

        return instance.getMethods().addMethod(UniformPair.class, "processViewResultPairDepthOneNoRStream", CodegenNamedParam.from(EventBean[].class, NAME_NEWDATA, EventBean[].class, NAME_OLDDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod processViewResultNewDepthOneCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(Object.class, "groupKey", localMethod(forge.getGenerateGroupKeySingle(), REF_NEWDATA, constantTrue()));
            if (forge.isSelectRStream()) {
                methodNode.getBlock().declareVar(EventBean.class, "rstream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("groupKey"), constantFalse(), REF_ISSYNTHESIZE));
            }
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT)
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
