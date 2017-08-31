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

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenStatementFor extends CodegenStatementWBlockBase {
    private final Class type;
    private final String name;
    private final CodegenExpression initialization;
    private final CodegenExpression termination;
    private final CodegenExpression increment;
    private CodegenBlock block;

    public CodegenStatementFor(CodegenBlock parent, Class type, String name, CodegenExpression initialization, CodegenExpression termination, CodegenExpression increment) {
        super(parent);
        this.type = type;
        this.name = name;
        this.initialization = initialization;
        this.termination = termination;
        this.increment = increment;
    }

    public void setBlock(CodegenBlock block) {
        this.block = block;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("for (");
        appendClassName(builder, type, null, imports);
        builder.append(" ").append(name).append("=");
        initialization.render(builder, imports, isInnerClass);
        builder.append("; ");
        termination.render(builder, imports, isInnerClass);
        builder.append("; ");
        increment.render(builder, imports, isInnerClass);
        builder.append(") {\n");
        block.render(builder, imports, isInnerClass, level + 1, indent);
        indent.indent(builder, level);
        builder.append("}\n");
    }

    public void mergeClasses(Set<Class> classes) {
        block.mergeClasses(classes);
        initialization.mergeClasses(classes);
        termination.mergeClasses(classes);
        increment.mergeClasses(classes);
    }
}
