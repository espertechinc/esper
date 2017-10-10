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

import java.util.Map;
import java.util.Set;

public class CodegenStatementSwitch extends CodegenStatementWBlockBase {
    private final String ref;
    private final int numOptions;
    private final CodegenBlock[] blocks;
    private final boolean blocksReturnValues;

    public CodegenStatementSwitch(CodegenBlock parent, String ref, int numOptions, boolean blocksReturnValues) {
        super(parent);
        this.ref = ref;
        this.numOptions = numOptions;
        blocks = new CodegenBlock[numOptions];
        for (int i = 0; i < numOptions; i++) {
            blocks[i] = new CodegenBlock(this);
        }
        this.blocksReturnValues = blocksReturnValues;
    }

    public CodegenBlock[] getBlocks() {
        return blocks;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("switch(").append(ref).append(") {\n");

        for (int i = 0; i < numOptions; i++) {
            indent.indent(builder, level + 1);
            builder.append("case ").append(i).append(": {\n");
            blocks[i].render(builder, imports, isInnerClass, level + 2, indent);

            if (!blocksReturnValues) {
                indent.indent(builder, level + 2);
                builder.append("break;\n");
            }

            indent.indent(builder, level + 1);
            builder.append("}\n");
        }

        indent.indent(builder, level + 1);
        builder.append("default: throw new UnsupportedOperationException();\n");

        indent.indent(builder, level);
        builder.append("}\n");
    }

    public void mergeClasses(Set<Class> classes) {
        for (int i = 0; i < numOptions; i++) {
            blocks[i].mergeClasses(classes);
        }
    }
}
