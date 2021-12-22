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
package com.espertech.esper.common.internal.epl.classprovided.compiletime;

import com.espertech.esper.common.internal.compile.compiler.CompilerAbstractionClassCollection;
import com.espertech.esper.common.internal.compile.compiler.CompilerAbstractionCompilationContext;
import com.espertech.esper.common.internal.compile.compiler.CompilerAbstractionCompileSourcesResult;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClassProvidedPrecompileUtil {
    public static ClassProvidedPrecompileResult compileClassProvided(List<String> classTexts, Consumer<Object> compileResultConsumer, StatementCompileTimeServices compileTimeServices, ClassProvidedPrecompileResult optionalPrior)
            throws ExprValidationException {
        if (classTexts == null || classTexts.isEmpty()) {
            return ClassProvidedPrecompileResult.EMPTY;
        }

        List<String> classWithText = new ArrayList<>(classTexts.size());
        for (String classText : classTexts) {
            if (classText.trim().isEmpty()) {
                continue;
            }
            classWithText.add(classText);
        }
        if (classWithText.isEmpty()) {
            return ClassProvidedPrecompileResult.EMPTY;
        }

        if (!compileTimeServices.getConfiguration().getCompiler().getByteCode().isAllowInlinedClass()) {
            throw new ExprValidationException("Inlined-class compilation has been disabled by configuration");
        }

        CompilerAbstractionClassCollection allBytes = compileTimeServices.getCompilerAbstraction().newClassCollection();
        List<Class> allClasses = new ArrayList<>();
        if (optionalPrior != null) {
            allBytes.add(optionalPrior.getBytes());
        }

        Map<String, List<String>> classTextToClassName;
        try {
            CompilerAbstractionCompilationContext ctx = new CompilerAbstractionCompilationContext(compileTimeServices.getServices(), compileResultConsumer, Collections.emptyList());
            CompilerAbstractionCompileSourcesResult result = compileTimeServices.getCompilerAbstraction().compileSources(classTexts, ctx, allBytes);
            classTextToClassName = result.getCodeToClassNames();
        } catch (RuntimeException ex) {
            throw new ExprValidationException("Failed to compile an inlined-class: " + ex.getMessage(), ex);
        }

        ByteArrayProvidingClassLoader cl = new ByteArrayProvidingClassLoader(allBytes.getClasses(), compileTimeServices.getServices().getParentClassLoader());
        for (Map.Entry<String, List<String>> entry : classTextToClassName.entrySet()) {
            if (entry.getKey().trim().isEmpty()) {
                continue;
            }

            for (String className : entry.getValue()) {
                Class clazz;
                try {
                    clazz = Class.forName(className, false, cl);
                } catch (RuntimeException | ClassNotFoundException e) {
                    throw handleException(e, "Failed to load class '" + className + "'", entry.getKey());
                }
                allClasses.add(clazz);
            }
        }

        return new ClassProvidedPrecompileResult(allBytes.getClasses(), allClasses);
    }

    private static ExprValidationException handleException(Exception ex, String action, String classText) {
        return new ExprValidationException(action + ": " + ex.getMessage() + " for class [\"\"\"" + classText + "\"\"\"]", ex);
    }
}
