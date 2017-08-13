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

import com.espertech.esper.codegen.model.method.CodegenLocalMethodBuilder;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class CodegenContext {

    private final boolean debug;
    private final IdentityHashMap<Object, CodegenMember> members = new IdentityHashMap<>();
    private final List<CodegenMethod> methods = new ArrayList<>();
    private int currentMemberNumber;
    private int currentMethodNumber;

    public CodegenContext(boolean debug) {
        this.debug = debug;
    }

    public <T> CodegenMember makeAddMember(Class<? extends T> clazz, T object) {
        CodegenMember existing = members.get(object);
        if (existing != null) {
            return existing;
        }

        int memberNumber = currentMemberNumber++;
        CodegenMember member = new CodegenMember(new CodegenMemberId(memberNumber), clazz, object);
        members.put(object, member);
        return member;
    }

    public CodegenLocalMethodBuilder addMethod(Class returnType, Class generator) {
        int methodNumber = currentMethodNumber++;
        return new CodegenLocalMethodBuilder(returnType, getGeneratorDetail(generator, debug), this, new CodegenMethodId(methodNumber));
    }

    public IdentityHashMap<Object, CodegenMember> getMembers() {
        return members;
    }

    public List<CodegenMethod> getMethods() {
        return methods;
    }

    private String getGeneratorDetail(Class generator, boolean debug) {
        if (!debug) {
            return generator.getSimpleName();
        }
        String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
        return generator.getName() + " --- " + className + "." + methodName + "():" + lineNumber;
    }
}
