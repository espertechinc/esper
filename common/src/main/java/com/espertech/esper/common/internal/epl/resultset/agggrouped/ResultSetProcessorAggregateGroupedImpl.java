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
package com.espertech.esper.common.internal.epl.resultset.agggrouped;

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
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolled;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactoryField;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputAllGroupReps;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroupImpl;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.NAME_EPS;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil.*;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.METHOD_APPLYAGGJOINRESULTKEYEDJOIN;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.METHOD_APPLYAGGVIEWRESULTKEYEDVIEW;
import static com.espertech.esper.common.internal.util.CollectionUtil.*;

/**
 * Result-set processor for the aggregate-grouped case:
 * there is a group-by and one or more non-aggregation event properties in the select clause are not listed in the group by,
 * and there are aggregation functions.
 * <p>
 * This processor does perform grouping by computing MultiKey group-by keys for each row.
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 * <p>
 * Aggregation state is a table of rows held by aggegation service where the row key is the group-by MultiKey.
 */
public class ResultSetProcessorAggregateGroupedImpl {

    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";
    private final static String NAME_OUTPUTFIRSTHELPER = "outputFirstHelper";
    private final static String NAME_OUTPUTALLGROUPREPS = "outputAllGroupReps";

    public static void applyViewResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .ifCondition(notEqualsNull(REF_NEWDATA))
                .forEach(EventBean.EPTYPE, "aNewData", REF_NEWDATA)
                .assignArrayElement("eventsPerStream", constant(0), ref("aNewData"))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                .blockEnd()
                .blockEnd()
                .ifCondition(notEqualsNull(REF_OLDDATA))
                .forEach(EventBean.EPTYPE, "anOldData", REF_OLDDATA)
                .assignArrayElement("eventsPerStream", constant(0), ref("anOldData"))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                .blockEnd()
                .blockEnd();
    }

    public static void applyJoinResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock()
                .ifCondition(not(exprDotMethod(REF_NEWDATA, "isEmpty")))
                .forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewEvent", REF_NEWDATA)
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewEvent"), "getArray")))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                .blockEnd()
                .blockEnd()
                .ifCondition(and(notEqualsNull(REF_OLDDATA), not(exprDotMethod(REF_OLDDATA, "isEmpty"))))
                .forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldEvent", REF_OLDDATA)
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldEvent"), "getArray")))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantFalse()))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                .blockEnd()
                .blockEnd();
    }

    public static void processJoinResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataGroupByKeys", localMethod(forge.getGenerateGroupKeyArrayJoin(), REF_NEWDATA, constantTrue()))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataGroupByKeys", localMethod(forge.getGenerateGroupKeyArrayJoin(), REF_OLDDATA, constantFalse()));

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }

        method.getBlock().staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataGroupByKeys"), REF_OLDDATA, ref("oldDataGroupByKeys"));

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, REF_OLDDATA, ref("oldDataGroupByKeys"), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(generateOutputEventsJoin, REF_NEWDATA, ref("newDataGroupByKeys"), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    public static void processViewResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        CodegenMethod processViewResultNewDepthOne = processViewResultNewDepthOneCodegen(forge, classScope, instance);
        CodegenMethod processViewResultPairDepthOneNoRStream = processViewResultPairDepthOneCodegen(forge, classScope, instance);

        CodegenBlock ifShortcut = method.getBlock().ifCondition(and(notEqualsNull(REF_NEWDATA), equalsIdentity(arrayLength(REF_NEWDATA), constant(1))));
        ifShortcut.ifCondition(or(equalsNull(REF_OLDDATA), equalsIdentity(arrayLength(REF_OLDDATA), constant(0))))
                .blockReturn(localMethod(processViewResultNewDepthOne, REF_NEWDATA, REF_ISSYNTHESIZE))
                .ifCondition(equalsIdentity(arrayLength(REF_OLDDATA), constant(1)))
                .blockReturn(localMethod(processViewResultPairDepthOneNoRStream, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE));

        method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataGroupByKeys", localMethod(forge.getGenerateGroupKeyArrayView(), REF_NEWDATA, constantTrue()))
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataGroupByKeys", localMethod(forge.getGenerateGroupKeyArrayView(), REF_OLDDATA, constantFalse()))
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataGroupByKeys"), REF_OLDDATA, ref("oldDataGroupByKeys"), ref("eventsPerStream"));

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, REF_OLDDATA, ref("oldDataGroupByKeys"), constantFalse(), REF_ISSYNTHESIZE, ref("eventsPerStream")) : constantNull())
                .declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(generateOutputEventsView, REF_NEWDATA, ref("newDataGroupByKeys"), constantTrue(), REF_ISSYNTHESIZE, ref("eventsPerStream")))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    private static CodegenMethod generateOutputEventsViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifNullReturnNull(ref("outputEvents"))
                    .declareVar(EventBean.EPTYPEARRAY, "events", newArrayByLength(EventBean.EPTYPE, arrayLength(ref("outputEvents"))))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", newArrayByLength(EPTypePremade.OBJECT.getEPType(), arrayLength(ref("outputEvents"))));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean.EPTYPEARRAYARRAY, "currentGenerators", newArrayByLength(EventBean.EPTYPEARRAY, arrayLength(ref("outputEvents"))));
            }

            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "countOutputRows", constant(0))
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "cpid", exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forLoop = methodNode.getBlock().forLoopIntSimple("countInputRows", arrayLength(ref("outputEvents")));
                forLoop.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("countInputRows")), ref("cpid"), constantNull())
                        .assignArrayElement(REF_EPS, constant(0), arrayAtIndex(ref("outputEvents"), ref("countInputRows")));

                if (forge.getOptionalHavingNode() != null) {
                    forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forLoop.assignArrayElement("events", ref("countOutputRows"), exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT))
                        .assignArrayElement("keys", ref("countOutputRows"), arrayAtIndex(ref("groupByKeys"), ref("countInputRows")));

                if (forge.isSorting()) {
                    forLoop.assignArrayElement("currentGenerators", ref("countOutputRows"), newArrayWithInit(EventBean.EPTYPE, arrayAtIndex(ref("outputEvents"), ref("countInputRows"))));
                }

                forLoop.incrementRef("countOutputRows")
                        .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("countOutputRows"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean.EPTYPEARRAY, "generateOutputEventsView",
                CodegenNamedParam.from(EventBean.EPTYPEARRAY, "outputEvents", EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EventBean.EPTYPEARRAY, NAME_EPS),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    public static void acceptHelperVisitorCodegen(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTALLGROUPREPS));
        }
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTLASTHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTALLHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTFIRSTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTFIRSTHELPER));
        }
    }

    private static CodegenMethod generateOutputEventsJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(exprDotMethod(ref("resultSet"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(EventBean.EPTYPEARRAY, "events", newArrayByLength(EventBean.EPTYPE, exprDotMethod(ref("resultSet"), "size")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", newArrayByLength(EPTypePremade.OBJECT.getEPType(), exprDotMethod(ref("resultSet"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean.EPTYPEARRAYARRAY, "currentGenerators", newArrayByLength(EventBean.EPTYPEARRAY, exprDotMethod(ref("resultSet"), "size")));
            }

            methodNode.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "countOutputRows", constant(0))
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "countInputRows", constant(-1))
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "cpid", exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forLoop = methodNode.getBlock().forEach(MultiKeyArrayOfKeys.EPTYPE, "row", ref("resultSet"));
                forLoop.incrementRef("countInputRows")
                        .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("row"), "getArray")))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("countInputRows")), ref("cpid"), constantNull());

                if (forge.getOptionalHavingNode() != null) {
                    forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forLoop.assignArrayElement("events", ref("countOutputRows"), exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT))
                        .assignArrayElement("keys", ref("countOutputRows"), arrayAtIndex(ref("groupByKeys"), ref("countInputRows")));

                if (forge.isSorting()) {
                    forLoop.assignArrayElement("currentGenerators", ref("countOutputRows"), ref("eventsPerStream"));
                }

                forLoop.incrementRef("countOutputRows")
                        .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("countOutputRows"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean.EPTYPEARRAY, "generateOutputEventsJoin",
                CodegenNamedParam.from(EPTypePremade.SET.getEPType(), "resultSet", EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    public static void getIteratorViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, method, classScope, instance), REF_VIEWABLE));
            return;
        }

        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
                .declareVar(EPTypePremade.ITERATOR.getEPType(), "it", exprDotMethod(REF_VIEWABLE, "iterator"))
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));

        {
            method.getBlock().whileLoop(exprDotMethod(ref("it"), "hasNext"))
                    .assignArrayElement(ref("eventsPerStream"), constant(0), cast(EventBean.EPTYPE, exprDotMethod(ref("it"), "next")))
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT)
                    .blockEnd();
        }

        method.getBlock().declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, method, classScope, instance), REF_VIEWABLE)))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenMethod parent, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod iterator = parent.makeChild(EPTypePremade.ITERATOR.getEPType(), ResultSetProcessorAggregateGroupedImpl.class, classScope).addParam(Viewable.EPTYPE, NAME_VIEWABLE);
        if (!forge.isSorting()) {
            iterator.getBlock().methodReturn(newInstance(ResultSetProcessorAggregateGroupedIterator.EPTYPE, exprDotMethod(REF_VIEWABLE, "iterator"), ref("this"), MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT));
            return iterator;
        }

        // Pull all parent events, generate order keys
        iterator.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .declareVar(EPTypePremade.LIST.getEPType(), "outgoingEvents", newInstance(EPTypePremade.ARRAYLIST.getEPType()))
                .declareVar(EPTypePremade.LIST.getEPType(), "orderKeys", newInstance(EPTypePremade.ARRAYLIST.getEPType()));

        {
            CodegenBlock forLoop = iterator.getBlock().forEach(EventBean.EPTYPE, "candidate", REF_VIEWABLE);
            forLoop.assignArrayElement(ref("eventsPerStream"), constant(0), ref("candidate"))
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                forLoop.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
            }

            forLoop.exprDotMethod(ref("outgoingEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))
                    .exprDotMethod(ref("orderKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
        }

        iterator.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_ORDEROUTGOINGGETITERATOR, ref("outgoingEvents"), ref("orderKeys"), MEMBER_ORDERBYPROCESSOR, MEMBER_AGENTINSTANCECONTEXT));
        return iterator;
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", localMethod(forge.getGenerateGroupKeyArrayJoin(), REF_JOINSET, constantTrue()))
                .declareVar(EventBean.EPTYPEARRAY, "result", localMethod(generateOutputEventsJoin, REF_JOINSET, ref("groupByKeys"), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.EPTYPE, ref("result")));
    }

    public static void clearMethodCodegen(CodegenMethod method) {
        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT);
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
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

    public static void processOutputLimitedViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
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

    public static void stopMethodCodegen(CodegenMethod method, CodegenInstanceAux instance) {
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

    static CodegenMethod generateOutputBatchedJoinUnkeyedCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0))
                    .declareVarNoInit(EventBean.EPTYPEARRAY, "eventsPerStream");

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(MultiKeyArrayOfKeys.EPTYPE, "row", ref("outputEvents"));
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("count")), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .assignRef("eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("row"), "getArray")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT)))
                            .incrementRef("count")
                            .blockContinue();
                }

                forEach.exprDotMethod(ref("resultEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT));
                }

                forEach.incrementRef("count");
            }
        };
        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedJoinUnkeyed",
                CodegenNamedParam.from(EPTypePremade.SET.getEPType(), "outputEvents", EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.COLLECTION.getEPType(), "resultEvents", EPTypePremade.LIST.getEPType(), "optSortKeys"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }

    static void generateOutputBatchedSingleCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupByKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockReturn(constantNull());
            }

            methodNode.getBlock().methodReturn(exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
        };
        instance.getMethods().addMethod(EventBean.EPTYPE, "generateOutputBatchedSingle", CodegenNamedParam.from(EPTypePremade.OBJECT.getEPType(), "groupByKey", EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE), ResultSetProcessorUtil.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedViewPerKeyCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(EventBean.EPTYPE, "outputEvent", ref("outputEvents"));
                forEach.declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", arrayAtIndex(ref("groupByKeys"), ref("count")))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .assignArrayElement(ref("eventsPerStream"), constant(0), arrayAtIndex(ref("outputEvents"), ref("count")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forEach.exprDotMethod(ref("resultEvents"), "put", ref("groupKey"), exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("optSortKeys"), "put", ref("groupKey"), exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT));
                }

                forEach.incrementRef("count");
            }
        };
        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedViewPerKey",
                CodegenNamedParam.from(EventBean.EPTYPEARRAY, "outputEvents", EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.MAP.getEPType(), "resultEvents", EPTypePremade.MAP.getEPType(), "optSortKeys", EventBean.EPTYPEARRAY, "eventsPerStream"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }


    static CodegenMethod generateOutputBatchedJoinPerKeyCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(MultiKeyArrayOfKeys.EPTYPE, "row", ref("outputEvents"));
                forEach.declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", arrayAtIndex(ref("groupByKeys"), ref("count")))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("row"), "getArray")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
                }

                forEach.exprDotMethod(ref("resultEvents"), "put", ref("groupKey"), exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("optSortKeys"), "put", ref("groupKey"), exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT));
                }

                forEach.incrementRef("count");
            }
        };
        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedJoinPerKey",
                CodegenNamedParam.from(EPTypePremade.SET.getEPType(), "outputEvents", EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.MAP.getEPType(), "resultEvents", EPTypePremade.MAP.getEPType(), "optSortKeys"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }

    static void removedAggregationGroupKeyCodegen(CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            if (instance.hasMember(NAME_OUTPUTALLGROUPREPS)) {
                method.getBlock().exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "remove", ref("key"));
            }
            if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
                method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), "remove", ref("key"));
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

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorAggregateGroupedForge forge, String methodName, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);

        if (forge.isOutputAll()) {
            CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType.EPTYPEARRAY, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorAggregateGroupedOutputAllHelper.EPTYPE);
            StateMgmtSetting stateMgmtSettings = forge.getOutputAllOptSettings().get();
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(factory, "makeRSAggregateGroupedOutputAll", MEMBER_AGENTINSTANCECONTEXT, ref("this"), groupKeyTypes, groupKeyMKSerde, eventTypes, stateMgmtSettings.toExpression()));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorAggregateGroupedOutputLastHelper.EPTYPE);
            StateMgmtSetting stateMgmtSettings = forge.getOutputLastOptSettings().get();
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(factory, "makeRSAggregateGroupedOutputLastOpt", MEMBER_AGENTINSTANCECONTEXT, ref("this"), groupKeyTypes, groupKeyMKSerde, stateMgmtSettings.toExpression()));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenMethod method) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputView", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputView", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenMethod method) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    private static void processOutputLimitedJoinLastCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedJoinPerKey = generateOutputBatchedJoinPerKeyCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "lastPerGroupNew", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()))
                .declareVar(EPTypePremade.MAP.getEPType(), "lastPerGroupOld", forge.isSelectRStream() ? newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()) : constantNull());

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "newEventsSortKey", constantNull())
                .declareVar(EPTypePremade.MAP.getEPType(), "oldEventsSortKey", constantNull());
        if (forge.isSorting()) {
            method.getBlock().assignRef("newEventsSortKey", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()))
                    .assignRef("oldEventsSortKey", forge.isSelectRStream() ? newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()) : constantNull());
        }

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()));

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedJoinPerKey, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("lastPerGroupOld"), ref("oldEventsSortKey"));
            }

            forEach.localMethod(generateOutputBatchedJoinPerKey, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("lastPerGroupNew"), ref("newEventsSortKey"));
        }

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS, ref("lastPerGroupNew")))
                .declareVar(EventBean.EPTYPEARRAY, "oldEventsArr", forge.isSelectRStream() ? staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS, ref("lastPerGroupOld")) : constantNull());

        if (forge.isSorting()) {
            method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("newEventsSortKey")))
                    .assignRef("newEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), MEMBER_AGENTINSTANCECONTEXT));
            if (forge.isSelectRStream()) {
                method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortKeysOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("oldEventsSortKey")))
                        .assignRef("oldEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("sortKeysOld"), MEMBER_AGENTINSTANCECONTEXT));
            }
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private static void processOutputLimitedJoinFirstCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedAddToList = generateOutputBatchedAddToListCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.EPTYPE, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.EPTYPE);
        StateMgmtSetting outputHelperSettings = forge.getOutputFirstHelperSettings().get();
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(helperFactory, "makeRSGroupedOutputFirst", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, outputFactory, constantNull(), constant(-1), groupKeyMKSerde, outputHelperSettings.toExpression()));

        method.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "newEvents", newInstance(EPTypePremade.LINKEDLIST.getEPType()));
        method.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "newEventsSortKey", constantNull());
        if (forge.isSorting()) {
            method.getBlock().assignRef("newEventsSortKey", newInstance(EPTypePremade.LINKEDLIST.getEPType()));
        }

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "workCollection", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));

        if (forge.getOptionalHavingNode() == null) {
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                            .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                                .incrementRef("count");
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                            .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aOldData", ref("oldData"));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aOldData"), "getArray")))
                                .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                                .incrementRef("count");
                    }
                }

                forEach.localMethod(generateOutputBatchedAddToList, ref("workCollection"), constantFalse(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
            }
        } else {
            // having clause present, having clause evaluates at the level of individual posts
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()))
                        .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                            .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT)))
                                .incrementRef("count")
                                .blockContinue();

                        forloop.declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                    }
                }

                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                            .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aOldData", ref("oldData"));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aOldData"), "getArray")))
                                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT)))
                                .incrementRef("count")
                                .blockContinue();

                        forloop.declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                    }
                }

                forEach.localMethod(generateOutputBatchedAddToList, ref("workCollection"), constantFalse(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
            }
        }

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYEVENTS, ref("newEvents")));

        if (forge.isSorting()) {
            method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYOBJECTS, ref("newEventsSortKey")))
                    .assignRef("newEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), MEMBER_AGENTINSTANCECONTEXT));
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), constantNull()));
    }

    private static void processOutputLimitedJoinAllCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedJoinUnkeyed = generateOutputBatchedJoinUnkeyedCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedAddToListSingle = generateOutputBatchedAddToListSingleCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType.EPTYPEARRAY, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.EPTYPE);
        StateMgmtSetting stateMgmtSettings = forge.getOutputAllHelperSettings().get();
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(helperFactory, "makeRSGroupedOutputAllNoOpt", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, groupKeyMKSerde, eventTypes, stateMgmtSettings.toExpression()));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "workCollection", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                        .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

                {
                    ifNewData.forEach(MultiKeyArrayOfKeys.EPTYPE, "aNewData", ref("newData"))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("aNewData"), "getArray")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                            .incrementRef("count")
                            .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"))
                            .exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"));
                }

                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                        .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
                {
                    ifOldData.forEach(MultiKeyArrayOfKeys.EPTYPE, "anOldData", ref("oldData"))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                            .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("anOldData"), "getArray")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                            .incrementRef("count");
                }
            }

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
            }
            forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
        }

        method.getBlock().declareVar(EPTypePremade.ITERATOR.getEPType(), "entryIterator", exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("entryIterator"), "hasNext"))
                    .declareVar(EPTypePremade.MAPENTRY.getEPType(), "entry", cast(EPTypePremade.MAPENTRY.getEPType(), exprDotMethod(ref("entryIterator"), "next")))
                    .ifCondition(not(exprDotMethod(ref("workCollection"), "containsKey", exprDotMethod(ref("entry"), "getKey"))))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedJoinDefaultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedJoinUnkeyed = generateOutputBatchedJoinUnkeyedCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
            }

            forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedViewPerKey = generateOutputBatchedViewPerKeyCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "lastPerGroupNew", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()))
                .declareVar(EPTypePremade.MAP.getEPType(), "lastPerGroupOld", forge.isSelectRStream() ? newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()) : constantNull());

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "newEventsSortKey", constantNull())
                .declareVar(EPTypePremade.MAP.getEPType(), "oldEventsSortKey", constantNull());
        if (forge.isSorting()) {
            method.getBlock().assignRef("newEventsSortKey", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()))
                    .assignRef("oldEventsSortKey", forge.isSelectRStream() ? newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()) : constantNull());
        }

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()));

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedViewPerKey, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("lastPerGroupOld"), ref("oldEventsSortKey"), ref("eventsPerStream"));
            }

            forEach.localMethod(generateOutputBatchedViewPerKey, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("lastPerGroupNew"), ref("newEventsSortKey"), ref("eventsPerStream"));
        }

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS, ref("lastPerGroupNew")))
                .declareVar(EventBean.EPTYPEARRAY, "oldEventsArr", forge.isSelectRStream() ? staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEEVENTS, ref("lastPerGroupOld")) : constantNull());

        if (forge.isSorting()) {
            method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("newEventsSortKey")))
                    .assignRef("newEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), MEMBER_AGENTINSTANCECONTEXT));
            if (forge.isSelectRStream()) {
                method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortKeysOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("oldEventsSortKey")))
                        .assignRef("oldEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("sortKeysOld"), MEMBER_AGENTINSTANCECONTEXT));
            }
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private static void processOutputLimitedViewFirstCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedAddToList = generateOutputBatchedAddToListCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.EPTYPE, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.EPTYPE);
        StateMgmtSetting outputHelperSettings = forge.getOutputFirstHelperSettings().get();
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(helperFactory, "makeRSGroupedOutputFirst", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, outputFactory, constantNull(), constant(-1), groupKeyMKSerde, outputHelperSettings.toExpression()));

        method.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "newEvents", newInstance(EPTypePremade.LINKEDLIST.getEPType()));
        method.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "newEventsSortKey", constantNull());
        if (forge.isSorting()) {
            method.getBlock().assignRef("newEventsSortKey", newInstance(EPTypePremade.LINKEDLIST.getEPType()));
        }

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "workCollection", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()))
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));

        if (forge.getOptionalHavingNode() == null) {
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()));
                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                                .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.EPTYPE, arrayAtIndex(ref("newData"), ref("i"))));
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forLoopIntSimple("i", arrayLength(ref("oldData")));
                        forloop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("oldData"), ref("i")))
                                .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("i")))
                                .declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.EPTYPE, arrayAtIndex(ref("oldData"), ref("i"))));
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }

                forEach.localMethod(generateOutputBatchedAddToList, ref("workCollection"), constantFalse(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
            }
        } else {
            // having clause present, having clause evaluates at the level of individual posts
            {
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
                forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                        .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()))
                        .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                                .assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.EPTYPE, arrayAtIndex(ref("newData"), ref("i"))));
                    }
                }

                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forLoopIntSimple("i", arrayLength(ref("oldData")));
                        forloop.declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("i")))
                                .assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("oldData"), ref("i")))
                                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.EPTYPE, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.EPTYPE, arrayAtIndex(ref("oldData"), ref("i"))));
                    }
                }

                forEach.localMethod(generateOutputBatchedAddToList, ref("workCollection"), constantFalse(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
            }
        }

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "newEventsArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYEVENTS, ref("newEvents")));

        if (forge.isSorting()) {
            method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortKeysNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYOBJECTS, ref("newEventsSortKey")))
                    .assignRef("newEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), MEMBER_AGENTINSTANCECONTEXT));
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), constantNull()));
    }

    private static void processOutputLimitedViewAllCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedViewUnkeyed = generateOutputBatchedViewUnkeyedCodegen(forge, classScope, instance);
        CodegenMethod generateOutputBatchedAddToListSingle = generateOutputBatchedAddToListSingleCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType.EPTYPEARRAY, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.EPTYPE);
        StateMgmtSetting stateMgmtSettings = forge.getOutputAllHelperSettings().get();
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(helperFactory, "makeRSGroupedOutputAllNoOpt", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, groupKeyMKSerde, eventTypes, stateMgmtSettings.toExpression()));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "workCollection", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()))
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()));

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                        .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

                {
                    ifNewData.forEach(EventBean.EPTYPE, "aNewData", ref("newData"))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                            .assignArrayElement(ref("eventsPerStream"), constant(0), ref("aNewData"))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                            .incrementRef("count")
                            .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"))
                            .exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.EPTYPE, ref("aNewData")));
                }

                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                        .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));
                {
                    ifOldData.forEach(EventBean.EPTYPE, "anOldData", ref("oldData"))
                            .declareVar(EPTypePremade.OBJECT.getEPType(), "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                            .assignArrayElement(ref("eventsPerStream"), constant(0), ref("anOldData"))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                            .incrementRef("count");
                }
            }

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedViewUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), ref("eventsPerStream"));
            }
            forEach.localMethod(generateOutputBatchedViewUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), ref("eventsPerStream"));
        }

        method.getBlock().declareVar(EPTypePremade.ITERATOR.getEPType(), "entryIterator", exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("entryIterator"), "hasNext"))
                    .declareVar(EPTypePremade.MAPENTRY.getEPType(), "entry", cast(EPTypePremade.MAPENTRY.getEPType(), exprDotMethod(ref("entryIterator"), "next")))
                    .ifCondition(not(exprDotMethod(ref("workCollection"), "containsKey", exprDotMethod(ref("entry"), "getKey"))))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedViewUnkeyed = generateOutputBatchedViewUnkeyedCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                    .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()));

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedViewUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), ref("eventsPerStream"));
            }

            forEach.localMethod(generateOutputBatchedViewUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), ref("eventsPerStream"));
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static CodegenMethod generateOutputBatchedAddToListCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedAddToListSingle = generateOutputBatchedAddToListSingleCodegen(forge, classScope, instance);

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().forEach(EPTypePremade.MAPENTRY.getEPType(), "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("entry"), "getValue")), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("resultEvents"), ref("optSortKeys"));
        };

        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedAddToList",
                CodegenNamedParam.from(EPTypePremade.MAP.getEPType(), "keysAndEvents", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LIST.getEPType(), "resultEvents", EPTypePremade.LIST.getEPType(), "optSortKeys"),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    private static CodegenMethod generateOutputBatchedAddToListSingleCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            {
                methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("key"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

                if (forge.getOptionalHavingNode() != null) {
                    methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockReturnNoValue();
                }

                methodNode.getBlock().exprDotMethod(ref("resultEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    methodNode.getBlock().exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT));
                }
            }
        };

        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedAddToListSingle",
                CodegenNamedParam.from(EPTypePremade.OBJECT.getEPType(), "key", EventBean.EPTYPEARRAY, "eventsPerStream", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.LIST.getEPType(), "resultEvents", EPTypePremade.LIST.getEPType(), "optSortKeys"),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedViewUnkeyedCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(EventBean.EPTYPE, "outputEvent", ref("outputEvents"));
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("count")), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .assignArrayElement(ref("eventsPerStream"), constant(0), arrayAtIndex(ref("outputEvents"), ref("count")));

                if (forge.getOptionalHavingNode() != null) {
                    forEach.ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT)))
                            .incrementRef("count")
                            .blockContinue();
                }

                forEach.exprDotMethod(ref("resultEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));

                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("optSortKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT));
                }

                forEach.incrementRef("count");
            }
        };
        return instance.getMethods().addMethod(EPTypePremade.VOID.getEPType(), "generateOutputBatchedViewUnkeyed",
                CodegenNamedParam.from(EventBean.EPTYPEARRAY, "outputEvents", EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE, EPTypePremade.COLLECTION.getEPType(), "resultEvents", EPTypePremade.LIST.getEPType(), "optSortKeys", EventBean.EPTYPEARRAY, "eventsPerStream"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }

    private static CodegenMethod processViewResultPairDepthOneCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);
        CodegenMethod generateGroupKeySingle = forge.getGenerateGroupKeySingle();

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(EPTypePremade.OBJECT.getEPType(), "newGroupKey", localMethod(generateGroupKeySingle, REF_NEWDATA, constantTrue()))
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "oldGroupKey", localMethod(generateGroupKeySingle, REF_OLDDATA, constantFalse()))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("newGroupKey"), MEMBER_AGENTINSTANCECONTEXT)
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", REF_OLDDATA, ref("oldGroupKey"), MEMBER_AGENTINSTANCECONTEXT)
                    .declareVar(EventBean.EPTYPE, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("newGroupKey"), constantTrue(), REF_ISSYNTHESIZE));
            if (!forge.isSelectRStream()) {
                methodNode.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")));
            } else {
                methodNode.getBlock().declareVar(EventBean.EPTYPE, "rstream", localMethod(shortcutEvalGivenKey, REF_OLDDATA, ref("oldGroupKey"), constantFalse(), REF_ISSYNTHESIZE))
                        .methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfAllNullSingle", ref("istream"), ref("rstream")));
            }
        };

        return instance.getMethods().addMethod(UniformPair.EPTYPE, "processViewResultPairDepthOne", CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_NEWDATA, EventBean.EPTYPEARRAY, NAME_OLDDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod processViewResultNewDepthOneCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock()
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "groupKey", localMethod(forge.getGenerateGroupKeySingle(), REF_NEWDATA, constantTrue()))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT)
                    .declareVar(EventBean.EPTYPE, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("groupKey"), constantTrue(), REF_ISSYNTHESIZE))
                    .methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")));
        };

        return instance.getMethods().addMethod(UniformPair.EPTYPE, "processViewResultNewDepthOneCodegen", CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_NEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod shortcutEvalGivenKeyCodegen(ExprForge optionalHavingNode, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());
            if (optionalHavingNode != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockReturn(constantNull());
            }
            methodNode.getBlock().methodReturn(exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
        };

        return instance.getMethods().addMethod(EventBean.EPTYPE, "shortcutEvalGivenKey",
                CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.OBJECT.getEPType(), "groupKey", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISSYNTHESIZE),
                ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }
}
