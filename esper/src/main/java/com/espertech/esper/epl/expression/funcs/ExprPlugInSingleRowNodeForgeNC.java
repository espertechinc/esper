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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.expression.dot.ExprDotNodeForgeStaticMethod;

public class ExprPlugInSingleRowNodeForgeNC extends ExprPlugInSingleRowNodeForge {

    private final ExprDotNodeForgeStaticMethod inner;

    public ExprPlugInSingleRowNodeForgeNC(ExprPlugInSingleRowNode parent, ExprDotNodeForgeStaticMethod inner) {
        super(parent, false);
        this.inner = inner;
    }

    public ExprEvaluator getExprEvaluator() {
        return inner.getExprEvaluator();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return inner.evaluateCodegen(requiredType, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return inner.getEvaluationType();
    }
}
