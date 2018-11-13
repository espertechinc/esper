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
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariantStream;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactory;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.event.variant.VariantSpec;
import com.espertech.esper.common.internal.util.CRC32Util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class EventTypeRepositoryVariantStreamUtil {
    public static void buildVariantStreams(EventTypeRepositoryImpl repo, Map<String, ConfigurationCommonVariantStream> variantStreams, EventTypeFactory eventTypeFactory) {
        for (Map.Entry<String, ConfigurationCommonVariantStream> entry : variantStreams.entrySet()) {
            if (repo.getTypeByName(entry.getKey()) != null) {
                continue;
            }

            addVariantStream(entry.getKey(), entry.getValue(), repo, eventTypeFactory);
        }
    }

    /**
     * Validate the variant stream definition.
     *
     * @param variantStreamname   the stream name
     * @param variantStreamConfig the configuration information
     * @param repo                the event types
     * @return specification for variant streams
     */
    private static VariantSpec validateVariantStream(String variantStreamname, ConfigurationCommonVariantStream variantStreamConfig, EventTypeRepositoryImpl repo) {
        if (variantStreamConfig.getTypeVariance() == ConfigurationCommonVariantStream.TypeVariance.PREDEFINED) {
            if (variantStreamConfig.getVariantTypeNames().isEmpty()) {
                throw new ConfigurationException("Invalid variant stream configuration, no event type name has been added and default type variance requires at least one type, for name '" + variantStreamname + "'");
            }
        }

        Set<EventType> types = new LinkedHashSet<EventType>();
        for (String typeName : variantStreamConfig.getVariantTypeNames()) {
            EventType type = repo.getTypeByName(typeName);
            if (type == null) {
                throw new ConfigurationException("Event type by name '" + typeName + "' could not be found for use in variant stream configuration by name '" + variantStreamname + "'");
            }
            types.add(type);
        }

        EventType[] eventTypes = types.toArray(new EventType[types.size()]);
        return new VariantSpec(eventTypes, variantStreamConfig.getTypeVariance());
    }

    private static void addVariantStream(String name, ConfigurationCommonVariantStream config, EventTypeRepositoryImpl repo, EventTypeFactory eventTypeFactory) {
        VariantSpec variantSpec = validateVariantStream(name, config, repo);
        EventTypeMetadata metadata = new EventTypeMetadata(name, null, EventTypeTypeClass.VARIANT, EventTypeApplicationType.VARIANT, NameAccessModifier.PRECONFIGURED, EventTypeBusModifier.BUS, false, new EventTypeIdPair(CRC32Util.computeCRC32(name), -1));
        VariantEventType variantEventType = eventTypeFactory.createVariant(metadata, variantSpec);
        repo.addType(variantEventType);
    }
}
