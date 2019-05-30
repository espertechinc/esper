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
package com.espertech.esper.common.internal.epl.resultset.rowforall;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.collection.SingleEventIterator;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactoryField;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil.*;
import static com.espertech.esper.common.internal.util.CollectionUtil.METHOD_TOARRAYMAYNULL;

/**
 * Result set processor for the case: aggregation functions used in the select clause, and no group-by,
 * and all properties in the select clause are under an aggregation function.
 * <p>
 * This processor does not perform grouping, every event entering and leaving is in the same group.
 * Produces one old event and one new event row every time either at least one old or new event is received.
 * Aggregation state is simply one row holding all the state.
 */
public class ResultSetProcessorRowForAllImpl {
    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";

    public static void processJoinResultCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instanceMethods) {
        CodegenMethod selectList = getSelectListEventsAsArrayCodegen(forge, classScope, instanceMethods);

        if (forge.isUnidirectional()) {
            method.getBlock().expression(exprDotMethod(ref("this"), "clear"));
        }

        CodegenExpression selectOld;
        if (forge.isSelectRStream()) {
            selectOld = localMethod(selectList, constantFalse(), REF_ISSYNTHESIZE, constantTrue());
        } else {
            selectOld = constantNull();
        }
        method.getBlock()
                .declareVar(EventBean[].class, "selectOldEvents", selectOld)
                .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA)
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(selectList, constantTrue(), REF_ISSYNTHESIZE, constantTrue()))
                .ifCondition(and(equalsNull(ref("selectNewEvents")), equalsNull(ref("selectOldEvents"))))
                .blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    public static void processViewResultCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {

        CodegenMethod selectList = getSelectListEventsAsArrayCodegen(forge, classScope, instance);

        CodegenExpression selectOld;
        if (forge.isSelectRStream()) {
            selectOld = localMethod(selectList, constantFalse(), REF_ISSYNTHESIZE, constantFalse());
        } else {
            selectOld = constantNull();
        }
        method.getBlock()
                .declareVar(EventBean[].class, "selectOldEvents", selectOld)
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA, ref("eventsPerStream"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(selectList, constantTrue(), REF_ISSYNTHESIZE, constantFalse()))
                .ifCondition(and(equalsNull(ref("selectNewEvents")), equalsNull(ref("selectOldEvents"))))
                .blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    static void getIteratorViewCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod obtainMethod = obtainIteratorCodegen(forge, classScope, method, instance);
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainMethod));
            return;
        }

        method.getBlock()
                .staticMethod(ResultSetProcessorUtil.class, METHOD_CLEARANDAGGREGATEUNGROUPED, MEMBER_AGENTINSTANCECONTEXT, MEMBER_AGGREGATIONSVC, REF_VIEWABLE)
                .declareVar(Iterator.class, "iterator", localMethod(obtainMethod))
                .expression(exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT))
                .methodReturn(ref("iterator"));
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod select = getSelectListEventsAsArrayCodegen(forge, classScope, instance);
        method.getBlock()
                .declareVar(EventBean[].class, "result", localMethod(select, constant(true), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("result")));
    }

    static void clearCodegen(CodegenMethod method) {
        method.getBlock().expression(exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT));
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            processOutputLimitedJoinLastCodegen(forge, classScope, method, instance);
        } else {
            processOutputLimitedJoinDefaultCodegen(forge, classScope, method, instance);
        }
    }

    public static void processOutputLimitedViewCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            processOutputLimitedViewLastCodegen(forge, classScope, method, instance);
        } else {
            processOutputLimitedViewDefaultCodegen(forge, classScope, method, instance);
        }
    }

    public static void applyViewResultCodegen(CodegenMethod method) {
        method.getBlock().staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA, newArrayByLength(EventBean.class, constant(1)));
    }

    public static void applyJoinResultCodegen(CodegenMethod method) {
        method.getBlock().staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA);
    }

    static void stopCodegen(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), "destroy");
        }
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen("processView", forge, classScope, method, instance);
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen("processJoin", forge, classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(String methodName, ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);

        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorRowForAllOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(factory, "makeRSRowForAllOutputAll", ref("this"), MEMBER_AGENTINSTANCECONTEXT));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.LAST) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorRowForAllOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(factory, "makeRSRowForAllOutputLast", ref("this"), MEMBER_AGENTINSTANCECONTEXT));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        }
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowForAllForge forge, CodegenMethod method) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputView", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputView", REF_ISSYNTHESIZE));
        }
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowForAllForge forge, CodegenMethod method) {
        if (forge.getOutputLimitSpec().getDisplayLimit() == OutputLimitLimitType.ALL) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        }
    }

    public static void acceptHelperVisitorCodegen(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTLASTHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTALLHELPER));
        }
    }

    private static void processOutputLimitedJoinDefaultCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod getSelectListEventAddList = getSelectListEventsAddListCodegen(forge, classScope, instance);
        CodegenMethod getSelectListEventAsArray = getSelectListEventsAsArrayCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }
            if (forge.isSelectRStream()) {
                forEach.localMethod(getSelectListEventAddList, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"));
                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("oldEventsSortKey"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", constantNull(), constantFalse(), MEMBER_AGENTINSTANCECONTEXT));
                }
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, cast(Set.class, exprDotMethod(ref("pair"), "getFirst")), cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));
            forEach.localMethod(getSelectListEventAddList, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"));
            if (forge.isSorting()) {
                forEach.exprDotMethod(ref("newEventsSortKey"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", constantNull(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
            }
            forEach.blockEnd();
        }

        CodegenBlock ifEmpty = method.getBlock().ifCondition(not(exprDotMethod(REF_JOINEVENTSSET, "isEmpty")));
        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(ifEmpty, ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());

        method.getBlock().declareVar(EventBean[].class, "newEventsArr", localMethod(getSelectListEventAsArray, constantTrue(), REF_ISSYNTHESIZE, constantFalse()))
                .declareVar(EventBean[].class, "oldEventsArr", forge.isSelectRStream() ? localMethod(getSelectListEventAsArray, constantFalse(), REF_ISSYNTHESIZE, constantFalse()) : constantNull())
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private static void processOutputLimitedJoinLastCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod getSelectListEventSingle = getSelectListEventSingleCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EventBean.class, "lastOldEvent", constantNull())
                .declareVar(EventBean.class, "lastNewEvent", constantNull());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }
            if (forge.isSelectRStream()) {
                forEach.ifCondition(equalsNull(ref("lastOldEvent")))
                        .assignRef("lastOldEvent", localMethod(getSelectListEventSingle, constantFalse(), REF_ISSYNTHESIZE))
                        .blockEnd();
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, cast(Set.class, exprDotMethod(ref("pair"), "getFirst")), cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));
            forEach.assignRef("lastNewEvent", localMethod(getSelectListEventSingle, constantTrue(), REF_ISSYNTHESIZE));
        }

        {
            CodegenBlock ifEmpty = method.getBlock().ifCondition(exprDotMethod(REF_JOINEVENTSSET, "isEmpty"));
            if (forge.isSelectRStream()) {
                ifEmpty.assignRef("lastOldEvent", localMethod(getSelectListEventSingle, constantFalse(), REF_ISSYNTHESIZE))
                        .assignRef("lastNewEvent", ref("lastOldEvent"));
            } else {
                ifEmpty.assignRef("lastNewEvent", localMethod(getSelectListEventSingle, constantTrue(), REF_ISSYNTHESIZE));
            }
        }

        method.getBlock()
                .declareVar(EventBean[].class, "lastNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastNewEvent")))
                .declareVar(EventBean[].class, "lastOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastOldEvent")))
                .ifCondition(and(equalsNull(ref("lastNew")), equalsNull(ref("lastOld"))))
                .blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("lastNew"), ref("lastOld")));
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod getSelectListEventAddList = getSelectListEventsAddListCodegen(forge, classScope, instance);
        CodegenMethod getSelectListEventAsArray = getSelectListEventsAsArrayCodegen(forge, classScope, instance);

        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));
        CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
        {
            if (forge.isSelectRStream()) {
                forEach.localMethod(getSelectListEventAddList, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"));
                if (forge.isSorting()) {
                    forEach.exprDotMethod(ref("oldEventsSortKey"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", constantNull(), constantFalse(), MEMBER_AGENTINSTANCECONTEXT));
                }
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")), cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")), ref("eventsPerStream"));
            forEach.localMethod(getSelectListEventAddList, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"));
            if (forge.isSorting()) {
                forEach.exprDotMethod(ref("newEventsSortKey"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", constantNull(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
            }
            forEach.blockEnd();
        }

        CodegenBlock ifEmpty = method.getBlock().ifCondition(not(exprDotMethod(REF_VIEWEVENTSLIST, "isEmpty")));
        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(ifEmpty, ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());

        method.getBlock().declareVar(EventBean[].class, "newEventsArr", localMethod(getSelectListEventAsArray, constantTrue(), REF_ISSYNTHESIZE, constantFalse()))
                .declareVar(EventBean[].class, "oldEventsArr", forge.isSelectRStream() ? localMethod(getSelectListEventAsArray, constantFalse(), REF_ISSYNTHESIZE, constantFalse()) : constantNull())
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("newEventsArr"), ref("oldEventsArr")));
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod getSelectListEventSingle = getSelectListEventSingleCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EventBean.class, "lastOldEvent", constantNull())
                .declareVar(EventBean.class, "lastNewEvent", constantNull())
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            if (forge.isSelectRStream()) {
                forEach.ifCondition(equalsNull(ref("lastOldEvent")))
                        .assignRef("lastOldEvent", localMethod(getSelectListEventSingle, constantFalse(), REF_ISSYNTHESIZE))
                        .blockEnd();
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")), cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")), ref("eventsPerStream"));
            forEach.assignRef("lastNewEvent", localMethod(getSelectListEventSingle, constantTrue(), REF_ISSYNTHESIZE));
        }

        {
            CodegenBlock ifEmpty = method.getBlock().ifCondition(exprDotMethod(REF_VIEWEVENTSLIST, "isEmpty"));
            if (forge.isSelectRStream()) {
                ifEmpty.assignRef("lastOldEvent", localMethod(getSelectListEventSingle, constantFalse(), REF_ISSYNTHESIZE))
                        .assignRef("lastNewEvent", ref("lastOldEvent"));
            } else {
                ifEmpty.assignRef("lastNewEvent", localMethod(getSelectListEventSingle, constantTrue(), REF_ISSYNTHESIZE));
            }
        }

        method.getBlock()
                .declareVar(EventBean[].class, "lastNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastNewEvent")))
                .declareVar(EventBean[].class, "lastOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastOldEvent")))
                .ifCondition(and(equalsNull(ref("lastNew")), equalsNull(ref("lastOld"))))
                .blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("lastNew"), ref("lastOld")));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenMethod parent, CodegenInstanceAux instance) {
        CodegenMethod selectList = getSelectListEventsAsArrayCodegen(forge, classScope, instance);
        CodegenMethod method = parent.makeChild(Iterator.class, ResultSetProcessorRowForAllImpl.class, classScope);
        method.getBlock().declareVar(EventBean[].class, "events", localMethod(selectList, constantTrue(), constantTrue(), constantFalse()))
                .ifRefNull("events")
                .blockReturn(enumValue(CollectionUtil.class, "NULL_EVENT_ITERATOR"))
                .methodReturn(newInstance(SingleEventIterator.class, arrayAtIndex(ref("events"), constant(0))));
        return method;
    }

    private static CodegenMethod getSelectListEventSingleCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            if (forge.getOptionalHavingNode() != null) {
                method.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), constantNull(), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT)))
                        .blockReturn(constantNull());
            }
            method.getBlock().methodReturn(exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", enumValue(CollectionUtil.class, "EVENTBEANARRAY_EMPTY"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
        };
        return instance.getMethods().addMethod(EventBean.class, "getSelectListEventSingle", CodegenNamedParam.from(boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE), ResultSetProcessorRowForAllImpl.class, classScope, code);
    }

    private static CodegenMethod getSelectListEventsAddListCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            if (forge.getOptionalHavingNode() != null) {
                method.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), constantNull(), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT)))
                        .blockReturnNoValue();
            }
            method.getBlock().declareVar(EventBean.class, "theEvent", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", enumValue(CollectionUtil.class, "EVENTBEANARRAY_EMPTY"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT))
                    .expression(exprDotMethod(ref("resultEvents"), "add", ref("theEvent")));
        };
        return instance.getMethods().addMethod(void.class, "getSelectListEventsAddList", CodegenNamedParam.from(boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, List.class, "resultEvents"), ResultSetProcessorRowForAllImpl.class, classScope, code);
    }

    static CodegenMethod getSelectListEventsAsArrayCodegen(ResultSetProcessorRowForAllForge forge, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            if (forge.getOptionalHavingNode() != null) {
                method.getBlock().ifCondition(not(localMethod(instance.getMethods().getMethod("evaluateHavingClause"), constantNull(), REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT)))
                        .blockReturn(constantNull());
            }
            method.getBlock().declareVar(EventBean.class, "theEvent", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", enumValue(CollectionUtil.class, "EVENTBEANARRAY_EMPTY"), REF_ISNEWDATA, REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT))
                    .declareVar(EventBean[].class, "result", newArrayByLength(EventBean.class, constant(1)))
                    .assignArrayElement("result", constant(0), ref("theEvent"))
                    .methodReturn(ref("result"));
        };
        return instance.getMethods().addMethod(EventBean[].class, "getSelectListEventsAsArray", CodegenNamedParam.from(boolean.class, NAME_ISNEWDATA, boolean.class, NAME_ISSYNTHESIZE, boolean.class, "join"), ResultSetProcessorRowForAllImpl.class, classScope, code);
    }
}
