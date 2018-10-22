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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

public class ExprDotNodeForgeVariable extends ExprDotNodeForge {

    private final ExprDotNodeImpl parent;
    private final VariableMetaData variable;
    private final ExprDotStaticMethodWrap resultWrapLambda;
    private final ExprDotForge[] chainForge;

    public ExprDotNodeForgeVariable(ExprDotNodeImpl parent, VariableMetaData variable, ExprDotStaticMethodWrap resultWrapLambda, ExprDotForge[] chainForge) {
        this.parent = parent;
        this.variable = variable;
        this.resultWrapLambda = resultWrapLambda;
        this.chainForge = chainForge;
    }

    public ExprEvaluator getExprEvaluator() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotNodeForgeVariableEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprDot", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public Class getEvaluationType() {
        if (chainForge.length == 0) {
            return variable.getType();
        } else {
            return EPTypeHelper.getClassSingleValued(chainForge[chainForge.length - 1].getTypeInfo());
        }
    }

    public VariableMetaData getVariable() {
        return variable;
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
