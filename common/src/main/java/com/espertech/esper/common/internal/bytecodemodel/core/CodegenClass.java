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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodegenClass {
    private final CodegenClassType classType;
    private final String packageName;
    private final String className;
    private final CodegenClassInterfacesAndExtension supers = new CodegenClassInterfacesAndExtension();
    private final CodegenCtor optionalCtor;
    private final List<CodegenTypedParam> explicitMembers;
    private final CodegenClassMethods methods;
    private final List<CodegenInnerClass> innerClasses;

    public CodegenClass(CodegenClassType classType,
                        Class optionalInterfaceImplemented,
                        String className,
                        CodegenClassScope codegenClassScope,
                        List<CodegenTypedParam> explicitMembers,
                        CodegenCtor optionalCtor,
                        CodegenClassMethods methods,
                        List<CodegenInnerClass> innerClasses) {
        this(classType, className, codegenClassScope, explicitMembers, optionalCtor, methods, innerClasses);
        if (optionalInterfaceImplemented != null) {
            if (optionalInterfaceImplemented.isInterface()) {
                supers.addInterfaceImplemented(optionalInterfaceImplemented);
            } else {
                supers.setClassExtended(optionalInterfaceImplemented);
            }
        }
    }

    public CodegenClass(CodegenClassType classType,
                        String className,
                        CodegenClassScope codegenClassScope,
                        List<CodegenTypedParam> explicitMembers,
                        CodegenCtor optionalCtor,
                        CodegenClassMethods methods,
                        List<CodegenInnerClass> innerClasses) {
        this.classType = classType;
        this.packageName = codegenClassScope.getPackageScope().getPackageName();
        this.className = className;
        this.explicitMembers = explicitMembers;
        this.optionalCtor = optionalCtor;
        this.methods = methods;

        List<CodegenInnerClass> allInnerClasses = new ArrayList<>(innerClasses);
        allInnerClasses.addAll(codegenClassScope.getAdditionalInnerClasses());
        this.innerClasses = allInnerClasses;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public CodegenClassInterfacesAndExtension getSupers() {
        return supers;
    }

    public List<CodegenTypedParam> getExplicitMembers() {
        return explicitMembers;
    }

    public List<CodegenMethodWGraph> getPublicMethods() {
        return methods.getPublicMethods();
    }

    public List<CodegenMethodWGraph> getPrivateMethods() {
        return methods.getPrivateMethods();
    }

    public List<CodegenInnerClass> getInnerClasses() {
        return innerClasses;
    }

    public void addInnerClass(CodegenInnerClass innerClass) {
        innerClasses.add(innerClass);
    }

    public CodegenCtor getOptionalCtor() {
        return optionalCtor;
    }

    public CodegenClassType getClassType() {
        return classType;
    }

    public Set<Class> getReferencedClasses() {
        Set<Class> classes = new HashSet<>();
        addReferencedClasses(supers, methods, classes);
        addReferencedClasses(explicitMembers, classes);
        if (optionalCtor != null) {
            optionalCtor.mergeClasses(classes);
        }

        for (CodegenInnerClass inner : innerClasses) {
            addReferencedClasses(inner.getSupers(), inner.getMethods(), classes);
            addReferencedClasses(inner.getExplicitMembers(), classes);
            if (inner.getCtor() != null) {
                inner.getCtor().mergeClasses(classes);
            }
        }
        return classes;
    }

    public String toString() {
        return "CodegenClass{" +
            "className='" + className + '\'' +
            '}';
    }

    private static void addReferencedClasses(CodegenClassInterfacesAndExtension supers, CodegenClassMethods methods, Set<Class> classes) {
        supers.addReferenced(classes);
        for (CodegenMethodWGraph publicMethod : methods.getPublicMethods()) {
            publicMethod.mergeClasses(classes);
        }
        for (CodegenMethodWGraph privateMethod : methods.getPrivateMethods()) {
            privateMethod.mergeClasses(classes);
        }
    }

    private static void addReferencedClasses(List<CodegenTypedParam> names, Set<Class> classes) {
        for (CodegenTypedParam param : names) {
            param.mergeClasses(classes);
        }
    }
}
