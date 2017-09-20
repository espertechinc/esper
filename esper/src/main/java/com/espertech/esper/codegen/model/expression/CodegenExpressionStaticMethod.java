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
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.renderExpressions;

public class CodegenExpressionStaticMethod implements CodegenExpression {
    private final Class target;
    private final String methodName;
    private final CodegenExpression[] params;

    public CodegenExpressionStaticMethod(Class target, String methodName, CodegenExpression[] params) {
        this.target = target;
        this.methodName = methodName;
        this.params = params;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        appendClassName(builder, target, null, imports);
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
}
