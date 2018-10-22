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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariantStream;

/**
 * Specification for a variant event stream.
 */
public class VariantSpec {
    private final EventType[] eventTypes;
    private final ConfigurationCommonVariantStream.TypeVariance typeVariance;

    /**
     * Ctor.
     *
     * @param eventTypes   types of events for variant stream, or empty list
     * @param typeVariance enum specifying type variance
     */
    public VariantSpec(EventType[] eventTypes, ConfigurationCommonVariantStream.TypeVariance typeVariance) {
        this.eventTypes = eventTypes;
        this.typeVariance = typeVariance;
    }

    /**
     * Returns types allowed for variant streams.
     *
     * @return types
     */
    public EventType[] getEventTypes() {
        return eventTypes;
    }

    /**
     * Returns the type variance enum.
     *
     * @return type variance
     */
    public ConfigurationCommonVariantStream.TypeVariance getTypeVariance() {
        return typeVariance;
    }
}
