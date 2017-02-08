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
package com.espertech.esper.dataflow.util;

import com.espertech.esper.client.dataflow.EPDataFlowSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFlowSignalManager {

    private Map<Integer, List<DataFlowSignalListener>> listenersPerOp = new HashMap<Integer, List<DataFlowSignalListener>>();

    public void processSignal(int operatorNum, EPDataFlowSignal signal) {
        List<DataFlowSignalListener> listeners = listenersPerOp.get(operatorNum);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (DataFlowSignalListener listener : listeners) {
            listener.processSignal(signal);
        }
    }

    public void addSignalListener(int producerOpNum, DataFlowSignalListener listener) {
        List<DataFlowSignalListener> listeners = listenersPerOp.get(producerOpNum);
        if (listeners == null) {
            listeners = new ArrayList<DataFlowSignalListener>();
            listenersPerOp.put(producerOpNum, listeners);
        }
        listeners.add(listener);
    }
}
