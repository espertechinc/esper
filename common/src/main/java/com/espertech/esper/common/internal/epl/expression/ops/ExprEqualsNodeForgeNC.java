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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

public class ExprEqualsNodeForgeNC extends ExprEqualsNodeForge {
    public ExprEqualsNodeForgeNC(ExprEqualsNodeImpl parent) {
        super(parent);
    }

    public ExprEvaluator getExprEvaluator() {
        ExprForge lhs = getForgeRenderable().getChildNodes()[0].getForge();
        ExprForge rhs = getForgeRenderable().getChildNodes()[1].getForge();
        Class lhsType = lhs.getEvaluationType();
        if (!getForgeRenderable().isIs()) {
            if (lhsType != null && lhsType.isArray()) {
                Class componentType = lhsType.getComponentType();
                if (componentType == boolean.class) {
                    return new ExprEqualsNodeForgeNCEvalEqualsArrayBoolean(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
                } else if (componentType == byte.class) {
                    return new ExprEqualsNodeForgeNCEvalEqualsArrayByte(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
                } else if (componentType == char.class) {
                    return new ExprEqualsNodeForgeNCEvalEqualsArrayChar(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
                } else if (componentType == long.class) {
                    return new ExprEqualsNodeForgeNCEvalEqualsArrayLong(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
                } else if (componentType == double.class) {
                    return new ExprEqualsNodeForgeNCEvalEqualsArrayDouble(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
                } else if (componentType == float.class) {
                    return new ExprEqualsNodeForgeNCEvalEqualsArrayFloat(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
                } else if (componentType == short.class) {
                    return new ExprEqualsNodeForgeNCEvalEqualsArrayShort(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
                } else if (componentType == int.class) {
                    return new ExprEqualsNodeForgeNCEvalEqualsArrayInt(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
                }
                return new ExprEqualsNodeForgeNCEvalEqualsArrayObject(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            }
            return new ExprEqualsNodeForgeNCEvalEqualsNonArray(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
        }

        if (lhsType != null && lhsType.isArray()) {
            Class componentType = lhsType.getComponentType();
            if (componentType == boolean.class) {
                return new ExprEqualsNodeForgeNCEvalIsArrayBoolean(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            } else if (componentType == byte.class) {
                return new ExprEqualsNodeForgeNCEvalIsArrayByte(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            } else if (componentType == char.class) {
                return new ExprEqualsNodeForgeNCEvalIsArrayChar(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            } else if (componentType == long.class) {
                return new ExprEqualsNodeForgeNCEvalIsArrayLong(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            } else if (componentType == double.class) {
                return new ExprEqualsNodeForgeNCEvalIsArrayDouble(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            } else if (componentType == float.class) {
                return new ExprEqualsNodeForgeNCEvalIsArrayFloat(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            } else if (componentType == short.class) {
                return new ExprEqualsNodeForgeNCEvalIsArrayShort(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            } else if (componentType == int.class) {
                return new ExprEqualsNodeForgeNCEvalIsArrayInt(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
            }
            return new ExprEqualsNodeForgeNCEvalIsArrayObject(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
        }
        return new ExprEqualsNodeForgeNCEvalIsNonArray(getForgeRenderable(), lhs.getExprEvaluator(), rhs.getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, getForgeRenderable().isIs() ? "ExprIs" : "ExprEquals", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge lhs = getForgeRenderable().getChildNodes()[0].getForge();
        ExprForge rhs = getForgeRenderable().getChildNodes()[1].getForge();
        if (!getForgeRenderable().isIs()) {
            if (lhs.getEvaluationType() == null || rhs.getEvaluationType() == null) {
                return constantNull();
            }
            return localMethod(ExprEqualsNodeForgeNCForgeEquals.codegen(ExprEqualsNodeForgeNC.this, codegenMethodScope, exprSymbol, codegenClassScope, lhs, rhs));
        }
        return localMethod(ExprEqualsNodeForgeNCForgeIs.codegen(ExprEqualsNodeForgeNC.this, codegenMethodScope, exprSymbol, codegenClassScope, lhs, rhs));
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
