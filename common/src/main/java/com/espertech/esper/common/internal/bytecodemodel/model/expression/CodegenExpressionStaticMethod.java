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
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.renderExpressions;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.traverseMultiple;

public class CodegenExpressionStaticMethod implements CodegenExpression {
    private final Class target;
    private final String targetClassName;
    private final String methodName;
    private final CodegenExpression[] params;

    public CodegenExpressionStaticMethod(Class target, String methodName, CodegenExpression[] params) {
        this.target = target;
        this.targetClassName = null;
        this.methodName = methodName;
        this.params = params;
    }

    public CodegenExpressionStaticMethod(String targetClassName, String methodName, CodegenExpression[] params) {
        this.target = null;
        this.targetClassName = targetClassName;
        this.methodName = methodName;
        this.params = params;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        if (target != null) {
            appendClassName(builder, target, null, imports);
        } else {
            builder.append(targetClassName);
        }
        builder.append(".");
        builder.append(methodName);
        builder.append("(");
        renderExpressions(builder, params, imports, isInnerClass);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(target);
        CodegenExpressionBuilder.mergeClassesExpressions(classes, params);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        traverseMultiple(params, consumer);
    }
}
