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

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionCastExpression implements CodegenExpression {
    private final EPTypeClass clazz;
    private final String typeName;
    private final CodegenExpression expression;

    public CodegenExpressionCastExpression(EPTypeClass clazz, CodegenExpression expression) {
        if (clazz == null) {
            throw new IllegalArgumentException("Cast-to class is a null value");
        }
        this.clazz = clazz;
        this.typeName = null;
        this.expression = expression;
    }

    public CodegenExpressionCastExpression(String typeName, CodegenExpression expression) {
        if (typeName == null) {
            throw new IllegalArgumentException("Cast-to class is a null value");
        }
        this.clazz = null;
        this.typeName = typeName;
        this.expression = expression;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("((");
        if (clazz != null) {
            appendClassName(builder, clazz, imports, false); // Disallow array-type-parameterized
        } else {
            appendClassName(builder, typeName);
        }
        builder.append(")");
        expression.render(builder, imports, isInnerClass);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        if (clazz != null) {
            clazz.traverseClasses(classes::add);
        }
        expression.mergeClasses(classes);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        consumer.accept(expression);
    }
}
