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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single annotation.
 */
public class AnnotationPart implements Serializable {
    private static final long serialVersionUID = 2404842336644400196L;

    private String treeObjectName;
    private String name;

    // Map of identifier name and value can be any of the following:
    //      <"value"|attribute name, constant|array of value (Object[])| AnnotationPart
    private List<AnnotationAttribute> attributes = new ArrayList<AnnotationAttribute>();

    /**
     * Ctor.
     */
    public AnnotationPart() {
    }

    /**
     * Copy annotation values.
     *
     * @param other to copy
     */
    public void copy(AnnotationPart other) {
        name = other.name;
        attributes = other.attributes;
    }

    /**
     * Returns the internal expression id assigned for tools to identify the expression.
     *
     * @return object name
     */
    public String getTreeObjectName() {
        return treeObjectName;
    }

    /**
     * Sets an internal expression id assigned for tools to identify the expression.
     *
     * @param treeObjectName object name
     */
    public void setTreeObjectName(String treeObjectName) {
        this.treeObjectName = treeObjectName;
    }

    /**
     * Ctor.
     *
     * @param name of annotation
     */
    public AnnotationPart(String name) {
        this.name = name;
    }

    /**
     * Ctor.
     *
     * @param name       name of annotation
     * @param attributes are the attribute values
     */
    public AnnotationPart(String name, List<AnnotationAttribute> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    /**
     * Returns annotation interface class name.
     *
     * @return name of class, can be fully qualified
     */
    public String getName() {
        return name;
    }

    /**
     * Sets annotation interface class name.
     *
     * @param name name of class, can be fully qualified
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Add value.
     *
     * @param value to add
     */
    public void addValue(Object value) {
        attributes.add(new AnnotationAttribute("value", value));
    }

    /**
     * Add named value.
     *
     * @param name  name
     * @param value value
     */
    public void addValue(String name, Object value) {
        attributes.add(new AnnotationAttribute(name, value));
    }

    /**
     * Returns annotation attributes.
     *
     * @return the attribute values
     */
    public List<AnnotationAttribute> getAttributes() {
        return attributes;
    }

    /**
     * Print.
     *
     * @param writer      to print to
     * @param annotations annotations
     * @param formatter   for newline-whitespace formatting
     */
    public static void toEPL(StringWriter writer, List<AnnotationPart> annotations, EPStatementFormatter formatter) {
        if ((annotations == null) || (annotations.isEmpty())) {
            return;
        }

        for (AnnotationPart part : annotations) {
            if (part.getName() == null) {
                continue;
            }
            formatter.beginAnnotation(writer);
            part.toEPL(writer);
        }
    }

    /**
     * Print part.
     *
     * @param writer to write to
     */
    public void toEPL(StringWriter writer) {
        writer.append("@");
        writer.append(name);

        if (attributes.isEmpty()) {
            return;
        }

        if (attributes.size() == 1) {
            if (attributes.get(0).getName() == null || attributes.get(0).getName().equals("value")) {
                writer.append("(");
                toEPL(writer, attributes.get(0).getValue());
                writer.append(")");
                return;
            }
        }

        String delimiter = "";
        writer.append("(");
        for (AnnotationAttribute attribute : attributes) {
            if (attribute.getValue() == null) {
                return;
            }
            writer.append(delimiter);
            writer.append(attribute.getName());
            writer.append("=");
            toEPL(writer, attribute.getValue());
            delimiter = ",";
        }
        writer.append(")");
    }

    private void toEPL(StringWriter writer, Object second) {
        if (second instanceof String) {
            writer.append("'");
            writer.append(second.toString());
            writer.append("'");
        } else if (second instanceof AnnotationPart) {
            ((AnnotationPart) second).toEPL(writer);
        } else if (second.getClass().isEnum()) {
            writer.append(second.getClass().getName());
            writer.append(".");
            writer.append(second.toString());
        } else if (second.getClass().isArray()) {
            String delimiter = "";
            writer.append("{");
            for (int i = 0; i < Array.getLength(second); i++) {
                writer.append(delimiter);
                toEPL(writer, Array.get(second, i));
                delimiter = ",";
            }
            writer.append("}");
        } else {
            writer.append(second.toString());
        }
    }
}
