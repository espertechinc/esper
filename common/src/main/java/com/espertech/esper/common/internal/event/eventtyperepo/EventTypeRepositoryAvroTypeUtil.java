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
package com.espertech.esper.common.internal.event.eventtyperepo;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.CRC32Util;

import java.util.Map;
import java.util.Set;

public class EventTypeRepositoryAvroTypeUtil {
    public static void buildAvroTypes(EventTypeRepositoryImpl repo, Map<String, ConfigurationCommonEventTypeAvro> eventTypesAvro, EventTypeAvroHandler eventTypeAvroHandler, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        for (Map.Entry<String, ConfigurationCommonEventTypeAvro> entry : eventTypesAvro.entrySet()) {
            if (repo.getTypeByName(entry.getKey()) != null) {
                continue;
            }

            buildAvroType(repo, entry.getKey(), entry.getValue(), eventTypeAvroHandler, eventBeanTypedEventFactory);
        }
    }

    private static void buildAvroType(EventTypeRepositoryImpl eventTypeRepositoryPreconfigured, String eventTypeName, ConfigurationCommonEventTypeAvro config, EventTypeAvroHandler eventTypeAvroHandler, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, null, EventTypeTypeClass.APPLICATION, EventTypeApplicationType.AVRO, NameAccessModifier.PRECONFIGURED, EventTypeBusModifier.BUS, false, new EventTypeIdPair(CRC32Util.computeCRC32(eventTypeName), -1));
        Pair<EventType[], Set<EventType>> avroSuperTypes = EventTypeUtility.getSuperTypesDepthFirst(config.getSuperTypes(), EventUnderlyingType.AVRO, eventTypeRepositoryPreconfigured);
        AvroSchemaEventType newEventType = eventTypeAvroHandler.newEventTypeFromSchema(metadata, eventBeanTypedEventFactory, config, avroSuperTypes.getFirst(), avroSuperTypes.getSecond());
        eventTypeRepositoryPreconfigured.addType(newEventType);
    }
}
