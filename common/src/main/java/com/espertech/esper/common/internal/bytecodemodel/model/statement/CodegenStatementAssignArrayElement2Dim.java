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
package com.espertech.esper.common.internal.bytecodemodel.model.statement;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CodegenStatementAssignArrayElement2Dim extends CodegenStatementBase {
    private final CodegenExpression array;
    private final CodegenExpression indexOne;
    private final CodegenExpression indexTwo;
    private final CodegenExpression expression;

    public CodegenStatementAssignArrayElement2Dim(CodegenExpression array, CodegenExpression indexOne, CodegenExpression indexTwo, CodegenExpression expression) {
        this.array = array;
        this.indexOne = indexOne;
        this.indexTwo = indexTwo;
        this.expression = expression;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        array.render(builder, imports, isInnerClass);
        builder.append("[");
        indexOne.render(builder, imports, isInnerClass);
        builder.append("][");
        indexTwo.render(builder, imports, isInnerClass);
        builder.append("]=");
        expression.render(builder, imports, isInnerClass);
    }

    public void mergeClasses(Set<Class> classes) {
        array.mergeClasses(classes);
        indexOne.mergeClasses(classes);
        indexTwo.mergeClasses(classes);
        expression.mergeClasses(classes);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        consumer.accept(array);
        consumer.accept(indexOne);
        consumer.accept(indexTwo);
        consumer.accept(expression);
    }
}
