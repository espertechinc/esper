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
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.type.RelationalOpEnum;

public class ExprRelationalOpAllAnyNodeForge implements ExprForge {
    private final ExprRelationalOpAllAnyNode parent;
    private final RelationalOpEnum.Computer computer;
    private final boolean hasCollectionOrArray;

    public ExprRelationalOpAllAnyNodeForge(ExprRelationalOpAllAnyNode parent, RelationalOpEnum.Computer computer, boolean hasCollectionOrArray) {
        this.parent = parent;
        this.computer = computer;
        this.hasCollectionOrArray = hasCollectionOrArray;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprRelationalOpAllAnyNodeForgeEval(this, ExprNodeUtilityCore.getEvaluatorsNoCompile(parent.getChildNodes()));
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprRelationalOpAllAnyNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public ExprRelationalOpAllAnyNode getForgeRenderable() {
        return parent;
    }

    public RelationalOpEnum.Computer getComputer() {
        return computer;
    }

    public boolean isHasCollectionOrArray() {
        return hasCollectionOrArray;
    }
}
