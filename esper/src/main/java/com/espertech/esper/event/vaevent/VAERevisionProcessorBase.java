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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Base revision processor.
 */
public abstract class VAERevisionProcessorBase implements ValueAddEventProcessor {
    /**
     * Revision type specification.
     */
    protected final RevisionSpec revisionSpec;

    /**
     * Name of type.
     */
    protected final String revisionEventTypeName;

    /**
     * Revision event type.
     */
    protected RevisionEventType revisionEventType;

    /**
     * For interogating nested properties.
     */
    protected EventAdapterService eventAdapterService;

    /**
     * Map of participating type to descriptor.
     */
    protected Map<EventType, RevisionTypeDesc> typeDescriptors;

    /**
     * Ctor.
     *
     * @param revisionSpec          specification
     * @param revisioneventTypeName name of event type
     * @param eventAdapterService   for nested property handling
     */
    protected VAERevisionProcessorBase(RevisionSpec revisionSpec, String revisioneventTypeName, EventAdapterService eventAdapterService) {
        this.revisionSpec = revisionSpec;
        this.revisionEventTypeName = revisioneventTypeName;
        this.eventAdapterService = eventAdapterService;
        this.typeDescriptors = new HashMap<EventType, RevisionTypeDesc>();
    }

    public RevisionEventType getValueAddEventType() {
        return revisionEventType;
    }

    public void validateEventType(EventType eventType) throws ExprValidationException {
        if (eventType == revisionSpec.getBaseEventType()) {
            return;
        }
        if (typeDescriptors.containsKey(eventType)) {
            return;
        }

        if (eventType == null) {
            throw new ExprValidationException(getMessage());
        }

        // Check all the supertypes to see if one of the matches the full or delta types
        Iterator<EventType> deepSupers = eventType.getDeepSuperTypes();
        if (deepSupers == null) {
            throw new ExprValidationException(getMessage());
        }

        EventType type;
        for (; deepSupers.hasNext(); ) {
            type = deepSupers.next();
            if (type == revisionSpec.getBaseEventType()) {
                return;
            }
            if (typeDescriptors.containsKey(type)) {
                return;
            }
        }

        throw new ExprValidationException(getMessage());
    }

    private String getMessage() {
        return "Selected event type is not a valid base or delta event type of revision event type '"
                + revisionEventTypeName + "'";
    }

    public EventAdapterService getEventAdapterService() {
        return eventAdapterService;
    }
}
