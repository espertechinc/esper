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

import java.util.List;

public class CodegenInnerClass {
    private final String className;
    private final Class interfaceImplemented;
    private final CodegenCtor ctor;
    private final List<CodegenTypedParam> explicitMembers;
    private final CodegenClassMethods methods;
    private String interfaceGenericClass;

    public CodegenInnerClass(String className, Class interfaceImplemented, CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassMethods methods) {
        this.className = className;
        this.interfaceImplemented = interfaceImplemented;
        this.ctor = ctor;
        this.explicitMembers = explicitMembers;
        this.methods = methods;
    }

    public String getClassName() {
        return className;
    }

    public Class getInterfaceImplemented() {
        return interfaceImplemented;
    }

    public CodegenClassMethods getMethods() {
        return methods;
    }

    public CodegenCtor getCtor() {
        return ctor;
    }

    public List<CodegenTypedParam> getExplicitMembers() {
        return explicitMembers;
    }

    public String getInterfaceGenericClass() {
        return interfaceGenericClass;
    }

    public void setInterfaceGenericClass(String interfaceGenericClass) {
        this.interfaceGenericClass = interfaceGenericClass;
    }
}
