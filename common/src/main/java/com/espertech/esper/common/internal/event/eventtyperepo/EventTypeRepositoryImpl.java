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
import com.espertech.esper.common.internal.util.CRC32Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EventTypeRepositoryImpl implements EventTypeRepository {
    private final boolean compileTime;
    private final Map<String, EventType> nameToTypeMap = new HashMap<>();
    private final Map<Long, EventType> idToTypeMap = new HashMap<>();

    public EventTypeRepositoryImpl(boolean compileTime) {
        this.compileTime = compileTime;
    }

    public EventType getTypeByName(String typeName) {
        return nameToTypeMap.get(typeName);
    }

    public EventType getTypeById(long eventTypeIdPublic) {
        return idToTypeMap.get(eventTypeIdPublic);
    }

    public Collection<EventType> getAllTypes() {
        return nameToTypeMap.values();
    }

    public void addType(EventType eventType) {
        String name = eventType.getMetadata().getName();
        if (nameToTypeMap.containsKey(name)) {
            throw new IllegalArgumentException("Event type by name '" + name + "' already registered");
        }
        nameToTypeMap.put(name, eventType);

        long publicId = eventType.getMetadata().getEventTypeIdPair().getPublicId();
        if (compileTime) {
            publicId = CRC32Util.computeCRC32(name);
        } else {
            if (publicId == -1) {
                throw new IllegalArgumentException("Event type by name '" + name + "' has a public id of -1 at runtime");
            }
        }

        EventType sameIdType = idToTypeMap.get(publicId);
        if (sameIdType != null) {
            throw new IllegalArgumentException("Event type by name '" + name + "' has a public crc32 id overlap with event type by name '" + sameIdType.getName() + "', please consider renaming either of these types");
        }
        idToTypeMap.put(publicId, eventType);
    }

    public void removeType(EventType eventType) {
        nameToTypeMap.remove(eventType.getName());
        idToTypeMap.remove(CRC32Util.computeCRC32(eventType.getName()));
    }

    public void mergeFrom(EventTypeRepositoryImpl other) {
        for (Map.Entry<String, EventType> entry : other.getNameToTypeMap().entrySet()) {
            if (nameToTypeMap.containsKey(entry.getKey())) {
                continue;
            }
            addType(entry.getValue());
        }
    }

    public Map<String, EventType> getNameToTypeMap() {
        return nameToTypeMap;
    }
}
