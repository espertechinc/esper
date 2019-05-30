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

public class CodegenChainElement {
    private final String method;
    private final CodegenExpression[] optionalParams;

    public CodegenChainElement(String method, CodegenExpression[] optionalParams) {
        this.method = method;
        this.optionalParams = optionalParams;
        if (optionalParams != null) {
            for (int i = 0; i < optionalParams.length; i++) {
                if (optionalParams[i] == null) {
                    throw new IllegalArgumentException("Invalid null expression parameter at position " + i);
                }
            }
        }
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append(method).append("(");
        if (optionalParams != null) {
            String delimiter = "";
            for (CodegenExpression param : optionalParams) {
                builder.append(delimiter);
                param.render(builder, imports, isInnerClass);
                delimiter = ",";
            }
        }
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        if (optionalParams != null) {
            for (CodegenExpression param : optionalParams) {
                param.mergeClasses(classes);
            }
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        traverseMultiple(optionalParams, consumer);
    }
}
