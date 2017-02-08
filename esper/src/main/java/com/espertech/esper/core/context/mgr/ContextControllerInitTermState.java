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

import java.io.Serializable;
import java.util.Map;

/**
 * State of the overlapping and non-overlapping context.
 * Serializable for the purpose of SPI testing.
 */
public class ContextControllerInitTermState implements Serializable {

    private static final long serialVersionUID = 5940952673981877766L;
    private final long startTime;
    private final Map<String, Object> patternData;

    public ContextControllerInitTermState(long startTime, Map<String, Object> patternData) {
        this.startTime = startTime;
        this.patternData = patternData;
    }

    public long getStartTime() {
        return startTime;
    }

    public Map<String, Object> getPatternData() {
        return patternData;
    }
}
