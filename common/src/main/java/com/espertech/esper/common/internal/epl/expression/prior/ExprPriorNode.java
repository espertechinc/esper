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
package com.espertech.esper.common.internal.epl.expression.prior;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the 'prior' prior event function in an expression node tree.
 */
public class ExprPriorNode extends ExprNodeBase implements ExprEvaluator, ExprForgeInstrumentable {
    private Class resultType;
    private int streamNumber;
    private int constantIndexNumber;
    private ExprForge innerForge;
    private int relativeIndex = -1;
    private CodegenFieldName priorStrategyFieldName;

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

    public ExprForge getInnerForge() {
        return innerForge;
    }

    public ExprForge getForge() {
        return this;
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Prior node must have 2 parameters");
        }
        if (!(this.getChildNodes()[0].getForge().getForgeConstantType().isCompileTimeConstant())) {
            throw new ExprValidationException("Prior function requires a constant-value integer-typed index expression as the first parameter");
        }

        // Child identifier nodes receive optional event
        ExprNodeUtilityMake.setChildIdentNodesOptionalEvent(this);

        ExprNode constantNode = this.getChildNodes()[0];
        Class constantNodeType = constantNode.getForge().getEvaluationType();
        if (constantNodeType != Integer.class && constantNodeType != int.class) {
            throw new ExprValidationException("Prior function requires an integer index parameter");
        }

        Object value = constantNode.getForge().getExprEvaluator().evaluate(null, false, null);
        constantIndexNumber = ((Number) value).intValue();
        innerForge = this.getChildNodes()[1].getForge();

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
        priorStrategyFieldName = validationContext.getMemberNames().priorStrategy(streamNumber);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod method = parent.makeChild(resultType, this.getClass(), codegenClassScope);

        CodegenMethod innerEval = CodegenLegoMethodExpression.codegenExpression(innerForge, method, codegenClassScope);
        CodegenExpressionRef eps = exprSymbol.getAddEPS(method);

        // see ExprPriorEvalStrategyBase
        CodegenExpression future = codegenClassScope.getPackageScope().addOrGetFieldWellKnown(priorStrategyFieldName, PriorEvalStrategy.class);
        method.getBlock()
                .declareVar(EventBean.class, "originalEvent", arrayAtIndex(eps, constant(streamNumber)))
                .declareVar(EventBean.class, "substituteEvent", exprDotMethod(future, "getSubstituteEvent", ref("originalEvent"), exprSymbol.getAddIsNewData(method),
                        constant(constantIndexNumber), constant(relativeIndex), exprSymbol.getAddExprEvalCtx(method), constant(streamNumber)))
                .assignArrayElement(eps, constant(streamNumber), ref("substituteEvent"))
                .declareVar(resultType, "evalResult", localMethod(innerEval, eps, exprSymbol.getAddIsNewData(method), exprSymbol.getAddExprEvalCtx(method)))
                .assignArrayElement(eps, constant(streamNumber), ref("originalEvent"))
                .methodReturn(ref("evalResult"));

        return localMethod(method);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprPrior", requiredType, parent, exprSymbol, codegenClassScope).build();
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

    public void setRelativeIndex(int relativeIndex) {
        this.relativeIndex = relativeIndex;
    }
}
