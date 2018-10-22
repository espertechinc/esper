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
package com.espertech.esper.common.internal.epl.expression.etc;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.io.StringWriter;

public class ExprEvalWithTypeWidener implements ExprForge {

    private final TypeWidenerSPI widener;
    private final ExprNode validated;
    private final Class targetType;

    public ExprEvalWithTypeWidener(TypeWidenerSPI widener, ExprNode validated, Class targetType) {
        this.widener = widener;
        this.validated = validated;
        this.targetType = targetType;
    }

    public ExprEvaluator getExprEvaluator() {
        throw new UnsupportedOperationException("Not available at compile time");
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression inner = validated.getForge().evaluateCodegen(validated.getForge().getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
        return widener.widenCodegen(inner, codegenMethodScope, codegenClassScope);
    }

    public Class getEvaluationType() {
        return targetType;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return new ExprNodeRenderable() {
            public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
                writer.append(ExprEvalWithTypeWidener.class.getSimpleName());
            }
        };
    }
}
