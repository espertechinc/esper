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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents an OR expression in a filter expression tree.
 */
public class ExprOrNode extends ExprNodeBase implements ExprForge {
    private static final long serialVersionUID = -1079540621551505814L;

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprOrNodeEval(this, ExprNodeUtilityCore.getEvaluatorsNoCompile(this.getChildNodes()));
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprOrNodeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Sub-nodes must be returning boolean
        for (ExprNode child : getChildNodes()) {
            Class childType = child.getForge().getEvaluationType();
            if (!JavaClassHelper.isBoolean(childType)) {
                throw new ExprValidationException("Incorrect use of OR clause, sub-expressions do not return boolean");
            }
        }

        if (this.getChildNodes().length <= 1) {
            throw new ExprValidationException("The OR operator requires at least 2 child expressions");
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String appendStr = "";
        for (ExprNode child : this.getChildNodes()) {
            writer.append(appendStr);
            child.toEPL(writer, getPrecedence());
            appendStr = " or ";
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.OR;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprOrNode)) {
            return false;
        }

        return true;
    }
}
