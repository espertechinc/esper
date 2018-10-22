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
package com.espertech.esper.common.internal.collection;

public enum PathRegistryObjectType {
    CONTEXT("context", "A"),
    NAMEDWINDOW("named window", "A"),
    EVENTTYPE("event type", "An"),
    TABLE("table", "A"),
    VARIABLE("variable", "A"),
    EXPRDECL("declared-expression", "A"),
    SCRIPT("script", "A"),
    INDEX("index", "An");

    private final String name;
    private final String prefix;

    PathRegistryObjectType(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }
}
