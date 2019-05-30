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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenStatementTryCatch extends CodegenStatementWBlockBase {

    private CodegenBlock tryBlock;
    private List<CodegenStatementTryCatchCatchBlock> catchBlocks = new ArrayList<>(1);
    private CodegenBlock finallyBlock;

    public CodegenStatementTryCatch(CodegenBlock parent) {
        super(parent);
    }

    public void setTry(CodegenBlock block) {
        if (tryBlock != null) {
            throw new IllegalStateException("Try-block already provided");
        }
        tryBlock = block;
    }

    public CodegenBlock addCatch(Class ex, String name) {
        CodegenBlock block = new CodegenBlock(this);
        catchBlocks.add(new CodegenStatementTryCatchCatchBlock(ex, name, block));
        return block;
    }

    public CodegenBlock tryFinally() {
        if (finallyBlock != null) {
            throw new IllegalStateException("Finally already set");
        }
        finallyBlock = new CodegenBlock(this);
        return finallyBlock;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("try {\n");
        tryBlock.render(builder, imports, isInnerClass, level + 1, indent);
        indent.indent(builder, level);
        builder.append("}");

        String delimiter = "";
        for (CodegenStatementTryCatchCatchBlock pair : catchBlocks) {
            builder.append(delimiter);
            builder.append(" catch (");
            appendClassName(builder, pair.getEx(), null, imports);
            builder.append(' ');
            builder.append(pair.getName());
            builder.append(") {\n");
            pair.getBlock().render(builder, imports, isInnerClass, level + 1, indent);
            indent.indent(builder, level);
            builder.append("}");
            delimiter = "\n";
        }
        if (finallyBlock != null) {
            builder.append("\n");
            indent.indent(builder, level);
            builder.append("finally {\n");
            finallyBlock.render(builder, imports, isInnerClass, level + 1, indent);
            indent.indent(builder, level);
            builder.append("}");
        }
        builder.append("\n");
    }

    public void mergeClasses(Set<Class> classes) {
        tryBlock.mergeClasses(classes);
        for (CodegenStatementTryCatchCatchBlock pair : catchBlocks) {
            pair.mergeClasses(classes);
        }
        if (finallyBlock != null) {
            finallyBlock.mergeClasses(classes);
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        tryBlock.traverseExpressions(consumer);
        for (CodegenStatementTryCatchCatchBlock pair : catchBlocks) {
            pair.traverseExpressions(consumer);
        }
        if (finallyBlock != null) {
            finallyBlock.traverseExpressions(consumer);
        }
    }
}
