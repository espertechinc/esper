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
package com.espertech.esper.common.internal.bytecodemodel.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenMethodWGraph {
    private final String name;
    private final CodegenMethodFootprint footprint;
    private final CodegenBlock block;
    private final boolean isPublic;
    private final List<Class> thrown;
    private final CodegenMethod originator;
    private boolean isStatic;

    public CodegenMethodWGraph(String name, CodegenMethodFootprint footprint, CodegenBlock block, boolean isPublic, List<Class> thrown, CodegenMethod originator) {
        this.name = name;
        this.footprint = footprint;
        this.block = block;
        this.isPublic = isPublic;
        this.thrown = thrown;
        this.originator = originator;
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
        if (isStatic) {
            builder.append("static ");
        }
        if (footprint.getReturnType() != null) {
            appendClassName(builder, footprint.getReturnType(), null, imports);
        } else {
            builder.append(footprint.getReturnTypeName());
        }
        builder.append(" ").append(name);
        builder.append("(");
        String delimiter = "";
        for (CodegenNamedParam param : footprint.getParams()) {
            builder.append(delimiter);
            param.render(builder, imports);
            delimiter = ",";
        }
        builder.append(")");
        if (!thrown.isEmpty()) {
            builder.append(" throws ");
            String delimiterThrown = "";
            for (Class ex : thrown) {
                builder.append(delimiterThrown);
                appendClassName(builder, ex, null, imports);
                delimiterThrown = ",";
            }
        }
        builder.append("{\n");
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

    public boolean isStatic() {
        return isStatic;
    }

    public CodegenMethodWGraph setStatic(boolean aStatic) {
        isStatic = aStatic;
        return this;
    }

    public CodegenMethod getOriginator() {
        return originator;
    }

    public String toString() {
        return "CodegenMethodWGraph{" +
            "name='" + name + '\'' +
            '}';
    }
}
