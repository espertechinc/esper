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
package com.espertech.esper.adapter;

/**
 * Subscriptions are associated with an output adapter and dictate which events are sent to a given adapter.
 */
public interface Subscription {
    /**
     * Returns the subscription name.
     *
     * @return subscription name
     */
    public String getSubscriptionName();

    /**
     * Sets the subscription name.
     *
     * @param name is the subscription name
     */
    public void setSubscriptionName(String name);

    /**
     * Returns the type name of the event type we are looking for.
     *
     * @return event type name
     */
    public String getEventTypeName();

    /**
     * Returns the output adapter this subscription is associated with.
     *
     * @return output adapter
     */
    public OutputAdapter getAdapter();

    /**
     * Sets the output adapter this subscription is associated with.
     *
     * @param adapter to set
     */
    public void registerAdapter(OutputAdapter adapter);
}
