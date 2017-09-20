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

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenCtorParam {
    private final String typeName;
    private final Class type;
    private final String name;

    public CodegenCtorParam(String typeName, Class type, String name) {
        if (type == null && typeName == null) {
            throw new IllegalArgumentException("Invalid null type");
        }
        this.typeName = typeName;
        this.type = type;
        this.name = name;
    }

    public CodegenCtorParam(Class type, String name) {
        this(null, type, name);
    }

    public CodegenCtorParam(String type, String name) {
        this(type, null, name);
    }

    public String getName() {
        return name;
    }

    public void renderAsParameter(StringBuilder builder, Map<Class, String> imports) {
        if (type != null) {
            appendClassName(builder, type, null, imports);
        } else {
            builder.append(typeName);
        }
        builder.append(" ").append(name);
    }

    public void mergeClasses(Set<Class> classes) {
        if (type != null) {
            classes.add(type);
        }
    }

    public void renderAsMember(StringBuilder builder, Map<Class, String> imports) {
        if (type != null) {
            appendClassName(builder, type, null, imports);
        } else {
            builder.append(typeName);
        }
        builder.append(" ").append(name);

    }
}
