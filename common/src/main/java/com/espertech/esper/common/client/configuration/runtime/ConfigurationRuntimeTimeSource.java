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
package com.espertech.esper.common.client.configuration.runtime;

import com.espertech.esper.common.client.util.TimeSourceType;

import java.io.Serializable;

/**
 * Time source configuration, the default in MILLI (millisecond resolution from System.currentTimeMillis).
 */
public class ConfigurationRuntimeTimeSource implements Serializable {
    private static final long serialVersionUID = 5082072580765252557L;

    private TimeSourceType timeSourceType;

    /**
     * Ctor.
     */
    public ConfigurationRuntimeTimeSource() {
        timeSourceType = TimeSourceType.MILLI;
    }

    /**
     * Returns the time source type.
     *
     * @return time source type enum
     */
    public TimeSourceType getTimeSourceType() {
        return timeSourceType;
    }

    /**
     * Sets the time source type.
     *
     * @param timeSourceType time source type enum
     */
    public void setTimeSourceType(TimeSourceType timeSourceType) {
        this.timeSourceType = timeSourceType;
    }

}
