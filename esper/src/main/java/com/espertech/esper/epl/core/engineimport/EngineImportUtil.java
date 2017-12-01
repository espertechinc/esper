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
package com.espertech.esper.epl.core.engineimport;

import com.espertech.esper.client.annotation.Hook;
import com.espertech.esper.client.annotation.HookType;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class EngineImportUtil {
    /**
     * Resolve a string constant as a possible enumeration value, returning null if not resolved.
     *
     * @param constant            to resolve
     * @param engineImportService for engine-level use to resolve enums, can be null
     * @param isAnnotation        whether we are in an annotation
     * @return null or enumeration value
     * @throws ExprValidationException if there is an error accessing the enum
     */
    public static Object resolveIdentAsEnumConst(String constant, EngineImportService engineImportService, boolean isAnnotation)
            throws ExprValidationException {
        int lastDotIndex = constant.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return null;
        }
        String className = constant.substring(0, lastDotIndex);
        String constName = constant.substring(lastDotIndex + 1);

        // un-escape
        className = unescape(className);
        constName = unescape(constName);

        Class clazz;
        try {
            clazz = engineImportService.resolveClass(className, isAnnotation);
        } catch (EngineImportException e) {
            return null;
        }

        Field field;
        try {
            field = clazz.getField(constName);
        } catch (NoSuchFieldException e) {
            return null;
        }

        int modifiers = field.getModifiers();
        if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
            try {
                return field.get(null);
            } catch (IllegalAccessException e) {
                throw new ExprValidationException("Exception accessing field '" + field.getName() + "': " + e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * Returns an instance of a hook as specified by an annotation.
     *
     * @param annotations         to search
     * @param hookType            type to look for
     * @param interfaceExpected   interface required
     * @param engineImportService for resolving references
     * @return hook instance
     * @throws ExprValidationException if instantiation failed
     */
    public static Object getAnnotationHook(Annotation[] annotations, HookType hookType, Class interfaceExpected, EngineImportService engineImportService)
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
            clazz = engineImportService.resolveClass(hookClass, false);
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

    private static String unescape(String name) {
        if (name.startsWith("`") && name.endsWith("`")) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }
}
