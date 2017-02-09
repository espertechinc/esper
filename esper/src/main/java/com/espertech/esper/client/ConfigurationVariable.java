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
package com.espertech.esper.client;

import java.io.Serializable;

/**
 * Provides variable configuration.
 */
public class ConfigurationVariable implements Serializable {
    private String type;
    private Object initializationValue;
    private boolean constant;
    private static final long serialVersionUID = 4273849084807284503L;

    /**
     * Returns the variable type as a fully-qualified class name, primitive type or event type name.
     *
     * @return type name
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the variable type as a fully-qualified class name, primitive type or event type name.
     *
     * @param type type name
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the initialization value, or null if none was supplied.
     * <p>
     * String-type initialization values for numeric or boolean types are allowed and are parsed.
     *
     * @return default value
     */
    public Object getInitializationValue() {
        return initializationValue;
    }

    /**
     * Sets the variable type.
     * <p>
     * Variables are scalar values and primitive or boxed Java builtin types are accepted.
     *
     * @param initializationValue the default value or null if the default value is null
     */
    public void setInitializationValue(Object initializationValue) {
        this.initializationValue = initializationValue;
    }

    /**
     * Returns true if the variable is a constant, or false for regular variable.
     *
     * @return true for constant, false for variable
     */
    public boolean isConstant() {
        return constant;
    }

    /**
     * Set to true to indicate that the variable is a constant, or false for regular variable.
     *
     * @param constant true for constant, false for variable
     */
    public void setConstant(boolean constant) {
        this.constant = constant;
    }
}
