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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;

import java.io.StringWriter;

/**
 * Represents an expression node that returns the predefined type and
 * that cannot be evaluated.
 */
public class ExprTypedNoEvalNode extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static final long serialVersionUID = -6120042141834089857L;

    private final String returnTypeName;
    private final Class returnType;

    public ExprTypedNoEvalNode(String returnTypeName, Class returnType) {
        this.returnTypeName = returnTypeName;
        this.returnType = returnType;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(returnTypeName);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw new EPException(this.getClass().getSimpleName() + " cannot be evaluated");
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        throw new IllegalStateException("Typed-no-eval-expression does not allow code generation");
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.NONE;
    }
}
