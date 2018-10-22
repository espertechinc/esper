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
package com.espertech.esper.runtime.internal.dataflow.op.epstatementsource;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowIRStreamCollector;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowIRStreamCollectorContext;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

public class EmitterCollectorUpdateListener implements UpdateListener {
    private final EPDataFlowIRStreamCollector collector;
    private final LocalEmitter emitterForCollector;
    private final boolean submitEventBean;

    public EmitterCollectorUpdateListener(EPDataFlowIRStreamCollector collector, LocalEmitter emitterForCollector, boolean submitEventBean) {
        this.collector = collector;
        this.emitterForCollector = emitterForCollector;
        this.submitEventBean = submitEventBean;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {

        EPDataFlowIRStreamCollectorContext holder = new EPDataFlowIRStreamCollectorContext(emitterForCollector, submitEventBean, newEvents, oldEvents, statement, runtime);
        collector.collect(holder);
    }
}
