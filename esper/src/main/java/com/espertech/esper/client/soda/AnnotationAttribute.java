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

/**
 * Represents a single annotation attribute, the value of which may itself be a single value, array or further annotations.
 */
public class AnnotationAttribute implements Serializable {

    private static final long serialVersionUID = -2448173068516111756L;

    private String name;
    private Serializable value;

    /**
     * Ctor.
     */
    public AnnotationAttribute() {
    }

    /**
     * Ctor.
     *
     * @param name  annotation name
     * @param value annotation value, could be a primitive, array or another annotation
     */
    public AnnotationAttribute(String name, Object value) {
        this.name = name;
        this.value = (Serializable) value;
    }

    /**
     * Returns annotation name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets annotation name.
     *
     * @param name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns annotation value.
     *
     * @return value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets annotation value.
     *
     * @param value to set
     */
    public void setValue(Object value) {
        this.value = (Serializable) value;
    }
}