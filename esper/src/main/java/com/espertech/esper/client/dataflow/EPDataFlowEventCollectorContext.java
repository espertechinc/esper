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
package com.espertech.esper.client.dataflow;

/**
 * For use with {@link EPDataFlowEventCollector} provides collection context.
 * <p>
 * Do not retain handles to this instance as its contents may change.
 * </p>
 */
public class EPDataFlowEventCollectorContext {
    private final EventBusCollector eventBusCollector;
    private Object event;

    /**
     * Ctor.
     *
     * @param eventBusCollector for sending events to the event bus
     * @param event             to process
     */
    public EPDataFlowEventCollectorContext(EventBusCollector eventBusCollector, Object event) {
        this.eventBusCollector = eventBusCollector;
        this.event = event;
    }

    /**
     * Returns the event.
     *
     * @return event
     */
    public Object getEvent() {
        return event;
    }

    /**
     * Sets the event.
     *
     * @param event to set
     */
    public void setEvent(Object event) {
        this.event = event;
    }

    /**
     * Returns the emitter for the event bus.
     *
     * @return emitter
     */
    public EventBusCollector getEventBusCollector() {
        return eventBusCollector;
    }
}
