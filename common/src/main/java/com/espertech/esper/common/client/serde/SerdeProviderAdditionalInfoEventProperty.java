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
package com.espertech.esper.common.client.serde;

import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;

/**
 * Information about the event property for which to obtain a serde.
 */
public class SerdeProviderAdditionalInfoEventProperty extends SerdeProviderAdditionalInfo {
    private String eventTypeName;
    private String eventPropertyName;

    /**
     * Ctor.
     * @param raw statement information
     * @param eventTypeName event type name
     * @param eventPropertyName property name
     */
    public SerdeProviderAdditionalInfoEventProperty(StatementRawInfo raw, String eventTypeName, String eventPropertyName) {
        super(raw);
        this.eventTypeName = eventTypeName;
        this.eventPropertyName = eventPropertyName;
    }

    /**
     * Sets the event type name
     *
     * @param eventTypeName name
     */
    public void setEventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    /**
     * Sets the event property name
     *
     * @param eventPropertyName name
     */
    public void setEventPropertyName(String eventPropertyName) {
        this.eventPropertyName = eventPropertyName;
    }

    /**
     * Returns the event property name
     *
     * @return event property name
     */
    public String getEventPropertyName() {
        return eventPropertyName;
    }

    /**
     * Returns the event type name
     *
     * @return event type name
     */
    public String getEventTypeName() {
        return eventTypeName;
    }

    public String toString() {
        return "event-type '" + eventTypeName + '\'' +
            " property '" + eventPropertyName + '\'';
    }
}
