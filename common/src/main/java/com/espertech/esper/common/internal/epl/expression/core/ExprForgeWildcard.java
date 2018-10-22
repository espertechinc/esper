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

import java.io.StringWriter;

public class ExprForgeWildcard implements ExprForge {

    private final Class underlyingTypeStream0;

    public ExprForgeWildcard(Class underlyingTypeStream0) {
        this.underlyingTypeStream0 = underlyingTypeStream0;
    }

    public ExprEvaluator getExprEvaluator() {
        return ExprEvaluatorWildcard.INSTANCE;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprEvaluatorWildcard.codegen(requiredType, underlyingTypeStream0, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public Class getEvaluationType() {
        return underlyingTypeStream0;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return ExprForgeWildcardRenderable.INSTANCE;
    }

    private final static class ExprForgeWildcardRenderable implements ExprNodeRenderable {

        private final static ExprForgeWildcardRenderable INSTANCE = new ExprForgeWildcardRenderable();

        private ExprForgeWildcardRenderable() {
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
            writer.append("underlying-stream-0");
        }
    }
}
