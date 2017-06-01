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

import com.espertech.esper.codegen.core.CodeGenerationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Map;
import java.util.Set;

public class CodegenExpressionNewArray implements CodegenExpression {
    private final Class component;
    private final CodegenExpression expression;

    public CodegenExpressionNewArray(Class component, CodegenExpression expression) {
        this.component = component;
        this.expression = expression;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        int numDimensions = JavaClassHelper.getNumberOfDimensions(component);
        Class outermostType = JavaClassHelper.getComponentTypeOutermost(component);
        builder.append("new ");
        CodeGenerationHelper.appendClassName(builder, outermostType, null, imports);
        builder.append("[");
        expression.render(builder, imports);
        builder.append("]");
        for (int i = 0; i < numDimensions; i++) {
            builder.append("[]");
        }
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(component);
        expression.mergeClasses(classes);
    }
}
