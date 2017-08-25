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

public class CodegenExpressionEqualsReference implements CodegenExpression {
    private final CodegenExpression lhs;
    private final CodegenExpression rhs;
    private final boolean isNot;

    public CodegenExpressionEqualsReference(CodegenExpression lhs, CodegenExpression rhs, boolean isNot) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.isNot = isNot;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("(");
        lhs.render(builder, imports, isInnerClass);
        builder.append(isNot ? "!=" : "==");
        rhs.render(builder, imports, isInnerClass);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        lhs.mergeClasses(classes);
        rhs.mergeClasses(classes);
    }
}
