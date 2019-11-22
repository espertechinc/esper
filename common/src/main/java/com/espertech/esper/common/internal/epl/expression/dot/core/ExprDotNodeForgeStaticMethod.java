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
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotStaticMethodWrap;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.ValueAndFieldDesc;

import java.lang.reflect.Method;

public class ExprDotNodeForgeStaticMethod extends ExprDotNodeForge {

    private final ExprNode parent;
    private final boolean isReturnsConstantResult;
    private final String classOrPropertyName;
    private final Method staticMethod;
    private final ExprForge[] childForges;
    private final boolean isConstantParameters;
    private final ExprDotForge[] chainForges;
    private final ExprDotStaticMethodWrap resultWrapLambda;
    private final boolean rethrowExceptions;
    private final ValueAndFieldDesc targetObject;
    private final String optionalStatementName;

    public ExprDotNodeForgeStaticMethod(ExprNode parent, boolean isReturnsConstantResult, String classOrPropertyName, Method staticMethod, ExprForge[] childForges, boolean isConstantParameters, ExprDotForge[] chainForges, ExprDotStaticMethodWrap resultWrapLambda, boolean rethrowExceptions, ValueAndFieldDesc targetObject, String optionalStatementName) {
        this.parent = parent;
        this.isReturnsConstantResult = isReturnsConstantResult;
        this.classOrPropertyName = classOrPropertyName;
        this.staticMethod = staticMethod;
        this.childForges = childForges;
        if (chainForges.length > 0) {
            this.isConstantParameters = false;
        } else {
            this.isConstantParameters = isConstantParameters;
        }
        this.chainForges = chainForges;
        this.resultWrapLambda = resultWrapLambda;
        this.rethrowExceptions = rethrowExceptions;
        this.targetObject = targetObject;
        this.optionalStatementName = optionalStatementName;
    }

    public ExprEvaluator getExprEvaluator() {
        ExprEvaluator[] childEvals = ExprNodeUtilityQuery.getEvaluatorsNoCompile(childForges);
        return new ExprDotNodeForgeStaticMethodEval(this, childEvals, ExprDotNodeUtility.getEvaluators(chainForges));
    }

    public Class getEvaluationType() {
        if (chainForges.length == 0) {
            return JavaClassHelper.getBoxedType(staticMethod.getReturnType());
        } else {
            return EPTypeHelper.getNormalizedClass(chainForges[chainForges.length - 1].getTypeInfo());
        }
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotNodeForgeStaticMethodEval.codegenExprEval(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprDot", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return ExprDotNodeForgeStaticMethodEval.codegenGet(beanExpression, this, codegenMethodScope, codegenClassScope);
    }

    public String getClassOrPropertyName() {
        return classOrPropertyName;
    }

    public Method getStaticMethod() {
        return staticMethod;
    }

    public ExprForge[] getChildForges() {
        return childForges;
    }

    public boolean isConstantParameters() {
        return isConstantParameters;
    }

    public ExprDotForge[] getChainForges() {
        return chainForges;
    }

    public ExprDotStaticMethodWrap getResultWrapLambda() {
        return resultWrapLambda;
    }

    public boolean isRethrowExceptions() {
        return rethrowExceptions;
    }

    public ValueAndFieldDesc getTargetObject() {
        return targetObject;
    }

    public ExprNode getForgeRenderable() {
        return parent;
    }

    public boolean isReturnsConstantResult() {
        return isReturnsConstantResult;
    }

    public FilterExprAnalyzerAffector getFilterExprAnalyzerAffector() {
        return null;
    }

    public Integer getStreamNumReferenced() {
        return null;
    }

    public String getRootPropertyName() {
        return null;
    }

    public String getOptionalStatementName() {
        return optionalStatementName;
    }
}

