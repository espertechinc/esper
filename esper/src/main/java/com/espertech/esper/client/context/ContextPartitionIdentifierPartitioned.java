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
 * Context partition identifier for segmented contexts.
 */
public class ContextPartitionIdentifierPartitioned extends ContextPartitionIdentifier {
    private static final long serialVersionUID = 426396467569470582L;
    private Object[] keys;

    /**
     * Ctor.
     */
    public ContextPartitionIdentifierPartitioned() {
    }

    /**
     * Ctor.
     *
     * @param keys partitioning keys
     */
    public ContextPartitionIdentifierPartitioned(Object[] keys) {
        this.keys = keys;
    }

    /**
     * Returns the partition keys.
     *
     * @return keys
     */
    public Object[] getKeys() {
        return keys;
    }

    /**
     * Sets the partition keys.
     *
     * @param keys to set
     */
    public void setKeys(Object[] keys) {
        this.keys = keys;
    }

    public boolean compareTo(ContextPartitionIdentifier other) {
        if (!(other instanceof ContextPartitionIdentifierPartitioned)) {
            return false;
        }
        return Arrays.equals(keys, ((ContextPartitionIdentifierPartitioned) other).keys);
    }

    public String toString() {
        return "ContextPartitionIdentifierPartitioned{" +
                "keys=" + Arrays.toString(keys) +
                '}';
    }
}
