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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.traverseMultiple;

public class CodegenExpressionNewInstanceInnerClass implements CodegenExpression {
    private final String innerName;
    private final CodegenExpression[] params;

    public CodegenExpressionNewInstanceInnerClass(String innerName, CodegenExpression[] params) {
        this.innerName = innerName;
        this.params = params;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("new ").append(innerName).append("(");
        CodegenExpressionBuilder.renderExpressions(builder, params, imports, isInnerClass);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        CodegenExpressionBuilder.mergeClassesExpressions(classes, params);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        traverseMultiple(params, consumer);
    }
}
