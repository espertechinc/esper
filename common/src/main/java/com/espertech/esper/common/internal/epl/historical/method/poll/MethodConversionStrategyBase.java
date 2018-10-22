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
package com.espertech.esper.common.internal.epl.historical.method.poll;

import com.espertech.esper.common.client.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MethodConversionStrategyBase implements MethodConversionStrategy {
    private static final Logger log = LoggerFactory.getLogger(MethodConversionStrategyBase.class);

    protected EventType eventType;

    protected boolean checkNonNullArrayValue(Object value, MethodTargetStrategy origin) {
        if (value == null) {
            log.warn("Expected non-null return result from " + origin.getPlan() + ", but received null array element value");
            return false;
        }
        return true;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
