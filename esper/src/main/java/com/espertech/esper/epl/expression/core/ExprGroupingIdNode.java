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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;

public class ExprGroupingIdNode extends ExprNodeBase implements ExprForge, ExprEvaluator {

    private static final long serialVersionUID = -8044246020091631492L;
    private Integer id;

    public void setId(int id) {
        this.id = id;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (!validationContext.isAllowRollupFunctions()) {
            throw makeException("grouping_id");
        }
        return null;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return Integer.class;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprForge getForge() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        ExprNodeUtilityCore.toExpressionStringWFunctionName("grouping_id", this.getChildNodes(), writer);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return id;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constant(id);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.NONE;
    }

    public static ExprValidationException makeException(String functionName) {
        return new ExprValidationException("The " + functionName + " function requires the group-by clause to specify rollup, cube or grouping sets, and may only be used in the select-clause, having-clause or order-by-clause");
    }
}
