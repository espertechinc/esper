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

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionInstanceOf implements CodegenExpression {
    private final CodegenExpression lhs;
    private final Class clazz;
    private final boolean not;

    public CodegenExpressionInstanceOf(CodegenExpression lhs, Class clazz, boolean not) {
        this.lhs = lhs;
        this.clazz = clazz;
        this.not = not;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        if (not) {
            builder.append("!(");
        }
        lhs.render(builder, imports, isInnerClass);
        builder.append(" ").append("instanceof ");
        appendClassName(builder, clazz, null, imports);
        if (not) {
            builder.append(")");
        }
    }

    public void mergeClasses(Set<Class> classes) {
        lhs.mergeClasses(classes);
        classes.add(clazz);
    }
}
