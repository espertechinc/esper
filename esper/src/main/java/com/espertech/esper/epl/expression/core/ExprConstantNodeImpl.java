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
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents a constant in an expressiun tree.
 */
public class ExprConstantNodeImpl extends ExprNodeBase implements ExprConstantNode, ExprEvaluator, ExprForge {
    private Object value;
    private final Class clazz;
    private static final long serialVersionUID = 3154169410675962539L;

    /**
     * Ctor.
     *
     * @param value is the constant's value.
     */
    public ExprConstantNodeImpl(Object value) {
        this.value = value;
        if (value == null) {
            clazz = null;
        } else {
            clazz = JavaClassHelper.getPrimitiveType(value.getClass());
        }
    }

    public boolean isConstantValue() {
        return true;
    }

    /**
     * Ctor.
     *
     * @param value     is the constant's value.
     * @param valueType is the constant's value type.
     */
    public ExprConstantNodeImpl(Object value, Class valueType) {
        this.value = value;
        if (value == null) {
            clazz = valueType;
        } else {
            clazz = JavaClassHelper.getPrimitiveType(value.getClass());
        }
    }

    /**
     * Ctor - for use when the constant should return a given type and the actual value is always null.
     *
     * @param clazz the type of the constant null.
     */
    public ExprConstantNodeImpl(Class clazz) {
        this.clazz = JavaClassHelper.getBoxedType(clazz);
        this.value = null;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return clazz;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (value == null) {
            return constantNull();
        }
        if (value.getClass().isEnum()) {
            return enumValue(value.getClass(), value.toString());
        }
        if (!JavaClassHelper.isJavaBuiltinDataType(value.getClass())) {
            CodegenMember constant = codegenClassScope.makeAddMember(value.getClass(), value);
            return member(constant.getMemberId());
        }
        return constant(value);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.NONE;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public boolean isConstantResult() {
        return true;
    }

    /**
     * Returns the constant's value.
     *
     * @return value of constant
     */
    public Object getConstantValue(ExprEvaluatorContext context) {
        return value;
    }

    /**
     * Sets the value of the constant.
     *
     * @param value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

    public Class getConstantType() {
        return clazz;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qaExprConst(value);
        }
        return value;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (value instanceof String) {
            writer.append("\"" + value + '\"');
        } else if (value == null) {
            writer.append("null");
        } else {
            writer.append(value.toString());
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprConstantNodeImpl)) {
            return false;
        }

        ExprConstantNodeImpl other = (ExprConstantNodeImpl) node;

        if ((other.value == null) && (this.value != null)) {
            return false;
        }
        if ((other.value != null) && (this.value == null)) {
            return false;
        }
        if ((other.value == null) && (this.value == null)) {
            return true;
        }
        return other.value.equals(this.value);
    }
}
