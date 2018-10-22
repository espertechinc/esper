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
package com.espertech.esper.common.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Descriptor for use in create-schema syntax to define property name and type of an event property.
 */
public class SchemaColumnDesc implements Serializable {
    private static final long serialVersionUID = 5068685531968720148L;

    private String name;
    private String type;

    /**
     * Ctor.
     */
    public SchemaColumnDesc() {
    }

    /**
     * Ctor.
     *
     * @param name column name
     * @param type type name
     */
    public SchemaColumnDesc(String name, String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Returns property name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns property type.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Set property name.
     *
     * @param name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set property type.
     *
     * @param type type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Render to EPL.
     *
     * @param writer to render to
     */
    public void toEPL(StringWriter writer) {
        writer.write(name);
        writer.write(' ');
        writer.write(type);
    }

}
