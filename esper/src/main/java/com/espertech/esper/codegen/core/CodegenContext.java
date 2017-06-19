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

import com.espertech.esper.codegen.compile.CodegenCompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodegenContext {

    private final List<CodegenMember> members = new ArrayList<>();
    private final List<CodegenMethod> methods = new ArrayList<>();

    public void addMember(String memberName, Class clazz, Object object) {
        members.add(new CodegenMember(memberName, clazz, object));
    }

    public void addMember(String memberName, Class clazz, Class optionalTypeParam, Object object) {
        members.add(new CodegenMember(memberName, clazz, optionalTypeParam, object));
    }

    public void addMember(CodegenMember entry) {
        members.add(entry);
    }

    public CodegenMember makeMember(Class clazz, Object object) {
        String memberName = CodeGenerationIDGenerator.generateMember();
        return new CodegenMember(memberName, clazz, object);
    }

    public CodegenMember makeAddMember(Class clazz, Object object) {
        CodegenMember member = makeMember(clazz, object);
        members.add(member);
        return member;
    }

    public CodegenMember makeMember(Class clazz, Class optionalTypeParam, Object object) {
        String memberName = CodeGenerationIDGenerator.generateMember();
        return new CodegenMember(memberName, clazz, optionalTypeParam, object);
    }

    public CodegenBlock addMethod(Class returnType, Class paramType, String paramName, Class generator) {
        String methodName = CodeGenerationIDGenerator.generateMethod();
        CodegenMethod method = new CodegenMethod(returnType, methodName, Collections.singletonList(new CodegenNamedParam(paramType, paramName)), getGeneratorDetail(generator));
        methods.add(method);
        return method.statements();
    }

    public CodegenBlock addMethod(Class returnType, Class generator) {
        String methodName = CodeGenerationIDGenerator.generateMethod();
        CodegenMethod method = new CodegenMethod(returnType, methodName, Collections.<CodegenNamedParam>emptyList(), getGeneratorDetail(generator));
        methods.add(method);
        return method.statements();
    }

    public List<CodegenMember> getMembers() {
        return members;
    }

    public List<CodegenMethod> getMethods() {
        return methods;
    }

    private String getGeneratorDetail(Class generator) {
        if (!CodegenCompiler.DEBUG) {
            return generator.getSimpleName();
        }
        String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
        return generator.getName() + " --- " + className + "." + methodName + "():" + lineNumber;
    }
}
