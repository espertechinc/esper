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

import com.espertech.esper.core.context.util.ContextPartitionImportCallback;

import java.util.TreeMap;

public class ContextControllerState {
    private final TreeMap<ContextStatePathKey, ContextStatePathValue> states;
    private final boolean imported;
    private final ContextPartitionImportCallback partitionImportCallback;

    public ContextControllerState(TreeMap<ContextStatePathKey, ContextStatePathValue> states, boolean imported, ContextPartitionImportCallback partitionImportCallback) {
        this.states = states;
        this.imported = imported;
        this.partitionImportCallback = partitionImportCallback;
    }

    public TreeMap<ContextStatePathKey, ContextStatePathValue> getStates() {
        return states;
    }

    public boolean isImported() {
        return imported;
    }

    public ContextPartitionImportCallback getPartitionImportCallback() {
        return partitionImportCallback;
    }
}
