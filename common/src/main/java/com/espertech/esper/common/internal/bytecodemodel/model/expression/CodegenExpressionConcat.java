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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.traverseMultiple;

public class CodegenExpressionConcat implements CodegenExpression {
    private final CodegenExpression[] stringExpressions;

    public CodegenExpressionConcat(CodegenExpression[] stringExpressions) {
        this.stringExpressions = stringExpressions;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        boolean first = true;
        for (CodegenExpression expression : stringExpressions) {
            if (!first) {
                builder.append("+");
            }
            first = false;
            expression.render(builder, imports, isInnerClass);
        }
    }

    public void mergeClasses(Set<Class> classes) {
        for (CodegenExpression expr : stringExpressions) {
            expr.mergeClasses(classes);
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        traverseMultiple(stringExpressions, consumer);
    }
}
