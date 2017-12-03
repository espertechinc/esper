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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.core.service.*;
import com.espertech.esper.epl.metric.StatementMetricHandle;
import com.espertech.esper.filter.FilterHandleCallback;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.FilterValueSet;

import java.util.Collection;
import java.util.List;

/**
 * Subscription is a concept for selecting events for processing out of all events available from an engine instance.
 */
public abstract class BaseSubscription implements Subscription, FilterHandleCallback {
    /**
     * The output adapter to which the subscription applies.
     */
    protected OutputAdapter adapter;

    /**
     * The event type of the events we are subscribing for.
     */
    protected String eventTypeName;

    /**
     * The name of the subscription.
     */
    protected String subscriptionName;

    public abstract void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches);

    /**
     * Ctor, assigns default name.
     */
    public BaseSubscription() {
        subscriptionName = "default";
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public String getEventTypeName() {
        return eventTypeName;
    }

    /**
     * Set the event type name we are looking for.
     *
     * @param eventTypeName is a type name
     */
    public void seteventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    public OutputAdapter getAdapter() {
        return adapter;
    }

    public void registerAdapter(OutputAdapter adapter) {
        this.adapter = adapter;
        registerAdapter(((AdapterSPI) adapter).getEPServiceProvider());
    }

    /**
     * Register an adapter.
     *
     * @param epService engine
     */
    public void registerAdapter(EPServiceProvider epService) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        EventType eventType = spi.getEventAdapterService().getExistsTypeByName(eventTypeName);
        FilterValueSet fvs = new FilterSpecCompiled(eventType, null, new List[0], null).getValueSet(null, null, null, null, null);

        String name = "subscription:" + subscriptionName;
        StatementMetricHandle metricsHandle = spi.getMetricReportingService().getStatementHandle(-1, name);
        EPStatementHandle statementHandle = new EPStatementHandle(-1, name, name, StatementType.ESPERIO, name, false, metricsHandle, 0, false, false, spi.getServicesContext().getMultiMatchHandlerFactory().getDefaultHandler());
        EPStatementAgentInstanceHandle agentHandle = new EPStatementAgentInstanceHandle(statementHandle, new StatementAgentInstanceRWLockImpl(false), -1, new StatementAgentInstanceFilterVersion(), null);
        EPStatementHandleCallback registerHandle = new EPStatementHandleCallback(agentHandle, this);
        spi.getFilterService().add(fvs, registerHandle);
    }
}
