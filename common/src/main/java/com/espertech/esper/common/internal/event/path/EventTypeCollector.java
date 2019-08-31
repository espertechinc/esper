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
package com.espertech.esper.common.internal.event.path;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.event.json.core.JsonEventTypeDetail;

import java.util.LinkedHashMap;
import java.util.Set;

public interface EventTypeCollector {
    void registerMap(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes,
                     String startTimestampPropertyName, String endTimestampPropertyName);

    void registerObjectArray(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes,
                             String startTimestampPropertyName, String endTimestampPropertyName);

    void registerWrapper(EventTypeMetadata metadata, EventType underlying, LinkedHashMap<String, Object> properties);

    void registerBean(EventTypeMetadata metadata, Class clazz, String startTimestampName, String endTimestampName,
                      EventType[] superTypes, Set<EventType> deepSupertypes);

    void registerXML(EventTypeMetadata metadata, String representsFragmentOfProperty, String representsOriginalTypeName);

    void registerXMLNewType(EventTypeMetadata metadata, ConfigurationCommonEventTypeXMLDOM config);

    void registerAvro(EventTypeMetadata metadata, String schemaJson, String[] superTypes);

    void registerJson(EventTypeMetadata metadata, LinkedHashMap<String, Object> properties, String[] superTypes,
                      String startTimestampPropertyName, String endTimestampPropertyName, JsonEventTypeDetail detail);

    void registerVariant(EventTypeMetadata metadata, EventType[] variants, boolean any);

    void registerSerde(EventTypeMetadata metadata, DataInputOutputSerde<Object> underlyingSerde, Class underlyingClass);
}
