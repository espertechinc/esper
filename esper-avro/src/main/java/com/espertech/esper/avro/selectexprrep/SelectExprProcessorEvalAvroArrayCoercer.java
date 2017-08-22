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
package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.TypeWidener;

import java.io.StringWriter;

public class SelectExprProcessorEvalAvroArrayCoercer implements ExprEvaluator, ExprForge, ExprNodeRenderable {
    private final ExprForge forge;
    private final TypeWidener widener;
    private final Class resultType;
    private ExprEvaluator eval;

    public SelectExprProcessorEvalAvroArrayCoercer(ExprForge forge, TypeWidener widener, Class resultType) {
        this.forge = forge;
        this.widener = widener;
        this.resultType = resultType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = eval.evaluate(eventsPerStream, isNewData, context);
        return widener.widen(result);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return widener.widenCodegen(forge.evaluateCodegen(requiredType, codegenMethodScope, exprSymbol, codegenClassScope), codegenMethodScope, codegenClassScope);
    }

    public ExprEvaluator getExprEvaluator() {
        this.eval = forge.getExprEvaluator();
        return this;
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
