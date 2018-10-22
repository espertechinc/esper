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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

public class ExprCoalesceNodeForge implements ExprForgeInstrumentable {
    private final ExprCoalesceNode parent;
    private final Class resultType;
    private final boolean[] isNumericCoercion;

    public ExprCoalesceNodeForge(ExprCoalesceNode parent, Class resultType, boolean[] isNumericCoercion) {
        this.parent = parent;
        this.resultType = resultType;
        this.isNumericCoercion = isNumericCoercion;
    }

    public ExprCoalesceNode getForgeRenderable() {
        return parent;
    }

    public boolean[] getIsNumericCoercion() {
        return isNumericCoercion;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprCoalesceNodeForgeEval(this, ExprNodeUtilityQuery.getEvaluatorsNoCompile(parent.getChildNodes()));
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprCoalesceNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprCoalesce", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }
}
