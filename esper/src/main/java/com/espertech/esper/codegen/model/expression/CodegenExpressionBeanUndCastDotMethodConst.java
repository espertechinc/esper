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

public class CodegenExpressionBeanUndCastDotMethodConst implements CodegenExpression {
    private final Class clazz;
    private final CodegenExpression expression;
    private final String method;
    private final Object constant;

    public CodegenExpressionBeanUndCastDotMethodConst(Class clazz, CodegenExpression expression, String method, Object constant) {
        this.clazz = clazz;
        this.expression = expression;
        this.method = method;
        this.constant = constant;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("((");
        appendClassName(builder, clazz, null, imports);
        builder.append(")");
        expression.render(builder, imports, isInnerClass);
        builder.append(".getUnderlying()).");
        builder.append(method).append("(");
        renderConstant(builder, constant, imports);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        expression.mergeClasses(classes);
        classes.add(clazz);
    }
}
