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

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;

import java.util.*;

public class CodegenClass {
    private final String packageName;
    private final String className;
    private final Class interfaceImplemented;
    private final IdentityHashMap<Object, CodegenMember> members;
    private final CodegenClassMethods methods;

    public CodegenClass(Class interfaceClass, CodegenClassScope codegenClassScope, String engineURI, CodegenClassMethods methods) {
        this("com.espertech.esper.generated.uri_" + engineURI,
                interfaceClass.getSimpleName() + "_" + CodeGenerationIDGenerator.generateClass(),
                interfaceClass,
                codegenClassScope.getMembers(),
                methods);
    }

    private CodegenClass(String packageName, String className, Class interfaceImplemented, IdentityHashMap<Object, CodegenMember> members, CodegenClassMethods methods) {
        this.packageName = packageName;
        this.className = className;
        this.interfaceImplemented = interfaceImplemented;
        this.members = members;
        this.methods = methods;
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

    public IdentityHashMap<Object, CodegenMember> getMembers() {
        return members;
    }

    public List<CodegenMethod> getPublicMethods() {
        return methods.getPublicMethods();
    }

    public List<CodegenMethod> getPrivateMethods() {
        return methods.getPrivateMethods();
    }

    public Set<Class> getReferencedClasses() {
        Set<Class> classes = new HashSet<>();
        classes.add(interfaceImplemented);
        for (Map.Entry<Object, CodegenMember> memberEntry : members.entrySet()) {
            memberEntry.getValue().mergeClasses(classes);
        }
        for (CodegenMethod publicMethod : methods.getPublicMethods()) {
            publicMethod.mergeClasses(classes);
        }
        for (CodegenMethod privateMethod : methods.getPrivateMethods()) {
            privateMethod.mergeClasses(classes);
        }
        return classes;
    }
}
