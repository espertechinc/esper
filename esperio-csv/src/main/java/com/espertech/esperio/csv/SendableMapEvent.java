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
package com.espertech.esperio.csv;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of SendableEvent that wraps a Map event for
 * sending into the runtime.
 */
public class SendableMapEvent extends AbstractSendableEvent {
    private final Map<String, Object> mapToSend;
    private final String eventTypeName;

    /**
     * Ctor.
     *
     * @param mapToSend     - the map to send into the runtime
     * @param eventTypeName - the event type name for the map event
     * @param timestamp     - the timestamp for this event
     * @param scheduleSlot  - the schedule slot for the entity that created this event
     */
    public SendableMapEvent(Map<String, Object> mapToSend, String eventTypeName, long timestamp, long scheduleSlot) {
        super(timestamp, scheduleSlot);
        //if properties names contain a '.' we need to rebuild the nested map property
        Map toSend = new HashMap();
        for (String property : mapToSend.keySet()) {
            int dot = property.indexOf('.');
            if (dot > 0) {
                String prefix = property.substring(0, dot);
                String postfix = property.substring(dot + 1, property.length());
                if (!toSend.containsKey(prefix)) {
                    Map nested = new HashMap();
                    nested.put(postfix, mapToSend.get(property));
                    toSend.put(prefix, nested);
                } else {
                    Map nested = (Map) toSend.get(prefix);
                    nested.put(postfix, mapToSend.get(property));
                }
            } else {
                toSend.put(property, mapToSend.get(property));
            }
        }
        this.mapToSend = toSend;
        this.eventTypeName = eventTypeName;
    }

    /* (non-Javadoc)
     * @see com.espertech.esperio.csv.SendableEvent#send(com.espertech.esper.client.EPRuntime)
     */
    public void send(AbstractSender sender) {
        sender.sendEvent(this, mapToSend, eventTypeName);
    }

    public String toString() {
        return mapToSend.toString();
    }
}
