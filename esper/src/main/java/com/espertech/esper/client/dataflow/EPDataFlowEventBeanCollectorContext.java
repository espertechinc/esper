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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;

/**
 * For use with {@link EPDataFlowEventBeanCollector} provides collection context.
 * <p>
 * Do not retain handles to this instance as its contents may change.
 * </p>
 */
public class EPDataFlowEventBeanCollectorContext {
    private final EPDataFlowEmitter emitter;
    private final boolean submitEventBean;
    private EventBean event;

    /**
     * Ctor.
     *
     * @param emitter         to emit into the data flow
     * @param submitEventBean indicator whether to submit EventBean or underlying events
     * @param event           to process
     */
    public EPDataFlowEventBeanCollectorContext(EPDataFlowEmitter emitter, boolean submitEventBean, EventBean event) {
        this.emitter = emitter;
        this.submitEventBean = submitEventBean;
        this.event = event;
    }

    /**
     * Returns the event to process.
     *
     * @return event
     */
    public EventBean getEvent() {
        return event;
    }

    /**
     * Sets the event to process.
     *
     * @param event to process
     */
    public void setEvent(EventBean event) {
        this.event = event;
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
     * Returns true to submit EventBean instances, false to submit underlying event.
     *
     * @return indicator whether wrapper required or not
     */
    public boolean isSubmitEventBean() {
        return submitEventBean;
    }
}
