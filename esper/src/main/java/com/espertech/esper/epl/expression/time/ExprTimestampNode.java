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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.exprDotMethodChain;

/**
 * Represents the CURRENT_TIMESTAMP() function or reserved keyword in an expression tree.
 */
public class ExprTimestampNode extends ExprNodeBase implements ExprEvaluator, ExprForge {
    private static final long serialVersionUID = -6332243334897136751L;

    /**
     * Ctor.
     */
    public ExprTimestampNode() {
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return Long.class;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(codegenMethodScope);
        return exprDotMethodChain(refExprEvalCtx).add("getTimeProvider").add("getTime");
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 0) {
            throw new ExprValidationException("current_timestamp function node cannot have a child node");
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            long value = exprEvaluatorContext.getTimeProvider().getTime();
            InstrumentationHelper.get().qaExprTimestamp(this, value);
            return value;
        }
        return exprEvaluatorContext.getTimeProvider().getTime();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("current_timestamp()");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprTimestampNode)) {
            return false;
        }
        return true;
    }
}
