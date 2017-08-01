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
import static com.espertech.esper.codegen.model.expression.CodegenExpressionUtil.renderConstant;

public class CodegenExpressionStaticMethodTakingExprAndConst implements CodegenExpression {
    private final Class target;
    private final String methodName;
    private final CodegenExpression expression;
    private final Object[] consts;

    public CodegenExpressionStaticMethodTakingExprAndConst(Class target, String methodName, CodegenExpression expression, Object[] consts) {
        this.target = target;
        this.methodName = methodName;
        this.expression = expression;
        this.consts = consts;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        appendClassName(builder, target, null, imports);
        builder.append(".");
        builder.append(methodName);
        builder.append("(");
        expression.render(builder, imports);
        for (Object constant : consts) {
            builder.append(",");
            renderConstant(builder, constant, imports);
        }
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        expression.mergeClasses(classes);
        classes.add(target);
    }
}
