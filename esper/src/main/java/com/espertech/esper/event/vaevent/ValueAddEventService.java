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

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.ConfigurationRevisionEventType;
import com.espertech.esper.client.ConfigurationVariantStream;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeIdGenerator;
import com.espertech.esper.view.StatementStopService;

import java.util.Map;

/**
 * Service associating handling vaue-added event types, such a revision event types and variant stream event types.
 * <p>
 * Associates named windows and revision event types.
 */
public interface ValueAddEventService {
    /**
     * Called at initialization time, verifies configurations provided.
     *
     * @param revisionTypes        is the revision types to add
     * @param variantStreams       is the variant streams to add
     * @param eventAdapterService  for obtaining event type information for each name
     * @param eventTypeIdGenerator event type id provider
     */
    public void init(Map<String, ConfigurationRevisionEventType> revisionTypes, Map<String, ConfigurationVariantStream> variantStreams, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator);

    /**
     * Adds a new revision event types.
     *
     * @param name                to add
     * @param config              the revision event type configuration
     * @param eventAdapterService for obtaining event type information for each name
     */
    public void addRevisionEventType(String name, ConfigurationRevisionEventType config, EventAdapterService eventAdapterService);

    /**
     * Adds a new variant stream.
     *
     * @param variantEventTypeName the name of the type
     * @param variantStreamConfig  the configs
     * @param eventAdapterService  for handling nested events
     * @param eventTypeIdGenerator event type id provider
     * @throws ConfigurationException if the configuration is invalid
     */
    public void addVariantStream(String variantEventTypeName, ConfigurationVariantStream variantStreamConfig, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator)
            throws ConfigurationException;

    /**
     * Upon named window creation, and during resolution of type specified as part of a named window create statement,
     * returns looks up the revision event type name provided and return the revision event type if found, or null if not found.
     *
     * @param name to look up
     * @return null if not found, of event type
     */
    public EventType getValueAddUnderlyingType(String name);

    /**
     * Upon named window creation, create a unique revision event type that this window processes.
     *
     * @param namedWindowName      name of window
     * @param typeName             name to use
     * @param statementStopService for handling stops
     * @param eventAdapterService  for event type info
     * @param eventTypeIdGenerator event type id provider
     * @return revision event type
     */
    public EventType createRevisionType(String namedWindowName, String typeName, StatementStopService statementStopService, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator);

    /**
     * Upon named window creation, check if the name used is a revision event type name.
     *
     * @param name to check
     * @return true if revision event type, false if not
     */
    public boolean isRevisionTypeName(String name);

    /**
     * Gets a value-added event processor.
     *
     * @param name of the value-add events
     * @return processor
     */
    public ValueAddEventProcessor getValueAddProcessor(String name);

    /**
     * Returns all event types representing value-add event types.
     *
     * @return value-add event type
     */
    public EventType[] getValueAddedTypes();
}
