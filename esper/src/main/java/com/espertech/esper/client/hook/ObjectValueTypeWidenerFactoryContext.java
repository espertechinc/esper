/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.client.hook;

import com.espertech.esper.client.EventType;

public class ObjectValueTypeWidenerFactoryContext {
    private final Class clazz;
    private final String propertyName;
    private final EventType eventType;
    private final String statementName;
    private final String engineURI;

    public ObjectValueTypeWidenerFactoryContext(Class clazz, String propertyName, EventType eventType, String statementName, String engineURI) {
        this.clazz = clazz;
        this.propertyName = propertyName;
        this.eventType = eventType;
        this.statementName = statementName;
        this.engineURI = engineURI;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getStatementName() {
        return statementName;
    }

    public String getEngineURI() {
        return engineURI;
    }

    public EventType getEventType() {
        return eventType;
    }
}
