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

import java.util.Map;

/**
 * A spec for CSVAdapters.
 */
public class CSVInputAdapterSpec {
    private boolean usingEngineThread, usingExternalTimer, usingTimeSpanEvents;
    private String timestampColumn;
    private String eventTypeName;
    private AdapterInputSource adapterInputSource;
    private Integer eventsPerSec;
    private String[] propertyOrder;
    private boolean looping;
    private Map<String, Object> propertyTypes;

    /**
     * Ctor.
     *
     * @param adapterInputSource - the source for the CSV data
     * @param eventTypeName      - the name for the event type created from the CSV data
     */
    public CSVInputAdapterSpec(AdapterInputSource adapterInputSource, String eventTypeName) {
        this.adapterInputSource = adapterInputSource;
        this.eventTypeName = eventTypeName;
    }

    /**
     * Sets the number of events per seconds.
     *
     * @param eventsPerSec number of events to send per second
     */
    public void setEventsPerSec(int eventsPerSec) {
        this.eventsPerSec = eventsPerSec;
    }

    /**
     * @param propertyOrder - the property order of the properties in the CSV file
     */
    public void setPropertyOrder(String[] propertyOrder) {
        this.propertyOrder = propertyOrder;
    }

    /**
     * @param looping - the isLooping value to set
     */
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    /**
     * Set the propertyTypes value
     *
     * @param propertyTypes - a mapping between the names and types of the properties in the
     *                      CSV file; this will also be the form of the Map event created
     *                      from the data
     */
    public void setPropertyTypes(Map<String, Object> propertyTypes) {
        this.propertyTypes = propertyTypes;
    }

    /**
     * Set to true to use the engine timer thread for the work, or false to use the current thread.
     *
     * @param usingEngineThread true for timer thread
     */
    public void setUsingEngineThread(boolean usingEngineThread) {
        this.usingEngineThread = usingEngineThread;
    }

    /**
     * @return the usingEngineThread
     */
    public boolean isUsingEngineThread() {
        return usingEngineThread;
    }

    /**
     * Set to true to use esper's external timer mechanism instead of internal timing
     *
     * @param usingExternalTimer true for external timer
     */
    public void setUsingExternalTimer(boolean usingExternalTimer) {
        this.usingExternalTimer = usingExternalTimer;
    }

    /**
     * @return true for using external timer
     */
    public boolean isUsingExternalTimer() {
        return usingExternalTimer;
    }

    /**
     * Set the timestamp column name.
     *
     * @param timestampColumn - the name of the column to use for timestamps
     */
    public void setTimestampColumn(String timestampColumn) {
        this.timestampColumn = timestampColumn;
    }

    /**
     * @return the timestampColumn
     */
    public String getTimestampColumn() {
        return timestampColumn;
    }

    /**
     * @return the adapterInputSource
     */
    public AdapterInputSource getAdapterInputSource() {
        return adapterInputSource;
    }

    /**
     * @param adapterInputSource the adapterInputSource to set
     */
    public void setAdapterInputSource(AdapterInputSource adapterInputSource) {
        this.adapterInputSource = adapterInputSource;
    }

    /**
     * @return the eventTypeName
     */
    public String geteventTypeName() {
        return eventTypeName;
    }

    /**
     * @param eventTypeName the eventTypeName to set
     */
    public void seteventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    /**
     * @return the eventsPerSec
     */
    public Integer getEventsPerSec() {
        return eventsPerSec;
    }

    /**
     * @return the looping
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * @return the propertyOrder
     */
    public String[] getPropertyOrder() {
        return propertyOrder;
    }

    /**
     * @return the propertyTypes
     */
    public Map<String, Object> getPropertyTypes() {
        return propertyTypes;
    }

    /**
     * Returns the indicator whether {@link com.espertech.esper.client.time.CurrentTimeEvent} (false, the default)
     * or {@link com.espertech.esper.client.time.CurrentTimeSpanEvent} (true) are used for time advancing.
     *
     * @return indicator
     */
    public boolean isUsingTimeSpanEvents() {
        return usingTimeSpanEvents;
    }

    /**
     * Sets the indicator whether {@link com.espertech.esper.client.time.CurrentTimeEvent} (false, the default)
     * or {@link com.espertech.esper.client.time.CurrentTimeSpanEvent} (true) are used for time advancing.
     *
     * @param usingTimeSpanEvents new value
     */
    public void setUsingTimeSpanEvents(boolean usingTimeSpanEvents) {
        this.usingTimeSpanEvents = usingTimeSpanEvents;
    }
}
