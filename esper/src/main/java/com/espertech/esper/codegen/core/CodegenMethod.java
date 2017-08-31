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

import com.espertech.esper.codegen.base.CodegenBlock;

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenMethod {
    private final String name;
    private final CodegenMethodFootprint footprint;
    private final CodegenBlock block;
    private final boolean isPublic;

    public CodegenMethod(String name, CodegenMethodFootprint footprint, CodegenBlock block, boolean isPublic) {
        this.name = name;
        this.footprint = footprint;
        this.block = block;
        this.isPublic = isPublic;
    }

    public void mergeClasses(Set<Class> classes) {
        footprint.mergeClasses(classes);
        block.mergeClasses(classes);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isPublic, boolean isInnerClass, CodegenIndent indent, int additionalIndent) {
        if (footprint.getOptionalComment() != null) {
            indent.indent(builder, 1 + additionalIndent);
            builder.append("// ").append(footprint.getOptionalComment()).append("\n");
        }

        indent.indent(builder, 1 + additionalIndent);
        if (isPublic) {
            builder.append("public ");
        }
        appendClassName(builder, footprint.getReturnType(), null, imports);
        builder.append(" ").append(name);
        builder.append("(");
        String delimiter = "";
        for (CodegenNamedParam param : footprint.getParams()) {
            builder.append(delimiter);
            param.render(builder, imports);
            delimiter = ",";
        }
        builder.append("){\n");
        block.render(builder, imports, isInnerClass, 2 + additionalIndent, indent);
        indent.indent(builder, 1 + additionalIndent);
        builder.append("}\n");
    }

    public String getName() {
        return name;
    }

    public CodegenMethodFootprint getFootprint() {
        return footprint;
    }

    public CodegenBlock getBlock() {
        return block;
    }

    public boolean isPublic() {
        return isPublic;
    }
}
