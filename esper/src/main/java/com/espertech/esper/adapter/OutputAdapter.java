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

import java.util.Map;

/**
 * An output adapter transforms engine events and
 */
public interface OutputAdapter extends Adapter {
    /**
     * Sets the subscriptions for the output adapter.
     *
     * @param subscriptionMap is the active subscriptions.
     */
    public void setSubscriptionMap(Map<String, Subscription> subscriptionMap);

    /**
     * Returns the subscriptions.
     *
     * @return map of name and subscription
     */
    public Map<String, Subscription> getSubscriptionMap();

    /**
     * Returns a given subscription by it's name, or null if not found
     *
     * @param subscriptionName is the subscription
     * @return subcription or null
     */
    public Subscription getSubscription(String subscriptionName);
}
