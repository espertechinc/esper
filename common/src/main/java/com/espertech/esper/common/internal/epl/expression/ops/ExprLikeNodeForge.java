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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;

public abstract class ExprLikeNodeForge implements ExprForgeInstrumentable {
    private final ExprLikeNode parent;
    private final boolean isNumericValue;

    public abstract ExprEvaluator getExprEvaluator();

    public abstract CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

    public ExprLikeNodeForge(ExprLikeNode parent, boolean isNumericValue) {
        this.parent = parent;
        this.isNumericValue = isNumericValue;
    }

    public ExprLikeNode getForgeRenderable() {
        return parent;
    }

    public boolean isNumericValue() {
        return isNumericValue;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

}
