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
package com.espertech.esper.common.internal.bytecodemodel.model.expression;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionCastUnderlying implements CodegenExpression {
    private final Class clazz;
    private final String className;
    private final CodegenExpression expression;

    public CodegenExpressionCastUnderlying(Class clazz, CodegenExpression expression) {
        this.clazz = clazz;
        this.className = null;
        this.expression = expression;
    }

    public CodegenExpressionCastUnderlying(String className, CodegenExpression expression) {
        this.clazz = null;
        this.className = className;
        this.expression = expression;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("((");
        if (clazz != null) {
            appendClassName(builder, clazz, null, imports);
        } else {
            builder.append(className);
        }
        builder.append(")");
        expression.render(builder, imports, isInnerClass);
        builder.append(".").append("getUnderlying())");
    }

    public void mergeClasses(Set<Class> classes) {
        if (clazz != null) {
            classes.add(clazz);
        }
        expression.mergeClasses(classes);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        consumer.accept(expression);
    }
}
