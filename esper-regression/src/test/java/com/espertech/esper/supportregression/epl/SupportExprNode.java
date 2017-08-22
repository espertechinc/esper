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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;

public class SupportExprNode extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private static int validateCount;

    private Class type;
    private Object value;
    private int validateCountSnapshot;

    public static void setValidateCount(int validateCount) {
        SupportExprNode.validateCount = validateCount;
    }

    public SupportExprNode(Class type) {
        this.type = type;
        this.value = null;
    }

    public SupportExprNode(Object value) {
        this.type = value.getClass();
        this.value = value;
    }

    public SupportExprNode(Object value, Class type) {
        this.value = value;
        this.type = type;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Keep a count for if and when this was validated
        validateCount++;
        validateCountSnapshot = validateCount;
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public int getValidateCountSnapshot() {
        return validateCountSnapshot;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return value;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constant(value);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.NONE;
    }

    public Class getEvaluationType() {
        return type;
    }

    public ExprForge getForge() {
        return this;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (value instanceof String) {
            writer.append("\"" + value + "\"");
        } else {
            if (value == null) {
                writer.append("null");
            } else {
                writer.append(value.toString());
            }
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof SupportExprNode)) {
            return false;
        }
        SupportExprNode other = (SupportExprNode) node;
        return value.equals(other.value);
    }
}
