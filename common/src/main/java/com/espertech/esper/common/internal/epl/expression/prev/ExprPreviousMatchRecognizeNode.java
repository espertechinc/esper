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
package com.espertech.esper.common.internal.epl.expression.prev;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogStateRandomAccess;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the 'prev' previous event function in match-recognize "define" item.
 */
public class ExprPreviousMatchRecognizeNode extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private Class resultType;
    private int streamNumber;
    private Integer constantIndexNumber;

    private int assignedIndex;
    private CodegenFieldName previousStrategyFieldName;

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Match-Recognize Previous expression must have 2 parameters");
        }

        if (!(this.getChildNodes()[0] instanceof ExprIdentNode)) {
            throw new ExprValidationException("Match-Recognize Previous expression requires an property identifier as the first parameter");
        }

        if (!this.getChildNodes()[1].getForge().getForgeConstantType().isCompileTimeConstant() || (!JavaClassHelper.isNumericNonFP(this.getChildNodes()[1].getForge().getEvaluationType()))) {
            throw new ExprValidationException("Match-Recognize Previous expression requires an integer index parameter or expression as the second parameter");
        }

        ExprNode constantNode = this.getChildNodes()[1];
        Object value = constantNode.getForge().getExprEvaluator().evaluate(null, false, null);

        if (!(value instanceof Number)) {
            throw new ExprValidationException("Match-Recognize Previous expression requires an integer index parameter or expression as the second parameter");
        }
        constantIndexNumber = ((Number) value).intValue();

        // Determine stream number
        ExprIdentNode identNode = (ExprIdentNode) this.getChildNodes()[0];
        streamNumber = identNode.getStreamId();
        ExprForge forge = this.getChildNodes()[0].getForge();
        resultType = forge.getEvaluationType();
        previousStrategyFieldName = validationContext.getMemberNames().previousMatchrecognizeStrategy();

        return null;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    /**
     * Returns the index number.
     *
     * @return index number
     */
    public Integer getConstantIndexNumber() {
        if (constantIndexNumber == null) {
            ExprNode constantNode = this.getChildNodes()[1];
            Object value = constantNode.getForge().getExprEvaluator().evaluate(null, false, null);
            constantIndexNumber = ((Number) value).intValue();
        }
        return constantIndexNumber;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(resultType, this.getClass(), classScope);
        CodegenExpressionRef eps = symbols.getAddEPS(method);

        CodegenExpressionField strategy = classScope.getPackageScope().addOrGetFieldWellKnown(previousStrategyFieldName, RowRecogPreviousStrategy.class);

        CodegenMethod innerEval = CodegenLegoMethodExpression.codegenExpression(getChildNodes()[0].getForge(), method, classScope);

        method.getBlock()
                .declareVar(RowRecogStateRandomAccess.class, "access", exprDotMethod(strategy, "getAccess", symbols.getAddExprEvalCtx(method)))
                .declareVar(EventBean.class, "substituteEvent", exprDotMethod(ref("access"), "getPreviousEvent", constant(assignedIndex)))
                .ifRefNullReturnNull("substituteEvent")
                .declareVar(EventBean.class, "originalEvent", arrayAtIndex(eps, constant(streamNumber)))
                .assignArrayElement(eps, constant(streamNumber), ref("substituteEvent"))
                .declareVar(resultType, "evalResult", localMethod(innerEval, eps, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)))
                .assignArrayElement(eps, constant(streamNumber), ref("originalEvent"))
                .methodReturn(ref("evalResult"));

        return localMethod(method);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("prev(");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(',');
        this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprPreviousMatchRecognizeNode)) {
            return false;
        }

        return true;
    }

    /**
     * Sets the index to use when accessing via getter
     *
     * @param assignedIndex index
     */
    public void setAssignedIndex(int assignedIndex) {
        this.assignedIndex = assignedIndex;
    }
}
