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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.traverseMultiple;

public class CodegenExpressionLocalMethod implements CodegenExpression {

    private final CodegenMethod methodNode;
    private final List<CodegenExpression> parameters;

    public CodegenExpressionLocalMethod(CodegenMethod methodNode, List<CodegenExpression> parameters) {
        this.methodNode = methodNode;
        if (methodNode == null) {
            throw new IllegalArgumentException("Null method node");
        }
        this.parameters = parameters;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {

        if (methodNode.getAssignedMethod() == null) {
            throw new IllegalStateException("Method has no assignment for " + methodNode.getAdditionalDebugInfo());
        }
        if (methodNode.getAssignedProviderClassName() != null) {
            builder.append(methodNode.getAssignedProviderClassName()).append(".");
        }
        builder.append(methodNode.getAssignedMethod().getName()).append("(");
        String delimiter = "";

        // pass explicit parameters first
        for (CodegenExpression expression : parameters) {
            builder.append(delimiter);
            expression.render(builder, imports, isInnerClass);
            delimiter = ",";
        }

        // pass pass-thru second
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
        for (CodegenExpression param : parameters) {
            param.mergeClasses(classes);
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        traverseMultiple(parameters, consumer);
    }

    public CodegenMethod getMethodNode() {
        return methodNode;
    }
}
