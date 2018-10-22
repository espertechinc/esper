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

import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;

import java.util.concurrent.LinkedBlockingQueue;

public class LocalEmitter implements EPDataFlowEmitter {

    private final LinkedBlockingQueue<Object> queue;

    public LocalEmitter(LinkedBlockingQueue<Object> queue) {
        this.queue = queue;
    }

    public void submit(Object object) {
        queue.add(object);
    }

    public void submitSignal(EPDataFlowSignal signal) {
        queue.add(signal);
    }

    public void submitPort(int portNumber, Object object) {
        queue.add(object);
    }
}
