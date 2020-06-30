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

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public class ClassDescriptor {
    public final static String PRIMITIVE_KEYWORD = "primitive";

    private final String classIdentifier;
    private List<ClassDescriptor> typeParameters;
    private int arrayDimensions;
    private boolean arrayOfPrimitive;

    public ClassDescriptor(String classIdentifier) {
        this.classIdentifier = classIdentifier;
        this.typeParameters = Collections.emptyList();
        this.arrayDimensions = 0;
        this.arrayOfPrimitive = false;
    }

    public ClassDescriptor(String classIdentifier, List<ClassDescriptor> typeParameters, int arrayDimensions, boolean arrayOfPrimitive) {
        this.classIdentifier = classIdentifier;
        this.typeParameters = typeParameters;
        this.arrayDimensions = arrayDimensions;
        this.arrayOfPrimitive = arrayOfPrimitive;
    }

    public static ClassDescriptor parseTypeText(String typeName) {
        return ClassDescriptorParser.parse(typeName);
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

    public List<ClassDescriptor> getTypeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(List<ClassDescriptor> typeParameters) {
        this.typeParameters = typeParameters;
    }

    public void setArrayDimensions(int arrayDimensions) {
        this.arrayDimensions = arrayDimensions;
    }

    public void setArrayOfPrimitive(boolean arrayOfPrimitive) {
        this.arrayOfPrimitive = arrayOfPrimitive;
    }

    public String toEPL() {
        StringWriter writer = new StringWriter();
        toEPL(writer);
        return writer.toString();
    }

    public void toEPL(StringWriter writer) {
        writer.append(classIdentifier);
        if (arrayDimensions == 0 && typeParameters.isEmpty()) {
            return;
        }
        if (!typeParameters.isEmpty()) {
            writer.append("<");
            String delimiter = "";
            for (ClassDescriptor typeParameter : typeParameters) {
                writer.append(delimiter);
                typeParameter.toEPL(writer);
                delimiter = ",";
            }
            writer.append(">");
        }
        if (arrayDimensions > 0) {
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassDescriptor that = (ClassDescriptor) o;

        if (arrayDimensions != that.arrayDimensions) return false;
        if (arrayOfPrimitive != that.arrayOfPrimitive) return false;
        if (!classIdentifier.equals(that.classIdentifier)) return false;
        return typeParameters.equals(that.typeParameters);
    }

    public int hashCode() {
        int result = classIdentifier.hashCode();
        result = 31 * result + typeParameters.hashCode();
        result = 31 * result + arrayDimensions;
        result = 31 * result + (arrayOfPrimitive ? 1 : 0);
        return result;
    }

    public String toString() {
        return "ClassIdentifierWArray{" +
            "classIdentifier='" + classIdentifier + '\'' +
            ", typeParameters=" + typeParameters +
            ", arrayDimensions=" + arrayDimensions +
            ", arrayOfPrimitive=" + arrayOfPrimitive +
            '}';
    }
}
