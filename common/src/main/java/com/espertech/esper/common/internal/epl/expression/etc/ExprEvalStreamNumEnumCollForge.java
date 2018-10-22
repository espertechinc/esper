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

import java.util.Collection;

public class ExprEvalStreamNumEnumCollForge implements ExprForge {
    private final ExprEnumerationForge enumeration;

    public ExprEvalStreamNumEnumCollForge(ExprEnumerationForge enumeration) {
        this.enumeration = enumeration;
    }

    public ExprEvaluator getExprEvaluator() {
        throw new UnsupportedOperationException("Not available at compile time");
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return enumeration.evaluateGetROCollectionEventsCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public Class getEvaluationType() {
        return Collection.class;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return enumeration.getForgeRenderable();
    }
}
