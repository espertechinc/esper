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
package com.espertech.esper.common.internal.epl.resultset.handthru;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.collection.TransformEventIterator;
import com.espertech.esper.common.internal.collection.UniformPair;

import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.handthru.ResultSetProcessorHandThroughUtil.METHOD_GETSELECTEVENTSNOHAVINGHANDTHRUJOIN;
import static com.espertech.esper.common.internal.epl.resultset.handthru.ResultSetProcessorHandThroughUtil.METHOD_GETSELECTEVENTSNOHAVINGHANDTHRUVIEW;

/**
 * Result set processor for the hand-through case:
 * no aggregation functions used in the select clause, and no group-by, no having and ordering.
 */
public class ResultSetProcessorHandThrough {

    static void processJoinResultCodegen(ResultSetProcessorHandThroughFactoryForge prototype, CodegenMethod method) {
        CodegenExpression oldEvents = constantNull();
        if (prototype.isSelectRStream()) {
            oldEvents = staticMethod(ResultSetProcessorHandThroughUtil.class, METHOD_GETSELECTEVENTSNOHAVINGHANDTHRUJOIN, MEMBER_SELECTEXPRPROCESSOR, REF_OLDDATA, constant(false), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT);
        }
        CodegenExpression newEvents = staticMethod(ResultSetProcessorHandThroughUtil.class, METHOD_GETSELECTEVENTSNOHAVINGHANDTHRUJOIN, MEMBER_SELECTEXPRPROCESSOR, REF_NEWDATA, constant(true), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT);

        method.getBlock()
                .declareVar(EventBean[].class, "selectOldEvents", oldEvents)
                .declareVar(EventBean[].class, "selectNewEvents", newEvents)
                .methodReturn(newInstance(UniformPair.class, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    static void processViewResultCodegen(ResultSetProcessorHandThroughFactoryForge prototype, CodegenMethod method) {
        CodegenExpression oldEvents = constantNull();
        if (prototype.isSelectRStream()) {
            oldEvents = staticMethod(ResultSetProcessorHandThroughUtil.class, METHOD_GETSELECTEVENTSNOHAVINGHANDTHRUVIEW, MEMBER_SELECTEXPRPROCESSOR, REF_OLDDATA, constant(false), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT);
        }
        CodegenExpression newEvents = staticMethod(ResultSetProcessorHandThroughUtil.class, METHOD_GETSELECTEVENTSNOHAVINGHANDTHRUVIEW, MEMBER_SELECTEXPRPROCESSOR, REF_NEWDATA, constant(true), REF_ISSYNTHESIZE, MEMBER_AGENTINSTANCECONTEXT);

        method.getBlock()
                .declareVar(EventBean[].class, "selectOldEvents", oldEvents)
                .declareVar(EventBean[].class, "selectNewEvents", newEvents)
                .methodReturn(newInstance(UniformPair.class, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    static void getIteratorViewCodegen(CodegenMethod methodNode) {
        methodNode.getBlock().methodReturn(newInstance(TransformEventIterator.class, exprDotMethod(REF_VIEWABLE, "iterator"), newInstance(ResultSetProcessorHandtruTransform.class, ref("this"))));
    }

    static void getIteratorJoinCodegen(CodegenMethod method) {
        method.getBlock()
                .declareVar(UniformPair.class, EventBean[].class, "result", exprDotMethod(ref("this"), "processJoinResult", REF_JOINSET, staticMethod(Collections.class, "emptySet"), constant(true)))
                .methodReturn(newInstance(ArrayEventIterator.class, cast(EventBean[].class, exprDotMethod(ref("result"), "getFirst"))));
    }
}
