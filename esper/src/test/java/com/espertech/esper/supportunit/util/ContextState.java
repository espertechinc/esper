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
package com.espertech.esper.supportunit.util;

public class ContextState {
    private final int level;
    private final int parentPath;
    private final int subpath;
    private final int agentInstanceId;
    private final Object payload;
    private final boolean started;

    public ContextState(int level, int parentPath, int subpath, int agentInstanceId, Object payload, boolean started) {
        this.level = level;
        this.parentPath = parentPath;
        this.subpath = subpath;
        this.agentInstanceId = agentInstanceId;
        this.payload = payload;
        this.started = started;
    }

    public int getLevel() {
        return level;
    }

    public int getParentPath() {
        return parentPath;
    }

    public int getSubpath() {
        return subpath;
    }

    public int getAgentInstanceId() {
        return agentInstanceId;
    }

    public Object getPayload() {
        return payload;
    }

    public boolean isStarted() {
        return started;
    }
}
