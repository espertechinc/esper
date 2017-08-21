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

import com.espertech.esper.codegen.base.CodegenMethodNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodegenExpressionLocalCall implements CodegenExpression {

    private final CodegenMethodNode methodNode;
    private final List<CodegenExpression> parameters;

    public CodegenExpressionLocalCall(CodegenMethodNode methodNode, List<CodegenExpression> parameters) {
        this.methodNode = methodNode;
        this.parameters = parameters;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {

        if (methodNode.getAssignedMethod() == null) {
            throw new IllegalStateException("Method has no assignment for " + methodNode.getGenerator().getSimpleName());
        }
        builder.append(methodNode.getAssignedMethod().getName()).append("(");
        String delimiter = "";

        // pass explicit parameters first
        for (CodegenExpression expression : parameters) {
            builder.append(delimiter);
            expression.render(builder, imports);
            delimiter = ",";
        }

        // pass pass-thru second
        if (methodNode.getDeepParameters() == null) {
            throw new IllegalStateException("Method has no parameter assignments, see " + methodNode.getGenerator().getSimpleName());
        }
        if (methodNode.getOptionalSymbolProvider() == null) {
            for (String name : methodNode.getDeepParameters()) {
                builder.append(delimiter);
                builder.append(name);
                delimiter = ",";
            }
        }
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        methodNode.mergeClasses(classes);
    }
}
