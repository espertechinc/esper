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

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenIndent;
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.*;

public class CodegenStatementIf extends CodegenStatementWBlockBase {
    private List<CodegenStatementIfConditionBlock> blocks = new ArrayList<CodegenStatementIfConditionBlock>();
    private CodegenBlock optionalElse;

    public CodegenStatementIf(CodegenBlock parent) {
        super(parent);
    }

    public CodegenBlock ifBlock(CodegenExpression condition) {
        if (!blocks.isEmpty()) {
            throw new IllegalStateException("Use add-else instead");
        }
        CodegenBlock block = new CodegenBlock(this);
        blocks.add(new CodegenStatementIfConditionBlock(condition, block));
        return block;
    }

    public CodegenBlock addElseIf(CodegenExpression condition) {
        if (blocks.isEmpty()) {
            throw new IllegalStateException("Use if-block instead");
        }
        CodegenBlock block = new CodegenBlock(this);
        blocks.add(new CodegenStatementIfConditionBlock(condition, block));
        return block;
    }

    public CodegenBlock addElse() {
        if (blocks.isEmpty()) {
            throw new IllegalStateException("Use if-block instead");
        }
        if (optionalElse != null) {
            throw new IllegalStateException("Else already found");
        }
        optionalElse = new CodegenBlock(this);
        return optionalElse;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {

        Iterator<CodegenStatementIfConditionBlock> it = blocks.iterator();
        CodegenStatementIfConditionBlock first = it.next();
        first.render(builder, imports, isInnerClass, level, indent);

        while (it.hasNext()) {
            builder.append(" else ");
            it.next().render(builder, imports, isInnerClass, level, indent);
        }

        if (optionalElse != null) {
            builder.append(" else {\n");
            optionalElse.render(builder, imports, isInnerClass, level + 1, indent);
            indent.indent(builder, level);
            builder.append("}");
        }
        builder.append("\n");
    }

    public void mergeClasses(Set<Class> classes) {
        for (CodegenStatementIfConditionBlock pair : blocks) {
            pair.mergeClasses(classes);
        }
        if (optionalElse != null) {
            optionalElse.mergeClasses(classes);
        }
    }
}
