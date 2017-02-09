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
package com.espertech.esper.client.hook;

/**
 * As part of a lookup context, see {@link VirtualDataWindowLookupContext}, this object encapsulates information about a single
 * property in a correlated where-clause.
 */
public class VirtualDataWindowLookupFieldDesc {
    private String propertyName;
    private VirtualDataWindowLookupOp operator;
    private Class lookupValueType;

    /**
     * Ctor.
     *
     * @param propertyName    property name queried in where-clause
     * @param operator        operator
     * @param lookupValueType lookup key type
     */
    public VirtualDataWindowLookupFieldDesc(String propertyName, VirtualDataWindowLookupOp operator, Class lookupValueType) {
        this.propertyName = propertyName;
        this.operator = operator;
        this.lookupValueType = lookupValueType;
    }

    /**
     * Sets the operator.
     *
     * @param operator to set
     */
    public void setOperator(VirtualDataWindowLookupOp operator) {
        this.operator = operator;
    }

    /**
     * Sets the lookup value type.
     *
     * @param lookupValueType type
     */
    public void setLookupValueType(Class lookupValueType) {
        this.lookupValueType = lookupValueType;
    }

    /**
     * Returns the property name queried in the where-clause.
     *
     * @return property name.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the type of lookup value provided.
     *
     * @return lookup value type (aka. key type)
     */
    public Class getLookupValueType() {
        return lookupValueType;
    }

    /**
     * Returns the operator.
     *
     * @return operator
     */
    public VirtualDataWindowLookupOp getOperator() {
        return operator;
    }
}
