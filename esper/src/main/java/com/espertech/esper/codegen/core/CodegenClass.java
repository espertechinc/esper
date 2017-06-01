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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodegenClass {
    private final String packageName;
    private final String className;
    private final Class interfaceImplemented;
    private final List<CodegenMember> members;
    private final List<CodegenMethod> publicMethods;
    private final List<CodegenMethod> privateMethods;

    public CodegenClass(String packageName, String className, Class interfaceImplemented, List<CodegenMember> members, List<CodegenMethod> publicMethods, List<CodegenMethod> privateMethods) {
        this.packageName = packageName;
        this.className = className;
        this.interfaceImplemented = interfaceImplemented;
        this.members = members;
        this.publicMethods = publicMethods;
        this.privateMethods = privateMethods;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public Class getInterfaceImplemented() {
        return interfaceImplemented;
    }

    public List<CodegenMember> getMembers() {
        return members;
    }

    public List<CodegenMethod> getPublicMethods() {
        return publicMethods;
    }

    public List<CodegenMethod> getPrivateMethods() {
        return privateMethods;
    }

    public Set<Class> getReferencedClasses() {
        Set<Class> classes = new HashSet<>();
        classes.add(interfaceImplemented);
        for (CodegenMember member : members) {
            member.mergeClasses(classes);
        }
        for (CodegenMethod publicMethod : publicMethods) {
            publicMethod.mergeClasses(classes);
        }
        for (CodegenMethod privateMethod : privateMethods) {
            privateMethod.mergeClasses(classes);
        }
        return classes;
    }
}
