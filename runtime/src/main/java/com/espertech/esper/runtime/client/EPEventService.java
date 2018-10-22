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
package com.espertech.esper.runtime.client;

import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.EventTypeException;

/**
 * Service for processing events and advancing time.
 */
public interface EPEventService extends EPEventServiceSendEvent, EPEventServiceRouteEvent, EPEventServiceTimeControl {

    /**
     * Returns a facility to process event objects that are of a known type.
     * <p>
     * Given an event type name this method returns a sender that allows to send in
     * event objects of that type. The event objects send in via the event sender
     * are expected to match the event type, thus the event sender does
     * not inspect the event object other then perform basic checking.
     * <p>
     * For events backed by a Java class (JavaBean events), the sender ensures that the
     * object send in matches in class, or implements or extends the class underlying the event type
     * for the given event type name. Note that event type identity for Java class events is the Java class.
     * When assigning two different event type names to the same Java class the names are an alias for the same
     * event type i.e. there is always a single event type to represent a given Java class.
     * <p>
     * For events backed by a Object[] (Object-array events), the sender does not perform any checking other
     * then checking that the event object indeed is an array of object.
     * <p>
     * For events backed by a java.util.Map (Map events), the sender does not perform any checking other
     * then checking that the event object indeed implements Map.
     * <p>
     * For events backed by a org.w3c.Node (XML DOM events), the sender checks that the root element name
     * indeed does match the root element name for the event type name.
     *
     * @param eventTypeName is the name of the event type
     * @return sender for fast-access processing of event objects of known type (and content)
     * @throws EventTypeException thrown to indicate that the name does not exist
     */
    EventSender getEventSender(String eventTypeName) throws EventTypeException;

    /**
     * Sets a listener to receive events that are unmatched by any statement.
     * <p>
     * Events that can be unmatched are all events that are send into a runtime via one
     * of the sendEvent methods, or that have been generated via insert-into clause.
     * <p>
     * For an event to be unmatched by any statement, the event must not match any
     * statement's event stream filter criteria (a where-clause is NOT a filter criteria for a stream, as below).
     * <p>
     * Note: In the following statement a MyEvent event does always match
     * this statement's event stream filter criteria, regardless of the value of the 'quantity' property.
     * <pre>select * from MyEvent where quantity &gt; 5</pre>
     * <br>
     * In the following statement only a MyEvent event with a 'quantity' property value of 5 or less does not match
     * this statement's event stream filter criteria:
     * <pre>select * from MyEvent(quantity &gt; 5)</pre>
     * <p>
     * For patterns, if no pattern sub-expression is active for such event, the event is also unmatched.
     *
     * @param listener is the listener to receive notification of unmatched events, or null to unregister a
     *                 previously registered listener
     */
    void setUnmatchedListener(UnmatchedListener listener);

    /**
     * Number of events evaluated over the lifetime of the event stream processing runtime,
     * or since the last resetStats() call.
     *
     * @return number of events received
     */
    long getNumEventsEvaluated();

    /**
     * Reset the count of the number of events received and emitted
     */
    void resetStats();
}
