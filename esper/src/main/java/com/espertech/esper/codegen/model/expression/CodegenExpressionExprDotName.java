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

public class CodegenExpressionExprDotName implements CodegenExpression {
    private final CodegenExpression lhs;
    private final String name;

    public CodegenExpressionExprDotName(CodegenExpression lhs, String name) {
        this.lhs = lhs;
        this.name = name;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        if (lhs instanceof CodegenExpressionRef) {
            lhs.render(builder, imports, isInnerClass);
        } else {
            builder.append("(");
            lhs.render(builder, imports, isInnerClass);
            builder.append(")");
        }
        builder.append('.').append(name);
    }

    public void mergeClasses(Set<Class> classes) {
        lhs.mergeClasses(classes);
    }
}
