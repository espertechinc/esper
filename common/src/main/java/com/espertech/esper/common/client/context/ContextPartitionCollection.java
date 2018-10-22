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
package com.espertech.esper.common.client.context;

import java.util.Map;

/**
 * A collection of context partitions each uniquely identified by a context partition id (agent instance id).
 */
public class ContextPartitionCollection {
    private final Map<Integer, ContextPartitionIdentifier> identifiers;

    /**
     * Ctor.
     *
     * @param identifiers per agent instance id
     */
    public ContextPartitionCollection(Map<Integer, ContextPartitionIdentifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Returns the identifiers per agent instance id
     *
     * @return descriptors
     */
    public Map<Integer, ContextPartitionIdentifier> getIdentifiers() {
        return identifiers;
    }
}
