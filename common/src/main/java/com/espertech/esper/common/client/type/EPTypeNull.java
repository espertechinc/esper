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
package com.espertech.esper.common.client.type;

/**
 * {@link EPTypeNull} represents a null-type.
 * <p>
 *     EPL allows null-typed columns, such as in <code>create schema MyEvent(emptyColumn null)</code>.
 *     EPL allows null to occur in the select-clause, as exemplified in <code>select null as item from OrderEvent</code>.
 *     Your application may obtain the property type using {@link com.espertech.esper.common.client.EventType#getPropertyEPType(String)}
 *     which returns {@link EPTypeNull#INSTANCE} when the property is of type null.
 * </p>
 * <p>
 *     {@link EPTypeNull} is a singleton.
 * </p>
 */
public class EPTypeNull implements EPType {

    /**
     * Instance.
     */
    public final static EPTypeNull INSTANCE = new EPTypeNull();

    private EPTypeNull() {
    }

    public String getTypeName() {
        return "null";
    }

    public String toString() {
        return getTypeName();
    }
}
