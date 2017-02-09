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
package com.espertech.esper.view;

import com.espertech.esper.client.EventType;
import com.espertech.esper.util.JavaClassHelper;

/**
 * Utility class for checking in a schema if fields exist and/or have an expected type.
 */
public final class PropertyCheckHelper {
    /**
     * Check if the field identified by the field name exists according to the schema.
     *
     * @param type      contains metadata about fields
     * @param fieldName is the field's field name to test
     * @return a String error message if the field doesn't exist, or null to indicate success
     */
    public static String exists(EventType type, String fieldName) {
        Class clazz = getClass(type, fieldName);

        if (clazz == null) {
            return "Parent view does not contain a field named '" + fieldName + '\'';
        }

        return null;
    }

    /**
     * Check if the fields identified by the field names both exists according to the schema.
     *
     * @param type         contains metadata about fields
     * @param fieldNameOne is the first field's field name to test
     * @param fieldNameTwo is the first field's field name to test
     * @return a String error message if either of the fields doesn't exist, or null to indicate success
     */
    public static String exists(EventType type, String fieldNameOne, String fieldNameTwo) {
        Class clazz = getClass(type, fieldNameOne);

        if (clazz == null) {
            return "Parent view does not contain a field named '" + fieldNameOne + '\'';
        }

        clazz = getClass(type, fieldNameTwo);

        if (clazz == null) {
            return "Parent view does not contain a field named '" + fieldNameTwo + '\'';
        }

        return null;
    }

    /**
     * Check if the field identified by the field name is a valid numeric field according to the schema.
     *
     * @param type             contains metadata about fields
     * @param numericFieldName is the field's field name to test
     * @return a String error message if the field doesn't exist or is not numeric, or null to indicate success
     */
    public static String checkNumeric(EventType type, String numericFieldName) {
        return checkFieldNumeric(type, numericFieldName);
    }

    /**
     * Check if the fields identified by their field names are valid numeric field according to the schema.
     *
     * @param type              contains metadata about fields
     * @param numericFieldNameX is the first field's field name to test
     * @param numericFieldNameY is the second field's field name to test
     * @return a String error message if the field doesn't exist or is not numeric, or null to indicate success
     */
    public static String checkNumeric(EventType type, String numericFieldNameX, String numericFieldNameY) {
        String error = checkFieldNumeric(type, numericFieldNameX);
        if (error != null) {
            return error;
        }

        return checkFieldNumeric(type, numericFieldNameY);
    }

    /**
     * Check if the field identified by the field name is of type long according to the schema.
     *
     * @param type          contains metadata about fields
     * @param longFieldName is the field's field name to test
     * @return a String error message if the field doesn't exist or is not a long, or null to indicate success
     */
    public static String checkLong(EventType type, String longFieldName) {
        Class clazz = getClass(type, longFieldName);

        if (clazz == null) {
            return "Parent view does not contain a field named '" + longFieldName + '\'';
        }

        if ((clazz != Long.class) && (clazz != long.class)) {
            return "Parent view field named '" + longFieldName + "' is not of type long";
        }

        return checkFieldNumeric(type, longFieldName);
    }

    /**
     * Returns the class for the field as defined in the schema.
     *
     * @param type      contains metadata about fields
     * @param fieldName is the field's name to return the type for
     * @return type of field.
     */
    private static Class getClass(EventType type, String fieldName) {
        return type.getPropertyType(fieldName);
    }

    // Perform the schema checking for if a field exists and is numeric
    private static String checkFieldNumeric(EventType type, String numericFieldName) {
        Class clazz = getClass(type, numericFieldName);

        if (clazz == null) {
            return "Parent view does not contain a field named '" + numericFieldName + '\'';
        }

        if (!JavaClassHelper.isNumeric(clazz)) {
            return "Parent view field named '" + numericFieldName + "' is not a number";
        }

        return null;
    }

}
