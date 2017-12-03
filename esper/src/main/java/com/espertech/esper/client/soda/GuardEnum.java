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

/**
 * Enum for all build-in guards.
 */
public enum GuardEnum {
    /**
     * Timer guard.
     */
    TIMER_WITHIN("timer", "within"),
    TIMER_WITHINMAX("timer", "withinmax"),
    WHILE_GUARD("internal", "while");

    private final String namespace;
    private final String name;

    GuardEnum(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    /**
     * Returns the namespace name.
     *
     * @return namespace name
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns name.
     *
     * @return short name
     */
    public String getName() {
        return name;
    }

    public static boolean isWhile(String namespace, String name) {
        return namespace.equals(WHILE_GUARD.getNamespace()) && (name.equals(WHILE_GUARD.getName()));
    }
}
