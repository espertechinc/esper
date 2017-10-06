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

import com.espertech.esper.codegen.base.CodegenMember;

import java.util.List;
import java.util.Map;

public class CodegenInnerClass {
    private final String className;
    private final Class interfaceImplemented;
    private final CodegenCtor ctor;
    private final List<CodegenTypedParam> explicitMembers;
    private final Map<Object, CodegenMember> implicitMembers;
    private final CodegenClassMethods methods;

    public CodegenInnerClass(String className, Class interfaceImplemented, CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, Map<Object, CodegenMember> implicitMembers, CodegenClassMethods methods) {
        this.className = className;
        this.interfaceImplemented = interfaceImplemented;
        this.ctor = ctor;
        this.implicitMembers = implicitMembers;
        this.explicitMembers = explicitMembers;
        this.methods = methods;
    }

    public String getClassName() {
        return className;
    }

    public Class getInterfaceImplemented() {
        return interfaceImplemented;
    }

    public Map<Object, CodegenMember> getImplicitMembers() {
        return implicitMembers;
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
}
