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
package com.espertech.esper.client.util;

import com.espertech.esper.client.EventType;
import com.espertech.esper.event.util.OutputValueRenderer;

/**
 * Context for use with the {@link EventPropertyRenderer} interface for use with the JSON or XML event renderes to handle custom event property rendering.
 * <p>Do not retain a handle to the renderer context as this object changes for each event property.</p>
 */
public class EventPropertyRendererContext {
    private final EventType eventType;
    private final boolean jsonFormatted;

    private String propertyName;
    private Object propertyValue;
    private Integer indexedPropertyIndex;
    private String mappedPropertyKey;
    private StringBuilder stringBuilder;
    private OutputValueRenderer defaultRenderer;

    /**
     * Ctor.
     *
     * @param eventType     event type
     * @param jsonFormatted boolean if JSON formatted
     */
    public EventPropertyRendererContext(EventType eventType, boolean jsonFormatted) {
        this.eventType = eventType;
        this.jsonFormatted = jsonFormatted;
    }

    /**
     * Returns the property name to be rendered.
     *
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the property name to be rendered.
     *
     * @param propertyName property name
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Returns the property value.
     *
     * @return value
     */
    public Object getPropertyValue() {
        return propertyValue;
    }

    /**
     * Sets the property value.
     *
     * @param propertyValue to set
     */
    public void setPropertyValue(Object propertyValue) {
        this.propertyValue = propertyValue;
    }

    /**
     * Returns the output value default renderer.
     *
     * @return renderer
     */
    public OutputValueRenderer getDefaultRenderer() {
        return defaultRenderer;
    }

    /**
     * Sets the output value default renderer.
     *
     * @param defaultRenderer renderer to set
     */
    public void setDefaultRenderer(OutputValueRenderer defaultRenderer) {
        this.defaultRenderer = defaultRenderer;
    }

    /**
     * Sets the string builer
     *
     * @param stringBuilder to set
     */
    public void setStringBuilderAndReset(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
        this.mappedPropertyKey = null;
        this.indexedPropertyIndex = null;
    }

    /**
     * Returns the string builder.
     *
     * @return string builder to use
     */
    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    /**
     * Returns the event type
     *
     * @return event type
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns the index for indexed properties.
     *
     * @return property index
     */
    public Integer getIndexedPropertyIndex() {
        return indexedPropertyIndex;
    }

    /**
     * Sets the index for indexed properties.
     *
     * @param indexedPropertyIndex property index
     */
    public void setIndexedPropertyIndex(Integer indexedPropertyIndex) {
        this.indexedPropertyIndex = indexedPropertyIndex;
    }

    /**
     * Returns the map key for mapped properties
     *
     * @return map key
     */
    public String getMappedPropertyKey() {
        return mappedPropertyKey;
    }

    /**
     * Sets the map key for mapped properties
     *
     * @param mappedPropertyKey map key to set
     */
    public void setMappedPropertyKey(String mappedPropertyKey) {
        this.mappedPropertyKey = mappedPropertyKey;
    }

    /**
     * Returns true for JSON formatted.
     *
     * @return indicator
     */
    public boolean isJsonFormatted() {
        return jsonFormatted;
    }

    /**
     * Copies context.
     *
     * @return copy
     */
    public EventPropertyRendererContext copy() {
        EventPropertyRendererContext copy = new EventPropertyRendererContext(this.getEventType(), this.isJsonFormatted());
        copy.setMappedPropertyKey(this.getMappedPropertyKey());
        copy.setIndexedPropertyIndex(this.getIndexedPropertyIndex());
        copy.setDefaultRenderer(this.getDefaultRenderer());
        copy.setPropertyName(this.getPropertyName());
        copy.setPropertyValue(this.getPropertyValue());
        return copy;
    }
}
