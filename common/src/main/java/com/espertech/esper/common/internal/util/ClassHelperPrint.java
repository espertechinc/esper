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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import static com.espertech.esper.common.client.type.EPTypePremade.*;

public class ClassHelperPrint {
    private static final Set<EPTypePremade> BASETYPES = new HashSet<>();

    static {
        BASETYPES.add(BYTEPRIMITIVE);
        BASETYPES.add(CHARPRIMITIVE);
        BASETYPES.add(SHORTPRIMITIVE);
        BASETYPES.add(INTEGERPRIMITIVE);
        BASETYPES.add(LONGPRIMITIVE);
        BASETYPES.add(DOUBLEPRIMITIVE);
        BASETYPES.add(FLOATPRIMITIVE);
        BASETYPES.add(BOOLEANBOXED);
        BASETYPES.add(BYTEBOXED);
        BASETYPES.add(CHARBOXED);
        BASETYPES.add(SHORTBOXED);
        BASETYPES.add(INTEGERBOXED);
        BASETYPES.add(LONGBOXED);
        BASETYPES.add(DOUBLEBOXED);
        BASETYPES.add(FLOATBOXED);
        BASETYPES.add(BIGINTEGER);
        BASETYPES.add(BIGDECIMAL);
        BASETYPES.add(BIGINTEGERARRAY);
        BASETYPES.add(BIGDECIMALARRAY);
        BASETYPES.add(BIGINTEGERARRAYARRAY);
        BASETYPES.add(BIGDECIMALARRAYARRAY);
        BASETYPES.add(STRING);
        BASETYPES.add(NUMBER);
        BASETYPES.add(OBJECT);
        BASETYPES.add(VOID);
    }

    public static String getClassNameFullyQualPrettyObject(Object value) {
        return value == null ? EPTypeNull.INSTANCE.getTypeName() : getClassNameFullyQualPretty(value.getClass());
    }

    public static String getClassNameFullyQualPretty(EPType type) {
        return type == null ? EPTypeNull.INSTANCE.getTypeName() : type.getTypeName();
    }

    public static String getClassNameFullyQualPretty(Class clazz) {
        if (clazz == null) {
            return EPTypeNull.INSTANCE.getTypeName();
        }
        String className = getClassNameNonArray(JavaClassHelper.getArrayComponentTypeInnermost(clazz));
        if (!clazz.isArray()) {
            return className;
        }
        StringWriter buf = new StringWriter();
        buf.append(className);
        appendDimensions(buf, clazz);
        return buf.toString();
    }

    public static String getClassNameFullyQualPrettyWithClassloader(Class clazz) {
        String name = getClassNameFullyQualPretty(clazz);
        String classloader = getClassLoaderId(clazz.getClassLoader());
        return name + "(loaded by " + classloader + ")";
    }

    public static String getClassLoaderId(ClassLoader classLoader) {
        if (classLoader == null) {
            return "(classloader is null)";
        }
        return classLoader.getClass().getName() + "@" + System.identityHashCode(classLoader);
    }

    public static void appendDimensions(StringWriter writer, Class typeClass) {
        int numDimensions = JavaClassHelper.getArrayDimensions(typeClass);
        for (int i = 0; i < numDimensions; i++) {
            writer.append("[]");
        }
    }

    private static String getClassNameNonArray(Class clazz) {
        if (clazz == null) {
            return EPTypeNull.INSTANCE.getTypeName();
        }
        EPTypePremade premade = EPTypePremade.getExisting(clazz);
        if (premade != null && isBaseType(premade)) {
            return premade.getEPType().getType().getSimpleName();
        }
        return clazz.getName();
    }

    private static boolean isBaseType(EPTypePremade premade) {
        return BASETYPES.contains(premade);
    }

    /**
     * Returns a comma-separated parameter type list in readable form,
     * considering arrays and null-type parameters.
     *
     * @param parameters is the parameter types to render
     * @return rendered list of parameters
     */
    public static String getParameterAsString(Class[] parameters) {
        StringBuilder builder = new StringBuilder();
        String delimiterComma = ", ";
        String delimiter = "";
        for (Class param : parameters) {
            builder.append(delimiter);
            builder.append(getParameterAsString(param));
            delimiter = delimiterComma;
        }
        return builder.toString();
    }

    /**
     * Returns a comma-separated parameter type list in readable form,
     * considering arrays and null-type parameters.
     *
     * @param parameters is the parameter types to render
     * @return rendered list of parameters
     */
    public static String getParameterAsString(EPType[] parameters) {
        StringBuilder builder = new StringBuilder();
        String delimiterComma = ", ";
        String delimiter = "";
        for (EPType param : parameters) {
            builder.append(delimiter);
            builder.append(getParameterAsString(param));
            delimiter = delimiterComma;
        }
        return builder.toString();
    }

    /**
     * Returns a parameter as a string text, allowing null values to represent a null
     * select expression type.
     *
     * @param param is the parameter type
     * @return string representation of parameter
     */
    public static String getParameterAsString(Class param) {
        if (param == null) {
            return EPTypeNull.INSTANCE.getTypeName();
        }
        return param.getSimpleName();
    }

    /**
     * Returns a parameter as a string text, allowing null values to represent a null
     * select expression type.
     *
     * @param param is the parameter type
     * @return string representation of parameter
     */
    public static String getParameterAsString(EPType param) {
        if (param == null || param == EPTypeNull.INSTANCE) {
            return EPTypeNull.INSTANCE.getTypeName();
        }
        return ((EPTypeClass) param).getType().getSimpleName();
    }
}
