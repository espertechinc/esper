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
package com.espertech.esper.common.internal.epl.expression.etc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class ExprEvalSystemProperty extends ExprNodeBase implements ExprForge, ExprEvaluator {
    public final static String SYSTEM_PROPETIES_NAME = "systemproperties";

    private final String systemPropertyName;

    public ExprEvalSystemProperty(String systemPropertyName) {
        this.systemPropertyName = systemPropertyName;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return staticMethod(System.class, "getProperty", constant(systemPropertyName));
    }

    public Class getEvaluationType() {
        return String.class;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return System.getProperty(systemPropertyName);
    }

    public ExprNodeRenderable getForgeRenderable() {
        return new ExprNodeRenderable() {
            public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
                toPrecedenceFreeEPL(writer);
            }
        };
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(SYSTEM_PROPETIES_NAME).append("'").append(systemPropertyName).append("'");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode other, boolean ignoreStreamPrefix) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        ExprEvalSystemProperty that = (ExprEvalSystemProperty) other;

        return systemPropertyName.equals(that.systemPropertyName);
    }
}
