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
package com.espertech.esper.epl.annotation;

/**
 * Represents a attribute of an annotation.
 */
public class AnnotationAttribute {
    private final String name;
    private final Class type;
    private final Object defaultValue;

    /**
     * Ctor.
     *
     * @param name         name of attribute
     * @param type         attribute type
     * @param defaultValue default value, if any is specified
     */
    public AnnotationAttribute(String name, Class type, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns attribute name.
     *
     * @return attribute name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns attribute type.
     *
     * @return attribute type
     */
    public Class getType() {
        return type;
    }

    /**
     * Returns default value of annotation.
     *
     * @return default value
     */
    public Object getDefaultValue() {
        return defaultValue;
    }
}
