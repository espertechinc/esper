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
    private final CodegenClassInterfacesAndExtension supers = new CodegenClassInterfacesAndExtension();
    private final CodegenCtor ctor;
    private final List<CodegenTypedParam> explicitMembers;
    private final CodegenClassMethods methods;

    public CodegenInnerClass(String className, Class optionalInterfaceImplemented, CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassMethods methods) {
        this(className, ctor, explicitMembers, methods);
        if (optionalInterfaceImplemented != null) {
            if (optionalInterfaceImplemented.isInterface()) {
                supers.addInterfaceImplemented(optionalInterfaceImplemented);
            } else {
                supers.setClassExtended(optionalInterfaceImplemented);
            }
        }
    }

    public CodegenInnerClass(String className, CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassMethods methods) {
        this.className = className;
        this.ctor = ctor;
        this.explicitMembers = explicitMembers;
        this.methods = methods;
    }

    public String getClassName() {
        return className;
    }

    public CodegenClassInterfacesAndExtension getSupers() {
        return supers;
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

    public String toString() {
        return "CodegenInnerClass{" +
            "className='" + className + '\'' +
            '}';
    }
}
