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

/**
 * Context partition identifier for hash context.
 */
public class ContextPartitionIdentifierHash extends ContextPartitionIdentifier {
    private static final long serialVersionUID = -4175881385322677930L;
    private int hash;

    /**
     * Ctor.
     */
    public ContextPartitionIdentifierHash() {
    }

    /**
     * Ctor.
     *
     * @param hash code
     */
    public ContextPartitionIdentifierHash(int hash) {
        this.hash = hash;
    }

    /**
     * Returns the hash code.
     *
     * @return hash code
     */
    public int getHash() {
        return hash;
    }

    /**
     * Sets the hash code.
     *
     * @param hash hash code
     */
    public void setHash(int hash) {
        this.hash = hash;
    }

    public boolean compareTo(ContextPartitionIdentifier other) {
        if (!(other instanceof ContextPartitionIdentifierHash)) {
            return false;
        }
        return hash == ((ContextPartitionIdentifierHash) other).hash;
    }

    public String toString() {
        return "ContextPartitionIdentifierHash{" +
                "hash=" + hash +
                '}';
    }
}
