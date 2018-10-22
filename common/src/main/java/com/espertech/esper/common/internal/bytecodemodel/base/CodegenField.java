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
package com.espertech.esper.common.internal.bytecodemodel.base;

import java.util.Set;

public class CodegenField {
    private final String clazz;
    private final String name;
    private final Class type;
    private final Class optionalTypeParam;
    private final boolean isFinal;

    public CodegenField(String clazz, String name, Class type, Class optionalTypeParam, boolean isFinal) {
        this.clazz = clazz;
        this.name = name;
        this.type = type;
        this.optionalTypeParam = optionalTypeParam;
        this.isFinal = isFinal;
    }

    public Class getOptionalTypeParam() {
        return optionalTypeParam;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenField that = (CodegenField) o;

        return name.equals(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(type);
        if (optionalTypeParam != null) {
            classes.add(optionalTypeParam);
        }
    }

    public String getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void render(StringBuilder builder) {
        builder.append(clazz).append('.').append(name);
    }
}
