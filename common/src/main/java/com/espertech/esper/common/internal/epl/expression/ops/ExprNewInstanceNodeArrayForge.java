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
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeRenderable;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

public class ExprNewInstanceNodeArrayForge implements ExprForgeInstrumentable {

    private final ExprNewInstanceNode parent;
    private final Class targetClass;
    private final Class targetClassArrayed;

    public ExprNewInstanceNodeArrayForge(ExprNewInstanceNode parent, Class targetClass, Class targetClassArrayed) {
        this.parent = parent;
        this.targetClass = targetClass;
        this.targetClassArrayed = targetClassArrayed;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprNewInstanceNodeArrayForgeEval(this);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprNewInstance", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprNewInstanceNodeArrayForgeEval.evaluateCodegen(requiredType, this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Class getEvaluationType() {
        return targetClassArrayed;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return parent;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNewInstanceNode getParent() {
        return parent;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public Class getTargetClassArrayed() {
        return targetClassArrayed;
    }
}
