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

public class CodegenStatementSwitch extends CodegenStatementWBlockBase {
    private final CodegenExpression switchExpression;
    private final CodegenExpression[] options;
    private final CodegenBlock[] blocks;
    private final CodegenBlock defaultBlock;
    private final boolean blocksReturnValues;
    private final boolean withDefaultUnsupported;

    public CodegenStatementSwitch(CodegenBlock parent, CodegenExpression switchExpression, CodegenExpression[] options, boolean blocksReturnValues, boolean withDefaultUnsupported) {
        super(parent);
        this.switchExpression = switchExpression;
        this.options = options;
        blocks = new CodegenBlock[options.length];
        for (int i = 0; i < options.length; i++) {
            blocks[i] = new CodegenBlock(this);
        }
        this.blocksReturnValues = blocksReturnValues;
        this.withDefaultUnsupported = withDefaultUnsupported;
        this.defaultBlock = new CodegenBlock(this);
    }

    public CodegenBlock[] getBlocks() {
        return blocks;
    }

    public CodegenBlock getDefaultBlock() {
        return defaultBlock;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("switch(");
        switchExpression.render(builder, imports, isInnerClass);
        builder.append(") {\n");

        for (int i = 0; i < options.length; i++) {
            indent.indent(builder, level + 1);
            builder.append("case ");
            options[i].render(builder, imports, isInnerClass);
            builder.append(": {\n");
            blocks[i].render(builder, imports, isInnerClass, level + 2, indent);

            if (!blocksReturnValues) {
                indent.indent(builder, level + 2);
                builder.append("break;\n");
            }

            indent.indent(builder, level + 1);
            builder.append("}\n");
        }

        builder.append("default: ");
        if (withDefaultUnsupported) {
            indent.indent(builder, level + 1);
            builder.append("throw new UnsupportedOperationException();\n");
        } else {
            defaultBlock.render(builder, imports, isInnerClass, level + 2, indent);
        }

        indent.indent(builder, level);
        builder.append("}\n");
    }

    public void mergeClasses(Set<Class> classes) {
        switchExpression.mergeClasses(classes);
        for (int i = 0; i < blocks.length; i++) {
            blocks[i].mergeClasses(classes);
        }
        for (int i = 0; i < options.length; i++) {
            options[i].mergeClasses(classes);
        }
        if (defaultBlock != null) {
            defaultBlock.mergeClasses(classes);
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        consumer.accept(switchExpression);
        for (int i = 0; i < blocks.length; i++) {
            blocks[i].traverseExpressions(consumer);
        }
        for (int i = 0; i < options.length; i++) {
            consumer.accept(options[i]);
        }
        if (defaultBlock != null) {
            defaultBlock.traverseExpressions(consumer);
        }
    }
}
