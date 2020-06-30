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

public class CodegenExpressionNewInstance implements CodegenExpression {
    private final EPTypeClass clazz;
    private final CodegenExpression[] params;

    public CodegenExpressionNewInstance(EPTypeClass clazz, CodegenExpression[] params) {
        this.clazz = clazz;
        this.params = params;
        CodegenExpression.assertNonNullArgs(params);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("new ");
        CodeGenerationHelper.appendClassName(builder, clazz, imports);
        builder.append("(");
        CodegenExpressionBuilder.renderExpressions(builder, params, imports, isInnerClass);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        clazz.traverseClasses(clz -> classes.add(clz));
        CodegenExpressionBuilder.mergeClassesExpressions(classes, params);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        traverseMultiple(params, consumer);
    }
}
