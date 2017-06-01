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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenMethod {
    private final Class returnType;
    private final String methodName;
    private final List<CodegenNamedParam> params;
    private final String optionalComment;
    private CodegenBlock block;

    public CodegenMethod(Class returnType, String methodName, List<CodegenNamedParam> params, String optionalComment) {
        this.returnType = returnType;
        this.methodName = methodName;
        this.params = params;
        this.optionalComment = optionalComment;
    }

    public Class getReturnType() {
        return returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<CodegenNamedParam> getParams() {
        return params;
    }

    public CodegenBlock statements() {
        allocateBlock();
        return block;
    }

    public void mergeClasses(Set<Class> classes) {
        allocateBlock();
        classes.add(returnType);
        block.mergeClasses(classes);
        for (CodegenNamedParam param : params) {
            param.mergeClasses(classes);
        }
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isPublic) {
        allocateBlock();
        if (optionalComment != null) {
            builder.append("// ").append(optionalComment).append("\n");
        }
        if (isPublic) {
            builder.append("public ");
        }
        appendClassName(builder, returnType, null, imports);
        builder.append(" ").append(methodName).append("(");
        CodegenNamedParam.render(builder, params, imports);
        builder.append("){\n");
        block.render(builder, imports);
        builder.append("}\n");
    }

    private void allocateBlock() {
        if (block == null) {
            block = new CodegenBlock(this);
        }
    }
}
