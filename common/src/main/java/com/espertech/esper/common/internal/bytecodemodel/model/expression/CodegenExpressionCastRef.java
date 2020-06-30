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

public class CodegenExpressionCastRef implements CodegenExpression {
    private final EPTypeClass clazz;
    private final String ref;

    public CodegenExpressionCastRef(EPTypeClass clazz, String ref) {
        this.clazz = clazz;
        this.ref = ref;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("((");
        appendClassName(builder, clazz, imports);
        builder.append(")").append(ref).append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        clazz.traverseClasses(classes::add);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
    }
}
