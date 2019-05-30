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
package com.espertech.esper.common.internal.bytecodemodel.util;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;

import java.util.*;

public class CodegenStackGenerator {

    public static void recursiveBuildStack(CodegenMethod methodNode, String name, CodegenClassMethods methods) {
        if (methodNode.getOptionalSymbolProvider() == null) {
            throw new IllegalArgumentException("Method node does not have symbol provider");
        }

        Map<String, Class> currentSymbols = new HashMap<>();
        methodNode.getOptionalSymbolProvider().provide(currentSymbols);

        if (!(methodNode instanceof CodegenCtor)) {
            CodegenMethodFootprint footprint = new CodegenMethodFootprint(methodNode.getReturnType(), methodNode.getReturnTypeName(), methodNode.getLocalParams(), methodNode.getAdditionalDebugInfo());
            CodegenMethodWGraph method = new CodegenMethodWGraph(name, footprint, methodNode.getBlock(), true, methodNode.getThrown(), methodNode).setStatic(methodNode.isStatic());
            methodNode.setAssignedMethod(method);
            methods.getPublicMethods().add(method);
        }

        for (CodegenMethod child : methodNode.getChildren()) {
            recursiveAdd(child, currentSymbols, methods.getPrivateMethods(), methodNode.isStatic());
        }
    }

    private static void recursiveAdd(CodegenMethod methodNode, Map<String, Class> currentSymbols, List<CodegenMethodWGraph> privateMethods, boolean isStatic) {
        TreeSet<String> namesPassed = getNamesPassed(methodNode);
        methodNode.setDeepParameters(namesPassed);

        List<CodegenNamedParam> paramset = new ArrayList<>(namesPassed.size() + methodNode.getLocalParams().size());

        // add local params
        for (CodegenNamedParam named : methodNode.getLocalParams()) {
            paramset.add(named);
        }

        // add pass-thru for those methods that do not have their own scope
        if (methodNode.getOptionalSymbolProvider() == null) {
            for (String name : namesPassed) {
                Class symbolType = currentSymbols.get(name);
                if (symbolType == null) {
                    throw new IllegalStateException("Failed to find named parameter '" + name + "' for method from " + methodNode.getAdditionalDebugInfo());
                }
                paramset.add(new CodegenNamedParam(symbolType, name));
            }
        } else {
            currentSymbols = new HashMap<>();
            methodNode.getOptionalSymbolProvider().provide(currentSymbols);
        }

        String name = "m" + privateMethods.size();
        CodegenMethodFootprint footprint = new CodegenMethodFootprint(methodNode.getReturnType(), methodNode.getReturnTypeName(), paramset, methodNode.getAdditionalDebugInfo());
        CodegenMethodWGraph method = new CodegenMethodWGraph(name, footprint, methodNode.getBlock(), false, methodNode.getThrown(), methodNode).setStatic(isStatic);
        methodNode.setAssignedMethod(method);
        privateMethods.add(method);

        for (CodegenMethod child : methodNode.getChildren()) {
            recursiveAdd(child, currentSymbols, privateMethods, isStatic);
        }
    }

    private static TreeSet<String> getNamesPassed(CodegenMethod node) {
        TreeSet<String> names = new TreeSet<>();
        recursiveGetNamesPassed(node, names);
        return names;
    }

    private static void recursiveGetNamesPassed(CodegenMethod node, Set<String> names) {
        if (node.getOptionalSymbolProvider() != null) {
            return;
        }
        for (CodegenExpressionRef ref : node.getEnvironment()) {
            names.add(ref.getRef());
        }
        for (CodegenMethod child : node.getChildren()) {
            recursiveGetNamesPassed(child, names);
        }
    }
}
