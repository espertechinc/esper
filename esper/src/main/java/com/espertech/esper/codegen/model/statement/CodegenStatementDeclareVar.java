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

import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenStatementDeclareVar extends CodegenStatementBase {
    private final Class clazz;
    private final String var;
    private final CodegenExpression initializer;

    public CodegenStatementDeclareVar(Class clazz, String var, CodegenExpression initializer) {
        this.clazz = clazz;
        this.var = var;
        this.initializer = initializer;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports) {
        appendClassName(builder, clazz, null, imports);
        builder.append(" ").append(var).append("=");
        initializer.render(builder, imports);
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(clazz);
        initializer.mergeClasses(classes);
    }
}
