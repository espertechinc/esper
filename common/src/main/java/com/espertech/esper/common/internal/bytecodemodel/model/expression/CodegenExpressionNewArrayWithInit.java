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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.traverseMultiple;
import static com.espertech.esper.common.internal.bytecodemodel.util.CodegenClassUtil.getComponentTypeOutermost;
import static com.espertech.esper.common.internal.bytecodemodel.util.CodegenClassUtil.getNumberOfDimensions;

public class CodegenExpressionNewArrayWithInit implements CodegenExpression {
    private final EPTypeClass component;
    private final CodegenExpression[] expressions;

    public CodegenExpressionNewArrayWithInit(EPTypeClass component, CodegenExpression[] expressions) {
        this.component = component;
        this.expressions = expressions;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        int numDimensions = getNumberOfDimensions(component.getType());
        Class outermostType = getComponentTypeOutermost(component.getType());
        builder.append("new ");
        CodeGenerationHelper.appendClassName(builder, outermostType, imports);
        builder.append("[]");
        for (int i = 0; i < numDimensions; i++) {
            builder.append("[]");
        }
        builder.append("{");
        CodegenExpressionBuilder.renderExpressions(builder, expressions, imports, isInnerClass);
        builder.append("}");
    }

    public void mergeClasses(Set<Class> classes) {
        component.traverseClasses(classes::add);
        for (CodegenExpression expression : expressions) {
            expression.mergeClasses(classes);
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        traverseMultiple(expressions, consumer);
    }
}
