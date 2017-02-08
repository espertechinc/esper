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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.context.ContextPartitionDescriptor;

import java.util.Map;
import java.util.TreeMap;

public class ContextStatePathDescriptor {
    private final TreeMap<ContextStatePathKey, ContextStatePathValue> paths;
    private final Map<Integer, ContextPartitionDescriptor> contextPartitionInformation;

    public ContextStatePathDescriptor(TreeMap<ContextStatePathKey, ContextStatePathValue> paths, Map<Integer, ContextPartitionDescriptor> contextPartitionInformation) {
        this.paths = paths;
        this.contextPartitionInformation = contextPartitionInformation;
    }

    public TreeMap<ContextStatePathKey, ContextStatePathValue> getPaths() {
        return paths;
    }

    public Map<Integer, ContextPartitionDescriptor> getContextPartitionInformation() {
        return contextPartitionInformation;
    }
}

