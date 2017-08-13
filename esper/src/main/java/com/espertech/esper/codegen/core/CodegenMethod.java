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
package com.espertech.esper.codegen.core;

import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSet;

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenMethod {
    private final CodegenMethodFootprint footprint;
    private CodegenBlock block;

    public CodegenMethod(CodegenMethodFootprint footprint) {
        this.footprint = footprint;
    }

    public CodegenMethod(CodegenMethodFootprint footprint, CodegenExpression expression) {
        this.footprint = footprint;
        statements().methodReturn(expression);
    }

    public CodegenMethodFootprint getFootprint() {
        return footprint;
    }

    public CodegenBlock statements() {
        allocateBlock();
        return block;
    }

    public void mergeClasses(Set<Class> classes) {
        footprint.mergeClasses(classes);
        allocateBlock();
        block.mergeClasses(classes);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isPublic, CodegenIndent indent) {
        allocateBlock();

        if (footprint.getOptionalComment() != null) {
            indent.indent(builder, 1);
            builder.append("// ").append(footprint.getOptionalComment()).append("\n");
        }

        indent.indent(builder, 1);
        if (isPublic) {
            builder.append("public ");
        }
        appendClassName(builder, footprint.getReturnType(), null, imports);
        builder.append(" ");
        footprint.getMethodId().render(builder);
        builder.append("(");
        String delimiter = "";
        for (CodegenParamSet param : footprint.getParams()) {
            builder.append(delimiter);
            param.render(builder, imports, indent, footprint.getOptionalComment());
            delimiter = ",";
        }
        builder.append("){\n");
        block.render(builder, imports, 2, indent);
        indent.indent(builder, 1);
        builder.append("}\n");
    }

    private void allocateBlock() {
        if (block == null) {
            block = new CodegenBlock(this);
        }
    }
}
