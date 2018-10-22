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
package com.espertech.esper.common.internal.avro.selectexprrep;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.io.StringWriter;

public class SelectExprProcessorEvalAvroArrayCoercer implements ExprEvaluator, ExprForge, ExprNodeRenderable {
    private final ExprForge forge;
    private final TypeWidenerSPI widener;
    private final Class resultType;
    private ExprEvaluator eval;

    public SelectExprProcessorEvalAvroArrayCoercer(ExprForge forge, TypeWidenerSPI widener, Class resultType) {
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

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
