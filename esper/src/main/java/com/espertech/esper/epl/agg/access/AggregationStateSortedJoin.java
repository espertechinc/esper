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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.RefCountedSetAtomicInteger;
import com.espertech.esper.epl.agg.aggregator.AggregatorCodegenUtil;
import com.espertech.esper.epl.agg.factory.AggregationStateSortedForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Implementation of access function for single-stream (not joins).
 */
public class AggregationStateSortedJoin extends AggregationStateSortedImpl {
    protected final RefCountedSetAtomicInteger refs;

    public AggregationStateSortedJoin(AggregationStateSortedSpec spec) {
        super(spec);
        refs = new RefCountedSetAtomicInteger();
    }

    protected boolean referenceEvent(EventBean theEvent) {
        return refs.add(theEvent);
    }

    protected boolean dereferenceEvent(EventBean theEvent) {
        return refs.remove(theEvent);
    }

    @Override
    public void clear() {
        super.clear();
        refs.clear();
    }

    public static void rowMemberCodegenJoin(AggregationStateSortedForge forge, int stateNumber, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, CodegenClassScope classScope) {
        AggregationStateSortedImpl.rowMemberCodegen(forge, stateNumber, ctor, membersColumnized, classScope);
        membersColumnized.addMember(stateNumber, RefCountedSetAtomicInteger.class, "refs");
        ctor.getBlock().assignRef(refCol("refs", stateNumber), newInstance(RefCountedSetAtomicInteger.class));
    }

    public static void applyEnterCodegen(AggregationStateSortedForge forge, int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
        if (forge.getExpr().getOptionalFilter() != null) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forge.getExpr().getOptionalFilter().getForge(), method, symbols, classScope);
        }
        CodegenExpressionRef refs = refCol("refs", stateNumber);
        CodegenExpressionRef eps = symbols.getAddEPS(method);
        CodegenExpressionRef ctx = symbols.getAddExprEvalCtx(method);
        CodegenMethodNode referenceAdd = referenceAddToCollCodegen(forge, stateNumber, method, namedMethods, classScope);
        method.getBlock().declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getSpec().getStreamId())))
                .ifRefNull("theEvent").blockReturnNoValue()
                .ifCondition(exprDotMethod(refs, "add", ref("theEvent")))
                .localMethod(referenceAdd, ref("theEvent"), eps, ctx);
    }

    public static void applyLeaveCodegen(AggregationStateSortedForge forge, int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenNamedMethods namedMethods, CodegenClassScope classScope) {
        if (forge.getExpr().getOptionalFilter() != null) {
            AggregatorCodegenUtil.prefixWithFilterCheck(forge.getExpr().getOptionalFilter().getForge(), method, symbols, classScope);
        }
        CodegenExpressionRef refs = refCol("refs", stateNumber);
        CodegenExpressionRef eps = symbols.getAddEPS(method);
        CodegenExpressionRef ctx = symbols.getAddExprEvalCtx(method);
        CodegenMethodNode dereferenceRemove = dereferenceRemoveFromCollCodegen(forge, stateNumber, method, namedMethods, classScope);
        method.getBlock().declareVar(EventBean.class, "theEvent", arrayAtIndex(eps, constant(forge.getSpec().getStreamId())))
                .ifRefNull("theEvent").blockReturnNoValue()
                .ifCondition(exprDotMethod(refs, "remove", ref("theEvent")))
                .localMethod(dereferenceRemove, ref("theEvent"), eps, ctx);
    }

    public static void clearCodegen(int stateNumber, CodegenMethodNode method) {
        AggregationStateSortedImpl.clearCodegen(stateNumber, method);
        method.getBlock().exprDotMethod(refCol("refs", stateNumber), "clear");
    }
}
