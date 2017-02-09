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

import java.util.Map;

/**
 * A collection of context partitions each uniquely identified by a context partition id (agent instance id).
 */
public class ContextPartitionCollection {
    private final Map<Integer, ContextPartitionDescriptor> descriptors;

    /**
     * Ctor.
     *
     * @param descriptors per agent instance id
     */
    public ContextPartitionCollection(Map<Integer, ContextPartitionDescriptor> descriptors) {
        this.descriptors = descriptors;
    }

    /**
     * Returns the descriptors per agent instance id
     *
     * @return descriptors
     */
    public Map<Integer, ContextPartitionDescriptor> getDescriptors() {
        return descriptors;
    }
}
