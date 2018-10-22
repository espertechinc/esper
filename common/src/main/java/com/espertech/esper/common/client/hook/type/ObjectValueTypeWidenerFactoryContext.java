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
package com.espertech.esper.common.client.hook.type;

import com.espertech.esper.common.client.EventType;

/**
 * For Avro types for widening objects to Avro record values, see {@link ObjectValueTypeWidenerFactory}
 */
public class ObjectValueTypeWidenerFactoryContext {
    private final Class clazz;
    private final String propertyName;
    private final EventType eventType;
    private final String statementName;

    /**
     * Ctor.
     *
     * @param clazz         class
     * @param propertyName  property name
     * @param eventType     event type
     * @param statementName statement name
     */
    public ObjectValueTypeWidenerFactoryContext(Class clazz, String propertyName, EventType eventType, String statementName) {
        this.clazz = clazz;
        this.propertyName = propertyName;
        this.eventType = eventType;
        this.statementName = statementName;
    }

    /**
     * Returns the class
     *
     * @return class
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns the property name
     *
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the statement name
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the event type
     *
     * @return event type
     */
    public EventType getEventType() {
        return eventType;
    }
}
