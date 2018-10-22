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
package com.espertech.esper.common.internal.type;

import com.espertech.esper.common.client.EPException;

import java.io.StringWriter;
import java.util.Locale;

public class ClassIdentifierWArray {
    public final static String PRIMITIVE_KEYWORD = "primitive";

    private final String classIdentifier;
    private final int arrayDimensions;
    private final boolean arrayOfPrimitive;

    public ClassIdentifierWArray(String classIdentifier) {
        this.classIdentifier = classIdentifier;
        this.arrayDimensions = 0;
        this.arrayOfPrimitive = false;
    }

    public ClassIdentifierWArray(String classIdentifier, int arrayDimensions, boolean arrayOfPrimitive) {
        this.classIdentifier = classIdentifier;
        this.arrayDimensions = arrayDimensions;
        this.arrayOfPrimitive = arrayOfPrimitive;
    }

    public static ClassIdentifierWArray parseSODA(String typeName) {
        int indexStart = typeName.indexOf("[");
        if (indexStart == -1) {
            return new ClassIdentifierWArray(typeName);
        }

        String name = typeName.substring(0, indexStart);
        String arrayPart = typeName.substring(indexStart).toLowerCase(Locale.ENGLISH).trim();
        arrayPart = arrayPart.replace(" ", "");
        String primitive = "[" + PRIMITIVE_KEYWORD + "]";
        if (!arrayPart.startsWith("[]") && !arrayPart.startsWith(primitive)) {
            throw new EPException("Invalid array keyword '" + arrayPart + "', expected ']' or '" + ClassIdentifierWArray.PRIMITIVE_KEYWORD + "'");
        }
        boolean arrayOfPrimitive = arrayPart.startsWith(primitive);
        if (arrayPart.equals("[]") || arrayPart.equals(primitive)) {
            return new ClassIdentifierWArray(name, 1, arrayOfPrimitive);
        }
        int dimensions = arrayPart.split("[]]", -1).length - 1;
        return new ClassIdentifierWArray(name, dimensions, arrayOfPrimitive);
    }

    public String getClassIdentifier() {
        return classIdentifier;
    }

    public int getArrayDimensions() {
        return arrayDimensions;
    }

    public boolean isArrayOfPrimitive() {
        return arrayOfPrimitive;
    }

    public String toEPL() {
        StringWriter writer = new StringWriter();
        toEPL(writer);
        return writer.toString();
    }

    public void toEPL(StringWriter writer) {
        writer.append(classIdentifier);
        if (arrayDimensions == 0) {
            return;
        }
        writer.append("[");
        if (arrayOfPrimitive) {
            writer.append("primitive");
        }
        writer.append("]");
        for (int i = 1; i < arrayDimensions; i++) {
            writer.append("[]");
        }
    }
}
