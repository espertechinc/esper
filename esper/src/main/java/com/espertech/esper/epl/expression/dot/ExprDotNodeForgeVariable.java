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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.variable.VariableReader;

public class ExprDotNodeForgeVariable extends ExprDotNodeForge {

    private final ExprDotNodeImpl parent;
    private final VariableReader variableReader;
    private final ExprDotStaticMethodWrap resultWrapLambda;
    private final ExprDotForge[] chainForge;

    public ExprDotNodeForgeVariable(ExprDotNodeImpl parent, VariableReader variableReader, ExprDotStaticMethodWrap resultWrapLambda, ExprDotForge[] chainForge) {
        this.parent = parent;
        this.variableReader = variableReader;
        this.resultWrapLambda = resultWrapLambda;
        this.chainForge = chainForge;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprDotNodeForgeVariableEval(this, ExprDotNodeUtility.getEvaluators(chainForge));
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotNodeForgeVariableEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        if (chainForge.length == 0) {
            return variableReader.getVariableMetaData().getType();
        } else {
            return EPTypeHelper.getClassSingleValued(chainForge[chainForge.length - 1].getTypeInfo());
        }
    }

    public VariableReader getVariableReader() {
        return variableReader;
    }

    public ExprDotStaticMethodWrap getResultWrapLambda() {
        return resultWrapLambda;
    }

    public ExprDotNodeImpl getForgeRenderable() {
        return parent;
    }

    public boolean isReturnsConstantResult() {
        return false;
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

    public ExprDotForge[] getChainForge() {
        return chainForge;
    }
}
