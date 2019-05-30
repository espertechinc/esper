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

public class CodegenExpressionClass implements CodegenExpression {
    private final Class target;

    public CodegenExpressionClass(Class target) {
        if (target == null) {
            throw new IllegalArgumentException("Invalid null target");
        }
        this.target = target;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        renderClass(target, builder, imports);
    }

    public static void renderClass(Class clazz, StringBuilder builder, Map<Class, String> imports) {
        appendClassName(builder, clazz, null, imports);
        builder.append(".");
        builder.append("class");
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(target);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
    }
}
