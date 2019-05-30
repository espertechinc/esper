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

public class CodegenExpressionIncrementDecrement implements CodegenExpression {

    private final CodegenExpression ref;
    private final boolean increment;

    public CodegenExpressionIncrementDecrement(CodegenExpression ref, boolean increment) {
        this.ref = ref;
        this.increment = increment;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        ref.render(builder, imports, isInnerClass);
        builder.append(increment ? "++" : "--");
    }

    public void mergeClasses(Set<Class> classes) {
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        consumer.accept(ref);
    }
}
