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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Method;

public class ExprDotMethodForgeNoDuck implements ExprDotForge {

    private final String optionalStatementName;
    private final Method method;
    private final EPTypeClass methodTargetType;
    private final ExprForge[] parameters;
    private final WrapType wrapType;

    public ExprDotMethodForgeNoDuck(String optionalStatementName, Method method, EPTypeClass methodTargetType, ExprForge[] parameters, WrapType wrapType) {
        this.optionalStatementName = optionalStatementName;
        this.method = method;
        this.methodTargetType = methodTargetType;
        this.parameters = parameters;
        this.wrapType = wrapType;
    }

    public EPChainableType getTypeInfo() {
        if (wrapType == WrapType.WRAPARRAY) {
            EPTypeClass returnType = ClassHelperGenericType.getMethodReturnEPType(method);
            EPTypeClass componentType = JavaClassHelper.getArrayComponentType(returnType);
            return EPChainableTypeHelper.collectionOfSingleValue(componentType);
        } else {
            return EPChainableTypeHelper.fromMethod(method, methodTargetType);
        }
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitMethod(method.getName());
    }

    public ExprDotEval getDotEvaluator() {
        ExprEvaluator[] evaluators = ExprNodeUtilityQuery.getEvaluatorsNoCompile(parameters);
        if (wrapType == WrapType.WRAPARRAY) {
            return new ExprDotMethodForgeNoDuckEvalWrapArray(this, evaluators);
        } else if (wrapType == WrapType.PLAIN) {
            return new ExprDotMethodForgeNoDuckEvalPlain(this, evaluators);
        } else {
            return new ExprDotMethodForgeNoDuckEvalUnderlying(this, evaluators);
        }
    }

    public CodegenExpression codegen(CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (wrapType == WrapType.WRAPARRAY) {
            return ExprDotMethodForgeNoDuckEvalWrapArray.codegenWrapArray(this, inner, innerType, parent, symbols, classScope);
        } else if (wrapType == WrapType.PLAIN) {
            return ExprDotMethodForgeNoDuckEvalPlain.codegenPlain(this, inner, innerType, parent, symbols, classScope);
        } else {
            return ExprDotMethodForgeNoDuckEvalUnderlying.codegenUnderlying(this, inner, innerType, parent, symbols, classScope);
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

    public WrapType getWrapType() {
        return wrapType;
    }

    public enum WrapType {
        WRAPARRAY,
        UNDERLYING,
        PLAIN
    }
}
