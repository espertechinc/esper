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
package com.espertech.esper.epl.named;

/**
 * Event indicating named window lifecycle management.
 */
public class NamedWindowLifecycleEvent {
    private String name;
    private NamedWindowProcessor processor;
    private NamedWindowLifecycleEvent.LifecycleEventType eventType;
    private Object[] parameters;

    /**
     * Event types.
     */
    public static enum LifecycleEventType {
        /**
         * Named window created.
         */
        CREATE,

        /**
         * Named window removed.
         */
        DESTROY
    }

    /**
     * Ctor.
     *
     * @param name       is the name of the named window
     * @param processor  instance for processing the named window contents
     * @param eventType  the type of event
     * @param parameters event parameters
     */
    protected NamedWindowLifecycleEvent(String name, NamedWindowProcessor processor, NamedWindowLifecycleEvent.LifecycleEventType eventType, Object... parameters) {
        this.name = name;
        this.processor = processor;
        this.eventType = eventType;
        this.parameters = parameters;
    }

    /**
     * Returns the named window name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the processor originating the event.
     *
     * @return processor
     */
    public NamedWindowProcessor getProcessor() {
        return processor;
    }

    /**
     * Returns the event type.
     *
     * @return type of event
     */
    public NamedWindowLifecycleEvent.LifecycleEventType getEventType() {
        return eventType;
    }

    /**
     * Returns event parameters.
     *
     * @return params
     */
    public Object[] getParameters() {
        return parameters;
    }
}
