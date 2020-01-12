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
    CONTEXT("context"),
    /**
     * Named window.
     */
    NAMEDWINDOW("named window"),
    /**
     * Event type.
     */
    EVENTTYPE("event type"),
    /**
     * Table.
     */
    TABLE("table"),
    /**
     * Variable
     */
    VARIABLE("variable"),
    /**
     * Expression.
     */
    EXPRESSION("expression"),
    /**
     * Script.
     */
    SCRIPT("script"),
    /**
     * Index.
     */
    INDEX("index");

    private final String prettyName;

    EPObjectType(String prettyName) {
        this.prettyName = prettyName;
    }

    /**
     * Returns the pretty-print name
     * @return name
     */
    public String getPrettyName() {
        return prettyName;
    }
}
