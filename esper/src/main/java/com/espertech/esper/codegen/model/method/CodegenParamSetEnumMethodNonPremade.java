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
package com.espertech.esper.codegen.model.method;

import com.espertech.esper.codegen.core.CodegenIndent;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Map;
import java.util.Set;

public class CodegenParamSetEnumMethodNonPremade extends CodegenParamSet {

    private final CodegenExpression eps;
    private final CodegenExpression enumcoll;
    private final CodegenExpression isNewData;
    private final CodegenExpression exprCtx;

    public CodegenParamSetEnumMethodNonPremade(CodegenExpression eps, CodegenExpression enumcoll, CodegenExpression isNewData, CodegenExpression exprCtx) {
        this.eps = eps;
        this.enumcoll = enumcoll;
        this.isNewData = isNewData;
        this.exprCtx = exprCtx;
    }

    public CodegenExpression enumcoll() {
        return enumcoll;
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(ExprEvaluatorContext.class);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, CodegenIndent codegenIndent, String optionalComment) {
        throw new IllegalStateException("Rendering not allowed, use " + CodegenParamSetEnumMethodPremade.class.getSimpleName() + ".INSTANCE instead (from " + optionalComment + ")");
    }

    public CodegenPassSet getPassAll() {
        return new CodegenPassSet() {
            public void render(StringBuilder builder, Map<Class, String> imports) {
                eps.render(builder, imports);
                builder.append(",");
                enumcoll.render(builder, imports);
                builder.append(",");
                isNewData.render(builder, imports);
                builder.append(",");
                exprCtx.render(builder, imports);
            }

            public void mergeClasses(Set<Class> classes) {
            }
        };
    }
}

