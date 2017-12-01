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
package com.espertech.esper.epl.expression.prev;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.rowregex.RegexExprPreviousEvalStrategy;
import com.espertech.esper.rowregex.RegexPartitionStateRandomAccess;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the 'prev' previous event function in match-recognize "define" item.
 */
public class ExprPreviousMatchRecognizeNode extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static final long serialVersionUID = 0L;

    private Class resultType;
    private int streamNumber;
    private Integer constantIndexNumber;

    private transient RegexExprPreviousEvalStrategy strategy;
    private transient ExprEvaluator evaluator;
    private int assignedIndex;

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Match-Recognize Previous expression must have 2 parameters");
        }

        if (!(this.getChildNodes()[0] instanceof ExprIdentNode)) {
            throw new ExprValidationException("Match-Recognize Previous expression requires an property identifier as the first parameter");
        }

        if (!this.getChildNodes()[1].isConstantResult() || (!JavaClassHelper.isNumericNonFP(this.getChildNodes()[1].getForge().getEvaluationType()))) {
            throw new ExprValidationException("Match-Recognize Previous expression requires an integer index parameter or expression as the second parameter");
        }

        ExprNode constantNode = this.getChildNodes()[1];
        Object value = constantNode.getForge().getExprEvaluator().evaluate(null, false, validationContext.getExprEvaluatorContext());
        if (!(value instanceof Number)) {
            throw new ExprValidationException("Match-Recognize Previous expression requires an integer index parameter or expression as the second parameter");
        }
        constantIndexNumber = ((Number) value).intValue();

        // Determine stream number
        ExprIdentNode identNode = (ExprIdentNode) this.getChildNodes()[0];
        streamNumber = identNode.getStreamId();
        ExprForge forge = this.getChildNodes()[0].getForge();
        evaluator = forge.getExprEvaluator();
        resultType = forge.getEvaluationType();
        return null;
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
        RegexPartitionStateRandomAccess access = strategy.getAccess(exprEvaluatorContext);
        EventBean substituteEvent = access.getPreviousEvent(assignedIndex);

        if (substituteEvent == null) {
            return null;
        }

        // Substitute original event with prior event, evaluate inner expression
        EventBean originalEvent = eventsPerStream[streamNumber];
        eventsPerStream[streamNumber] = substituteEvent;
        Object evalResult = evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        eventsPerStream[streamNumber] = originalEvent;

        return evalResult;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
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

    public void setStrategy(RegexExprPreviousEvalStrategy strategy) {
        this.strategy = strategy;
    }
}
