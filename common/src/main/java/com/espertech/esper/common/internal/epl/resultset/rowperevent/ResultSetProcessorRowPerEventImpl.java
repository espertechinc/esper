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
import com.espertech.esper.common.client.type.EPTypePremade;
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
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.Viewable;

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
        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, REF_NEWDATA, REF_OLDDATA, ref("eventsPerStream"));
    }

    public static void applyJoinResultCodegen(CodegenMethod method) {
        method.getBlock().staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, REF_NEWDATA, REF_OLDDATA);
    }

    public static void processJoinResultCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", constantNull())
                .declareVarNoInit(EventBean.EPTYPEARRAY, "selectNewEvents");

        if (forge.isUnidirectional()) {
            method.getBlock().exprDotMethod(ref("this"), "clear");
        }
        method.getBlock().staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, REF_NEWDATA, REF_OLDDATA);

        ResultSetProcessorUtil.processJoinResultCodegen(method, classScope, instance, forge.getOptionalHavingNode() != null, forge.isSelectRStream(), forge.isSorting(), true);
    }

    public static void processViewResultCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", constantNull())
                .declareVarNoInit(EventBean.EPTYPEARRAY, "selectNewEvents")
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, REF_NEWDATA, REF_OLDDATA, ref("eventsPerStream"));

        ResultSetProcessorUtil.processViewResultCodegen(method, classScope, instance, forge.getOptionalHavingNode() != null, forge.isSelectRStream(), forge.isSorting(), true);
    }

    public static void getIteratorViewCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method) {
        if (!forge.isHistoricalOnly()) {
            method.getBlock().methodReturn(localMethod(obtainIteratorCodegen(forge, classScope, method), REF_VIEWABLE));
            return;
        }

        method.getBlock()
                .staticMethod(ResultSetProcessorUtil.class, METHOD_CLEARANDAGGREGATEUNGROUPED, MEMBER_EXPREVALCONTEXT, MEMBER_AGGREGATIONSVC, REF_VIEWABLE)
                .declareVar(EPTypePremade.ITERATOR.getEPType(), "iterator", localMethod(obtainIteratorCodegen(forge, classScope, method), REF_VIEWABLE))
                .declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "deque", staticMethod(ResultSetProcessorUtil.class, METHOD_ITERATORTODEQUE, ref("iterator")))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_EXPREVALCONTEXT)
                .methodReturn(exprDotMethod(ref("deque"), "iterator"));
    }

    private static CodegenMethod obtainIteratorCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod parent) {
        CodegenMethod iterator = parent.makeChild(EPTypePremade.ITERATOR.getEPType(), ResultSetProcessorRowPerEventImpl.class, classScope).addParam(Viewable.EPTYPE, NAME_VIEWABLE);
        if (!forge.isSorting()) {
            iterator.getBlock().methodReturn(newInstance(ResultSetProcessorRowPerEventIterator.EPTYPE, exprDotMethod(REF_VIEWABLE, "iterator"), ref("this"), MEMBER_EXPREVALCONTEXT));
            return iterator;
        }

        iterator.getBlock().declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .declareVar(EPTypePremade.LIST.getEPType(), "outgoingEvents", newInstance(EPTypePremade.ARRAYLIST.getEPType()))
                .declareVar(EPTypePremade.LIST.getEPType(), "orderKeys", newInstance(EPTypePremade.ARRAYLIST.getEPType()));

        {
            CodegenBlock forEach = iterator.getBlock().forEach(EventBean.EPTYPE, "candidate", REF_VIEWABLE);
            forEach.assignArrayElement("eventsPerStream", constant(0), ref("candidate"));
            if (forge.getOptionalHavingNode() != null) {
                forEach.ifCondition(not(exprDotMethod(ref("this"), "evaluateHavingClause", ref("eventsPerStream"), constant(true), MEMBER_EXPREVALCONTEXT))).blockContinue();
            }
            forEach.exprDotMethod(ref("outgoingEvents"), "add", exprDotMethod(MEMBER_SELECTEXPRPROCESSOR, "process", ref("eventsPerStream"), constantTrue(), constantTrue(), MEMBER_EXPREVALCONTEXT))
                    .exprDotMethod(ref("orderKeys"), "add", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), MEMBER_EXPREVALCONTEXT));
        }

        iterator.getBlock().methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_ORDEROUTGOINGGETITERATOR, ref("outgoingEvents"), ref("orderKeys"), MEMBER_ORDERBYPROCESSOR, MEMBER_EXPREVALCONTEXT));
        return iterator;
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.getOptionalHavingNode() == null) {
            if (!forge.isSorting()) {
                method.getBlock().declareVar(EventBean.EPTYPEARRAY, "result", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, REF_JOINSET, constantTrue(), constantTrue(), MEMBER_EXPREVALCONTEXT));
            } else {
                method.getBlock().declareVar(EventBean.EPTYPEARRAY, "result", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTJOINEVENTSNOHAVINGWITHORDERBY, MEMBER_AGGREGATIONSVC, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_JOINSET, constantTrue(), constantTrue(), MEMBER_EXPREVALCONTEXT));
            }
        } else {
            if (!forge.isSorting()) {
                CodegenMethod select = ResultSetProcessorUtil.getSelectJoinEventsHavingCodegen(classScope, instance);
                method.getBlock().declareVar(EventBean.EPTYPEARRAY, "result", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_JOINSET, constantTrue(), constantTrue(), MEMBER_EXPREVALCONTEXT));
            } else {
                CodegenMethod select = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderByCodegen(classScope, instance);
                method.getBlock().declareVar(EventBean.EPTYPEARRAY, "result", localMethod(select, MEMBER_AGGREGATIONSVC, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_JOINSET, constantTrue(), constantTrue(), MEMBER_EXPREVALCONTEXT));
            }
        }
        method.getBlock().methodReturn(newInstance(ArrayEventIterator.EPTYPE, ref("result")));
    }

    public static void clearMethodCodegen(CodegenMethod method) {
        method.getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "clearResults", MEMBER_EXPREVALCONTEXT);
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
            instance.addMember(NAME_OUTPUTALLUNORDHELPER, ResultSetProcessorRowPerEventOutputAllHelper.EPTYPE);
            StateMgmtSetting stateMgmtSettings = forge.getOutputAllHelperSettings().get();
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLUNORDHELPER, exprDotMethod(factory, "makeRSRowPerEventOutputAll", ref("this"), MEMBER_EXPREVALCONTEXT, stateMgmtSettings.toExpression()));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLUNORDHELPER), methodName, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTUNORDHELPER, ResultSetProcessorRowPerEventOutputLastHelper.EPTYPE);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTUNORDHELPER, exprDotMethod(factory, "makeRSRowPerEventOutputLast", ref("this"), MEMBER_EXPREVALCONTEXT));
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
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")));
            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }
            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, REF_NEWDATA, REF_OLDDATA);

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                if (forge.getOptionalHavingNode() == null) {
                    if (!forge.isSorting()) {
                        forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_EXPREVALCONTEXT);
                    } else {
                        forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTJOINEVENTSNOHAVINGWITHORDERBY, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), MEMBER_EXPREVALCONTEXT);
                    }
                } else {
                    // generate old events using having then select
                    if (!forge.isSorting()) {
                        CodegenMethod select = ResultSetProcessorUtil.populateSelectJoinEventsHavingCodegen(classScope, instance);
                        forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_EXPREVALCONTEXT);
                    } else {
                        CodegenMethod select = ResultSetProcessorUtil.populateSelectJoinEventsHavingWithOrderByCodegen(classScope, instance);
                        forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), MEMBER_EXPREVALCONTEXT);
                    }
                }
            }

            // generate new events using select expressions
            if (forge.getOptionalHavingNode() == null) {
                if (!forge.isSorting()) {
                    forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), MEMBER_EXPREVALCONTEXT);
                } else {
                    forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTJOINEVENTSNOHAVINGWITHORDERBY, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_EXPREVALCONTEXT);
                }
            } else {
                if (!forge.isSorting()) {
                    CodegenMethod select = ResultSetProcessorUtil.populateSelectJoinEventsHavingCodegen(classScope, instance);
                    forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_NEWDATA, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), MEMBER_EXPREVALCONTEXT);
                } else {
                    CodegenMethod select = ResultSetProcessorUtil.populateSelectJoinEventsHavingWithOrderByCodegen(classScope, instance);
                    forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_EXPREVALCONTEXT);
                }
            }
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedJoinLastCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean.EPTYPE, "lastOldEvent", constantNull())
                .declareVar(EventBean.EPTYPE, "lastNewEvent", constantNull());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_JOINEVENTSSET);
            forEach.declareVar(EPTypePremade.SET.getEPType(), "newData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EPTypePremade.SET.getEPType(), "oldData", cast(EPTypePremade.SET.getEPType(), exprDotMethod(ref("pair"), "getSecond")));

            if (forge.isUnidirectional()) {
                forEach.exprDotMethod(ref("this"), "clear");
            }

            forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGJOINRESULT, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, ref("newData"), ref("oldData"));

            if (forge.isSelectRStream()) {
                if (forge.getOptionalHavingNode() == null) {
                    forEach.declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
                } else {
                    CodegenMethod select = ResultSetProcessorUtil.getSelectJoinEventsHavingCodegen(classScope, instance);
                    forEach.declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
                }
                forEach.ifCondition(and(notEqualsNull(ref("selectOldEvents")), relational(arrayLength(ref("selectOldEvents")), CodegenExpressionRelational.CodegenRelational.GT, constant(0))))
                        .assignRef("lastOldEvent", arrayAtIndex(ref("selectOldEvents"), op(arrayLength(ref("selectOldEvents")), "-", constant(1))))
                        .blockEnd();
            }

            // generate new events using select expressions
            if (forge.getOptionalHavingNode() == null) {
                forEach.declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTJOINEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
            } else {
                CodegenMethod select = ResultSetProcessorUtil.getSelectJoinEventsHavingCodegen(classScope, instance);
                forEach.declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
            }
            forEach.ifCondition(and(notEqualsNull(ref("selectNewEvents")), relational(arrayLength(ref("selectNewEvents")), CodegenExpressionRelational.CodegenRelational.GT, constant(0))))
                    .assignRef("lastNewEvent", arrayAtIndex(ref("selectNewEvents"), op(arrayLength(ref("selectNewEvents")), "-", constant(1))))
                    .blockEnd();
        }

        method.getBlock()
                .declareVar(EventBean.EPTYPEARRAY, "lastNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastNewEvent")))
                .declareVar(EventBean.EPTYPEARRAY, "lastOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastOldEvent")))
                .ifCondition(and(equalsNull(ref("lastNew")), equalsNull(ref("lastOld")))).blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.EPTYPE, ref("lastNew"), ref("lastOld")));
    }

    private static void processOutputLimitedViewDefaultCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorUtil.prefixCodegenNewOldEvents(method.getBlock(), forge.isSorting(), forge.isSelectRStream());

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)))
                    .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, REF_NEWDATA, REF_OLDDATA, ref("eventsPerStream"));

            // generate old events using select expressions
            if (forge.isSelectRStream()) {
                if (forge.getOptionalHavingNode() == null) {
                    if (!forge.isSorting()) {
                        forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_EXPREVALCONTEXT);
                    } else {
                        forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTEVENTSNOHAVINGWITHORDERBY, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_EXPREVALCONTEXT);
                    }
                } else {
                    // generate old events using having then select
                    if (!forge.isSorting()) {
                        CodegenMethod select = ResultSetProcessorUtil.populateSelectEventsHavingCodegen(classScope, instance);
                        forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), MEMBER_EXPREVALCONTEXT);
                    } else {
                        CodegenMethod select = ResultSetProcessorUtil.populateSelectEventsHavingWithOrderByCodegen(classScope, instance);
                        forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_OLDDATA, constantFalse(), REF_ISSYNTHESIZE, ref("oldEvents"), ref("oldEventsSortKey"), MEMBER_EXPREVALCONTEXT);
                        throw new UnsupportedOperationException();
                    }
                }
            }

            // generate new events using select expressions
            if (forge.getOptionalHavingNode() == null) {
                if (!forge.isSorting()) {
                    forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), MEMBER_EXPREVALCONTEXT);
                } else {
                    forEach.staticMethod(ResultSetProcessorUtil.class, METHOD_POPULATESELECTEVENTSNOHAVINGWITHORDERBY, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_EXPREVALCONTEXT);
                }
            } else {
                if (!forge.isSorting()) {
                    CodegenMethod select = ResultSetProcessorUtil.populateSelectEventsHavingCodegen(classScope, instance);
                    forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, REF_NEWDATA, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), MEMBER_EXPREVALCONTEXT);
                } else {
                    CodegenMethod select = ResultSetProcessorUtil.populateSelectEventsHavingWithOrderByCodegen(classScope, instance);
                    forEach.localMethod(select, MEMBER_SELECTEXPRPROCESSOR, MEMBER_ORDERBYPROCESSOR, REF_NEWDATA, constantTrue(), REF_ISSYNTHESIZE, ref("newEvents"), ref("newEventsSortKey"), MEMBER_EXPREVALCONTEXT);
                }
            }
        }

        ResultSetProcessorUtil.finalizeOutputMaySortMayRStreamCodegen(method.getBlock(), ref("newEvents"), ref("newEventsSortKey"), ref("oldEvents"), ref("oldEventsSortKey"), forge.isSelectRStream(), forge.isSorting());
    }

    private static void processOutputLimitedViewLastCodegen(ResultSetProcessorRowPerEventForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean.EPTYPE, "lastOldEvent", constantNull())
                .declareVar(EventBean.EPTYPE, "lastNewEvent", constantNull())
                .declareVar(EventBean.EPTYPEARRAY, "eventsPerStream", newArrayByLength(EventBean.EPTYPE, constant(1)));

        {
            CodegenBlock forEach = method.getBlock().forEach(UniformPair.EPTYPE, "pair", REF_VIEWEVENTSLIST);
            forEach.declareVar(EventBean.EPTYPEARRAY, "newData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getFirst")))
                    .declareVar(EventBean.EPTYPEARRAY, "oldData", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("pair"), "getSecond")))
                    .staticMethod(ResultSetProcessorUtil.class, METHOD_APPLYAGGVIEWRESULT, MEMBER_AGGREGATIONSVC, MEMBER_EXPREVALCONTEXT, ref("newData"), ref("oldData"), ref("eventsPerStream"));

            if (forge.isSelectRStream()) {
                if (forge.getOptionalHavingNode() == null) {
                    forEach.declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
                } else {
                    CodegenMethod select = ResultSetProcessorUtil.getSelectEventsHavingCodegen(classScope, instance);
                    forEach.declareVar(EventBean.EPTYPEARRAY, "selectOldEvents", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, ref("oldData"), constantFalse(), REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
                }
                forEach.ifCondition(and(notEqualsNull(ref("selectOldEvents")), relational(arrayLength(ref("selectOldEvents")), CodegenExpressionRelational.CodegenRelational.GT, constant(0))))
                        .assignRef("lastOldEvent", arrayAtIndex(ref("selectOldEvents"), op(arrayLength(ref("selectOldEvents")), "-", constant(1))))
                        .blockEnd();
            }

            // generate new events using select expressions
            if (forge.getOptionalHavingNode() == null) {
                forEach.declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
            } else {
                CodegenMethod select = ResultSetProcessorUtil.getSelectEventsHavingCodegen(classScope, instance);
                forEach.declareVar(EventBean.EPTYPEARRAY, "selectNewEvents", localMethod(select, MEMBER_SELECTEXPRPROCESSOR, ref("newData"), constantTrue(), REF_ISSYNTHESIZE, MEMBER_EXPREVALCONTEXT));
            }
            forEach.ifCondition(and(notEqualsNull(ref("selectNewEvents")), relational(arrayLength(ref("selectNewEvents")), CodegenExpressionRelational.CodegenRelational.GT, constant(0))))
                    .assignRef("lastNewEvent", arrayAtIndex(ref("selectNewEvents"), op(arrayLength(ref("selectNewEvents")), "-", constant(1))))
                    .blockEnd();
        }

        method.getBlock()
                .declareVar(EventBean.EPTYPEARRAY, "lastNew", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastNewEvent")))
                .declareVar(EventBean.EPTYPEARRAY, "lastOld", staticMethod(CollectionUtil.class, METHOD_TOARRAYMAYNULL, ref("lastOldEvent")))
                .ifCondition(and(equalsNull(ref("lastNew")), equalsNull(ref("lastOld")))).blockReturn(constantNull())
                .methodReturn(newInstance(UniformPair.EPTYPE, ref("lastNew"), ref("lastOld")));
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
