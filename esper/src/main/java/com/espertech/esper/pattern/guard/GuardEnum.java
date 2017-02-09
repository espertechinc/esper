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
package com.espertech.esper.pattern.guard;

/**
 * Enum for all build-in guards.
 */
public enum GuardEnum {
    /**
     * Timer guard.
     */
    TIMER_WITHIN("timer", "within", TimerWithinGuardFactory.class),
    TIMER_WITHINMAX("timer", "withinmax", TimerWithinOrMaxCountGuardFactory.class),
    WHILE_GUARD("internal", "while", ExpressionGuardFactory.class);

    private final String namespace;
    private final String name;
    private final Class clazz;

    GuardEnum(String namespace, String name, Class clazz) {
        this.namespace = namespace;
        this.name = name;
        this.clazz = clazz;
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

    /**
     * Gets the implementation class.
     *
     * @return implementation class
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns the enum for the given namespace and name.
     *
     * @param namespace - guard namespace
     * @param name      - guard name
     * @return enum
     */
    public static GuardEnum forName(String namespace, String name) {
        for (GuardEnum guardEnum : GuardEnum.values()) {
            if ((guardEnum.namespace.equals(namespace)) && (guardEnum.name.equals(name))) {
                return guardEnum;
            }
        }

        return null;
    }

    public static boolean isWhile(String namespace, String name) {
        return namespace.equals(WHILE_GUARD.getNamespace()) && (name.equals(WHILE_GUARD.getName()));
    }
}
