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
package com.espertech.esper.epl.expression.codegen;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.base.CodegenSymbolProvider;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprForgeCodegenSymbol implements CodegenSymbolProvider {
    private final boolean allowUnderlyingReferences;

    private int currentParamNum;
    private Map<Integer, Pair<CodegenExpressionRef, EventType>> underlyingStreamNums = Collections.emptyMap();
    private CodegenExpressionRef optionalEPSRef;
    private CodegenExpressionRef optionalIsNewDataRef;
    private CodegenExpressionRef optionalExprEvalCtxRef;

    public ExprForgeCodegenSymbol(boolean allowUnderlyingReferences) {
        this.allowUnderlyingReferences = allowUnderlyingReferences;
    }

    public boolean isAllowUnderlyingReferences() {
        return allowUnderlyingReferences;
    }

    public CodegenExpressionRef getAddEPS(CodegenMethodScope scope) {
        if (optionalEPSRef == null) {
            optionalEPSRef = ExprForgeCodegenNames.REF_EPS;
        }
        scope.addSymbol(optionalEPSRef);
        return optionalEPSRef;
    }

    public CodegenExpressionRef getAddIsNewData(CodegenMethodScope scope) {
        if (optionalIsNewDataRef == null) {
            optionalIsNewDataRef = ExprForgeCodegenNames.REF_ISNEWDATA;
        }
        scope.addSymbol(optionalIsNewDataRef);
        return optionalIsNewDataRef;
    }

    public CodegenExpressionRef getAddExprEvalCtx(CodegenMethodScope scope) {
        if (optionalExprEvalCtxRef == null) {
            optionalExprEvalCtxRef = ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
        }
        scope.addSymbol(optionalExprEvalCtxRef);
        return optionalExprEvalCtxRef;
    }

    public CodegenExpressionRef getAddRequiredUnderlying(CodegenMethodScope scope, int streamNum, EventType eventType) {
        if (underlyingStreamNums.isEmpty()) {
            underlyingStreamNums = new HashMap<>();
        }
        Pair<CodegenExpressionRef, EventType> existing = underlyingStreamNums.get(streamNum);
        if (existing != null) {
            scope.addSymbol(existing.getFirst());
            return existing.getFirst();
        }
        CodegenExpressionRef assigned = ref("u" + currentParamNum);
        underlyingStreamNums.put(streamNum, new Pair<>(assigned, eventType));
        currentParamNum++;
        scope.addSymbol(assigned);
        return assigned;
    }

    public void provide(Map<String, Class> symbols) {
        if (optionalEPSRef != null) {
            symbols.put(optionalEPSRef.getRef(), EventBean[].class);
        }
        if (optionalExprEvalCtxRef != null) {
            symbols.put(optionalExprEvalCtxRef.getRef(), ExprEvaluatorContext.class);
        }
        if (optionalIsNewDataRef != null) {
            symbols.put(optionalIsNewDataRef.getRef(), boolean.class);
        }
        if (allowUnderlyingReferences) {
            for (Map.Entry<Integer, Pair<CodegenExpressionRef, EventType>> entry : underlyingStreamNums.entrySet()) {
                symbols.put(entry.getValue().getFirst().getRef(), entry.getValue().getSecond().getUnderlyingType());
            }
        }
    }

    public void derivedSymbolsCodegen(CodegenMethodNode parent, CodegenBlock processBlock) {
        for (Map.Entry<Integer, Pair<CodegenExpressionRef, EventType>> underlying : underlyingStreamNums.entrySet()) {
            Class underlyingType = underlying.getValue().getSecond().getUnderlyingType();
            CodegenMethodNode methodNode = parent.makeChild(underlyingType, ExprForgeCodegenSymbol.class).addParam(EventBean[].class, ExprForgeCodegenNames.NAME_EPS);
            methodNode.getBlock()
                    .declareVar(EventBean.class, "event", arrayAtIndex(ref(ExprForgeCodegenNames.NAME_EPS), constant(underlying.getKey())))
                    .ifRefNullReturnNull("event")
                    .methodReturn(cast(underlyingType, exprDotUnderlying(ref("event"))));
            processBlock.declareVar(underlyingType, underlying.getValue().getFirst().getRef(), localMethod(methodNode, ExprForgeCodegenNames.REF_EPS));
        }
    }
}
