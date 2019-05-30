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
package com.espertech.esper.common.internal.epl.resultset.rowperevent;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactoryField;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil.*;
import static com.espertech.esper.common.internal.util.CollectionUtil.METHOD_TOARRAYMAYNULL;

/**
 * Result set processor for the case: aggregation functions used in the select clause, and no group-by,
 * and not all of the properties in the select clause are under an aggregation function.
 * <p>
 * This processor does not perform grouping, every event entering and leaving is in the same group.
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 * Aggregation state is simply one row holding all the state.
 */
public class ResultSetProcessorRowPerEventImpl {
    private final static String NAME_OUTPUTALLUNORDHELPER = "outputAllUnordHelper";
    private final static String NAME_OUTPUTLASTUNORDHELPER = "outputLastUnordHelper";

    public static void applyViewResultCodegen(CodegenMethod method) {
        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA, ref("eventsPerStream"));
    }

    public static void applyJoinResultCodegen(CodegenMethod method) {
        method.getBlock().staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA);
    }

    public static void processJoinResultCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", constantNull())
                .declareVarNoInit(EventBean[].class, "selectNewEvents");

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }
        method.getBlock().staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA);

        ResultSetProcessorUtil.processJoinResultCodegen(method, classScope, instance, forge.getOptionalHavingNode() != null, forge.isSelectRStream(), forge.isSorting(), true);
    }

    public static void processViewResultCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", constantNull())
                .declareVarNoInit(EventBean[].class, "selectNewEvents")
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA, ref("eventsPerStream"));

        ResultSetProcessorUtil.processViewResultCodegen(method, classScope, instance, forge.getOptionalHavingNode() != null, forge.isSelectRStream(), forge.isSorting(), true);
    }

    public static void getIteratorViewCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, classScope, method), REF_VIEWABLE));
            return;
        }

        method.getBlock()
                .staticMethod(ResultSetProcessorUtil.class, METHOD_CLEARANDAGGREGATEUNGROUPED, MEMBER_AGENTINSTANCECONTEXT, MEMBER_AGGREGATIONSVC, REF_VIEWABLE)
                .declareVar(Iterator.class, "iterator", localMethod(obtainIteratorCodegen(forge, classScope, method), REF_VIEWABLE))
                .declareVar(ArrayDeque.class, "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, ref("iterator")))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod parent) {
        CodegenMethod iterator = parent.makeChild(Iterator.class, ResultSetProcessorRowPerEventImpl.class, classScope).addParam(Viewable.class, NAME_VIEWABLE);
        if (!forge.isSorting()) {
            iterator.getBlock().methodReturn(newInstance(ResultSetProcessorRowPerEventIterator.class, exprDotMethod(REF_VIEWABLE, "iterator"), ref("this"), MEMBER_AGENTINSTANCECONTEXT));
            return iterator;
        }

        iterator.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(List.class, "outgoingEvents", newInstance(ArrayList.class))
                .declareVar(List.class, "orderKeys", newInstance(ArrayList.class));

        {
            CodegenBlock forEach = iterator.getBlock().forEach(EventBean.class, "candidate", REF_VIEWABLE);
            forEach.assignArrayElement("eventsPerStream", constant(0), ref("candidate"));
            if (forge.getOptionalHavingNode() != null) {
                forEach.ifCondition(not(exprDotMethod(ref("this"), "evaluateHavingClause", ref("eventsPerStream"), constant(true), MEMBER_AGENTINSTANCECONTEXT))).blockContinue();
            }
            forEach.exprDotMethod(ref("outgoingEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT))
                    .exprDotMethod(ref("orderKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
        }

        iterator.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_ORDEROUTGOINGGETITERATOR, ref("outgoingEvents"), ref("orderKeys"), MEMBER_ORDERBYPROCESSOR, MEMBER_AGENTINSTANCECONTEXT));
        return iterator;
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.getOptionalHavingNode() == null) {
            if (!forge.isSorting()) {
                method.getBlock().declareVar(EventBean[].class, "result", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, REF_JOINSET, constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
            } else {
                method.getBlock().declareVar(EventBean[].class, "result", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTJOINEVENTSNOHAVINGWITHORDERBY, MEMBER_AGGREGATIONSVC, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_JOINSET, constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
            }
        } else {
            if (!forge.isSorting()) {
                CodegenMethod select = ResultSetProcessorUtil.getSelectJoinEventsHavingCodegen(classScope, instance);
                method.getBlock().declareVar(EventBean[].class, "result", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_JOINSET, constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
            } else {
                CodegenMethod select = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderByCodegen(classScope, instance);
                method.getBlock().declareVar(EventBean[].class, "result", localMethod(select, MEMBER_AGGREGATIONSVC, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_JOINSET, constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
            }
        }
        method.getBlock().methodReturn(newInstance(ArrayEventIterator.class, ref("result")));
    }

    public static void clearMethodCodegen(CodegenMethod method) {
        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_AGENTINSTANCECONTEXT);
    }

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.isOutputLast()) {
            processOutputLimitedJoinLastCodegen(forge, classScope, method, instance);
        } else {
            processOutputLimitedJoinDefaultCodegen(forge, classScope, method, instance);
        }
    }

    public static void processOutputLimitedViewCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.isOutputLast()) {
            processOutputLimitedViewLastCodegen(forge, classScope, method, instance);
        } else {
            processOutputLimitedViewDefaultCodegen(forge, classScope, method, instance);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorRowPerEventForge forge, String methodName, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);

        if (forge.isOutputAll()) {
            instance.addMember(NAME_OUTPUTALLUNORDHELPER, ResultSetProcessorRowPerEventOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLUNORDHELPER, exprDotMethod(factory, "makeRSRowPerEventOutputAll", ref("this"), MEMBER_AGENTINSTANCECONTEXT));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLUNORDHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTUNORDHELPER, ResultSetProcessorRowPerEventOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTUNORDHELPER, exprDotMethod(factory, "makeRSRowPerEventOutputLast", ref("this"), MEMBER_AGENTINSTANCECONTEXT));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTUNORDHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        }
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorRowPerEventForge forge, CodegenMethod method) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLUNORDHELPER), "output"));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTUNORDHELPER), "output"));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorRowPerEventForge forge, CodegenMethod method) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLUNORDHELPER), "output"));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTUNORDHELPER), "output"));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    static void stopCodegen(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTUNORDHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTUNORDHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLUNORDHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLUNORDHELPER), "destroy");
        }
    }

    private static void processOutputLimitedJoinDefaultCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));
            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA);

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                if (forge.getOptionalHavingNode() == null) {
                    if (!forge.isSorting()) {
                        forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_AGENTINSTANCECONTEXT);
                    } else {
                        forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTJOINEVENTSNOHAVINGWITHORDERBY, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                } else {
                    // generate old events using having then select
                    if (!forge.isSorting()) {
                        CodegenMethod select = ResultSetProcessorUtil.populateSelectJoinEventsHavingCodegen(classScope, instance);
                        forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_AGENTINSTANCECONTEXT);
                    } else {
                        CodegenMethod select = ResultSetProcessorUtil.populateSelectJoinEventsHavingWithOrderByCodegen(classScope, instance);
                        forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                }
            }

            // generate new events using select expressions
            if (forge.getOptionalHavingNode() == null) {
                if (!forge.isSorting()) {
                    forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), MEMBER_AGENTINSTANCECONTEXT);
                } else {
                    forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTJOINEVENTSNOHAVINGWITHORDERBY, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT);
                }
            } else {
                if (!forge.isSorting()) {
                    CodegenMethod select = ResultSetProcessorUtil.populateSelectJoinEventsHavingCodegen(classScope, instance);
                    forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_NEWDATA, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), MEMBER_AGENTINSTANCECONTEXT);
                } else {
                    CodegenMethod select = ResultSetProcessorUtil.populateSelectJoinEventsHavingWithOrderByCodegen(classScope, instance);
                    forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedJoinLastCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean.class, "lastOldEvent", constantNull())
                .declareVar(EventBean.class, "lastNewEvent", constantNull());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(Set.class, "newData", cast(Set.class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(Set.class, "oldData", cast(Set.class, exprDotMethod(ref("pair"), "getSecond")));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("oldData"));

            if (forge.isSelectRStream()) {
                if (forge.getOptionalHavingNode() == null) {
                    forEach.declareVar(EventBean[].class, "selectOldEvents", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
                } else {
                    CodegenMethod select = ResultSetProcessorUtil.getSelectJoinEventsHavingCodegen(classScope, instance);
                    forEach.declareVar(EventBean[].class, "selectOldEvents", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
                }
                forEach.ifCondition(and(notEqualsNull(ref("selectOldEvents")), relational(arrayLength(ref("selectOldEvents")), CodegenExpressionRelational.CodegenRelational.GT, constant(0))))
                        .assignRef("lastOldEvent", arrayAtIndex(ref("selectOldEvents"), op(arrayLength(ref("selectOldEvents")), "-", constant(1))))
                        .blockEnd();
            }

            // generate new events using select expressions
            if (forge.getOptionalHavingNode() == null) {
                forEach.declareVar(EventBean[].class, "selectNewEvents", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
            } else {
                CodegenMethod select = ResultSetProcessorUtil.getSelectJoinEventsHavingCodegen(classScope, instance);
                forEach.declareVar(EventBean[].class, "selectNewEvents", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
            }
            forEach.ifCondition(and(notEqualsNull(ref("selectNewEvents")), relational(arrayLength(ref("selectNewEvents")), CodegenExpressionRelational.CodegenRelational.GT, constant(0))))
                    .assignRef("lastNewEvent", arrayAtIndex(ref("selectNewEvents"), op(arrayLength(ref("selectNewEvents")), "-", constant(1))))
                    .blockEnd();
        }

        method.getBlock()
                .declareVar(EventBean[].class, "lastNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastNewEvent")))
                .declareVar(EventBean[].class, "lastOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastOldEvent")))
                .ifCondition(and(equalsNull(ref("lastNew")), equalsNull(ref("lastOld")))).blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("lastNew"), ref("lastOld")));
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                    .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, REF_OLDDATA, ref("eventsPerStream"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                if (forge.getOptionalHavingNode() == null) {
                    if (!forge.isSorting()) {
                        forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_AGENTINSTANCECONTEXT);
                    } else {
                        forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTEVENTSNOHAVINGWITHORDERBY, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_AGENTINSTANCECONTEXT);
                    }
                } else {
                    // generate old events using having then select
                    if (!forge.isSorting()) {
                        CodegenMethod select = ResultSetProcessorUtil.populateSelectEventsHavingCodegen(classScope, instance);
                        forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_AGENTINSTANCECONTEXT);
                    } else {
                        CodegenMethod select = ResultSetProcessorUtil.populateSelectEventsHavingWithOrderByCodegen(classScope, instance);
                        forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT);
                        throw new UnsupportedOperationException();
                    }
                }
            }

            // generate new events using select expressions
            if (forge.getOptionalHavingNode() == null) {
                if (!forge.isSorting()) {
                    forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), MEMBER_AGENTINSTANCECONTEXT);
                } else {
                    forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTEVENTSNOHAVINGWITHORDERBY, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT);
                }
            } else {
                if (!forge.isSorting()) {
                    CodegenMethod select = ResultSetProcessorUtil.populateSelectEventsHavingCodegen(classScope, instance);
                    forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_NEWDATA, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), MEMBER_AGENTINSTANCECONTEXT);
                } else {
                    CodegenMethod select = ResultSetProcessorUtil.populateSelectEventsHavingWithOrderByCodegen(classScope, instance);
                    forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_NEWDATA, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_AGENTINSTANCECONTEXT);
                }
            }
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean.class, "lastOldEvent", constantNull())
                .declareVar(EventBean.class, "lastNewEvent", constantNull())
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.class, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean[].class, "newData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean[].class, "oldData", cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")))
                    .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, ref("newData"), ref("oldData"), ref("eventsPerStream"));

            if (forge.isSelectRStream()) {
                if (forge.getOptionalHavingNode() == null) {
                    forEach.declareVar(EventBean[].class, "selectOldEvents", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
                } else {
                    CodegenMethod select = ResultSetProcessorUtil.getSelectEventsHavingCodegen(classScope, instance);
                    forEach.declareVar(EventBean[].class, "selectOldEvents", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
                }
                forEach.ifCondition(and(notEqualsNull(ref("selectOldEvents")), relational(arrayLength(ref("selectOldEvents")), CodegenExpressionRelational.CodegenRelational.GT, constant(0))))
                        .assignRef("lastOldEvent", arrayAtIndex(ref("selectOldEvents"), op(arrayLength(ref("selectOldEvents")), "-", constant(1))))
                        .blockEnd();
            }

            // generate new events using select expressions
            if (forge.getOptionalHavingNode() == null) {
                forEach.declareVar(EventBean[].class, "selectNewEvents", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
            } else {
                CodegenMethod select = ResultSetProcessorUtil.getSelectEventsHavingCodegen(classScope, instance);
                forEach.declareVar(EventBean[].class, "selectNewEvents", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT));
            }
            forEach.ifCondition(and(notEqualsNull(ref("selectNewEvents")), relational(arrayLength(ref("selectNewEvents")), CodegenExpressionRelational.CodegenRelational.GT, constant(0))))
                    .assignRef("lastNewEvent", arrayAtIndex(ref("selectNewEvents"), op(arrayLength(ref("selectNewEvents")), "-", constant(1))))
                    .blockEnd();
        }

        method.getBlock()
                .declareVar(EventBean[].class, "lastNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastNewEvent")))
                .declareVar(EventBean[].class, "lastOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastOldEvent")))
                .ifCondition(and(equalsNull(ref("lastNew")), equalsNull(ref("lastOld")))).blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.class, ref("lastNew"), ref("lastOld")));
    }

    public static void acceptHelperVisitorCodegen(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTUNORDHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTLASTUNORDHELPER));
        }
        if (instance.hasMember(NAME_OUTPUTALLUNORDHELPER)) {
            method.getBlock().exprDotMethod(REF_RESULTSETVISITOR, "visit", member(NAME_OUTPUTALLUNORDHELPER));
        }
    }
}
