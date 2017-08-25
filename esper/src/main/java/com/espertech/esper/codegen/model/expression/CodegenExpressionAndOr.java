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

public class CodegenExpressionAndOr implements CodegenExpression {
    private final boolean isAnd;
    private final CodegenExpression first;
    private final CodegenExpression second;
    private final CodegenExpression[] optionalMore;

    public CodegenExpressionAndOr(boolean isAnd, CodegenExpression first, CodegenExpression second, CodegenExpression[] optionalMore) {
        this.isAnd = isAnd;
        this.first = first;
        this.second = second;
        this.optionalMore = optionalMore;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        first.render(builder, imports, isInnerClass);
        builder.append(isAnd ? "&&" : "||");
        second.render(builder, imports, isInnerClass);

        if (optionalMore != null) {
            for (CodegenExpression expr : optionalMore) {
                builder.append(isAnd ? "&&" : "||");
                expr.render(builder, imports, isInnerClass);
            }
        }
    }

    public void mergeClasses(Set<Class> classes) {
        first.mergeClasses(classes);
        second.mergeClasses(classes);

        if (optionalMore != null) {
            for (CodegenExpression expr : optionalMore) {
                expr.mergeClasses(classes);
            }
        }
    }
}
