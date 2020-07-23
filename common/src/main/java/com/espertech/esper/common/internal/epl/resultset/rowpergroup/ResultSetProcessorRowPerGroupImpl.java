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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.StateMgmtSetting;
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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
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

import java.util.Objects;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
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
        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
            .ifCondition(notEqualsNull(REF_NEWDATA))
            .forEach(EventBean.EPTYPE, "aNewData", REF_NEWDATA)
            .assignArrayElement("eventsPerStream", constant(0), ref("aNewData"))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT)
            .blockEnd()
            .blockEnd()
            .ifCondition(notEqualsNull(REF_OLDDATA))
            .forEach(EventBean.EPTYPE, "anOldData", REF_OLDDATA)
            .assignArrayElement("eventsPerStream", constant(0), ref("anOldData"))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT)
            .blockEnd()
            .blockEnd();
    }

    public static void applyJoinResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock()
            .ifCondition(not(exprDotMethod(REF_NEWDATA, "isEmpty")))
            .forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewEvent", REF_NEWDATA)
            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewEvent"), "getArray")))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT)
            .blockEnd()
            .blockEnd()
            .ifCondition(and(notEqualsNull(REF_OLDDATA), not(exprDotMethod(REF_OLDDATA, "isEmpty"))))
            .forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldEvent", REF_OLDDATA)
            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldEvent"), "getArray")))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT)
            .blockEnd()
            .blockEnd();
    }

    public static void processJoinResultCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeyArrayJoin = generateGroupKeyArrayJoinTakingMapCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "keysAndEvents", newInstance(EPTypePremade.HASHMAP.getEPType()))
            .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(generateGroupKeyArrayJoin, REF_NEWDATA, ref("keysAndEvents"), constantTrue()))
            .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(generateGroupKeyArrayJoin, REF_OLDDATA, ref("keysAndEvents"), constantFalse()));

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
            .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"))
            .declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE))
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
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "keysAndEvents", newInstance(EPTypePremade.HASHMAP.getEPType()))
            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
            .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(generateGroupKeysKeepEvent, REF_NEWDATA, ref("keysAndEvents"), constantTrue(), ref("eventsPerStream")))
            .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(generateGroupKeysKeepEvent, REF_OLDDATA, ref("keysAndEvents"), constantFalse(), ref("eventsPerStream")))
            .declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, ref("keysAndEvents"), constantFalse(), REF_ISSYNTHESIZE, ref("eventsPerStream")) : constantNull())
            .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"), ref("eventsPerStream"))
            .declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(generateOutputEventsView, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE, ref("eventsPerStream")))
            .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    static CodegenMethod generateOutputEventsViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(EventBean.EPTYPEARRAY, "events", newArrayByLength(EventBean.EPTYPE, exprDotMethod(ref("keysAndEvents"), "size")))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", newArrayByLength(EPTypePremade.OBJECT.getEPType(), exprDotMethod(ref("keysAndEvents"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean.EPTYPEARRAYARRAY, "currentGenerators", newArrayByLength(EventBean.EPTYPEARRAY, exprDotMethod(ref("keysAndEvents"), "size")));
            }

            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0))
                .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "cpid", exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), ref("cpid"), constantNull())
                    .assignArrayElement(REF_EPS, constant(0), cast(EventBean.EPTYPE, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_EXPREVALCONTEXT))).blockContinue();
                }

                forEach.assignArrayElement("events", ref("count"), exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT))
                    .assignArrayElement("keys", ref("count"), exprDotMethod(ref("entry"), "getKey"));

                if (forge.isSorting()) {
                    forEach.assignArrayElement("currentGenerators", ref("count"), newArrayWithInit(EventBean.EPTYPE, cast(EventBean.EPTYPE, exprDotMethod(ref("entry"), "getValue"))));
                }

                forEach.incrementRef("count")
                    .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("count"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean.EPTYPEARRAY, "generateOutputEventsView",
            CodegenNamedParam.from(EPTypePremade.MAP.getEPType(), "keysAndEvents", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EventBean.EPTYPEARRAY, NAME_EPS),
            ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedRowFromMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        Consumer<CodegenMethod> code = method -> {
            method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));
            {
                CodegenBlock forLoop = method.getBlock().forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forLoop.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull())
                    .assignArrayElement("eventsPerStream", constant(0), cast(EventBean.EPTYPE, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_EXPREVALCONTEXT))).blockContinue();
                }

                forLoop.exprDotMethod(ref("resultEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));

                if (forge.isSorting()) {
                    forLoop.exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_EXPREVALCONTEXT));
                }
            }
        };

        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedRowFromMap",
            CodegenNamedParam.from(EPTypePremade.MAP.getEPType(), "keysAndEvents", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LIST.getEPType(), "resultEvents", EPTypePremade.LIST.getEPType(), "optSortKeys", ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedArrFromIteratorCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);

        Consumer<CodegenMethod> code = method -> method.getBlock().whileLoop(exprDotMethod(ref("keysAndEvents"), "hasNext"))
            .declareVar(EPTypePremade.MAPENTRY.getEPType(), "entry", cast(EPTypePremade.MAPENTRY.getEPType(), exprDotMethod(ref("keysAndEvents"), "next")))
            .localMethod(generateOutputBatchedRowAddToList, ref("join"), exprDotMethod(ref("entry"), "getKey"), cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("resultEvents"), ref("optSortKeys"));

        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedArrFromIterator",
            CodegenNamedParam.from(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "join", EPTypePremade.ITERATOR.getEPType(), "keysAndEvents", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LIST.getEPType(), "resultEvents", EPTypePremade.LIST.getEPType(), "optSortKeys"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputBatchedRowAddToListCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                method.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_EXPREVALCONTEXT)))
                    .blockReturnNoValue();
            }

            method.getBlock().exprDotMethod(ref("resultEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));

            if (forge.isSorting()) {
                method.getBlock().exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", REF_EPS, REF_ISNEWDATA, MEMBER_EXPREVALCONTEXT));
            }
        };

        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedRowAddToList",
            CodegenNamedParam.from(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "join", EPTypePremade.OBJECT.getEPType(), "mk", EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LIST.getEPType(), "resultEvents", EPTypePremade.LIST.getEPType(), "optSortKeys"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedNoSortWMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_EXPREVALCONTEXT)))
                    .blockReturn(constantNull());
            }
            methodNode.getBlock().methodReturn(exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
        };

        return instance.getMethods().addMethod(EventBean.EPTYPE, "generateOutputBatchedNoSortWMap",
            CodegenNamedParam.from(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "join", EPTypePremade.OBJECT.getEPType(), "mk", EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE),
            ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputEventsJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(EventBean.EPTYPEARRAY, "events", newArrayByLength(EventBean.EPTYPE, exprDotMethod(ref("keysAndEvents"), "size")))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", newArrayByLength(EPTypePremade.OBJECT.getEPType(), exprDotMethod(ref("keysAndEvents"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean.EPTYPEARRAYARRAY, "currentGenerators", newArrayByLength(EventBean.EPTYPEARRAY, exprDotMethod(ref("keysAndEvents"), "size")));
            }

            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0))
                .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "cpid", exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"));
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("entry"), "getKey"), ref("cpid"), constantNull())
                    .declareVar(EventBean.EPTYPEARRAY, NAME_EPS, cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_EXPREVALCONTEXT))).blockContinue();
                }

                forEach.assignArrayElement("events", ref("count"), exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT))
                    .assignArrayElement("keys", ref("count"), exprDotMethod(ref("entry"), "getKey"));

                if (forge.isSorting()) {
                    forEach.assignArrayElement("currentGenerators", ref("count"), ref("eventsPerStream"));
                }

                forEach.incrementRef("count")
                    .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("count"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean.EPTYPEARRAY, "generateOutputEventsJoin",
            CodegenNamedParam.from(EPTypePremade.MAP.getEPType(), "keysAndEvents", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE),
            ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod generateGroupKeysKeepEventCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifRefNullReturnNull("events")
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", newArrayByLength(EPTypePremade.OBJECT.getEPType(), arrayLength(ref("events"))));
            {
                methodNode.getBlock().forLoopIntSimple("i", arrayLength(ref("events")))
                    .assignArrayElement(REF_EPS, constant(0), arrayAtIndex(ref("events"), ref("i")))
                    .assignArrayElement("keys", ref("i"), localMethod(forge.getGenerateGroupKeySingle(), REF_EPS, REF_ISNEWDATA))
                    .exprDotMethod(ref("eventPerKey"), "put", arrayAtIndex(ref("keys"), ref("i")), arrayAtIndex(ref("events"), ref("i")))
                    .blockEnd();
            }
            methodNode.getBlock().methodReturn(ref("keys"));
        };

        return instance.getMethods().addMethod(EPTypePremade.OBJECTARRAY.getEPType(), "generateGroupKeysKeepEvent",
            CodegenNamedParam.from(EventBean.EPTYPEARRAY, "events", EPTypePremade.MAP.getEPType(), "eventPerKey", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EventBean.EPTYPEARRAY, NAME_EPS), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod generateGroupKeyArrayJoinTakingMapCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(or(equalsNull(ref("resultSet")), exprDotMethod(ref("resultSet"), "isEmpty"))).blockReturn(constantNull())
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", newArrayByLength(EPTypePremade.OBJECT.getEPType(), exprDotMethod(ref("resultSet"), "size")))
                .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
            {
                methodNode.getBlock().forEach(MultiKeyArrayOfKeys.EPTYPE, "eventsPerStream", ref("resultSet"))
                    .declareVar(EventBean.EPTYPEARRAY, "eps", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("eventsPerStream"), "getArray")))
                    .assignArrayElement("keys", ref("count"), localMethod(forge.getGenerateGroupKeySingle(), ref("eps"), REF_ISNEWDATA))
                    .exprDotMethod(ref("eventPerKey"), "put", arrayAtIndex(ref("keys"), ref("count")), ref("eps"))
                    .incrementRef("count")
                    .blockEnd();
            }
            methodNode.getBlock().methodReturn(ref("keys"));
        };

        return instance.getMethods().addMethod(EPTypePremade.OBJECTARRAY.getEPType(), "generateGroupKeyArrayJoinTakingMapCodegen",
            CodegenNamedParam.from(EPTypePremade.SET.getEPType(), "resultSet", EPTypePremade.MAP.getEPType(), "eventPerKey", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public static void getIteratorViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE));
            return;
        }

        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_EXPREVALCONTEXT)
            .declareVar(EPTypePremade.ITERATOR.getEPType(), "it", exprDotMethod(REF_VIEWABLE, "iterator"))
            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));

        {
            method.getBlock().whileLoop(exprDotMethod(ref("it"), "hasNext"))
                .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.EPTYPE, exprDotMethod(ref("it"), "next")))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKey"), MEMBER_EXPREVALCONTEXT)
                .blockEnd();
        }

        method.getBlock().declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE)))
            .exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_EXPREVALCONTEXT)
            .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod parent, CodegenInstanceAux instance) {
        CodegenMethod iterator = parent.makeChild(EPTypePremade.ITERATOR.getEPType(), ResultSetProcessorRowPerGroupImpl.class, classScope).addParam(Viewable.EPTYPE, NAME_VIEWABLE);
        if (!forge.isSorting()) {
            iterator.getBlock().methodReturn(newInstance(ResultSetProcessorRowPerGroupIterator.EPTYPE, exprDotMethod(REF_VIEWABLE, "iterator"), ref("this"), MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT));
            return iterator;
        }

        CodegenMethod getIteratorSorted = getIteratorSortedCodegen(forge, classScope, instance);
        iterator.getBlock().methodReturn(localMethod(getIteratorSorted, exprDotMethod(REF_VIEWABLE, "iterator")));
        return iterator;
    }

    static CodegenMethod getIteratorSortedCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        Consumer<CodegenMethod> code = method -> {
            method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .declareVar(EPTypePremade.ARRAYLIST.getEPType(), "outgoingEvents", newInstance(EPTypePremade.ARRAYLIST.getEPType()))
                .declareVar(EPTypePremade.ARRAYLIST.getEPType(), "orderKeys", newInstance(EPTypePremade.ARRAYLIST.getEPType()))
                .declareVar(EPTypePremade.SET.getEPType(), "priorSeenGroups", newInstance(EPTypePremade.HASHSET.getEPType()));

            {
                CodegenBlock whileLoop = method.getBlock().whileLoop(exprDotMethod(ref("parentIter"), "hasNext"));
                whileLoop.declareVar(EventBean.EPTYPE, "candidate", cast(EventBean.EPTYPE, exprDotMethod(ref("parentIter"), "next")))
                    .assignArrayElement("eventsPerStream", constant(0), ref("candidate"))
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull());

                if (forge.getOptionalHavingNode() != null) {
                    whileLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, constantTrue(), MEMBER_EXPREVALCONTEXT))).blockContinue();
                }
                whileLoop.ifCondition(exprDotMethod(ref("priorSeenGroups"), "contains", ref("groupKey"))).blockContinue();

                whileLoop.exprDotMethod(ref("priorSeenGroups"), "add", ref("groupKey"))
                    .exprDotMethod(ref("outgoingEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), constantTrue(), constantTrue(), MEMBER_EXPREVALCONTEXT))
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "orderKey", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), MEMBER_EXPREVALCONTEXT))
                    .exprDotMethod(ref("orderKeys"), "add", ref("orderKey"));
            }

            method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_ORDEROUTGOINGGETITERATOR, ref("outgoingEvents"), ref("orderKeys"), MEMBER_ORDERBYPROCESSOR, MEMBER_EXPREVALCONTEXT));
        };
        return instance.getMethods().addMethod(EPTypePremade.ITERATOR.getEPType(), "getIteratorSorted", CodegenNamedParam.from(EPTypePremade.ITERATOR.getEPType(), "parentIter"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeyArrayJoin = generateGroupKeyArrayJoinTakingMapCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);
        method.getBlock()
            .declareVar(EPTypePremade.MAP.getEPType(), "keysAndEvents", newInstance(EPTypePremade.HASHMAP.getEPType()))
            .expression(localMethod(generateGroupKeyArrayJoin, REF_JOINSET, ref("keysAndEvents"), constantTrue()))
            .declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(generateOutputEventsJoin, ref("keysAndEvents"), constantTrue(), constantTrue()))
            .methodReturn(newInstance(ArrayEventIterator.EPTYPE, ref("selectNewEvents")));
    }

    public static void clearMethodCodegen(CodegenMethod method) {
        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_EXPREVALCONTEXT);
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

        instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "removedAggregationGroupKey", CodegenNamedParam.from(EPTypePremade.OBJECT.getEPType(), "key"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorRowPerGroupForge forge, String methodName, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType.EPTYPEARRAY, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);

        if (forge.isOutputAll()) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorRowPerGroupOutputAllHelper.EPTYPE);
            StateMgmtSetting stateMgmtSettings = forge.getOutputAllOptHelperSettings().get();
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(factory, "makeRSRowPerGroupOutputAllOpt", MEMBER_EXPREVALCONTEXT, ref("this"), constant(forge.getGroupKeyTypes()), groupKeyMKSerde, eventTypes, stateMgmtSettings.toExpression()));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorRowPerGroupOutputLastHelper.EPTYPE);
            StateMgmtSetting stateMgmtSettings = forge.getOutputLastOptHelperSettings().get();
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(factory, "makeRSRowPerGroupOutputLastOpt", MEMBER_EXPREVALCONTEXT, ref("this"), constant(forge.getGroupKeyTypes()), groupKeyMKSerde, eventTypes, stateMgmtSettings.toExpression()));
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

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "groupRepsView", newInstance(EPTypePremade.HASHMAP.getEPType()));
        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNewData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"));
                    forNew.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
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
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.EPTYPE, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.EPTYPE);
        StateMgmtSetting outputHelperSettings = forge.getOutputFirstSettings().get();
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(helperFactory, "makeRSGroupedOutputFirst", MEMBER_EXPREVALCONTEXT, groupKeyTypes, outputFactory, constantNull(), constant(-1), groupKeyMKSerde, outputHelperSettings.toExpression()));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "groupRepsView", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));

        if (forge.getOptionalHavingNode() == null) {
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"));
                        forloop.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                            .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_EXPREVALCONTEXT, outputFactory))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"));
                        forloop.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
                            .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_EXPREVALCONTEXT, outputFactory))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
                    }
                }
            }
        } else {
            method.getBlock().exprDotMethod(ref("groupRepsView"), "clear");
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantTrue()))
                    .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    ifNewData.declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull())
                            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                            .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), MEMBER_EXPREVALCONTEXT)))
                            .incrementRef("count")
                            .blockContinue();

                        forloop.declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_EXPREVALCONTEXT, outputFactory))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
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
                        .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull())
                            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                            .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), MEMBER_EXPREVALCONTEXT)))
                            .incrementRef("count")
                            .blockContinue();

                        forloop.declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_EXPREVALCONTEXT, outputFactory))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
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
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType.EPTYPEARRAY, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.EPTYPE);
        StateMgmtSetting stateMgmtSettings = forge.getOutputAllHelperSettings().get();
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(helperFactory, "makeRSGroupedOutputAllNoOpt", MEMBER_EXPREVALCONTEXT, constant(forge.getGroupKeyTypes()), groupKeyMKSerde, eventTypes, stateMgmtSettings.toExpression()));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        if (forge.isSelectRStream()) {
            method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
        }

        {
            CodegenBlock forLoop = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forLoop.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")));

            if (forge.isUnidirectional()) {
                forLoop.exprDotMethod(ref("this"), "clear");
            }

            {
                CodegenBlock ifNewData = forLoop.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNewData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"));
                    forNew.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forLoop.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
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

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "keysAndEvents", newInstance(EPTypePremade.HASHMAP.getEPType()));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            forEach.declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(generateGroupKeyArrayJoinTakingMap, ref("newData"), ref("keysAndEvents"), constantTrue()))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(generateGroupKeyArrayJoinTakingMap, ref("oldData"), ref("keysAndEvents"), constantFalse()));

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethodChain(ref("keysAndEvents")).add("entrySet").add("iterator"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"))
                .localMethod(generateOutputBatchedArrFromIterator, constantTrue(), exprDotMethodChain(ref("keysAndEvents")).add("entrySet").add("iterator"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"))
                .exprDotMethod(ref("keysAndEvents"), "clear");
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedRowAddToList = generateOutputBatchedRowAddToListCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedArrFromIterator = generateOutputBatchedArrFromIteratorCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "groupRepsView", newInstance(EPTypePremade.HASHMAP.getEPType()));
        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")));

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNewData.forEach(EventBean.EPTYPE, "aNewData", ref("newData"));
                    forNew.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("aNewData")))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"));
                    forOld.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
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
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.EPTYPE, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.EPTYPE);
        StateMgmtSetting outputHelperSettings = forge.getOutputFirstSettings().get();
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(helperFactory, "makeRSGroupedOutputFirst", MEMBER_EXPREVALCONTEXT, groupKeyTypes, outputFactory, constantNull(), constant(-1), groupKeyMKSerde, outputHelperSettings.toExpression()));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "groupRepsView", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));

        if (forge.getOptionalHavingNode() == null) {
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forEach(EventBean.EPTYPE, "aNewData", ref("newData"));
                        forloop.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("aNewData")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                            .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_EXPREVALCONTEXT, outputFactory))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"));
                        forloop.declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
                            .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_EXPREVALCONTEXT, outputFactory))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        CodegenBlock ifExists = ifPass.ifCondition(equalsNull(exprDotMethod(ref("groupRepsView"), "put", ref("mk"), ref("eventsPerStream"))));
                        if (forge.isSelectRStream()) {
                            ifExists.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                        }
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
                    }
                }
            }
        } else {
            // having clause present, having clause evaluates at the level of individual posts
            method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStreamOneStream", newArrayByLength(EventBean.EPTYPE, constant(1)));

            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()))
                    .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStreamOneStream"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                            .assignArrayElement("eventsPerStreamOneStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull())
                            .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStreamOneStream"), constantTrue(), MEMBER_EXPREVALCONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_EXPREVALCONTEXT, outputFactory))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"))
                            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayWithInit(EventBean.EPTYPE, arrayAtIndex(ref("newData"), ref("i"))));
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
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("i")))
                            .assignArrayElement("eventsPerStreamOneStream", constant(0), arrayAtIndex(ref("oldData"), ref("i")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull())
                            .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStreamOneStream"), constantFalse(), MEMBER_EXPREVALCONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_EXPREVALCONTEXT, outputFactory))
                            .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"))
                            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayWithInit(EventBean.EPTYPE, arrayAtIndex(ref("oldData"), ref("i"))));
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
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType.EPTYPEARRAY, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.EPTYPE);
        StateMgmtSetting stateMgmtSettings = forge.getOutputAllHelperSettings().get();
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(helperFactory, "makeRSGroupedOutputAllNoOpt", MEMBER_EXPREVALCONTEXT, constant(forge.getGroupKeyTypes()), groupKeyMKSerde, eventTypes, stateMgmtSettings.toExpression()));

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));
        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        if (forge.isSelectRStream()) {
            method.getBlock().localMethod(generateOutputBatchedArrFromIterator, constantFalse(), exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
        }

        {
            CodegenBlock forLoop = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forLoop.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")));

            {
                CodegenBlock ifNewData = forLoop.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNewData.forEach(EventBean.EPTYPE, "aNewData", ref("newData"));
                    forNew.assignArrayElement(ref("eventsPerStream"), constant(0), ref("aNewData"))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    CodegenBlock ifNotFound = forNew.ifCondition(equalsNull(exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.EPTYPE, ref("aNewData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantTrue(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
                }
            }

            {
                CodegenBlock ifOldData = forLoop.ifCondition(notEqualsNull(ref("oldData")));
                {
                    CodegenBlock forOld = ifOldData.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"));
                    forOld.assignArrayElement(ref("eventsPerStream"), constant(0), ref("anOldData"))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    CodegenBlock ifNotFound = forOld.ifCondition(equalsNull(exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))));
                    if (forge.isSelectRStream()) {
                        ifNotFound.localMethod(generateOutputBatchedRowAddToList, constantFalse(), ref("mk"), ref("eventsPerStream"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
                    }
                    forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_EXPREVALCONTEXT);
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

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "keysAndEvents", newInstance(EPTypePremade.HASHMAP.getEPType()))
            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));
        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(generateGroupKeysKeepEvent, ref("newData"), ref("keysAndEvents"), constantTrue(), ref("eventsPerStream")))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(generateGroupKeysKeepEvent, ref("oldData"), ref("keysAndEvents"), constantFalse(), ref("eventsPerStream")));

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedRowFromMap, ref("keysAndEvents"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), MEMBER_EXPREVALCONTEXT);
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"))
                .localMethod(generateOutputBatchedRowFromMap, ref("keysAndEvents"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_EXPREVALCONTEXT)
                .exprDotMethod(ref("keysAndEvents"), "clear");
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    static CodegenMethod shortcutEvalGivenKeyCodegen(ExprForge optionalHavingNode, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull());
            if (optionalHavingNode != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_EXPREVALCONTEXT))).blockReturn(constantNull());
            }
            methodNode.getBlock().methodReturn(exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
        };

        return instance.getMethods().addMethod(EventBean.EPTYPE, "shortcutEvalGivenKey",
            CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.OBJECT.getEPType(), "groupKey", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE),
            ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod processViewResultPairDepthOneNoRStreamCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(EPTypePremade.OBJECT.getEPType(), "newGroupKey", localMethod(forge.getGenerateGroupKeySingle(), REF_NEWDATA, constantTrue()))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "oldGroupKey", localMethod(forge.getGenerateGroupKeySingle(), REF_OLDDATA, constantFalse()))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("newGroupKey"), MEMBER_EXPREVALCONTEXT)
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", REF_OLDDATA, ref("oldGroupKey"), MEMBER_EXPREVALCONTEXT)
                .ifCondition(staticMethod(Objects.class, "equals", ref("newGroupKey"), ref("oldGroupKey")))
                .declareVar(EventBean.EPTYPE, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("newGroupKey"), constantTrue(), REF_ISSYNTHESIZE))
                .blockReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")))
                .declareVar(EventBean.EPTYPE, "newKeyEvent", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("newGroupKey"), constant(true), REF_ISSYNTHESIZE))
                .declareVar(EventBean.EPTYPE, "oldKeyEvent", localMethod(shortcutEvalGivenKey, REF_OLDDATA, ref("oldGroupKey"), constant(true), REF_ISSYNTHESIZE));

            if (forge.isSorting()) {
                methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("newGroupKey"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull())
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "newSortKey", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", REF_NEWDATA, constantTrue(), MEMBER_EXPREVALCONTEXT))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("newGroupKey"), exprDotMethod(MEMBER_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull())
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "oldSortKey", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", REF_OLDDATA, constantTrue(), MEMBER_EXPREVALCONTEXT))
                    .declareVar(EventBean.EPTYPEARRAY, "sorted", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortTwoKeys", ref("newKeyEvent"), ref("newSortKey"), ref("oldKeyEvent"), ref("oldSortKey")))
                    .methodReturn(newInstance(UniformPair.EPTYPE, ref("sorted"), constantNull()));
            } else {
                methodNode.getBlock()
                    .ifCondition(and(notEqualsNull(ref("newKeyEvent")), notEqualsNull(ref("oldKeyEvent"))))
                    .blockReturn(newInstance(UniformPair.EPTYPE, newArrayWithInit(EventBean.EPTYPE, ref("newKeyEvent"), ref("oldKeyEvent")), constantNull()))
                    .ifCondition(notEqualsNull(ref("newKeyEvent")))
                    .blockReturn(newInstance(UniformPair.EPTYPE, newArrayWithInit(EventBean.EPTYPE, ref("newKeyEvent")), constantNull()))
                    .ifCondition(notEqualsNull(ref("oldKeyEvent")))
                    .blockReturn(newInstance(UniformPair.EPTYPE, newArrayWithInit(EventBean.EPTYPE, ref("oldKeyEvent")), constantNull()))
                    .methodReturn(constantNull());
            }
        };

        return instance.getMethods().addMethod(UniformPair.EPTYPE, "processViewResultPairDepthOneNoRStream", CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_NEWDATA, EventBean.EPTYPEARRAY, NAME_OLDDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    static CodegenMethod processViewResultNewDepthOneCodegen(ResultSetProcessorRowPerGroupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", localMethod(forge.getGenerateGroupKeySingle(), REF_NEWDATA, constantTrue()));
            if (forge.isSelectRStream()) {
                methodNode.getBlock().declareVar(EventBean.EPTYPE, "rstream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("groupKey"), constantFalse(), REF_ISSYNTHESIZE));
            }
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("groupKey"), MEMBER_EXPREVALCONTEXT)
                .declareVar(EventBean.EPTYPE, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("groupKey"), constantTrue(), REF_ISSYNTHESIZE));
            if (forge.isSelectRStream()) {
                methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfAllNullSingle", ref("istream"), ref("rstream")));
            } else {
                methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")));
            }
        };

        return instance.getMethods().addMethod(UniformPair.EPTYPE, "processViewResultNewDepthOneCodegen", CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_NEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }
}
