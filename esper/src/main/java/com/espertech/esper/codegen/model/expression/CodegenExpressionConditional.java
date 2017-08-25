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

public class CodegenExpressionConditional implements CodegenExpression {
    private final CodegenExpression condition;
    private final CodegenExpression expressionTrue;
    private final CodegenExpression expressionFalse;

    public CodegenExpressionConditional(CodegenExpression condition, CodegenExpression expressionTrue, CodegenExpression expressionFalse) {
        this.condition = condition;
        this.expressionTrue = expressionTrue;
        this.expressionFalse = expressionFalse;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("(");
        condition.render(builder, imports, isInnerClass);
        builder.append(" ? ");
        expressionTrue.render(builder, imports, isInnerClass);
        builder.append(" : ");
        expressionFalse.render(builder, imports, isInnerClass);
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        condition.mergeClasses(classes);
        expressionTrue.mergeClasses(classes);
        expressionFalse.mergeClasses(classes);
    }
}
