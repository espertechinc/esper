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
package com.espertech.esper.runtime.client.util;

/**
 * EPL object type.
 */
public enum EPObjectType {
    /**
     * Context.
     */
    CONTEXT(),
    /**
     * Named window.
     */
    NAMEDWINDOW(),
    /**
     * Event type.
     */
    EVENTTYPE(),
    /**
     * Table.
     */
    TABLE(),
    /**
     * Variable
     */
    VARIABLE(),
    /**
     * Expression.
     */
    EXPRESSION(),
    /**
     * Script.
     */
    SCRIPT(),
    /**
     * Index.
     */
    INDEX();
}
