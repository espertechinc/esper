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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventPropertyGetterIndexedSPI;

import java.io.StringWriter;

public class PropertyDotNonLambdaIndexedForge implements ExprForge, ExprNodeRenderable {

    private final int streamId;
    private final EventPropertyGetterIndexedSPI indexedGetter;
    private final ExprForge paramForge;
    private final Class returnType;

    public PropertyDotNonLambdaIndexedForge(int streamId, EventPropertyGetterIndexedSPI indexedGetter, ExprForge paramForge, Class returnType) {
        this.streamId = streamId;
        this.indexedGetter = indexedGetter;
        this.paramForge = paramForge;
        this.returnType = returnType;
    }

    public ExprEvaluator getExprEvaluator() {
        return new PropertyDotNonLambdaIndexedForgeEval(this, paramForge.getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return PropertyDotNonLambdaIndexedForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public int getStreamId() {
        return streamId;
    }

    public EventPropertyGetterIndexedSPI getIndexedGetter() {
        return indexedGetter;
    }

    public ExprForge getParamForge() {
        return paramForge;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
