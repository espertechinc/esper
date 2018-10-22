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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;

import java.io.StringWriter;
import java.util.List;

/**
 * Represents the case-when-then-else control flow function is an expression tree.
 */
public class ExprLambdaGoesNode extends ExprNodeBase implements ExprForge, ExprEvaluator, ExprDeclaredOrLambdaNode {
    private List<String> goesToNames;

    public ExprLambdaGoesNode(List<String> goesToNames) {
        this.goesToNames = goesToNames;
    }

    public boolean validated() {
        return true;
    }

    public List<String> getGoesToNames() {
        return goesToNames;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        throw new UnsupportedOperationException();
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        throw new UnsupportedOperationException();
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.MINIMUM;
    }

    public ExprForge getForge() {
        return this;
    }

    public Class getEvaluationType() {
        return null;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }
}


