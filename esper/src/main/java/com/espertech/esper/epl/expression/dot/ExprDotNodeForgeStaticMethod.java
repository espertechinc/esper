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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.dot.ExprDotStaticMethodWrap;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastMethod;

public class ExprDotNodeForgeStaticMethod extends ExprDotNodeForge {

    private final ExprNode parent;
    private final boolean isReturnsConstantResult;
    private final String statementName;
    private final String classOrPropertyName;
    private final FastMethod staticMethod;
    private final ExprForge[] childForges;
    private final boolean isConstantParameters;
    private final ExprDotForge[] chainForges;
    private final ExprDotStaticMethodWrap resultWrapLambda;
    private final boolean rethrowExceptions;
    private final Object targetObject;

    public ExprDotNodeForgeStaticMethod(ExprNode parent, boolean isReturnsConstantResult, String statementName, String classOrPropertyName, FastMethod staticMethod, ExprForge[] childForges, boolean isConstantParameters, ExprDotForge[] chainForges, ExprDotStaticMethodWrap resultWrapLambda, boolean rethrowExceptions, Object targetObject) {
        this.parent = parent;
        this.isReturnsConstantResult = isReturnsConstantResult;
        this.statementName = statementName;
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
    }

    public ExprEvaluator getExprEvaluator() {
        ExprEvaluator[] childEvals = ExprNodeUtilityCore.getEvaluatorsNoCompile(childForges);
        return new ExprDotNodeForgeStaticMethodEval(this, childEvals, ExprDotNodeUtility.getEvaluators(chainForges));
    }

    public Class getEvaluationType() {
        if (chainForges.length == 0) {
            return JavaClassHelper.getBoxedType(staticMethod.getReturnType());
        } else {
            return EPTypeHelper.getNormalizedClass(chainForges[chainForges.length - 1].getTypeInfo());
        }
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotNodeForgeStaticMethodEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public String getStatementName() {
        return statementName;
    }

    public String getClassOrPropertyName() {
        return classOrPropertyName;
    }

    public FastMethod getStaticMethod() {
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

    public Object getTargetObject() {
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
}

