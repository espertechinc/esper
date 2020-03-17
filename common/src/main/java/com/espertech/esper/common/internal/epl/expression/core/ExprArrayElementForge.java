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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;

public abstract class ExprArrayElementForge implements ExprForge {
    protected final ExprArrayElement parent;
    protected final Class componentType;
    protected final Class arrayType;

    public ExprArrayElementForge(ExprArrayElement parent, Class componentType, Class arrayType) {
        this.parent = parent;
        this.componentType = componentType;
        this.arrayType = arrayType;
    }

    public ExprEvaluator getExprEvaluator() {
        throw new UnsupportedOperationException("Evaluator is not available");
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        throw new UnsupportedOperationException("Evaluation code generation not available");
    }

    public Class getEvaluationType() {
        return componentType;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return (writer, parentPrecedence) -> parent.toPrecedenceFreeEPL(writer);
    }

    public Class getArrayType() {
        return arrayType;
    }

    public ExprArrayElement getParent() {
        return parent;
    }

    public Class getComponentType() {
        return componentType;
    }

}
