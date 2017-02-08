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

import com.espertech.esper.client.context.ContextPartitionState;

public class ContextStatePathValue {
    private final Integer optionalContextPartitionId;
    private final byte[] blob;
    private ContextPartitionState state;

    public ContextStatePathValue(Integer optionalContextPartitionId, byte[] blob, ContextPartitionState state) {
        this.optionalContextPartitionId = optionalContextPartitionId;
        this.blob = blob;
        this.state = state;
    }

    public Integer getOptionalContextPartitionId() {
        return optionalContextPartitionId;
    }

    public byte[] getBlob() {
        return blob;
    }

    public ContextPartitionState getState() {
        return state;
    }

    public void setState(ContextPartitionState state) {
        this.state = state;
    }

    public String toString() {
        return "ContextStatePathValue{" +
                "optionalContextPartitionId=" + optionalContextPartitionId +
                ", state=" + state +
                '}';
    }
}

