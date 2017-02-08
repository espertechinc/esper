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
package com.espertech.esper.avro.core;

import com.espertech.esper.client.ConfigurationEventTypeAvro;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import org.apache.avro.Schema;

import java.util.Set;

public class EventAdapterAvroHandlerImpl extends EventAdapterAvroHandlerBase {

    public AvroSchemaEventType makeType(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterService eventAdapterService, Schema schema, ConfigurationEventTypeAvro optionalConfig, EventType[] supertypes, Set<EventType> deepSupertypes) {
        return new AvroEventType(metadata, eventTypeName, typeId, eventAdapterService, schema, optionalConfig == null ? null : optionalConfig.getStartTimestampPropertyName(), optionalConfig == null ? null : optionalConfig.getEndTimestampPropertyName(), supertypes, deepSupertypes);
    }
}
