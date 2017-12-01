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
package com.espertech.esper.epl.expression.table;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.strategy.ExprTableExprEvaluatorBase;

import java.io.StringWriter;

public class ExprTableIdentNode extends ExprNodeBase implements ExprForge {

    private static final long serialVersionUID = -7470838423411921276L;

    private final String streamOrPropertyName;
    private final String unresolvedPropertyName;
    private transient ExprTableExprEvaluatorBase eval;

    public ExprTableIdentNode(String streamOrPropertyName, String unresolvedPropertyName) {
        this.streamOrPropertyName = streamOrPropertyName;
        this.unresolvedPropertyName = unresolvedPropertyName;
    }

    public void setEval(ExprTableExprEvaluatorBase eval) {
        this.eval = eval;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        ExprIdentNodeImpl.toPrecedenceFreeEPL(writer, streamOrPropertyName, unresolvedPropertyName);
    }

    public ExprEvaluator getExprEvaluator() {
        return eval;
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, eval, eval.getReturnType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    public Class getEvaluationType() {
        return eval.getReturnType();
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }
}
