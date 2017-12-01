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

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.util.CodegenClassUtil.getComponentTypeOutermost;
import static com.espertech.esper.codegen.util.CodegenClassUtil.getNumberOfDimensions;

public class CodegenExpressionNewArrayWithInit implements CodegenExpression {
    private final Class component;
    private final CodegenExpression[] expressions;

    public CodegenExpressionNewArrayWithInit(Class component, CodegenExpression[] expressions) {
        this.component = component;
        this.expressions = expressions;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        int numDimensions = getNumberOfDimensions(component);
        Class outermostType = getComponentTypeOutermost(component);
        builder.append("new ");
        CodeGenerationHelper.appendClassName(builder, outermostType, null, imports);
        builder.append("[]");
        for (int i = 0; i < numDimensions; i++) {
            builder.append("[]");
        }
        builder.append("{");
        CodegenExpressionBuilder.renderExpressions(builder, expressions, imports, isInnerClass);
        builder.append("}");
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(component);
        for (CodegenExpression expression : expressions) {
            expression.mergeClasses(classes);
        }
    }
}
