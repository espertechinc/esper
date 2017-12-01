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
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * Represents the 'prev' previous event function in an expression node tree.
 */
public class ExprPreviousNode extends ExprNodeBase implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval, ExprForge {
    private static final long serialVersionUID = 0L;

    private final ExprPreviousNodePreviousType previousType;

    private Class resultType;
    private int streamNumber;
    private Integer constantIndexNumber;
    private boolean isConstantIndex;
    private transient EventType enumerationMethodType;

    private transient ExprPreviousEvalStrategy evaluator;

    public ExprPreviousNode(ExprPreviousNodePreviousType previousType) {
        this.previousType = previousType;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public void setEvaluator(ExprPreviousEvalStrategy evaluator) {
        this.evaluator = evaluator;
    }

    public int getStreamNumber() {
        return streamNumber;
    }

    public Integer getConstantIndexNumber() {
        return constantIndexNumber;
    }

    public boolean isConstantIndex() {
        return isConstantIndex;
    }

    public Class getResultType() {
        return resultType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public Class getEvaluationType() {
        return getResultType();
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if ((this.getChildNodes().length > 2) || (this.getChildNodes().length == 0)) {
            throw new ExprValidationException("Previous node must have 1 or 2 parameters");
        }

        // add constant of 1 for previous index
        if (this.getChildNodes().length == 1) {
            if (previousType == ExprPreviousNodePreviousType.PREV) {
                this.addChildNodeToFront(new ExprConstantNodeImpl(1));
            } else {
                this.addChildNodeToFront(new ExprConstantNodeImpl(0));
            }
        }

        // the row recognition patterns allows "prev(prop, index)", we switch index the first position
        if (ExprNodeUtilityCore.isConstantValueExpr(this.getChildNodes()[1])) {
            ExprNode first = this.getChildNodes()[0];
            ExprNode second = this.getChildNodes()[1];
            this.setChildNodes(second, first);
        }

        // Determine if the index is a constant value or an expression to evaluate
        if (this.getChildNodes()[0].isConstantResult()) {
            ExprNode constantNode = this.getChildNodes()[0];
            Object value = constantNode.getForge().getExprEvaluator().evaluate(null, false, validationContext.getExprEvaluatorContext());
            if (!(value instanceof Number)) {
                throw new ExprValidationException("Previous function requires an integer index parameter or expression");
            }

            Number valueNumber = (Number) value;
            if (JavaClassHelper.isFloatingPointNumber(valueNumber)) {
                throw new ExprValidationException("Previous function requires an integer index parameter or expression");
            }

            constantIndexNumber = valueNumber.intValue();
            isConstantIndex = true;
        }

        // Determine stream number
        if (this.getChildNodes()[1] instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) this.getChildNodes()[1];
            streamNumber = identNode.getStreamId();
            resultType = JavaClassHelper.getBoxedType(this.getChildNodes()[1].getForge().getEvaluationType());
        } else if (this.getChildNodes()[1] instanceof ExprStreamUnderlyingNode) {
            ExprStreamUnderlyingNode streamNode = (ExprStreamUnderlyingNode) this.getChildNodes()[1];
            streamNumber = streamNode.getStreamId();
            resultType = JavaClassHelper.getBoxedType(this.getChildNodes()[1].getForge().getEvaluationType());
            enumerationMethodType = validationContext.getStreamTypeService().getEventTypes()[streamNode.getStreamId()];
        } else {
            throw new ExprValidationException("Previous function requires an event property as parameter");
        }

        if (previousType == ExprPreviousNodePreviousType.PREVCOUNT) {
            resultType = Long.class;
        }
        if (previousType == ExprPreviousNodePreviousType.PREVWINDOW) {
            resultType = JavaClassHelper.getArrayType(resultType);
        }

        if (validationContext.getViewResourceDelegate() == null) {
            throw new ExprValidationException("Previous function cannot be used in this context");
        }
        validationContext.getViewResourceDelegate().addPreviousRequest(this);
        return null;
    }

    public ExprPreviousNodePreviousType getPreviousType() {
        return previousType;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (!isNewData) {
            return null;
        }
        return evaluator.evaluateGetCollEvents(eventsPerStream, context);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionEvents(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (!isNewData) {
            return null;
        }
        return evaluator.evaluateGetEventBean(eventsPerStream, context);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetEventBean(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (!isNewData) {
            return null;
        }
        return evaluator.evaluateGetCollScalar(eventsPerStream, context);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionScalar(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (previousType == ExprPreviousNodePreviousType.PREV || previousType == ExprPreviousNodePreviousType.PREVTAIL) {
            return null;
        }
        return enumerationMethodType;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (previousType == ExprPreviousNodePreviousType.PREV || previousType == ExprPreviousNodePreviousType.PREVTAIL) {
            return enumerationMethodType;
        }
        return null;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        if (resultType.isArray()) {
            return resultType.getComponentType();
        }
        return resultType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprPrev(this, isNewData);
            Object result = null;
            if (isNewData) {
                result = evaluator.evaluate(eventsPerStream, exprEvaluatorContext);
            }
            InstrumentationHelper.get().aExprPrev(result);
            return result;
        }

        if (!isNewData) {
            return null;
        }
        return evaluator.evaluate(eventsPerStream, exprEvaluatorContext);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(previousType.toString().toLowerCase(Locale.ENGLISH));
        writer.append("(");
        if (previousType == ExprPreviousNodePreviousType.PREVCOUNT || previousType == ExprPreviousNodePreviousType.PREVWINDOW) {
            this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        } else {
            this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            if (this.getChildNodes().length > 1) {
                writer.append(",");
                this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            }
        }
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    @Override
    public int hashCode() {
        return previousType != null ? previousType.hashCode() : 0;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (node == null || getClass() != node.getClass()) {
            return false;
        }

        ExprPreviousNode that = (ExprPreviousNode) node;

        if (previousType != that.previousType) {
            return false;
        }

        return true;
    }
}
