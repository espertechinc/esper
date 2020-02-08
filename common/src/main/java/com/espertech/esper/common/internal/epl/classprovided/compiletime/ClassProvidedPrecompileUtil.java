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

import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.CompilerServicesCompileException;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.*;

public class ClassProvidedPrecompileUtil {
    public static ClassProvidedPrecompileResult compileClassProvided(List<String> classTexts, StatementCompileTimeServices compileTimeServices, ClassProvidedPrecompileResult optionalPrior)
        throws ExprValidationException {
        if (classTexts == null || classTexts.isEmpty()) {
            return new ClassProvidedPrecompileResult(Collections.emptyMap(), Collections.emptyList());
        }

        int index = -1;
        Map<String, byte[]> allBytes = new HashMap<>();
        List<Class> allClasses = new ArrayList<>();
        if (optionalPrior != null) {
            allBytes.putAll(optionalPrior.getBytes());
        }
        ByteArrayProvidingClassLoader cl = new ByteArrayProvidingClassLoader(allBytes, compileTimeServices.getServices().getParentClassLoader());

        for (String classText : classTexts) {
            if (classText.trim().isEmpty()) {
                continue;
            }

            index++;
            String className = "provided_" + index + "_" + CodeGenerationIDGenerator.generateClassNameUUID();
            Map<String, byte[]> output = new HashMap<>();

            try {
                compileTimeServices.getCompilerServices().compileClass(classText, className, allBytes, output, compileTimeServices.getServices());
            } catch (CompilerServicesCompileException ex) {
                throw handleException(ex, "Failed to compile class", classText);
            }

            for (Map.Entry<String, byte[]> entry : output.entrySet()) {
                if (allBytes.containsKey(entry.getKey())) {
                    throw new ExprValidationException("Duplicate class by name '" + entry.getKey() + "'");
                }
            }

            allBytes.putAll(output);
            List<Class> classes = new ArrayList<>(2);
            for (Map.Entry<String, byte[]> entry : output.entrySet()) {
                try {
                    Class clazz = Class.forName(entry.getKey(), false, cl);
                    classes.add(clazz);
                } catch (RuntimeException | ClassNotFoundException e) {
                    throw handleException(e, "Failed to load class '" + entry.getKey() + "'", classText);
                }
            }
            allClasses.addAll(classes);
        }

        return new ClassProvidedPrecompileResult(allBytes, allClasses);
    }

    private static ExprValidationException handleException(Exception ex, String action, String classText) {
        return new ExprValidationException(action + ": " + ex.getMessage() + " for class [\"\"\"" + classText + "\"\"\"]", ex);
    }

    public static ClassProvidedClasspathExtension makeSvc(ClassProvidedPrecompileResult classesInlined, ClassProvidedPrecompileResult classesCreateClass, ClassProvidedCompileTimeResolver resolver) {
        if (classesInlined.getBytes().isEmpty() && classesCreateClass == null && resolver.isEmpty()) {
            return ClassProvidedClasspathExtensionEmpty.INSTANCE;
        }
        if (classesCreateClass == null) {
            return new ClassProvidedClasspathExtensionImpl(classesInlined, resolver);
        }
        if (classesInlined.getBytes().isEmpty()) {
            return new ClassProvidedClasspathExtensionImpl(classesCreateClass, resolver);
        }
        ClassProvidedPrecompileResult merged = ClassProvidedPrecompileResult.merge(classesInlined, classesCreateClass);
        return new ClassProvidedClasspathExtensionImpl(merged, resolver);
    }
}
