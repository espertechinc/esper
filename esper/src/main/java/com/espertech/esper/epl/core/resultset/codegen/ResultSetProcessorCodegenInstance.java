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
package com.espertech.esper.epl.core.resultset.codegen;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.*;
import java.util.function.Consumer;

public class ResultSetProcessorCodegenInstance {
    private final CodegenCtor factoryCtor;
    private final List<CodegenNamedParam> factoryExplicitMembers;

    private Map<String, CodegenMethodNode> methods;
    private List<ResultSetProcessorMemberEntry> members;
    private List<CodegenExpression> ctorExpressions;

    public ResultSetProcessorCodegenInstance(CodegenCtor factoryCtor, List<CodegenNamedParam> factoryExplicitMembers) {
        this.factoryCtor = factoryCtor;
        this.factoryExplicitMembers = factoryExplicitMembers;
    }

    public void addMember(String name, Class type, CodegenExpression initializer) {
        if (members == null) {
            members = new ArrayList<>(2);
        }
        for (ResultSetProcessorMemberEntry member : members) {
            if (member.getName().equals(name)) {
                return;
            }
        }
        members.add(new ResultSetProcessorMemberEntry(name, type, initializer));
    }

    public boolean hasMember(String name) {
        if (members == null) {
            return false;
        }
        for (ResultSetProcessorMemberEntry member : members) {
            if (member.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public CodegenMethodNode addMethod(Class returnType, String methodName, List<CodegenNamedParam> params, Class generator, CodegenClassScope classScope, Consumer<CodegenMethodNode> code) {
        if (methods == null) {
            methods = new HashMap<>();
        }

        CodegenMethodNode existing = methods.get(methodName);
        if (existing != null) {
            if (params.equals(existing.getLocalParams())) {
                return existing;
            }
            throw new IllegalStateException("Method by name '" + methodName + "' already registered");
        }
        CodegenMethodNode method = CodegenMethodNode.makeParentNode(returnType, generator, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(params);
        methods.put(methodName, method);
        code.accept(method);
        return method;
    }

    public Map<String, CodegenMethodNode> getMethods() {
        return methods == null ? Collections.emptyMap() : methods;
    }

    public List<ResultSetProcessorMemberEntry> getMembers() {
        return members == null ? Collections.emptyList() : members;
    }

    public CodegenMethodNode getMethod(String name) {
        CodegenMethodNode method = methods.get(name);
        if (method == null) {
            throw new IllegalStateException("Method by name '" + method + "' not found");
        }
        return method;
    }

    public void addCtorCode(CodegenExpression expression) {
        if (ctorExpressions == null) {
            ctorExpressions = new ArrayList<>(2);
        }
        ctorExpressions.add(expression);
    }

    public List<CodegenExpression> getCtorExpressions() {
        if (ctorExpressions == null) {
            return Collections.emptyList();
        }
        return ctorExpressions;
    }

    public CodegenCtor getFactoryCtor() {
        return factoryCtor;
    }

    public List<CodegenNamedParam> getFactoryExplicitMembers() {
        return factoryExplicitMembers;
    }
}
