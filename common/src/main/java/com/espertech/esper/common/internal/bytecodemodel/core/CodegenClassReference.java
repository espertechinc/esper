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

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenClassReference {
    private final EPTypeClass clazz;
    private final String className;

    public CodegenClassReference(String className) {
        this.clazz = null;
        if (className == null) {
            throw new IllegalArgumentException();
        }
        this.className = className;
    }

    public CodegenClassReference(EPTypeClass clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException();
        }
        this.clazz = clazz;
        this.className = null;
    }

    public EPTypeClass getClazz() {
        return clazz;
    }

    public String getClassName() {
        return className;
    }

    public void addReferenced(Set<Class> classes) {
        if (clazz != null) {
            clazz.traverseClasses(classes::add);
        }
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        if (clazz != null) {
            appendClassName(builder, clazz, imports);
        } else {
            builder.append(className);
        }
    }
}
