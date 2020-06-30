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

import java.io.Serializable;

/**
 * {@link EPType} is the common superinterface for all types in EPL.
 * <p>
 *     The EPL compiler and runtime do not use {@link Class} as Java type erasure means that a class instance
 *     does not provide information about its type parameters. For instance the type <code>List&lt;String&gt;</code>
 *     has <code>String</code> as the type parameter. Looking at <code>List.class</code> alone
 *     does not provide such type information.
 * </p>
 * <p>
 *     Further, EPL (and modern SQL) has Three-Valued Logic (3VL), wherein the <code>null</code>-value
 *     returns a value of type null. Java does not have a representation for the type null.
 *     EPL allows null-typed columns, such as in <code>create schema MyEvent(emptyColumn null)</code>.
 *     EPL allows null to occur in the select-clause, as exemplified in <code>select null as item from OrderEvent</code>.
 * </p>
 * <p>
 *     The null-type is represented by {@link EPTypeNull}. All other types are represented by {@link EPTypeClass} or
 *     {@link EPTypeClassParameterized}. This allows the EPL compiler and runtime to track null-type
 *     and parameterized types.
 * </p>
 */
public interface EPType extends Serializable {
    /**
     * Returns the type name.
     * @return type name
     */
    String getTypeName();
}
