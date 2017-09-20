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
package com.espertech.esper.codegen.model.expression;

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenIndent;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.codegen.model.statement.CodegenStatementWBlockBase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionNewAnonymousClass extends CodegenStatementWBlockBase implements CodegenExpression {

    private final Class interfaceClass;
    private final Class returnType;
    private final String methodName;
    private final List<CodegenNamedParam> params;
    private final CodegenBlock block;

    public CodegenExpressionNewAnonymousClass(CodegenBlock parentBlock, Class interfaceClass, Class returnType, String methodName, List<CodegenNamedParam> params) {
        super(parentBlock);
        this.interfaceClass = interfaceClass;
        this.returnType = returnType;
        this.methodName = methodName;
        this.params = params;
        this.block = new CodegenBlock(this);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("new ");
        appendClassName(builder, interfaceClass, null, imports);
        builder.append("() {\n");

        indent.indent(builder, level);
        builder.append("public ");
        appendClassName(builder, returnType, null, imports);
        builder.append(" ");
        builder.append(methodName).append("(");
        CodegenNamedParam.render(builder, params, imports);
        builder.append(") {\n");

        indent.indent(builder, level);
        block.render(builder, imports, isInnerClass, level, indent);
        builder.append("}");
        builder.append("}");
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        render(builder, imports, isInnerClass, 4, new CodegenIndent(true));
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(interfaceClass);
        classes.add(returnType);
        for (CodegenNamedParam named : params) {
            classes.add(named.getType());
        }
    }

    public CodegenBlock getBlock() {
        return block;
    }
}
