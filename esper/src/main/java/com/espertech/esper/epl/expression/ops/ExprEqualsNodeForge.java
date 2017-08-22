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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;

public abstract class ExprEqualsNodeForge implements ExprForge {
    private final ExprEqualsNodeImpl parent;

    public abstract ExprEvaluator getExprEvaluator();

    public abstract CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

    public ExprEqualsNodeForge(ExprEqualsNodeImpl parent) {
        this.parent = parent;
    }

    public ExprEqualsNodeImpl getForgeRenderable() {
        return parent;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }
}
