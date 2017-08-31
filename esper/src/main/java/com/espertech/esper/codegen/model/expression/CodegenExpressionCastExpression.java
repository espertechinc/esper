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

public class CodegenExpressionCastExpression implements CodegenExpression {
    private final Class clazz;
    private final CodegenExpression expression;

    public CodegenExpressionCastExpression(Class clazz, CodegenExpression expression) {
        if (clazz == null) {
            throw new IllegalArgumentException("Cast-to class is a null value");
        }
        this.clazz = clazz;
        this.expression = expression;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("((");
        appendClassName(builder, clazz, null, imports);
        builder.append(")");
        expression.render(builder, imports, isInnerClass);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(clazz);
        expression.mergeClasses(classes);
    }
}
