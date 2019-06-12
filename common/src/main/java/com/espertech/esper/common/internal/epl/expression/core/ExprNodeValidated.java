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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;

import java.io.StringWriter;

/**
 * A placeholder for another expression node that has been validated already.
 */
public class ExprNodeValidated extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private final ExprNode inner;

    /**
     * Ctor.
     *
     * @param inner nested expression node
     */
    public ExprNodeValidated(ExprNode inner) {
        this.inner = inner;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return inner.getForge().evaluateCodegen(requiredType, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Class getEvaluationType() {
        return inner.getForge().getEvaluationType();
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprPrecedenceEnum getPrecedence() {
        return inner.getPrecedence();
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        inner.toEPL(writer, parentPrecedence);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        inner.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (node instanceof ExprNodeValidated) {
            return inner.equalsNode(((ExprNodeValidated) node).inner, false);
        }
        return inner.equalsNode(node, false);
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public void accept(ExprNodeVisitor visitor) {
        if (visitor.isVisit(this)) {
            visitor.visit(this);
            inner.accept(visitor);
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return inner.getForge().getExprEvaluator().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return inner.getForge().getForgeConstantType();
    }
}
