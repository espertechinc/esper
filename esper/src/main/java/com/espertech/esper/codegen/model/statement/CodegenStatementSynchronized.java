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

import java.util.Map;
import java.util.Set;

public class CodegenStatementSynchronized extends CodegenStatementWBlockBase {
    private CodegenExpression expression;
    private CodegenBlock block;

    public CodegenStatementSynchronized(CodegenBlock parent, CodegenExpression expression) {
        super(parent);
        this.expression = expression;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("synchronized (");
        expression.render(builder, imports, isInnerClass);
        builder.append(") {\n");
        block.render(builder, imports, isInnerClass, level + 1, indent);
        indent.indent(builder, level);
        builder.append("}\n");
    }

    public void mergeClasses(Set<Class> classes) {
        expression.mergeClasses(classes);
        block.mergeClasses(classes);
    }

    public CodegenBlock makeBlock() {
        if (block != null) {
            throw new IllegalStateException("Block already allocated");
        }
        block = new CodegenBlock(this);
        return block;
    }
}
