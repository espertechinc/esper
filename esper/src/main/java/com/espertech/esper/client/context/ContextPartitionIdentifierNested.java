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
package com.espertech.esper.client.context;

import java.util.Arrays;

/**
 * Context partition identifier for nested contexts.
 */
public class ContextPartitionIdentifierNested extends ContextPartitionIdentifier {
    private static final long serialVersionUID = -6111517958714806085L;
    private ContextPartitionIdentifier[] identifiers;

    /**
     * Ctor.
     */
    public ContextPartitionIdentifierNested() {
    }

    /**
     * Ctor.
     *
     * @param identifiers nested identifiers, count should match nesting level of context
     */
    public ContextPartitionIdentifierNested(ContextPartitionIdentifier[] identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Returns nested partition identifiers.
     *
     * @return identifiers
     */
    public ContextPartitionIdentifier[] getIdentifiers() {
        return identifiers;
    }

    /**
     * Sets nested partition identifiers.
     *
     * @param identifiers identifiers
     */
    public void setIdentifiers(ContextPartitionIdentifier[] identifiers) {
        this.identifiers = identifiers;
    }

    public boolean compareTo(ContextPartitionIdentifier other) {
        if (!(other instanceof ContextPartitionIdentifierNested)) {
            return false;
        }
        ContextPartitionIdentifierNested nestedOther = (ContextPartitionIdentifierNested) other;
        if (nestedOther.getIdentifiers().length != identifiers.length) {
            return false;
        }
        for (int i = 0; i < identifiers.length; i++) {
            if (!identifiers[i].compareTo(nestedOther.getIdentifiers()[i])) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "ContextPartitionIdentifierNested{" +
                "identifiers=" + (identifiers == null ? null : Arrays.asList(identifiers)) +
                '}';
    }
}
