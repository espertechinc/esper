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

import com.espertech.esper.common.client.annotation.Hook;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.annotation.Annotation;

public class ClasspathImportUtil {

    /**
     * Returns an instance of a hook as specified by an annotation.
     *
     * @param annotations            to search
     * @param hookType               type to look for
     * @param interfaceExpected      interface required
     * @param classpathImportService for resolving references
     * @return hook instance
     * @throws ExprValidationException if instantiation failed
     */
    public static Object getAnnotationHook(Annotation[] annotations, HookType hookType, Class interfaceExpected, ClasspathImportService classpathImportService)
            throws ExprValidationException {
        if (annotations == null) {
            return null;
        }
        String hookClass = null;
        for (int i = 0; i < annotations.length; i++) {
            if (!(annotations[i] instanceof Hook)) {
                continue;
            }
            Hook hook = (Hook) annotations[i];
            if (hook.type() != hookType) {
                continue;
            }
            hookClass = hook.hook();
        }
        if (hookClass == null) {
            return null;
        }

        Class clazz;
        try {
            clazz = classpathImportService.resolveClass(hookClass, false);
        } catch (Exception e) {
            throw new ExprValidationException("Failed to resolve hook provider of hook type '" + hookType +
                    "' import '" + hookClass + "' :" + e.getMessage());
        }

        if (!JavaClassHelper.isImplementsInterface(clazz, interfaceExpected)) {
            throw new ExprValidationException("Hook provider for hook type '" + hookType + "' " +
                    "class '" + clazz.getName() + "' does not implement the required '" + interfaceExpected.getSimpleName() +
                    "' interface");
        }

        Object hook;
        try {
            hook = clazz.newInstance();
        } catch (Exception e) {
            throw new ExprValidationException("Failed to instantiate hook provider of hook type '" + hookType + "' " +
                    "class '" + clazz.getName() + "' :" + e.getMessage());
        }

        return hook;
    }
}
