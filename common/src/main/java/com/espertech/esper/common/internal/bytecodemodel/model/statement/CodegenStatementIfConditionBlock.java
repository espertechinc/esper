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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenIndent;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CodegenStatementIfConditionBlock {
    private final CodegenExpression condition;
    private final CodegenBlock block;

    public CodegenStatementIfConditionBlock(CodegenExpression condition, CodegenBlock block) {
        this.condition = condition;
        this.block = block;
    }

    public CodegenExpression getCondition() {
        return condition;
    }

    public CodegenBlock getBlock() {
        return block;
    }

    public void mergeClasses(Set<Class> classes) {
        condition.mergeClasses(classes);
        block.mergeClasses(classes);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("if (");
        condition.render(builder, imports, isInnerClass);
        builder.append(") {\n");
        block.render(builder, imports, isInnerClass, level + 1, indent);
        indent.indent(builder, level);
        builder.append("}");
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        consumer.accept(condition);
        block.traverseExpressions(consumer);
    }
}
