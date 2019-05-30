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
import com.espertech.esper.common.internal.util.CollectionUtil;
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

    public static void applyJoinResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
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

    public static void processJoinResultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputEventsJoin = generateOutputEventsJoinCodegen(forge, classScope, instance);

        method.getBlock().declareVar(Object[].class, "newDataGroupByKeys", localMethod(forge.getGenerateGroupKeyArrayJoin(), REF_NEWDATA, constantTrue()))
                .declareVar(Object[].class, "oldDataGroupByKeys", localMethod(forge.getGenerateGroupKeyArrayJoin(), REF_OLDDATA, constantFalse()));

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }

        method.getBlock().staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataGroupByKeys"), REF_OLDDATA, ref("oldDataGroupByKeys"));

        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsJoin, REF_OLDDATA, ref("oldDataGroupByKeys"), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsJoin, REF_NEWDATA, ref("newDataGroupByKeys"), constantTrue(), REF_ISSYNTHESIZE))
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

        method.getBlock().declareVar(Object[].class, "newDataGroupByKeys", localMethod(forge.getGenerateGroupKeyArrayView(), REF_NEWDATA, constantTrue()))
                .declareVar(Object[].class, "oldDataGroupByKeys", localMethod(forge.getGenerateGroupKeyArrayView(), REF_OLDDATA, constantFalse()))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataGroupByKeys"), REF_OLDDATA, ref("oldDataGroupByKeys"), ref("eventsPerStream"));

        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, REF_OLDDATA, ref("oldDataGroupByKeys"), constantFalse(), REF_ISSYNTHESIZE, ref("eventsPerStream")) : constantNull())
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsView, REF_NEWDATA, ref("newDataGroupByKeys"), constantTrue(), REF_ISSYNTHESIZE, ref("eventsPerStream")))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    private static CodegenMethod generateOutputEventsViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifNullReturnNull(ref("outputEvents"))
                    .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, arrayLength(ref("outputEvents"))))
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, arrayLength(ref("outputEvents"))));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean[][].class, "currentGenerators", newArrayByLength(EventBean[].class, arrayLength(ref("outputEvents"))));
            }

            methodNode.getBlock().declareVar(int.class, "countOutputRows", constant(0))
                    .declareVar(int.class, "cpid", exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

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
                    forLoop.assignArrayElement("currentGenerators", ref("countOutputRows"), newArrayWithInit(EventBean.class, arrayAtIndex(ref("outputEvents"), ref("countInputRows"))));
                }

                forLoop.incrementRef("countOutputRows")
                        .blockEnd();
            }

            ResultSetProcessorUtil.outputFromCountMaySortCodegen(methodNode.getBlock(), ref("countOutputRows"), ref("events"), ref("keys"), ref("currentGenerators"), forge.isSorting());
        };

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsView",
                CodegenNamedParam.from(EventBean[].class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, EventBean[].class, NAME_EPS),
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
                    .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, exprDotMethod(ref("resultSet"), "size")))
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("resultSet"), "size")));

            if (forge.isSorting()) {
                methodNode.getBlock().declareVar(EventBean[][].class, "currentGenerators", newArrayByLength(EventBean[].class, exprDotMethod(ref("resultSet"), "size")));
            }

            methodNode.getBlock().declareVar(int.class, "countOutputRows", constant(0))
                    .declareVar(int.class, "countInputRows", constant(-1))
                    .declareVar(int.class, "cpid", exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"));

            {
                CodegenBlock forLoop = methodNode.getBlock().forEach(MultiKeyArrayOfKeys.class, "row", ref("resultSet"));
                forLoop.incrementRef("countInputRows")
                        .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("row"), "getArray")))
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

        return instance.getMethods().addMethod(EventBean[].class, "generateOutputEventsJoin",
                CodegenNamedParam.from(Set.class, "resultSet", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    public static void getIteratorViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, method, classScope, instance), REF_VIEWABLE));
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

        method.getBlock().declareVar(ArrayDeque.class, "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, localMethod(obtainIteratorCodegen(forge, method, classScope, instance), REF_VIEWABLE)))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenMethod parent, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod iterator = parent.makeChild(Iterator.class, ResultSetProcessorAggregateGroupedImpl.class, classScope).addParam(Viewable.class, NAME_VIEWABLE);
        if (!forge.isSorting()) {
            iterator.getBlock().methodReturn(newInstance(ResultSetProcessorAggregateGroupedIterator.class, exprDotMethod(REF_VIEWABLE, "iterator"), ref("this"), MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT));
            return iterator;
        }

        // Pull all parent events, generate order keys
        iterator.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(List.class, "outgoingEvents", newInstance(ArrayList.class))
                .declareVar(List.class, "orderKeys", newInstance(ArrayList.class));

        {
            CodegenBlock forLoop = iterator.getBlock().forEach(EventBean.class, "candidate", REF_VIEWABLE);
            forLoop.assignArrayElement(ref("eventsPerStream"), constant(0), ref("candidate"))
                    .declareVar(Object.class, "groupKey", localMethod(forge.getGenerateGroupKeySingle(), ref("eventsPerStream"), constantTrue()))
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

        method.getBlock().declareVar(Object[].class, "groupByKeys", localMethod(forge.getGenerateGroupKeyArrayJoin(), REF_JOINSET, constantTrue()))
                .declareVar(EventBean[].class, "result", localMethod(generateOutputEventsJoin, REF_JOINSET, ref("groupByKeys"), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("result")));
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
                    .declareVar(int.class, "count", constant(0))
                    .declareVarNoInit(EventBean[].class, "eventsPerStream");

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(MultiKeyArrayOfKeys.class, "row", ref("outputEvents"));
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("count")), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .assignRef("eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("row"), "getArray")));

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
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedJoinUnkeyed",
                CodegenNamedParam.from(Set.class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Collection.class, "resultEvents", List.class, "optSortKeys"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }

    static void generateOutputBatchedSingleCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupByKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull());

            if (forge.getOptionalHavingNode() != null) {
                methodNode.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))).blockReturn(constantNull());
            }

            methodNode.getBlock().methodReturn(exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
        };
        instance.getMethods().addMethod(EventBean.class, "generateOutputBatchedSingle", CodegenNamedParam.from(Object.class, "groupByKey", EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorUtil.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedViewPerKeyCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(int.class, "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(EventBean.class, "outputEvent", ref("outputEvents"));
                forEach.declareVar(Object.class, "groupKey", arrayAtIndex(ref("groupByKeys"), ref("count")))
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
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedViewPerKey",
                CodegenNamedParam.from(EventBean[].class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Map.class, "resultEvents", Map.class, "optSortKeys", EventBean[].class, "eventsPerStream"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }


    static CodegenMethod generateOutputBatchedJoinPerKeyCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(int.class, "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(MultiKeyArrayOfKeys.class, "row", ref("outputEvents"));
                forEach.declareVar(Object.class, "groupKey", arrayAtIndex(ref("groupByKeys"), ref("count")))
                        .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("groupKey"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                        .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("row"), "getArray")));

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
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedJoinPerKey",
                CodegenNamedParam.from(Set.class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Map.class, "resultEvents", Map.class, "optSortKeys"), ResultSetProcessorAggregateGrouped.class, classScope, code);
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
        instance.getMethods().addMethod(void.class, "removedAggregationGroupKey", CodegenNamedParam.from(Object.class, "key"), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorAggregateGroupedForge forge, String methodName, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);

        if (forge.isOutputAll()) {
            CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorAggregateGroupedOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(factory, "makeRSAggregateGroupedOutputAll", MEMBER_AGENTINSTANCECONTEXT, ref("this"), groupKeyTypes, groupKeyMKSerde, eventTypes));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorAggregateGroupedOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(factory, "makeRSAggregateGroupedOutputLastOpt", MEMBER_AGENTINSTANCECONTEXT, ref("this"), groupKeyTypes, groupKeyMKSerde));
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
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()));

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

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
                    .assignRef("newEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), MEMBER_AGENTINSTANCECONTEXT));
            if (forge.isSelectRStream()) {
                method.getBlock().declareVar(Object[].class, "sortKeysOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("oldEventsSortKey")))
                        .assignRef("oldEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("sortKeysOld"), MEMBER_AGENTINSTANCECONTEXT));
            }
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private static void processOutputLimitedJoinFirstCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedAddToList = generateOutputBatchedAddToListCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(helperFactory, "makeRSGroupedOutputFirst", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, outputFactory, constantNull(), constant(-1), groupKeyMKSerde));

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
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                                .incrementRef("count");
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKeyArrayOfKeys.class, "aOldData", ref("oldData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aOldData"), "getArray")))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
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
                CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
                forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                        .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()))
                        .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGJOINRESULTKEYEDJOIN, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifNewData.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT)))
                                .incrementRef("count")
                                .blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        forloop.ifCondition(ref("pass"))
                                .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"));
                    }
                }

                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                            .declareVar(int.class, "count", constant(0));
                    {
                        CodegenBlock forloop = ifOldData.forEach(MultiKeyArrayOfKeys.class, "aOldData", ref("oldData"));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                                .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aOldData"), "getArray")))
                                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT)))
                                .incrementRef("count")
                                .blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
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
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(helperFactory, "makeRSGroupedOutputAllNoOpt", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, groupKeyMKSerde, eventTypes));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "workCollection", newInstance(LinkedHashMap.class));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                        .declareVar(int.class, "count", constant(0));

                {
                    ifNewData.forEach(MultiKeyArrayOfKeys.class, "aNewData", ref("newData"))
                            .declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("aNewData"), "getArray")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                            .incrementRef("count")
                            .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"))
                            .exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), ref("eventsPerStream"));
                }

                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                        .declareVar(int.class, "count", constant(0));
                {
                    ifOldData.forEach(MultiKeyArrayOfKeys.class, "anOldData", ref("oldData"))
                            .declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
                            .declareVar(EventBean[].class, "eventsPerStream", cast(EventBean[].class, exprDotMethod(ref("anOldData"), "getArray")))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                            .incrementRef("count");
                }
            }

            if (forge.isSelectRStream()) {
                forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("oldData"), ref("oldDataMultiKey"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"));
            }
            forEach.localMethod(generateOutputBatchedJoinUnkeyed, ref("newData"), ref("newDataMultiKey"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));
        }

        method.getBlock().declareVar(Iterator.class, "entryIterator", exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("entryIterator"), "hasNext"))
                    .declareVar(Map.Entry.class, "entry", cast(Map.Entry.class, exprDotMethod(ref("entryIterator"), "next")))
                    .ifCondition(not(exprDotMethod(ref("workCollection"), "containsKey", exprDotMethod(ref("entry"), "getKey"))))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedJoinDefaultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedJoinUnkeyed = generateOutputBatchedJoinUnkeyedCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayJoin(), ref("oldData"), constantFalse()));

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
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()));

            forEach.staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"));

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
                    .assignRef("newEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("newEventsArr"), ref("sortKeysNew"), MEMBER_AGENTINSTANCECONTEXT));
            if (forge.isSelectRStream()) {
                method.getBlock().declareVar(Object[].class, "sortKeysOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYNULLFOREMPTYVALUEVALUES, ref("oldEventsSortKey")))
                        .assignRef("oldEventsArr", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("oldEventsArr"), ref("sortKeysOld"), MEMBER_AGENTINSTANCECONTEXT));
            }
        }

        method.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private static void processOutputLimitedViewFirstCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedAddToList = generateOutputBatchedAddToListCodegen(forge, classScope, instance);

        CodegenExpressionField helperFactory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpressionField outputFactory = classScope.addFieldUnshared(true, OutputConditionPolledFactory.class, forge.getOptionalOutputFirstConditionFactory().make(classScope.getPackageScope().getInitMethod(), classScope));
        CodegenExpression groupKeyTypes = constant(forge.getGroupKeyTypes());
        CodegenExpression groupKeyMKSerde = forge.getMultiKeyClassRef().getExprMKSerde(method, classScope);
        instance.addMember(NAME_OUTPUTFIRSTHELPER, ResultSetProcessorGroupedOutputFirstHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTFIRSTHELPER, exprDotMethod(helperFactory, "makeRSGroupedOutputFirst", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, outputFactory, constantNull(), constant(-1), groupKeyMKSerde));

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
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()));
                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                                .declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(1), constant(0)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.class, arrayAtIndex(ref("newData"), ref("i"))));
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }
                {
                    CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")));
                    {
                        CodegenBlock forloop = ifOldData.forLoopIntSimple("i", arrayLength(ref("oldData")));
                        forloop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("oldData"), ref("i")))
                                .declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("i")))
                                .declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
                                .declareVar(boolean.class, "pass", exprDotMethod(ref("outputStateGroup"), "updateOutputCondition", constant(0), constant(1)));
                        CodegenBlock ifPass = forloop.ifCondition(ref("pass"));
                        ifPass.exprDotMethod(ref("workCollection"), "put", ref("mk"), newArrayWithInit(EventBean.class, arrayAtIndex(ref("oldData"), ref("i"))));
                        forloop.exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT);
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
                        .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                        .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()))
                        .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("newDataMultiKey"), ref("oldData"), ref("oldDataMultiKey"), ref("eventsPerStream"));

                {
                    CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")));
                    {
                        CodegenBlock forloop = ifNewData.forLoopIntSimple("i", arrayLength(ref("newData")));
                        forloop.declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("i")))
                                .assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("newData"), ref("i")))
                                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
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
                                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", ref("mk"), exprDotMethod(MEMBER_AGENTINSTANCECONTEXT, "getAgentInstanceId"), constantNull())
                                .ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), ref("eventsPerStream"), constantFalse(), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();

                        forloop.declareVar(OutputConditionPolled.class, "outputStateGroup", exprDotMethod(member(NAME_OUTPUTFIRSTHELPER), "getOrAllocate", ref("mk"), MEMBER_AGENTINSTANCECONTEXT, outputFactory))
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
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        instance.addMember(NAME_OUTPUTALLGROUPREPS, ResultSetProcessorGroupedOutputAllGroupReps.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLGROUPREPS, exprDotMethod(helperFactory, "makeRSGroupedOutputAllNoOpt", MEMBER_AGENTINSTANCECONTEXT, groupKeyTypes, groupKeyMKSerde, eventTypes));

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(Map.class, "workCollection", newInstance(LinkedHashMap.class))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()));

            {
                CodegenBlock ifNewData = forEach.ifCondition(notEqualsNull(ref("newData")))
                        .declareVar(int.class, "count", constant(0));

                {
                    ifNewData.forEach(EventBean.class, "aNewData", ref("newData"))
                            .declareVar(Object.class, "mk", arrayAtIndex(ref("newDataMultiKey"), ref("count")))
                            .assignArrayElement(ref("eventsPerStream"), constant(0), ref("aNewData"))
                            .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", ref("eventsPerStream"), ref("mk"), MEMBER_AGENTINSTANCECONTEXT)
                            .incrementRef("count")
                            .exprDotMethod(ref("workCollection"), "put", ref("mk"), ref("eventsPerStream"))
                            .exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "put", ref("mk"), newArrayWithInit(EventBean.class, ref("aNewData")));
                }

                CodegenBlock ifOldData = forEach.ifCondition(notEqualsNull(ref("oldData")))
                        .declareVar(int.class, "count", constant(0));
                {
                    ifOldData.forEach(EventBean.class, "anOldData", ref("oldData"))
                            .declareVar(Object.class, "mk", arrayAtIndex(ref("oldDataMultiKey"), ref("count")))
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

        method.getBlock().declareVar(Iterator.class, "entryIterator", exprDotMethod(member(NAME_OUTPUTALLGROUPREPS), "entryIterator"));
        {
            method.getBlock().whileLoop(exprDotMethod(ref("entryIterator"), "hasNext"))
                    .declareVar(Map.Entry.class, "entry", cast(Map.Entry.class, exprDotMethod(ref("entryIterator"), "next")))
                    .ifCondition(not(exprDotMethod(ref("workCollection"), "containsKey", exprDotMethod(ref("entry"), "getKey"))))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"));

        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputBatchedViewUnkeyed = generateOutputBatchedViewUnkeyedCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(Object[].class, "newDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("newData"), constantTrue()))
                    .declareVar(Object[].class, "oldDataMultiKey", localMethod(forge.getGenerateGroupKeyArrayView(), ref("oldData"), constantFalse()));

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
            methodNode.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(ref("keysAndEvents"), "entrySet"))
                    .localMethod(generateOutputBatchedAddToListSingle, exprDotMethod(ref("entry"), "getKey"), cast(EventBean[].class, exprDotMethod(ref("entry"), "getValue")), REF_ISNEWDATA, REF_ISSYNTHESIZE, ref("resultEvents"), ref("optSortKeys"));
        };

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedAddToList",
                CodegenNamedParam.from(Map.class, "keysAndEvents", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"),
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

        return instance.getMethods().addMethod(void.class, "generateOutputBatchedAddToListSingle",
                CodegenNamedParam.from(Object.class, "key", EventBean[].class, "eventsPerStream", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents", List.class, "optSortKeys"),
                ResultSetProcessorAggregateGroupedImpl.class, classScope, code);
    }

    static CodegenMethod generateOutputBatchedViewUnkeyedCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().ifCondition(equalsNull(ref("outputEvents"))).blockReturnNoValue()
                    .declareVar(int.class, "count", constant(0));

            {
                CodegenBlock forEach = methodNode.getBlock().forEach(EventBean.class, "outputEvent", ref("outputEvents"));
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
        return instance.getMethods().addMethod(void.class, "generateOutputBatchedViewUnkeyed",
                CodegenNamedParam.from(EventBean[].class, "outputEvents", Object[].class, "groupByKeys", boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, Collection.class, "resultEvents", List.class, "optSortKeys", EventBean[].class, "eventsPerStream"), ResultSetProcessorAggregateGrouped.class, classScope, code);
    }

    private static CodegenMethod processViewResultPairDepthOneCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);
        CodegenMethod generateGroupKeySingle = forge.getGenerateGroupKeySingle();

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock().declareVar(Object.class, "newGroupKey", localMethod(generateGroupKeySingle, REF_NEWDATA, constantTrue()))
                    .declareVar(Object.class, "oldGroupKey", localMethod(generateGroupKeySingle, REF_OLDDATA, constantFalse()))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("newGroupKey"), MEMBER_AGENTINSTANCECONTEXT)
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyLeave", REF_OLDDATA, ref("oldGroupKey"), MEMBER_AGENTINSTANCECONTEXT)
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

    private static CodegenMethod processViewResultNewDepthOneCodegen(ResultSetProcessorAggregateGroupedForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethod shortcutEvalGivenKey = shortcutEvalGivenKeyCodegen(forge.getOptionalHavingNode(), classScope, instance);

        Consumer<CodegenMethod> code = methodNode -> {
            methodNode.getBlock()
                    .declareVar(Object.class, "groupKey", localMethod(forge.getGenerateGroupKeySingle(), REF_NEWDATA, constantTrue()))
                    .exprDotMethod(MEMBER_AGGREGATIONSVC, "applyEnter", REF_NEWDATA, ref("groupKey"), MEMBER_AGENTINSTANCECONTEXT)
                    .declareVar(EventBean.class, "istream", localMethod(shortcutEvalGivenKey, REF_NEWDATA, ref("groupKey"), constantTrue(), REF_ISSYNTHESIZE))
                    .methodReturn(staticMethod(ResultSetProcessorUtil.class, "toPairNullIfNullIStream", ref("istream")));
        };

        return instance.getMethods().addMethod(UniformPair.class, "processViewResultNewDepthOneCodegen", CodegenNamedParam.from(EventBean[].class, NAME_NEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowPerGroupImpl.class, classScope, code);
    }

    private static CodegenMethod shortcutEvalGivenKeyCodegen(ExprForge optionalHavingNode, CodegenClassScope classScope, CodegenInstanceAux instance) {
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
}
