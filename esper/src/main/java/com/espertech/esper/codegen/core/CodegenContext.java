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
import java.util.List;

public class CodegenContext {

    private final boolean debug;
    private final List<CodegenMember> members = new ArrayList<>();
    private final List<CodegenMethod> methods = new ArrayList<>();

    public CodegenContext(boolean debug) {
        this.debug = debug;
    }

    public void addMember(CodegenMember entry) {
        members.add(entry);
    }

    public CodegenMember makeMember(Class clazz, Object object) {
        String memberName = CodeGenerationIDGenerator.generateMember();
        return new CodegenMember(memberName, clazz, object);
    }

    public <T> CodegenMember makeAddMember(Class<? extends T> clazz, T object) {
        CodegenMember member = makeMember(clazz, object);
        members.add(member);
        return member;
    }

    public CodegenMember makeMember(Class clazz, Class optionalTypeParam, Object object) {
        String memberName = CodeGenerationIDGenerator.generateMember();
        return new CodegenMember(memberName, clazz, optionalTypeParam, object);
    }

    public CodegenLocalMethodBuilder addMethod(Class returnType, Class generator) {
        return new CodegenLocalMethodBuilder(returnType, getGeneratorDetail(generator, debug), this);
    }

    public List<CodegenMember> getMembers() {
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
