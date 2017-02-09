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
package com.espertech.esper.event.xml;

/**
 * Pair of namespace and name.
 */
public class NamespaceNamePair {
    private final String namespace;
    private final String name;

    /**
     * Ctor.
     *
     * @param namespace namespace
     * @param name      name
     */
    public NamespaceNamePair(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    /**
     * Returns the name.
     *
     * @return name part
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the namespace.
     *
     * @return namespace part
     */
    public String getNamespace() {
        return namespace;
    }

    public String toString() {
        return namespace + " " + name;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NamespaceNamePair that = (NamespaceNamePair) o;

        if (!name.equals(that.name)) {
            return false;
        }
        if (!namespace.equals(that.namespace)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = namespace.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
