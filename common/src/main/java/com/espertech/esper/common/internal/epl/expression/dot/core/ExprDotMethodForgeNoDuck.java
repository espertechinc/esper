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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.lang.reflect.Method;

public class ExprDotMethodForgeNoDuck implements ExprDotForge {

    private final String optionalStatementName;
    private final Method method;
    private final ExprForge[] parameters;
    private final Type type;

    public ExprDotMethodForgeNoDuck(String optionalStatementName, Method method, ExprForge[] parameters, Type type) {
        this.optionalStatementName = optionalStatementName;
        this.method = method;
        this.parameters = parameters;
        this.type = type;
    }

    public EPType getTypeInfo() {
        if (type == Type.WRAPARRAY) {
            return EPTypeHelper.collectionOfSingleValue(method.getReturnType().getComponentType());
        } else {
            return EPTypeHelper.fromMethod(method);
        }
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitMethod(method.getName());
    }

    public ExprDotEval getDotEvaluator() {
        ExprEvaluator[] evaluators = ExprNodeUtilityQuery.getEvaluatorsNoCompile(parameters);
        if (type == Type.WRAPARRAY) {
            return new ExprDotMethodForgeNoDuckEvalWrapArray(this, evaluators);
        } else if (type == Type.PLAIN) {
            return new ExprDotMethodForgeNoDuckEvalPlain(this, evaluators);
        } else {
            return new ExprDotMethodForgeNoDuckEvalUnderlying(this, evaluators);
        }
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (type == Type.WRAPARRAY) {
            return ExprDotMethodForgeNoDuckEvalWrapArray.codegenWrapArray(this, inner, innerType, codegenMethodScope, exprSymbol, codegenClassScope);
        } else if (type == Type.PLAIN) {
            return ExprDotMethodForgeNoDuckEvalPlain.codegenPlain(this, inner, innerType, codegenMethodScope, exprSymbol, codegenClassScope);
        } else {
            return ExprDotMethodForgeNoDuckEvalUnderlying.codegenUnderlying(this, inner, innerType, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public String getOptionalStatementName() {
        return optionalStatementName;
    }

    public Method getMethod() {
        return method;
    }

    public ExprForge[] getParameters() {
        return parameters;
    }

    public enum Type {
        WRAPARRAY,
        UNDERLYING,
        PLAIN
    }
}
