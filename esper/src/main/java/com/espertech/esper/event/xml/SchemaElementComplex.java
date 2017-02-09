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
package com.espertech.esper.event.xml;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a complex element possibly with attributes, simple elements, other complex child elements
 * and may itself have a simple type.
 */
public class SchemaElementComplex implements SchemaElement, Serializable {
    private static final long serialVersionUID = 1636920955323577240L;
    private String name;
    private String namespace;
    private List<SchemaItemAttribute> attributes;
    private List<SchemaElementSimple> simpleElements;
    private List<SchemaElementComplex> children;
    private boolean isArray;
    private Short optionalSimpleType;     // If not null then the complex element itself has a type defined for it.
    private String optionalSimpleTypeName;     // If not null then the complex element itself has a type defined for it.

    /**
     * Ctor.
     *
     * @param name                   the element name
     * @param namespace              the element namespace
     * @param attributes             the attributes or empty if none
     * @param children               the child complex elements or empty if none
     * @param simpleElements         the simple elements or empty if none
     * @param isArray                if unbound or max&gt;1
     * @param optionalSimpleType     if the element does itself have a type
     * @param optionalSimpleTypeName if the element does itself have a type
     */
    public SchemaElementComplex(String name, String namespace, List<SchemaItemAttribute> attributes, List<SchemaElementComplex> children, List<SchemaElementSimple> simpleElements, boolean isArray, Short optionalSimpleType, String optionalSimpleTypeName) {
        this.name = name;
        this.namespace = namespace;
        this.attributes = attributes;
        this.children = children;
        this.simpleElements = simpleElements;
        this.isArray = isArray;
        this.optionalSimpleType = optionalSimpleType;
        this.optionalSimpleTypeName = optionalSimpleTypeName;
    }

    /**
     * Returns the name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns attributes.
     *
     * @return attributes
     */
    public List<SchemaItemAttribute> getAttributes() {
        return attributes;
    }

    /**
     * Returns attributes.
     *
     * @return attributes
     */
    public List<SchemaElementComplex> getChildren() {
        return children;
    }

    /**
     * Returns simple child elements.
     *
     * @return simple child elements
     */
    public List<SchemaElementSimple> getSimpleElements() {
        return simpleElements;
    }

    /**
     * Returns true if unbound or max greater one.
     *
     * @return true if array
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Null if not a simple type declared, or type if declared.
     *
     * @return simple type name
     */
    public Short getOptionalSimpleType() {
        return optionalSimpleType;
    }

    /**
     * Returns type name if itself has a type.
     *
     * @return type name
     */
    public String getOptionalSimpleTypeName() {
        return optionalSimpleTypeName;
    }

    public String toString() {
        return "Complex " + namespace + " " + name;
    }
}
