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

import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.util.EnumValue;
import com.espertech.esper.common.internal.util.ValueAndFieldDesc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ClasspathImportCompileTimeUtil {

    public static ValueAndFieldDesc resolveIdentAsEnumConst(String constant, ClasspathImportServiceCompileTime classpathImportService, boolean isAnnotation)
            throws ExprValidationException {
        EnumValue enumValue = resolveIdentAsEnum(constant, classpathImportService, isAnnotation);
        if (enumValue == null) {
            return null;
        }
        try {
            return new ValueAndFieldDesc(enumValue.getEnumField().get(null), enumValue.getEnumField());
        } catch (IllegalAccessException e) {
            throw new ExprValidationException("Exception accessing field '" + enumValue.getEnumField().getName() + "': " + e.getMessage(), e);
        }
    }

    public static EnumValue resolveIdentAsEnum(String constant, ClasspathImportServiceCompileTime classpathImportService, boolean isAnnotation) {
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
            clazz = classpathImportService.resolveClass(className, isAnnotation);
        } catch (ClasspathImportException e) {
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
            return new EnumValue(clazz, field);
        }

        return null;
    }

    private static String unescape(String name) {
        if (name.startsWith("`") && name.endsWith("`")) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }
}
