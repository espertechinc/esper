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
package com.espertech.esper.common.internal.epl.historical.common;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.SortedSet;
import java.util.TreeSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class HistoricalEventViewableForgeBase implements HistoricalEventViewableForge {
    protected final int streamNum;
    protected final EventType eventType;
    protected ExprForge[] inputParamEvaluators;
    protected final TreeSet<Integer> subordinateStreams = new TreeSet<>();
    protected int scheduleCallbackId = -1;

    public abstract Class typeOfImplementation();

    public abstract void codegenSetter(CodegenExpressionRef ref, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public HistoricalEventViewableForgeBase(int streamNum, EventType eventType) {
        this.streamNum = streamNum;
        this.eventType = eventType;
    }

    public SortedSet<Integer> getRequiredStreams() {
        return subordinateStreams;
    }

    public final CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(typeOfImplementation(), this.getClass(), classScope);
        CodegenExpressionRef ref = ref("hist");
        method.getBlock().declareVar(typeOfImplementation(), ref.getRef(), newInstance(typeOfImplementation()))
                .exprDotMethod(ref, "setStreamNumber", constant(streamNum))
                .exprDotMethod(ref, "setEventType", EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref, "setHasRequiredStreams", constant(!subordinateStreams.isEmpty()))
                .exprDotMethod(ref, "setScheduleCallbackId", constant(scheduleCallbackId))
                .exprDotMethod(ref, "setEvaluator", ExprNodeUtilityCodegen.codegenEvaluatorMayMultiKeyWCoerce(inputParamEvaluators, null, method, this.getClass(), classScope));
        codegenSetter(ref, method, symbols, classScope);
        method.getBlock()
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add("addReadyCallback", ref))
                .methodReturn(ref);
        return localMethod(method);
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }
}
