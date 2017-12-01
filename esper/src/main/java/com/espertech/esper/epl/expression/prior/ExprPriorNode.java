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
package com.espertech.esper.epl.expression.prior;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the 'prior' prior event function in an expression node tree.
 */
public class ExprPriorNode extends ExprNodeBase implements ExprEvaluator, ExprForge {
    private Class resultType;
    private int streamNumber;
    private int constantIndexNumber;
    private transient ExprPriorEvalStrategy priorStrategy;
    private transient ExprEvaluator innerEvaluator;
    private static final long serialVersionUID = -2115346817501589366L;

    @Override
    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public int getStreamNumber() {
        return streamNumber;
    }

    public int getConstantIndexNumber() {
        return constantIndexNumber;
    }

    public void setPriorStrategy(ExprPriorEvalStrategy priorStrategy) {
        this.priorStrategy = priorStrategy;
    }

    public ExprEvaluator getInnerEvaluator() {
        return innerEvaluator;
    }

    public ExprForge getForge() {
        return this;
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Prior node must have 2 parameters");
        }
        if (!(this.getChildNodes()[0].isConstantResult())) {
            throw new ExprValidationException("Prior function requires a constant-value integer-typed index expression as the first parameter");
        }
        ExprNode constantNode = this.getChildNodes()[0];
        Class constantNodeType = constantNode.getForge().getEvaluationType();
        if (constantNodeType != Integer.class && constantNodeType != int.class) {
            throw new ExprValidationException("Prior function requires an integer index parameter");
        }

        Object value = constantNode.getForge().getExprEvaluator().evaluate(null, false, validationContext.getExprEvaluatorContext());
        constantIndexNumber = ((Number) value).intValue();
        ExprForge innerForge = this.getChildNodes()[1].getForge();
        innerEvaluator = innerForge.getExprEvaluator();

        // Determine stream number
        // Determine stream number
        if (this.getChildNodes()[1] instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) this.getChildNodes()[1];
            streamNumber = identNode.getStreamId();
            resultType = JavaClassHelper.getBoxedType(innerForge.getEvaluationType());
        } else if (this.getChildNodes()[1] instanceof ExprStreamUnderlyingNode) {
            ExprStreamUnderlyingNode streamNode = (ExprStreamUnderlyingNode) this.getChildNodes()[1];
            streamNumber = streamNode.getStreamId();
            resultType = JavaClassHelper.getBoxedType(innerForge.getEvaluationType());
        } else {
            throw new ExprValidationException("Previous function requires an event property as parameter");
        }

        // add request
        if (validationContext.getViewResourceDelegate() == null) {
            throw new ExprValidationException("Prior function cannot be used in this context");
        }
        validationContext.getViewResourceDelegate().addPriorNodeRequest(this);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprPrior(this);
            Object result = priorStrategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext, streamNumber, innerEvaluator, constantIndexNumber);
            InstrumentationHelper.get().aExprPrior(result);
            return result;
        }
        return priorStrategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext, streamNumber, innerEvaluator, constantIndexNumber);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("prior(");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(',');
        this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprPriorNode)) {
            return false;
        }

        return true;
    }
}
