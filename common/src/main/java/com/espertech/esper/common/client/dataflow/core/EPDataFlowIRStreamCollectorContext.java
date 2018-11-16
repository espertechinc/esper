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
package com.espertech.esper.common.client.dataflow.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;

/**
 * Context for use with {@link EPDataFlowIRStreamCollector}.
 * <p>
 * Do not retain a handle of this object as its contents are subject to change.
 * </p>
 */
public class EPDataFlowIRStreamCollectorContext {
    private final EPDataFlowEmitter emitter;
    private final boolean submitEventBean;
    private final EventBean[] newEvents;
    private final EventBean[] oldEvents;
    private final Object statement;
    private final Object runtime;

    /**
     * Ctor.
     *
     * @param emitter         data flow emitter
     * @param submitEventBean indicator whether the EventBean or the underlying event object must be submmitted
     * @param newEvents       insert stream events
     * @param oldEvents       remove stream events
     * @param statement       statement posting events
     * @param runtime         runtime instance
     */
    public EPDataFlowIRStreamCollectorContext(EPDataFlowEmitter emitter, boolean submitEventBean, EventBean[] newEvents, EventBean[] oldEvents, Object statement, Object runtime) {
        this.emitter = emitter;
        this.submitEventBean = submitEventBean;
        this.newEvents = newEvents;
        this.oldEvents = oldEvents;
        this.statement = statement;
        this.runtime = runtime;
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
     * Returns the statement and can safely be cast to EPStatement when needed(typed object to not require a dependency on runtime)
     *
     * @return statement
     */
    public Object getStatement() {
        return statement;
    }

    /**
     * Returns the runtime instance and can safely be cast to runtime when needed(typed object to not require a dependency on runtime)
     *
     * @return runtime instance
     */
    public Object getRuntime() {
        return runtime;
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
