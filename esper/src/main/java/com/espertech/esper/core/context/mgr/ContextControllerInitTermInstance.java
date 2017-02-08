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

import java.util.Map;

public class ContextControllerInitTermInstance {

    private final ContextControllerInstanceHandle instanceHandle;
    private final Map<String, Object> startProperties;
    private final long startTime;
    private final Long endTime;
    private final int subPathId;

    public ContextControllerInitTermInstance(ContextControllerInstanceHandle instanceHandle, Map<String, Object> startProperties, long startTime, Long endTime, int subPathId) {
        this.instanceHandle = instanceHandle;
        this.startProperties = startProperties;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subPathId = subPathId;
    }

    public ContextControllerInstanceHandle getInstanceHandle() {
        return instanceHandle;
    }

    public Map<String, Object> getStartProperties() {
        return startProperties;
    }

    public long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public int getSubPathId() {
        return subPathId;
    }
}
