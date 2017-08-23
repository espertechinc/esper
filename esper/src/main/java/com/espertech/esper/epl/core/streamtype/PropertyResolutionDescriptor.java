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
import com.espertech.esper.client.FragmentEventType;

/**
 * Encapsulates the result of resolving a property and optional stream name against a supplied list of streams
 * {@link StreamTypeService}.
 */
public class PropertyResolutionDescriptor {
    private String streamName;
    private EventType streamEventType;
    private String propertyName;
    private int streamNum;
    private Class propertyType;
    private FragmentEventType fragmentEventType;

    /**
     * Ctor.
     *
     * @param streamName        is the stream name
     * @param streamEventType   is the event type of the stream where the property was found
     * @param propertyName      is the regular name of property
     * @param streamNum         is the number offset of the stream
     * @param propertyType      is the type of the property
     * @param fragmentEventType fragment type
     */
    public PropertyResolutionDescriptor(String streamName, EventType streamEventType, String propertyName, int streamNum, Class propertyType, FragmentEventType fragmentEventType) {
        this.streamName = streamName;
        this.streamEventType = streamEventType;
        this.propertyName = propertyName;
        this.streamNum = streamNum;
        this.propertyType = propertyType;
        this.fragmentEventType = fragmentEventType;
    }

    /**
     * Returns stream name.
     *
     * @return stream name
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Returns event type of the stream that the property was found in.
     *
     * @return stream's event type
     */
    public EventType getStreamEventType() {
        return streamEventType;
    }

    /**
     * Returns resolved property name of the property as it exists in a stream.
     *
     * @return property name as resolved in a stream
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the number of the stream the property was found in.
     *
     * @return stream offset number starting at zero to N-1 where N is the number of streams
     */
    public int getStreamNum() {
        return streamNum;
    }

    /**
     * Returns the property type of the resolved property.
     *
     * @return class of property
     */
    public Class getPropertyType() {
        return propertyType;
    }

    public FragmentEventType getFragmentEventType() {
        return fragmentEventType;
    }
}
