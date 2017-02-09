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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;

/**
 * Context for use with {@link EPDataFlowIRStreamCollector}.
 * <p>
 * Do not retain a handle of this object as its contents are subject to change.
 * </p>
 */
public class EPDataFlowIRStreamCollectorContext {
    private final EPDataFlowEmitter emitter;
    private final boolean submitEventBean;
    private EventBean[] newEvents;
    private EventBean[] oldEvents;
    private EPStatement statement;
    private EPServiceProvider epServiceProvider;

    /**
     * Ctor.
     *
     * @param emitter           data flow emitter
     * @param submitEventBean   indicator whether the EventBean or the underlying event object must be submmitted
     * @param newEvents         insert stream events
     * @param oldEvents         remove stream events
     * @param statement         statement posting events
     * @param epServiceProvider engine instances
     */
    public EPDataFlowIRStreamCollectorContext(EPDataFlowEmitter emitter, boolean submitEventBean, EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
        this.emitter = emitter;
        this.submitEventBean = submitEventBean;
        this.newEvents = newEvents;
        this.oldEvents = oldEvents;
        this.statement = statement;
        this.epServiceProvider = epServiceProvider;
    }

    /**
     * Returns the emitter.
     *
     * @return emitter
     */
    public EPDataFlowEmitter getEmitter() {
        return emitter;
    }

    /**
     * Returns insert stream.
     *
     * @return events
     */
    public EventBean[] getNewEvents() {
        return newEvents;
    }

    /**
     * Returns remove stream.
     *
     * @return events
     */
    public EventBean[] getOldEvents() {
        return oldEvents;
    }

    /**
     * Returns the statement.
     *
     * @return statement
     */
    public EPStatement getStatement() {
        return statement;
    }

    /**
     * Returns the engine instance.
     *
     * @return engine instance
     */
    public EPServiceProvider getEpServiceProvider() {
        return epServiceProvider;
    }

    /**
     * Sets insert stream events
     *
     * @param newEvents to set
     */
    public void setNewEvents(EventBean[] newEvents) {
        this.newEvents = newEvents;
    }

    /**
     * Sets remove stream events
     *
     * @param oldEvents to set
     */
    public void setOldEvents(EventBean[] oldEvents) {
        this.oldEvents = oldEvents;
    }

    /**
     * Sets statement.
     *
     * @param statement to set
     */
    public void setStatement(EPStatement statement) {
        this.statement = statement;
    }

    /**
     * Sets engine instance.
     *
     * @param epServiceProvider to set
     */
    public void setEpServiceProvider(EPServiceProvider epServiceProvider) {
        this.epServiceProvider = epServiceProvider;
    }

    /**
     * Returns indicator whether to submit wrapped events (EventBean) or underlying events
     *
     * @return wrapped event indicator
     */
    public boolean isSubmitEventBean() {
        return submitEventBean;
    }
}
