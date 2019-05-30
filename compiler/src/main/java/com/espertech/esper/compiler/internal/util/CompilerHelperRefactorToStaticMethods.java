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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInnerClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenMethodWGraph;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;
import java.util.function.Function;

public class CompilerHelperRefactorToStaticMethods {
    public final static int MAX_METHODS_PER_CLASS_MINIMUM = 1000;

    public static void refactorMethods(List<CodegenClass> classes, int maxMethodsPerClass) {
        for (CodegenClass clazz : classes) {
            refactorMethodsClass(clazz, maxMethodsPerClass);
        }
    }

    private static void refactorMethodsClass(CodegenClass clazz, int maxMethodsPerClass) {
        CodegenInnerClass[] inners = clazz.getInnerClasses().toArray(new CodegenInnerClass[0]);
        for (CodegenInnerClass inner : inners) {
            refactorMethodsInnerClass(clazz, inner, maxMethodsPerClass);
        }
    }

    private static void refactorMethodsInnerClass(CodegenClass clazz, CodegenInnerClass inner, int maxMethodsPerClass) {
        if (maxMethodsPerClass < MAX_METHODS_PER_CLASS_MINIMUM) {
            throw new EPException("Invalid value for maximum number of methods per class, expected a minimum of " + MAX_METHODS_PER_CLASS_MINIMUM + " but received " + maxMethodsPerClass);
        }
        int size = inner.getMethods().size();
        if (size <= maxMethodsPerClass) {
            return;
        }

        // collect static methods bottom-up
        Set<CodegenMethodWGraph> collectedStaticMethods = new HashSet<>();
        Function<CodegenMethod, Boolean> permittedMethods = method -> collectedStaticMethods.contains(method.getAssignedMethod());
        for (CodegenMethodWGraph publicMethod : inner.getMethods().getPublicMethods()) {
            recursiveBottomUpCollectStatic(publicMethod.getOriginator(), collectedStaticMethods, permittedMethods);
        }

        // collect static methods from private methods preserving the order they appear in
        List<CodegenMethodWGraph> staticMethods = new ArrayList<>();
        int count = -1;
        for (CodegenMethodWGraph privateMethod : inner.getMethods().getPrivateMethods()) {
            count++;
            if (count < maxMethodsPerClass) {
                continue;
            }
            if (collectedStaticMethods.contains(privateMethod)) {
                staticMethods.add(privateMethod);
            }
        }

        if (staticMethods.isEmpty()) {
            return;
        }

        // assign to buckets
        List<List<CodegenMethodWGraph>> statics = CollectionUtil.subdivide(staticMethods, maxMethodsPerClass);

        // for each bucket
        for (int i = 0; i < statics.size(); i++) {
            List<CodegenMethodWGraph> bucket = statics.get(i);

            // new inner class
            String className = inner.getClassName() + "util" + i;
            CodegenClassMethods methods = new CodegenClassMethods();
            methods.getPrivateMethods().addAll(bucket);
            for (CodegenMethodWGraph privateMethod : bucket) {
                privateMethod.setStatic(true);
            }
            CodegenInnerClass utilClass = new CodegenInnerClass(className, null, Collections.emptyList(), methods);
            clazz.addInnerClass(utilClass);

            // repoint
            for (CodegenMethodWGraph privateMethod : bucket) {
                privateMethod.getOriginator().setAssignedProviderClassName(className);
            }

            // remove private methods from inner class
            inner.getMethods().getPrivateMethods().removeAll(bucket);
        }
    }

    private static void recursiveBottomUpCollectStatic(CodegenMethod method, Set<CodegenMethodWGraph> collected, Function<CodegenMethod, Boolean> permittedMethods) {
        for (CodegenMethod child : method.getChildren()) {
            recursiveBottomUpCollectStatic(child, collected, permittedMethods);
        }

        for (CodegenMethod child : method.getChildren()) {
            if (!collected.contains(child.getAssignedMethod())) {
                return;
            }
        }

        if (!method.getBlock().hasInstanceAccess(permittedMethods)) {
            collected.add(method.getAssignedMethod());
        }
    }
}
