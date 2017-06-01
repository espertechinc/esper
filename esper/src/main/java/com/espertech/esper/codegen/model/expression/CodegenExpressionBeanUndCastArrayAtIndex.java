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

public class CodegenExpressionBeanUndCastArrayAtIndex implements CodegenExpression {
    private final Class clazz;
    private final CodegenExpression ref;
    private final int index;

    public CodegenExpressionBeanUndCastArrayAtIndex(Class clazz, CodegenExpression ref, int index) {
        this.clazz = clazz;
        this.ref = ref;
        this.index = index;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        builder.append("((");
        appendClassName(builder, clazz, null, imports);
        builder.append(")");
        ref.render(builder, imports);
        builder.append(".getUnderlying())");
        builder.append("[").append(index).append("]");
    }

    public void mergeClasses(Set<Class> classes) {
        ref.mergeClasses(classes);
        classes.add(clazz);
    }
}
