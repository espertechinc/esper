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
package com.espertech.esper.supportunit.event;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.ConfigurationRevisionEventType;
import com.espertech.esper.client.ConfigurationVariantStream;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeIdGenerator;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.view.StatementStopService;

import java.util.Map;

public class SupportValueAddEventService implements ValueAddEventService {
    public void init(Map<String, ConfigurationRevisionEventType> revisionTypes, Map<String, ConfigurationVariantStream> variantStreams, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator) {

    }

    public void addRevisionEventType(String name, ConfigurationRevisionEventType config, EventAdapterService eventAdapterService) {

    }

    public void addVariantStream(String variantEventTypeName, ConfigurationVariantStream variantStreamConfig, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator) throws ConfigurationException {

    }

    public EventType getValueAddUnderlyingType(String name) {
        return null;
    }

    public EventType createRevisionType(String namedWindowName, String typeName, StatementStopService statementStopService, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator) {
        return null;
    }

    public boolean isRevisionTypeName(String name) {
        return false;
    }

    public ValueAddEventProcessor getValueAddProcessor(String name) {
        return null;
    }

    public EventType[] getValueAddedTypes() {
        return new EventType[0];
    }
}
