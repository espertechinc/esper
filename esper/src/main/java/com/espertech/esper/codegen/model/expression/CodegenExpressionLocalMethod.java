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
package com.espertech.esper.codegen.model.expression;

import com.espertech.esper.codegen.core.CodegenMethodId;

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.renderExpressions;

public class CodegenExpressionLocalMethod implements CodegenExpression {
    private final CodegenMethodId methodId;
    private final CodegenExpression[] expressions;

    public CodegenExpressionLocalMethod(CodegenMethodId methodId, CodegenExpression[] expressions) {
        this.methodId = methodId;
        this.expressions = expressions;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        methodId.render(builder);
        builder.append("(");
        renderExpressions(builder, expressions, imports);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        for (CodegenExpression expression : expressions) {
            expression.mergeClasses(classes);
        }
    }
}
