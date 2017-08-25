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
package com.espertech.esper.codegen.model.statement;

import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;

public class CodegenStatementAssignArrayElement extends CodegenStatementBase {
    private final CodegenExpression array;
    private final CodegenExpression index;
    private final CodegenExpression expression;

    public CodegenStatementAssignArrayElement(CodegenExpression array, CodegenExpression index, CodegenExpression expression) {
        this.array = array;
        this.index = index;
        this.expression = expression;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        array.render(builder, imports, isInnerClass);
        builder.append("[");
        index.render(builder, imports, isInnerClass);
        builder.append("]=");
        expression.render(builder, imports, isInnerClass);
    }

    public void mergeClasses(Set<Class> classes) {
        array.mergeClasses(classes);
        index.mergeClasses(classes);
        expression.mergeClasses(classes);
    }
}
