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
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;

public class CodegenStatementForInt extends CodegenStatementWBlockBase {
    private final String ref;
    private final CodegenExpression upperLimit;
    private CodegenBlock block;

    public CodegenStatementForInt(CodegenBlock parent, String ref, CodegenExpression upperLimit) {
        super(parent);
        this.ref = ref;
        this.upperLimit = upperLimit;
    }

    public void setBlock(CodegenBlock block) {
        this.block = block;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        builder.append("for (int ").append(ref).append("=0; i<");
        upperLimit.render(builder, imports);
        builder.append("; i++) {\n");
        block.render(builder, imports);
        builder.append("}\n");
    }

    public void mergeClasses(Set<Class> classes) {
        block.mergeClasses(classes);
        upperLimit.mergeClasses(classes);
    }
}
