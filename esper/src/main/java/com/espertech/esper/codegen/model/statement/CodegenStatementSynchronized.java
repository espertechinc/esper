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

import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenIndent;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

import java.util.Map;
import java.util.Set;

public class CodegenStatementSynchronized extends CodegenStatementWBlockBase {
    private CodegenExpressionRef ref;
    private CodegenBlock block;

    public CodegenStatementSynchronized(CodegenBlock parent, CodegenExpressionRef ref) {
        super(parent);
        this.ref = ref;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, int level, CodegenIndent indent) {
        builder.append("synchronized (");
        ref.render(builder, imports);
        builder.append(") {\n");
        block.render(builder, imports, level + 1, indent);
        indent.indent(builder, level);
        builder.append("}\n");
    }

    public void mergeClasses(Set<Class> classes) {
        ref.mergeClasses(classes);
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
