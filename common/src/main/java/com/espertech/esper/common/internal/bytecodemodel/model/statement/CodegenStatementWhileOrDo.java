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

public class CodegenStatementWhileOrDo extends CodegenStatementWBlockBase {
    private final CodegenExpression condition;
    private final boolean isWhile;

    private CodegenBlock block;

    public CodegenStatementWhileOrDo(CodegenBlock parent, CodegenExpression condition, boolean isWhile) {
        super(parent);
        this.condition = condition;
        this.isWhile = isWhile;
    }

    public void setBlock(CodegenBlock block) {
        this.block = block;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        if (isWhile) {
            builder.append("while (");
            condition.render(builder, imports, isInnerClass);
            builder.append(") {\n");
        } else {
            builder.append("do {\n");
        }
        block.render(builder, imports, isInnerClass, level + 1, indent);
        indent.indent(builder, level);
        builder.append("}\n");
        if (!isWhile) {
            indent.indent(builder, level);
            builder.append("while (");
            condition.render(builder, imports, isInnerClass);
            builder.append(");\n");
        }
    }

    public void mergeClasses(Set<Class> classes) {
        block.mergeClasses(classes);
        condition.mergeClasses(classes);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        block.traverseExpressions(consumer);
        consumer.accept(condition);
    }
}
