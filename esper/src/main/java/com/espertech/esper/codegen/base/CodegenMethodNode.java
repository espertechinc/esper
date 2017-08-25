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
package com.espertech.esper.codegen.base;

import com.espertech.esper.codegen.core.CodegenMethod;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CodegenMethodNode implements CodegenMethodScope {
    private final Class returnType;
    private final CodegenBlock block;
    private final String additionalDebugInfo;
    private final CodegenSymbolProvider optionalSymbolProvider;

    private List<CodegenMethodNode> children = Collections.emptyList();
    private List<CodegenExpressionRef> environment = Collections.emptyList();
    private List<CodegenNamedParam> localParams = Collections.emptyList();

    private Set<String> deepParameters;
    private CodegenMethod assignedMethod;

    protected CodegenMethodNode(Class returnType, Class generator, CodegenSymbolProvider optionalSymbolProvider, CodegenClassScope codegenClassScope) {
        this.returnType = returnType;
        this.optionalSymbolProvider = optionalSymbolProvider;
        this.block = new CodegenBlock(this);
        if (codegenClassScope.isDebug()) {
            additionalDebugInfo = getGeneratorDetail(generator);
        } else {
            additionalDebugInfo = generator.getSimpleName();
        }
    }

    public static CodegenMethodNode makeParentNode(Class returnType, Class generator, CodegenSymbolProvider symbolProvider, CodegenClassScope codegenClassScope) {
        if (symbolProvider == null) {
            throw new IllegalArgumentException("No symbol provider");
        }
        return new CodegenMethodNode(returnType, generator, symbolProvider, codegenClassScope);
    }

    public CodegenMethodNode makeChild(Class returnType, Class generator, CodegenClassScope codegenClassScope) {
        return addChild(new CodegenMethodNode(returnType, generator, null, codegenClassScope));
    }

    public CodegenMethodNode makeChildWithScope(Class returnType, Class generator, CodegenSymbolProvider symbolProvider, CodegenClassScope codegenClassScope) {
        return addChild(new CodegenMethodNode(returnType, generator, symbolProvider, codegenClassScope));
    }

    public CodegenMethodNode addSymbol(CodegenExpressionRef symbol) {
        if (environment.isEmpty()) {
            environment = new ArrayList<>(4);
        }
        environment.add(symbol);
        return this;
    }

    public void mergeClasses(Set<Class> classes) {
        block.mergeClasses(classes);
        classes.add(returnType);
    }

    public CodegenSymbolProvider getOptionalSymbolProvider() {
        return optionalSymbolProvider;
    }

    public List<CodegenMethodNode> getChildren() {
        return children;
    }

    public List<CodegenExpressionRef> getEnvironment() {
        return environment;
    }

    public Class getReturnType() {
        return returnType;
    }

    public String getAdditionalDebugInfo() {
        return additionalDebugInfo;
    }

    public CodegenBlock getBlock() {
        return block;
    }

    public List<CodegenNamedParam> getLocalParams() {
        return localParams;
    }

    public CodegenMethodNode addParam(Class type, String name) {
        if (localParams.isEmpty()) {
            localParams = new ArrayList<>(4);
        }
        localParams.add(new CodegenNamedParam(type, name));
        return this;
    }

    public CodegenMethodNode addParam(List<CodegenNamedParam> params) {
        if (localParams.isEmpty()) {
            localParams = new ArrayList<>(params.size());
        }
        localParams.addAll(params);
        return this;
    }

    public Set<String> getDeepParameters() {
        return deepParameters;
    }

    public void setDeepParameters(Set<String> deepParameters) {
        this.deepParameters = deepParameters;
    }

    public CodegenMethod getAssignedMethod() {
        return assignedMethod;
    }

    public void setAssignedMethod(CodegenMethod assignedMethod) {
        this.assignedMethod = assignedMethod;
    }

    private String getGeneratorDetail(Class generator) {
        String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
        return generator.getName() + " --- " + className + "." + methodName + "():" + lineNumber;
    }

    private CodegenMethodNode addChild(CodegenMethodNode methodNode) {
        if (children.isEmpty()) {
            children = new ArrayList<>();
        }
        children.add(methodNode);
        return methodNode;
    }
}
