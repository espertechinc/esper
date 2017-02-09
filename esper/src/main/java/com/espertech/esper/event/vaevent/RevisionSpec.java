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

import com.espertech.esper.client.ConfigurationRevisionEventType;
import com.espertech.esper.client.EventType;

/**
 * Specification for how to build a revision event type.
 * <p>
 * Compiled from the information provided via configuration, which has already been validated
 * before building this specification.
 */
public class RevisionSpec {
    private final ConfigurationRevisionEventType.PropertyRevision propertyRevision;
    private final EventType baseEventType;
    private final EventType[] deltaTypes;
    private final String[] deltaNames;
    private final String[] keyPropertyNames;
    private final String[] changesetPropertyNames;
    private final String[] baseEventOnlyPropertyNames;
    private final boolean deltaTypesAddProperties;
    private final boolean[] changesetPropertyDeltaContributed;

    /**
     * Ctor.
     *
     * @param propertyRevision                  strategy to use
     * @param baseEventType                     base type
     * @param deltaTypes                        delta types
     * @param deltaNames                        names of delta types
     * @param keyPropertyNames                  names of key properties
     * @param changesetPropertyNames            names of properties that change
     * @param baseEventOnlyPropertyNames        properties only available on the base event
     * @param deltaTypesAddProperties           boolean to indicate delta types add additional properties.
     * @param changesetPropertyDeltaContributed flag for each property indicating whether its contributed only by a delta event
     */
    public RevisionSpec(ConfigurationRevisionEventType.PropertyRevision propertyRevision, EventType baseEventType, EventType[] deltaTypes, String[] deltaNames, String[] keyPropertyNames, String[] changesetPropertyNames, String[] baseEventOnlyPropertyNames, boolean deltaTypesAddProperties, boolean[] changesetPropertyDeltaContributed) {
        this.propertyRevision = propertyRevision;
        this.baseEventType = baseEventType;
        this.deltaTypes = deltaTypes;
        this.deltaNames = deltaNames;
        this.keyPropertyNames = keyPropertyNames;
        this.changesetPropertyNames = changesetPropertyNames;
        this.baseEventOnlyPropertyNames = baseEventOnlyPropertyNames;
        this.deltaTypesAddProperties = deltaTypesAddProperties;
        this.changesetPropertyDeltaContributed = changesetPropertyDeltaContributed;
    }

    /**
     * Flag for each changeset property to indicate if only the delta contributes the property.
     *
     * @return flag per property
     */
    public boolean[] getChangesetPropertyDeltaContributed() {
        return changesetPropertyDeltaContributed;
    }

    /**
     * Returns the stratgegy for revisioning.
     *
     * @return enum
     */
    public ConfigurationRevisionEventType.PropertyRevision getPropertyRevision() {
        return propertyRevision;
    }

    /**
     * Returns the base event type.
     *
     * @return base type
     */
    public EventType getBaseEventType() {
        return baseEventType;
    }

    /**
     * Returns the delta event types.
     *
     * @return types
     */
    public EventType[] getDeltaTypes() {
        return deltaTypes;
    }

    /**
     * Returns names for delta events.
     *
     * @return event type names for delta events
     */
    public String[] getDeltaNames() {
        return deltaNames;
    }

    /**
     * Returns property names for key properties.
     *
     * @return property names
     */
    public String[] getKeyPropertyNames() {
        return keyPropertyNames;
    }

    /**
     * Returns property names of properties that change by deltas
     *
     * @return prop names
     */
    public String[] getChangesetPropertyNames() {
        return changesetPropertyNames;
    }

    /**
     * Returns the properies only found on the base event.
     *
     * @return base props
     */
    public String[] getBaseEventOnlyPropertyNames() {
        return baseEventOnlyPropertyNames;
    }

    /**
     * Returns true if delta types add properties.
     *
     * @return flag indicating if delta event types add properties
     */
    public boolean isDeltaTypesAddProperties() {
        return deltaTypesAddProperties;
    }
}
