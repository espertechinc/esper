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
package com.espertech.esper.common.internal.epl.resultset.simple;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.collection.TransformEventIterator;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactoryField;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.epl.resultset.handthru.ResultSetProcessorHandtruTransform;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil.METHOD_GETSELECTEVENTSNOHAVING;
import static com.espertech.esper.common.internal.event.core.EventBeanUtility.METHOD_FLATTENBATCHJOIN;
import static com.espertech.esper.common.internal.event.core.EventBeanUtility.METHOD_FLATTENBATCHSTREAM;
import static com.espertech.esper.common.internal.util.CollectionUtil.METHOD_TOARRAYEVENTS;
import static com.espertech.esper.common.internal.util.CollectionUtil.METHOD_TOARRAYOBJECTS;

/**
 * Result set processor for the simplest case: no aggregation functions used in the select clause, and no group-by.
 * <p>
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 */
public class ResultSetProcessorSimpleImpl {
    private final static String NAME_OUTPUTALLHELPER = "outputAllHelper";
    private final static String NAME_OUTPUTLASTHELPER = "outputLastHelper";

    public static void processJoinResultCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", constantNull())
                .declareVarNoInit(EventBean[].class, "selectNewEvents");
        ResultSetProcessorUtil.processJoinResultCodegen(method, classScope, instance, forge.getOptionalHavingNode() != null, forge.isSelectRStream(), forge.isSorting(), false);
    }

    public static void processViewResultCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(EventBean[].class, "selectOldEvents", constantNull())
                .declareVarNoInit(EventBean[].class, "selectNewEvents");
        ResultSetProcessorUtil.processViewResultCodegen(method, classScope, instance, forge.getOptionalHavingNode() != null, forge.isSelectRStream(), forge.isSorting(), false);
    }

    public static void getIteratorViewCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!forge.isSorting()) {
            // Return an iterator that gives row-by-row a result
            method.getBlock().methodReturn(newInstance(TransformEventIterator.class, exprDotMethod(REF_VIEWABLE, "iterator"), newInstance(ResultSetProcessorHandtruTransform.class, ref("this"))));
            return;
        }

        // Pull all events, generate order keys
        method.getBlock().declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .declareVar(List.class, "events", newInstance(ArrayList.class))
                .declareVar(List.class, "orderKeys", newInstance(ArrayList.class))
                .declareVar(Iterator.class, "parentIterator", exprDotMethod(REF_VIEWABLE, "iterator"))
                .ifCondition(equalsNull(ref("parentIterator"))).blockReturn(publicConstValue(CollectionUtil.class, "NULL_EVENT_ITERATOR"));

        {
            CodegenBlock loop = method.getBlock().forEach(EventBean.class, "aParent", REF_VIEWABLE);
            loop.assignArrayElement("eventsPerStream", constant(0), ref("aParent"))
                    .declareVar(Object.class, "orderKey", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "getSortKey", ref("eventsPerStream"), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));

            if (forge.getOptionalHavingNode() == null) {
                loop.declareVar(EventBean[].class, "result", staticMethod(ResultSetProcessorUtil.class, METHOD_GETSELECTEVENTSNOHAVING, MEMBER_SELECTEXPRPROCESSOR, ref("eventsPerStream"), constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
            } else {
                CodegenMethod select = ResultSetProcessorUtil.getSelectEventsHavingCodegen(classScope, instance);
                loop.declareVar(EventBean[].class, "result", localMethod(select, MEMBER_SELECTEXPRNONMEMBER, ref("eventsPerStream"), constantTrue(), constantTrue(), MEMBER_AGENTINSTANCECONTEXT));
            }

            loop.ifCondition(and(notEqualsNull(ref("result")), not(equalsIdentity(arrayLength(ref("result")), constant(0)))))
                    .exprDotMethod(ref("events"), "add", arrayAtIndex(ref("result"), constant(0)))
                    .exprDotMethod(ref("orderKeys"), "add", ref("orderKey"));
        }

        method.getBlock().declareVar(EventBean[].class, "outgoingEvents", staticMethod(CollectionUtil.class, METHOD_TOARRAYEVENTS, ref("events")))
                .declareVar(Object[].class, "orderKeysArr", staticMethod(CollectionUtil.class, METHOD_TOARRAYOBJECTS, ref("orderKeys")))
                .declareVar(EventBean[].class, "orderedEvents", exprDotMethod(MEMBER_ORDERBYPROCESSOR, "sortWOrderKeys", ref("outgoingEvents"), ref("orderKeysArr"), MEMBER_AGENTINSTANCECONTEXT))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("orderedEvents")));
    }

    public static void getIteratorJoinCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        method.getBlock().declareVar(UniformPair.class, "result", exprDotMethod(ref("this"), "processJoinResult", REF_JOINSET, staticMethod(Collections.class, "emptySet"), constantTrue()))
                .ifRefNull("result")
                .blockReturn(staticMethod(Collections.class, "emptyIterator"))
                .methodReturn(newInstance(ArrayEventIterator.class, cast(EventBean[].class, exprDotMethod(ref("result"), "getFirst"))));
    }

    public static void processOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processView", classScope, method, instance);
    }

    private static void processOutputLimitedLastAllNonBufferedCodegen(ResultSetProcessorSimpleForge forge, String methodName, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        if (forge.isOutputAll()) {
            instance.addMember(NAME_OUTPUTALLHELPER, ResultSetProcessorSimpleOutputAllHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTALLHELPER, exprDotMethod(factory, "makeRSSimpleOutputAll", ref("this"), MEMBER_AGENTINSTANCECONTEXT, eventTypes));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), methodName, REF_NEWDATA, REF_OLDDATA);
        } else if (forge.isOutputLast()) {
            instance.addMember(NAME_OUTPUTLASTHELPER, ResultSetProcessorSimpleOutputLastHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_OUTPUTLASTHELPER, exprDotMethod(factory, "makeRSSimpleOutputLast", ref("this"), MEMBER_AGENTINSTANCECONTEXT, eventTypes));
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), methodName, REF_NEWDATA, REF_OLDDATA);
        }
    }

    public static void processOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        processOutputLimitedLastAllNonBufferedCodegen(forge, "processJoin", classScope, method, instance);
    }

    public static void continueOutputLimitedLastAllNonBufferedViewCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputView", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputView", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public static void continueOutputLimitedLastAllNonBufferedJoinCodegen(ResultSetProcessorSimpleForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (forge.isOutputAll()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTALLHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else if (forge.isOutputLast()) {
            method.getBlock().methodReturn(exprDotMethod(member(NAME_OUTPUTLASTHELPER), "outputJoin", REF_ISSYNTHESIZE));
        } else {
            method.getBlock().methodReturn(constantNull());
        }
    }

    public static void stopMethodCodegen(CodegenMethod method, CodegenInstanceAux instance) {
        if (instance.hasMember(NAME_OUTPUTLASTHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTLASTHELPER), "destroy");
        }
        if (instance.hasMember(NAME_OUTPUTALLHELPER)) {
            method.getBlock().exprDotMethod(member(NAME_OUTPUTALLHELPER), "destroy");
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

    public static void processOutputLimitedJoinCodegen(ResultSetProcessorSimpleForge forge, CodegenMethod method) {
        if (!forge.isOutputLast()) {
            method.getBlock().declareVar(UniformPair.class, "pair", staticMethod(EventBeanUtility.class, METHOD_FLATTENBATCHJOIN, REF_JOINEVENTSSET))
                    .methodReturn(exprDotMethod(ref("this"), "processJoinResult",
                            cast(Set.class, exprDotMethod(ref("pair"), "getFirst")), cast(Set.class, exprDotMethod(ref("pair"), "getSecond")), REF_ISSYNTHESIZE));
            return;
        }
        method.getBlock().methodThrowUnsupported();
    }

    public static void processOutputLimitedViewCodegen(ResultSetProcessorSimpleForge forge, CodegenMethod method) {
        if (!forge.isOutputLast()) {
            method.getBlock().declareVar(UniformPair.class, "pair", staticMethod(EventBeanUtility.class, METHOD_FLATTENBATCHSTREAM, REF_VIEWEVENTSLIST))
                    .methodReturn(exprDotMethod(ref("this"), "processViewResult",
                            cast(EventBean[].class, exprDotMethod(ref("pair"), "getFirst")), cast(EventBean[].class, exprDotMethod(ref("pair"), "getSecond")), REF_ISSYNTHESIZE));
            return;
        }
        method.getBlock().methodThrowUnsupported();
    }
}
