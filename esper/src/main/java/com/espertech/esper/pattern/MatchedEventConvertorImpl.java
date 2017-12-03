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
package com.espertech.esper.pattern;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.map.MapEventBean;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapMeta;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Implements a convertor for pattern partial results to events per stream.
 */
public class MatchedEventConvertorImpl implements MatchedEventConvertor {
    private final EventBean[] eventsPerStream;
    private final LinkedHashMap<String, Pair<EventType, String>> filterTypes;
    private final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;
    private final MatchedEventMapMeta matchedEventMapMeta;

    public MatchedEventConvertorImpl(LinkedHashMap<String, Pair<EventType, String>> filterTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTags, EventAdapterService eventAdapterService) {
        int size = filterTypes.size();
        if (arrayEventTypes != null) {
            size += arrayEventTypes.size();
        }

        this.eventsPerStream = new EventBean[size];
        this.filterTypes = new LinkedHashMap<String, Pair<EventType, String>>(filterTypes);
        if (arrayEventTypes != null) {
            this.arrayEventTypes = new LinkedHashMap<String, Pair<EventType, String>>(arrayEventTypes);
        } else {
            this.arrayEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        }
        this.matchedEventMapMeta = new MatchedEventMapMeta(allTags.toArray(new String[allTags.size()]), !this.arrayEventTypes.isEmpty());
    }

    public EventBean[] convert(MatchedEventMap events) {
        int count = 0;
        for (Map.Entry<String, Pair<EventType, String>> entry : filterTypes.entrySet()) {
            EventBean theEvent = events.getMatchingEventByTag(entry.getKey());
            eventsPerStream[count++] = theEvent;
        }
        if (arrayEventTypes != null) {
            for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
                EventBean[] eventArray = (EventBean[]) events.getMatchingEventAsObjectByTag(entry.getKey());
                HashMap map = new HashMap();
                map.put(entry.getKey(), eventArray);
                EventBean theEvent = new MapEventBean(map, null);
                eventsPerStream[count++] = theEvent;
            }
        }
        return eventsPerStream;
    }

    public MatchedEventMapMeta getMatchedEventMapMeta() {
        return matchedEventMapMeta;
    }
}
