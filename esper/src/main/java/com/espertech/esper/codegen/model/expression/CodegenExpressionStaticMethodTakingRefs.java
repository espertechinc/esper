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

public class CodegenExpressionStaticMethodTakingRefs implements CodegenExpression {
    private final Class target;
    private final String methodName;
    private final String[] refs;

    public CodegenExpressionStaticMethodTakingRefs(Class target, String methodName, String[] refs) {
        this.target = target;
        this.methodName = methodName;
        this.refs = refs;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        appendClassName(builder, target, null, imports);
        builder.append(".");
        builder.append(methodName);
        builder.append("(");
        String delimiter = "";
        for (String parameter : refs) {
            builder.append(delimiter);
            builder.append(parameter);
            delimiter = ",";
        }
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(target);
    }
}
