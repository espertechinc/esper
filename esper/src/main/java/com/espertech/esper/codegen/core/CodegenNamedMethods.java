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
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenSymbolProvider;
import com.espertech.esper.codegen.base.CodegenSymbolProviderEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CodegenNamedMethods {
    private Map<String, CodegenMethodNode> methods;

    public CodegenMethodNode addMethod(Class returnType, String methodName, List<CodegenNamedParam> params, Class generator, CodegenClassScope classScope, Consumer<CodegenMethodNode> code) {
        return addMethodWithSymbols(returnType, methodName, params, generator, classScope, code, CodegenSymbolProviderEmpty.INSTANCE);
    }

    public CodegenMethodNode addMethodWithSymbols(Class returnType, String methodName, List<CodegenNamedParam> params, Class generator, CodegenClassScope classScope, Consumer<CodegenMethodNode> code, CodegenSymbolProvider symbolProvider) {
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
        CodegenMethodNode method = CodegenMethodNode.makeParentNode(returnType, generator, symbolProvider, classScope).addParam(params);
        methods.put(methodName, method);
        code.accept(method);
        return method;
    }

    public Map<String, CodegenMethodNode> getMethods() {
        return methods == null ? Collections.emptyMap() : methods;
    }

    public CodegenMethodNode getMethod(String name) {
        CodegenMethodNode method = methods.get(name);
        if (method == null) {
            throw new IllegalStateException("Method by name '" + method + "' not found");
        }
        return method;
    }
}
