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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.SimpleNumberCoercer;

public class ExprEqualsAllAnyNodeForge implements ExprForge {
    private final ExprEqualsAllAnyNode parent;
    private final boolean mustCoerce;
    private final SimpleNumberCoercer coercer;
    private final Class coercionTypeBoxed;
    private final boolean hasCollectionOrArray;

    public ExprEqualsAllAnyNodeForge(ExprEqualsAllAnyNode parent, boolean mustCoerce, SimpleNumberCoercer coercer, Class coercionTypeBoxed, boolean hasCollectionOrArray) {
        this.parent = parent;
        this.mustCoerce = mustCoerce;
        this.coercer = coercer;
        this.coercionTypeBoxed = coercionTypeBoxed;
        this.hasCollectionOrArray = hasCollectionOrArray;
    }

    public ExprEvaluator getExprEvaluator() {
        ExprEvaluator[] evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes());
        if (parent.isAll()) {
            if (!hasCollectionOrArray) {
                return new ExprEqualsAllAnyNodeForgeEvalAllNoColl(this, evaluators);
            }
            return new ExprEqualsAllAnyNodeForgeEvalAllWColl(this, evaluators);
        }
        if (!hasCollectionOrArray) {
            return new ExprEqualsAllAnyNodeForgeEvalAnyNoColl(this, evaluators);
        }
        return new ExprEqualsAllAnyNodeForgeEvalAnyWColl(this, evaluators);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (parent.isAll()) {
            return ExprEqualsAllAnyNodeForgeEvalAllWColl.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
        }
        return ExprEqualsAllAnyNodeForgeEvalAnyWColl.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public ExprEqualsAllAnyNode getForgeRenderable() {
        return parent;
    }

    public boolean isMustCoerce() {
        return mustCoerce;
    }

    public SimpleNumberCoercer getCoercer() {
        return coercer;
    }

    public Class getCoercionTypeBoxed() {
        return coercionTypeBoxed;
    }
}
