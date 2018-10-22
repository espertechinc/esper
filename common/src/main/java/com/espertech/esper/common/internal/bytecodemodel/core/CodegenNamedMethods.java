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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProvider;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CodegenNamedMethods {
    private Map<String, CodegenMethod> methods;

    public CodegenMethod addMethod(Class returnType, String methodName, List<CodegenNamedParam> params, Class generator, CodegenClassScope classScope, Consumer<CodegenMethod> code) {
        return addMethodWithSymbols(returnType, methodName, params, generator, classScope, code, CodegenSymbolProviderEmpty.INSTANCE);
    }

    public CodegenMethod addMethodWithSymbols(Class returnType, String methodName, List<CodegenNamedParam> params, Class generator, CodegenClassScope classScope, Consumer<CodegenMethod> code, CodegenSymbolProvider symbolProvider) {
        if (methods == null) {
            methods = new HashMap<>();
        }

        CodegenMethod existing = methods.get(methodName);
        if (existing != null) {
            if (params.equals(existing.getLocalParams())) {
                return existing;
            }
            throw new IllegalStateException("Method by name '" + methodName + "' already registered");
        }
        CodegenMethod method = CodegenMethod.makeParentNode(returnType, generator, symbolProvider, classScope).addParam(params);
        methods.put(methodName, method);
        code.accept(method);
        return method;
    }

    public Map<String, CodegenMethod> getMethods() {
        return methods == null ? Collections.emptyMap() : methods;
    }

    public CodegenMethod getMethod(String name) {
        CodegenMethod method = methods.get(name);
        if (method == null) {
            throw new IllegalStateException("Method by name '" + method + "' not found");
        }
        return method;
    }
}
