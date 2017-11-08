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
    private final CodegenCtor optionalCtor;
    private final List<CodegenTypedParam> explicitMembers;
    private final IdentityHashMap<Object, CodegenMember> implicitMembers;
    private final CodegenClassMethods methods;
    private final List<CodegenInnerClass> innerClasses;

    public CodegenClass(Class interfaceClass, String packageName, String className, CodegenClassScope codegenClassScope, List<CodegenTypedParam> explicitMembers, CodegenCtor optionalCtor, CodegenClassMethods methods, List<CodegenInnerClass> innerClasses) {
        this(packageName,
                className,
                interfaceClass,
                explicitMembers,
                codegenClassScope.getMembers(),
                optionalCtor,
                methods,
                innerClasses);
    }

    private CodegenClass(String packageName, String className, Class interfaceImplemented, List<CodegenTypedParam> explicitMembers, IdentityHashMap<Object, CodegenMember> implicitMembers, CodegenCtor optionalCtor, CodegenClassMethods methods, List<CodegenInnerClass> innerClasses) {
        this.packageName = packageName;
        this.className = className;
        this.interfaceImplemented = interfaceImplemented;
        this.explicitMembers = explicitMembers;
        this.implicitMembers = implicitMembers;
        this.optionalCtor = optionalCtor;
        this.methods = methods;
        this.innerClasses = innerClasses;
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

    public List<CodegenTypedParam> getExplicitMembers() {
        return explicitMembers;
    }

    public IdentityHashMap<Object, CodegenMember> getImplicitMembers() {
        return implicitMembers;
    }

    public List<CodegenMethod> getPublicMethods() {
        return methods.getPublicMethods();
    }

    public List<CodegenMethod> getPrivateMethods() {
        return methods.getPrivateMethods();
    }

    public List<CodegenInnerClass> getInnerClasses() {
        return innerClasses;
    }

    public CodegenCtor getOptionalCtor() {
        return optionalCtor;
    }

    public Set<Class> getReferencedClasses() {
        Set<Class> classes = new HashSet<>();
        addReferencedClasses(interfaceImplemented, implicitMembers, methods, classes);
        addReferencedClasses(explicitMembers, classes);
        if (optionalCtor != null) {
            optionalCtor.mergeClasses(classes);
        }

        for (CodegenInnerClass inner : innerClasses) {
            addReferencedClasses(inner.getInterfaceImplemented(), inner.getImplicitMembers(), inner.getMethods(), classes);
            addReferencedClasses(inner.getExplicitMembers(), classes);
            if (inner.getCtor() != null) {
                inner.getCtor().mergeClasses(classes);
            }
        }
        return classes;
    }

    private static void addReferencedClasses(Class interfaceImplemented, Map<Object, CodegenMember> members, CodegenClassMethods methods, Set<Class> classes) {
        if (interfaceImplemented != null) {
            classes.add(interfaceImplemented);
        }
        for (Map.Entry<Object, CodegenMember> memberEntry : members.entrySet()) {
            memberEntry.getValue().mergeClasses(classes);
        }
        for (CodegenMethod publicMethod : methods.getPublicMethods()) {
            publicMethod.mergeClasses(classes);
        }
        for (CodegenMethod privateMethod : methods.getPrivateMethods()) {
            privateMethod.mergeClasses(classes);
        }
    }

    private static void addReferencedClasses(List<CodegenTypedParam> names, Set<Class> classes) {
        for (CodegenTypedParam param : names) {
            param.mergeClasses(classes);
        }
    }
}
