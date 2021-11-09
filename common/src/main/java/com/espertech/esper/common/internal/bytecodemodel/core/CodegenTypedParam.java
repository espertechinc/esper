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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenTypedParam {
    private final String typeName;
    private final EPTypeClass type;
    private final String name;
    private final boolean memberWhenCtorParam;
    private final boolean isPublic;
    private boolean isFinal = true;
    private boolean isStatic = false;
    private CodegenExpression initializer = null;

    public CodegenTypedParam(String typeName, EPTypeClass type, String name, boolean memberWhenCtorParam, boolean isPublic) {
        if (type == null && typeName == null) {
            throw new IllegalArgumentException("Invalid null type");
        }
        this.typeName = typeName;
        this.type = type;
        this.name = name;
        this.memberWhenCtorParam = memberWhenCtorParam;
        this.isPublic = isPublic;
    }

    public CodegenTypedParam(String typeName, EPTypeClass type, String name) {
        this(typeName, type, name, true, false);
    }

    public CodegenTypedParam(EPTypeClass type, String name) {
        this(null, type, name);
    }

    public CodegenTypedParam(EPTypeClass type, String name, boolean memberWhenCtorParam) {
        this(null, type, name, memberWhenCtorParam, false);
    }

    public CodegenTypedParam(EPTypeClass type, String name, boolean memberWhenCtorParam, boolean isPublic) {
        this(null, type, name, memberWhenCtorParam, isPublic);
    }

    public CodegenTypedParam(String typeName, String name, boolean memberWhenCtorParam, boolean isPublic) {
        this(typeName, null, name, memberWhenCtorParam, isPublic);
    }

    public CodegenTypedParam(String type, String name) {
        this(type, null, name);
    }

    public CodegenTypedParam setFinal(boolean aFinal) {
        isFinal = aFinal;
        return this;
    }

    public CodegenTypedParam setStatic(boolean aStatic) {
        isStatic = aStatic;
        return this;
    }

    public String getName() {
        return name;
    }

    public void renderAsParameter(StringBuilder builder, Map<Class, String> imports) {
        if (type != null) {
            appendClassName(builder, type, imports);
        } else {
            appendClassName(builder, typeName);
        }
        builder.append(" ").append(name);
    }

    public void mergeClasses(Set<Class> classes) {
        if (type != null) {
            type.traverseClasses(classes::add);
        }
        if (initializer != null) {
            initializer.mergeClasses(classes);
        }
    }

    public void renderAsMember(StringBuilder builder, Map<Class, String> imports) {
        if (type != null) {
            appendClassName(builder, type, imports);
        } else {
            appendClassName(builder, typeName);
        }
        builder.append(" ").append(name);
    }

    public void renderType(StringBuilder builder, Map<Class, String> imports) {
        if (type != null) {
            appendClassName(builder, type, imports);
        } else {
            appendClassName(builder, typeName);
        }
    }

    public boolean isMemberWhenCtorParam() {
        return memberWhenCtorParam;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public CodegenExpression getInitializer() {
        return initializer;
    }

    public CodegenTypedParam setInitializer(CodegenExpression initializer) {
        this.initializer = initializer;
        return this;
    }

    public String toString() {
        return "CodegenTypedParam{" +
                "typeName='" + typeName + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", memberWhenCtorParam=" + memberWhenCtorParam +
                ", isPublic=" + isPublic +
                ", isFinal=" + isFinal +
                ", isStatic=" + isStatic +
                ", initializer=" + initializer +
                '}';
    }

    public void renderInitializer(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        if (initializer != null) {
            builder.append("=");
            initializer.render(builder, imports, isInnerClass);
        }
    }
}
