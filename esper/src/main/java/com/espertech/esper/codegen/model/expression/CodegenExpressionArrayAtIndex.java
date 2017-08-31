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

public class CodegenExpressionArrayAtIndex implements CodegenExpression {
    private final CodegenExpression expression;
    private final CodegenExpression index;

    public CodegenExpressionArrayAtIndex(CodegenExpression expression, CodegenExpression index) {
        this.expression = expression;
        this.index = index;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        expression.render(builder, imports, isInnerClass);
        builder.append("[");
        index.render(builder, imports, isInnerClass);
        builder.append("]");
    }

    public void mergeClasses(Set<Class> classes) {
        expression.mergeClasses(classes);
        index.mergeClasses(classes);
    }
}
