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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenStatementDeclareVar extends CodegenStatementBase {
    private final EPTypeClass clazz;
    private final String typeName;
    private final String var;
    private final CodegenExpression optionalInitializer;

    public CodegenStatementDeclareVar(EPTypeClass clazz, String var, CodegenExpression optionalInitializer) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        this.clazz = clazz;
        this.typeName = null;
        this.var = var;
        this.optionalInitializer = optionalInitializer;
    }

    public CodegenStatementDeclareVar(String typeName, String var, CodegenExpression optionalInitializer) {
        if (typeName == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        this.clazz = null;
        this.typeName = typeName;
        this.var = var;
        this.optionalInitializer = optionalInitializer;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        if (clazz != null) {
            appendClassName(builder, clazz, imports);
        } else {
            appendClassName(builder, typeName);
        }
        builder.append(" ").append(var);
        if (optionalInitializer != null) {
            builder.append("=");
            optionalInitializer.render(builder, imports, isInnerClass);
        }
    }

    public void mergeClasses(Set<Class> classes) {
        if (clazz != null) {
            clazz.traverseClasses(classes::add);
        }
        if (optionalInitializer != null) {
            optionalInitializer.mergeClasses(classes);
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        if (optionalInitializer != null) {
            consumer.accept(optionalInitializer);
        }
    }
}
