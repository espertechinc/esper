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

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CodegenExpressionAssign implements CodegenExpression {
    private final CodegenExpression lhs;
    private final CodegenExpression rhs;

    public CodegenExpressionAssign(CodegenExpression lhs, CodegenExpression rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        lhs.render(builder, imports, isInnerClass);
        builder.append("=");
        rhs.render(builder, imports, isInnerClass);
    }

    public void mergeClasses(Set<Class> classes) {
        lhs.mergeClasses(classes);
        rhs.mergeClasses(classes);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        consumer.accept(lhs);
        consumer.accept(rhs);
    }
}
