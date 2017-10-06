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
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprForgeCodegenSymbol implements CodegenSymbolProvider {
    private final boolean allowUnderlyingReferences;
    private final Boolean newDataValue;

    private int currentParamNum;
    private Map<Integer, EventTypeWithOptionalFlag> underlyingStreamNums = Collections.emptyMap();
    private CodegenExpressionRef optionalEPSRef;
    private CodegenExpressionRef optionalIsNewDataRef;
    private CodegenExpressionRef optionalExprEvalCtxRef;

    public ExprForgeCodegenSymbol(boolean allowUnderlyingReferences, Boolean newDataValue) {
        this.allowUnderlyingReferences = allowUnderlyingReferences;
        this.newDataValue = newDataValue;
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

    public CodegenExpression getAddIsNewData(CodegenMethodScope scope) {
        if (newDataValue != null) {  // new-data can be a const
            return constant(newDataValue);
        }

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

    public CodegenExpressionRef getAddRequiredUnderlying(CodegenMethodScope scope, int streamNum, EventType eventType, boolean optionalEvent) {
        if (underlyingStreamNums.isEmpty()) {
            underlyingStreamNums = new HashMap<>();
        }
        EventTypeWithOptionalFlag existing = underlyingStreamNums.get(streamNum);
        if (existing != null) {
            scope.addSymbol(existing.getRef());
            return existing.getRef();
        }
        CodegenExpressionRef assigned = ref("u" + currentParamNum);
        underlyingStreamNums.put(streamNum, new EventTypeWithOptionalFlag(assigned, eventType, optionalEvent));
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
            for (Map.Entry<Integer, EventTypeWithOptionalFlag> entry : underlyingStreamNums.entrySet()) {
                symbols.put(entry.getValue().getRef().getRef(), entry.getValue().getEventType().getUnderlyingType());
            }
        }
    }

    public void derivedSymbolsCodegen(CodegenMethodNode parent, CodegenBlock processBlock, CodegenClassScope codegenClassScope) {
        for (Map.Entry<Integer, EventTypeWithOptionalFlag> underlying : underlyingStreamNums.entrySet()) {
            Class underlyingType = underlying.getValue().getEventType().getUnderlyingType();
            String name = underlying.getValue().getRef().getRef();
            CodegenExpression arrayAtIndex = arrayAtIndex(ref(ExprForgeCodegenNames.NAME_EPS), constant(underlying.getKey()));

            if (!underlying.getValue().isOptionalEvent()) {
                processBlock.declareVar(underlyingType, name, cast(underlyingType, exprDotUnderlying(arrayAtIndex)));
            } else {
                CodegenMethodNode methodNode = parent.makeChild(underlyingType, ExprForgeCodegenSymbol.class, codegenClassScope).addParam(EventBean[].class, ExprForgeCodegenNames.NAME_EPS);
                methodNode.getBlock()
                        .declareVar(EventBean.class, "event", arrayAtIndex)
                        .ifRefNullReturnNull("event")
                        .methodReturn(cast(underlyingType, exprDotUnderlying(ref("event"))));
                processBlock.declareVar(underlyingType, name, localMethod(methodNode, ExprForgeCodegenNames.REF_EPS));
            }
        }
    }
}
