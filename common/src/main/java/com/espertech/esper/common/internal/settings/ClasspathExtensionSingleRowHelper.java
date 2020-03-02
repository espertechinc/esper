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
package com.espertech.esper.common.internal.settings;

import com.espertech.esper.common.client.hook.singlerowfunc.ExtensionSingleRowFunction;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public class ClasspathExtensionSingleRowHelper {
    public static void processAnnotations(List<Class> classes, String optionalModuleName, Map<String, ClasspathExtensionSingleRowDesc> singleRowFunctionExtensions) throws ExprValidationException {
        for (Class clazz : classes) {
            for (Annotation annotation : clazz.getDeclaredAnnotations()) {
                if (annotation instanceof ExtensionSingleRowFunction) {
                    processAnnotation((ExtensionSingleRowFunction) annotation, clazz, optionalModuleName, singleRowFunctionExtensions);
                }
            }
        }
    }

    private static void processAnnotation(ExtensionSingleRowFunction anno, Class clazz, String optionalModuleName, Map<String, ClasspathExtensionSingleRowDesc> singleRowFunctionExtensions) throws ExprValidationException {
        String name = anno.name().trim();
        String methodName = anno.methodName().trim();
        ClasspathImportSingleRowDesc desc = new ClasspathImportSingleRowDesc(clazz.getName(), methodName, anno.valueCache(), anno.filterOptimizable(), anno.rethrowExceptions(), anno.eventTypeName().isEmpty() ? null : anno.eventTypeName().trim());
        if (singleRowFunctionExtensions.containsKey(name)) {
            throw new ExprValidationException("The plug-in single-row function '" + name + "' occurs multiple times");
        }
        singleRowFunctionExtensions.put(name, new ClasspathExtensionSingleRowDesc(optionalModuleName, clazz, desc));
    }
}
