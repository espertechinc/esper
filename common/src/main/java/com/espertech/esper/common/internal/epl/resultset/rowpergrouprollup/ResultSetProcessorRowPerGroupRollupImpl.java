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
import com.espertech.esper.common.client.type.EPTypePremade;
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
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Collections;
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
                .declareVar(EPTypePremade.OBJECTARRAYARRAY.getEPType(), "newDataMultiKey", localMethod(generateGroupKeysJoin, REF_NEWDATA, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue()))
                .declareVar(EPTypePremade.OBJECTARRAYARRAY.getEPType(), "oldDataMultiKey", localMethod(generateGroupKeysJoin, REF_OLDDATA, ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse()))
                .declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"))
                .declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    static void processViewResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        addEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, forge, instance, method, classScope);
        CodegenMethod resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);
        CodegenMethod generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        method.getBlock().localMethod(resetEventPerGroupBufView)
                .declareVar(EPTypePremade.OBJECTARRAYARRAY.getEPType(), "newDataMultiKey", localMethod(generateGroupKeysView, REF_NEWDATA, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue()))
                .declareVar(EPTypePremade.OBJECTARRAYARRAY.getEPType(), "oldDataMultiKey", localMethod(generateGroupKeysView, REF_OLDDATA, ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse()))
                .declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"), ref("eventsPerStream"))
                .declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    private static void addEventPerGroupBufCodegen(String memberName, ResultSetProcessorRowPerGroupRollupForge forge, CodegenInstanceAux instance, CodegenMethod method, CodegenClassScope classScope) {
        if (!instance.hasMember(memberName)) {
            CodegenMethod init = method.makeChild(EPTypePremade.LINKEDHASHMAPARRAY.getEPType(), ResultSetProcessorRowPerGroupRollupImpl.class, classScope);
            instance.addMember(memberName, EPTypePremade.LINKEDHASHMAPARRAY.getEPType());
            instance.getServiceCtor().getBlock().assignMember(memberName, localMethod(init));
            int levelCount = forge.getGroupByRollupDesc().getLevels().length;
            init.getBlock().declareVar(EPTypePremade.LINKEDHASHMAPARRAY.getEPType(), memberName, newArrayByLength(EPTypePremade.LINKEDHASHMAP.getEPType(), constant(levelCount)))
                    .forLoopIntSimple("i", constant(levelCount))
                    .assignArrayElement(memberName, ref("i"), newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()))
                    .blockEnd()
                    .methodReturn(ref(memberName));
        }
    }

    static CodegenMethod generateOutputEventsViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                    .declareVar(EPTypePremade.ARRAYLIST.getEPType(), "events", newInstance(EPTypePremade.ARRAYLIST.getEPType(), constant(1)))
                    .declareVar(EPTypePremade.LIST.getEPType(), "currentGenerators", forge.isSorting() ? newInstance(EPTypePremade.ARRAYLIST.getEPType(), constant(1)) : constantNull())
                    .declareVar(AggregationGroupByRollupLevel.EPTYPEARRAY, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            {
                CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.EPTYPE, "level", ref("levels"));
                {
                    CodegenBlock forEvents = forLevels.forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(arrayAtIndex(ref("keysAndEvents"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                    forEvents.declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("entry"), "getKey"))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                            .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.EPTYPE, exprDotMethod(ref("entry"), "getValue")));

                    if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                        CodegenExpression having = arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
                        forEvents.ifCondition(not(exprDotMethod(having, "evaluateHaving", REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                    }

                    forEvents.exprDotMethod(ref("events"), "add", exprDotMethod(arrayAtIndex(MEMBER_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                    if (forge.isSorting()) {
                        forEvents.declareVar(EventBean.EPTYPEARRAY, "currentEventsPerStream", newArrayWithInit(EventBean.EPTYPE, cast(EventBean.EPTYPE, exprDotMethod(ref("entry"), "getValue"))))
                                .exprDotMethod(ref("currentGenerators"), "add", newInstance(GroupByRollupKey.EPTYPE, ref("currentEventsPerStream"), ref("level"), ref("groupKey")));
                    }
                }
            }

            methodNode.getBlock().ifCondition(exprDotMethod(ref("events"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(EventBean.EPTYPEARRAY, "outgoing", staticMethod(CollectionUtil.class, METHOD_TOARRAYEVENTS, ref("events")));
            if (forge.isSorting()) {
                methodNode.getBlock().ifCondition(relational(arrayLength(ref("outgoing")), GT, constant(1)))
                        .blockReturn(exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortRollup", ref("outgoing"), ref("currentGenerators"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT, MEMBER_AGGREGATIONSVC));
            }
            methodNode.getBlock().methodReturn(ref("outgoing"));
        };

        return instance.getMethods().addMethod(EventBean.EPTYPEARRAY, "generateOutputEventsView",
                CodegenNamedParam.from(EPTypePremade.MAPARRAY.getEPType(), "keysAndEvents", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputEventsJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EPTypePremade.ARRAYLIST.getEPType(), "events", newInstance(EPTypePremade.ARRAYLIST.getEPType(), constant(1)))
                    .declareVar(EPTypePremade.LIST.getEPType(), "currentGenerators", forge.isSorting() ? newInstance(EPTypePremade.ARRAYLIST.getEPType(), constant(1)) : constantNull())
                    .declareVar(AggregationGroupByRollupLevel.EPTYPEARRAY, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            {
                CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.EPTYPE, "level", ref("levels"));
                {
                    CodegenBlock forEvents = forLevels.forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(arrayAtIndex(ref("eventPairs"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                    forEvents.declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("entry"), "getKey"))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")));

                    if (forge.getPerLevelForges().getOptionalHavingForges() != null) {
                        CodegenExpression having = arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber"));
                        forEvents.ifCondition(not(exprDotMethod(having, "evaluateHaving", REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                    }

                    forEvents.exprDotMethod(ref("events"), "add", exprDotMethod(arrayAtIndex(MEMBER_SELECTEXPRPROCESSOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                    if (forge.isSorting()) {
                        forEvents.exprDotMethod(ref("currentGenerators"), "add", newInstance(GroupByRollupKey.EPTYPE, ref("eventsPerStream"), ref("level"), ref("groupKey")));
                    }
                }
            }

            methodNode.getBlock().ifCondition(exprDotMethod(ref("events"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(EventBean.EPTYPEARRAY, "outgoing", staticMethod(CollectionUtil.class, METHOD_TOARRAYEVENTS, ref("events")));
            if (forge.isSorting()) {
                methodNode.getBlock().ifCondition(relational(arrayLength(ref("outgoing")), GT, constant(1)))
                        .blockReturn(exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortRollup", ref("outgoing"), ref("currentGenerators"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT, MEMBER_AGGREGATIONSVC));
            }
            methodNode.getBlock().methodReturn(ref("outgoing"));
        };

        return instance.getMethods().addMethod(EventBean.EPTYPEARRAY, "generateOutputEventsJoin",
                CodegenNamedParam.from(EPTypePremade.MAPARRAY.getEPType(), "eventPairs", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    static void getIteratorViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE));
            return;
        }

        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
                .declareVar(EPTypePremade.ITERATOR.getEPType(), "it", exprDotMethod(REF_VIEWABLE, "iterator"))
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeys", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)))
                .declareVar(AggregationGroupByRollupLevel.EPTYPEARRAY, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("it"), "hasNext"))
                    .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.EPTYPE, exprDotMethod(ref("it"), "next")))
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                    .forLoopIntSimple("j", arrayLength(ref("levels")))
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                    .assignArrayElement("groupKeys", ref("j"), ref("subkey"))
                    .blockEnd()
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeys"), MEMBER_AGENTINSTANCECONTEXT)
                    .blockEnd();
        }

        method.getBlock().declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, classScope, method, instance), REF_VIEWABLE)))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod parent, CodegenInstanceAux instance) {
        CodegenMethod resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);
        CodegenMethod generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        CodegenMethod iterator = parent.makeChild(EPTypePremade.ITERATOR.getEPType(), ResultSetProcessorRowPerGroupRollupImpl.class, classScope).addParam(Viewable.EPTYPE, NAME_VIEWABLE);
        iterator.getBlock().localMethod(resetEventPerGroupBufView)
                .declareVar(EventBean.EPTYPEARRAY, "events", staticMethod(CollectionUtil.class, METHOD_ITERATORTOARRAYEVENTS, exprDotMethod(REF_VIEWABLE, "iterator")))
                .localMethod(generateGroupKeysView, ref("events"), ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue())
                .declareVar(EventBean.EPTYPEARRAY, "output", localMethod(generateOutputEventsView, ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.EPTYPE, ref("output")));
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
                .declareVar(EventBean.EPTYPEARRAY, "output", localMethod(generateOutputEventsJoin, ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.EPTYPE, ref("output")));
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
            method.getBlock().forEach(ResultSetProcessorGroupedOutputFirstHelper.EPTYPE, "helper", member(NAME_OUTPUTFIRSTHELPERS))
                    .exprDotMethod(REF_RESULTSETVISITOR, "visit", ref("helper"));
        }
    }

    private static void handleOutputLimitFirstViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        initGroupRepsPerLevelBufCodegen(instance, forge);
        CodegenMethod generateAndSort = generateAndSortCodegen(forge, classScope, instance);

        method.getBlock().forEach(EPTypePremade.MAP.getEPType(), "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");

        method.getBlock().declareVarNoInit(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count");
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

        method.getBlock().forEach(EPTypePremade.MAP.getEPType(), "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");

        method.getBlock().declareVarNoInit(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count");
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
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.EPTYPE, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        initOutputFirstHelpers(outputFactory, instance, forge, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeysPerLevel", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)))
                        .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

                {
                    CodegenBlock ifNewApplyAgg = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNew = ifNewApplyAgg.forEach(EventBean.EPTYPE, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("aNewData")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            forNew.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                        {
                            forOld.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifNewFirst = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNewFirst = ifNewFirst.forEach(EventBean.EPTYPE, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("aNewData")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forNewFirst.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
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
                        CodegenBlock forOldFirst = ifOldFirst.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forOldFirst.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
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

        return instance.getMethods().addMethod(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "handleOutputLimitFirstViewHaving",
                CodegenNamedParam.from(EPTypePremade.LIST.getEPType(), "viewEventsList", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LISTARRAY.getEPType(), "oldEventsPerLevel", EPTypePremade.LISTARRAY.getEPType(), "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private static CodegenMethod handleOutputLimitFirstJoinNoHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.EPTYPE, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        initOutputFirstHelpers(outputFactory, instance, forge, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0))
                    .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeysPerLevel", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)));

                {
                    CodegenBlock ifNewApplyAgg = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNew = ifNewApplyAgg.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock forLvl = forNew.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
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
                        CodegenBlock forOld = ifOldApplyAgg.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                        {
                            CodegenBlock forLvl = forOld.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
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

        return instance.getMethods().addMethod(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "handleOutputLimitFirstJoinNoHaving",
                CodegenNamedParam.from(EPTypePremade.LIST.getEPType(), NAME_JOINEVENTSSET, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LISTARRAY.getEPType(), "oldEventsPerLevel", EPTypePremade.LISTARRAY.getEPType(), "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private static CodegenMethod handleOutputLimitFirstJoinHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.EPTYPE, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        initOutputFirstHelpers(outputFactory, instance, forge, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0))
                    .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeysPerLevel", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)));
                {
                    CodegenBlock ifNewApplyAgg = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNew = ifNewApplyAgg.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            forNew.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forNew.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifOldApplyAgg = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forOld = ifOldApplyAgg.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                        {
                            forOld.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"));
                        }
                        forOld.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("groupKeysPerLevel"), MEMBER_AGENTINSTANCECONTEXT);
                    }

                    CodegenBlock ifNewFirst = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNewFirst = ifNewFirst.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forNewFirst.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
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
                        CodegenBlock forOldFirst = ifOldFirst.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock eachlvl = forOldFirst.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), ref("level"))
                                    .ifCondition(not(exprDotMethod(arrayAtIndex(MEMBER_HAVINGEVALUATOR_ARRAY, exprDotMethod(ref("level"), "getLevelNumber")), "evaluateHaving", ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue()
                                    .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
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

        return instance.getMethods().addMethod(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "handleOutputLimitFirstJoinHaving",
                CodegenNamedParam.from(EPTypePremade.LIST.getEPType(), NAME_JOINEVENTSSET, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LISTARRAY.getEPType(), "oldEventsPerLevel", EPTypePremade.LISTARRAY.getEPType(), "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private static CodegenMethod handleOutputLimitFirstViewNoHavingCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenExpression levelNumber = exprDotMethod(ref("level"), "getLevelNumber");
        CodegenMethod generateOutputBatchedGivenArray = generateOutputBatchedGivenArrayCodegen(forge, classScope, instance);
        initGroupRepsPerLevelBufCodegen(instance, forge);
        initRStreamEventsSortArrayBufCodegen(instance, forge);

        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.EPTYPE, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        initOutputFirstHelpers(outputFactory, instance, forge, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeysPerLevel", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)))
                        .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

                {
                    CodegenBlock ifNewApplyAgg = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forNew = ifNewApplyAgg.forEach(EventBean.EPTYPE, "aNewData", ref("newData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("aNewData")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                        {
                            CodegenBlock forLvl = forNew.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
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
                        CodegenBlock forOld = ifOldApplyAgg.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"))
                                .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                        {
                            CodegenBlock forLvl = forOld.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
                                    .assignArrayElement(ref("groupKeysPerLevel"), levelNumber, ref("groupKey"))
                                    .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(arrayAtIndex(member(NAME_OUTPUTFIRSTHELPERS), levelNumber), "getOrAllocate", ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                    .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
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

        return instance.getMethods().addMethod(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "handleOutputLimitFirstNoViewHaving",
                CodegenNamedParam.from(EPTypePremade.LIST.getEPType(), "viewEventsList", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LISTARRAY.getEPType(), "oldEventsPerLevel", EPTypePremade.LISTARRAY.getEPType(), "oldEventsSortKeyPerLevel"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    private static void handleOutputLimitDefaultViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedCollectView = generateOutputBatchedCollectViewCodegen(forge, classScope, instance);
        CodegenMethod generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethod resetEventPerGroupBufView = resetEventPerGroupBufCodegen(NAME_EVENTPERGROUPBUFVIEW, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));
        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .localMethod(resetEventPerGroupBufView)
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(generateGroupKeysView, ref("newData"), ref(NAME_EVENTPERGROUPBUFVIEW), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(generateGroupKeysView, ref("oldData"), ref(NAME_EVENTPERGROUPBUFVIEW), constantFalse()));

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
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                    .localMethod(resetEventPerGroupBufJoin)
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(generateGroupKeysJoin, ref("newData"), ref(NAME_EVENTPERGROUPBUFJOIN), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(generateGroupKeysJoin, ref("oldData"), ref(NAME_EVENTPERGROUPBUFJOIN), constantFalse()));

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
        instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "removedAggregationGroupKey", CodegenNamedParam.from(EPTypePremade.OBJECT.getEPType(), "key"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputBatchedGivenArrayCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);
        Consumer<CodegenMethod> code = methodNode -> methodNode.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "resultList", arrayAtIndex(ref("resultEvents"), exprDotMethod(ref("level"), "getLevelNumber")))
                .declareVarNoInit(EPTypePremade.LIST.getEPType(), "sortKeys")
                .ifCondition(equalsNull(ref("optSortKeys")))
                .assignRef("sortKeys", constantNull())
                .ifElse()
                .assignRef("sortKeys", arrayAtIndex(ref("optSortKeys"), exprDotMethod(ref("level"), "getLevelNumber")))
                .blockEnd()
                .localMethod(generateOutputBatched, ref("mk"), ref("level"), ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("resultList"), ref("sortKeys"));
        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedGivenArrayCodegen",
                CodegenNamedParam.from(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "join", EPTypePremade.OBJECT.getEPType(), "mk", AggregationGroupByRollupLevel.EPTYPE, "level", EventBean.EPTYPEARRAY, "eventsPerStream", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LISTARRAY.getEPType(), "resultEvents", EPTypePremade.LISTARRAY.getEPType(), "optSortKeys"),
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
        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatched",
                CodegenNamedParam.from(EPTypePremade.OBJECT.getEPType(), "mk", AggregationGroupByRollupLevel.EPTYPE, "level", EventBean.EPTYPEARRAY, "eventsPerStream", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LIST.getEPType(), "resultEvents", EPTypePremade.LIST.getEPType(), "optSortKeys"),
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

        instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedMapUnsorted",
                CodegenNamedParam.from(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "join", EPTypePremade.OBJECT.getEPType(), "mk", AggregationGroupByRollupLevel.EPTYPE, "level", EventBean.EPTYPEARRAY, "eventsPerStream", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.MAP.getEPType(), "resultEvents"),
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

        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
        }

        method.getBlock().forEach(EPTypePremade.MAP.getEPType(), "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeysPerLevel", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)))
                    .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

            {
                CodegenBlock ifNew = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNew.forEach(EventBean.EPTYPE, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("aNewData")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
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
                    CodegenBlock forOld = ifOld.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
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

        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
        }

        method.getBlock().forEach(EPTypePremade.MAP.getEPType(), "aGroupRepsView", ref(NAME_GROUPREPSPERLEVELBUF))
                .exprDotMethod(ref("aGroupRepsView"), "clear");

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeysPerLevel", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)))
                    .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

            {
                CodegenBlock ifNew = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNew.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
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
                    CodegenBlock forOld = ifOld.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
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

        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
            method.getBlock().forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(EPTypePremade.MAP.getEPType(), "groupGenerators", arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), exprDotMethod(ref("level"), "getLevelNumber")))
                    .forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(ref("groupGenerators"), "entrySet"))
                    .localMethod(generateOutputBatchedGivenArray, constantFalse(), exprDotMethod(ref("entry"), "getKey"), ref("level"), cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                    .incrementRef("count");
        }

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeysPerLevel", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)))
                    .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

            {
                CodegenBlock ifNew = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNew.forEach(EventBean.EPTYPE, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("aNewData")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
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
                    CodegenBlock forOld = ifOld.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", newArrayWithInit(EventBean.EPTYPE, ref("anOldData")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
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

        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
        if (forge.isSelectRStream()) {
            method.getBlock().exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "reset");
            method.getBlock().forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(EPTypePremade.MAP.getEPType(), "groupGenerators", arrayAtIndex(ref(NAME_GROUPREPSPERLEVELBUF), exprDotMethod(ref("level"), "getLevelNumber")))
                    .forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(ref("groupGenerators"), "entrySet"))
                    .localMethod(generateOutputBatchedGivenArray, constantFalse(), exprDotMethod(ref("entry"), "getKey"), ref("level"), cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")), constantFalse(), REF_ISSYNTHESIZE, exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getEventsPerLevel"), exprDotMethod(ref(NAME_RSTREAMEVENTSORTARRAYBUF), "getSortKeyPerLevel"))
                    .incrementRef("count");
        }

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupKeysPerLevel", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getGroupByRollupDesc().getLevels().length)))
                    .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

            {
                CodegenBlock ifNew = forEach.ifCondition(notEqualsNull(ref("newData")));
                {
                    CodegenBlock forNew = ifNew.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"))
                            .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()));
                    {
                        CodegenBlock forLevel = forNew.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
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
                    CodegenBlock forOld = ifOld.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"))
                            .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()));
                    {
                        CodegenBlock forLevel = forOld.forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", exprDotMethod(ref("level"), "computeSubkey", ref("groupKeyComplete")))
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
            methodNode.getBlock().declareVar(AggregationGroupByRollupLevel.EPTYPEARRAY, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.EPTYPE, "level", ref("levels"));
            {
                CodegenBlock forEvents = forLevels.forEach(EPTypePremade.MAPENTRY.getEPType(), "pair", exprDotMethod(arrayAtIndex(ref("eventPairs"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                forEvents.assignArrayElement("eventsPerStream", constant(0), cast(EventBean.EPTYPE, exprDotMethod(ref("pair"), "getValue")))
                        .localMethod(generateOutputBatched, exprDotMethod(ref("pair"), "getKey"), ref("level"), ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("events"), ref("sortKey"));
            }
        };

        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedCollectView",
                CodegenNamedParam.from(EPTypePremade.MAPARRAY.getEPType(), "eventPairs", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LIST.getEPType(), "events", EPTypePremade.LIST.getEPType(), "sortKey", EventBean.EPTYPEARRAY, "eventsPerStream"),
                ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputBatchedCollectJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {

        CodegenMethod generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(AggregationGroupByRollupLevel.EPTYPEARRAY, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));

            CodegenBlock forLevels = methodNode.getBlock().forEach(AggregationGroupByRollupLevel.EPTYPE, "level", ref("levels"));
            {
                CodegenBlock forEvents = forLevels.forEach(EPTypePremade.MAPENTRY.getEPType(), "pair", exprDotMethod(arrayAtIndex(ref("eventPairs"), exprDotMethod(ref("level"), "getLevelNumber")), "entrySet"));
                forEvents.localMethod(generateOutputBatched, exprDotMethod(ref("pair"), "getKey"), ref("level"), cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getValue")), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("events"), ref("sortKey"));
            }
        };

        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedCollectJoin",
                CodegenNamedParam.from(EPTypePremade.MAPARRAY.getEPType(), "eventPairs", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LIST.getEPType(), "events", EPTypePremade.LIST.getEPType(), "sortKey"),
                ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private static CodegenMethod resetEventPerGroupBufCodegen(String memberName, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> methodNode.getBlock().forEach(EPTypePremade.LINKEDHASHMAP.getEPType(), "anEventPerGroupBuf", ref(memberName))
                .exprDotMethod(ref("anEventPerGroupBuf"), "clear");

        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "resetEventPerGroupBuf", Collections.emptyList(), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    static CodegenMethod generateGroupKeysViewCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifRefNullReturnNull("events")
                    .declareVar(EPTypePremade.OBJECTARRAYARRAY.getEPType(), "result", newArrayByLength(EPTypePremade.OBJECTARRAY.getEPType(), arrayLength(ref("events"))))
                    .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                    .declareVar(AggregationGroupByRollupLevel.EPTYPEARRAY, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"));
            {
                CodegenBlock forLoop = methodNode.getBlock().forLoopIntSimple("i", arrayLength(ref("events")));
                forLoop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("events"), ref("i")))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), REF_ISNEWDATA))
                        .assignArrayElement("result", ref("i"), newArrayByLength(EPTypePremade.OBJECT.getEPType(), arrayLength(ref("levels"))));
                {
                    forLoop.forLoopIntSimple("j", arrayLength(ref("levels")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                            .assignArrayElement2Dim("result", ref("i"), ref("j"), ref("subkey"))
                            .exprDotMethod(arrayAtIndex(ref("eventPerKey"), exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "getLevelNumber")), "put", ref("subkey"), arrayAtIndex(ref("events"), ref("i")));
                }
            }

            methodNode.getBlock().methodReturn(ref("result"));
        };

        return instance.getMethods().addMethod(EPTypePremade.OBJECTARRAYARRAY.getEPType(), "generateGroupKeysView",
                CodegenNamedParam.from(EventBean.EPTYPEARRAY, "events", EPTypePremade.MAPARRAY.getEPType(), "eventPerKey", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private static CodegenMethod generateGroupKeysJoinCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(or(equalsNull(ref("events")), exprDotMethod(ref("events"), "isEmpty"))).blockReturn(constantNull())
                    .declareVar(EPTypePremade.OBJECTARRAYARRAY.getEPType(), "result", newArrayByLength(EPTypePremade.OBJECTARRAY.getEPType(), exprDotMethod(ref("events"), "size")))
                    .declareVar(AggregationGroupByRollupLevel.EPTYPEARRAY, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(-1));
            {
                CodegenBlock forLoop = methodNode.getBlock().forEach(MultiKeyArrayOfKeys.EPTYPE, "eventrow", ref("events"));
                forLoop.incrementRef("count")
                        .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("eventrow"), "getArray")))
                        .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), REF_ISNEWDATA))
                        .assignArrayElement("result", ref("count"), newArrayByLength(EPTypePremade.OBJECT.getEPType(), arrayLength(ref("levels"))));
                {
                    forLoop.forLoopIntSimple("j", arrayLength(ref("levels")))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                            .assignArrayElement2Dim("result", ref("count"), ref("j"), ref("subkey"))
                            .exprDotMethod(arrayAtIndex(ref("eventPerKey"), exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "getLevelNumber")), "put", ref("subkey"), ref("eventsPerStream"));
                }
            }

            methodNode.getBlock().methodReturn(ref("result"));
        };

        return instance.getMethods().addMethod(EPTypePremade.OBJECTARRAYARRAY.getEPType(), "generateGroupKeysJoin",
                CodegenNamedParam.from(EPTypePremade.SET.getEPType(), "events", EPTypePremade.MAPARRAY.getEPType(), "eventPerKey", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA), ResultSetProcessorRowPerGroupRollupImpl.class, classScope, code);
    }

    private static CodegenMethod generateAndSortCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatched = generateOutputBatchedCodegen(forge, instance, classScope);

        Consumer<CodegenMethod> code = methodNode -> {

            methodNode.getBlock().declareVar(EventBean.EPTYPEARRAY, "oldEventsArr", constantNull())
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldEventSortKeys", constantNull());

            if (forge.isSelectRStream()) {
                methodNode.getBlock().ifCondition(relational(ref("oldEventCount"), GT, constant(0)))
                        .declareVar(EventsAndSortKeysPair.EPTYPE, "pair", staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, METHOD_GETOLDEVENTSSORTKEYS, ref("oldEventCount"), ref(NAME_RSTREAMEVENTSORTARRAYBUF), MEMBER_ORDERBYPROCESSOR, exprDotMethod(ref("this"), "getGroupByRollupDesc")))
                        .assignRef("oldEventsArr", exprDotMethod(ref("pair"), "getEvents"))
                        .assignRef("oldEventSortKeys", exprDotMethod(ref("pair"), "getSortKeys"));
            }

            methodNode.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "newEvents", newInstance(EPTypePremade.ARRAYLIST.getEPType()))
                    .declareVar(EPTypePremade.LIST.getEPType(), "newEventsSortKey", forge.isSorting() ? newInstance(EPTypePremade.ARRAYLIST.getEPType()) : constantNull());

            methodNode.getBlock().forEach(AggregationGroupByRollupLevel.EPTYPE, "level", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                    .declareVar(EPTypePremade.MAP.getEPType(), "groupGenerators", arrayAtIndex(ref("outputLimitGroupRepsPerLevel"), exprDotMethod(ref("level"), "getLevelNumber")))
                    .forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(ref("groupGenerators"), "entrySet"))
                    .localMethod(generateOutputBatched, exprDotMethod(ref("entry"), "getKey"), ref("level"), cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

            methodNode.getBlock().declareVar(EventBean.EPTYPEARRAY, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYEVENTS, ref("newEvents")));
            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYOBJECTS, ref("newEventsSortKey")))
                        .assignRef("newEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), MEMBER_AGENTINSTANCECONTEXT));
                if (forge.isSelectRStream()) {
                    methodNode.getBlock().assignRef("oldEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("oldEventSortKeys"), MEMBER_AGENTINSTANCECONTEXT));
                }
            }

            methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
        };

        return instance.getMethods().addMethod(UniformPair.EPTYPE, "generateAndSort",
                CodegenNamedParam.from(EPTypePremade.MAPARRAY.getEPType(), "outputLimitGroupRepsPerLevel", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.INTEGERPRIMITIVE.getEPType(), "oldEventCount"),
                ResultSetProcessorUtil.class, classScope, code);
    }

    static void applyViewResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeysRow = generateGroupKeysRowCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));
        {
            CodegenBlock ifNew = method.getBlock().ifCondition(notEqualsNull(REF_NEWDATA));
            {
                ifNew.forEach(EventBean.EPTYPE, "aNewData", REF_NEWDATA)
                        .assignArrayElement("eventsPerStream", constant(0), ref("aNewData"))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantTrue()))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("keys"), MEMBER_AGENTINSTANCECONTEXT);
            }
        }
        {
            CodegenBlock ifOld = method.getBlock().ifCondition(notEqualsNull(REF_OLDDATA));
            {
                ifOld.forEach(EventBean.EPTYPE, "anOldData", REF_OLDDATA)
                        .assignArrayElement("eventsPerStream", constant(0), ref("anOldData"))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantFalse()))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("keys"), MEMBER_AGENTINSTANCECONTEXT);
            }
        }
    }

    public static void applyJoinResultCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeysRow = generateGroupKeysRowCodegen(forge, classScope, instance);

        method.getBlock().declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");
        {
            CodegenBlock ifNew = method.getBlock().ifCondition(notEqualsNull(REF_NEWDATA));
            {
                ifNew.forEach(MultiKeyArrayOfKeys.EPTYPE, "mk", REF_NEWDATA)
                        .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("mk"), "getArray")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantTrue()))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("keys"), MEMBER_AGENTINSTANCECONTEXT);
            }
        }
        {
            CodegenBlock ifOld = method.getBlock().ifCondition(notEqualsNull(REF_OLDDATA));
            {
                ifOld.forEach(MultiKeyArrayOfKeys.EPTYPE, "mk", REF_OLDDATA)
                        .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("mk"), "getArray")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", localMethod(generateGroupKeysRow, ref("eventsPerStream"), constantFalse()))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("keys"), MEMBER_AGENTINSTANCECONTEXT);
            }
        }
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorRowPerGroupRollupForge forge, String methodName, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType.EPTYPEARRAY, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));

        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorRowPerGroupRollupOutputAllHelper.EPTYPE);
            StateMgmtSetting stateMgmtSettings = forge.getOutputAllSettings().get();
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(factory, "makeRSRowPerGroupRollupAll", MEMBER_AGENTINSTANCECONTEXT, ref("this"), constant(forge.getGroupKeyTypes()), eventTypes, stateMgmtSettings.toExpression()));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorRowPerGroupRollupOutputLastHelper.EPTYPE);
            StateMgmtSetting stateMgmtSettings = forge.getOutputLastSettings().get();
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(factory, "makeRSRowPerGroupRollupLast", MEMBER_AGENTINSTANCECONTEXT, ref("this"), constant(forge.getGroupKeyTypes()), eventTypes, stateMgmtSettings.toExpression()));
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
            method.getBlock().forEach(ResultSetProcessorGroupedOutputFirstHelper.EPTYPE, "helper", member(NAME_OUTPUTFIRSTHELPERS))
                    .exprDotMethod(ref("helper"), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), "destroy");
        }
    }

    private static CodegenMethod generateGroupKeysRowCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> methodNode.getBlock().declareVar(EPTypePremade.OBJECT.getEPType(), "groupKeyComplete", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), REF_ISNEWDATA))
                .declareVar(AggregationGroupByRollupLevel.EPTYPEARRAY, "levels", exprDotMethodChain(ref("this")).add("getGroupByRollupDesc").add("getLevels"))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "result", newArrayByLength(EPTypePremade.OBJECT.getEPType(), arrayLength(ref("levels"))))
                .forLoopIntSimple("j", arrayLength(ref("levels")))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "subkey", exprDotMethod(arrayAtIndex(ref("levels"), ref("j")), "computeSubkey", ref("groupKeyComplete")))
                .assignArrayElement("result", ref("j"), ref("subkey"))
                .blockEnd()
                .methodReturn(ref("result"));

        return instance.getMethods().addMethod(EPTypePremade.OBJECTARRAY.getEPType(), "generateGroupKeysRow", CodegenNamedParam.from(EventBean.EPTYPEARRAY, "eventsPerStream", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA), ResultSetProcessorUtil.class, classScope, code);
    }

    private static void initGroupRepsPerLevelBufCodegen(CodegenInstanceAux instance, ResultSetProcessorRowPerGroupRollupForge forge) {
        if (!instance.hasMember(NAME_GROUPREPSPERLEVELBUF)) {
            instance.addMember(NAME_GROUPREPSPERLEVELBUF, EPTypePremade.MAPARRAY.getEPType());
            instance.getServiceCtor().getBlock().assignRef(NAME_GROUPREPSPERLEVELBUF, staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, METHOD_MAKEGROUPREPSPERLEVELBUF, constant(forge.getGroupByRollupDesc().getLevels().length)));
        }
    }

    private static void initRStreamEventsSortArrayBufCodegen(CodegenInstanceAux instance, ResultSetProcessorRowPerGroupRollupForge forge) {
        if (!instance.hasMember(NAME_RSTREAMEVENTSORTARRAYBUF)) {
            instance.addMember(NAME_RSTREAMEVENTSORTARRAYBUF, EventArrayAndSortKeyArray.EPTYPE);
            instance.getServiceCtor().getBlock().assignRef(NAME_RSTREAMEVENTSORTARRAYBUF, staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, METHOD_MAKERSTREAMSORTEDARRAYBUF, constant(forge.getGroupByRollupDesc().getLevels().length), constant(forge.isSorting())));
        }
    }

    private static void initOutputFirstHelpers(CodegenExpressionField outputConditionFactory, CodegenInstanceAux instance, ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope) {
        if (!instance.hasMember(NAME_OUTPUTFIRSTHELPERS)) {
            CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
            instance.addMember(NAME_OUTPUTFIRSTHELPERS, ResultSetProcessorGroupedOutputFirstHelper.EPTYPEARRAY);
            StateMgmtSetting stateMgmtSettings = forge.getOutputFirstSettings().get();
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPERS, staticMethod(ResultSetProcessorRowPerGroupRollupUtil.class, "initializeOutputFirstHelpers", factory,
                    MEMBER_AGENTINSTANCECONTEXT, constant(forge.getGroupKeyTypes()), exprDotMethod(ref("this"), "getGroupByRollupDesc"), outputConditionFactory, stateMgmtSettings.toExpression()));
        }
    }
}
