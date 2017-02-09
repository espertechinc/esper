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
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a XSD schema or other metadata for a class of XML documents.
 */
public class SchemaModel implements Serializable {
    private static final long serialVersionUID = -8949230627736368311L;
    private List<SchemaElementComplex> components;
    private List<String> namespaces;

    /**
     * Ctor.
     *
     * @param components the top level components.
     * @param namespaces list of namespaces
     */
    public SchemaModel(List<SchemaElementComplex> components, List<String> namespaces) {
        this.components = components;
        this.namespaces = namespaces;
    }

    /**
     * Ctor.
     *
     * @param component  top level component
     * @param namespaces list of namespaces
     */
    public SchemaModel(SchemaElementComplex component, List<String> namespaces) {
        components = new ArrayList<SchemaElementComplex>(1);
        components.add(component);
        this.namespaces = namespaces;
    }

    /**
     * Returns top-level components.
     *
     * @return components
     */
    public List<SchemaElementComplex> getComponents() {
        return components;
    }

    /**
     * Returns namespaces.
     *
     * @return namespaces
     */
    public List<String> getNamespaces() {
        return namespaces;
    }
}
