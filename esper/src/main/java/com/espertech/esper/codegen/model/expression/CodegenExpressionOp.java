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

import java.util.Map;
import java.util.Set;

public class CodegenExpressionOp implements CodegenExpression {
    private final CodegenExpression left;
    private final String expressionText;
    private final CodegenExpression right;

    public CodegenExpressionOp(CodegenExpression left, String expressionText, CodegenExpression right) {
        this.left = left;
        this.expressionText = expressionText;
        this.right = right;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("(");
        left.render(builder, imports, isInnerClass);
        builder.append(expressionText);
        right.render(builder, imports, isInnerClass);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        left.mergeClasses(classes);
        right.mergeClasses(classes);
    }
}
