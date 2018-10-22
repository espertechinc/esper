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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Set;

public class EventTypeAvroHandlerImpl extends EventTypeAvroHandlerBase {
    public AvroSchemaEventType makeType(EventTypeMetadata metadata, EventBeanTypedEventFactory eventBeanTypedEventFactory, Schema schema, ConfigurationCommonEventTypeAvro optionalConfig, EventType[] supertypes, Set<EventType> deepSupertypes) {
        return new AvroEventType(metadata, schema, optionalConfig == null ? null : optionalConfig.getStartTimestampPropertyName(), optionalConfig == null ? null : optionalConfig.getEndTimestampPropertyName(), supertypes, deepSupertypes, eventBeanTypedEventFactory, this);
    }

    public EventBean adapterForTypeAvro(Object avroGenericDataDotRecord, EventType existingType) {
        if (!(avroGenericDataDotRecord instanceof GenericData.Record)) {
            throw new EPException("Unexpected event object type '" + (avroGenericDataDotRecord == null ? "null" : avroGenericDataDotRecord.getClass().getName()) + "' encountered, please supply a GenericData.Record");
        }

        GenericData.Record record = (GenericData.Record) avroGenericDataDotRecord;
        return new AvroGenericDataEventBean(record, existingType);
    }
}
