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
package com.espertech.esper.epl.core.streamtype;

import com.espertech.esper.client.EventType;

/**
 * Service supplying stream number and property type information.
 */
public interface StreamTypeService {
    /**
     * Returns the offset of the stream and the type of the property for the given property name,
     * by looking through the types offered and matching up.
     * <p>
     * This method considers only a property name and looks at all streams to resolve the property name.
     *
     * @param propertyName   - property name in event
     * @param obtainFragment indicator whether fragment should be returned
     * @return descriptor with stream number, property type and property name
     * @throws DuplicatePropertyException to indicate property was found twice
     * @throws PropertyNotFoundException  to indicate property could not be resolved
     */
    public PropertyResolutionDescriptor resolveByPropertyName(String propertyName, boolean obtainFragment)
            throws DuplicatePropertyException, PropertyNotFoundException;

    /**
     * Returns the offset of the stream and the type of the property for the given property name,
     * by looking through the types offered considering only explicitly listed properties and matching up.
     * <p>
     * This method considers only a property name and looks at all streams to resolve the property name.
     *
     * @param propertyName   - property name in event
     * @param obtainFragment indicator whether fragment should be returned
     * @return descriptor with stream number, property type and property name
     * @throws DuplicatePropertyException to indicate property was found twice
     * @throws PropertyNotFoundException  to indicate property could not be resolved
     */
    public PropertyResolutionDescriptor resolveByPropertyNameExplicitProps(String propertyName, boolean obtainFragment)
            throws PropertyNotFoundException, DuplicatePropertyException;

    /**
     * Returns the offset of the stream and the type of the property for the given property name,
     * by using the specified stream name to resolve the property.
     * <p>
     * This method considers and explicit stream name and property name, both parameters are required.
     *
     * @param streamName     - name of stream, required
     * @param propertyName   - property name in event, , required
     * @param obtainFragment indicator whether fragment should be returned
     * @return descriptor with stream number, property type and property name
     * @throws PropertyNotFoundException to indicate property could not be resolved
     * @throws StreamNotFoundException   to indicate stream name could not be resolved
     */
    public PropertyResolutionDescriptor resolveByStreamAndPropName(String streamName, String propertyName, boolean obtainFragment)
            throws PropertyNotFoundException, StreamNotFoundException;

    /**
     * Returns the offset of the stream and the type of the property for the given property name,
     * by using the specified stream name to resolve the property and considering only explicitly listed properties.
     * <p>
     * This method considers and explicit stream name and property name, both parameters are required.
     *
     * @param streamName     - name of stream, required
     * @param propertyName   - property name in event, , required
     * @param obtainFragment indicator whether fragment should be returned
     * @return descriptor with stream number, property type and property name
     * @throws PropertyNotFoundException to indicate property could not be resolved
     * @throws StreamNotFoundException   to indicate stream name could not be resolved
     */
    public PropertyResolutionDescriptor resolveByStreamAndPropNameExplicitProps(String streamName, String propertyName, boolean obtainFragment)
            throws PropertyNotFoundException, StreamNotFoundException;

    /**
     * Returns the offset of the stream and the type of the property for the given property name,
     * by looking through the types offered and matching up.
     * <p>
     * This method considers a single property name that may or may not be prefixed by a stream name.
     * The resolution first attempts to find the property name itself, then attempts
     * to consider a stream name that may be part of the property name.
     *
     * @param streamAndPropertyName - stream name and property name (e.g. s0.p0) or just a property name (p0)
     * @param obtainFragment        indicator whether fragment should be returned
     * @return descriptor with stream number, property type and property name
     * @throws DuplicatePropertyException to indicate property was found twice
     * @throws PropertyNotFoundException  to indicate property could not be resolved
     */
    public PropertyResolutionDescriptor resolveByStreamAndPropName(String streamAndPropertyName, boolean obtainFragment)
            throws DuplicatePropertyException, PropertyNotFoundException;

    /**
     * Returns an array of event stream names in the order declared.
     *
     * @return stream names
     */
    public String[] getStreamNames();

    /**
     * Returns an array of event types for each event stream in the order declared.
     *
     * @return event types
     */
    public EventType[] getEventTypes();

    /**
     * Returns true for each stream without a data window.
     *
     * @return true for non-windowed streams.
     */
    public boolean[] getIStreamOnly();

    public int getStreamNumForStreamName(String streamWildcard);

    public boolean isOnDemandStreams();

    public String getEngineURIQualifier();

    public boolean hasPropertyAgnosticType();

    public boolean hasTableTypes();

    public boolean isStreamZeroUnambigous();

    public boolean isOptionalStreams();
}
