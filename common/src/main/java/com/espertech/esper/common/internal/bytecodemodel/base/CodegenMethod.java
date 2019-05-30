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
package com.espertech.esper.common.internal.bytecodemodel.base;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenMethodWGraph;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CodegenMethod implements CodegenMethodScope {
    private final Class returnType;
    private final String returnTypeName;
    private final CodegenBlock block;
    private final String additionalDebugInfo;
    private final CodegenSymbolProvider optionalSymbolProvider;
    private boolean isStatic;

    private List<CodegenMethod> children = Collections.emptyList();
    private List<CodegenExpressionRef> environment = Collections.emptyList();
    private List<CodegenNamedParam> localParams = Collections.emptyList();
    private List<Class> thrown = Collections.emptyList();

    private Set<String> deepParameters;
    private CodegenMethodWGraph assignedMethod;
    private String assignedProviderClassName;

    protected CodegenMethod(Class returnType, String returnTypeName, Class generator, CodegenSymbolProvider optionalSymbolProvider, CodegenScope env) {
        if (generator == null) {
            throw new IllegalArgumentException("Invalid null generator");
        }
        this.returnType = returnType;
        this.returnTypeName = returnTypeName;
        this.optionalSymbolProvider = optionalSymbolProvider;
        this.block = new CodegenBlock(this);
        if (env.isDebug()) {
            additionalDebugInfo = getGeneratorDetail(generator);
        } else {
            additionalDebugInfo = generator.getSimpleName();
        }
    }

    public static CodegenMethod makeParentNode(Class returnType, Class generator, CodegenScope env) {
        if (returnType == null) {
            throw new IllegalArgumentException("Invalid null return type");
        }
        return new CodegenMethod(returnType, null, generator, CodegenSymbolProviderEmpty.INSTANCE, env);
    }

    public static CodegenMethod makeParentNode(Class returnType, Class generator, CodegenSymbolProvider symbolProvider, CodegenScope env) {
        if (returnType == null) {
            throw new IllegalArgumentException("Invalid null return type");
        }
        if (symbolProvider == null) {
            throw new IllegalArgumentException("No symbol provider");
        }
        return new CodegenMethod(returnType, null, generator, symbolProvider, env);
    }

    public static CodegenMethod makeParentNode(String returnTypeName, Class generator, CodegenSymbolProvider symbolProvider, CodegenScope env) {
        if (returnTypeName == null) {
            throw new IllegalArgumentException("Invalid null return type");
        }
        if (symbolProvider == null) {
            throw new IllegalArgumentException("No symbol provider");
        }
        return new CodegenMethod(null, returnTypeName, generator, symbolProvider, env);
    }

    public CodegenMethod makeChild(Class returnType, Class generator, CodegenScope env) {
        if (returnType == null) {
            throw new IllegalArgumentException("Invalid null return type");
        }
        return addChild(new CodegenMethod(returnType, null, generator, null, env));
    }

    public CodegenMethod makeChild(String returnType, Class generator, CodegenScope env) {
        if (returnType == null) {
            throw new IllegalArgumentException("Invalid null return type");
        }
        return addChild(new CodegenMethod(null, returnType, generator, null, env));
    }

    public CodegenMethod makeChildWithScope(Class returnType, Class generator, CodegenSymbolProvider symbolProvider, CodegenScope env) {
        if (returnType == null) {
            throw new IllegalArgumentException("Invalid null return type");
        }
        return addChild(new CodegenMethod(returnType, null, generator, symbolProvider, env));
    }

    public CodegenMethod makeChildWithScope(String returnType, Class generator, CodegenSymbolProvider symbolProvider, CodegenScope env) {
        if (returnType == null) {
            throw new IllegalArgumentException("Invalid null return type");
        }
        return addChild(new CodegenMethod(null, returnType, generator, symbolProvider, env));
    }

    public CodegenMethod addSymbol(CodegenExpressionRef symbol) {
        if (environment.isEmpty()) {
            environment = new ArrayList<>(4);
        }
        environment.add(symbol);
        return this;
    }

    public void mergeClasses(Set<Class> classes) {
        block.mergeClasses(classes);
        classes.add(returnType);
        for (Class ex : thrown) {
            classes.add(ex);
        }
        for (CodegenNamedParam param : localParams) {
            param.mergeClasses(classes);
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        block.traverseExpressions(consumer);
    }

    public CodegenSymbolProvider getOptionalSymbolProvider() {
        return optionalSymbolProvider;
    }

    public List<CodegenMethod> getChildren() {
        return children;
    }

    public List<CodegenExpressionRef> getEnvironment() {
        return environment;
    }

    public Class getReturnType() {
        return returnType;
    }

    public String getReturnTypeName() {
        return returnTypeName;
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

    public CodegenMethod addParam(Class type, String name) {
        if (localParams.isEmpty()) {
            localParams = new ArrayList<>(4);
        }
        localParams.add(new CodegenNamedParam(type, name));
        return this;
    }

    public CodegenMethod addParam(String typeName, String name) {
        if (localParams.isEmpty()) {
            localParams = new ArrayList<>(4);
        }
        localParams.add(new CodegenNamedParam(typeName, name));
        return this;
    }

    public CodegenMethod addParam(List<CodegenNamedParam> params) {
        if (localParams.isEmpty()) {
            localParams = new ArrayList<>(params.size());
        }
        localParams.addAll(params);
        return this;
    }

    public CodegenMethod addThrown(Class throwableClass) {
        if (thrown.isEmpty()) {
            thrown = new ArrayList<>();
        }
        thrown.add(throwableClass);
        return this;
    }

    public List<Class> getThrown() {
        return thrown;
    }

    public Set<String> getDeepParameters() {
        return deepParameters;
    }

    public void setDeepParameters(Set<String> deepParameters) {
        this.deepParameters = deepParameters;
    }

    public CodegenMethodWGraph getAssignedMethod() {
        return assignedMethod;
    }

    public void setAssignedMethod(CodegenMethodWGraph assignedMethod) {
        this.assignedMethod = assignedMethod;
    }

    public void setAssignedProviderClassName(String assignedProviderClassName) {
        this.assignedProviderClassName = assignedProviderClassName;
    }

    public CodegenMethod setStatic(boolean aStatic) {
        isStatic = aStatic;
        return this;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getAssignedProviderClassName() {
        return assignedProviderClassName;
    }

    public String toString() {
        return assignedMethod == null ? "CodegenMethod" : ("CodegenMethod{name=" + assignedMethod.getName() + "}");
    }

    private String getGeneratorDetail(Class generator) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String stackString = null;
        for (int i = 1; i < 10; i++) {
            if (stack[i].getClassName().contains(CodegenMethod.class.getPackage().getName())) {
                continue;
            }
            stackString = getStackString(i, stack);
            break;
        }
        if (stackString == null) {
            stackString = getStackString(3, stack);
        }
        if (stackString.contains("makeSelectExprProcessors")) {
            throw new UnsupportedOperationException();
        }
        return generator.getSimpleName() + " --- " + stackString;
    }

    private String getStackString(int i, StackTraceElement[] stack) {
        String fullClassName = stack[i].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = stack[i].getMethodName();
        int lineNumber = stack[i].getLineNumber();
        return className + "." + methodName + "():" + lineNumber;
    }

    private CodegenMethod addChild(CodegenMethod methodNode) {
        if (children.isEmpty()) {
            children = new ArrayList<>();
        }
        children.add(methodNode);
        return methodNode;
    }
}
