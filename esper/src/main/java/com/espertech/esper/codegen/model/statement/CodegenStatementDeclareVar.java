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
    private final String typeName;
    private final Class optionalTypeVariable;
    private final String var;
    private final CodegenExpression optionalInitializer;

    public CodegenStatementDeclareVar(Class clazz, Class optionalTypeVariable, String var, CodegenExpression optionalInitializer) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        this.clazz = clazz;
        this.typeName = null;
        this.optionalTypeVariable = optionalTypeVariable;
        this.var = var;
        this.optionalInitializer = optionalInitializer;
    }

    public CodegenStatementDeclareVar(String typeName, Class optionalTypeVariable, String var, CodegenExpression optionalInitializer) {
        if (typeName == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        this.clazz = null;
        this.typeName = typeName;
        this.optionalTypeVariable = optionalTypeVariable;
        this.var = var;
        this.optionalInitializer = optionalInitializer;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        if (clazz != null) {
            appendClassName(builder, clazz, optionalTypeVariable, imports);
        } else {
            builder.append(typeName);
        }
        builder.append(" ").append(var);
        if (optionalInitializer != null) {
            builder.append("=");
            optionalInitializer.render(builder, imports, isInnerClass);
        }
    }

    public void mergeClasses(Set<Class> classes) {
        if (clazz != null) {
            classes.add(clazz);
        }
        if (optionalInitializer != null) {
            optionalInitializer.mergeClasses(classes);
        }
    }
}
