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

public class ContextControllerPartitionedEntry {

    private final ContextControllerInstanceHandle instanceHandle;
    private final ContextControllerCondition optionalTermination;

    public ContextControllerPartitionedEntry(ContextControllerInstanceHandle instanceHandle, ContextControllerCondition optionalTermination) {
        this.instanceHandle = instanceHandle;
        this.optionalTermination = optionalTermination;
    }

    public ContextControllerInstanceHandle getInstanceHandle() {
        return instanceHandle;
    }

    public ContextControllerCondition getOptionalTermination() {
        return optionalTermination;
    }
}
